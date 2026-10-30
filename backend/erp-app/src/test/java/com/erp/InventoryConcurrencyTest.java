package com.erp;

import com.erp.common.exception.BizException;
import com.erp.common.security.LoginUser;
import com.erp.inventory.entity.InvStock;
import com.erp.inventory.mapper.InvFlowMapper;
import com.erp.inventory.mapper.InvStockMapper;
import com.erp.inventory.service.StockService;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.entity.MdWarehouse;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.mapper.MdWarehouseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存并发安全测试：行锁 + 状态校验，验证并发扣减不会超卖、不出现负库存。
 * 使用独立的测试商品与仓库，测试结束物理清理，不污染业务数据。
 */
@SpringBootTest
class InventoryConcurrencyTest {

    @Autowired
    private StockService stockService;
    @Autowired
    private MdProductMapper productMapper;
    @Autowired
    private MdWarehouseMapper warehouseMapper;
    @Autowired
    private InvStockMapper stockMapper;
    @Autowired
    private InvFlowMapper flowMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long productId;
    private Long warehouseId;

    @BeforeEach
    void setUp() {
        LoginUser admin = new LoginUser();
        admin.setUserId(1L);
        admin.setUsername("admin");
        admin.setRoleCodes(new HashSet<>(Set.of("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));

        MdProduct product = new MdProduct();
        product.setCode("TST-P-" + System.nanoTime());
        product.setName("并发测试商品");
        product.setTaxRate(new BigDecimal("0.1300"));
        product.setPurchasePrice(new BigDecimal("10.0000"));
        product.setSalePrice(new BigDecimal("20.0000"));
        product.setSafetyStock(BigDecimal.ZERO);
        product.setStatus(1);
        productMapper.insert(product);
        productId = product.getId();

        MdWarehouse warehouse = new MdWarehouse();
        warehouse.setCode("TST-W-" + System.nanoTime());
        warehouse.setName("并发测试仓库");
        warehouse.setStatus(1);
        warehouseMapper.insert(warehouse);
        warehouseId = warehouse.getId();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM inv_flow WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM inv_stock WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM md_product WHERE id = ?", productId);
        jdbcTemplate.update("DELETE FROM md_warehouse WHERE id = ?", warehouseId);
        SecurityContextHolder.clearContext();
    }

    @Test
    void concurrentDecrease_shouldNeverOversell() throws Exception {
        stockService.increase(productId, warehouseId, "", new BigDecimal("100"), new BigDecimal("10"),
                "OTHER_IN", "TEST-IN", null, null, "并发测试期初");

        int threads = 20;
        int perThread = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    stockService.decrease(productId, warehouseId, "", new BigDecimal(perThread),
                            "OTHER_OUT", "TEST-OUT", null, null, "并发扣减");
                    success.incrementAndGet();
                } catch (BizException ex) {
                    insufficient.incrementAndGet();
                } catch (Exception ex) {
                    unexpected.incrementAndGet();
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "并发任务应在30秒内完成");

        InvStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                .eq(InvStock::getProductId, productId)
                .eq(InvStock::getWarehouseId, warehouseId)
                .eq(InvStock::getBatchNo, ""));

        assertEquals(0, unexpected.get(), "不应出现非业务异常");
        assertEquals(10, success.get(), "100 库存按 10 扣减，只允许 10 次成功");
        assertEquals(10, insufficient.get(), "其余 10 次应因库存不足失败");
        assertEquals(0, stock.getQtyOnHand().compareTo(BigDecimal.ZERO), "最终库存应为 0，不能为负");
        assertEquals(11, flowMapper.selectCount(new LambdaQueryWrapper<com.erp.inventory.entity.InvFlow>()
                .eq(com.erp.inventory.entity.InvFlow::getProductId, productId)), "流水=1条入库+10条成功出库");
    }

    @Test
    void weightedAverageCost_shouldBeCalculatedOnIncrease() {
        stockService.increase(productId, warehouseId, "B1", new BigDecimal("100"), new BigDecimal("10"),
                "OTHER_IN", "TEST-A", null, null, "第一批");
        stockService.increase(productId, warehouseId, "B1", new BigDecimal("100"), new BigDecimal("20"),
                "OTHER_IN", "TEST-B", null, null, "第二批");

        BigDecimal avg = stockService.averageCost(productId, warehouseId, "B1");
        assertEquals(0, avg.compareTo(new BigDecimal("15")), "加权平均成本应为 (100*10+100*20)/200=15");

        stockService.decrease(productId, warehouseId, "B1", new BigDecimal("50"),
                "OTHER_OUT", "TEST-C", null, null, "领用");
        InvStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                .eq(InvStock::getProductId, productId)
                .eq(InvStock::getWarehouseId, warehouseId)
                .eq(InvStock::getBatchNo, "B1"));
        assertEquals(0, stock.getQtyOnHand().compareTo(new BigDecimal("150")));
        assertEquals(0, stock.getAvgCost().compareTo(new BigDecimal("15")), "出库不改变移动平均成本");
    }
}

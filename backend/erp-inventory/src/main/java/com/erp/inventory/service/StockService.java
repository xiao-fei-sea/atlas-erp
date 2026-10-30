package com.erp.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.security.SecurityUtils;
import com.erp.common.util.MoneyUtils;
import com.erp.inventory.dto.StockView;
import com.erp.inventory.entity.InvFlow;
import com.erp.inventory.entity.InvStock;
import com.erp.inventory.mapper.InvFlowMapper;
import com.erp.inventory.mapper.InvStockMapper;
import com.erp.system.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存核心服务：所有库存变动必须走本服务，保证行锁 + 流水一致。
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final InvStockMapper stockMapper;
    private final InvFlowMapper flowMapper;
    private final SequenceService sequenceService;

    public PageResult<StockView> pageStocks(long page, long size, String keyword, Long warehouseId, boolean onlyAvailable) {
        IPage<StockView> result = stockMapper.selectStockPage(new Page<>(page, size), keyword, warehouseId, onlyAvailable);
        return PageResult.of(result);
    }

    public PageResult<com.erp.inventory.dto.FlowView> pageFlows(long page, long size, Long productId,
                                                                 Long warehouseId, String bizType, String bizNo) {
        return PageResult.of(flowMapper.selectFlowPage(new Page<>(page, size), productId, warehouseId, bizType, bizNo));
    }

    public List<StockView> warnings() {
        return stockMapper.selectWarnings();
    }

    @Transactional(rollbackFor = Exception.class)
    public void increase(Long productId, Long warehouseId, String batchNo, BigDecimal qty, BigDecimal unitCost,
                         String bizType, String bizNo, String refType, Long refId, String remark) {
        BigDecimal quantity = requirePositive(qty);
        String batch = normalizeBatch(batchNo);
        BigDecimal cost = MoneyUtils.nz(unitCost);
        InvStock stock = stockMapper.selectForUpdate(productId, warehouseId, batch);
        BigDecimal before;
        if (stock == null) {
            stock = new InvStock();
            stock.setProductId(productId);
            stock.setWarehouseId(warehouseId);
            stock.setBatchNo(batch);
            stock.setQtyOnHand(quantity);
            stock.setQtyReserved(MoneyUtils.zero());
            stock.setQtyFrozen(MoneyUtils.zero());
            stock.setAvgCost(cost);
            stock.setVersion(0);
            stockMapper.insert(stock);
            before = BigDecimal.ZERO;
        } else {
            before = MoneyUtils.nz(stock.getQtyOnHand());
            BigDecimal totalCost = before.multiply(MoneyUtils.nz(stock.getAvgCost())).add(quantity.multiply(cost));
            BigDecimal newQty = before.add(quantity);
            stock.setAvgCost(newQty.compareTo(BigDecimal.ZERO) > 0
                    ? totalCost.divide(newQty, MoneyUtils.SCALE, RoundingMode.HALF_UP)
                    : cost);
            stock.setQtyOnHand(newQty);
            stockMapper.updateById(stock);
        }
        writeFlow(productId, warehouseId, batch, quantity, before, before.add(quantity), cost,
                bizType, bizNo, refType, refId, remark);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decrease(Long productId, Long warehouseId, String batchNo, BigDecimal qty,
                         String bizType, String bizNo, String refType, Long refId, String remark) {
        BigDecimal quantity = requirePositive(qty);
        String batch = normalizeBatch(batchNo);
        InvStock stock = stockMapper.selectForUpdate(productId, warehouseId, batch);
        if (stock == null) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "库存不足：商品库存不存在");
        }
        BigDecimal onHand = MoneyUtils.nz(stock.getQtyOnHand());
        BigDecimal locked = MoneyUtils.nz(stock.getQtyReserved()).add(MoneyUtils.nz(stock.getQtyFrozen()));
        BigDecimal available = onHand.subtract(locked);
        if (available.compareTo(quantity) < 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "库存不足，可用数量 " + available.stripTrailingZeros().toPlainString());
        }
        stock.setQtyOnHand(onHand.subtract(quantity));
        stockMapper.updateById(stock);
        writeFlow(productId, warehouseId, batch, quantity.negate(), onHand, onHand.subtract(quantity),
                MoneyUtils.nz(stock.getAvgCost()), bizType, bizNo, refType, refId, remark);
    }

    @Transactional(rollbackFor = Exception.class)
    public void freeze(Long productId, Long warehouseId, String batchNo, BigDecimal qty, boolean freeze) {
        BigDecimal quantity = requirePositive(qty);
        InvStock stock = stockMapper.selectForUpdate(productId, warehouseId, normalizeBatch(batchNo));
        if (stock == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "库存记录不存在");
        }
        BigDecimal onHand = MoneyUtils.nz(stock.getQtyOnHand());
        BigDecimal frozen = MoneyUtils.nz(stock.getQtyFrozen());
        if (freeze) {
            if (onHand.subtract(MoneyUtils.nz(stock.getQtyReserved())).subtract(frozen).compareTo(quantity) < 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "可冻结数量不足");
            }
            stock.setQtyFrozen(frozen.add(quantity));
        } else {
            if (frozen.compareTo(quantity) < 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "解冻数量不能大于冻结数量");
            }
            stock.setQtyFrozen(frozen.subtract(quantity));
        }
        stockMapper.updateById(stock);
    }

    public BigDecimal averageCost(Long productId, Long warehouseId, String batchNo) {
        InvStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                .eq(InvStock::getProductId, productId)
                .eq(InvStock::getWarehouseId, warehouseId)
                .eq(InvStock::getBatchNo, normalizeBatch(batchNo)));
        return stock == null ? MoneyUtils.zero() : MoneyUtils.nz(stock.getAvgCost());
    }

    public BigDecimal available(Long productId, Long warehouseId, String batchNo) {
        InvStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                .eq(InvStock::getProductId, productId)
                .eq(InvStock::getWarehouseId, warehouseId)
                .eq(InvStock::getBatchNo, normalizeBatch(batchNo)));
        if (stock == null) {
            return MoneyUtils.zero();
        }
        return MoneyUtils.nz(stock.getQtyOnHand())
                .subtract(MoneyUtils.nz(stock.getQtyReserved()))
                .subtract(MoneyUtils.nz(stock.getQtyFrozen()));
    }

    private void writeFlow(Long productId, Long warehouseId, String batch, BigDecimal change,
                           BigDecimal before, BigDecimal after, BigDecimal unitCost,
                           String bizType, String bizNo, String refType, Long refId, String remark) {
        InvFlow flow = new InvFlow();
        flow.setFlowNo(sequenceService.next("FLOW"));
        flow.setBizType(bizType);
        flow.setBizNo(bizNo);
        flow.setProductId(productId);
        flow.setWarehouseId(warehouseId);
        flow.setBatchNo(batch);
        flow.setQtyChange(change);
        flow.setQtyBefore(before);
        flow.setQtyAfter(after);
        flow.setUnitCost(unitCost);
        flow.setAmount(change.multiply(unitCost).setScale(MoneyUtils.SCALE, RoundingMode.HALF_UP));
        flow.setRefType(refType);
        flow.setRefId(refId);
        flow.setRemark(remark);
        flow.setOperatorId(SecurityUtils.getUserId());
        flow.setCreatedAt(LocalDateTime.now());
        flowMapper.insert(flow);
    }

    private BigDecimal requirePositive(BigDecimal qty) {
        BigDecimal value = MoneyUtils.nz(qty);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "数量必须大于0");
        }
        return value;
    }

    private String normalizeBatch(String batchNo) {
        return StringUtils.hasText(batchNo) ? batchNo.trim() : "";
    }
}

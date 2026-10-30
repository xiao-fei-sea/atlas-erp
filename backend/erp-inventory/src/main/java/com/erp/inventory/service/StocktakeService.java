package com.erp.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.MoneyUtils;
import com.erp.inventory.dto.StocktakeRequest;
import com.erp.inventory.entity.InvStock;
import com.erp.inventory.entity.InvStocktake;
import com.erp.inventory.entity.InvStocktakeItem;
import com.erp.inventory.mapper.InvStockMapper;
import com.erp.inventory.mapper.InvStocktakeItemMapper;
import com.erp.inventory.mapper.InvStocktakeMapper;
import com.erp.masterdata.service.WarehouseService;
import com.erp.system.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StocktakeService {

    private final InvStocktakeMapper stocktakeMapper;
    private final InvStocktakeItemMapper itemMapper;
    private final InvStockMapper stockMapper;
    private final StockService stockService;
    private final WarehouseService warehouseService;
    private final SequenceService sequenceService;

    public PageResult<InvStocktake> page(long page, long size, String status, Long warehouseId) {
        LambdaQueryWrapper<InvStocktake> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), InvStocktake::getStatus, status)
                .eq(warehouseId != null, InvStocktake::getWarehouseId, warehouseId)
                .orderByDesc(InvStocktake::getId);
        return PageResult.of(stocktakeMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Map<String, Object> detail(Long id) {
        InvStocktake stocktake = getRequired(id);
        List<InvStocktakeItem> items = itemMapper.selectList(new LambdaQueryWrapper<InvStocktakeItem>()
                .eq(InvStocktakeItem::getStocktakeId, id));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stocktake", stocktake);
        data.put("items", items);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(StocktakeRequest request) {
        if (request.getWarehouseId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仓库不能为空");
        }
        warehouseService.getRequired(request.getWarehouseId());
        InvStocktake stocktake = new InvStocktake();
        stocktake.setStocktakeNo(sequenceService.next("STOCKTAKE"));
        stocktake.setWarehouseId(request.getWarehouseId());
        stocktake.setStocktakeDate(request.getStocktakeDate() == null ? LocalDate.now() : request.getStocktakeDate());
        stocktake.setStatus("DRAFT");
        stocktake.setRemark(request.getRemark());
        stocktakeMapper.insert(stocktake);

        List<InvStocktakeItem> rows = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getLoadStocks()) && (request.getItems() == null || request.getItems().isEmpty())) {
            List<InvStock> stocks = stockMapper.selectList(new LambdaQueryWrapper<InvStock>()
                    .eq(InvStock::getWarehouseId, request.getWarehouseId())
                    .gt(InvStock::getQtyOnHand, 0));
            for (InvStock stock : stocks) {
                InvStocktakeItem item = new InvStocktakeItem();
                item.setProductId(stock.getProductId());
                item.setBatchNo(stock.getBatchNo());
                item.setSystemQty(MoneyUtils.nz(stock.getQtyOnHand()));
                item.setActualQty(MoneyUtils.nz(stock.getQtyOnHand()));
                item.setDiffQty(MoneyUtils.zero());
                rows.add(item);
            }
        } else if (request.getItems() != null) {
            for (StocktakeRequest.Item source : request.getItems()) {
                if (source.getProductId() == null || source.getActualQty() == null) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "盘点明细的商品与实盘数量不能为空");
                }
                String batch = StringUtils.hasText(source.getBatchNo()) ? source.getBatchNo() : "";
                InvStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                        .eq(InvStock::getProductId, source.getProductId())
                        .eq(InvStock::getWarehouseId, request.getWarehouseId())
                        .eq(InvStock::getBatchNo, batch));
                BigDecimal systemQty = stock == null ? MoneyUtils.zero() : MoneyUtils.nz(stock.getQtyOnHand());
                BigDecimal actual = MoneyUtils.nz(source.getActualQty());
                InvStocktakeItem item = new InvStocktakeItem();
                item.setProductId(source.getProductId());
                item.setBatchNo(batch);
                item.setSystemQty(systemQty);
                item.setActualQty(actual);
                item.setDiffQty(actual.subtract(systemQty));
                rows.add(item);
            }
        }
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "该仓库暂无可盘点库存");
        }
        for (InvStocktakeItem item : rows) {
            item.setStocktakeId(stocktake.getId());
            itemMapper.insert(item);
        }
        return stocktake.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        getRequired(id);
        if (stocktakeMapper.updateStatus(id, "DRAFT", "SUBMITTED") == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的盘点单可提交");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        InvStocktake stocktake = getRequired(id);
        if (stocktakeMapper.updateStatus(id, "SUBMITTED", "APPROVED") == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅已提交状态的盘点单可审核");
        }
        List<InvStocktakeItem> items = itemMapper.selectList(new LambdaQueryWrapper<InvStocktakeItem>()
                .eq(InvStocktakeItem::getStocktakeId, id));
        for (InvStocktakeItem item : items) {
            BigDecimal diff = MoneyUtils.nz(item.getDiffQty());
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cost = stockService.averageCost(item.getProductId(), stocktake.getWarehouseId(), item.getBatchNo());
                stockService.increase(item.getProductId(), stocktake.getWarehouseId(), item.getBatchNo(), diff, cost,
                        "STOCKTAKE_IN", stocktake.getStocktakeNo(), "STOCKTAKE", id, "盘盈调整");
            } else {
                stockService.decrease(item.getProductId(), stocktake.getWarehouseId(), item.getBatchNo(), diff.negate(),
                        "STOCKTAKE_OUT", stocktake.getStocktakeNo(), "STOCKTAKE", id, "盘亏调整");
            }
        }
    }

    public void updateActual(Long id, Long itemId, BigDecimal actualQty) {
        InvStocktake stocktake = getRequired(id);
        if (!"DRAFT".equals(stocktake.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "仅草稿状态可修改实盘数量");
        }
        InvStocktakeItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getStocktakeId().equals(id)) {
            throw BizException.notFound("盘点明细不存在");
        }
        BigDecimal actual = MoneyUtils.nz(actualQty);
        item.setActualQty(actual);
        item.setDiffQty(actual.subtract(MoneyUtils.nz(item.getSystemQty())));
        itemMapper.updateById(item);
    }

    private InvStocktake getRequired(Long id) {
        InvStocktake stocktake = stocktakeMapper.selectById(id);
        if (stocktake == null) {
            throw BizException.notFound("盘点单不存在");
        }
        return stocktake;
    }
}

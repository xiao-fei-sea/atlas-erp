package com.erp.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.MoneyUtils;
import com.erp.finance.service.FinanceService;
import com.erp.inventory.service.StockService;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.service.WarehouseService;
import com.erp.purchase.dto.PurchaseReceiptRequest;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.entity.PurOrderItem;
import com.erp.purchase.entity.PurReceipt;
import com.erp.purchase.entity.PurReceiptItem;
import com.erp.purchase.mapper.PurOrderItemMapper;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.purchase.mapper.PurReceiptItemMapper;
import com.erp.purchase.mapper.PurReceiptMapper;
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
public class PurchaseReceiptService {

    private final PurReceiptMapper receiptMapper;
    private final PurReceiptItemMapper receiptItemMapper;
    private final PurOrderMapper orderMapper;
    private final PurOrderItemMapper orderItemMapper;
    private final MdProductMapper productMapper;
    private final WarehouseService warehouseService;
    private final StockService stockService;
    private final FinanceService financeService;
    private final SequenceService sequenceService;

    public PageResult<PurReceipt> page(long page, long size, String status, Long orderId) {
        LambdaQueryWrapper<PurReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), PurReceipt::getStatus, status)
                .eq(orderId != null, PurReceipt::getOrderId, orderId)
                .orderByDesc(PurReceipt::getId);
        return PageResult.of(receiptMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Map<String, Object> detail(Long id) {
        PurReceipt receipt = getRequired(id);
        List<PurReceiptItem> items = receiptItemMapper.selectList(new LambdaQueryWrapper<PurReceiptItem>()
                .eq(PurReceiptItem::getReceiptId, id).orderByAsc(PurReceiptItem::getId));
        List<Map<String, Object>> itemViews = new ArrayList<>();
        for (PurReceiptItem item : items) {
            MdProduct product = productMapper.selectById(item.getProductId());
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", item.getId());
            view.put("orderItemId", item.getOrderItemId());
            view.put("productId", item.getProductId());
            view.put("productName", product == null ? "" : product.getName());
            view.put("productCode", product == null ? "" : product.getCode());
            view.put("batchNo", item.getBatchNo());
            view.put("qty", item.getQty());
            view.put("price", item.getPrice());
            view.put("amount", item.getAmount());
            itemViews.add(view);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("receipt", receipt);
        data.put("items", itemViews);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseReceiptRequest request) {
        PurOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw BizException.notFound("采购订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus()) && !"PARTIAL_RECEIVED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "仅已审核或部分收货的订单可收货");
        }
        if (request.getWarehouseId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "收货仓库不能为空");
        }
        warehouseService.getRequired(request.getWarehouseId());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "收货明细不能为空");
        }
        PurReceipt receipt = new PurReceipt();
        receipt.setReceiptNo(sequenceService.next("PUR_RECEIPT"));
        receipt.setOrderId(order.getId());
        receipt.setSupplierId(order.getSupplierId());
        receipt.setWarehouseId(request.getWarehouseId());
        receipt.setReceiptDate(request.getReceiptDate() == null ? LocalDate.now() : request.getReceiptDate());
        receipt.setStatus("DRAFT");
        receipt.setRemark(request.getRemark());
        receipt.setTotalAmount(MoneyUtils.zero());
        receiptMapper.insert(receipt);

        BigDecimal total = MoneyUtils.zero();
        for (PurchaseReceiptRequest.Item source : request.getItems()) {
            PurOrderItem orderItem = orderItemMapper.selectById(source.getOrderItemId());
            if (orderItem == null || !orderItem.getOrderId().equals(order.getId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "收货明细与订单不匹配");
            }
            BigDecimal qty = MoneyUtils.nz(source.getQty());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "收货数量必须大于0");
            }
            BigDecimal remaining = MoneyUtils.nz(orderItem.getQty()).subtract(MoneyUtils.nz(orderItem.getReceivedQty()));
            if (qty.compareTo(remaining) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "收货数量超过订单剩余数量");
            }
            PurReceiptItem item = new PurReceiptItem();
            item.setReceiptId(receipt.getId());
            item.setOrderItemId(orderItem.getId());
            item.setProductId(orderItem.getProductId());
            item.setBatchNo(StringUtils.hasText(source.getBatchNo()) ? source.getBatchNo() : "");
            item.setQty(qty);
            item.setPrice(orderItem.getPrice());
            BigDecimal amount = MoneyUtils.multiply(orderItem.getPrice(), qty)
                    .add(MoneyUtils.taxAmount(MoneyUtils.multiply(orderItem.getPrice(), qty), orderItem.getTaxRate()));
            item.setAmount(amount);
            receiptItemMapper.insert(item);
            total = total.add(amount);
        }
        receipt.setTotalAmount(total);
        receiptMapper.updateById(receipt);
        return receipt.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        PurReceipt receipt = getRequired(id);
        if (receiptMapper.updateStatus(id, "DRAFT", "CONFIRMED") == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的收货单可以确认");
        }
        List<PurReceiptItem> items = receiptItemMapper.selectList(new LambdaQueryWrapper<PurReceiptItem>()
                .eq(PurReceiptItem::getReceiptId, id));
        for (PurReceiptItem item : items) {
            if (orderItemMapper.addReceivedQty(item.getOrderItemId(), item.getQty()) == 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "收货数量超过订单数量，请刷新后重试");
            }
            stockService.increase(item.getProductId(), receipt.getWarehouseId(), item.getBatchNo(), item.getQty(),
                    item.getPrice(), "PUR_RECEIPT", receipt.getReceiptNo(), "PUR_RECEIPT", receipt.getId(), "采购收货入库");
        }
        refreshOrderStatus(receipt.getOrderId());
        financeService.createPayable(receipt.getSupplierId(), "RECEIPT", receipt.getId(), receipt.getReceiptNo(),
                receipt.getTotalAmount(), receipt.getReceiptDate().plusDays(30), "采购收货生成应付");
    }

    private void refreshOrderStatus(Long orderId) {
        List<PurOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<PurOrderItem>()
                .eq(PurOrderItem::getOrderId, orderId));
        boolean allReceived = items.stream()
                .allMatch(i -> MoneyUtils.nz(i.getReceivedQty()).compareTo(MoneyUtils.nz(i.getQty())) >= 0);
        if (allReceived) {
            if (orderMapper.updateStatus(orderId, "PARTIAL_RECEIVED", "RECEIVED") == 0) {
                orderMapper.updateStatus(orderId, "APPROVED", "RECEIVED");
            }
        } else {
            orderMapper.updateStatus(orderId, "APPROVED", "PARTIAL_RECEIVED");
        }
    }

    private PurReceipt getRequired(Long id) {
        PurReceipt receipt = receiptMapper.selectById(id);
        if (receipt == null) {
            throw BizException.notFound("收货单不存在");
        }
        return receipt;
    }
}

package com.erp.sales.service;

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
import com.erp.sales.dto.SalesShipmentRequest;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.entity.SalOrderItem;
import com.erp.sales.entity.SalShipment;
import com.erp.sales.entity.SalShipmentItem;
import com.erp.sales.mapper.SalOrderItemMapper;
import com.erp.sales.mapper.SalOrderMapper;
import com.erp.sales.mapper.SalShipmentItemMapper;
import com.erp.sales.mapper.SalShipmentMapper;
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
public class SalesShipmentService {

    private final SalShipmentMapper shipmentMapper;
    private final SalShipmentItemMapper shipmentItemMapper;
    private final SalOrderMapper orderMapper;
    private final SalOrderItemMapper orderItemMapper;
    private final MdProductMapper productMapper;
    private final WarehouseService warehouseService;
    private final StockService stockService;
    private final FinanceService financeService;
    private final SequenceService sequenceService;

    public PageResult<SalShipment> page(long page, long size, String status, Long orderId) {
        LambdaQueryWrapper<SalShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), SalShipment::getStatus, status)
                .eq(orderId != null, SalShipment::getOrderId, orderId)
                .orderByDesc(SalShipment::getId);
        return PageResult.of(shipmentMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Map<String, Object> detail(Long id) {
        SalShipment shipment = getRequired(id);
        List<SalShipmentItem> items = shipmentItemMapper.selectList(new LambdaQueryWrapper<SalShipmentItem>()
                .eq(SalShipmentItem::getShipmentId, id).orderByAsc(SalShipmentItem::getId));
        List<Map<String, Object>> itemViews = new ArrayList<>();
        for (SalShipmentItem item : items) {
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
        data.put("shipment", shipment);
        data.put("items", itemViews);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SalesShipmentRequest request) {
        SalOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw BizException.notFound("销售订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus()) && !"PARTIAL_SHIPPED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "仅已审核或部分发货的订单可发货");
        }
        if (request.getWarehouseId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "发货仓库不能为空");
        }
        warehouseService.getRequired(request.getWarehouseId());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "发货明细不能为空");
        }
        SalShipment shipment = new SalShipment();
        shipment.setShipmentNo(sequenceService.next("SAL_SHIPMENT"));
        shipment.setOrderId(order.getId());
        shipment.setCustomerId(order.getCustomerId());
        shipment.setWarehouseId(request.getWarehouseId());
        shipment.setShipmentDate(request.getShipmentDate() == null ? LocalDate.now() : request.getShipmentDate());
        shipment.setStatus("DRAFT");
        shipment.setRemark(request.getRemark());
        shipment.setTotalAmount(MoneyUtils.zero());
        shipmentMapper.insert(shipment);

        BigDecimal total = MoneyUtils.zero();
        for (SalesShipmentRequest.Item source : request.getItems()) {
            SalOrderItem orderItem = orderItemMapper.selectById(source.getOrderItemId());
            if (orderItem == null || !orderItem.getOrderId().equals(order.getId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "发货明细与订单不匹配");
            }
            BigDecimal qty = MoneyUtils.nz(source.getQty());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "发货数量必须大于0");
            }
            BigDecimal remaining = MoneyUtils.nz(orderItem.getQty()).subtract(MoneyUtils.nz(orderItem.getShippedQty()));
            if (qty.compareTo(remaining) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "发货数量超过订单剩余数量");
            }
            SalShipmentItem item = new SalShipmentItem();
            item.setShipmentId(shipment.getId());
            item.setOrderItemId(orderItem.getId());
            item.setProductId(orderItem.getProductId());
            item.setBatchNo(StringUtils.hasText(source.getBatchNo()) ? source.getBatchNo() : "");
            item.setQty(qty);
            item.setPrice(orderItem.getPrice());
            BigDecimal amount = MoneyUtils.multiply(orderItem.getPrice(), qty)
                    .add(MoneyUtils.taxAmount(MoneyUtils.multiply(orderItem.getPrice(), qty), orderItem.getTaxRate()));
            item.setAmount(amount);
            shipmentItemMapper.insert(item);
            total = total.add(amount);
        }
        shipment.setTotalAmount(total);
        shipmentMapper.updateById(shipment);
        return shipment.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        SalShipment shipment = getRequired(id);
        if (shipmentMapper.updateStatus(id, "DRAFT", "CONFIRMED") == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的发货单可以确认");
        }
        List<SalShipmentItem> items = shipmentItemMapper.selectList(new LambdaQueryWrapper<SalShipmentItem>()
                .eq(SalShipmentItem::getShipmentId, id));
        for (SalShipmentItem item : items) {
            if (orderItemMapper.addShippedQty(item.getOrderItemId(), item.getQty()) == 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "发货数量超过订单数量，请刷新后重试");
            }
            stockService.decrease(item.getProductId(), shipment.getWarehouseId(), item.getBatchNo(), item.getQty(),
                    "SAL_SHIPMENT", shipment.getShipmentNo(), "SAL_SHIPMENT", shipment.getId(), "销售发货出库");
        }
        refreshOrderStatus(shipment.getOrderId());
        financeService.createReceivable(shipment.getCustomerId(), "SHIPMENT", shipment.getId(),
                shipment.getShipmentNo(), shipment.getTotalAmount(),
                shipment.getShipmentDate().plusDays(30), "销售发货生成应收");
    }

    private void refreshOrderStatus(Long orderId) {
        List<SalOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<SalOrderItem>()
                .eq(SalOrderItem::getOrderId, orderId));
        boolean allShipped = items.stream()
                .allMatch(i -> MoneyUtils.nz(i.getShippedQty()).compareTo(MoneyUtils.nz(i.getQty())) >= 0);
        if (allShipped) {
            if (orderMapper.updateStatus(orderId, "PARTIAL_SHIPPED", "SHIPPED") == 0) {
                orderMapper.updateStatus(orderId, "APPROVED", "SHIPPED");
            }
        } else {
            orderMapper.updateStatus(orderId, "APPROVED", "PARTIAL_SHIPPED");
        }
    }

    private SalShipment getRequired(Long id) {
        SalShipment shipment = shipmentMapper.selectById(id);
        if (shipment == null) {
            throw BizException.notFound("发货单不存在");
        }
        return shipment;
    }
}

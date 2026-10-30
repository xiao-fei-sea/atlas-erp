package com.erp.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.MoneyUtils;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.service.SupplierService;
import com.erp.purchase.dto.PurchaseOrderRequest;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.entity.PurOrderItem;
import com.erp.purchase.mapper.PurOrderItemMapper;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.system.service.SequenceService;
import com.erp.workflow.service.WorkflowService;
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
public class PurchaseOrderService {

    private final PurOrderMapper orderMapper;
    private final PurOrderItemMapper itemMapper;
    private final MdProductMapper productMapper;
    private final SupplierService supplierService;
    private final SequenceService sequenceService;
    private final WorkflowService workflowService;

    public PageResult<PurOrder> page(long page, long size, String status, Long supplierId, String keyword) {
        LambdaQueryWrapper<PurOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), PurOrder::getStatus, status)
                .eq(supplierId != null, PurOrder::getSupplierId, supplierId)
                .like(StringUtils.hasText(keyword), PurOrder::getOrderNo, keyword)
                .orderByDesc(PurOrder::getId);
        return PageResult.of(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Map<String, Object> detail(Long id) {
        PurOrder order = getRequired(id);
        List<PurOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<PurOrderItem>()
                .eq(PurOrderItem::getOrderId, id).orderByAsc(PurOrderItem::getId));
        List<Map<String, Object>> itemViews = new ArrayList<>();
        for (PurOrderItem item : items) {
            MdProduct product = productMapper.selectById(item.getProductId());
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", item.getId());
            view.put("productId", item.getProductId());
            view.put("productCode", product == null ? "" : product.getCode());
            view.put("productName", product == null ? "" : product.getName());
            view.put("spec", product == null ? "" : product.getSpec());
            view.put("qty", item.getQty());
            view.put("receivedQty", item.getReceivedQty());
            view.put("remainingQty", MoneyUtils.nz(item.getQty()).subtract(MoneyUtils.nz(item.getReceivedQty())));
            view.put("price", item.getPrice());
            view.put("taxRate", item.getTaxRate());
            view.put("amount", item.getAmount());
            itemViews.add(view);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order", order);
        data.put("items", itemViews);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseOrderRequest request) {
        validate(request);
        supplierService.getRequired(request.getSupplierId());
        PurOrder order = new PurOrder();
        order.setOrderNo(sequenceService.next("PUR_ORDER"));
        order.setSupplierId(request.getSupplierId());
        order.setOrderDate(request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate());
        order.setStatus("DRAFT");
        order.setDiscountAmount(MoneyUtils.zero());
        order.setRemark(request.getRemark());
        orderMapper.insert(order);
        BigDecimal total = saveItems(order.getId(), request.getItems());
        order.setTotalAmount(total);
        order.setTaxAmount(taxOf(request.getItems()));
        orderMapper.updateById(order);
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PurchaseOrderRequest request) {
        PurOrder order = getRequired(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "仅草稿状态的采购订单可以修改");
        }
        validate(request);
        supplierService.getRequired(request.getSupplierId());
        order.setSupplierId(request.getSupplierId());
        order.setOrderDate(request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate());
        order.setRemark(request.getRemark());
        itemMapper.delete(new LambdaQueryWrapper<PurOrderItem>().eq(PurOrderItem::getOrderId, id));
        order.setTotalAmount(saveItems(id, request.getItems()));
        order.setTaxAmount(taxOf(request.getItems()));
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PurOrder order = getRequired(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "仅草稿状态的采购订单可以删除");
        }
        itemMapper.delete(new LambdaQueryWrapper<PurOrderItem>().eq(PurOrderItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        PurOrder order = getRequired(id);
        if (orderMapper.updateStatus(id, "DRAFT", "PENDING") == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的采购订单可以提交");
        }
        Long instanceId = workflowService.submit("PUR_ORDER", id, order.getOrderNo(), order.getTotalAmount());
        orderMapper.bindApproval(id, instanceId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        getRequired(id);
        if (orderMapper.updateStatus(id, "DRAFT", "CANCELLED") == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的采购订单可以取消");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyApproval(Long orderId, String result) {
        orderMapper.updateStatus(orderId, "PENDING", result);
    }

    private BigDecimal saveItems(Long orderId, List<PurchaseOrderRequest.Item> items) {
        BigDecimal total = MoneyUtils.zero();
        for (PurchaseOrderRequest.Item source : items) {
            MdProduct product = productMapper.selectById(source.getProductId());
            if (product == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "商品不存在");
            }
            BigDecimal price = MoneyUtils.nz(source.getPrice());
            BigDecimal qty = MoneyUtils.nz(source.getQty());
            PurOrderItem item = new PurOrderItem();
            item.setOrderId(orderId);
            item.setProductId(source.getProductId());
            item.setQty(qty);
            item.setReceivedQty(MoneyUtils.zero());
            item.setPrice(price);
            item.setTaxRate(MoneyUtils.nz(source.getTaxRate()));
            item.setAmount(MoneyUtils.multiply(price, qty));
            itemMapper.insert(item);
            total = total.add(item.getAmount());
        }
        return total.setScale(MoneyUtils.SCALE);
    }

    private BigDecimal taxOf(List<PurchaseOrderRequest.Item> items) {
        BigDecimal tax = MoneyUtils.zero();
        for (PurchaseOrderRequest.Item source : items) {
            tax = tax.add(MoneyUtils.taxAmount(MoneyUtils.multiply(source.getPrice(), source.getQty()), source.getTaxRate()));
        }
        return tax;
    }

    private void validate(PurchaseOrderRequest request) {
        if (request.getSupplierId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "供应商不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "采购明细不能为空");
        }
        for (PurchaseOrderRequest.Item item : request.getItems()) {
            if (item.getProductId() == null || item.getQty() == null
                    || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "商品的采购数量必须大于0");
            }
        }
    }

    public PurOrder getRequired(Long id) {
        PurOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("采购订单不存在");
        }
        return order;
    }
}

package com.erp.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.MoneyUtils;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.service.CustomerService;
import com.erp.sales.dto.SalesOrderRequest;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.entity.SalOrderItem;
import com.erp.sales.mapper.SalOrderItemMapper;
import com.erp.sales.mapper.SalOrderMapper;
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
public class SalesOrderService {

    private final SalOrderMapper orderMapper;
    private final SalOrderItemMapper itemMapper;
    private final MdProductMapper productMapper;
    private final CustomerService customerService;
    private final SequenceService sequenceService;
    private final WorkflowService workflowService;

    public PageResult<SalOrder> page(long page, long size, String status, Long customerId, String keyword) {
        LambdaQueryWrapper<SalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), SalOrder::getStatus, status)
                .eq(customerId != null, SalOrder::getCustomerId, customerId)
                .like(StringUtils.hasText(keyword), SalOrder::getOrderNo, keyword)
                .orderByDesc(SalOrder::getId);
        return PageResult.of(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Map<String, Object> detail(Long id) {
        SalOrder order = getRequired(id);
        List<SalOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<SalOrderItem>()
                .eq(SalOrderItem::getOrderId, id).orderByAsc(SalOrderItem::getId));
        List<Map<String, Object>> itemViews = new ArrayList<>();
        for (SalOrderItem item : items) {
            MdProduct product = productMapper.selectById(item.getProductId());
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", item.getId());
            view.put("productId", item.getProductId());
            view.put("productCode", product == null ? "" : product.getCode());
            view.put("productName", product == null ? "" : product.getName());
            view.put("spec", product == null ? "" : product.getSpec());
            view.put("qty", item.getQty());
            view.put("shippedQty", item.getShippedQty());
            view.put("remainingQty", MoneyUtils.nz(item.getQty()).subtract(MoneyUtils.nz(item.getShippedQty())));
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
    public Long create(SalesOrderRequest request) {
        validate(request);
        customerService.getRequired(request.getCustomerId());
        SalOrder order = new SalOrder();
        order.setOrderNo(sequenceService.next("SAL_ORDER"));
        order.setCustomerId(request.getCustomerId());
        order.setOrderDate(request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate());
        order.setStatus("DRAFT");
        order.setDiscountAmount(MoneyUtils.zero());
        order.setRemark(request.getRemark());
        orderMapper.insert(order);
        order.setTotalAmount(saveItems(order.getId(), request.getItems()));
        order.setTaxAmount(taxOf(request.getItems()));
        orderMapper.updateById(order);
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SalesOrderRequest request) {
        SalOrder order = getRequired(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "仅草稿状态的销售订单可以修改");
        }
        validate(request);
        customerService.getRequired(request.getCustomerId());
        order.setCustomerId(request.getCustomerId());
        order.setOrderDate(request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate());
        order.setRemark(request.getRemark());
        itemMapper.delete(new LambdaQueryWrapper<SalOrderItem>().eq(SalOrderItem::getOrderId, id));
        order.setTotalAmount(saveItems(id, request.getItems()));
        order.setTaxAmount(taxOf(request.getItems()));
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalOrder order = getRequired(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "仅草稿状态的销售订单可以删除");
        }
        itemMapper.delete(new LambdaQueryWrapper<SalOrderItem>().eq(SalOrderItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        SalOrder order = getRequired(id);
        if (orderMapper.updateStatus(id, "DRAFT", "PENDING") == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的销售订单可以提交");
        }
        Long instanceId = workflowService.submit("SAL_ORDER", id, order.getOrderNo(), order.getTotalAmount());
        orderMapper.bindApproval(id, instanceId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        getRequired(id);
        if (orderMapper.updateStatus(id, "DRAFT", "CANCELLED") == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿状态的销售订单可以取消");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyApproval(Long orderId, String result) {
        orderMapper.updateStatus(orderId, "PENDING", result);
    }

    private BigDecimal saveItems(Long orderId, List<SalesOrderRequest.Item> items) {
        BigDecimal total = MoneyUtils.zero();
        for (SalesOrderRequest.Item source : items) {
            MdProduct product = productMapper.selectById(source.getProductId());
            if (product == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "商品不存在");
            }
            BigDecimal price = MoneyUtils.nz(source.getPrice());
            BigDecimal qty = MoneyUtils.nz(source.getQty());
            SalOrderItem item = new SalOrderItem();
            item.setOrderId(orderId);
            item.setProductId(source.getProductId());
            item.setQty(qty);
            item.setShippedQty(MoneyUtils.zero());
            item.setPrice(price);
            item.setTaxRate(MoneyUtils.nz(source.getTaxRate()));
            item.setAmount(MoneyUtils.multiply(price, qty));
            itemMapper.insert(item);
            total = total.add(item.getAmount());
        }
        return total.setScale(MoneyUtils.SCALE);
    }

    private BigDecimal taxOf(List<SalesOrderRequest.Item> items) {
        BigDecimal tax = MoneyUtils.zero();
        for (SalesOrderRequest.Item source : items) {
            tax = tax.add(MoneyUtils.taxAmount(MoneyUtils.multiply(source.getPrice(), source.getQty()), source.getTaxRate()));
        }
        return tax;
    }

    private void validate(SalesOrderRequest request) {
        if (request.getCustomerId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "客户不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "销售明细不能为空");
        }
        for (SalesOrderRequest.Item item : request.getItems()) {
            if (item.getProductId() == null || item.getQty() == null
                    || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "商品的销售数量必须大于0");
            }
        }
    }

    public SalOrder getRequired(Long id) {
        SalOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("销售订单不存在");
        }
        return order;
    }
}

package com.erp.purchase.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.purchase.dto.PurchaseOrderRequest;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/purchase/orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:order:view')")
    public Result<PageResult<PurOrder>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long supplierId,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(purchaseOrderService.page(page, size, status, supplierId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:order:view')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(purchaseOrderService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:order:create')")
    @OperLog(module = "采购管理", action = "新增采购订单")
    public Result<Long> create(@RequestBody PurchaseOrderRequest request) {
        return Result.ok(purchaseOrderService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:order:update')")
    @OperLog(module = "采购管理", action = "修改采购订单")
    public Result<Void> update(@PathVariable Long id, @RequestBody PurchaseOrderRequest request) {
        purchaseOrderService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:order:update')")
    @OperLog(module = "采购管理", action = "删除采购订单")
    public Result<Void> delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('purchase:order:submit')")
    @OperLog(module = "采购管理", action = "提交采购订单")
    public Result<Void> submit(@PathVariable Long id) {
        purchaseOrderService.submit(id);
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('purchase:order:submit')")
    @OperLog(module = "采购管理", action = "取消采购订单")
    public Result<Void> cancel(@PathVariable Long id) {
        purchaseOrderService.cancel(id);
        return Result.ok();
    }
}

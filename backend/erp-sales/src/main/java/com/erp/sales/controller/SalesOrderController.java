package com.erp.sales.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.sales.dto.SalesOrderRequest;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sales/orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('sales:order:view')")
    public Result<PageResult<SalOrder>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long customerId,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(salesOrderService.page(page, size, status, customerId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:order:view')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(salesOrderService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:order:create')")
    @OperLog(module = "销售管理", action = "新增销售订单")
    public Result<Long> create(@RequestBody SalesOrderRequest request) {
        return Result.ok(salesOrderService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:order:update')")
    @OperLog(module = "销售管理", action = "修改销售订单")
    public Result<Void> update(@PathVariable Long id, @RequestBody SalesOrderRequest request) {
        salesOrderService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:order:update')")
    @OperLog(module = "销售管理", action = "删除销售订单")
    public Result<Void> delete(@PathVariable Long id) {
        salesOrderService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('sales:order:submit')")
    @OperLog(module = "销售管理", action = "提交销售订单")
    public Result<Void> submit(@PathVariable Long id) {
        salesOrderService.submit(id);
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:order:submit')")
    @OperLog(module = "销售管理", action = "取消销售订单")
    public Result<Void> cancel(@PathVariable Long id) {
        salesOrderService.cancel(id);
        return Result.ok();
    }
}

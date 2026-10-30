package com.erp.sales.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.sales.dto.SalesShipmentRequest;
import com.erp.sales.entity.SalShipment;
import com.erp.sales.service.SalesShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sales/shipments")
@RequiredArgsConstructor
public class SalesShipmentController {

    private final SalesShipmentService salesShipmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('sales:shipment:view')")
    public Result<PageResult<SalShipment>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) Long orderId) {
        return Result.ok(salesShipmentService.page(page, size, status, orderId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:shipment:view')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(salesShipmentService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:shipment:create')")
    @OperLog(module = "销售管理", action = "新增发货单")
    public Result<Long> create(@RequestBody SalesShipmentRequest request) {
        return Result.ok(salesShipmentService.create(request));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('sales:shipment:confirm')")
    @OperLog(module = "销售管理", action = "确认发货")
    public Result<Void> confirm(@PathVariable Long id) {
        salesShipmentService.confirm(id);
        return Result.ok();
    }
}

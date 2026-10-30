package com.erp.inventory.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.inventory.dto.TransferRequest;
import com.erp.inventory.entity.InvTransfer;
import com.erp.inventory.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:transfer:view')")
    public Result<PageResult<InvTransfer>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) Long warehouseId) {
        return Result.ok(transferService.page(page, size, status, warehouseId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:transfer:view')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(transferService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:transfer:create')")
    @OperLog(module = "库存", action = "新增调拨单")
    public Result<Long> create(@RequestBody TransferRequest request) {
        return Result.ok(transferService.create(request));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('inventory:transfer:confirm')")
    @OperLog(module = "库存", action = "确认调拨")
    public Result<Void> confirm(@PathVariable Long id) {
        transferService.confirm(id);
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inventory:transfer:create')")
    @OperLog(module = "库存", action = "取消调拨")
    public Result<Void> cancel(@PathVariable Long id) {
        transferService.cancel(id);
        return Result.ok();
    }
}

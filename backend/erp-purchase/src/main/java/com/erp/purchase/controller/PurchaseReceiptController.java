package com.erp.purchase.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.purchase.dto.PurchaseReceiptRequest;
import com.erp.purchase.entity.PurReceipt;
import com.erp.purchase.service.PurchaseReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/purchase/receipts")
@RequiredArgsConstructor
public class PurchaseReceiptController {

    private final PurchaseReceiptService purchaseReceiptService;

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:receipt:view')")
    public Result<PageResult<PurReceipt>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long orderId) {
        return Result.ok(purchaseReceiptService.page(page, size, status, orderId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:receipt:view')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(purchaseReceiptService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:receipt:create')")
    @OperLog(module = "采购管理", action = "新增收货单")
    public Result<Long> create(@RequestBody PurchaseReceiptRequest request) {
        return Result.ok(purchaseReceiptService.create(request));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('purchase:receipt:confirm')")
    @OperLog(module = "采购管理", action = "确认收货")
    public Result<Void> confirm(@PathVariable Long id) {
        purchaseReceiptService.confirm(id);
        return Result.ok();
    }
}

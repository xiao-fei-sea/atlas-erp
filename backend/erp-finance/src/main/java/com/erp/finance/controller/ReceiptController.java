package com.erp.finance.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.finance.dto.ReceiptRequest;
import com.erp.finance.entity.FinReceipt;
import com.erp.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final FinanceService financeService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:receipt:view')")
    public Result<PageResult<FinReceipt>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long customerId) {
        return Result.ok(financeService.pageReceipts(page, size, customerId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:receipt:create')")
    @OperLog(module = "财务管理", action = "新增收款单")
    public Result<Long> create(@RequestBody ReceiptRequest request) {
        return Result.ok(financeService.createReceipt(request));
    }

    @PutMapping("/{id}/writeoff")
    @PreAuthorize("hasAuthority('finance:receipt:writeoff')")
    @OperLog(module = "财务管理", action = "收款核销")
    public Result<Void> writeoff(@PathVariable Long id, @RequestBody ReceiptRequest request) {
        financeService.writeoffReceipt(id, request.getAllocations());
        return Result.ok();
    }
}

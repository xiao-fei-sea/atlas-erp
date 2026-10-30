package com.erp.finance.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.finance.dto.PaymentRequest;
import com.erp.finance.entity.FinPayment;
import com.erp.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final FinanceService financeService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:payment:view')")
    public Result<PageResult<FinPayment>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long supplierId) {
        return Result.ok(financeService.pagePayments(page, size, supplierId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:payment:create')")
    @OperLog(module = "财务管理", action = "新增付款单")
    public Result<Long> create(@RequestBody PaymentRequest request) {
        return Result.ok(financeService.createPayment(request));
    }

    @PutMapping("/{id}/writeoff")
    @PreAuthorize("hasAuthority('finance:payment:writeoff')")
    @OperLog(module = "财务管理", action = "付款核销")
    public Result<Void> writeoff(@PathVariable Long id, @RequestBody PaymentRequest request) {
        financeService.writeoffPayment(id, request.getAllocations());
        return Result.ok();
    }
}

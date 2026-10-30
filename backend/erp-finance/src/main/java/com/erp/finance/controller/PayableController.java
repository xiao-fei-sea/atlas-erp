package com.erp.finance.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.finance.dto.AgingRow;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/payables")
@RequiredArgsConstructor
public class PayableController {

    private final FinanceService financeService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:payable:view')")
    public Result<PageResult<FinPayable>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long supplierId,
                                               @RequestParam(required = false) String status) {
        return Result.ok(financeService.pagePayables(page, size, supplierId, status));
    }

    @GetMapping("/open")
    @PreAuthorize("hasAuthority('finance:payable:view')")
    public Result<List<Map<String, Object>>> open(@RequestParam(required = false) Long supplierId) {
        return Result.ok(financeService.openPayables(supplierId));
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('finance:payable:view')")
    public Result<List<AgingRow>> aging() {
        return Result.ok(financeService.payableAging());
    }
}

package com.erp.finance.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.finance.dto.AgingRow;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/receivables")
@RequiredArgsConstructor
public class ReceivableController {

    private final FinanceService financeService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:receivable:view')")
    public Result<PageResult<FinReceivable>> page(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) Long customerId,
                                                  @RequestParam(required = false) String status) {
        return Result.ok(financeService.pageReceivables(page, size, customerId, status));
    }

    @GetMapping("/open")
    @PreAuthorize("hasAuthority('finance:receivable:view')")
    public Result<List<Map<String, Object>>> open(@RequestParam(required = false) Long customerId) {
        return Result.ok(financeService.openReceivables(customerId));
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('finance:receivable:view')")
    public Result<List<AgingRow>> aging() {
        return Result.ok(financeService.receivableAging());
    }
}

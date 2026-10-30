package com.erp.finance.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.finance.entity.FinAccount;
import com.erp.finance.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:account:view')")
    public Result<PageResult<FinAccount>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword) {
        return Result.ok(accountService.page(page, size, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('finance:account:view')")
    public Result<List<FinAccount>> options() {
        return Result.ok(accountService.listEnabled());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:account:create')")
    @OperLog(module = "财务管理", action = "新增资金账户")
    public Result<Long> create(@RequestBody FinAccount account) {
        return Result.ok(accountService.create(account));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:account:update')")
    @OperLog(module = "财务管理", action = "修改资金账户")
    public Result<Void> update(@PathVariable Long id, @RequestBody FinAccount account) {
        accountService.update(id, account);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:account:update')")
    @OperLog(module = "财务管理", action = "删除资金账户")
    public Result<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return Result.ok();
    }
}

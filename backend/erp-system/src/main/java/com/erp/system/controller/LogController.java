package com.erp.system.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.system.entity.SysLoginLog;
import com.erp.system.entity.SysOperLog;
import com.erp.system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping("/login")
    @PreAuthorize("hasAuthority('system:log:view')")
    public Result<PageResult<SysLoginLog>> login(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String username) {
        return Result.ok(logService.pageLogin(page, size, username));
    }

    @GetMapping("/oper")
    @PreAuthorize("hasAuthority('system:log:view')")
    public Result<PageResult<SysOperLog>> oper(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String module,
                                               @RequestParam(required = false) String operator) {
        return Result.ok(logService.pageOper(page, size, module, operator));
    }
}

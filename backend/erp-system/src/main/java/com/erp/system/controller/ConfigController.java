package com.erp.system.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.Result;
import com.erp.system.entity.SysConfig;
import com.erp.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:config:view')")
    public Result<List<SysConfig>> list(@RequestParam(required = false) String groupCode) {
        return Result.ok(configService.list(groupCode));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:config:update')")
    @OperLog(module = "系统管理", action = "保存参数")
    public Result<Void> save(@RequestBody SysConfig config) {
        configService.save(config);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:update')")
    @OperLog(module = "系统管理", action = "删除参数")
    public Result<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return Result.ok();
    }
}

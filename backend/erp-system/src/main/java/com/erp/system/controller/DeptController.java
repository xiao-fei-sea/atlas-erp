package com.erp.system.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.Result;
import com.erp.system.entity.SysDept;
import com.erp.system.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(deptService.tree());
    }

    @GetMapping("/list")
    public Result<List<SysDept>> list() {
        return Result.ok(deptService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:create')")
    @OperLog(module = "系统管理", action = "新增部门")
    public Result<Long> create(@RequestBody SysDept dept) {
        return Result.ok(deptService.create(dept));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:update')")
    @OperLog(module = "系统管理", action = "修改部门")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysDept dept) {
        deptService.update(id, dept);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    @OperLog(module = "系统管理", action = "删除部门")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}

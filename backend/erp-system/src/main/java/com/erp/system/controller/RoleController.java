package com.erp.system.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.system.dto.RoleSaveRequest;
import com.erp.system.entity.SysRole;
import com.erp.system.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(roleService.page(page, size, keyword, status));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.listAll());
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<List<Long>> menus(@PathVariable Long id) {
        return Result.ok(roleService.listMenuIds(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:create')")
    @OperLog(module = "系统管理", action = "新增角色")
    public Result<Long> create(@Valid @RequestBody RoleSaveRequest request) {
        return Result.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    @OperLog(module = "系统管理", action = "修改角色")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveRequest request) {
        roleService.update(id, request);
        return Result.ok();
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assign')")
    @OperLog(module = "系统管理", action = "分配角色权限")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignMenus(id, body.get("menuIds"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @OperLog(module = "系统管理", action = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}

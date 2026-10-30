package com.erp.system.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.Result;
import com.erp.system.dto.MenuNode;
import com.erp.system.entity.SysMenu;
import com.erp.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public Result<List<MenuNode>> tree() {
        return Result.ok(menuService.treeOfCurrentUser());
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:menu:view')")
    public Result<List<SysMenu>> list() {
        return Result.ok(menuService.listAll());
    }

    @GetMapping("/tree-all")
    @PreAuthorize("hasAuthority('system:menu:view')")
    public Result<List<MenuNode>> treeAll() {
        return Result.ok(menuService.treeAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:create')")
    @OperLog(module = "系统管理", action = "新增菜单")
    public Result<Long> create(@RequestBody SysMenu menu) {
        return Result.ok(menuService.create(menu));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:update')")
    @OperLog(module = "系统管理", action = "修改菜单")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menuService.update(id, menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    @OperLog(module = "系统管理", action = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}

package com.erp.system.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.Result;
import com.erp.system.entity.SysDictItem;
import com.erp.system.entity.SysDictType;
import com.erp.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/types")
    public Result<List<SysDictType>> types() {
        return Result.ok(dictService.listTypes());
    }

    @PostMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:create')")
    @OperLog(module = "系统管理", action = "新增字典")
    public Result<Long> createType(@RequestBody SysDictType type) {
        return Result.ok(dictService.createType(type));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    @OperLog(module = "系统管理", action = "修改字典")
    public Result<Void> updateType(@PathVariable Long id, @RequestBody SysDictType type) {
        dictService.updateType(id, type);
        return Result.ok();
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    @OperLog(module = "系统管理", action = "删除字典")
    public Result<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.ok();
    }

    @GetMapping("/types/{id}/items")
    public Result<List<SysDictItem>> items(@PathVariable Long id) {
        return Result.ok(dictService.listItems(id));
    }

    @GetMapping("/types/code/{code}/items")
    public Result<List<SysDictItem>> itemsByCode(@PathVariable String code) {
        return Result.ok(dictService.listItemsByCode(code));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('system:dict:create')")
    public Result<Long> createItem(@RequestBody SysDictItem item) {
        return Result.ok(dictService.createItem(item));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    public Result<Void> updateItem(@PathVariable Long id, @RequestBody SysDictItem item) {
        dictService.updateItem(id, item);
        return Result.ok();
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.ok();
    }
}

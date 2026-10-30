package com.erp.masterdata.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/masterdata/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    @PreAuthorize("hasAuthority('masterdata:unit:view')")
    public Result<PageResult<MdUnit>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(unitService.page(page, size, keyword, status));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('masterdata:unit:view')")
    public Result<List<MdUnit>> options() {
        return Result.ok(unitService.listEnabled());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('masterdata:unit:create')")
    @OperLog(module = "基础资料", action = "新增计量单位")
    public Result<Long> create(@RequestBody MdUnit unit) {
        return Result.ok(unitService.create(unit));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:unit:update')")
    @OperLog(module = "基础资料", action = "修改计量单位")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdUnit unit) {
        unitService.update(id, unit);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:unit:delete')")
    @OperLog(module = "基础资料", action = "删除计量单位")
    public Result<Void> delete(@PathVariable Long id) {
        unitService.delete(id);
        return Result.ok();
    }
}

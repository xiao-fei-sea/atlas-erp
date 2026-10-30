package com.erp.masterdata.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masterdata/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('masterdata:supplier:view')")
    public Result<PageResult<MdSupplier>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(supplierService.page(page, size, keyword, status));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('masterdata:supplier:view')")
    public Result<List<Map<String, Object>>> options() {
        List<Map<String, Object>> data = supplierService.listEnabled().stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "code", s.getCode(), "name", s.getName()))
                .toList();
        return Result.ok(data);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:supplier:view')")
    public Result<MdSupplier> detail(@PathVariable Long id) {
        return Result.ok(supplierService.getRequired(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('masterdata:supplier:create')")
    @OperLog(module = "基础资料", action = "新增供应商")
    public Result<Long> create(@RequestBody MdSupplier supplier) {
        return Result.ok(supplierService.create(supplier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:supplier:update')")
    @OperLog(module = "基础资料", action = "修改供应商")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdSupplier supplier) {
        supplierService.update(id, supplier);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:supplier:delete')")
    @OperLog(module = "基础资料", action = "删除供应商")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return Result.ok();
    }
}

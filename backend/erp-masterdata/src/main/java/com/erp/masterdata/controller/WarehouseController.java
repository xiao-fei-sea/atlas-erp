package com.erp.masterdata.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.masterdata.entity.MdLocation;
import com.erp.masterdata.entity.MdWarehouse;
import com.erp.masterdata.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masterdata/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("hasAuthority('masterdata:warehouse:view')")
    public Result<PageResult<MdWarehouse>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(warehouseService.page(page, size, keyword, status));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('masterdata:warehouse:view')")
    public Result<List<Map<String, Object>>> options() {
        List<Map<String, Object>> data = warehouseService.listEnabled().stream()
                .map(w -> Map.<String, Object>of("id", w.getId(), "code", w.getCode(), "name", w.getName()))
                .toList();
        return Result.ok(data);
    }

    @GetMapping("/{id}/locations")
    @PreAuthorize("hasAuthority('masterdata:warehouse:view')")
    public Result<List<MdLocation>> locations(@PathVariable Long id) {
        return Result.ok(warehouseService.listLocations(id));
    }

    @GetMapping("/locations")
    @PreAuthorize("hasAuthority('masterdata:warehouse:view')")
    public Result<List<MdLocation>> allLocations() {
        return Result.ok(warehouseService.listLocations(null));
    }

    @PostMapping("/{id}/locations")
    @PreAuthorize("hasAuthority('masterdata:warehouse:update')")
    @OperLog(module = "基础资料", action = "新增库位")
    public Result<Long> createLocation(@PathVariable Long id, @RequestBody MdLocation location) {
        return Result.ok(warehouseService.createLocation(id, location));
    }

    @DeleteMapping("/{id}/locations/{locationId}")
    @PreAuthorize("hasAuthority('masterdata:warehouse:update')")
    @OperLog(module = "基础资料", action = "删除库位")
    public Result<Void> deleteLocation(@PathVariable Long id, @PathVariable Long locationId) {
        warehouseService.deleteLocation(id, locationId);
        return Result.ok();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('masterdata:warehouse:create')")
    @OperLog(module = "基础资料", action = "新增仓库")
    public Result<Long> create(@RequestBody MdWarehouse warehouse) {
        return Result.ok(warehouseService.create(warehouse));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:warehouse:update')")
    @OperLog(module = "基础资料", action = "修改仓库")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdWarehouse warehouse) {
        warehouseService.update(id, warehouse);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:warehouse:delete')")
    @OperLog(module = "基础资料", action = "删除仓库")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.ok();
    }
}

package com.erp.inventory.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.inventory.dto.StocktakeRequest;
import com.erp.inventory.entity.InvStocktake;
import com.erp.inventory.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:stocktake:view')")
    public Result<PageResult<InvStocktake>> page(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Long warehouseId) {
        return Result.ok(stocktakeService.page(page, size, status, warehouseId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:stocktake:view')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(stocktakeService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:stocktake:create')")
    @OperLog(module = "库存", action = "新增盘点单")
    public Result<Long> create(@RequestBody StocktakeRequest request) {
        return Result.ok(stocktakeService.create(request));
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAuthority('inventory:stocktake:create')")
    @OperLog(module = "库存", action = "录入实盘数量")
    public Result<Void> updateActual(@PathVariable Long id, @PathVariable Long itemId,
                                     @RequestBody Map<String, Object> body) {
        stocktakeService.updateActual(id, itemId, new BigDecimal(String.valueOf(body.get("actualQty"))));
        return Result.ok();
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('inventory:stocktake:submit')")
    @OperLog(module = "库存", action = "提交盘点单")
    public Result<Void> submit(@PathVariable Long id) {
        stocktakeService.submit(id);
        return Result.ok();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:stocktake:approve')")
    @OperLog(module = "库存", action = "审核盘点单")
    public Result<Void> approve(@PathVariable Long id) {
        stocktakeService.approve(id);
        return Result.ok();
    }
}

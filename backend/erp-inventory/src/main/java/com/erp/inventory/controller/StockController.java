package com.erp.inventory.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.inventory.dto.FlowView;
import com.erp.inventory.dto.StockChangeItem;
import com.erp.inventory.dto.StockChangeRequest;
import com.erp.inventory.dto.StockView;
import com.erp.inventory.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/stocks")
    @PreAuthorize("hasAuthority('inventory:stock:view')")
    public Result<PageResult<StockView>> stocks(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long warehouseId,
                                                @RequestParam(defaultValue = "false") boolean onlyAvailable) {
        return Result.ok(stockService.pageStocks(page, size, keyword, warehouseId, onlyAvailable));
    }

    @GetMapping("/stocks/warnings")
    @PreAuthorize("hasAuthority('inventory:stock:view')")
    public Result<List<StockView>> warnings() {
        return Result.ok(stockService.warnings());
    }

    @GetMapping("/replenishment")
    @PreAuthorize("hasAuthority('inventory:stock:view')")
    public Result<List<StockView>> replenishment() {
        return Result.ok(stockService.warnings());
    }

    @GetMapping("/flows")
    @PreAuthorize("hasAuthority('inventory:flow:view')")
    public Result<PageResult<FlowView>> flows(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Long productId,
                                              @RequestParam(required = false) Long warehouseId,
                                              @RequestParam(required = false) String bizType,
                                              @RequestParam(required = false) String bizNo) {
        return Result.ok(stockService.pageFlows(page, size, productId, warehouseId, bizType, bizNo));
    }

    @PostMapping("/inbound")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    @OperLog(module = "库存", action = "其他入库")
    public Result<Void> inbound(@RequestBody StockChangeRequest request) {
        validate(request);
        for (StockChangeItem item : request.getItems()) {
            stockService.increase(item.getProductId(), request.getWarehouseId(), item.getBatchNo(), item.getQty(),
                    item.getUnitCost(), "INBOUND", request.getBizNo(), "MANUAL", null,
                    item.getRemark() == null ? request.getRemark() : item.getRemark());
        }
        return Result.ok();
    }

    @PostMapping("/outbound")
    @PreAuthorize("hasAuthority('inventory:outbound')")
    @OperLog(module = "库存", action = "其他出库")
    public Result<Void> outbound(@RequestBody StockChangeRequest request) {
        validate(request);
        for (StockChangeItem item : request.getItems()) {
            stockService.decrease(item.getProductId(), request.getWarehouseId(), item.getBatchNo(), item.getQty(),
                    "OUTBOUND", request.getBizNo(), "MANUAL", null,
                    item.getRemark() == null ? request.getRemark() : item.getRemark());
        }
        return Result.ok();
    }

    @PutMapping("/stocks/freeze")
    @PreAuthorize("hasAuthority('inventory:stock:freeze')")
    @OperLog(module = "库存", action = "冻结库存")
    public Result<Void> freeze(@RequestBody Map<String, Object> body) {
        applyFreeze(body, true);
        return Result.ok();
    }

    @PutMapping("/stocks/release")
    @PreAuthorize("hasAuthority('inventory:stock:freeze')")
    @OperLog(module = "库存", action = "解冻库存")
    public Result<Void> release(@RequestBody Map<String, Object> body) {
        applyFreeze(body, false);
        return Result.ok();
    }

    private void applyFreeze(Map<String, Object> body, boolean freeze) {
        Long productId = toLong(body.get("productId"));
        Long warehouseId = toLong(body.get("warehouseId"));
        BigDecimal qty = toDecimal(body.get("qty"));
        if (productId == null || warehouseId == null || qty == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品、仓库与数量不能为空");
        }
        String batchNo = body.get("batchNo") == null ? "" : String.valueOf(body.get("batchNo"));
        stockService.freeze(productId, warehouseId, batchNo, qty, freeze);
    }

    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }

    private void validate(StockChangeRequest request) {
        if (request.getWarehouseId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仓库不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "明细不能为空");
        }
    }
}

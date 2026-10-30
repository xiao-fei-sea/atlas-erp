package com.erp.masterdata.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masterdata/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('masterdata:product:view')")
    public Result<PageResult<MdProduct>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(productService.page(page, size, keyword, categoryId, status));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('masterdata:product:view')")
    public Result<List<Map<String, Object>>> options() {
        List<Map<String, Object>> data = productService.listEnabled().stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "code", p.getCode(),
                        "name", p.getName(),
                        "spec", p.getSpec() == null ? "" : p.getSpec(),
                        "unitId", p.getUnitId() == null ? 0L : p.getUnitId(),
                        "taxRate", p.getTaxRate(),
                        "purchasePrice", p.getPurchasePrice(),
                        "salePrice", p.getSalePrice()))
                .toList();
        return Result.ok(data);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('masterdata:product:export')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) Integer status) {
        byte[] content = productService.exportCsv(keyword, categoryId, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.csv")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(content);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:product:view')")
    public Result<MdProduct> detail(@PathVariable Long id) {
        return Result.ok(productService.getRequired(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('masterdata:product:create')")
    @OperLog(module = "基础资料", action = "新增商品")
    public Result<Long> create(@RequestBody MdProduct product) {
        return Result.ok(productService.create(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:product:update')")
    @OperLog(module = "基础资料", action = "修改商品")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdProduct product) {
        productService.update(id, product);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:product:delete')")
    @OperLog(module = "基础资料", action = "删除商品")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}

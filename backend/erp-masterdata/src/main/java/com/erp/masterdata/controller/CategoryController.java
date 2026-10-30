package com.erp.masterdata.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.masterdata.dto.CategoryNode;
import com.erp.masterdata.entity.MdCategory;
import com.erp.masterdata.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/masterdata/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('masterdata:category:view')")
    public Result<List<CategoryNode>> tree() {
        return Result.ok(categoryService.tree());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('masterdata:category:view')")
    public Result<PageResult<MdCategory>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(categoryService.page(page, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('masterdata:category:create')")
    @OperLog(module = "基础资料", action = "新增商品分类")
    public Result<Long> create(@RequestBody MdCategory category) {
        return Result.ok(categoryService.create(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:category:update')")
    @OperLog(module = "基础资料", action = "修改商品分类")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdCategory category) {
        categoryService.update(id, category);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:category:delete')")
    @OperLog(module = "基础资料", action = "删除商品分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}

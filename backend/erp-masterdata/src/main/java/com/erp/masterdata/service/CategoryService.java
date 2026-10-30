package com.erp.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.masterdata.dto.CategoryNode;
import com.erp.masterdata.entity.MdCategory;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdCategoryMapper;
import com.erp.masterdata.mapper.MdProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService extends ServiceImpl<MdCategoryMapper, MdCategory> {

    private final MdProductMapper productMapper;

    public List<CategoryNode> tree() {
        List<MdCategory> all = list(new LambdaQueryWrapper<MdCategory>().orderByAsc(MdCategory::getSort)
                .orderByAsc(MdCategory::getId));
        Map<Long, CategoryNode> index = new LinkedHashMap<>();
        all.forEach(c -> index.put(c.getId(), CategoryNode.from(c)));
        List<CategoryNode> roots = new ArrayList<>();
        index.values().forEach(node -> {
            CategoryNode parent = index.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        });
        return roots;
    }

    public PageResult<MdCategory> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<MdCategory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MdCategory::getCode, keyword).or().like(MdCategory::getName, keyword));
        }
        wrapper.eq(status != null, MdCategory::getStatus, status).orderByAsc(MdCategory::getSort);
        return PageResult.of(page(new Page<>(page, size), wrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(MdCategory category) {
        validate(category, null);
        category.setId(null);
        category.setParentId(category.getParentId() == null ? 0L : category.getParentId());
        category.setSort(category.getSort() == null ? 0 : category.getSort());
        category.setStatus(category.getStatus() == null ? 1 : category.getStatus());
        save(category);
        return category.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MdCategory category) {
        getRequired(id);
        validate(category, id);
        category.setId(id);
        updateById(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getRequired(id);
        long children = count(new LambdaQueryWrapper<MdCategory>().eq(MdCategory::getParentId, id));
        if (children > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "请先删除子分类");
        }
        long products = productMapper.selectCount(new LambdaQueryWrapper<MdProduct>().eq(MdProduct::getCategoryId, id));
        if (products > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "该分类下存在商品，不能删除");
        }
        removeById(id);
    }

    private MdCategory getRequired(Long id) {
        MdCategory category = getById(id);
        if (category == null) {
            throw BizException.notFound("商品分类不存在");
        }
        return category;
    }

    private void validate(MdCategory category, Long id) {
        if (!StringUtils.hasText(category.getCode()) || !StringUtils.hasText(category.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "分类编码与名称不能为空");
        }
        if (id != null && id.equals(category.getParentId())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "上级分类不能是自身");
        }
        long duplicated = count(new LambdaQueryWrapper<MdCategory>().eq(MdCategory::getCode, category.getCode())
                .ne(id != null, MdCategory::getId, id));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "分类编码已存在");
        }
    }
}

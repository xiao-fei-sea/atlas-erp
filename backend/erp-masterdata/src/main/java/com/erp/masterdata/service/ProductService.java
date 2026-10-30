package com.erp.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.CsvUtils;
import com.erp.common.util.MoneyUtils;
import com.erp.masterdata.entity.MdCategory;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.mapper.MdCategoryMapper;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.mapper.MdUnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService extends ServiceImpl<MdProductMapper, MdProduct> {

    private final MdCategoryMapper categoryMapper;
    private final MdUnitMapper unitMapper;

    public PageResult<MdProduct> page(long page, long size, String keyword, Long categoryId, Integer status) {
        LambdaQueryWrapper<MdProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MdProduct::getCode, keyword)
                    .or().like(MdProduct::getName, keyword)
                    .or().like(MdProduct::getBarcode, keyword));
        }
        wrapper.eq(categoryId != null, MdProduct::getCategoryId, categoryId)
                .eq(status != null, MdProduct::getStatus, status)
                .orderByDesc(MdProduct::getId);
        return PageResult.of(page(new Page<>(page, size), wrapper));
    }

    public List<MdProduct> listEnabled() {
        return list(new LambdaQueryWrapper<MdProduct>().eq(MdProduct::getStatus, 1).orderByAsc(MdProduct::getId));
    }

    public MdProduct getRequired(Long id) {
        MdProduct product = getById(id);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        return product;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(MdProduct product) {
        validate(product, null);
        product.setId(null);
        product.setTaxRate(MoneyUtils.nz(product.getTaxRate()));
        product.setPurchasePrice(MoneyUtils.nz(product.getPurchasePrice()));
        product.setSalePrice(MoneyUtils.nz(product.getSalePrice()));
        product.setSafetyStock(MoneyUtils.nz(product.getSafetyStock()));
        product.setStatus(product.getStatus() == null ? 1 : product.getStatus());
        save(product);
        return product.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MdProduct product) {
        getRequired(id);
        validate(product, id);
        product.setId(id);
        product.setTaxRate(MoneyUtils.nz(product.getTaxRate()));
        product.setPurchasePrice(MoneyUtils.nz(product.getPurchasePrice()));
        product.setSalePrice(MoneyUtils.nz(product.getSalePrice()));
        product.setSafetyStock(MoneyUtils.nz(product.getSafetyStock()));
        updateById(product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getRequired(id);
        removeById(id);
    }

    public byte[] exportCsv(String keyword, Long categoryId, Integer status) {
        LambdaQueryWrapper<MdProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MdProduct::getCode, keyword).or().like(MdProduct::getName, keyword));
        }
        wrapper.eq(categoryId != null, MdProduct::getCategoryId, categoryId)
                .eq(status != null, MdProduct::getStatus, status)
                .orderByAsc(MdProduct::getId);
        List<MdProduct> products = list(wrapper);
        Map<Long, String> categories = new HashMap<>();
        categoryMapper.selectList(null).forEach(c -> categories.put(c.getId(), c.getName()));
        Map<Long, String> units = new HashMap<>();
        unitMapper.selectList(null).forEach(u -> units.put(u.getId(), u.getName()));
        List<List<String>> rows = products.stream().map(p -> List.of(
                nvl(p.getCode()),
                nvl(p.getName()),
                nvl(categories.get(p.getCategoryId())),
                nvl(p.getSpec()),
                nvl(units.get(p.getUnitId())),
                nvl(p.getTaxRate()) + "",
                nvl(p.getPurchasePrice()) + "",
                nvl(p.getSalePrice()) + "",
                nvl(p.getSafetyStock()) + "",
                p.getStatus() != null && p.getStatus() == 1 ? "启用" : "停用"
        )).toList();
        return CsvUtils.toCsv(List.of("商品编码", "商品名称", "分类", "规格", "单位", "税率",
                "采购价", "销售价", "安全库存", "状态"), rows);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validate(MdProduct product, Long id) {
        if (!StringUtils.hasText(product.getCode()) || !StringUtils.hasText(product.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品编码与名称不能为空");
        }
        long duplicated = count(new LambdaQueryWrapper<MdProduct>().eq(MdProduct::getCode, product.getCode())
                .ne(id != null, MdProduct::getId, id));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "商品编码已存在");
        }
        if (product.getCategoryId() != null && categoryMapper.selectById(product.getCategoryId()) == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品分类不存在");
        }
        if (product.getUnitId() != null && unitMapper.selectById(product.getUnitId()) == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "计量单位不存在");
        }
    }
}

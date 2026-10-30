package com.erp.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.mapper.MdSupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SupplierService extends ServiceImpl<MdSupplierMapper, MdSupplier> {

    public PageResult<MdSupplier> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<MdSupplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MdSupplier::getCode, keyword)
                    .or().like(MdSupplier::getName, keyword)
                    .or().like(MdSupplier::getPhone, keyword));
        }
        wrapper.eq(status != null, MdSupplier::getStatus, status).orderByDesc(MdSupplier::getId);
        return PageResult.of(page(new Page<>(page, size), wrapper));
    }

    public List<MdSupplier> listEnabled() {
        return list(new LambdaQueryWrapper<MdSupplier>().eq(MdSupplier::getStatus, 1).orderByAsc(MdSupplier::getId));
    }

    public MdSupplier getRequired(Long id) {
        MdSupplier supplier = getById(id);
        if (supplier == null) {
            throw BizException.notFound("供应商不存在");
        }
        return supplier;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(MdSupplier supplier) {
        validate(supplier, null);
        supplier.setId(null);
        supplier.setStatus(supplier.getStatus() == null ? 1 : supplier.getStatus());
        save(supplier);
        return supplier.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MdSupplier supplier) {
        getRequired(id);
        validate(supplier, id);
        supplier.setId(id);
        updateById(supplier);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getRequired(id);
        removeById(id);
    }

    private void validate(MdSupplier supplier, Long id) {
        if (!StringUtils.hasText(supplier.getCode()) || !StringUtils.hasText(supplier.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "供应商编码与名称不能为空");
        }
        long duplicated = count(new LambdaQueryWrapper<MdSupplier>().eq(MdSupplier::getCode, supplier.getCode())
                .ne(id != null, MdSupplier::getId, id));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "供应商编码已存在");
        }
    }
}

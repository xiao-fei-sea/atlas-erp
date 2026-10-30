package com.erp.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.mapper.MdUnitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UnitService extends ServiceImpl<MdUnitMapper, MdUnit> {

    public PageResult<MdUnit> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<MdUnit> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MdUnit::getCode, keyword).or().like(MdUnit::getName, keyword));
        }
        wrapper.eq(status != null, MdUnit::getStatus, status).orderByAsc(MdUnit::getId);
        return PageResult.of(page(new Page<>(page, size), wrapper));
    }

    public List<MdUnit> listEnabled() {
        return list(new LambdaQueryWrapper<MdUnit>().eq(MdUnit::getStatus, 1).orderByAsc(MdUnit::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(MdUnit unit) {
        validate(unit, null);
        unit.setId(null);
        unit.setPrecisionNum(unit.getPrecisionNum() == null ? 0 : unit.getPrecisionNum());
        unit.setStatus(unit.getStatus() == null ? 1 : unit.getStatus());
        save(unit);
        return unit.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MdUnit unit) {
        if (getById(id) == null) {
            throw BizException.notFound("计量单位不存在");
        }
        validate(unit, id);
        unit.setId(id);
        updateById(unit);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (getById(id) == null) {
            throw BizException.notFound("计量单位不存在");
        }
        removeById(id);
    }

    private void validate(MdUnit unit, Long id) {
        if (!StringUtils.hasText(unit.getCode()) || !StringUtils.hasText(unit.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "单位编码与名称不能为空");
        }
        long duplicated = count(new LambdaQueryWrapper<MdUnit>().eq(MdUnit::getCode, unit.getCode())
                .ne(id != null, MdUnit::getId, id));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "单位编码已存在");
        }
    }
}

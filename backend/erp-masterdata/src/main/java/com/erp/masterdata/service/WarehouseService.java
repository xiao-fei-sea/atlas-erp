package com.erp.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.masterdata.entity.MdLocation;
import com.erp.masterdata.entity.MdWarehouse;
import com.erp.masterdata.mapper.MdLocationMapper;
import com.erp.masterdata.mapper.MdWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService extends ServiceImpl<MdWarehouseMapper, MdWarehouse> {

    private final MdLocationMapper locationMapper;

    public PageResult<MdWarehouse> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<MdWarehouse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MdWarehouse::getCode, keyword).or().like(MdWarehouse::getName, keyword));
        }
        wrapper.eq(status != null, MdWarehouse::getStatus, status).orderByAsc(MdWarehouse::getId);
        return PageResult.of(page(new Page<>(page, size), wrapper));
    }

    public List<MdWarehouse> listEnabled() {
        return list(new LambdaQueryWrapper<MdWarehouse>().eq(MdWarehouse::getStatus, 1).orderByAsc(MdWarehouse::getId));
    }

    public MdWarehouse getRequired(Long id) {
        MdWarehouse warehouse = getById(id);
        if (warehouse == null) {
            throw BizException.notFound("仓库不存在");
        }
        return warehouse;
    }

    public List<MdLocation> listLocations(Long warehouseId) {
        return locationMapper.selectList(new LambdaQueryWrapper<MdLocation>()
                .eq(warehouseId != null, MdLocation::getWarehouseId, warehouseId)
                .orderByAsc(MdLocation::getWarehouseId).orderByAsc(MdLocation::getCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(MdWarehouse warehouse) {
        validate(warehouse, null);
        warehouse.setId(null);
        warehouse.setStatus(warehouse.getStatus() == null ? 1 : warehouse.getStatus());
        save(warehouse);
        return warehouse.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MdWarehouse warehouse) {
        getRequired(id);
        validate(warehouse, id);
        warehouse.setId(id);
        updateById(warehouse);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getRequired(id);
        long locations = locationMapper.selectCount(new LambdaQueryWrapper<MdLocation>()
                .eq(MdLocation::getWarehouseId, id));
        if (locations > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "该仓库下存在库位，请先删除库位");
        }
        removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createLocation(Long warehouseId, MdLocation location) {
        getRequired(warehouseId);
        if (!StringUtils.hasText(location.getCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "库位编码不能为空");
        }
        long duplicated = locationMapper.selectCount(new LambdaQueryWrapper<MdLocation>()
                .eq(MdLocation::getWarehouseId, warehouseId).eq(MdLocation::getCode, location.getCode()));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "同一仓库下库位编码已存在");
        }
        location.setId(null);
        location.setWarehouseId(warehouseId);
        location.setStatus(location.getStatus() == null ? 1 : location.getStatus());
        locationMapper.insert(location);
        return location.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteLocation(Long warehouseId, Long locationId) {
        MdLocation location = locationMapper.selectById(locationId);
        if (location == null || !location.getWarehouseId().equals(warehouseId)) {
            throw BizException.notFound("库位不存在");
        }
        locationMapper.deleteById(locationId);
    }

    private void validate(MdWarehouse warehouse, Long id) {
        if (!StringUtils.hasText(warehouse.getCode()) || !StringUtils.hasText(warehouse.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仓库编码与名称不能为空");
        }
        long duplicated = count(new LambdaQueryWrapper<MdWarehouse>().eq(MdWarehouse::getCode, warehouse.getCode())
                .ne(id != null, MdWarehouse::getId, id));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "仓库编码已存在");
        }
    }
}

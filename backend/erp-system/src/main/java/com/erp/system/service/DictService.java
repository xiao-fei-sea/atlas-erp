package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.api.ErrorCode;
import com.erp.common.exception.BizException;
import com.erp.system.entity.SysDictItem;
import com.erp.system.entity.SysDictType;
import com.erp.system.mapper.SysDictItemMapper;
import com.erp.system.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    public List<SysDictType> listTypes() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>().orderByAsc(SysDictType::getId));
    }

    public Long createType(SysDictType type) {
        Long count = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getCode, type.getCode()));
        if (count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "字典编码已存在");
        }
        dictTypeMapper.insert(type);
        return type.getId();
    }

    public void updateType(Long id, SysDictType type) {
        type.setId(id);
        dictTypeMapper.updateById(type);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        dictTypeMapper.deleteById(id);
        dictItemMapper.delete(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictTypeId, id));
    }

    public List<SysDictItem> listItems(Long typeId) {
        return dictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictTypeId, typeId)
                .orderByAsc(SysDictItem::getSort));
    }

    public List<SysDictItem> listItemsByCode(String code) {
        SysDictType type = dictTypeMapper.selectOne(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getCode, code));
        if (type == null) {
            return List.of();
        }
        return listItems(type.getId());
    }

    public Long createItem(SysDictItem item) {
        dictItemMapper.insert(item);
        return item.getId();
    }

    public void updateItem(Long id, SysDictItem item) {
        item.setId(id);
        dictItemMapper.updateById(item);
    }

    public void deleteItem(Long id) {
        dictItemMapper.deleteById(id);
    }
}

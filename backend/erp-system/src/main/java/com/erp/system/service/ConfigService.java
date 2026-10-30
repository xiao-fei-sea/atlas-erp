package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.system.entity.SysConfig;
import com.erp.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigMapper configMapper;

    public List<SysConfig> list(String groupCode) {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(StringUtils.hasText(groupCode), SysConfig::getGroupCode, groupCode)
                .orderByAsc(SysConfig::getId));
    }

    public void save(SysConfig config) {
        SysConfig exists = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, config.getConfigKey()));
        if (exists == null) {
            configMapper.insert(config);
        } else {
            config.setId(exists.getId());
            configMapper.updateById(config);
        }
    }

    public String getValue(String key, String defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        return config == null || config.getConfigValue() == null ? defaultValue : config.getConfigValue();
    }

    public void delete(Long id) {
        configMapper.deleteById(id);
    }
}

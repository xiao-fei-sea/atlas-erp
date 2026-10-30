package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.PageResult;
import com.erp.system.entity.SysLoginLog;
import com.erp.system.entity.SysOperLog;
import com.erp.system.mapper.SysLoginLogMapper;
import com.erp.system.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperLogMapper operLogMapper;

    public void recordLogin(String username, String ip, String userAgent, boolean success, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setIp(ip);
        log.setUserAgent(userAgent != null && userAgent.length() > 250 ? userAgent.substring(0, 250) : userAgent);
        log.setSuccess(success ? 1 : 0);
        log.setMessage(message);
        log.setLoginAt(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    public PageResult<SysLoginLog> pageLogin(long page, long size, String username) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysLoginLog::getUsername, username)
                .orderByDesc(SysLoginLog::getId);
        return PageResult.of(loginLogMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public PageResult<SysOperLog> pageOper(long page, long size, String module, String operator) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(module), SysOperLog::getModule, module)
                .like(StringUtils.hasText(operator), SysOperLog::getOperatorName, operator)
                .orderByDesc(SysOperLog::getId);
        return PageResult.of(operLogMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public void recordOper(SysOperLog log) {
        operLogMapper.insert(log);
    }
}

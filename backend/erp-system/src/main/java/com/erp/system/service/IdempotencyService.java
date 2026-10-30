package com.erp.system.service;

import com.erp.common.api.ErrorCode;
import com.erp.common.exception.BizException;
import com.erp.common.security.SecurityUtils;
import com.erp.system.entity.SysIdempotency;
import com.erp.system.mapper.SysIdempotencyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 幂等控制：同一用户 + 同一幂等键只允许执行一次业务动作。
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final SysIdempotencyMapper idempotencyMapper;

    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(String key, String bizCode, Supplier<T> action) {
        if (!StringUtils.hasText(key)) {
            return action.get();
        }
        SysIdempotency record = new SysIdempotency();
        record.setIdemKey(key);
        record.setUserId(SecurityUtils.getUserId());
        record.setBizCode(bizCode);
        record.setCreatedAt(LocalDateTime.now());
        try {
            idempotencyMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.IDEMPOTENT_REPEAT, "该操作已提交，请勿重复提交");
        }
        return action.get();
    }
}

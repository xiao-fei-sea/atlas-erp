package com.erp.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.MoneyUtils;
import com.erp.finance.entity.FinAccount;
import com.erp.finance.mapper.FinAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AccountService extends ServiceImpl<FinAccountMapper, FinAccount> {

    public PageResult<FinAccount> page(long page, long size, String keyword) {
        LambdaQueryWrapper<FinAccount> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(FinAccount::getCode, keyword).or().like(FinAccount::getName, keyword));
        }
        wrapper.orderByAsc(FinAccount::getId);
        return PageResult.of(page(new Page<>(page, size), wrapper));
    }

    public List<FinAccount> listEnabled() {
        return list(new LambdaQueryWrapper<FinAccount>().eq(FinAccount::getStatus, 1).orderByAsc(FinAccount::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(FinAccount account) {
        validate(account, null);
        account.setId(null);
        account.setBalance(MoneyUtils.nz(account.getBalance()));
        account.setType(StringUtils.hasText(account.getType()) ? account.getType() : "BANK");
        account.setStatus(account.getStatus() == null ? 1 : account.getStatus());
        save(account);
        return account.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinAccount account) {
        FinAccount exists = getById(id);
        if (exists == null) {
            throw BizException.notFound("资金账户不存在");
        }
        validate(account, id);
        account.setId(id);
        account.setBalance(MoneyUtils.nz(account.getBalance()));
        updateById(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FinAccount account = getById(id);
        if (account == null) {
            throw BizException.notFound("资金账户不存在");
        }
        if (MoneyUtils.nz(account.getBalance()).compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "账户余额不为0，不能删除");
        }
        removeById(id);
    }

    private void validate(FinAccount account, Long id) {
        if (!StringUtils.hasText(account.getCode()) || !StringUtils.hasText(account.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "账户编码与名称不能为空");
        }
        long duplicated = count(new LambdaQueryWrapper<FinAccount>().eq(FinAccount::getCode, account.getCode())
                .ne(id != null, FinAccount::getId, id));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "账户编码已存在");
        }
    }
}

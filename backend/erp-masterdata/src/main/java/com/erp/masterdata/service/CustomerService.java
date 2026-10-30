package com.erp.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.util.MoneyUtils;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.entity.MdCustomerContact;
import com.erp.masterdata.mapper.MdCustomerContactMapper;
import com.erp.masterdata.mapper.MdCustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService extends ServiceImpl<MdCustomerMapper, MdCustomer> {

    private final MdCustomerContactMapper contactMapper;

    public PageResult<MdCustomer> page(long page, long size, String keyword, String level, Integer status) {
        LambdaQueryWrapper<MdCustomer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MdCustomer::getCode, keyword)
                    .or().like(MdCustomer::getName, keyword)
                    .or().like(MdCustomer::getPhone, keyword));
        }
        wrapper.eq(StringUtils.hasText(level), MdCustomer::getLevel, level)
                .eq(status != null, MdCustomer::getStatus, status)
                .orderByDesc(MdCustomer::getId);
        return PageResult.of(page(new Page<>(page, size), wrapper));
    }

    public List<MdCustomer> listEnabled() {
        return list(new LambdaQueryWrapper<MdCustomer>().eq(MdCustomer::getStatus, 1).orderByAsc(MdCustomer::getId));
    }

    public MdCustomer getRequired(Long id) {
        MdCustomer customer = getById(id);
        if (customer == null) {
            throw BizException.notFound("客户不存在");
        }
        return customer;
    }

    public List<MdCustomerContact> listContacts(Long customerId) {
        return contactMapper.selectList(new LambdaQueryWrapper<MdCustomerContact>()
                .eq(MdCustomerContact::getCustomerId, customerId).orderByAsc(MdCustomerContact::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(MdCustomer customer) {
        validate(customer, null);
        customer.setId(null);
        customer.setLevel(StringUtils.hasText(customer.getLevel()) ? customer.getLevel() : "C");
        customer.setCreditLimit(MoneyUtils.nz(customer.getCreditLimit()));
        customer.setStatus(customer.getStatus() == null ? 1 : customer.getStatus());
        save(customer);
        return customer.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MdCustomer customer) {
        getRequired(id);
        validate(customer, id);
        customer.setId(id);
        customer.setCreditLimit(MoneyUtils.nz(customer.getCreditLimit()));
        updateById(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getRequired(id);
        removeById(id);
        contactMapper.delete(new LambdaQueryWrapper<MdCustomerContact>()
                .eq(MdCustomerContact::getCustomerId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addContact(Long customerId, MdCustomerContact contact) {
        getRequired(customerId);
        if (!StringUtils.hasText(contact.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "联系人姓名不能为空");
        }
        contact.setId(null);
        contact.setCustomerId(customerId);
        contactMapper.insert(contact);
        return contact.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteContact(Long customerId, Long contactId) {
        MdCustomerContact contact = contactMapper.selectById(contactId);
        if (contact == null || !contact.getCustomerId().equals(customerId)) {
            throw BizException.notFound("联系人不存在");
        }
        contactMapper.deleteById(contactId);
    }

    private void validate(MdCustomer customer, Long id) {
        if (!StringUtils.hasText(customer.getCode()) || !StringUtils.hasText(customer.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "客户编码与名称不能为空");
        }
        long duplicated = count(new LambdaQueryWrapper<MdCustomer>().eq(MdCustomer::getCode, customer.getCode())
                .ne(id != null, MdCustomer::getId, id));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "客户编码已存在");
        }
    }
}

package com.erp.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.security.SecurityUtils;
import com.erp.common.util.MoneyUtils;
import com.erp.crm.entity.CrmFollowUp;
import com.erp.crm.entity.CrmLead;
import com.erp.crm.entity.CrmOpportunity;
import com.erp.crm.entity.CrmVisitPlan;
import com.erp.crm.mapper.CrmFollowUpMapper;
import com.erp.crm.mapper.CrmLeadMapper;
import com.erp.crm.mapper.CrmOpportunityMapper;
import com.erp.crm.mapper.CrmVisitPlanMapper;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.service.CustomerService;
import com.erp.system.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrmService {

    private final CrmLeadMapper leadMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmFollowUpMapper followUpMapper;
    private final CrmVisitPlanMapper visitPlanMapper;
    private final CustomerService customerService;
    private final SequenceService sequenceService;

    public PageResult<CrmLead> pageLeads(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<CrmLead> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmLead::getName, keyword)
                    .or().like(CrmLead::getCompany, keyword)
                    .or().like(CrmLead::getPhone, keyword));
        }
        wrapper.eq(StringUtils.hasText(status), CrmLead::getStatus, status).orderByDesc(CrmLead::getId);
        return PageResult.of(leadMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createLead(CrmLead lead) {
        if (!StringUtils.hasText(lead.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "线索名称不能为空");
        }
        lead.setId(null);
        lead.setStatus(StringUtils.hasText(lead.getStatus()) ? lead.getStatus() : "NEW");
        lead.setOwnerId(lead.getOwnerId() == null ? SecurityUtils.getUserId() : lead.getOwnerId());
        leadMapper.insert(lead);
        return lead.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateLead(Long id, CrmLead lead) {
        CrmLead exists = leadMapper.selectById(id);
        if (exists == null) {
            throw BizException.notFound("线索不存在");
        }
        lead.setId(id);
        leadMapper.updateById(lead);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long convertLead(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw BizException.notFound("线索不存在");
        }
        if (lead.getCustomerId() != null) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "该线索已转客户");
        }
        MdCustomer customer = new MdCustomer();
        customer.setCode(sequenceService.next("CUSTOMER"));
        customer.setName(StringUtils.hasText(lead.getCompany()) ? lead.getCompany() : lead.getName());
        customer.setContactName(lead.getName());
        customer.setPhone(lead.getPhone());
        customer.setLevel("C");
        customer.setCreditLimit(MoneyUtils.zero());
        customer.setStatus(1);
        customer.setRemark("由线索转换");
        Long customerId = customerService.create(customer);
        lead.setStatus("CONVERTED");
        lead.setCustomerId(customerId);
        leadMapper.updateById(lead);
        return customerId;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteLead(Long id) {
        if (leadMapper.selectById(id) == null) {
            throw BizException.notFound("线索不存在");
        }
        leadMapper.deleteById(id);
    }

    public PageResult<CrmOpportunity> pageOpportunities(long page, long size, String keyword, String stage) {
        LambdaQueryWrapper<CrmOpportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), CrmOpportunity::getName, keyword)
                .eq(StringUtils.hasText(stage), CrmOpportunity::getStage, stage)
                .orderByDesc(CrmOpportunity::getId);
        return PageResult.of(opportunityMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createOpportunity(CrmOpportunity opportunity) {
        if (!StringUtils.hasText(opportunity.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商机名称不能为空");
        }
        if (opportunity.getCustomerId() != null) {
            customerService.getRequired(opportunity.getCustomerId());
        }
        opportunity.setId(null);
        opportunity.setStage(StringUtils.hasText(opportunity.getStage()) ? opportunity.getStage() : "INITIAL");
        opportunity.setProbability(opportunity.getProbability() == null ? 10 : opportunity.getProbability());
        opportunity.setAmount(MoneyUtils.nz(opportunity.getAmount()));
        opportunity.setOwnerId(opportunity.getOwnerId() == null ? SecurityUtils.getUserId() : opportunity.getOwnerId());
        opportunityMapper.insert(opportunity);
        return opportunity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateOpportunity(Long id, CrmOpportunity opportunity) {
        if (opportunityMapper.selectById(id) == null) {
            throw BizException.notFound("商机不存在");
        }
        opportunity.setId(id);
        opportunity.setAmount(MoneyUtils.nz(opportunity.getAmount()));
        opportunityMapper.updateById(opportunity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStage(Long id, String stage, Integer probability) {
        CrmOpportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw BizException.notFound("商机不存在");
        }
        opportunity.setStage(stage);
        if (probability != null) {
            opportunity.setProbability(probability);
        }
        opportunityMapper.updateById(opportunity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOpportunity(Long id) {
        if (opportunityMapper.selectById(id) == null) {
            throw BizException.notFound("商机不存在");
        }
        opportunityMapper.deleteById(id);
    }

    public List<CrmFollowUp> listFollowUps(String targetType, Long targetId) {
        return followUpMapper.selectList(new LambdaQueryWrapper<CrmFollowUp>()
                .eq(StringUtils.hasText(targetType), CrmFollowUp::getTargetType, targetType)
                .eq(targetId != null, CrmFollowUp::getTargetId, targetId)
                .orderByDesc(CrmFollowUp::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addFollowUp(CrmFollowUp followUp) {
        if (!StringUtils.hasText(followUp.getContent()) || followUp.getTargetId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "跟进对象与内容不能为空");
        }
        followUp.setId(null);
        followUp.setTargetType(StringUtils.hasText(followUp.getTargetType()) ? followUp.getTargetType() : "CUSTOMER");
        followUp.setFollowAt(followUp.getFollowAt() == null ? LocalDateTime.now() : followUp.getFollowAt());
        followUp.setOwnerId(SecurityUtils.getUserId());
        followUp.setCreatedAt(LocalDateTime.now());
        followUpMapper.insert(followUp);
        return followUp.getId();
    }

    public PageResult<CrmVisitPlan> pageVisitPlans(long page, long size, String status) {
        LambdaQueryWrapper<CrmVisitPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), CrmVisitPlan::getStatus, status).orderByAsc(CrmVisitPlan::getPlanDate);
        return PageResult.of(visitPlanMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createVisitPlan(CrmVisitPlan plan) {
        if (plan.getCustomerId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "拜访客户不能为空");
        }
        customerService.getRequired(plan.getCustomerId());
        plan.setId(null);
        plan.setPlanDate(plan.getPlanDate() == null ? LocalDate.now() : plan.getPlanDate());
        plan.setStatus(StringUtils.hasText(plan.getStatus()) ? plan.getStatus() : "PLANNED");
        plan.setOwnerId(SecurityUtils.getUserId());
        visitPlanMapper.insert(plan);
        return plan.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateVisitStatus(Long id, String status) {
        CrmVisitPlan plan = visitPlanMapper.selectById(id);
        if (plan == null) {
            throw BizException.notFound("拜访计划不存在");
        }
        plan.setStatus(status);
        visitPlanMapper.updateById(plan);
    }
}

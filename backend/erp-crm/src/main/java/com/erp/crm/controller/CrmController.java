package com.erp.crm.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.crm.entity.CrmFollowUp;
import com.erp.crm.entity.CrmLead;
import com.erp.crm.entity.CrmOpportunity;
import com.erp.crm.entity.CrmVisitPlan;
import com.erp.crm.service.CrmService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm")
@RequiredArgsConstructor
public class CrmController {

    private final CrmService crmService;

    @GetMapping("/leads")
    @PreAuthorize("hasAuthority('crm:lead:view')")
    public Result<PageResult<CrmLead>> leads(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status) {
        return Result.ok(crmService.pageLeads(page, size, keyword, status));
    }

    @PostMapping("/leads")
    @PreAuthorize("hasAuthority('crm:lead:create')")
    @OperLog(module = "客户关系", action = "新增线索")
    public Result<Long> createLead(@RequestBody CrmLead lead) {
        return Result.ok(crmService.createLead(lead));
    }

    @PutMapping("/leads/{id}")
    @PreAuthorize("hasAuthority('crm:lead:update')")
    @OperLog(module = "客户关系", action = "修改线索")
    public Result<Void> updateLead(@PathVariable Long id, @RequestBody CrmLead lead) {
        crmService.updateLead(id, lead);
        return Result.ok();
    }

    @PutMapping("/leads/{id}/convert")
    @PreAuthorize("hasAuthority('crm:lead:update')")
    @OperLog(module = "客户关系", action = "线索转客户")
    public Result<Long> convertLead(@PathVariable Long id) {
        return Result.ok(crmService.convertLead(id));
    }

    @DeleteMapping("/leads/{id}")
    @PreAuthorize("hasAuthority('crm:lead:update')")
    @OperLog(module = "客户关系", action = "删除线索")
    public Result<Void> deleteLead(@PathVariable Long id) {
        crmService.deleteLead(id);
        return Result.ok();
    }

    @GetMapping("/opportunities")
    @PreAuthorize("hasAuthority('crm:opportunity:view')")
    public Result<PageResult<CrmOpportunity>> opportunities(@RequestParam(defaultValue = "1") long page,
                                                            @RequestParam(defaultValue = "10") long size,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String stage) {
        return Result.ok(crmService.pageOpportunities(page, size, keyword, stage));
    }

    @PostMapping("/opportunities")
    @PreAuthorize("hasAuthority('crm:opportunity:create')")
    @OperLog(module = "客户关系", action = "新增商机")
    public Result<Long> createOpportunity(@RequestBody CrmOpportunity opportunity) {
        return Result.ok(crmService.createOpportunity(opportunity));
    }

    @PutMapping("/opportunities/{id}")
    @PreAuthorize("hasAuthority('crm:opportunity:update')")
    @OperLog(module = "客户关系", action = "修改商机")
    public Result<Void> updateOpportunity(@PathVariable Long id, @RequestBody CrmOpportunity opportunity) {
        crmService.updateOpportunity(id, opportunity);
        return Result.ok();
    }

    @PutMapping("/opportunities/{id}/stage")
    @PreAuthorize("hasAuthority('crm:opportunity:update')")
    @OperLog(module = "客户关系", action = "推进商机阶段")
    public Result<Void> updateStage(@PathVariable Long id, @RequestParam String stage,
                                    @RequestParam(required = false) Integer probability) {
        crmService.updateStage(id, stage, probability);
        return Result.ok();
    }

    @DeleteMapping("/opportunities/{id}")
    @PreAuthorize("hasAuthority('crm:opportunity:update')")
    @OperLog(module = "客户关系", action = "删除商机")
    public Result<Void> deleteOpportunity(@PathVariable Long id) {
        crmService.deleteOpportunity(id);
        return Result.ok();
    }

    @GetMapping("/followups")
    @PreAuthorize("hasAuthority('crm:followup:view')")
    public Result<List<CrmFollowUp>> followUps(@RequestParam(required = false) String targetType,
                                               @RequestParam(required = false) Long targetId) {
        return Result.ok(crmService.listFollowUps(targetType, targetId));
    }

    @PostMapping("/followups")
    @PreAuthorize("hasAuthority('crm:followup:create')")
    @OperLog(module = "客户关系", action = "新增跟进记录")
    public Result<Long> addFollowUp(@RequestBody CrmFollowUp followUp) {
        return Result.ok(crmService.addFollowUp(followUp));
    }

    @GetMapping("/visits")
    @PreAuthorize("hasAuthority('crm:followup:view')")
    public Result<PageResult<CrmVisitPlan>> visits(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String status) {
        return Result.ok(crmService.pageVisitPlans(page, size, status));
    }

    @PostMapping("/visits")
    @PreAuthorize("hasAuthority('crm:followup:create')")
    @OperLog(module = "客户关系", action = "新增拜访计划")
    public Result<Long> createVisit(@RequestBody CrmVisitPlan plan) {
        return Result.ok(crmService.createVisitPlan(plan));
    }

    @PutMapping("/visits/{id}/status")
    @PreAuthorize("hasAuthority('crm:followup:create')")
    @OperLog(module = "客户关系", action = "更新拜访状态")
    public Result<Void> updateVisitStatus(@PathVariable Long id, @RequestParam String status) {
        crmService.updateVisitStatus(id, status);
        return Result.ok();
    }
}

package com.erp.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审批超时扫描：定时将超过截止时间的待办标记为超时。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowOverdueScheduler {

    private final WorkflowService workflowService;

    @Scheduled(fixedDelayString = "${erp.workflow.overdue-scan-ms:300000}")
    public void scanOverdue() {
        int count = workflowService.remindOverdueTasks();
        if (count > 0) {
            log.info("审批超时扫描完成，标记 {} 条待办为超时", count);
        }
    }
}

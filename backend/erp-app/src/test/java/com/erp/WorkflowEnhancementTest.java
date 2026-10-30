package com.erp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.erp.common.security.LoginUser;
import com.erp.workflow.dto.ProcessSaveRequest;
import com.erp.workflow.entity.WfInstance;
import com.erp.workflow.entity.WfTask;
import com.erp.workflow.mapper.WfTaskMapper;
import com.erp.workflow.service.WorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批增强集成测试：会签、或签、转交、部门审批条件、超时标记。
 * 所有用例在事务内执行并回滚。
 */
@SpringBootTest
@Transactional
class WorkflowEnhancementTest {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private WfTaskMapper wfTaskMapper;

    @BeforeEach
    void loginAsAdmin() {
        login(1L, "admin", 1L, "ADMIN");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void countersignNode_shouldAdvanceOnlyWhenAllApproved() {
        Long processId = createProcess("TEST_AND", "TEST_AND",
                node(1, "USER", "3,4", "AND", null, 0));
        Long instanceId = workflowService.submit("TEST_AND", 9001L, "TEST-AND-1", new BigDecimal("100"));
        WfInstance instance = workflowService.findRunning("TEST_AND", 9001L);
        assertNotNull(instance);

        List<WfTask> tasks = pendingTasks(instanceId);
        assertEquals(2, tasks.size());
        assertEquals("AND", tasks.get(0).getSignType());

        workflowService.approve(tasks.get(0).getId(), "同意");
        instance = workflowService.findRunning("TEST_AND", 9001L);
        assertEquals("RUNNING", instance.getStatus(), "会签未全部通过时应保持审批中");
        assertEquals(1, pendingTasks(instanceId).size());

        workflowService.approve(tasks.get(1).getId(), "同意");
        assertEquals("APPROVED", workflowService.findRunning("TEST_AND", 9001L).getStatus());
    }

    @Test
    void orSignNode_shouldCancelOtherPendingTasksAfterOneApproval() {
        createProcess("TEST_OR", "TEST_OR",
                node(1, "USER", "3,4", "OR", null, 0));
        Long instanceId = workflowService.submit("TEST_OR", 9002L, "TEST-OR-1", new BigDecimal("100"));
        List<WfTask> tasks = pendingTasks(instanceId);
        assertEquals(2, tasks.size());

        workflowService.approve(tasks.get(0).getId(), "同意");
        assertEquals("APPROVED", workflowService.findRunning("TEST_OR", 9002L).getStatus());
        WfTask cancelled = wfTaskMapper.selectById(tasks.get(1).getId());
        assertEquals("CANCELLED", cancelled.getStatus());
    }

    @Test
    void transfer_shouldReassignApproverAndKeepOriginal() {
        createProcess("TEST_TRANSFER", "TEST_TRANSFER",
                node(1, "USER", "3", "OR", null, 0));
        Long instanceId = workflowService.submit("TEST_TRANSFER", 9003L, "TEST-TRANSFER-1", new BigDecimal("100"));
        WfTask task = pendingTasks(instanceId).get(0);
        assertEquals(3L, task.getApproverId().longValue());

        workflowService.transfer(task.getId(), 2L, "本人出差，转交处理");
        WfTask transferred = wfTaskMapper.selectById(task.getId());
        assertEquals(2L, transferred.getApproverId().longValue());
        assertEquals(3L, transferred.getOriginApproverId().longValue());
        assertEquals(1L, transferred.getTransferredBy().longValue());
        assertNotNull(transferred.getTransferredAt());
    }

    @Test
    void deptCondition_shouldMatchSubmitterDeptOnly() {
        createProcess("TEST_DEPT_OTHER", "TEST_DEPT_OTHER",
                node(1, "USER", "3", "OR", 3L, 0));
        createProcess("TEST_DEPT_SALES", "TEST_DEPT_SALES",
                node(1, "USER", "3", "OR", 2L, 0));

        login(2L, "sales01", 2L, "SALES");
        Long otherInstance = workflowService.submit("TEST_DEPT_OTHER", 9004L, "TEST-DEPT-1", new BigDecimal("100"));
        WfInstance other = workflowService.findRunning("TEST_DEPT_OTHER", 9004L);
        assertEquals("APPROVED", other.getStatus(), "部门不匹配节点应被跳过并自动通过");

        Long salesInstance = workflowService.submit("TEST_DEPT_SALES", 9005L, "TEST-DEPT-2", new BigDecimal("100"));
        WfInstance sales = workflowService.findRunning("TEST_DEPT_SALES", 9005L);
        assertEquals("RUNNING", sales.getStatus(), "部门匹配节点应生成待办");
        assertEquals(2L, sales.getSubmitterDeptId().longValue());
        assertEquals(1, pendingTasks(salesInstance).size());
        assertTrue(otherInstance > 0);
    }

    @Test
    void overdueTask_shouldBeMarkedByReminderScan() {
        createProcess("TEST_OVERDUE", "TEST_OVERDUE",
                node(1, "USER", "3", "OR", null, 1));
        Long instanceId = workflowService.submit("TEST_OVERDUE", 9006L, "TEST-OVERDUE-1", new BigDecimal("100"));
        WfTask task = pendingTasks(instanceId).get(0);
        assertNotNull(task.getDueAt());

        wfTaskMapper.update(null, new LambdaUpdateWrapper<WfTask>()
                .eq(WfTask::getId, task.getId())
                .set(WfTask::getDueAt, LocalDateTime.now().minusHours(1))
                .set(WfTask::getOverdue, 0));
        workflowService.remindOverdueTasks();

        assertEquals(Integer.valueOf(1), wfTaskMapper.selectById(task.getId()).getOverdue());
    }

    private List<WfTask> pendingTasks(Long instanceId) {
        return wfTaskMapper.selectList(new LambdaQueryWrapper<WfTask>()
                .eq(WfTask::getInstanceId, instanceId)
                .eq(WfTask::getStatus, "PENDING")
                .orderByAsc(WfTask::getId));
    }

    private Long createProcess(String code, String bizType, ProcessSaveRequest.Node node) {
        ProcessSaveRequest request = new ProcessSaveRequest();
        request.setCode(code);
        request.setName(code);
        request.setBizType(bizType);
        request.setEnabled(1);
        request.setNodes(new ArrayList<>(List.of(node)));
        return workflowService.createProcess(request);
    }

    private ProcessSaveRequest.Node node(Integer seq, String approverType, String approverIds,
                                         String signType, Long deptId, Integer timeoutHours) {
        ProcessSaveRequest.Node node = new ProcessSaveRequest.Node();
        node.setSeq(seq);
        node.setName("测试节点" + seq);
        node.setApproverType(approverType);
        node.setApproverIds(approverIds);
        node.setSignType(signType);
        node.setDeptId(deptId);
        node.setTimeoutHours(timeoutHours);
        node.setMinAmount(BigDecimal.ZERO);
        node.setMaxAmount(BigDecimal.ZERO);
        return node;
    }

    private void login(Long userId, String username, Long deptId, String... roles) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setRealName(username);
        user.setDeptId(deptId);
        user.setDataScope("ALL");
        user.setRoleCodes(new HashSet<>(Set.of(roles)));
        user.setPermissions(new HashSet<>(Set.of("*:*:*")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}

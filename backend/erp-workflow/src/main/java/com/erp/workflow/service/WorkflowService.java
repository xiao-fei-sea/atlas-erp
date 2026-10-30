package com.erp.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.security.LoginUser;
import com.erp.common.security.SecurityUtils;
import com.erp.common.util.MoneyUtils;
import com.erp.system.entity.SysRole;
import com.erp.system.entity.SysUser;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysUserMapper;
import com.erp.workflow.dto.ProcessSaveRequest;
import com.erp.workflow.dto.TaskView;
import com.erp.workflow.entity.WfHistory;
import com.erp.workflow.entity.WfInstance;
import com.erp.workflow.entity.WfNode;
import com.erp.workflow.entity.WfProcess;
import com.erp.workflow.entity.WfTask;
import com.erp.workflow.event.ApprovalResultEvent;
import com.erp.workflow.mapper.WfHistoryMapper;
import com.erp.workflow.mapper.WfInstanceMapper;
import com.erp.workflow.mapper.WfNodeMapper;
import com.erp.workflow.mapper.WfProcessMapper;
import com.erp.workflow.mapper.WfTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WfProcessMapper processMapper;
    private final WfNodeMapper nodeMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfHistoryMapper historyMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    public PageResult<WfProcess> pageProcesses(long page, long size, String bizType) {
        LambdaQueryWrapper<WfProcess> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(bizType), WfProcess::getBizType, bizType).orderByAsc(WfProcess::getId);
        return PageResult.of(processMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Map<String, Object> processDetail(Long id) {
        WfProcess process = processMapper.selectById(id);
        if (process == null) {
            throw BizException.notFound("审批流程不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("process", process);
        data.put("nodes", nodesOf(id));
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createProcess(ProcessSaveRequest request) {
        validateProcess(request, null);
        WfProcess process = new WfProcess();
        process.setCode(request.getCode());
        process.setName(request.getName());
        process.setBizType(request.getBizType());
        process.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        process.setRemark(request.getRemark());
        processMapper.insert(process);
        saveNodes(process.getId(), request.getNodes());
        return process.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProcess(Long id, ProcessSaveRequest request) {
        WfProcess process = processMapper.selectById(id);
        if (process == null) {
            throw BizException.notFound("审批流程不存在");
        }
        validateProcess(request, id);
        process.setCode(request.getCode());
        process.setName(request.getName());
        process.setBizType(request.getBizType());
        process.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        process.setRemark(request.getRemark());
        processMapper.updateById(process);
        nodeMapper.delete(new LambdaQueryWrapper<WfNode>().eq(WfNode::getProcessId, id));
        saveNodes(id, request.getNodes());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProcess(Long id) {
        if (processMapper.selectById(id) == null) {
            throw BizException.notFound("审批流程不存在");
        }
        long running = instanceMapper.selectCount(new LambdaQueryWrapper<WfInstance>()
                .eq(WfInstance::getProcessId, id).eq(WfInstance::getStatus, "RUNNING"));
        if (running > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "存在运行中的审批实例，不能删除");
        }
        nodeMapper.delete(new LambdaQueryWrapper<WfNode>().eq(WfNode::getProcessId, id));
        processMapper.deleteById(id);
    }

    public List<TaskView> myPendingTasks() {
        LoginUser user = SecurityUtils.getLoginUser();
        if (SecurityUtils.isAdmin()) {
            return taskMapper.selectTasks("PENDING", null, null);
        }
        return taskMapper.selectTasks("PENDING", user.getUserId(), new ArrayList<>(user.getRoleCodes()));
    }

    public PageResult<WfInstance> pageInstances(long page, long size, String status, String bizType) {
        LambdaQueryWrapper<WfInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), WfInstance::getStatus, status)
                .eq(StringUtils.hasText(bizType), WfInstance::getBizType, bizType)
                .orderByDesc(WfInstance::getId);
        return PageResult.of(instanceMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Map<String, Object> instanceDetail(Long id) {
        WfInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw BizException.notFound("审批实例不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("instance", instance);
        data.put("tasks", taskMapper.selectList(new LambdaQueryWrapper<WfTask>()
                .eq(WfTask::getInstanceId, id).orderByAsc(WfTask::getNodeSeq)));
        data.put("history", historyMapper.selectList(new LambdaQueryWrapper<WfHistory>()
                .eq(WfHistory::getInstanceId, id).orderByAsc(WfHistory::getId)));
        WfProcess process = processMapper.selectById(instance.getProcessId());
        data.put("processName", process == null ? "" : process.getName());
        return data;
    }

    /**
     * 提交审批：按金额匹配节点；无匹配节点时自动通过。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(String bizType, Long bizId, String bizNo, BigDecimal amount) {
        WfProcess process = processMapper.selectOne(new LambdaQueryWrapper<WfProcess>()
                .eq(WfProcess::getBizType, bizType).eq(WfProcess::getEnabled, 1)
                .orderByAsc(WfProcess::getId).last("LIMIT 1"));
        if (process == null) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "未配置 " + bizType + " 的审批流程");
        }
        BigDecimal total = MoneyUtils.nz(amount);
        LoginUser submitter = SecurityUtils.getLoginUser();
        List<WfNode> nodes = applicableNodes(process.getId(), total, submitter.getDeptId());
        WfInstance instance = new WfInstance();
        instance.setProcessId(process.getId());
        instance.setBizType(bizType);
        instance.setBizId(bizId);
        instance.setBizNo(bizNo);
        instance.setAmount(total);
        instance.setSubmitterId(submitter.getUserId());
        instance.setSubmitterDeptId(submitter.getDeptId());
        instance.setCurrentSeq(nodes.isEmpty() ? 0 : nodes.get(0).getSeq());
        if (nodes.isEmpty()) {
            instance.setStatus("APPROVED");
            instance.setFinishedAt(LocalDateTime.now());
            instanceMapper.insert(instance);
            history(instance.getId(), 0, "AUTO_APPROVE", "无匹配审批节点，自动通过");
            eventPublisher.publishEvent(new ApprovalResultEvent(this, instance.getId(), bizType, bizId, bizNo, "APPROVED"));
        } else {
            instance.setStatus("RUNNING");
            instanceMapper.insert(instance);
            createTasks(instance, nodes.get(0));
            history(instance.getId(), 0, "SUBMIT", "提交审批");
        }
        return instance.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long taskId, String opinion) {
        WfTask task = getTask(taskId);
        WfInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null || !"RUNNING".equals(instance.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "审批实例不在运行中");
        }
        checkApprover(task);
        if (taskMapper.act(taskId, "APPROVED", opinion, SecurityUtils.getUserId()) == 0) {
            throw new BizException(ErrorCode.CONFLICT, "该任务已被处理");
        }
        history(instance.getId(), task.getNodeSeq(), "APPROVE", opinion);
        if ("AND".equals(normalizeSignType(task.getSignType()))
                && taskMapper.countPendingAtNode(instance.getId(), task.getNodeSeq()) > 0) {
            // 会签节点：等待其他审批人处理完成
            return;
        }
        if ("OR".equals(normalizeSignType(task.getSignType()))) {
            // 或签节点：一人通过后其余待办自动取消
            taskMapper.cancelPendingAtNode(instance.getId(), task.getNodeSeq());
        }
        advanceToNextNode(instance, task.getNodeSeq());
    }

    /**
     * 转交审批任务：将待办转给其他用户处理，并记录原审批人与转交历史。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long taskId, Long targetUserId, String reason) {
        if (targetUserId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择转交目标用户");
        }
        WfTask task = getTask(taskId);
        WfInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null || !"RUNNING".equals(instance.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "审批实例不在运行中");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "该任务已被处理");
        }
        checkApprover(task);
        SysUser target = userMapper.selectById(targetUserId);
        if (target == null || (target.getStatus() != null && target.getStatus() != 1)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "转交目标用户不存在或已停用");
        }
        if (targetUserId.equals(SecurityUtils.getUserId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能转交给自己");
        }
        if (taskMapper.transfer(taskId, targetUserId, SecurityUtils.getUserId()) == 0) {
            throw new BizException(ErrorCode.CONFLICT, "该任务已被处理");
        }
        String note = "转交给 " + target.getRealName();
        if (StringUtils.hasText(reason)) {
            note = note + "：" + reason;
        }
        history(instance.getId(), task.getNodeSeq(), "TRANSFER", note);
    }

    /** 扫描并标记超时未处理的待办任务。 */
    @Transactional(rollbackFor = Exception.class)
    public int remindOverdueTasks() {
        return taskMapper.markOverdue();
    }

    /** 转交目标用户选项。 */
    public List<Map<String, Object>> transferOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (SysUser user : userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1).orderByAsc(SysUser::getId))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("name", user.getRealName());
            item.put("deptId", user.getDeptId());
            options.add(item);
        }
        return options;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, String opinion) {
        WfTask task = getTask(taskId);
        WfInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null || !"RUNNING".equals(instance.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "审批实例不在运行中");
        }
        checkApprover(task);
        if (taskMapper.act(taskId, "REJECTED", opinion, SecurityUtils.getUserId()) == 0) {
            throw new BizException(ErrorCode.CONFLICT, "该任务已被处理");
        }
        taskMapper.cancelPending(instance.getId());
        instanceMapper.advance(instance.getId(), "RUNNING", "REJECTED", task.getNodeSeq(), LocalDateTime.now());
        history(instance.getId(), task.getNodeSeq(), "REJECT", opinion);
        eventPublisher.publishEvent(new ApprovalResultEvent(this, instance.getId(), instance.getBizType(),
                instance.getBizId(), instance.getBizNo(), "REJECTED"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long instanceId, String reason) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw BizException.notFound("审批实例不存在");
        }
        if (!SecurityUtils.isAdmin() && !instance.getSubmitterId().equals(SecurityUtils.getUserId())) {
            throw BizException.forbidden("只能撤回自己提交的审批");
        }
        if (instanceMapper.advance(instanceId, "RUNNING", "CANCELLED", instance.getCurrentSeq(),
                LocalDateTime.now()) == 0) {
            throw new BizException(ErrorCode.CONFLICT, "仅运行中的审批可撤回");
        }
        taskMapper.cancelPending(instanceId);
        history(instanceId, instance.getCurrentSeq(), "CANCEL", reason);
        eventPublisher.publishEvent(new ApprovalResultEvent(this, instanceId, instance.getBizType(),
                instance.getBizId(), instance.getBizNo(), "CANCELLED"));
    }

    public WfInstance findRunning(String bizType, Long bizId) {
        return instanceMapper.selectOne(new LambdaQueryWrapper<WfInstance>()
                .eq(WfInstance::getBizType, bizType).eq(WfInstance::getBizId, bizId)
                .orderByDesc(WfInstance::getId).last("LIMIT 1"));
    }

    private void checkApprover(WfTask task) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (SecurityUtils.isAdmin()) {
            return;
        }
        boolean allowed = "USER".equals(task.getApproverType())
                ? user.getUserId().equals(task.getApproverId())
                : user.getRoleCodes().contains(task.getApproverRole());
        if (!allowed) {
            throw BizException.forbidden("当前用户无权处理该审批任务");
        }
    }

    private void advanceToNextNode(WfInstance instance, Integer currentSeq) {
        List<WfNode> nodes = applicableNodes(instance.getProcessId(), MoneyUtils.nz(instance.getAmount()),
                instance.getSubmitterDeptId());
        WfNode next = nodes.stream().filter(n -> n.getSeq() > currentSeq)
                .min(Comparator.comparing(WfNode::getSeq)).orElse(null);
        if (next == null) {
            instanceMapper.advance(instance.getId(), "RUNNING", "APPROVED", currentSeq, LocalDateTime.now());
            eventPublisher.publishEvent(new ApprovalResultEvent(this, instance.getId(), instance.getBizType(),
                    instance.getBizId(), instance.getBizNo(), "APPROVED"));
        } else {
            instanceMapper.advance(instance.getId(), "RUNNING", "RUNNING", next.getSeq(), null);
            instance.setCurrentSeq(next.getSeq());
            createTasks(instance, next);
        }
    }

    private void createTasks(WfInstance instance, WfNode node) {
        String signType = normalizeSignType(node.getSignType());
        Integer timeoutHours = node.getTimeoutHours();
        LocalDateTime dueAt = timeoutHours != null && timeoutHours > 0
                ? LocalDateTime.now().plusHours(timeoutHours) : null;
        for (Long approverId : resolveApproverIds(node)) {
            WfTask task = new WfTask();
            task.setInstanceId(instance.getId());
            task.setNodeSeq(node.getSeq());
            task.setNodeName(node.getName());
            task.setSignType(signType);
            task.setApproverType(node.getApproverType());
            if ("ROLE".equals(node.getApproverType())) {
                SysRole role = roleMapper.selectById(approverId);
                if (role == null) {
                    throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "审批节点角色不存在");
                }
                task.setApproverRole(role.getCode());
            } else {
                task.setApproverId(approverId);
            }
            task.setStatus("PENDING");
            task.setDueAt(dueAt);
            task.setOverdue(0);
            taskMapper.insert(task);
        }
    }

    /** 解析节点审批人，approverIds 优先，未配置时回退到 approverId。 */
    private List<Long> resolveApproverIds(WfNode node) {
        List<Long> ids = new ArrayList<>();
        if (StringUtils.hasText(node.getApproverIds())) {
            for (String part : node.getApproverIds().split(",")) {
                String text = part.trim();
                if (!text.isEmpty()) {
                    try {
                        Long id = Long.valueOf(text);
                        if (!ids.contains(id)) {
                            ids.add(id);
                        }
                    } catch (NumberFormatException ignored) {
                        // 忽略非法审批人ID
                    }
                }
            }
        }
        if (ids.isEmpty() && node.getApproverId() != null) {
            ids.add(node.getApproverId());
        }
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "审批节点未配置审批人");
        }
        return ids;
    }

    private String normalizeSignType(String signType) {
        return "AND".equals(signType) ? "AND" : "OR";
    }

    private List<WfNode> applicableNodes(Long processId, BigDecimal amount, Long deptId) {
        List<WfNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<WfNode>()
                .eq(WfNode::getProcessId, processId).orderByAsc(WfNode::getSeq));
        List<WfNode> matched = new ArrayList<>();
        for (WfNode node : nodes) {
            if (node.getDeptId() != null && node.getDeptId() != 0
                    && (deptId == null || !node.getDeptId().equals(deptId))) {
                continue;
            }
            BigDecimal min = MoneyUtils.nz(node.getMinAmount());
            BigDecimal max = MoneyUtils.nz(node.getMaxAmount());
            boolean upperOk = max.compareTo(BigDecimal.ZERO) == 0 || amount.compareTo(max) <= 0;
            if (amount.compareTo(min) >= 0 && upperOk) {
                matched.add(node);
            }
        }
        return matched;
    }

    private void saveNodes(Long processId, List<ProcessSaveRequest.Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        int seq = 0;
        for (ProcessSaveRequest.Node source : nodes) {
            seq++;
            WfNode node = new WfNode();
            node.setProcessId(processId);
            node.setSeq(source.getSeq() == null ? seq : source.getSeq());
            node.setName(source.getName());
            node.setApproverType(StringUtils.hasText(source.getApproverType()) ? source.getApproverType() : "ROLE");
            Long approverId = source.getApproverId();
            if (approverId == null && StringUtils.hasText(source.getApproverIds())) {
                for (String part : source.getApproverIds().split(",")) {
                    String text = part.trim();
                    if (!text.isEmpty()) {
                        try {
                            approverId = Long.valueOf(text);
                            break;
                        } catch (NumberFormatException ignored) {
                            // 忽略非法审批人ID
                        }
                    }
                }
            }
            if (approverId == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "审批节点未配置审批人");
            }
            node.setApproverId(approverId);
            node.setSignType(normalizeSignType(source.getSignType()));
            node.setApproverIds(source.getApproverIds());
            node.setDeptId(source.getDeptId());
            node.setTimeoutHours(source.getTimeoutHours() == null ? 0 : source.getTimeoutHours());
            node.setMinAmount(MoneyUtils.nz(source.getMinAmount()));
            node.setMaxAmount(MoneyUtils.nz(source.getMaxAmount()));
            nodeMapper.insert(node);
        }
    }

    private List<WfNode> nodesOf(Long processId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<WfNode>()
                .eq(WfNode::getProcessId, processId).orderByAsc(WfNode::getSeq));
    }

    private void validateProcess(ProcessSaveRequest request, Long id) {
        if (!StringUtils.hasText(request.getCode()) || !StringUtils.hasText(request.getName())
                || !StringUtils.hasText(request.getBizType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "流程编码、名称与业务类型不能为空");
        }
        long duplicated = processMapper.selectCount(new LambdaQueryWrapper<WfProcess>()
                .eq(WfProcess::getCode, request.getCode()).ne(id != null, WfProcess::getId, id));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "流程编码已存在");
        }
    }

    private WfTask getTask(Long taskId) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BizException.notFound("审批任务不存在");
        }
        return task;
    }

    private void history(Long instanceId, Integer seq, String action, String opinion) {
        WfHistory history = new WfHistory();
        history.setInstanceId(instanceId);
        history.setNodeSeq(seq);
        history.setAction(action);
        history.setOperatorId(SecurityUtils.getUserId());
        history.setOperatorName(SecurityUtils.getUsername());
        history.setOpinion(opinion);
        history.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(history);
    }
}

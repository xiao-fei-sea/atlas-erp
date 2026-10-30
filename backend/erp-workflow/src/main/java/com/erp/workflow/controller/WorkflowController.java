package com.erp.workflow.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.workflow.dto.ProcessSaveRequest;
import com.erp.workflow.dto.TaskView;
import com.erp.workflow.entity.WfInstance;
import com.erp.workflow.entity.WfProcess;
import com.erp.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/processes")
    @PreAuthorize("hasAuthority('workflow:process:view')")
    public Result<PageResult<WfProcess>> processes(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String bizType) {
        return Result.ok(workflowService.pageProcesses(page, size, bizType));
    }

    @GetMapping("/processes/{id}")
    @PreAuthorize("hasAuthority('workflow:process:view')")
    public Result<Map<String, Object>> processDetail(@PathVariable Long id) {
        return Result.ok(workflowService.processDetail(id));
    }

    @PostMapping("/processes")
    @PreAuthorize("hasAuthority('workflow:process:create')")
    @OperLog(module = "审批中心", action = "新增审批流程")
    public Result<Long> createProcess(@RequestBody ProcessSaveRequest request) {
        return Result.ok(workflowService.createProcess(request));
    }

    @PutMapping("/processes/{id}")
    @PreAuthorize("hasAuthority('workflow:process:update')")
    @OperLog(module = "审批中心", action = "修改审批流程")
    public Result<Void> updateProcess(@PathVariable Long id, @RequestBody ProcessSaveRequest request) {
        workflowService.updateProcess(id, request);
        return Result.ok();
    }

    @DeleteMapping("/processes/{id}")
    @PreAuthorize("hasAuthority('workflow:process:update')")
    @OperLog(module = "审批中心", action = "删除审批流程")
    public Result<Void> deleteProcess(@PathVariable Long id) {
        workflowService.deleteProcess(id);
        return Result.ok();
    }

    @GetMapping("/tasks/pending")
    @PreAuthorize("hasAuthority('workflow:task:view')")
    public Result<List<TaskView>> pendingTasks() {
        return Result.ok(workflowService.myPendingTasks());
    }

    @PostMapping("/tasks/{id}/approve")
    @PreAuthorize("hasAuthority('workflow:task:view')")
    @OperLog(module = "审批中心", action = "审批通过")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        workflowService.approve(id, body == null ? null : body.get("opinion"));
        return Result.ok();
    }

    @PostMapping("/tasks/{id}/reject")
    @PreAuthorize("hasAuthority('workflow:task:view')")
    @OperLog(module = "审批中心", action = "审批驳回")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        workflowService.reject(id, body == null ? null : body.get("opinion"));
        return Result.ok();
    }

    @GetMapping("/tasks/transfer-options")
    @PreAuthorize("hasAuthority('workflow:task:transfer')")
    public Result<List<Map<String, Object>>> transferOptions() {
        return Result.ok(workflowService.transferOptions());
    }

    @PostMapping("/tasks/{id}/transfer")
    @PreAuthorize("hasAuthority('workflow:task:transfer')")
    @OperLog(module = "审批中心", action = "转交审批任务")
    public Result<Void> transfer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long targetUserId = body == null || body.get("targetUserId") == null
                ? null : Long.valueOf(body.get("targetUserId").toString());
        String reason = body == null || body.get("reason") == null ? null : body.get("reason").toString();
        workflowService.transfer(id, targetUserId, reason);
        return Result.ok();
    }

    @GetMapping("/instances")
    @PreAuthorize("hasAuthority('workflow:instance:view')")
    public Result<PageResult<WfInstance>> instances(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String bizType) {
        return Result.ok(workflowService.pageInstances(page, size, status, bizType));
    }

    @GetMapping("/instances/{id}")
    @PreAuthorize("hasAuthority('workflow:instance:view')")
    public Result<Map<String, Object>> instanceDetail(@PathVariable Long id) {
        return Result.ok(workflowService.instanceDetail(id));
    }

    @PutMapping("/instances/{id}/cancel")
    @PreAuthorize("hasAuthority('workflow:instance:view')")
    @OperLog(module = "审批中心", action = "撤回审批")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        workflowService.cancel(id, body == null ? null : body.get("reason"));
        return Result.ok();
    }
}

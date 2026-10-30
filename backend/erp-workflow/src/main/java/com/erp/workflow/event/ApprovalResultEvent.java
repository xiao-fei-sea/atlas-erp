package com.erp.workflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 审批结果事件：业务模块监听后更新自身单据状态。
 */
@Getter
public class ApprovalResultEvent extends ApplicationEvent {

    private final Long instanceId;
    private final String bizType;
    private final Long bizId;
    private final String bizNo;
    private final String status;

    public ApprovalResultEvent(Object source, Long instanceId, String bizType, Long bizId, String bizNo, String status) {
        super(source);
        this.instanceId = instanceId;
        this.bizType = bizType;
        this.bizId = bizId;
        this.bizNo = bizNo;
        this.status = status;
    }
}

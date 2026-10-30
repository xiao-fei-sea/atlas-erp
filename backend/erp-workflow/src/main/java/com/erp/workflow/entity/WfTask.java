package com.erp.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_task")
public class WfTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Integer nodeSeq;
    private String nodeName;
    private String signType;
    private String approverType;
    private Long approverId;
    private String approverRole;
    private Long originApproverId;
    private Long transferredBy;
    private LocalDateTime transferredAt;
    private LocalDateTime dueAt;
    private Integer overdue;
    private String status;
    private String opinion;
    private Long actedBy;
    private LocalDateTime actedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

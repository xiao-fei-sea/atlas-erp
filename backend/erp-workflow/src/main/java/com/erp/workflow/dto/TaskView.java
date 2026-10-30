package com.erp.workflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaskView {

    private Long id;
    private Long instanceId;
    private Integer nodeSeq;
    private String nodeName;
    private String signType;
    private String approverType;
    private Long approverId;
    private String approverRole;
    private String status;
    private String opinion;
    private LocalDateTime dueAt;
    private Integer overdue;
    private LocalDateTime createdAt;
    private String bizType;
    private String bizNo;
    private BigDecimal amount;
    private Long submitterId;
    private String submitterName;
}

package com.erp.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_instance")
public class WfInstance extends BaseEntity {

    private Long processId;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private BigDecimal amount;
    private Long submitterId;
    private Long submitterDeptId;
    private String status;
    private Integer currentSeq;
    private LocalDateTime finishedAt;
}

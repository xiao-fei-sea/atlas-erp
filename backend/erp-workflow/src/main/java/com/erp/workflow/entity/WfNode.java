package com.erp.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node")
public class WfNode extends BaseEntity {

    private Long processId;
    private Integer seq;
    private String name;
    private String approverType;
    private Long approverId;
    private String signType;
    private String approverIds;
    private Long deptId;
    private Integer timeoutHours;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}

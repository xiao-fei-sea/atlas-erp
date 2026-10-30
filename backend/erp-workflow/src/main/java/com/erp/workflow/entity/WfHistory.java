package com.erp.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_history")
public class WfHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Integer nodeSeq;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String opinion;
    private LocalDateTime createdAt;
}

package com.erp.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process")
public class WfProcess extends BaseEntity {

    private String code;
    private String name;
    private String bizType;
    private Integer enabled;
    private String remark;
}

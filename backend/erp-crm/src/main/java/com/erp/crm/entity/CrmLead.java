package com.erp.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead")
public class CrmLead extends BaseEntity {

    private String name;
    private String company;
    private String phone;
    private String source;
    private String status;
    private Long ownerId;
    private Long customerId;
    private String remark;
}

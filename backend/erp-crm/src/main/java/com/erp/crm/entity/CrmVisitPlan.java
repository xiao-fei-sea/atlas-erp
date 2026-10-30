package com.erp.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_visit_plan")
public class CrmVisitPlan extends BaseEntity {

    private Long customerId;
    private LocalDate planDate;
    private String purpose;
    private String status;
    private Long ownerId;
}

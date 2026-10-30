package com.erp.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class CrmOpportunity extends BaseEntity {

    private Long customerId;
    private String name;
    private BigDecimal amount;
    private String stage;
    private Integer probability;
    private LocalDate expectedCloseDate;
    private Long ownerId;
    private String remark;
}

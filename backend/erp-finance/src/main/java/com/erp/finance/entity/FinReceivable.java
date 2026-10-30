package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_receivable")
public class FinReceivable extends BaseEntity {

    private String receivableNo;
    private Long customerId;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private BigDecimal amount;
    private BigDecimal settledAmount;
    private String status;
    private LocalDate dueDate;
    private String remark;
}

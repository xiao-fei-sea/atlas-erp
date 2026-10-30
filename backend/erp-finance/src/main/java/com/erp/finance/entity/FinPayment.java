package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_payment")
public class FinPayment extends BaseEntity {

    private String paymentNo;
    private Long supplierId;
    private Long accountId;
    private BigDecimal amount;
    private BigDecimal usedAmount;
    private LocalDate paymentDate;
    private String status;
    private String remark;
}

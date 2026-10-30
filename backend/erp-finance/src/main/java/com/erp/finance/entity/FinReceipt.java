package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_receipt")
public class FinReceipt extends BaseEntity {

    private String receiptNo;
    private Long customerId;
    private Long accountId;
    private BigDecimal amount;
    private BigDecimal usedAmount;
    private LocalDate receiptDate;
    private String status;
    private String remark;
}

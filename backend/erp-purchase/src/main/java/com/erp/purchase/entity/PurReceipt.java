package com.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pur_receipt")
public class PurReceipt extends BaseEntity {

    private String receiptNo;
    private Long orderId;
    private Long supplierId;
    private Long warehouseId;
    private LocalDate receiptDate;
    private String status;
    private BigDecimal totalAmount;
    private String remark;
}

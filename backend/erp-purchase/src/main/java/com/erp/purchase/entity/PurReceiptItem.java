package com.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pur_receipt_item")
public class PurReceiptItem extends BaseEntity {

    private Long receiptId;
    private Long orderItemId;
    private Long productId;
    private String batchNo;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal amount;
}

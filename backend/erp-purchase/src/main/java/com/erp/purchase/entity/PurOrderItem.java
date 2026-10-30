package com.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pur_order_item")
public class PurOrderItem extends BaseEntity {

    private Long orderId;
    private Long productId;
    private BigDecimal qty;
    private BigDecimal receivedQty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
}

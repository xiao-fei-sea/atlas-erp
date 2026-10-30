package com.erp.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sal_order_item")
public class SalOrderItem extends BaseEntity {

    private Long orderId;
    private Long productId;
    private BigDecimal qty;
    private BigDecimal shippedQty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
}

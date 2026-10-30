package com.erp.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sal_shipment_item")
public class SalShipmentItem extends BaseEntity {

    private Long shipmentId;
    private Long orderItemId;
    private Long productId;
    private String batchNo;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal amount;
}

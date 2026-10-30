package com.erp.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sal_shipment")
public class SalShipment extends BaseEntity {

    private String shipmentNo;
    private Long orderId;
    private Long customerId;
    private Long warehouseId;
    private LocalDate shipmentDate;
    private String status;
    private BigDecimal totalAmount;
    private String remark;
}

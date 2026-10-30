package com.erp.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlowView {

    private Long id;
    private String flowNo;
    private String bizType;
    private String bizNo;
    private Long productId;
    private String productCode;
    private String productName;
    private String warehouseName;
    private String batchNo;
    private BigDecimal qtyChange;
    private BigDecimal qtyBefore;
    private BigDecimal qtyAfter;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createdAt;
}

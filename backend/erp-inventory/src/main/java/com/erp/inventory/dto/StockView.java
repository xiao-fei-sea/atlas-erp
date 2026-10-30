package com.erp.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockView {

    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;
    private Long unitId;
    private Long warehouseId;
    private String warehouseName;
    private String batchNo;
    private LocalDate expiryDate;
    private BigDecimal qtyOnHand;
    private BigDecimal qtyReserved;
    private BigDecimal qtyFrozen;
    private BigDecimal availableQty;
    private BigDecimal avgCost;
    private Integer version;
}

package com.erp.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockChangeItem {

    private Long productId;
    private String batchNo;
    private BigDecimal qty;
    private BigDecimal unitCost;
    private String remark;
}

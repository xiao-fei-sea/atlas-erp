package com.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseOrderRequest {

    private Long supplierId;
    private LocalDate orderDate;
    private String remark;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long productId;
        private BigDecimal qty;
        private BigDecimal price;
        private BigDecimal taxRate;
    }
}

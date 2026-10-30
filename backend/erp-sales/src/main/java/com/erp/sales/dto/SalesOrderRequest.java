package com.erp.sales.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class SalesOrderRequest {

    private Long customerId;
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

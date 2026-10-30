package com.erp.sales.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class SalesShipmentRequest {

    private Long orderId;
    private Long warehouseId;
    private LocalDate shipmentDate;
    private String remark;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long orderItemId;
        private String batchNo;
        private BigDecimal qty;
    }
}

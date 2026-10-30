package com.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseReceiptRequest {

    private Long orderId;
    private Long warehouseId;
    private LocalDate receiptDate;
    private String remark;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long orderItemId;
        private String batchNo;
        private BigDecimal qty;
    }
}

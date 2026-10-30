package com.erp.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TransferRequest {

    private Long fromWarehouseId;
    private Long toWarehouseId;
    private LocalDate transferDate;
    private String remark;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long productId;
        private String batchNo;
        private BigDecimal qty;
    }
}

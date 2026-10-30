package com.erp.inventory.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class StocktakeRequest {

    private Long warehouseId;
    private LocalDate stocktakeDate;
    private String remark;
    private Boolean loadStocks = Boolean.TRUE;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long productId;
        private String batchNo;
        private java.math.BigDecimal actualQty;
    }
}

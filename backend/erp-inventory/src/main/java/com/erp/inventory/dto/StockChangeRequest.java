package com.erp.inventory.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StockChangeRequest {

    private Long warehouseId;
    private String bizNo;
    private String refType;
    private Long refId;
    private String remark;
    private List<StockChangeItem> items = new ArrayList<>();
}

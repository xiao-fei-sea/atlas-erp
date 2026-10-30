package com.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReceiptRequest {

    private Long customerId;
    private Long accountId;
    private BigDecimal amount;
    private LocalDate receiptDate;
    private String remark;
    private List<Allocation> allocations = new ArrayList<>();

    @Data
    public static class Allocation {
        private Long receivableId;
        private BigDecimal amount;
    }
}

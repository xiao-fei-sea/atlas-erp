package com.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentRequest {

    private Long supplierId;
    private Long accountId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String remark;
    private List<Allocation> allocations = new ArrayList<>();

    @Data
    public static class Allocation {
        private Long payableId;
        private BigDecimal amount;
    }
}

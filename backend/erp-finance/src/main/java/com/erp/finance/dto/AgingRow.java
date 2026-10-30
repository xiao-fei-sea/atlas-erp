package com.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账龄分析行：按往来单位聚合。
 */
@Data
public class AgingRow {

    private Long partnerId;
    private String partnerName;
    private BigDecimal notDue;
    private BigDecimal overdue1to30;
    private BigDecimal overdue31to60;
    private BigDecimal overdue61to90;
    private BigDecimal overdueOver90;
    private BigDecimal total;
}

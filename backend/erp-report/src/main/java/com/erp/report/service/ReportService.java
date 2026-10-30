package com.erp.report.service;

import com.erp.common.util.CsvUtils;
import com.erp.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;

    public Map<String, Object> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", scaleRow(reportMapper.dashboardSummary()));
        result.put("salesTrend", scaleRows(reportMapper.salesTrend()));
        result.put("purchaseTrend", scaleRows(reportMapper.purchaseTrend()));
        result.put("topProducts", scaleRows(reportMapper.topProducts(5)));
        result.put("salesStatus", scaleRows(reportMapper.salesStatusDistribution()));
        return result;
    }

    public Map<String, Object> salesReport(String startDate, String endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", scaleRow(reportMapper.salesSummary(startDate, endDate)));
        result.put("byDay", scaleRows(reportMapper.salesByDay(startDate, endDate)));
        result.put("byProduct", scaleRows(reportMapper.salesByProduct(startDate, endDate)));
        result.put("byCustomer", scaleRows(reportMapper.salesByCustomer(startDate, endDate)));
        result.put("byStatus", scaleRows(reportMapper.salesStatus(startDate, endDate)));
        return result;
    }

    public Map<String, Object> purchaseReport(String startDate, String endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", scaleRow(reportMapper.purchaseSummary(startDate, endDate)));
        result.put("byDay", scaleRows(reportMapper.purchaseByDay(startDate, endDate)));
        result.put("byProduct", scaleRows(reportMapper.purchaseByProduct(startDate, endDate)));
        result.put("bySupplier", scaleRows(reportMapper.purchaseBySupplier(startDate, endDate)));
        result.put("byStatus", scaleRows(reportMapper.purchaseStatus(startDate, endDate)));
        return result;
    }

    public Map<String, Object> inventoryReport(int days) {
        int range = Math.max(1, Math.min(days, 365));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", scaleRow(reportMapper.stockValueSummary()));
        result.put("byWarehouse", scaleRows(reportMapper.stockByWarehouse()));
        result.put("byCategory", scaleRows(reportMapper.stockByCategory()));
        result.put("lowStock", scaleRows(reportMapper.lowStockList()));
        result.put("flowSummary", scaleRows(reportMapper.flowSummary(range)));
        return result;
    }

    public byte[] exportSales(String startDate, String endDate) {
        List<String> headers = List.of("订单号", "客户", "订单日期", "状态", "金额", "税额");
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> row : reportMapper.salesOrderRows(startDate, endDate)) {
            rows.add(List.of(
                    str(row.get("orderNo")),
                    str(row.get("customerName")),
                    str(row.get("orderDate")),
                    str(row.get("status")),
                    str(row.get("totalAmount")),
                    str(row.get("taxAmount"))));
        }
        return CsvUtils.toCsv(headers, rows);
    }

    public byte[] exportPurchase(String startDate, String endDate) {
        List<String> headers = List.of("订单号", "供应商", "订单日期", "状态", "金额", "税额");
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> row : reportMapper.purchaseOrderRows(startDate, endDate)) {
            rows.add(List.of(
                    str(row.get("orderNo")),
                    str(row.get("supplierName")),
                    str(row.get("orderDate")),
                    str(row.get("status")),
                    str(row.get("totalAmount")),
                    str(row.get("taxAmount"))));
        }
        return CsvUtils.toCsv(headers, rows);
    }

    public byte[] exportLowStock() {
        List<String> headers = List.of("商品编码", "商品名称", "规格", "安全库存", "当前库存", "库存金额");
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> row : reportMapper.lowStockList()) {
            rows.add(List.of(
                    str(row.get("productCode")),
                    str(row.get("productName")),
                    str(row.get("spec")),
                    str(row.get("safetyStock")),
                    str(row.get("qtyOnHand")),
                    str(row.get("stockValue"))));
        }
        return CsvUtils.toCsv(headers, rows);
    }

    private Map<String, Object> scaleRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (row != null) {
            row.forEach((k, v) -> result.put(k, scaleValue(v)));
        }
        return result;
    }

    private List<Map<String, Object>> scaleRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (rows != null) {
            rows.forEach(row -> result.add(scaleRow(row)));
        }
        return result;
    }

    private Object scaleValue(Object value) {
        if (value instanceof BigDecimal amount) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        return value;
    }

    private String str(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal amount) {
            return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return String.valueOf(value);
    }
}

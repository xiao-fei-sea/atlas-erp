package com.erp.report.controller;

import com.erp.common.api.Result;
import com.erp.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('report:dashboard:view')")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(reportService.dashboard());
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('report:sales:view')")
    public Result<Map<String, Object>> sales(@RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate) {
        return Result.ok(reportService.salesReport(startDate, endDate));
    }

    @GetMapping("/purchase")
    @PreAuthorize("hasAuthority('report:purchase:view')")
    public Result<Map<String, Object>> purchase(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        return Result.ok(reportService.purchaseReport(startDate, endDate));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority('report:inventory:view')")
    public Result<Map<String, Object>> inventory(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(reportService.inventoryReport(days));
    }

    @GetMapping("/sales/export")
    @PreAuthorize("hasAuthority('report:sales:view')")
    public ResponseEntity<byte[]> exportSales(@RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate) {
        return csv(reportService.exportSales(startDate, endDate), "sales-report.csv");
    }

    @GetMapping("/purchase/export")
    @PreAuthorize("hasAuthority('report:purchase:view')")
    public ResponseEntity<byte[]> exportPurchase(@RequestParam(required = false) String startDate,
                                                 @RequestParam(required = false) String endDate) {
        return csv(reportService.exportPurchase(startDate, endDate), "purchase-report.csv");
    }

    @GetMapping("/inventory/export")
    @PreAuthorize("hasAuthority('report:inventory:view')")
    public ResponseEntity<byte[]> exportLowStock() {
        return csv(reportService.exportLowStock(), "low-stock.csv");
    }

    private ResponseEntity<byte[]> csv(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content);
    }
}

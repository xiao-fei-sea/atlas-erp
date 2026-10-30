package com.erp.sales.listener;

import com.erp.sales.service.SalesOrderService;
import com.erp.workflow.event.ApprovalResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 销售订单审批结果回写。
 */
@Component
@RequiredArgsConstructor
public class SalesApprovalListener {

    private final SalesOrderService salesOrderService;

    @EventListener
    public void onApprovalResult(ApprovalResultEvent event) {
        if (!"SAL_ORDER".equals(event.getBizType())) {
            return;
        }
        String target = switch (event.getStatus()) {
            case "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            case "CANCELLED" -> "DRAFT";
            default -> null;
        };
        if (target != null) {
            salesOrderService.applyApproval(event.getBizId(), target);
        }
    }
}

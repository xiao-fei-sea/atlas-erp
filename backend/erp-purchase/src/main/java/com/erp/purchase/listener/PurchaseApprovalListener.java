package com.erp.purchase.listener;

import com.erp.purchase.service.PurchaseOrderService;
import com.erp.workflow.event.ApprovalResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 采购订单审批结果回写。
 */
@Component
@RequiredArgsConstructor
public class PurchaseApprovalListener {

    private final PurchaseOrderService purchaseOrderService;

    @EventListener
    public void onApprovalResult(ApprovalResultEvent event) {
        if (!"PUR_ORDER".equals(event.getBizType())) {
            return;
        }
        String target = switch (event.getStatus()) {
            case "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            case "CANCELLED" -> "DRAFT";
            default -> null;
        };
        if (target != null) {
            purchaseOrderService.applyApproval(event.getBizId(), target);
        }
    }
}

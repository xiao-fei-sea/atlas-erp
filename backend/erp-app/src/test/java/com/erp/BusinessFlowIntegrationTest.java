package com.erp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.security.LoginUser;
import com.erp.finance.dto.PaymentRequest;
import com.erp.finance.dto.ReceiptRequest;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.mapper.FinPayableMapper;
import com.erp.finance.mapper.FinReceivableMapper;
import com.erp.finance.service.FinanceService;
import com.erp.inventory.service.StockService;
import com.erp.purchase.dto.PurchaseOrderRequest;
import com.erp.purchase.dto.PurchaseReceiptRequest;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.entity.PurOrderItem;
import com.erp.purchase.mapper.PurOrderItemMapper;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.purchase.service.PurchaseOrderService;
import com.erp.purchase.service.PurchaseReceiptService;
import com.erp.sales.dto.SalesOrderRequest;
import com.erp.sales.dto.SalesShipmentRequest;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.entity.SalOrderItem;
import com.erp.sales.mapper.SalOrderItemMapper;
import com.erp.sales.mapper.SalOrderMapper;
import com.erp.sales.service.SalesOrderService;
import com.erp.sales.service.SalesShipmentService;
import com.erp.workflow.entity.WfInstance;
import com.erp.workflow.entity.WfTask;
import com.erp.workflow.mapper.WfTaskMapper;
import com.erp.workflow.service.WorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 业务闭环集成测试：销售（订单→审批→发货→应收→收款→核销）与采购（订单→审批→收货→应付→付款→核销）。
 * 每个测试在事务内执行并回滚，不污染业务数据。
 */
@SpringBootTest
@Transactional
class BusinessFlowIntegrationTest {

    @Autowired
    private SalesOrderService salesOrderService;
    @Autowired
    private SalesShipmentService salesShipmentService;
    @Autowired
    private SalOrderMapper salOrderMapper;
    @Autowired
    private SalOrderItemMapper salOrderItemMapper;
    @Autowired
    private PurchaseOrderService purchaseOrderService;
    @Autowired
    private PurchaseReceiptService purchaseReceiptService;
    @Autowired
    private PurOrderMapper purOrderMapper;
    @Autowired
    private PurOrderItemMapper purOrderItemMapper;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private WfTaskMapper wfTaskMapper;
    @Autowired
    private FinanceService financeService;
    @Autowired
    private FinReceivableMapper receivableMapper;
    @Autowired
    private FinPayableMapper payableMapper;
    @Autowired
    private StockService stockService;

    @BeforeEach
    void loginAsAdmin() {
        LoginUser admin = new LoginUser();
        admin.setUserId(1L);
        admin.setUsername("admin");
        admin.setRealName("系统管理员");
        admin.setDeptId(1L);
        admin.setDataScope("ALL");
        admin.setRoleCodes(new HashSet<>(Set.of("ADMIN")));
        admin.setPermissions(new HashSet<>(Set.of("*:*:*")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void salesFlow_shouldCloseLoopFromOrderToSettlement() {
        // 1. 创建销售订单：10 件 * 89 元，税率 13%
        SalesOrderRequest order = new SalesOrderRequest();
        order.setCustomerId(1L);
        SalesOrderRequest.Item item = new SalesOrderRequest.Item();
        item.setProductId(1L);
        item.setQty(new BigDecimal("10"));
        item.setPrice(new BigDecimal("89"));
        item.setTaxRate(new BigDecimal("0.13"));
        order.getItems().add(item);
        Long orderId = salesOrderService.create(order);
        assertEquals("DRAFT", salOrderMapper.selectById(orderId).getStatus());
        assertEquals(0, salOrderMapper.selectById(orderId).getTotalAmount().compareTo(new BigDecimal("890")));

        // 2. 提交审批 → 审批通过
        salesOrderService.submit(orderId);
        assertEquals("PENDING", salOrderMapper.selectById(orderId).getStatus());
        WfInstance instance = workflowService.findRunning("SAL_ORDER", orderId);
        assertNotNull(instance);
        WfTask task = wfTaskMapper.selectOne(new LambdaQueryWrapper<WfTask>()
                .eq(WfTask::getInstanceId, instance.getId()).eq(WfTask::getStatus, "PENDING"));
        assertNotNull(task, "提交后应生成待办任务");
        workflowService.approve(task.getId(), "同意");
        assertEquals("APPROVED", salOrderMapper.selectById(orderId).getStatus());
        assertEquals("APPROVED", workflowService.findRunning("SAL_ORDER", orderId).getStatus());

        // 3. 发货并确认出库
        SalOrderItem orderItem = salOrderItemMapper.selectOne(new LambdaQueryWrapper<SalOrderItem>()
                .eq(SalOrderItem::getOrderId, orderId));
        BigDecimal availableBefore = stockService.available(1L, 1L, "");
        SalesShipmentRequest shipment = new SalesShipmentRequest();
        shipment.setOrderId(orderId);
        shipment.setWarehouseId(1L);
        SalesShipmentRequest.Item shipItem = new SalesShipmentRequest.Item();
        shipItem.setOrderItemId(orderItem.getId());
        shipItem.setBatchNo("");
        shipItem.setQty(new BigDecimal("10"));
        shipment.getItems().add(shipItem);
        Long shipmentId = salesShipmentService.create(shipment);
        salesShipmentService.confirm(shipmentId);

        assertEquals("SHIPPED", salOrderMapper.selectById(orderId).getStatus());
        assertEquals(0, stockService.available(1L, 1L, "").compareTo(availableBefore.subtract(new BigDecimal("10"))));

        // 4. 发货金额 = 890 + 115.70 税 = 1005.70，生成应收
        FinReceivable receivable = receivableMapper.selectOne(new LambdaQueryWrapper<FinReceivable>()
                .eq(FinReceivable::getSourceType, "SHIPMENT").eq(FinReceivable::getSourceId, shipmentId));
        assertNotNull(receivable, "发货确认后应生成应收");
        assertEquals(0, receivable.getAmount().compareTo(new BigDecimal("1005.70")));
        assertEquals("UNSETTLED", receivable.getStatus());

        // 5. 收款并全额核销 → 应收结清
        ReceiptRequest receipt = new ReceiptRequest();
        receipt.setCustomerId(1L);
        receipt.setAccountId(1L);
        receipt.setAmount(new BigDecimal("1005.70"));
        ReceiptRequest.Allocation allocation = new ReceiptRequest.Allocation();
        allocation.setReceivableId(receivable.getId());
        allocation.setAmount(new BigDecimal("1005.70"));
        receipt.getAllocations().add(allocation);
        financeService.createReceipt(receipt);

        FinReceivable settled = receivableMapper.selectById(receivable.getId());
        assertEquals("SETTLED", settled.getStatus());
        assertEquals(0, settled.getSettledAmount().compareTo(new BigDecimal("1005.70")));
    }

    @Test
    void purchaseFlow_shouldCloseLoopFromOrderToSettlement() {
        // 1. 创建采购订单：5 件 * 40 元，税率 13%
        PurchaseOrderRequest order = new PurchaseOrderRequest();
        order.setSupplierId(1L);
        PurchaseOrderRequest.Item item = new PurchaseOrderRequest.Item();
        item.setProductId(1L);
        item.setQty(new BigDecimal("5"));
        item.setPrice(new BigDecimal("40"));
        item.setTaxRate(new BigDecimal("0.13"));
        order.getItems().add(item);
        Long orderId = purchaseOrderService.create(order);
        assertEquals(0, purOrderMapper.selectById(orderId).getTotalAmount().compareTo(new BigDecimal("200")));

        // 2. 提交并审批通过
        purchaseOrderService.submit(orderId);
        assertEquals("PENDING", purOrderMapper.selectById(orderId).getStatus());
        WfInstance instance = workflowService.findRunning("PUR_ORDER", orderId);
        WfTask task = wfTaskMapper.selectOne(new LambdaQueryWrapper<WfTask>()
                .eq(WfTask::getInstanceId, instance.getId()).eq(WfTask::getStatus, "PENDING"));
        workflowService.approve(task.getId(), "同意");
        assertEquals("APPROVED", purOrderMapper.selectById(orderId).getStatus());

        // 3. 收货入库（独立批次，避免影响其他库存）
        String batchNo = "TBT-" + System.nanoTime();
        PurOrderItem orderItem = purOrderItemMapper.selectOne(new LambdaQueryWrapper<PurOrderItem>()
                .eq(PurOrderItem::getOrderId, orderId));
        PurchaseReceiptRequest request = new PurchaseReceiptRequest();
        request.setOrderId(orderId);
        request.setWarehouseId(1L);
        PurchaseReceiptRequest.Item receiptItem = new PurchaseReceiptRequest.Item();
        receiptItem.setOrderItemId(orderItem.getId());
        receiptItem.setBatchNo(batchNo);
        receiptItem.setQty(new BigDecimal("5"));
        request.getItems().add(receiptItem);
        Long receiptId = purchaseReceiptService.create(request);
        purchaseReceiptService.confirm(receiptId);

        assertEquals("RECEIVED", purOrderMapper.selectById(orderId).getStatus());
        assertEquals(0, stockService.available(1L, 1L, batchNo).compareTo(new BigDecimal("5")));
        assertEquals(0, stockService.averageCost(1L, 1L, batchNo).compareTo(new BigDecimal("40")));

        // 4. 应付金额 = 200 + 26 税 = 226
        FinPayable payable = payableMapper.selectOne(new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getSourceType, "RECEIPT").eq(FinPayable::getSourceId, receiptId));
        assertNotNull(payable);
        assertEquals(0, payable.getAmount().compareTo(new BigDecimal("226")));

        // 5. 付款并全额核销
        PaymentRequest payment = new PaymentRequest();
        payment.setSupplierId(1L);
        payment.setAccountId(1L);
        payment.setAmount(new BigDecimal("226"));
        PaymentRequest.Allocation allocation = new PaymentRequest.Allocation();
        allocation.setPayableId(payable.getId());
        allocation.setAmount(new BigDecimal("226"));
        payment.getAllocations().add(allocation);
        financeService.createPayment(payment);

        assertEquals("SETTLED", payableMapper.selectById(payable.getId()).getStatus());
    }

    @Test
    void draftOrder_shouldNotAllowShipment() {
        SalesOrderRequest order = new SalesOrderRequest();
        order.setCustomerId(1L);
        SalesOrderRequest.Item item = new SalesOrderRequest.Item();
        item.setProductId(1L);
        item.setQty(new BigDecimal("1"));
        item.setPrice(new BigDecimal("89"));
        item.setTaxRate(new BigDecimal("0.13"));
        order.getItems().add(item);
        Long orderId = salesOrderService.create(order);

        SalesShipmentRequest shipment = new SalesShipmentRequest();
        shipment.setOrderId(orderId);
        shipment.setWarehouseId(1L);
        SalesShipmentRequest.Item shipItem = new SalesShipmentRequest.Item();
        shipItem.setOrderItemId(salOrderItemMapper.selectOne(new LambdaQueryWrapper<SalOrderItem>()
                .eq(SalOrderItem::getOrderId, orderId)).getId());
        shipItem.setQty(new BigDecimal("1"));
        shipment.getItems().add(shipItem);

        com.erp.common.exception.BizException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.erp.common.exception.BizException.class, () -> salesShipmentService.create(shipment));
        assertEquals(42200, ex.getCode());
    }

    @Test
    void submittedOrder_shouldRejectDuplicateSubmit() {
        SalesOrderRequest order = new SalesOrderRequest();
        order.setCustomerId(1L);
        SalesOrderRequest.Item item = new SalesOrderRequest.Item();
        item.setProductId(1L);
        item.setQty(new BigDecimal("1"));
        item.setPrice(new BigDecimal("89"));
        item.setTaxRate(new BigDecimal("0.13"));
        order.getItems().add(item);
        Long orderId = salesOrderService.create(order);
        salesOrderService.submit(orderId);

        com.erp.common.exception.BizException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.erp.common.exception.BizException.class, () -> salesOrderService.submit(orderId));
        assertEquals(40900, ex.getCode());
    }
}

package com.erp.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.security.SecurityUtils;
import com.erp.common.util.MoneyUtils;
import com.erp.finance.dto.AgingRow;
import com.erp.finance.dto.PaymentRequest;
import com.erp.finance.dto.ReceiptRequest;
import com.erp.finance.entity.FinAccount;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.entity.FinPayment;
import com.erp.finance.entity.FinPaymentAlloc;
import com.erp.finance.entity.FinReceipt;
import com.erp.finance.entity.FinReceiptAlloc;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.mapper.FinAccountMapper;
import com.erp.finance.mapper.FinPayableMapper;
import com.erp.finance.mapper.FinPaymentAllocMapper;
import com.erp.finance.mapper.FinPaymentMapper;
import com.erp.finance.mapper.FinReceiptAllocMapper;
import com.erp.finance.mapper.FinReceiptMapper;
import com.erp.finance.mapper.FinReceivableMapper;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.mapper.MdCustomerMapper;
import com.erp.masterdata.mapper.MdSupplierMapper;
import com.erp.system.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务核心：应收应付生成、收付款、核销与账龄。
 */
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinAccountMapper accountMapper;
    private final FinReceivableMapper receivableMapper;
    private final FinPayableMapper payableMapper;
    private final FinReceiptMapper receiptMapper;
    private final FinReceiptAllocMapper receiptAllocMapper;
    private final FinPaymentMapper paymentMapper;
    private final FinPaymentAllocMapper paymentAllocMapper;
    private final MdCustomerMapper customerMapper;
    private final MdSupplierMapper supplierMapper;
    private final SequenceService sequenceService;

    @Transactional(rollbackFor = Exception.class)
    public Long createReceivable(Long customerId, String sourceType, Long sourceId, String sourceNo,
                                 BigDecimal amount, LocalDate dueDate, String remark) {
        FinReceivable receivable = new FinReceivable();
        receivable.setReceivableNo(sequenceService.next("RECEIVABLE"));
        receivable.setCustomerId(customerId);
        receivable.setSourceType(sourceType);
        receivable.setSourceId(sourceId);
        receivable.setSourceNo(sourceNo);
        receivable.setAmount(MoneyUtils.nz(amount));
        receivable.setSettledAmount(MoneyUtils.zero());
        receivable.setStatus("UNSETTLED");
        receivable.setDueDate(dueDate == null ? LocalDate.now().plusDays(30) : dueDate);
        receivable.setRemark(remark);
        receivableMapper.insert(receivable);
        return receivable.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createPayable(Long supplierId, String sourceType, Long sourceId, String sourceNo,
                              BigDecimal amount, LocalDate dueDate, String remark) {
        FinPayable payable = new FinPayable();
        payable.setPayableNo(sequenceService.next("PAYABLE"));
        payable.setSupplierId(supplierId);
        payable.setSourceType(sourceType);
        payable.setSourceId(sourceId);
        payable.setSourceNo(sourceNo);
        payable.setAmount(MoneyUtils.nz(amount));
        payable.setSettledAmount(MoneyUtils.zero());
        payable.setStatus("UNSETTLED");
        payable.setDueDate(dueDate == null ? LocalDate.now().plusDays(30) : dueDate);
        payable.setRemark(remark);
        payableMapper.insert(payable);
        return payable.getId();
    }

    public PageResult<FinReceivable> pageReceivables(long page, long size, Long customerId, String status) {
        LambdaQueryWrapper<FinReceivable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(customerId != null, FinReceivable::getCustomerId, customerId)
                .eq(StringUtils.hasText(status), FinReceivable::getStatus, status)
                .orderByDesc(FinReceivable::getId);
        return PageResult.of(receivableMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public PageResult<FinPayable> pagePayables(long page, long size, Long supplierId, String status) {
        LambdaQueryWrapper<FinPayable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(supplierId != null, FinPayable::getSupplierId, supplierId)
                .eq(StringUtils.hasText(status), FinPayable::getStatus, status)
                .orderByDesc(FinPayable::getId);
        return PageResult.of(payableMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public List<Map<String, Object>> openReceivables(Long customerId) {
        LambdaQueryWrapper<FinReceivable> wrapper = new LambdaQueryWrapper<FinReceivable>()
                .ne(FinReceivable::getStatus, "SETTLED")
                .eq(customerId != null, FinReceivable::getCustomerId, customerId)
                .orderByAsc(FinReceivable::getDueDate);
        return receivableMapper.selectList(wrapper).stream().map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "no", r.getReceivableNo(),
                "customerId", r.getCustomerId(),
                "amount", r.getAmount(),
                "settledAmount", r.getSettledAmount(),
                "outstanding", MoneyUtils.nz(r.getAmount()).subtract(MoneyUtils.nz(r.getSettledAmount())),
                "dueDate", r.getDueDate() == null ? "" : r.getDueDate().toString(),
                "status", r.getStatus()
        )).toList();
    }

    public List<Map<String, Object>> openPayables(Long supplierId) {
        LambdaQueryWrapper<FinPayable> wrapper = new LambdaQueryWrapper<FinPayable>()
                .ne(FinPayable::getStatus, "SETTLED")
                .eq(supplierId != null, FinPayable::getSupplierId, supplierId)
                .orderByAsc(FinPayable::getDueDate);
        return payableMapper.selectList(wrapper).stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "no", p.getPayableNo(),
                "supplierId", p.getSupplierId(),
                "amount", p.getAmount(),
                "settledAmount", p.getSettledAmount(),
                "outstanding", MoneyUtils.nz(p.getAmount()).subtract(MoneyUtils.nz(p.getSettledAmount())),
                "dueDate", p.getDueDate() == null ? "" : p.getDueDate().toString(),
                "status", p.getStatus()
        )).toList();
    }

    public PageResult<FinReceipt> pageReceipts(long page, long size, Long customerId) {
        LambdaQueryWrapper<FinReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(customerId != null, FinReceipt::getCustomerId, customerId).orderByDesc(FinReceipt::getId);
        return PageResult.of(receiptMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public PageResult<FinPayment> pagePayments(long page, long size, Long supplierId) {
        LambdaQueryWrapper<FinPayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(supplierId != null, FinPayment::getSupplierId, supplierId).orderByDesc(FinPayment::getId);
        return PageResult.of(paymentMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createReceipt(ReceiptRequest request) {
        if (request.getCustomerId() == null || !MoneyUtils.gt(request.getAmount(), BigDecimal.ZERO)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "客户与收款金额不能为空");
        }
        FinReceipt receipt = new FinReceipt();
        receipt.setReceiptNo(sequenceService.next("RECEIPT"));
        receipt.setCustomerId(request.getCustomerId());
        receipt.setAccountId(request.getAccountId());
        receipt.setAmount(MoneyUtils.nz(request.getAmount()));
        receipt.setUsedAmount(MoneyUtils.zero());
        receipt.setReceiptDate(request.getReceiptDate() == null ? LocalDate.now() : request.getReceiptDate());
        receipt.setStatus("CONFIRMED");
        receipt.setRemark(request.getRemark());
        receiptMapper.insert(receipt);
        applyReceiptAllocations(receipt, request.getAmount(), request.getAllocations().stream()
                .map(a -> Map.entry(a.getReceivableId(), a.getAmount())).toList());
        if (receipt.getAccountId() != null) {
            changeAccountBalance(receipt.getAccountId(), MoneyUtils.nz(request.getAmount()));
        }
        return receipt.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void writeoffReceipt(Long receiptId, List<ReceiptRequest.Allocation> allocations) {
        FinReceipt receipt = receiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw BizException.notFound("收款单不存在");
        }
        applyReceiptAllocations(receipt, MoneyUtils.nz(receipt.getAmount()).subtract(MoneyUtils.nz(receipt.getUsedAmount())),
                allocations.stream().map(a -> Map.entry(a.getReceivableId(), a.getAmount())).toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createPayment(PaymentRequest request) {
        if (request.getSupplierId() == null || !MoneyUtils.gt(request.getAmount(), BigDecimal.ZERO)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "供应商与付款金额不能为空");
        }
        FinPayment payment = new FinPayment();
        payment.setPaymentNo(sequenceService.next("PAYMENT"));
        payment.setSupplierId(request.getSupplierId());
        payment.setAccountId(request.getAccountId());
        payment.setAmount(MoneyUtils.nz(request.getAmount()));
        payment.setUsedAmount(MoneyUtils.zero());
        payment.setPaymentDate(request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate());
        payment.setStatus("CONFIRMED");
        payment.setRemark(request.getRemark());
        paymentMapper.insert(payment);
        applyPaymentAllocations(payment, request.getAmount(), request.getAllocations().stream()
                .map(a -> Map.entry(a.getPayableId(), a.getAmount())).toList());
        if (payment.getAccountId() != null) {
            changeAccountBalance(payment.getAccountId(), MoneyUtils.nz(request.getAmount()).negate());
        }
        return payment.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void writeoffPayment(Long paymentId, List<PaymentRequest.Allocation> allocations) {
        FinPayment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw BizException.notFound("付款单不存在");
        }
        applyPaymentAllocations(payment, MoneyUtils.nz(payment.getAmount()).subtract(MoneyUtils.nz(payment.getUsedAmount())),
                allocations.stream().map(a -> Map.entry(a.getPayableId(), a.getAmount())).toList());
    }

    public List<AgingRow> receivableAging() {
        List<FinReceivable> open = receivableMapper.selectList(new LambdaQueryWrapper<FinReceivable>()
                .ne(FinReceivable::getStatus, "SETTLED"));
        Map<Long, AgingRow> rows = new LinkedHashMap<>();
        for (FinReceivable item : open) {
            AgingRow row = rows.computeIfAbsent(item.getCustomerId(), key -> {
                AgingRow created = new AgingRow();
                created.setPartnerId(key);
                created.setNotDue(MoneyUtils.zero());
                created.setOverdue1to30(MoneyUtils.zero());
                created.setOverdue31to60(MoneyUtils.zero());
                created.setOverdue61to90(MoneyUtils.zero());
                created.setOverdueOver90(MoneyUtils.zero());
                created.setTotal(MoneyUtils.zero());
                MdCustomer customer = customerMapper.selectById(key);
                created.setPartnerName(customer == null ? "" : customer.getName());
                return created;
            });
            BigDecimal outstanding = MoneyUtils.nz(item.getAmount()).subtract(MoneyUtils.nz(item.getSettledAmount()));
            fillAging(row, outstanding, item.getDueDate());
        }
        return new ArrayList<>(rows.values());
    }

    public List<AgingRow> payableAging() {
        List<FinPayable> open = payableMapper.selectList(new LambdaQueryWrapper<FinPayable>()
                .ne(FinPayable::getStatus, "SETTLED"));
        Map<Long, AgingRow> rows = new LinkedHashMap<>();
        for (FinPayable item : open) {
            AgingRow row = rows.computeIfAbsent(item.getSupplierId(), key -> {
                AgingRow created = new AgingRow();
                created.setPartnerId(key);
                created.setNotDue(MoneyUtils.zero());
                created.setOverdue1to30(MoneyUtils.zero());
                created.setOverdue31to60(MoneyUtils.zero());
                created.setOverdue61to90(MoneyUtils.zero());
                created.setOverdueOver90(MoneyUtils.zero());
                created.setTotal(MoneyUtils.zero());
                MdSupplier supplier = supplierMapper.selectById(key);
                created.setPartnerName(supplier == null ? "" : supplier.getName());
                return created;
            });
            BigDecimal outstanding = MoneyUtils.nz(item.getAmount()).subtract(MoneyUtils.nz(item.getSettledAmount()));
            fillAging(row, outstanding, item.getDueDate());
        }
        return new ArrayList<>(rows.values());
    }

    private void fillAging(AgingRow row, BigDecimal outstanding, LocalDate dueDate) {
        long overdueDays = dueDate == null ? 0 : ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        if (overdueDays <= 0) {
            row.setNotDue(MoneyUtils.add(row.getNotDue(), outstanding));
        } else if (overdueDays <= 30) {
            row.setOverdue1to30(MoneyUtils.add(row.getOverdue1to30(), outstanding));
        } else if (overdueDays <= 60) {
            row.setOverdue31to60(MoneyUtils.add(row.getOverdue31to60(), outstanding));
        } else if (overdueDays <= 90) {
            row.setOverdue61to90(MoneyUtils.add(row.getOverdue61to90(), outstanding));
        } else {
            row.setOverdueOver90(MoneyUtils.add(row.getOverdueOver90(), outstanding));
        }
        row.setTotal(MoneyUtils.add(row.getTotal(), outstanding));
    }

    private void applyReceiptAllocations(FinReceipt receipt, BigDecimal limit,
                                        List<Map.Entry<Long, BigDecimal>> allocations) {
        BigDecimal remaining = MoneyUtils.nz(limit);
        for (Map.Entry<Long, BigDecimal> entry : allocations) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "核销明细不能为空");
            }
            BigDecimal allocAmount = MoneyUtils.nz(entry.getValue());
            if (allocAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "核销金额必须大于0");
            }
            if (allocAmount.compareTo(remaining) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "核销金额超过可核销余额");
            }
            FinReceivable receivable = receivableMapper.selectForUpdate(entry.getKey());
            if (receivable == null) {
                throw BizException.notFound("应收单不存在");
            }
            if (!receivable.getCustomerId().equals(receipt.getCustomerId())) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "应收单与收款单客户不一致");
            }
            BigDecimal outstanding = MoneyUtils.nz(receivable.getAmount())
                    .subtract(MoneyUtils.nz(receivable.getSettledAmount()));
            if (allocAmount.compareTo(outstanding) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "核销金额超过应收余额");
            }
            BigDecimal settled = MoneyUtils.nz(receivable.getSettledAmount()).add(allocAmount);
            receivable.setSettledAmount(settled);
            receivable.setStatus(settled.compareTo(MoneyUtils.nz(receivable.getAmount())) >= 0 ? "SETTLED" : "PARTIAL");
            receivableMapper.updateById(receivable);

            FinReceiptAlloc record = new FinReceiptAlloc();
            record.setReceiptId(receipt.getId());
            record.setReceivableId(receivable.getId());
            record.setAmount(allocAmount);
            record.setCreatedBy(SecurityUtils.getUserId());
            record.setCreatedAt(LocalDateTime.now());
            receiptAllocMapper.insert(record);

            remaining = remaining.subtract(allocAmount);
            receipt.setUsedAmount(MoneyUtils.nz(receipt.getUsedAmount()).add(allocAmount));
        }
        receiptMapper.updateById(receipt);
    }

    private void applyPaymentAllocations(FinPayment payment, BigDecimal limit,
                                         List<Map.Entry<Long, BigDecimal>> allocations) {
        BigDecimal remaining = MoneyUtils.nz(limit);
        for (Map.Entry<Long, BigDecimal> entry : allocations) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "核销明细不能为空");
            }
            BigDecimal allocAmount = MoneyUtils.nz(entry.getValue());
            if (allocAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "核销金额必须大于0");
            }
            if (allocAmount.compareTo(remaining) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "核销金额超过可核销余额");
            }
            FinPayable payable = payableMapper.selectForUpdate(entry.getKey());
            if (payable == null) {
                throw BizException.notFound("应付单不存在");
            }
            if (!payable.getSupplierId().equals(payment.getSupplierId())) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "应付单与付款单供应商不一致");
            }
            BigDecimal outstanding = MoneyUtils.nz(payable.getAmount())
                    .subtract(MoneyUtils.nz(payable.getSettledAmount()));
            if (allocAmount.compareTo(outstanding) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "核销金额超过应付余额");
            }
            BigDecimal settled = MoneyUtils.nz(payable.getSettledAmount()).add(allocAmount);
            payable.setSettledAmount(settled);
            payable.setStatus(settled.compareTo(MoneyUtils.nz(payable.getAmount())) >= 0 ? "SETTLED" : "PARTIAL");
            payableMapper.updateById(payable);

            FinPaymentAlloc record = new FinPaymentAlloc();
            record.setPaymentId(payment.getId());
            record.setPayableId(payable.getId());
            record.setAmount(allocAmount);
            record.setCreatedBy(SecurityUtils.getUserId());
            record.setCreatedAt(LocalDateTime.now());
            paymentAllocMapper.insert(record);

            remaining = remaining.subtract(allocAmount);
            payment.setUsedAmount(MoneyUtils.nz(payment.getUsedAmount()).add(allocAmount));
        }
        paymentMapper.updateById(payment);
    }

    private void changeAccountBalance(Long accountId, BigDecimal delta) {
        FinAccount account = accountMapper.selectById(accountId);
        if (account == null) {
            throw BizException.notFound("资金账户不存在");
        }
        account.setBalance(MoneyUtils.nz(account.getBalance()).add(delta));
        accountMapper.updateById(account);
    }
}

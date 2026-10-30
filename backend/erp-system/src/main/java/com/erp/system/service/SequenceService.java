package com.erp.system.service;

import com.erp.system.entity.SysIdSequence;
import com.erp.system.mapper.SysIdSequenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 单据编号生成：前缀 + 日期 + 4 位流水，行锁保证并发唯一。
 */
@Service
@RequiredArgsConstructor
public class SequenceService {

    private static final Map<String, String> DEFAULT_PREFIX = Map.ofEntries(
            Map.entry("PUR_ORDER", "PO"),
            Map.entry("PUR_RECEIPT", "PR"),
            Map.entry("PUR_RETURN", "PT"),
            Map.entry("SAL_ORDER", "SO"),
            Map.entry("SAL_SHIPMENT", "SH"),
            Map.entry("SAL_RETURN", "ST"),
            Map.entry("TRANSFER", "TR"),
            Map.entry("STOCKTAKE", "SK"),
            Map.entry("RECEIPT", "RC"),
            Map.entry("PAYMENT", "PY"),
            Map.entry("RECEIVABLE", "AR"),
            Map.entry("PAYABLE", "AP"),
            Map.entry("EXPENSE", "EX"),
            Map.entry("CUSTOMER", "CU"),
            Map.entry("FLOW", "FL")
    );

    private final SysIdSequenceMapper sequenceMapper;

    @Transactional(rollbackFor = Exception.class)
    public String next(String seqCode) {
        SysIdSequence sequence = sequenceMapper.selectForUpdate(seqCode);
        if (sequence == null) {
            try {
                SysIdSequence created = new SysIdSequence();
                created.setSeqCode(seqCode);
                created.setPrefix(DEFAULT_PREFIX.getOrDefault(seqCode, seqCode));
                created.setDatePattern("yyyyMMdd");
                created.setCurrentValue(0L);
                sequenceMapper.insert(created);
            } catch (DuplicateKeyException ignored) {
                // 并发创建时忽略，重新加锁读取
            }
            sequence = sequenceMapper.selectForUpdate(seqCode);
        }
        if (sequence == null) {
            throw new IllegalStateException("编号序列初始化失败: " + seqCode);
        }
        String datePattern = sequence.getDatePattern() == null ? "yyyyMMdd" : sequence.getDatePattern();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern(datePattern));
        long nextValue = today.equals(sequence.getDateKey()) ? sequence.getCurrentValue() + 1 : 1L;
        sequence.setCurrentValue(nextValue);
        sequence.setDateKey(today);
        sequenceMapper.updateById(sequence);
        return (sequence.getPrefix() == null ? "" : sequence.getPrefix()) + today
                + String.format("%04d", nextValue);
    }
}

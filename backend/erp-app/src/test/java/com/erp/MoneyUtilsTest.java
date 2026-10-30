package com.erp;

import com.erp.common.util.CsvUtils;
import com.erp.common.util.MoneyUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 金额与 CSV 工具单元测试：验证精度、舍入与空值处理。
 */
class MoneyUtilsTest {

    @Test
    void nz_shouldDefaultNullToZeroWithScale4() {
        assertEquals(new BigDecimal("0.0000"), MoneyUtils.nz(null));
        assertEquals(new BigDecimal("1.2346"), MoneyUtils.nz(new BigDecimal("1.23456")));
    }

    @Test
    void multiply_shouldKeepFourDecimalsHalfUp() {
        assertEquals(new BigDecimal("0.3000"), MoneyUtils.multiply(new BigDecimal("0.1"), new BigDecimal("3")));
        assertEquals(new BigDecimal("1005.7000"),
                MoneyUtils.multiply(new BigDecimal("89"), new BigDecimal("11.3")));
    }

    @Test
    void add_shouldIgnoreNull() {
        assertEquals(new BigDecimal("3.0000"),
                MoneyUtils.add(new BigDecimal("1"), null, new BigDecimal("2")));
    }

    @Test
    void taxAmount_shouldReturnZeroWhenRateIsNullOrZero() {
        assertEquals(new BigDecimal("0.0000"), MoneyUtils.taxAmount(new BigDecimal("100"), null));
        assertEquals(new BigDecimal("0.0000"), MoneyUtils.taxAmount(new BigDecimal("100"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("13.0000"), MoneyUtils.taxAmount(new BigDecimal("100"), new BigDecimal("0.13")));
    }

    @Test
    void scale2_shouldRoundToTwoDecimals() {
        assertEquals(new BigDecimal("1.01"), MoneyUtils.scale2(new BigDecimal("1.005")));
        assertEquals(new BigDecimal("0.00"), MoneyUtils.scale2(null));
    }

    @Test
    void comparisons_shouldTreatNullAsZero() {
        assertFalse(MoneyUtils.gt(null, BigDecimal.ZERO));
        assertTrue(MoneyUtils.gte(null, BigDecimal.ZERO));
        assertTrue(MoneyUtils.gt(new BigDecimal("0.0001"), BigDecimal.ZERO));
    }

    @Test
    void csv_shouldEscapeCommaAndQuoteAndKeepBom() {
        byte[] bytes = CsvUtils.toCsv(List.of("编码", "名称"),
                List.of(List.of("A,01", "带\"引号\"的值")));
        String csv = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("编码,名称"));
        assertTrue(csv.contains("\"A,01\""));
        assertTrue(csv.contains("\"带\"\"引号\"\"的值\""));
    }
}

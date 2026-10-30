package com.erp.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额计算工具：统一精度，禁止 double/float。
 */
public final class MoneyUtils {

    public static final int SCALE = 4;
    public static final int AMOUNT_SCALE = 2;

    private MoneyUtils() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? zero() : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal add(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                sum = sum.add(value);
            }
        }
        return sum.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal multiply(BigDecimal price, BigDecimal qty) {
        return nz(price).multiply(nz(qty)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal taxAmount(BigDecimal amount, BigDecimal taxRate) {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) <= 0) {
            return zero();
        }
        return nz(amount).multiply(taxRate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP)
                : value.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean gt(BigDecimal left, BigDecimal right) {
        return nz(left).compareTo(nz(right)) > 0;
    }

    public static boolean gte(BigDecimal left, BigDecimal right) {
        return nz(left).compareTo(nz(right)) >= 0;
    }
}

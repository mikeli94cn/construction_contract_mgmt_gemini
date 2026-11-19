package com.ccm.common.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralized BigDecimal arithmetic utilities for construction financial calculations.
 * Database columns are DECIMAL(19, 4) and rates are DECIMAL(7, 4).
 */
public final class MoneyUtils {

    public static final int MONEY_SCALE = 4;
    public static final int RATE_SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);

    private MoneyUtils() {
    }

    public static BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal scaleRate(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, ROUNDING);
        }
        return rate.setScale(RATE_SCALE, ROUNDING);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return scaleMoney(a).add(scaleMoney(b)).setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return scaleMoney(a).subtract(scaleMoney(b)).setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal amount, BigDecimal factor) {
        if (amount == null || factor == null) {
            return ZERO;
        }
        return amount.multiply(factor).setScale(MONEY_SCALE, ROUNDING);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }
}

package com.dolcecandela.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arrondi des montants en dinars tunisiens (3 décimales = millimes). */
public final class Money {

    public static final int SCALE = 3;

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }
}

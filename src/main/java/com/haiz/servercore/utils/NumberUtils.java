package com.haiz.servercore.utils;

import java.text.DecimalFormat;

public final class NumberUtils {
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#,##0.0");
    private static final DecimalFormat INTEGER = new DecimalFormat("#,##0");

    private NumberUtils() {
    }

    public static String oneDecimal(double value) {
        return ONE_DECIMAL.format(value);
    }

    public static String integer(long value) {
        return INTEGER.format(value);
    }
}

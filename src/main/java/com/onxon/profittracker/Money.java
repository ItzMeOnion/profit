package com.onxon.profittracker;

import java.util.Locale;

/** Formats numbers the way Donut SMP shows them: $1.25M, $900K and so on. */
public final class Money {
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T"};

    private Money() {}

    public static String compact(double value) {
        double v = Math.abs(value);
        int idx = 0;
        while (v >= 1000 && idx < SUFFIXES.length - 1) {
            v /= 1000.0;
            idx++;
        }
        String num = idx == 0 ? String.format(Locale.US, "%.0f", v) : String.format(Locale.US, "%.2f", v);
        if (num.contains(".")) {
            num = num.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return "$" + num + SUFFIXES[idx];
    }

    /** "+$1.2M", "-$300K" or "$0". */
    public static String signed(double value) {
        if (Math.abs(value) < 0.005) return "$0";
        return (value > 0 ? "+" : "-") + compact(value);
    }

    /** Green for profit, red for loss, grey for even. */
    public static int signColor(double value) {
        if (value > 0.005) return Draw.GREEN;
        if (value < -0.005) return Draw.RED;
        return Draw.GRAY;
    }
}

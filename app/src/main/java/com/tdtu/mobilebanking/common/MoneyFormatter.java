package com.tdtu.mobilebanking.common;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Định dạng tiền VND. Tiền luôn là long (đơn vị đồng), không dùng double (hợp đồng mục 2).
 * Ví dụ: 1234567 -> "1.234.567 ₫".
 */
public final class MoneyFormatter {

    private static final DecimalFormat FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        FORMAT = new DecimalFormat("#,##0", symbols);
    }

    private MoneyFormatter() {
    }

    public static String format(long vnd) {
        synchronized (FORMAT) {
            return FORMAT.format(vnd) + " ₫";
        }
    }

    /** Chỉ phần số, không có ký hiệu ₫. */
    public static String formatPlain(long vnd) {
        synchronized (FORMAT) {
            return FORMAT.format(vnd);
        }
    }

    /**
     * Đọc số tiền người dùng gõ ("1.000.000", "1000000", "1,000,000 đ").
     *
     * @return số tiền, hoặc -1 nếu không hợp lệ.
     */
    public static long parse(String input) {
        if (input == null) return -1;
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 15) return -1;
        return Long.parseLong(digits);
    }

    /** Lãi suất %/năm, ví dụ 4.5 -> "4,50%". */
    public static String formatRate(double percent) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%.2f%%", percent);
    }
}

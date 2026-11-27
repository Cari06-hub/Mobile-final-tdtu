package com.tdtu.mobilebanking.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Tiện ích ngày tháng dùng chung. */
public final class DateUtils {

    private static final SimpleDateFormat DAY = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    private static final SimpleDateFormat MONTH_KEY = new SimpleDateFormat("yyyy-MM", Locale.US);

    private DateUtils() {
    }

    public static String formatDay(long millis) {
        if (millis <= 0) return "—";
        synchronized (DAY) {
            return DAY.format(new Date(millis));
        }
    }

    /** "2026-11" — dùng cho route history?month=. */
    public static String monthKey(long millis) {
        synchronized (MONTH_KEY) {
            return MONTH_KEY.format(new Date(millis));
        }
    }

    public static long addMonths(long millis, int months) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.add(Calendar.MONTH, months);
        return c.getTimeInMillis();
    }

    public static long addDays(long millis, int days) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTimeInMillis();
    }

    /** Số tháng tròn đã trôi qua từ start đến now (không âm). */
    public static int fullMonthsBetween(long start, long now) {
        if (now <= start) return 0;
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(start);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(now);
        int months = (b.get(Calendar.YEAR) - a.get(Calendar.YEAR)) * 12
                + (b.get(Calendar.MONTH) - a.get(Calendar.MONTH));
        if (b.get(Calendar.DAY_OF_MONTH) < a.get(Calendar.DAY_OF_MONTH)) months--;
        return Math.max(0, months);
    }
}

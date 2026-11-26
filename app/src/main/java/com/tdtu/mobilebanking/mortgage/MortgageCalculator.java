package com.tdtu.mobilebanking.mortgage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * C3 - Lịch trả nợ theo công thức annuity (trả đều mỗi kỳ), thuần Java để dễ test.
 *
 *   r = lãi suất năm / số kỳ trong năm   (12 nếu trả hàng tháng, 26 nếu 2 tuần/lần)
 *   n = số kỳ                            (termMonths, hoặc termMonths × 26 / 12)
 *   A = P × r / (1 − (1 + r)^−n)          số tiền phải trả mỗi kỳ
 * Mỗi kỳ: lãi = dư nợ × r; gốc = A − lãi; dư nợ mới = dư nợ − gốc.
 * Kỳ cuối trả nốt phần dư nợ còn lại để dư nợ về đúng 0 (bù sai số làm tròn).
 */
public final class MortgageCalculator {

    public enum Frequency {
        MONTHLY, BIWEEKLY;

        public static Frequency parse(String s) {
            return "BIWEEKLY".equalsIgnoreCase(s) ? BIWEEKLY : MONTHLY;
        }

        public int periodsPerYear() {
            return this == MONTHLY ? 12 : 26;
        }
    }

    /** Một kỳ trả nợ. */
    public static final class Installment {
        public final int index;            // kỳ thứ (1..n)
        public final long dueMillis;       // ngày đến hạn
        public final long payment;         // tổng phải trả kỳ này
        public final long principalPart;   // phần gốc
        public final long interest;        // phần lãi
        public final long remainingAfter;  // dư nợ sau khi trả kỳ này

        Installment(int index, long dueMillis, long payment, long principalPart,
                    long interest, long remainingAfter) {
            this.index = index;
            this.dueMillis = dueMillis;
            this.payment = payment;
            this.principalPart = principalPart;
            this.interest = interest;
            this.remainingAfter = remainingAfter;
        }
    }

    private MortgageCalculator() {
    }

    public static int periods(int termMonths, Frequency f) {
        return f == Frequency.MONTHLY ? termMonths : (int) Math.round(termMonths * 26 / 12.0);
    }

    public static double periodRate(double annualRatePercent, Frequency f) {
        return annualRatePercent / 100.0 / f.periodsPerYear();
    }

    /** Số tiền trả mỗi kỳ (A). */
    public static long installment(long principal, double annualRatePercent, int termMonths, Frequency f) {
        int n = periods(termMonths, f);
        if (n <= 0 || principal <= 0) return 0;
        double r = periodRate(annualRatePercent, f);
        if (r == 0) return (principal + n - 1) / n;   // không lãi: chia đều, làm tròn lên
        double a = principal * r / (1 - Math.pow(1 + r, -n));
        return Math.round(a);
    }

    public static long dueDate(long startMillis, int index, Frequency f) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startMillis);
        if (f == Frequency.MONTHLY) c.add(Calendar.MONTH, index);
        else c.add(Calendar.DAY_OF_MONTH, 14 * index);
        return c.getTimeInMillis();
    }

    public static List<Installment> schedule(long principal, double annualRatePercent,
                                             int termMonths, Frequency f, long startMillis) {
        int n = periods(termMonths, f);
        List<Installment> list = new ArrayList<>(Math.max(n, 0));
        if (n <= 0 || principal <= 0) return list;
        double r = periodRate(annualRatePercent, f);
        long pay = installment(principal, annualRatePercent, termMonths, f);
        long remaining = principal;
        for (int i = 1; i <= n && remaining > 0; i++) {
            long interest = Math.round(remaining * r);
            long principalPart = pay - interest;
            if (i == n || principalPart > remaining) principalPart = remaining;
            remaining -= principalPart;
            list.add(new Installment(i, dueDate(startMillis, i, f),
                    principalPart + interest, principalPart, interest, remaining));
        }
        return list;
    }

    /**
     * Số kỳ đã trả, suy ra từ dư nợ hiện tại (outstanding): kỳ i coi là đã trả nếu
     * dư nợ sau kỳ i >= outstanding. Yêu cầu B: khi trả kỳ, giảm outstanding đúng bằng
     * principalPart (gửi trong meta), không trừ cả phần lãi.
     */
    public static int paidCount(List<Installment> schedule, long outstanding) {
        int paid = 0;
        for (Installment it : schedule) {
            if (it.remainingAfter >= outstanding) paid++;
            else break;
        }
        return paid;
    }

    public static long totalInterest(List<Installment> schedule) {
        long sum = 0;
        for (Installment it : schedule) sum += it.interest;
        return sum;
    }
}

package com.tdtu.mobilebanking.saving;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * C2 - Công thức tiết kiệm (lãi đơn, trả lãi cuối kỳ như sổ tiết kiệm có kỳ hạn thông thường).
 * Lớp thuần Java, không phụ thuộc Android, nên viết unit test dễ.
 *
 *   Lãi mỗi tháng   = gốc × lãi suất năm / 12
 *   Lãi cả kỳ hạn   = gốc × lãi suất năm × số tháng / 12
 *   Tiền nhận khi đáo hạn = gốc + lãi cả kỳ hạn
 * Tiền là long (VND), làm tròn đến đồng.
 */
public final class SavingCalculator {

    /** Một dòng trong bảng lãi theo tháng. */
    public static final class ProfitRow {
        public final int month;          // tháng thứ mấy (1..n)
        public final long dateMillis;    // ngày ghi nhận lãi tháng đó
        public final long profit;        // lãi của tháng
        public final long cumulative;    // lãi cộng dồn

        ProfitRow(int month, long dateMillis, long profit, long cumulative) {
            this.month = month;
            this.dateMillis = dateMillis;
            this.profit = profit;
            this.cumulative = cumulative;
        }
    }

    private SavingCalculator() {
    }

    public static long monthlyProfit(long principal, double annualRatePercent) {
        return Math.round(principal * annualRatePercent / 100.0 / 12.0);
    }

    public static long totalProfit(long principal, double annualRatePercent, int termMonths) {
        return Math.round(principal * annualRatePercent / 100.0 * termMonths / 12.0);
    }

    public static long maturityMillis(long openedAtMillis, int termMonths) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(openedAtMillis);
        c.add(Calendar.MONTH, termMonths);
        return c.getTimeInMillis();
    }

    /**
     * Bảng lãi theo tháng. Sổ không kỳ hạn (termMonths = 0) thì lập bảng 12 tháng để xem trước.
     * Dòng cuối được chỉnh để tổng lãi khớp đúng totalProfit (tránh lệch do làm tròn).
     */
    public static List<ProfitRow> schedule(long principal, double annualRatePercent,
                                           int termMonths, long openedAtMillis) {
        int months = termMonths > 0 ? termMonths : 12;
        long perMonth = monthlyProfit(principal, annualRatePercent);
        long total = totalProfit(principal, annualRatePercent, months);
        List<ProfitRow> rows = new ArrayList<>(months);
        long cumulative = 0;
        for (int m = 1; m <= months; m++) {
            long profit = (m == months) ? total - cumulative : perMonth;
            cumulative += profit;
            rows.add(new ProfitRow(m, maturityMillis(openedAtMillis, m), profit, cumulative));
        }
        return rows;
    }
}

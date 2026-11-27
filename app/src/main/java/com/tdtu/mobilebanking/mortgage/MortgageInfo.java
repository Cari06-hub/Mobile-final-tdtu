package com.tdtu.mobilebanking.mortgage;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tdtu.mobilebanking.common.FirestoreUtils;

import java.util.List;

/**
 * C3 - Dữ liệu hiển thị của khoản vay, đọc từ accounts/{id} (type = "MORTGAGE").
 * Field theo hợp đồng mục 2: MortgageAccount {principal, annualRate, termMonths,
 * frequency: MONTHLY | BIWEEKLY, startDate, outstanding}.
 */
public class MortgageInfo {
    public String id;
    public String shortNumber;
    public long principal;
    public double annualRate;
    public int termMonths;
    public MortgageCalculator.Frequency frequency;
    public long startDate;
    public long outstanding;
    public String status;

    public static MortgageInfo from(DocumentSnapshot d) {
        MortgageInfo m = new MortgageInfo();
        m.id = d.getId();
        m.shortNumber = FirestoreUtils.shortNumber(d);
        m.principal = FirestoreUtils.getLong(d, "principal");
        m.annualRate = FirestoreUtils.getDouble(d, "annualRate");
        m.termMonths = FirestoreUtils.getInt(d, "termMonths");
        m.frequency = MortgageCalculator.Frequency.parse(FirestoreUtils.getString(d, "frequency", "MONTHLY"));
        m.startDate = FirestoreUtils.getMillis(d, "startDate");
        if (m.startDate == 0) m.startDate = FirestoreUtils.getMillis(d, "createdAt");
        // outstanding chưa có (khoản vay mới) thì coi như chưa trả kỳ nào
        m.outstanding = d.contains("outstanding") ? FirestoreUtils.getLong(d, "outstanding") : m.principal;
        m.status = FirestoreUtils.getString(d, "status", "ACTIVE");
        return m;
    }

    public List<MortgageCalculator.Installment> schedule() {
        return MortgageCalculator.schedule(principal, annualRate, termMonths, frequency, startDate);
    }

    public long installment() {
        return MortgageCalculator.installment(principal, annualRate, termMonths, frequency);
    }
}

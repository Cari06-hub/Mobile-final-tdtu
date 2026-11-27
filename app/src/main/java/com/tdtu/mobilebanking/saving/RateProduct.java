package com.tdtu.mobilebanking.saving;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tdtu.mobilebanking.common.FirestoreUtils;

/**
 * C2 - Sản phẩm tiết kiệm, document rates/{productId}.
 * Field theo docs/firebase-schema.md của A và seed/seed.js:
 *   productName (String), termMonths (int, 0 = không kỳ hạn), annualRate (double, %/năm),
 *   minDeposit (long VND), status ("ACTIVE" | "INACTIVE"), updatedAt, updatedBy.
 */
public class RateProduct {

    public static final long DEFAULT_MIN_DEPOSIT = 1_000_000L;

    public String id;
    public String name;
    public int termMonths;
    public double annualRate;
    public long minDeposit = DEFAULT_MIN_DEPOSIT;
    public boolean active = true;
    public long updatedAt;

    public static RateProduct from(DocumentSnapshot d) {
        RateProduct p = new RateProduct();
        p.id = d.getId();
        p.name = FirestoreUtils.getString(d, "productName",
                FirestoreUtils.getString(d, "name", d.getId()));
        p.termMonths = FirestoreUtils.getInt(d, "termMonths");
        p.annualRate = FirestoreUtils.getDouble(d, "annualRate");
        long min = FirestoreUtils.getLong(d, "minDeposit");
        p.minDeposit = min > 0 ? min : DEFAULT_MIN_DEPOSIT;
        // Thiếu field status thì coi như đang áp dụng
        p.active = "ACTIVE".equals(FirestoreUtils.getString(d, "status", "ACTIVE"));
        p.updatedAt = FirestoreUtils.getMillis(d, "updatedAt");
        return p;
    }

    public String termText() {
        return termMonths == 0 ? "Không kỳ hạn" : "Kỳ hạn " + termMonths + " tháng";
    }
}

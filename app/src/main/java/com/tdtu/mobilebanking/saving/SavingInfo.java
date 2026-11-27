package com.tdtu.mobilebanking.saving;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tdtu.mobilebanking.common.FirestoreUtils;

/**
 * C2 - Dữ liệu hiển thị của một tài khoản tiết kiệm, đọc từ accounts/{id} (type = "SAVING").
 * Field theo hợp đồng mục 2: SavingAccount {productId, principal, termMonths, openedAt}
 * cộng các field chung của Account {ownerId, type, balance, status, createdAt}.
 *
 * lockedRate: lãi suất chốt lúc mở sổ. Đề xuất A3/B4 ghi field này khi mở sổ để việc officer
 * đổi lãi suất chỉ áp cho sổ mở mới. Sổ không có lockedRate thì dùng lãi suất hiện tại của
 * rates/{productId}.
 */
public class SavingInfo {
    public String id;
    public String shortNumber;
    public String productId;
    public long principal;
    public long balance;
    public int termMonths;
    public long openedAt;
    public String status;
    @Nullable
    public Double lockedRate;

    public static SavingInfo from(DocumentSnapshot d) {
        SavingInfo s = new SavingInfo();
        s.id = d.getId();
        s.shortNumber = FirestoreUtils.shortNumber(d);
        s.productId = FirestoreUtils.getString(d, "productId", "");
        s.balance = FirestoreUtils.getLong(d, "balance");
        s.principal = FirestoreUtils.getLong(d, "principal");
        if (s.principal == 0) s.principal = s.balance;
        s.termMonths = FirestoreUtils.getInt(d, "termMonths");
        s.openedAt = FirestoreUtils.getMillis(d, "openedAt");
        if (s.openedAt == 0) s.openedAt = FirestoreUtils.getMillis(d, "createdAt");
        s.status = FirestoreUtils.getString(d, "status", "ACTIVE");
        s.lockedRate = FirestoreUtils.getDoubleOrNull(d, "lockedRate");
        return s;
    }

    /** Lãi suất áp dụng cho sổ này. */
    public double effectiveRate(@Nullable RateProduct product) {
        if (lockedRate != null) return lockedRate;
        return product == null ? 0 : product.annualRate;
    }
}

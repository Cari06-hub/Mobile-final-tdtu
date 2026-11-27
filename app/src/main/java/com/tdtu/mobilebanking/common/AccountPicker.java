package com.tdtu.mobilebanking.common;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tdtu.mobilebanking.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Hộp thoại chọn tài khoản thanh toán (CHECKING) làm nguồn tiền.
 * Dùng cho: gửi tiết kiệm, trả kỳ mortgage, hóa đơn, nạp ĐT, checkout, liên ngân hàng.
 * Có 1 tài khoản thì chọn luôn, không hỏi.
 */
public final class AccountPicker {

    public static final class Picked {
        public final String id;
        public final long balance;
        public final String label;

        Picked(String id, long balance, String label) {
            this.id = id;
            this.balance = balance;
            this.label = label;
        }
    }

    public interface OnPicked {
        void onPicked(@NonNull Picked account);
    }

    private AccountPicker() {
    }

    public static void pickChecking(@NonNull Context ctx, @NonNull OnPicked callback) {
        String uid = FirestoreUtils.uid();
        if (uid == null) {
            Toast.makeText(ctx, R.string.common_not_signed_in, Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseFirestore.getInstance().collection("accounts")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("type", "CHECKING")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Picked> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String status = FirestoreUtils.getString(d, "status", "ACTIVE");
                        if (!"ACTIVE".equals(status)) continue;
                        long balance = FirestoreUtils.getLong(d, "balance");
                        String label = "Thanh toán •••" + FirestoreUtils.shortNumber(d)
                                + " — " + MoneyFormatter.format(balance);
                        list.add(new Picked(d.getId(), balance, label));
                    }
                    if (list.isEmpty()) {
                        Toast.makeText(ctx, R.string.common_no_checking, Toast.LENGTH_LONG).show();
                    } else if (list.size() == 1) {
                        callback.onPicked(list.get(0));
                    } else {
                        String[] labels = new String[list.size()];
                        for (int i = 0; i < list.size(); i++) labels[i] = list.get(i).label;
                        new MaterialAlertDialogBuilder(ctx)
                                .setTitle(R.string.common_pick_source)
                                .setItems(labels, (dlg, which) -> callback.onPicked(list.get(which)))
                                .setNegativeButton(R.string.common_cancel, null)
                                .show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ctx,
                        ctx.getString(R.string.common_error_format, e.getMessage()),
                        Toast.LENGTH_LONG).show());
    }
}

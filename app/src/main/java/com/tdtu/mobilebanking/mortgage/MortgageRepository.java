package com.tdtu.mobilebanking.mortgage;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.tdtu.mobilebanking.common.FirestoreUtils;
import com.tdtu.mobilebanking.common.ResultCallback;

import java.util.ArrayList;
import java.util.List;

/** C3 - Repository khoản vay (chỉ đọc; trả kỳ đi qua ConfirmTransaction của B). */
public class MortgageRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    public ListenerRegistration listenMyMortgages(ResultCallback<List<MortgageInfo>> cb) {
        String uid = FirestoreUtils.uid();
        if (uid == null) {
            cb.onError(new IllegalStateException("Bạn chưa đăng nhập"));
            return null;
        }
        return db.collection("accounts")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("type", "MORTGAGE")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        cb.onError(e);
                        return;
                    }
                    List<MortgageInfo> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) list.add(MortgageInfo.from(d));
                    cb.onSuccess(list);
                });
    }

    public ListenerRegistration listenMortgage(String accountId, ResultCallback<MortgageInfo> cb) {
        return db.collection("accounts").document(accountId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        cb.onError(e);
                    } else if (doc != null && doc.exists()) {
                        cb.onSuccess(MortgageInfo.from(doc));
                    }
                });
    }
}

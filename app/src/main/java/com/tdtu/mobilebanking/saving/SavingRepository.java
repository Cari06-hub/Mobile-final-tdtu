package com.tdtu.mobilebanking.saving;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.tdtu.mobilebanking.common.FirestoreUtils;
import com.tdtu.mobilebanking.common.ResultCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C2 - Repository tiết kiệm: mọi truy cập Firestore của module saving đi qua đây
 * (ViewModel/Fragment không gọi Firestore trực tiếp).
 */
public class SavingRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Lắng nghe realtime các sổ tiết kiệm của người đang đăng nhập. */
    @Nullable
    public ListenerRegistration listenMySavings(ResultCallback<List<SavingInfo>> cb) {
        String uid = FirestoreUtils.uid();
        if (uid == null) {
            cb.onError(new IllegalStateException("Bạn chưa đăng nhập"));
            return null;
        }
        // Rules của A chỉ cho đọc khi ownerId == uid, nên query BẮT BUỘC có điều kiện ownerId
        return db.collection("accounts")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("type", "SAVING")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        cb.onError(e);
                        return;
                    }
                    List<SavingInfo> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) list.add(SavingInfo.from(d));
                    cb.onSuccess(list);
                });
    }

    public ListenerRegistration listenSaving(String accountId, ResultCallback<SavingInfo> cb) {
        return db.collection("accounts").document(accountId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        cb.onError(e);
                    } else if (doc != null && doc.exists()) {
                        cb.onSuccess(SavingInfo.from(doc));
                    }
                });
    }

    /** Lắng nghe bảng lãi suất, sắp theo kỳ hạn. */
    public ListenerRegistration listenRates(boolean onlyActive, ResultCallback<List<RateProduct>> cb) {
        return db.collection("rates")
                .orderBy("termMonths", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        cb.onError(e);
                        return;
                    }
                    List<RateProduct> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        RateProduct p = RateProduct.from(d);
                        if (!onlyActive || p.active) list.add(p);
                    }
                    cb.onSuccess(list);
                });
    }

    /**
     * Officer tạo hoặc sửa sản phẩm lãi suất. p.id == null thì tạo document mới.
     * Rules của A: chỉ officer được ghi rates/.
     */
    public Task<Void> saveRate(RateProduct p) {
        DocumentReference ref = (p.id == null || p.id.isEmpty())
                ? db.collection("rates").document()
                : db.collection("rates").document(p.id);
        Map<String, Object> data = new HashMap<>();
        data.put("productName", p.name);
        data.put("termMonths", p.termMonths);
        data.put("annualRate", p.annualRate);
        data.put("minDeposit", p.minDeposit);
        data.put("status", p.active ? "ACTIVE" : "INACTIVE");
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedBy", FirestoreUtils.uid());
        return ref.set(data, SetOptions.merge());
    }
}

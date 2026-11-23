package com.tdtu.mobilebanking.common.contract;

import androidx.fragment.app.Fragment;

/**
 * Interface cho eKYC — owner: Dev A.
 * Được gọi trong pipeline ConfirmTransactionFragment khi amount >= highValueThreshold.
 */
public interface KycVerifier {
    /**
     * Khởi động luồng xác thực khuôn mặt.
     * @param caller Fragment đang gọi (để launch camera overlay)
     * @param callback Trả về true nếu xác thực thành công
     */
    void verify(Fragment caller, Callback<Boolean> callback);

    interface Callback<T> {
        void onResult(T result);
        void onError(String errorCode);
    }
}

package com.tdtu.mobilebanking.common.contract;

import com.tdtu.mobilebanking.common.model.TxRequest;

/**
 * Interface cho Transaction engine — owner: Dev B.
 */
public interface TransactionService {
    void execute(TxRequest request, Callback callback);

    interface Callback {
        void onSuccess(String txId);
        void onError(String errorCode);
        // Codes: INSUFFICIENT_FUNDS, LIMIT_EXCEEDED, KYC_REQUIRED, FACE_FAILED,
        //        OTP_FAILED, OTP_EXPIRED, DUPLICATE, INVALID_TARGET
    }
}

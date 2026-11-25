package com.tdtu.mobilebanking.kyc;

import androidx.fragment.app.Fragment;

import com.tdtu.mobilebanking.common.ErrorCodes;
import com.tdtu.mobilebanking.common.contract.KycVerifier;
import com.tdtu.mobilebanking.kyc.ui.KycFaceVerifyFragment;

/**
 * RealKycVerifier — implements KycVerifier interface (Dev A, A4).
 * Mở KycFaceVerifyFragment dạng fullscreen overlay trên caller Fragment để không làm mất
 * trạng thái giao dịch đang nhập trên ConfirmTransactionFragment (Dev B).
 */
public class RealKycVerifier implements KycVerifier {

    private static final String TAG_DIALOG = "kyc_face_verify_overlay";

    @Override
    public void verify(Fragment caller, Callback<Boolean> callback) {
        if (caller == null || !caller.isAdded()) {
            callback.onError(ErrorCodes.FACE_FAILED);
            return;
        }
        KycFaceVerifyFragment dialog = new KycFaceVerifyFragment();
        dialog.setVerifyCallback(new KycFaceVerifyFragment.VerifyCallback() {
            @Override
            public void onResult(boolean success) {
                callback.onResult(success);
            }

            @Override
            public void onError(String errorCode) {
                callback.onError(errorCode);
            }
        });
        dialog.show(caller.getChildFragmentManager(), TAG_DIALOG);
    }
}

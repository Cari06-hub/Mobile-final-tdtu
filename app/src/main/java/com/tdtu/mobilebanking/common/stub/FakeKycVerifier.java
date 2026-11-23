package com.tdtu.mobilebanking.common.stub;

import androidx.fragment.app.Fragment;
import com.tdtu.mobilebanking.common.contract.KycVerifier;

/**
 * FakeKycVerifier — luôn trả SUCCESS trong bản debug.
 * Thay bằng RealKycVerifier (Dev A, A4) trước IP-2.
 */
public class FakeKycVerifier implements KycVerifier {
    @Override
    public void verify(Fragment caller, Callback<Boolean> callback) {
        // Giả lập thành công sau 500ms
        caller.requireView().postDelayed(() -> callback.onResult(true), 500);
    }
}

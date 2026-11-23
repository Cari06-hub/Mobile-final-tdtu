package com.tdtu.mobilebanking.common.stub;

import com.tdtu.mobilebanking.common.contract.OtpService;

/**
 * FakeOtpService — OTP mặc định "123456" trong bản debug.
 * Thay bằng RealOtpService (Dev B, B3) trước IP-1.
 */
public class FakeOtpService implements OtpService {
    private static final String DEBUG_OTP = "123456";
    private String lastKey;

    @Override
    public void sendOtp(String idempotencyKey, long amount, String recipientEmail, Callback callback) {
        this.lastKey = idempotencyKey;
        android.util.Log.d("FakeOTP", "OTP sent for key=" + idempotencyKey + " | Use: " + DEBUG_OTP);
        callback.onSuccess();
    }

    @Override
    public void verifyOtp(String idempotencyKey, String otpInput, Callback callback) {
        if (DEBUG_OTP.equals(otpInput)) {
            callback.onSuccess();
        } else {
            callback.onError("OTP_FAILED");
        }
    }
}

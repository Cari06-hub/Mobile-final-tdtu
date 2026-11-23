package com.tdtu.mobilebanking.common.contract;

/**
 * Interface cho OTP 2FA — owner: Dev B.
 * Hash + TTL 60s, sai 3 lần hủy, gửi lại sau 30s.
 */
public interface OtpService {
    /** Sinh và gửi OTP gắn với idempotencyKey + amount */
    void sendOtp(String idempotencyKey, long amount, String recipientEmail, Callback callback);

    /** Xác thực OTP người dùng nhập */
    void verifyOtp(String idempotencyKey, String otpInput, Callback callback);

    interface Callback {
        void onSuccess();
        void onError(String errorCode); // OTP_FAILED, OTP_EXPIRED, OTP_LOCKED
    }
}

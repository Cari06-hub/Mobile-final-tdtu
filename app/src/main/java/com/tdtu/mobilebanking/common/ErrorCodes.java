package com.tdtu.mobilebanking.common;

public final class ErrorCodes {
    private ErrorCodes() {}

    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String LIMIT_EXCEEDED      = "LIMIT_EXCEEDED";
    public static final String KYC_REQUIRED        = "KYC_REQUIRED";
    public static final String FACE_FAILED         = "FACE_FAILED";
    public static final String OTP_FAILED          = "OTP_FAILED";
    public static final String OTP_EXPIRED         = "OTP_EXPIRED";
    public static final String OTP_LOCKED          = "OTP_LOCKED";
    public static final String DUPLICATE           = "DUPLICATE";
    public static final String INVALID_TARGET      = "INVALID_TARGET";
    public static final String NETWORK_ERROR       = "NETWORK_ERROR";
    public static final String PERMISSION_DENIED   = "PERMISSION_DENIED";
}

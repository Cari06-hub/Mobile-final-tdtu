package com.tdtu.mobilebanking.common.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {
    public String uid;
    public String role;         // CUSTOMER | OFFICER
    public String fullName;
    public String cccd;         // Số CCCD - bị khóa sau eKYC
    public String dob;          // Ngày sinh - bị khóa sau eKYC
    public String phone;
    public String email;
    public String address;
    public String kycStatus;    // PENDING | VERIFIED | REJECTED
    @ServerTimestamp
    public Date createdAt;

    public boolean isKycVerified() {
        return "VERIFIED".equals(kycStatus);
    }

    public boolean isOfficer() {
        return "OFFICER".equals(role);
    }
}

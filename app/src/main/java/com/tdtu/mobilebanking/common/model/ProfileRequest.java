package com.tdtu.mobilebanking.common.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.Map;

/** profileRequests/{id} — Yêu cầu sửa hồ sơ của khách, officer duyệt/từ chối */
public class ProfileRequest {
    public String id;
    public String uid;
    public Map<String, String> requestedChanges; // key: field, value: new value
    public String status;    // PENDING | APPROVED | REJECTED
    public String reason;    // Lý do từ chối
    public String reviewedBy;
    @ServerTimestamp
    public Date createdAt;
    @ServerTimestamp
    public Date reviewedAt;
}

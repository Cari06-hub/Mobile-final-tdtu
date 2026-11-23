package com.tdtu.mobilebanking.common.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Abstract Account model — owner: Dev B.
 */
public abstract class Account {
    public String id;
    public String ownerId;
    public String type;         // CHECKING | SAVING | MORTGAGE
    public long balance;        // VND, long
    public String status;       // ACTIVE | LOCKED | CLOSED
    public String lastTxId;
    @ServerTimestamp
    public Date createdAt;

    protected Account() {}

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}

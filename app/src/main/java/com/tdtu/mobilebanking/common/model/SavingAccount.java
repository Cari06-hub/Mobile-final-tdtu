package com.tdtu.mobilebanking.common.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class SavingAccount extends Account {
    public String productId;    // rates/{productId}
    public long principal;      // VND
    public int termMonths;
    @ServerTimestamp
    public Date openedAt;

    public SavingAccount() {
        this.type = "SAVING";
    }
}

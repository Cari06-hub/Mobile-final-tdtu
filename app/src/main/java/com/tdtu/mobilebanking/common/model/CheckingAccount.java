package com.tdtu.mobilebanking.common.model;

public class CheckingAccount extends Account {
    /** Hạn mức rút/chuyển mỗi ngày (VND) */
    public long dailyLimit;

    public CheckingAccount() {
        this.type = "CHECKING";
    }
}

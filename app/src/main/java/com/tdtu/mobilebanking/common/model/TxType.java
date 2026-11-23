package com.tdtu.mobilebanking.common.model;

public enum TxType {
    DEPOSIT_GATEWAY,
    WITHDRAW,
    TRANSFER_INTERNAL,
    TRANSFER_EXTERNAL,
    BILL,
    TOPUP_PHONE,
    CHECKOUT,
    SAVING_DEPOSIT,
    MORTGAGE_PAYMENT,
    COUNTER_DEPOSIT   // Officer nạp tiền tại quầy cho khách
}

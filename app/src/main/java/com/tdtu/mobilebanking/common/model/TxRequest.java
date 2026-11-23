package com.tdtu.mobilebanking.common.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

/**
 * Sealed request truyền vào ConfirmTransactionFragment — owner: Dev B.
 * Mọi luồng có tiền đều tạo TxRequest và điều hướng vào màn này.
 */
public final class TxRequest implements Serializable {
    /** UUID tạo khi mở màn, dùng làm ID document transactions/{id} — idempotency key */
    public String idempotencyKey;
    public TxType type;
    /** null nếu tiền đến từ VNPay/gateway */
    public String fromAccountId;
    /** null nếu tiền đi ra ngoài (hóa đơn, liên ngân hàng, rút) */
    public String toAccountId;
    /** Luôn > 0, đơn vị VND, lưu long */
    public long amount;
    /** Metadata: mã hóa đơn, ngân hàng nhận, mã vé, v.v. */
    public HashMap<String, String> meta;

    public TxRequest() {
        this.idempotencyKey = UUID.randomUUID().toString();
        this.meta = new HashMap<>();
    }

    /** Builder pattern để tạo request gọn hơn */
    public static class Builder {
        private final TxRequest req = new TxRequest();

        public Builder type(TxType type) { req.type = type; return this; }
        public Builder from(String fromAccountId) { req.fromAccountId = fromAccountId; return this; }
        public Builder to(String toAccountId) { req.toAccountId = toAccountId; return this; }
        public Builder amount(long amount) { req.amount = amount; return this; }
        public Builder meta(String key, String value) { req.meta.put(key, value); return this; }
        public TxRequest build() {
            if (req.type == null) throw new IllegalStateException("TxType is required");
            if (req.amount <= 0) throw new IllegalStateException("amount must be > 0");
            return req;
        }
    }
}

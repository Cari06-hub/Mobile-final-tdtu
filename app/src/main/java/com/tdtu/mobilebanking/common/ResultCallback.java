package com.tdtu.mobilebanking.common;

/** Callback chung cho Repository: trả kết quả hoặc lỗi. */
public interface ResultCallback<T> {
    void onSuccess(T result);

    void onError(Exception e);
}

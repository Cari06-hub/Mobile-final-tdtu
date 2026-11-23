package com.tdtu.mobilebanking.common.contract;

import com.tdtu.mobilebanking.common.model.Account;
import java.util.List;

/**
 * Interface AccountRepository — owner: Dev B.
 */
public interface AccountRepository {
    void getAccountsByOwner(String ownerId, Callback<List<Account>> callback);
    void getAccount(String accountId, Callback<Account> callback);
    void createAccount(Account account, Callback<String> callback);
    void updateAccount(Account account, Callback<Void> callback);

    interface Callback<T> {
        void onSuccess(T data);
        void onError(String errorCode);
    }
}

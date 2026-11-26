package com.tdtu.mobilebanking.mortgage;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.tdtu.mobilebanking.common.ResultCallback;

import java.util.List;

/** C3 - ViewModel khoản vay. */
public class MortgageViewModel extends ViewModel {

    private final MortgageRepository repo = new MortgageRepository();
    private final MutableLiveData<List<MortgageInfo>> myMortgages = new MutableLiveData<>();
    private final MutableLiveData<MortgageInfo> detail = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration listReg, detailReg;

    public LiveData<List<MortgageInfo>> getMyMortgages() { return myMortgages; }
    public LiveData<MortgageInfo> getDetail() { return detail; }
    public LiveData<String> getError() { return error; }

    public void loadMyMortgages() {
        if (listReg != null) return;
        listReg = repo.listenMyMortgages(callback(myMortgages));
    }

    public void loadDetail(String accountId) {
        if (detailReg != null) return;
        detailReg = repo.listenMortgage(accountId, callback(detail));
    }

    private <T> ResultCallback<T> callback(MutableLiveData<T> target) {
        return new ResultCallback<T>() {
            @Override
            public void onSuccess(T result) {
                target.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        };
    }

    @Override
    protected void onCleared() {
        if (listReg != null) listReg.remove();
        if (detailReg != null) detailReg.remove();
    }
}

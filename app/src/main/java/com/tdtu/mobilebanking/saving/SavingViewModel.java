package com.tdtu.mobilebanking.saving;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;
import com.tdtu.mobilebanking.common.ResultCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C2 - ViewModel (MVVM) của module tiết kiệm. Fragment chỉ observe LiveData.
 * Listener Firestore được gỡ trong onCleared() để không rò rỉ khi rời màn hình.
 */
public class SavingViewModel extends ViewModel {

    private final SavingRepository repo = new SavingRepository();

    private final MutableLiveData<List<SavingInfo>> mySavings = new MutableLiveData<>();
    private final MutableLiveData<SavingInfo> detail = new MutableLiveData<>();
    private final MutableLiveData<List<RateProduct>> rates = new MutableLiveData<>();
    private final MutableLiveData<Map<String, RateProduct>> rateMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ListenerRegistration savingsReg, detailReg, ratesReg;

    public LiveData<List<SavingInfo>> getMySavings() { return mySavings; }
    public LiveData<SavingInfo> getDetail() { return detail; }
    public LiveData<List<RateProduct>> getRates() { return rates; }
    public LiveData<Map<String, RateProduct>> getRateMap() { return rateMap; }
    public LiveData<String> getError() { return error; }

    public void loadMySavings() {
        if (savingsReg != null) return;
        savingsReg = repo.listenMySavings(callback(mySavings));
    }

    public void loadDetail(String accountId) {
        if (detailReg != null) return;
        detailReg = repo.listenSaving(accountId, callback(detail));
    }

    public void loadRates(boolean onlyActive) {
        if (ratesReg != null) return;
        ratesReg = repo.listenRates(onlyActive, new ResultCallback<List<RateProduct>>() {
            @Override
            public void onSuccess(List<RateProduct> list) {
                rates.setValue(list);
                Map<String, RateProduct> map = new HashMap<>();
                for (RateProduct p : list) map.put(p.id, p);
                rateMap.setValue(map);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    public Task<Void> saveRate(RateProduct p) {
        return repo.saveRate(p);
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
        if (savingsReg != null) savingsReg.remove();
        if (detailReg != null) detailReg.remove();
        if (ratesReg != null) ratesReg.remove();
    }
}

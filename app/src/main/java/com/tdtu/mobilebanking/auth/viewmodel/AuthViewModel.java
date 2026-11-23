package com.tdtu.mobilebanking.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tdtu.mobilebanking.auth.data.AuthRepository;
import com.tdtu.mobilebanking.common.model.User;

public class AuthViewModel extends ViewModel {
    private final AuthRepository repo = AuthRepository.getInstance();

    private final MutableLiveData<AuthResult> _result = new MutableLiveData<>();
    public final LiveData<AuthResult> result = _result;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public final LiveData<Boolean> loading = _loading;

    public void login(String email, String password) {
        _loading.setValue(true);
        repo.login(email, password, new AuthRepository.Callback() {
            @Override
            public void onSuccess(User user) {
                _loading.postValue(false);
                _result.postValue(AuthResult.success(user));
            }
            @Override
            public void onError(String message) {
                _loading.postValue(false);
                _result.postValue(AuthResult.failure(message));
            }
        });
    }

    public void register(String email, String password, User userInfo) {
        _loading.setValue(true);
        repo.register(email, password, userInfo, new AuthRepository.Callback() {
            @Override
            public void onSuccess(User user) {
                _loading.postValue(false);
                _result.postValue(AuthResult.success(user));
            }
            @Override
            public void onError(String message) {
                _loading.postValue(false);
                _result.postValue(AuthResult.failure(message));
            }
        });
    }

    public void sendPasswordReset(String email) {
        _loading.setValue(true);
        repo.sendPasswordResetEmail(email, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                _loading.postValue(false);
                _result.postValue(AuthResult.resetSent());
            }
            @Override
            public void onError(String message) {
                _loading.postValue(false);
                _result.postValue(AuthResult.failure(message));
            }
        });
    }

    // Wrapper class cho kết quả
    public static class AuthResult {
        public enum Type { SUCCESS, FAILURE, RESET_SENT }
        public final Type type;
        public final User user;
        public final String error;

        private AuthResult(Type type, User user, String error) {
            this.type = type;
            this.user = user;
            this.error = error;
        }

        public static AuthResult success(User user) { return new AuthResult(Type.SUCCESS, user, null); }
        public static AuthResult failure(String error) { return new AuthResult(Type.FAILURE, null, error); }
        public static AuthResult resetSent() { return new AuthResult(Type.RESET_SENT, null, null); }
    }
}

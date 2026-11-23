package com.tdtu.mobilebanking.auth.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tdtu.mobilebanking.common.model.User;

public class AuthRepository {
    private static AuthRepository instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public static AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void login(String email, String password, Callback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    fetchUserRole(uid, callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void fetchUserRole(String uid, Callback callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.uid = uid;
                            callback.onSuccess(user);
                        } else {
                            callback.onError("USER_NOT_FOUND");
                        }
                    } else {
                        callback.onError("USER_NOT_FOUND");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void register(String email, String password, User userInfo, Callback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    userInfo.uid = uid;
                    userInfo.role = "CUSTOMER";
                    userInfo.kycStatus = "PENDING";
                    db.collection("users").document(uid).set(userInfo)
                            .addOnSuccessListener(v -> callback.onSuccess(userInfo))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendPasswordResetEmail(String email, SimpleCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void logout() {
        auth.signOut();
    }

    public interface Callback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }
}

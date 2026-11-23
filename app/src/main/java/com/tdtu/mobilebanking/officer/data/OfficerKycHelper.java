package com.tdtu.mobilebanking.officer.data;

import android.content.Context;
import android.util.Patterns;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Officer tạo tài khoản khách mà KHÔNG bị đăng xuất.
 * - Firebase Auth: chạy trên FirebaseApp phụ "officerCreate".
 * - Firestore: chạy trên app MẶC ĐỊNH (officer đang đăng nhập) để Rules nhận quyền officer.
 * Owner: Dev A (A3)
 */
public class OfficerKycHelper {

    public interface Callback {
        void onSuccess(String customerUid, String accountId, String accountNumber);
        void onError(String message);
    }

    private interface WriteResult {
        void ok(String accountId, String accountNumber);
        void fail(Exception e);
    }

    private static final String SECONDARY_APP = "officerCreate";
    private static final int MAX_TRIES = 3;
    private static final long DEFAULT_DAILY_LIMIT = 50_000_000L;
    private static final SecureRandom RNG = new SecureRandom();

    private final Context ctx;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private boolean busy = false;

    public OfficerKycHelper(Context context) {
        this.ctx = context.getApplicationContext();
    }

    /** Tạo khách hàng + tài khoản Checking mặc định trong 1 lần gọi. */
    public void createCustomerWithChecking(String email, String tempPassword,
                                           String fullName, String phone,
                                           String cccd, String dob, String address,
                                           Callback cb) {
        if (busy) { cb.onError("Đang xử lý, vui lòng chờ."); return; }
        String err = validate(email, tempPassword, fullName, phone, cccd);
        if (err != null) { cb.onError(err); return; }

        FirebaseUser officer = FirebaseAuth.getInstance().getCurrentUser();
        if (officer == null) { cb.onError("Phiên officer hết hạn."); return; }

        busy = true;
        FirebaseApp secondaryApp = getOrCreateSecondaryApp();
        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(email.trim(), tempPassword)
                .addOnSuccessListener(r -> {
                    FirebaseUser newUser = r.getUser();
                    if (newUser == null) {
                        cleanup(secondaryApp, secondaryAuth);
                        cb.onError("Không tạo được tài khoản đăng nhập.");
                        return;
                    }
                    Map<String, Object> profile = buildProfile(
                            newUser.getUid(), email.trim(), fullName.trim(),
                            phone.trim(), cccd.trim(), dob, address, officer.getUid());

                    writeProfileAndAccount(newUser.getUid(), fullName.trim(),
                            officer.getUid(), profile, 1, new WriteResult() {
                                @Override public void ok(String accId, String accNum) {
                                    cleanup(secondaryApp, secondaryAuth);
                                    cb.onSuccess(newUser.getUid(), accId, accNum);
                                }
                                @Override public void fail(Exception e) {
                                    newUser.delete().addOnCompleteListener(t -> {
                                        cleanup(secondaryApp, secondaryAuth);
                                        cb.onError("Lỗi lưu hồ sơ: " + e.getMessage());
                                    });
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    cleanup(secondaryApp, secondaryAuth);
                    cb.onError(mapAuthError(e));
                });
    }

    /**
     * Mở thêm tài khoản Saving hoặc Mortgage cho khách đã có.
     * Chỉ ghi vào Firestore (không tạo thêm Firebase Auth user).
     */
    public void openSavingAccount(String ownerId, String productId,
                                   long principal, int termMonths,
                                   String officerUid, Callback cb) {
        if (busy) { cb.onError("Đang xử lý."); return; }
        busy = true;
        String accountNumber = newAccountNumber();
        DocumentReference dirRef = db.collection("accountDirectory").document(accountNumber);

        dirRef.get(Source.SERVER).addOnSuccessListener(snap -> {
            if (snap.exists()) {
                busy = false;
                // thử lại với số tài khoản khác
                openSavingAccount(ownerId, productId, principal, termMonths, officerUid, cb);
                return;
            }
            DocumentReference accRef = db.collection("accounts").document();

            Map<String, Object> acc = new HashMap<>();
            acc.put("ownerId", ownerId);
            acc.put("accountNumber", accountNumber);
            acc.put("type", "SAVING");
            acc.put("balance", principal);
            acc.put("currency", "VND");
            acc.put("status", "ACTIVE");
            acc.put("lastTxId", null);
            acc.put("productId", productId);
            acc.put("principal", principal);
            acc.put("termMonths", termMonths);
            acc.put("openedAt", FieldValue.serverTimestamp());
            acc.put("createdBy", officerUid);
            acc.put("createdAt", FieldValue.serverTimestamp());
            acc.put("dailyLimit", 0L);

            Map<String, Object> dir = new HashMap<>();
            dir.put("accountId", accRef.getId());
            dir.put("ownerId", ownerId);
            dir.put("ownerName", "");
            dir.put("type", "SAVING");
            dir.put("status", "ACTIVE");

            WriteBatch batch = db.batch();
            batch.set(accRef, acc);
            batch.set(dirRef, dir);
            batch.commit()
                    .addOnSuccessListener(v -> { busy = false; cb.onSuccess(ownerId, accRef.getId(), accountNumber); })
                    .addOnFailureListener(e -> { busy = false; cb.onError("Lỗi: " + e.getMessage()); });
        }).addOnFailureListener(e -> { busy = false; cb.onError("Lỗi: " + e.getMessage()); });
    }

    public void openMortgageAccount(String ownerId, long principal, double annualRate,
                                     int termMonths, String frequency, String startDate,
                                     String officerUid, Callback cb) {
        if (busy) { cb.onError("Đang xử lý."); return; }
        busy = true;
        String accountNumber = newAccountNumber();
        DocumentReference dirRef = db.collection("accountDirectory").document(accountNumber);

        dirRef.get(Source.SERVER).addOnSuccessListener(snap -> {
            if (snap.exists()) {
                busy = false;
                openMortgageAccount(ownerId, principal, annualRate, termMonths,
                        frequency, startDate, officerUid, cb);
                return;
            }
            DocumentReference accRef = db.collection("accounts").document();

            Map<String, Object> acc = new HashMap<>();
            acc.put("ownerId", ownerId);
            acc.put("accountNumber", accountNumber);
            acc.put("type", "MORTGAGE");
            acc.put("balance", 0L);
            acc.put("currency", "VND");
            acc.put("status", "ACTIVE");
            acc.put("lastTxId", null);
            acc.put("principal", principal);
            acc.put("annualRate", annualRate);
            acc.put("termMonths", termMonths);
            acc.put("frequency", frequency);
            acc.put("startDate", startDate);
            acc.put("outstanding", principal);
            acc.put("createdBy", officerUid);
            acc.put("createdAt", FieldValue.serverTimestamp());
            acc.put("dailyLimit", 0L);

            Map<String, Object> dir = new HashMap<>();
            dir.put("accountId", accRef.getId());
            dir.put("ownerId", ownerId);
            dir.put("ownerName", "");
            dir.put("type", "MORTGAGE");
            dir.put("status", "ACTIVE");

            WriteBatch batch = db.batch();
            batch.set(accRef, acc);
            batch.set(dirRef, dir);
            batch.commit()
                    .addOnSuccessListener(v -> { busy = false; cb.onSuccess(ownerId, accRef.getId(), accountNumber); })
                    .addOnFailureListener(e -> { busy = false; cb.onError("Lỗi: " + e.getMessage()); });
        }).addOnFailureListener(e -> { busy = false; cb.onError("Lỗi: " + e.getMessage()); });
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void writeProfileAndAccount(String uid, String fullName, String officerUid,
                                         Map<String, Object> profile, int attempt, WriteResult out) {
        String accountNumber = newAccountNumber();
        DocumentReference dirRef = db.collection("accountDirectory").document(accountNumber);

        dirRef.get(Source.SERVER).addOnSuccessListener(snap -> {
            if (snap.exists()) {
                if (attempt >= MAX_TRIES) { out.fail(new Exception("Không sinh được số TK")); return; }
                writeProfileAndAccount(uid, fullName, officerUid, profile, attempt + 1, out);
                return;
            }
            DocumentReference userRef = db.collection("users").document(uid);
            DocumentReference accRef  = db.collection("accounts").document();

            Map<String, Object> acc = new HashMap<>();
            acc.put("ownerId", uid);
            acc.put("accountNumber", accountNumber);
            acc.put("type", "CHECKING");
            acc.put("balance", 0L);
            acc.put("currency", "VND");
            acc.put("status", "ACTIVE");
            acc.put("dailyLimit", DEFAULT_DAILY_LIMIT);
            acc.put("lastTxId", null);
            acc.put("createdBy", officerUid);
            acc.put("createdAt", FieldValue.serverTimestamp());

            Map<String, Object> dir = new HashMap<>();
            dir.put("accountId", accRef.getId());
            dir.put("ownerId", uid);
            dir.put("ownerName", fullName);
            dir.put("type", "CHECKING");
            dir.put("status", "ACTIVE");

            WriteBatch batch = db.batch();
            batch.set(userRef, profile);
            batch.set(accRef, acc);
            batch.set(dirRef, dir);
            batch.commit()
                    .addOnSuccessListener(v -> out.ok(accRef.getId(), accountNumber))
                    .addOnFailureListener(out::fail);
        }).addOnFailureListener(out::fail);
    }

    private static Map<String, Object> buildProfile(String uid, String email, String fullName,
                                                     String phone, String cccd, String dob,
                                                     String address, String officerUid) {
        Map<String, Object> m = new HashMap<>();
        m.put("uid", uid);
        m.put("email", email);
        m.put("fullName", fullName);
        m.put("phone", phone);
        m.put("cccd", cccd);
        m.put("dob", dob);
        m.put("address", address);
        m.put("role", "customer");
        m.put("kycStatus", "PENDING");
        m.put("status", "ACTIVE");
        m.put("mustChangePassword", true);
        m.put("createdBy", officerUid);
        m.put("createdAt", FieldValue.serverTimestamp());
        return m;
    }

    private FirebaseApp getOrCreateSecondaryApp() {
        try { return FirebaseApp.getInstance(SECONDARY_APP); }
        catch (IllegalStateException e) {
            return FirebaseApp.initializeApp(ctx, FirebaseApp.getInstance().getOptions(), SECONDARY_APP);
        }
    }

    private void cleanup(FirebaseApp app, FirebaseAuth auth) {
        auth.signOut();
        try { app.delete(); } catch (Exception ignored) {}
        busy = false;
    }

    private static String newAccountNumber() {
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 11; i++) sb.append(RNG.nextInt(10));
        return sb.toString();
    }

    private static String validate(String email, String password, String fullName,
                                    String phone, String cccd) {
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())
            return "Email không hợp lệ.";
        if (password == null || password.length() < 6)
            return "Mật khẩu tạm phải ít nhất 6 ký tự.";
        if (fullName == null || fullName.trim().isEmpty())
            return "Chưa nhập họ tên.";
        if (phone == null || !phone.trim().matches("0\\d{9}"))
            return "SĐT phải 10 số, bắt đầu bằng 0.";
        if (cccd == null || !cccd.trim().matches("\\d{12}"))
            return "CCCD phải 12 chữ số.";
        return null;
    }

    private static String mapAuthError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) return "Email đã có tài khoản.";
        if (e instanceof FirebaseAuthWeakPasswordException)  return "Mật khẩu quá yếu (tối thiểu 6 ký tự).";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "Email không hợp lệ.";
        return "Lỗi: " + e.getMessage();
    }
}

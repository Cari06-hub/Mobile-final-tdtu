package com.tdtu.mobilebanking.profile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.common.contract.KycVerifier;
import com.tdtu.mobilebanking.databinding.FragmentCustomerProfileBinding;
import com.tdtu.mobilebanking.kyc.RealKycVerifier;

import java.util.HashMap;
import java.util.Map;

/** A5 — Customer views and requests edits to their profile */
public class CustomerProfileFragment extends Fragment {

    private FragmentCustomerProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Original values to detect changes
    private String originalPhone = "";
    private String originalEmail = "";
    private String originalAddress = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        loadProfile();

        binding.btnSave.setOnClickListener(v -> submitChanges());
        binding.btnChangePassword.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profile_to_changePassword));
        binding.btnChangePin.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profile_to_changePin));
        binding.btnStartKyc.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_kyc));
        binding.btnTestFaceVerify.setOnClickListener(v ->
                new RealKycVerifier().verify(this, new KycVerifier.Callback<Boolean>() {
                    @Override
                    public void onResult(Boolean result) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                Boolean.TRUE.equals(result)
                                        ? "eKYC khuôn mặt: HỢP LỆ"
                                        : "eKYC khuôn mặt: THẤT BẠI",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorCode) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Lỗi eKYC: " + errorCode, Toast.LENGTH_LONG).show();
                    }
                }));
        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            NavOptions opts = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_root, true)
                    .build();
            Navigation.findNavController(v).navigate(R.id.nav_auth, null, opts);
        });
    }

    private void loadProfile() {
        if (currentUser == null) return;
        setLoading(true);
        db.collection("users").document(currentUser.getUid())
                .get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    setLoading(false);
                    if (!doc.exists()) return;

                    String kycStatus = doc.getString("kycStatus");
                    boolean verified = "VERIFIED".equals(kycStatus);

                    binding.etFullName.setText(doc.getString("fullName"));
                    binding.etFullName.setEnabled(false); // always locked

                    binding.etCccd.setText(doc.getString("cccd"));
                    binding.etCccd.setEnabled(!verified); // locked after KYC

                    binding.etDob.setText(doc.getString("dob"));
                    binding.etDob.setEnabled(!verified); // locked after KYC

                    originalPhone = safeStr(doc.getString("phone"));
                    originalEmail = safeStr(doc.getString("email"));
                    originalAddress = safeStr(doc.getString("address"));

                    binding.etPhone.setText(originalPhone);
                    binding.etEmail.setText(originalEmail);
                    binding.etAddress.setText(originalAddress);

                    binding.tvKycStatus.setText("KYC: " + (kycStatus != null ? kycStatus : "PENDING"));
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Lỗi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void submitChanges() {
        String newPhone = binding.etPhone.getText().toString().trim();
        String newEmail = binding.etEmail.getText().toString().trim();
        String newAddress = binding.etAddress.getText().toString().trim();

        boolean changed = !newPhone.equals(originalPhone)
                || !newEmail.equals(originalEmail)
                || !newAddress.equals(originalAddress);

        if (!changed) {
            Toast.makeText(requireContext(), "Không có thay đổi nào", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPhone.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Điện thoại và email không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Build requested changes map
        Map<String, String> requestedChanges = new HashMap<>();
        if (!newPhone.equals(originalPhone)) requestedChanges.put("phone", newPhone);
        if (!newEmail.equals(originalEmail)) requestedChanges.put("email", newEmail);
        if (!newAddress.equals(originalAddress)) requestedChanges.put("address", newAddress);

        // Write to profileRequests — officer must approve
        Map<String, Object> request = new HashMap<>();
        request.put("uid", currentUser.getUid());
        request.put("requestedChanges", requestedChanges);
        request.put("status", "PENDING");
        request.put("createdAt", FieldValue.serverTimestamp());

        db.collection("profileRequests").add(request)
                .addOnSuccessListener(ref -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Đã gửi yêu cầu. Nhân viên sẽ duyệt trong thời gian sớm nhất.",
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Lỗi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.tdtu.mobilebanking.profile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.tdtu.mobilebanking.databinding.FragmentChangePinBinding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/** A5 — Customer sets or changes their 6-digit transaction PIN */
public class ChangePinFragment extends Fragment {

    private FragmentChangePinBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePinBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        binding.btnConfirm.setOnClickListener(v -> changePin(view));
    }

    private void changePin(View view) {
        String currentPin = binding.etCurrentPin.getText().toString().trim();
        String newPin = binding.etNewPin.getText().toString().trim();
        String confirmPin = binding.etConfirmPin.getText().toString().trim();

        if (newPin.length() != 6 || confirmPin.length() != 6) {
            Toast.makeText(requireContext(), "PIN phải đúng 6 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPin.equals(confirmPin)) {
            binding.etConfirmPin.setError("PIN xác nhận không khớp");
            return;
        }
        if (currentUser == null) return;

        setLoading(true);
        // Load current pinHash from Firestore
        db.collection("users").document(currentUser.getUid())
                .get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String storedHash = doc.getString("pinHash");
                    if (storedHash != null) {
                        // Existing PIN: verify currentPin
                        if (currentPin.length() != 6) {
                            setLoading(false);
                            binding.etCurrentPin.setError("Nhập PIN hiện tại (6 số)");
                            return;
                        }
                        String enteredHash = sha256(currentPin);
                        if (!storedHash.equals(enteredHash)) {
                            setLoading(false);
                            Toast.makeText(requireContext(), "PIN hiện tại không đúng", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // Save new PIN hash
                    String newHash = sha256(newPin);
                    Map<String, Object> update = new HashMap<>();
                    update.put("pinHash", newHash);
                    db.collection("users").document(currentUser.getUid())
                            .update(update)
                            .addOnSuccessListener(u -> {
                                setLoading(false);
                                Toast.makeText(requireContext(), "Đổi PIN thành công", Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(view).navigateUp();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(requireContext(), "Lỗi lưu PIN: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** SHA-256 hash of a string, returned as hex */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnConfirm.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

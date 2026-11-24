package com.tdtu.mobilebanking.officer.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;
import com.tdtu.mobilebanking.databinding.FragmentProfileRequestDetailBinding;

import java.util.Map;

/** A6 — Officer reviews a single profile-change request and approves or rejects it */
public class ProfileRequestDetailFragment extends Fragment {

    private FragmentProfileRequestDetailBinding binding;
    private FirebaseFirestore db;
    private String requestId;
    private String customerUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileRequestDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        requestId = getArguments() != null ? getArguments().getString("requestId") : null;

        if (requestId == null) {
            Navigation.findNavController(view).navigateUp();
            return;
        }
        loadRequest(view);
    }

    @SuppressWarnings("unchecked")
    private void loadRequest(View view) {
        setLoading(true);
        db.collection("profileRequests").document(requestId)
                .get(Source.SERVER)
                .addOnSuccessListener(reqDoc -> {
                    if (!isAdded()) return;
                    customerUid = reqDoc.getString("uid");
                    Map<String, String> changes = (Map<String, String>) reqDoc.get("requestedChanges");

                    // Load current user data to show diff
                    if (customerUid == null) { setLoading(false); return; }
                    db.collection("users").document(customerUid)
                            .get(Source.SERVER)
                            .addOnSuccessListener(userDoc -> {
                                if (!isAdded()) return;
                                setLoading(false);

                                // Current values
                                binding.tvCurrentPhone.setText(safeStr(userDoc.getString("phone")));
                                binding.tvCurrentEmail.setText(safeStr(userDoc.getString("email")));
                                binding.tvCurrentAddress.setText(safeStr(userDoc.getString("address")));
                                binding.tvCustomerName.setText(safeStr(userDoc.getString("fullName")));

                                // Requested changes
                                if (changes != null) {
                                    binding.tvNewPhone.setText(safeStr(changes.get("phone")));
                                    binding.tvNewEmail.setText(safeStr(changes.get("email")));
                                    binding.tvNewAddress.setText(safeStr(changes.get("address")));
                                }

                                binding.btnApprove.setOnClickListener(v -> approve(changes, view));
                                binding.btnReject.setOnClickListener(v -> showRejectDialog(view));
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void approve(Map<String, String> changes, View view) {
        if (customerUid == null || changes == null) return;
        setLoading(true);
        String officerUid = FirebaseAuth.getInstance().getUid();

        WriteBatch batch = db.batch();
        // Update user document with new values
        for (Map.Entry<String, String> entry : changes.entrySet()) {
            batch.update(db.collection("users").document(customerUid),
                    entry.getKey(), entry.getValue());
        }
        // Update request status
        batch.update(db.collection("profileRequests").document(requestId),
                "status", "APPROVED",
                "reviewedBy", officerUid,
                "reviewedAt", FieldValue.serverTimestamp());

        batch.commit()
                .addOnSuccessListener(u -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Đã duyệt yêu cầu", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).navigateUp();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showRejectDialog(View view) {
        EditText etReason = new EditText(requireContext());
        etReason.setHint("Lý do từ chối");
        new AlertDialog.Builder(requireContext())
                .setTitle("Từ chối yêu cầu")
                .setView(etReason)
                .setPositiveButton("Xác nhận", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(requireContext(), "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reject(reason, view);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void reject(String reason, View view) {
        setLoading(true);
        String officerUid = FirebaseAuth.getInstance().getUid();
        db.collection("profileRequests").document(requestId)
                .update("status", "REJECTED",
                        "reviewedBy", officerUid,
                        "reviewedAt", FieldValue.serverTimestamp(),
                        "reason", reason)
                .addOnSuccessListener(u -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Đã từ chối yêu cầu", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).navigateUp();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnApprove.setEnabled(!loading);
        binding.btnReject.setEnabled(!loading);
    }

    private String safeStr(String s) { return s != null ? s : "—"; }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

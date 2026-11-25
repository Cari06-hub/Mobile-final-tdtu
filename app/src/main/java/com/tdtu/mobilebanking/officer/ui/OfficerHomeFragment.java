package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentOfficerHomeBinding;

import java.util.Arrays;
import java.util.List;

/**
 * Officer Dashboard — owned by Dev A.
 * Điều hướng sang các chức năng officer.
 * D4: tìm kiếm khách hàng theo email, CCCD, số điện thoại hoặc họ tên.
 */
public class OfficerHomeFragment extends Fragment {
    private static final List<String> SEARCH_FIELDS =
            Arrays.asList("email", "cccd", "phone", "fullName");

    private FragmentOfficerHomeBinding binding;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOfficerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        binding.btnCreateCustomer.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_officer_home_to_create_customer));

        binding.btnOpenAccount.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_officer_home_to_open_account));

        binding.btnProfileRequests.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_officer_home_to_profile_requests));

        binding.btnRateManagement.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_officer_home_to_rate_management));

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            NavOptions opts = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_root, true)
                    .build();
            Navigation.findNavController(v).navigate(R.id.nav_auth, null, opts);
        });

        binding.btnSearch.setOnClickListener(v -> searchCustomer());
    }

    private void searchCustomer() {
        String keyword = binding.etSearch.getText() != null
                ? binding.etSearch.getText().toString().trim() : "";
        if (TextUtils.isEmpty(keyword)) {
            Snackbar.make(requireView(), "Nhập email, CCCD, SĐT hoặc họ tên", Snackbar.LENGTH_SHORT).show();
            return;
        }
        binding.progressSearch.setVisibility(View.VISIBLE);
        binding.btnSearch.setEnabled(false);
        searchByFieldIndex(keyword, 0);
    }

    /** Query từng trường đơn lẻ để dùng built-in single-field index (tránh lỗi thiếu composite index). */
    private void searchByFieldIndex(String keyword, int fieldIdx) {
        if (fieldIdx >= SEARCH_FIELDS.size()) {
            resetSearch();
            if (isAdded()) {
                Snackbar.make(requireView(),
                        "Không tìm thấy khách hàng với từ khóa: " + keyword,
                        Snackbar.LENGTH_LONG).show();
            }
            return;
        }

        String field = SEARCH_FIELDS.get(fieldIdx);
        db.collection("users")
                .whereEqualTo(field, keyword)
                .limit(5)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded()) return;
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        if ("customer".equals(doc.getString("role"))) {
                            navigateToCustomer(doc.getId());
                            return;
                        }
                    }
                    searchByFieldIndex(keyword, fieldIdx + 1);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    resetSearch();
                    Snackbar.make(requireView(), "Lỗi tìm kiếm: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void navigateToCustomer(String uid) {
        resetSearch();
        Bundle args = new Bundle();
        args.putString("customerId", uid);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_officer_home_to_customer_detail, args);
    }

    private void resetSearch() {
        if (!isAdded() || binding == null) return;
        binding.progressSearch.setVisibility(View.GONE);
        binding.btnSearch.setEnabled(true);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}

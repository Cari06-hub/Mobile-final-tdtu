package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentOpenAccountBinding;
import com.tdtu.mobilebanking.officer.data.OfficerKycHelper;

public class OpenAccountFragment extends Fragment {
    private FragmentOpenAccountBinding binding;
    private OfficerKycHelper helper;
    private String selectedType = "SAVING";

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentOpenAccountBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        helper = new OfficerKycHelper(requireContext());

        if (getArguments() != null) {
            String prefillUid = getArguments().getString("customerId", "");
            if (!prefillUid.isEmpty()) {
                binding.etCustomerId.setText(prefillUid);
            }
        }

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Chọn loại tài khoản
        binding.rgAccountType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSaving) {
                selectedType = "SAVING";
                binding.layoutSaving.setVisibility(View.VISIBLE);
                binding.layoutMortgage.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbMortgage) {
                selectedType = "MORTGAGE";
                binding.layoutSaving.setVisibility(View.GONE);
                binding.layoutMortgage.setVisibility(View.VISIBLE);
            }
        });

        binding.btnOpenAccount.setOnClickListener(v -> {
            String ownerId = binding.etCustomerId.getText().toString().trim();
            if (ownerId.isEmpty()) {
                Snackbar.make(requireView(), "Nhập UID khách hàng", Snackbar.LENGTH_SHORT).show();
                return;
            }
            String officerUid = FirebaseAuth.getInstance().getUid();
            if (officerUid == null) return;

            setLoading(true);
            if ("SAVING".equals(selectedType)) {
                openSaving(ownerId, officerUid);
            } else {
                openMortgage(ownerId, officerUid);
            }
        });
    }

    private void openSaving(String ownerId, String officerUid) {
        String productId = binding.etProductId.getText().toString().trim();
        String principalStr = binding.etPrincipal.getText().toString().trim();
        String termStr = binding.etTermMonths.getText().toString().trim();
        if (productId.isEmpty() || principalStr.isEmpty() || termStr.isEmpty()) {
            Snackbar.make(requireView(), "Điền đủ thông tin tiết kiệm", Snackbar.LENGTH_SHORT).show();
            setLoading(false); return;
        }
        long principal = Long.parseLong(principalStr);
        int term = Integer.parseInt(termStr);
        helper.openSavingAccount(ownerId, productId, principal, term, officerUid, makeCallback());
    }

    private void openMortgage(String ownerId, String officerUid) {
        String principalStr = binding.etMortgagePrincipal.getText().toString().trim();
        String rateStr = binding.etAnnualRate.getText().toString().trim();
        String termStr = binding.etMortgageTerm.getText().toString().trim();
        String startDate = binding.etStartDate.getText().toString().trim();
        String frequency = binding.rbMonthly.isChecked() ? "MONTHLY" : "BIWEEKLY";
        if (principalStr.isEmpty() || rateStr.isEmpty() || termStr.isEmpty() || startDate.isEmpty()) {
            Snackbar.make(requireView(), "Điền đủ thông tin vay", Snackbar.LENGTH_SHORT).show();
            setLoading(false); return;
        }
        helper.openMortgageAccount(ownerId, Long.parseLong(principalStr),
                Double.parseDouble(rateStr), Integer.parseInt(termStr),
                frequency, startDate, officerUid, makeCallback());
    }

    private OfficerKycHelper.Callback makeCallback() {
        return new OfficerKycHelper.Callback() {
            @Override public void onSuccess(String uid, String accId, String accNum) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Snackbar.make(requireView(), "✅ Mở tài khoản thành công. Số TK: " + accNum,
                            Snackbar.LENGTH_LONG).show();
                    requireView().postDelayed(() -> {
                        if (isAdded()) Navigation.findNavController(requireView()).navigateUp();
                    }, 1500);
                });
            }
            @Override public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> { setLoading(false);
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show(); });
            }
        };
    }

    private void setLoading(boolean on) {
        binding.btnOpenAccount.setEnabled(!on);
        binding.progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}

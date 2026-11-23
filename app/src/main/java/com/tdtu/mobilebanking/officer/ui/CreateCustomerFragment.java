package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentCreateCustomerBinding;
import com.tdtu.mobilebanking.officer.data.OfficerKycHelper;

public class CreateCustomerFragment extends Fragment {
    private FragmentCreateCustomerBinding binding;
    private OfficerKycHelper helper;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentCreateCustomerBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        helper = new OfficerKycHelper(requireContext());

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.btnCreate.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etTempPassword.getText().toString().trim();
            String name     = binding.etFullName.getText().toString().trim();
            String phone    = binding.etPhone.getText().toString().trim();
            String cccd     = binding.etCccd.getText().toString().trim();
            String dob      = binding.etDob.getText().toString().trim();
            String address  = binding.etAddress.getText().toString().trim();

            setLoading(true);
            helper.createCustomerWithChecking(email, password, name, phone, cccd, dob, address,
                    new OfficerKycHelper.Callback() {
                        @Override
                        public void onSuccess(String uid, String accId, String accNum) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                setLoading(false);
                                Snackbar.make(requireView(),
                                        "✅ Đã tạo khách. Số TK: " + accNum,
                                        Snackbar.LENGTH_LONG).show();
                                // Quay lại dashboard sau 1.5s
                                requireView().postDelayed(() -> {
                                    if (isAdded()) Navigation.findNavController(requireView()).navigateUp();
                                }, 1500);
                            });
                        }
                        @Override
                        public void onError(String message) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                setLoading(false);
                                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                            });
                        }
                    });
        });
    }

    private void setLoading(boolean on) {
        binding.btnCreate.setEnabled(!on);
        binding.progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}

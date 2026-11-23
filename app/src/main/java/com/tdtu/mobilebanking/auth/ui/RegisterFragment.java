package com.tdtu.mobilebanking.auth.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.auth.viewmodel.AuthViewModel;
import com.tdtu.mobilebanking.common.model.User;
import com.tdtu.mobilebanking.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        binding.btnRegister.setOnClickListener(v -> {
            if (!validateInput()) return;
            User user = buildUser();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            viewModel.register(email, password, user);
        });

        viewModel.loading.observe(getViewLifecycleOwner(), loading ->
                binding.btnRegister.setEnabled(!loading));

        viewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.type == AuthViewModel.AuthResult.Type.SUCCESS) {
                // Điều hướng về login sau khi đăng ký xong
                Snackbar.make(requireView(), "Đăng ký thành công! Vui lòng đăng nhập.",
                        Snackbar.LENGTH_LONG).show();
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_register_to_login);
            } else {
                Snackbar.make(requireView(), result.error != null ? result.error : "Lỗi đăng ký",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInput() {
        boolean valid = true;
        String name = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String cccd = binding.etCccd.getText().toString().trim();

        if (name.isEmpty()) { binding.tilFullName.setError(getString(R.string.auth_error_name_empty)); valid = false; }
        else binding.tilFullName.setError(null);

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.auth_error_email_invalid)); valid = false;
        } else binding.tilEmail.setError(null);

        if (password.length() < 8) {
            binding.tilPassword.setError(getString(R.string.auth_error_password_short)); valid = false;
        } else binding.tilPassword.setError(null);

        if (!cccd.matches("\\d{12}")) {
            binding.tilCccd.setError(getString(R.string.auth_error_cccd_invalid)); valid = false;
        } else binding.tilCccd.setError(null);

        return valid;
    }

    private User buildUser() {
        User user = new User();
        user.fullName = binding.etFullName.getText().toString().trim();
        user.email = binding.etEmail.getText().toString().trim();
        user.phone = binding.etPhone.getText().toString().trim();
        user.cccd = binding.etCccd.getText().toString().trim();
        user.dob = binding.etDob.getText().toString().trim();
        user.address = binding.etAddress.getText().toString().trim();
        return user;
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}

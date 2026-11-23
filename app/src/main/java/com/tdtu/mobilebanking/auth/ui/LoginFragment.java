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
import com.tdtu.mobilebanking.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Auto-login nếu đã có session
        if (viewModel.isLoggedIn()) {
            fetchRoleAndRoute();
            return;
        }

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            if (!validateInput(email, password)) return;
            viewModel.login(email, password);
        });

        binding.tvRegister.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_login_to_register));

        binding.tvForgotPassword.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_login_to_forgot));
    }

    private boolean validateInput(String email, String password) {
        boolean valid = true;
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.auth_error_email_invalid));
            valid = false;
        } else binding.tilEmail.setError(null);

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.auth_error_password_empty));
            valid = false;
        } else binding.tilPassword.setError(null);
        return valid;
    }

    private void observeViewModel() {
        viewModel.loading.observe(getViewLifecycleOwner(), loading -> {
            binding.btnLogin.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.type) {
                case SUCCESS:
                    routeByRole(result.user.role);
                    break;
                case FAILURE:
                    Snackbar.make(requireView(),
                            result.error != null ? result.error : getString(R.string.auth_error_wrong_credentials),
                            Snackbar.LENGTH_LONG).show();
                    break;
            }
        });
    }

    /** Đọc role từ Firestore rồi điều hướng (dùng khi auto-login). */
    private void fetchRoleAndRoute() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);
        viewModel.fetchCurrentUserRole(role -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);
                if (role != null) routeByRole(role);
            });
        });
    }

    private void routeByRole(String role) {
        if (!isAdded()) return;
        if ("officer".equals(role)) {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_login_to_officer_home);
        } else {
            // customer → D2: navigate to customer home (stub hiện tại)
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_login_to_home);
        }
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}

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
import com.tdtu.mobilebanking.databinding.FragmentForgotPasswordBinding;

public class ForgotPasswordFragment extends Fragment {
    private FragmentForgotPasswordBinding binding;
    private AuthViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        binding.btnSendReset.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.setError(getString(R.string.auth_error_email_invalid));
                return;
            }
            binding.tilEmail.setError(null);
            viewModel.sendPasswordReset(email);
        });

        viewModel.loading.observe(getViewLifecycleOwner(), loading ->
                binding.btnSendReset.setEnabled(!loading));

        viewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.type == AuthViewModel.AuthResult.Type.RESET_SENT) {
                Snackbar.make(requireView(), getString(R.string.auth_success_reset_sent),
                        Snackbar.LENGTH_LONG).show();
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_forgot_to_login);
            } else {
                Snackbar.make(requireView(), result.error != null ? result.error : "Lỗi gửi email",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}

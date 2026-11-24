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

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tdtu.mobilebanking.databinding.FragmentChangePasswordBinding;

/** A5 — Customer changes their own password (requires re-authentication) */
public class ChangePasswordFragment extends Fragment {

    private FragmentChangePasswordBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnConfirm.setOnClickListener(v -> changePassword(view));
    }

    private void changePassword(View view) {
        String current = binding.etCurrentPassword.getText().toString();
        String newPwd = binding.etNewPassword.getText().toString();
        String confirm = binding.etConfirmPassword.getText().toString();

        if (current.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPwd.equals(confirm)) {
            binding.etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }
        if (!isValidPassword(newPwd)) {
            binding.etNewPassword.setError("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa và số");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Phiên đăng nhập hết hạn", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        // Must re-authenticate before updating password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), current);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> user.updatePassword(newPwd)
                        .addOnSuccessListener(u -> {
                            setLoading(false);
                            Toast.makeText(requireContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(view).navigateUp();
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_LONG).show();
                });
    }

    /** Password must be >= 8 chars, contain at least one uppercase and one digit */
    private boolean isValidPassword(String pwd) {
        if (pwd.length() < 8) return false;
        boolean hasUpper = false, hasDigit = false;
        for (char c : pwd.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasDigit;
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

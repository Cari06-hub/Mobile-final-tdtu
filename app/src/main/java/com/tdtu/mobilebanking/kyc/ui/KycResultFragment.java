package com.tdtu.mobilebanking.kyc.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentKycResultBinding;

/** Shows the final result of a KYC step (CCCD verification or face enrol) */
public class KycResultFragment extends Fragment {

    private FragmentKycResultBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKycResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean success = getArguments() != null && getArguments().getBoolean("success", false);
        String message = getArguments() != null
                ? getArguments().getString("message", "") : "";

        if (success) {
            binding.ivStatus.setImageResource(R.drawable.ic_check_circle);
            binding.ivStatus.setColorFilter(
                    requireContext().getColor(R.color.md_theme_primary));
        } else {
            binding.ivStatus.setImageResource(R.drawable.ic_error);
            binding.ivStatus.setColorFilter(
                    requireContext().getColor(R.color.md_theme_error));
        }
        binding.tvMessage.setText(message);

        binding.btnDone.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack(R.id.nav_root, false));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

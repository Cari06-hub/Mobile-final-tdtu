package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentOfficerHomeBinding;

/**
 * Officer Dashboard — owned by Dev A.
 * Điều hướng sang các chức năng officer.
 */
public class OfficerHomeFragment extends Fragment {
    private FragmentOfficerHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOfficerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
            // Navigate back to login
            Navigation.findNavController(v).navigate(R.id.nav_auth);
        });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}

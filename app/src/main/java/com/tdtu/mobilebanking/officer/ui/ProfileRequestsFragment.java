package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentProfileRequestsBinding;
import com.tdtu.mobilebanking.officer.adapter.ProfileRequestAdapter;

/** A6 — Officer views and acts on pending profile-change requests */
public class ProfileRequestsFragment extends Fragment {

    private FragmentProfileRequestsBinding binding;
    private FirebaseFirestore db;
    private ProfileRequestAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        adapter = new ProfileRequestAdapter(requestId -> {
            Bundle args = new Bundle();
            args.putString("requestId", requestId);
            Navigation.findNavController(view)
                    .navigate(R.id.action_profileRequests_to_profileRequestDetail, args);
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        loadRequests();
    }

    private void loadRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("profileRequests")
                .whereEqualTo("status", "PENDING")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    adapter.submitList(query.getDocuments());
                    binding.tvEmpty.setVisibility(query.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvEmpty.setText("Lỗi tải dữ liệu: " + e.getMessage());
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

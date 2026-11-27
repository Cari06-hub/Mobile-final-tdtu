package com.tdtu.mobilebanking.common.home;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.common.DeepLinkRouter;
import com.tdtu.mobilebanking.common.FirestoreUtils;
import com.tdtu.mobilebanking.common.ResultCallback;
import com.tdtu.mobilebanking.databinding.FragmentHomeBinding;
import com.tdtu.mobilebanking.seed.SeedData;

import java.util.Arrays;
import java.util.List;

/**
 * Trang chủ khách hàng: lời chào + lưới chức năng.
 * Ô nào mà module chưa merge thì hiện thông báo "đang hoàn thiện" thay vì crash.
 */
public class HomeFragment extends Fragment {

    /** Hồ sơ khách (A5) nằm ở graph nav_account của A, chưa có deepLink nên mở bằng id. */
    private static final String ROUTE_PROFILE = "profile";

    private FragmentHomeBinding b;

    private static final List<HomeMenuAdapter.MenuItem> MENU = Arrays.asList(
            new HomeMenuAdapter.MenuItem("💳", R.string.tile_accounts, DeepLinkRouter.ACCOUNTS),
            new HomeMenuAdapter.MenuItem("🔁", R.string.tile_transfer, DeepLinkRouter.TRANSFER),
            new HomeMenuAdapter.MenuItem("🏦", R.string.tile_interbank, DeepLinkRouter.INTERBANK),
            new HomeMenuAdapter.MenuItem("🧾", R.string.tile_history, DeepLinkRouter.HISTORY),
            new HomeMenuAdapter.MenuItem("🐷", R.string.tile_saving, DeepLinkRouter.SAVING),
            new HomeMenuAdapter.MenuItem("📈", R.string.tile_rates, DeepLinkRouter.SAVING_RATES),
            new HomeMenuAdapter.MenuItem("🏠", R.string.tile_mortgage, DeepLinkRouter.MORTGAGE),
            new HomeMenuAdapter.MenuItem("💡", R.string.tile_bills, DeepLinkRouter.PAY_BILLS),
            new HomeMenuAdapter.MenuItem("📱", R.string.tile_phone, DeepLinkRouter.PAY_PHONE),
            new HomeMenuAdapter.MenuItem("✈️", R.string.tile_booking, DeepLinkRouter.BOOKING),
            new HomeMenuAdapter.MenuItem("📍", R.string.tile_branches, DeepLinkRouter.BRANCHES),
            new HomeMenuAdapter.MenuItem("🤖", R.string.tile_chatbot, DeepLinkRouter.CHATBOT),
            new HomeMenuAdapter.MenuItem("👤", R.string.tile_profile, ROUTE_PROFILE)
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.menuGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        b.menuGrid.setAdapter(new HomeMenuAdapter(MENU, this::openRoute));
        loadGreeting();
        setupSeedButton();
    }

    private void openRoute(String route) {
        if (ROUTE_PROFILE.equals(route)) {
            NavHostFragment.findNavController(this).navigate(R.id.nav_account);
            return;
        }
        boolean ok = DeepLinkRouter.navigate(NavHostFragment.findNavController(this), route);
        if (!ok) {
            Toast.makeText(requireContext(), R.string.common_feature_not_ready, Toast.LENGTH_SHORT).show();
        }
    }

    /** Đọc users/{uid}.fullName (schema của A). Không có thì giữ lời chào mặc định. */
    private void loadGreeting() {
        String uid = FirestoreUtils.uid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (b == null || !doc.exists()) return;
                    String name = FirestoreUtils.getString(doc, "fullName", "");
                    if (!name.isEmpty()) b.greeting.setText(getString(R.string.home_greeting, name));
                });
    }

    /** C7 - Nút nạp dữ liệu mẫu, chỉ hiện ở bản debug (APK release không có). */
    private void setupSeedButton() {
        boolean debuggable = (requireContext().getApplicationInfo().flags
                & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!debuggable) return;
        b.seedButton.setVisibility(View.VISIBLE);
        b.seedButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.seed_confirm)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_ok, (d, w) -> runSeed())
                .show());
    }

    private void runSeed() {
        b.seedButton.setEnabled(false);
        SeedData.uploadAll(FirebaseFirestore.getInstance(), new ResultCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (b == null) return;
                b.seedButton.setEnabled(true);
                Toast.makeText(requireContext(), getString(R.string.seed_done, count), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Exception e) {
                if (b == null) return;
                b.seedButton.setEnabled(true);
                Toast.makeText(requireContext(),
                        getString(R.string.common_error_format, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

package com.tdtu.mobilebanking.saving;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.common.ui.ScreenHeader;
import com.tdtu.mobilebanking.databinding.DialogEditRateBinding;
import com.tdtu.mobilebanking.databinding.FragmentRateListBinding;

/**
 * C2 - Officer chỉnh lãi suất theo chính sách ngân hàng (đề bài mục 3).
 * Màn hình thật trong nav_officer.xml của A là officer/ui/RateManagementFragment,
 * lớp đó chỉ kế thừa lớp này. Quyền ghi do Security Rules của A kiểm tra (rates/: chỉ officer được write).
 */
public class OfficerRateFragment extends Fragment {

    private FragmentRateListBinding b;
    private SavingViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentRateListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(SavingViewModel.class);
        ScreenHeader.bind(this, b.header, R.string.saving_officer_rates_title);

        b.note.setVisibility(View.VISIBLE);
        b.note.setText(R.string.saving_rate_note);
        b.fab.setVisibility(View.VISIBLE);
        b.fab.setOnClickListener(v -> showEditDialog(null));

        RateAdapter adapter = new RateAdapter(this::showEditDialog);
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        vm.getRates().observe(getViewLifecycleOwner(), list -> {
            b.progress.setVisibility(View.GONE);
            adapter.submit(list);
        });
        vm.getError().observe(getViewLifecycleOwner(), msg -> {
            b.progress.setVisibility(View.GONE);
            Toast.makeText(requireContext(), getString(R.string.common_error_format, msg),
                    Toast.LENGTH_LONG).show();
        });
        vm.loadRates(false);  // officer thấy cả sản phẩm đã ngừng
    }

    /** existing == null: thêm sản phẩm mới. */
    private void showEditDialog(@Nullable RateProduct existing) {
        DialogEditRateBinding d = DialogEditRateBinding.inflate(getLayoutInflater());
        if (existing != null) {
            d.nameInput.setText(existing.name);
            d.termInput.setText(String.valueOf(existing.termMonths));
            d.termInput.setEnabled(false);   // đổi kỳ hạn = sản phẩm khác, nên khóa
            d.rateInput.setText(String.valueOf(existing.annualRate));
            d.activeSwitch.setChecked(existing.active);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? R.string.saving_add_product : R.string.saving_edit_product)
                .setView(d.getRoot())
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_save, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            RateProduct p = readForm(d, existing);
            if (p == null) return;
            v.setEnabled(false);
            vm.saveRate(p)
                    .addOnSuccessListener(x -> {
                        dialog.dismiss();
                        if (b != null) Toast.makeText(requireContext(), R.string.saving_saved, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        v.setEnabled(true);
                        if (b != null) Toast.makeText(requireContext(),
                                getString(R.string.common_error_format, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }

    /** Đọc và kiểm tra form. Trả null nếu có lỗi (đã hiện lỗi trên ô nhập). */
    @Nullable
    private RateProduct readForm(DialogEditRateBinding d, @Nullable RateProduct existing) {
        d.nameLayout.setError(null);
        d.termLayout.setError(null);
        d.rateLayout.setError(null);

        String name = String.valueOf(d.nameInput.getText()).trim();
        if (name.isEmpty()) {
            d.nameLayout.setError(getString(R.string.saving_invalid_name));
            return null;
        }
        int term;
        try {
            term = Integer.parseInt(String.valueOf(d.termInput.getText()).trim());
        } catch (NumberFormatException e) {
            term = -1;
        }
        if (term < 0 || term > 60) {
            d.termLayout.setError(getString(R.string.saving_invalid_term));
            return null;
        }
        double rate;
        try {
            // Bàn phím tiếng Việt có thể gõ dấu phẩy
            rate = Double.parseDouble(String.valueOf(d.rateInput.getText()).trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            rate = -1;
        }
        if (rate < 0 || rate > 20) {
            d.rateLayout.setError(getString(R.string.saving_invalid_rate));
            return null;
        }

        RateProduct p = new RateProduct();
        p.id = existing == null ? null : existing.id;
        p.minDeposit = existing == null ? RateProduct.DEFAULT_MIN_DEPOSIT : existing.minDeposit;
        p.name = name;
        p.termMonths = term;
        p.annualRate = Math.round(rate * 100) / 100.0;   // giữ 2 chữ số thập phân
        p.active = d.activeSwitch.isChecked();
        return p;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

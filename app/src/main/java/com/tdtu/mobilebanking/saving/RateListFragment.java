package com.tdtu.mobilebanking.saving;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.tdtu.mobilebanking.common.AccountPicker;
import com.tdtu.mobilebanking.common.MoneyFormatter;
import com.tdtu.mobilebanking.common.TxNavigator;
import com.tdtu.mobilebanking.common.ui.ScreenHeader;
import com.tdtu.mobilebanking.databinding.DialogOpenSavingBinding;
import com.tdtu.mobilebanking.databinding.FragmentRateListBinding;
import com.tdtu.mobilebanking.common.model.TxRequest;
import com.tdtu.mobilebanking.common.model.TxType;

/**
 * C2 - Khách xem bảng lãi suất và mở sổ tiết kiệm mới.
 * Mở sổ = giao dịch SAVING_DEPOSIT: chuyển tiền từ tài khoản thanh toán sang sổ mới.
 * Màn này chỉ dựng TxRequest; việc trừ tiền + tạo sổ do pipeline của B (B4) làm.
 */
public class RateListFragment extends Fragment {

    private FragmentRateListBinding b;
    private SavingViewModel vm;
    private RateAdapter adapter;

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
        ScreenHeader.bind(this, b.header, R.string.saving_rates_title);
        adapter = new RateAdapter(this::showOpenDialog);
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
        vm.loadRates(true);   // khách chỉ thấy sản phẩm đang áp dụng
    }

    private void showOpenDialog(RateProduct product) {
        DialogOpenSavingBinding d = DialogOpenSavingBinding.inflate(getLayoutInflater());
        updatePreview(d, product, -1);
        d.amountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int e) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int e) { }
            @Override
            public void afterTextChanged(Editable s) {
                d.amountLayout.setError(null);
                updatePreview(d, product, MoneyFormatter.parse(s.toString()));
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.saving_open_title, product.name))
                .setView(d.getRoot())
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_continue, null)
                .create();
        dialog.show();
        // Gắn listener sau show() để dialog không tự đóng khi dữ liệu sai
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long amount = MoneyFormatter.parse(String.valueOf(d.amountInput.getText()));
            if (amount < product.minDeposit) {
                d.amountLayout.setError(getString(R.string.saving_min_amount,
                        MoneyFormatter.format(product.minDeposit)));
                return;
            }
            dialog.dismiss();
            AccountPicker.pickChecking(requireContext(), source -> openTransaction(product, source, amount));
        });
    }

    private void updatePreview(DialogOpenSavingBinding d, RateProduct p, long amount) {
        long value = Math.max(amount, 0);
        int months = p.termMonths > 0 ? p.termMonths : 12;
        long monthly = SavingCalculator.monthlyProfit(value, p.annualRate);
        long total = SavingCalculator.totalProfit(value, p.annualRate, months);
        d.preview.setText(getString(R.string.saving_preview, MoneyFormatter.format(monthly),
                MoneyFormatter.format(total), MoneyFormatter.format(value + total)));
    }

    private void openTransaction(RateProduct product, AccountPicker.Picked source, long amount) {
        // B dùng meta để tạo sổ mới đúng sản phẩm và chốt lãi suất (lockedRate)
        TxRequest req = new TxRequest.Builder()
                .type(TxType.SAVING_DEPOSIT)
                .from(source.id)
                .amount(amount)
                .meta("productId", product.id)
                .meta("productName", product.name)
                .meta("termMonths", String.valueOf(product.termMonths))
                .meta("lockedRate", String.valueOf(product.annualRate))
                .build();
        TxNavigator.open(this, req);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

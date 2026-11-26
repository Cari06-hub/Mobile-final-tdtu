package com.tdtu.mobilebanking.saving;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.common.MoneyFormatter;
import com.tdtu.mobilebanking.common.ui.AccountCardAdapter;
import com.tdtu.mobilebanking.common.ui.ScreenHeader;
import com.tdtu.mobilebanking.databinding.FragmentAccountListBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** C2 - Danh sách sổ tiết kiệm của khách. */
public class SavingListFragment extends Fragment {

    private FragmentAccountListBinding b;
    private SavingViewModel vm;
    private AccountCardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentAccountListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(SavingViewModel.class);
        ScreenHeader.bind(this, b.header, R.string.saving_list_title);

        b.summaryLabel.setText(R.string.saving_total_label);
        b.summaryValue.setText(MoneyFormatter.format(0));
        b.emptyText.setText(R.string.saving_empty);
        b.actionButton.setVisibility(View.VISIBLE);
        b.actionButton.setText(R.string.saving_open_new);
        b.actionButton.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(R.id.action_savingList_to_rates));

        adapter = new AccountCardAdapter(item -> {
            Bundle args = new Bundle();
            args.putString("accountId", item.id);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_savingList_to_detail, args);
        });
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        vm.getMySavings().observe(getViewLifecycleOwner(), list -> render());
        vm.getRateMap().observe(getViewLifecycleOwner(), map -> render());
        vm.getError().observe(getViewLifecycleOwner(), msg -> {
            b.progress.setVisibility(View.GONE);
            Toast.makeText(requireContext(), getString(R.string.common_error_format, msg),
                    Toast.LENGTH_LONG).show();
        });

        vm.loadRates(false);   // lấy cả sản phẩm đã ngừng, vì sổ cũ có thể thuộc sản phẩm đó
        vm.loadMySavings();
    }

    private void render() {
        List<SavingInfo> list = vm.getMySavings().getValue();
        if (list == null) return;   // chưa tải xong
        Map<String, RateProduct> rates = vm.getRateMap().getValue();
        if (rates == null) rates = new HashMap<>();

        long total = 0;
        List<AccountCardAdapter.CardItem> items = new ArrayList<>();
        for (SavingInfo s : list) {
            if ("CLOSED".equals(s.status)) continue;
            RateProduct p = rates.get(s.productId);
            double rate = s.effectiveRate(p);
            total += s.principal;
            String subtitle = (p != null ? p.name : s.termMonths + " tháng")
                    + " · " + MoneyFormatter.formatRate(rate) + "/năm";
            String note = getString(R.string.saving_item_profit,
                    MoneyFormatter.format(SavingCalculator.monthlyProfit(s.principal, rate)));
            items.add(new AccountCardAdapter.CardItem(s.id,
                    getString(R.string.saving_item_title, s.shortNumber),
                    subtitle, MoneyFormatter.format(s.principal), note));
        }
        b.progress.setVisibility(View.GONE);
        b.summaryValue.setText(MoneyFormatter.format(total));
        b.emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(items);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

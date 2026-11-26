package com.tdtu.mobilebanking.mortgage;

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
import com.tdtu.mobilebanking.common.DateUtils;
import com.tdtu.mobilebanking.common.MoneyFormatter;
import com.tdtu.mobilebanking.common.ui.AccountCardAdapter;
import com.tdtu.mobilebanking.common.ui.ScreenHeader;
import com.tdtu.mobilebanking.databinding.FragmentAccountListBinding;

import java.util.ArrayList;
import java.util.List;

/** C3 - Danh sách khoản vay của khách (dùng chung layout với danh sách tiết kiệm). */
public class MortgageListFragment extends Fragment {

    private FragmentAccountListBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentAccountListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MortgageViewModel vm = new ViewModelProvider(this).get(MortgageViewModel.class);
        ScreenHeader.bind(this, b.header, R.string.mortgage_list_title);
        b.summaryLabel.setText(R.string.mortgage_total_label);
        b.summaryValue.setText(MoneyFormatter.format(0));
        b.emptyText.setText(R.string.mortgage_empty);

        AccountCardAdapter adapter = new AccountCardAdapter(item -> {
            Bundle args = new Bundle();
            args.putString("accountId", item.id);
            NavHostFragment.findNavController(this).navigate(R.id.action_mortgageList_to_detail, args);
        });
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        vm.getMyMortgages().observe(getViewLifecycleOwner(), list -> {
            long total = 0;
            List<AccountCardAdapter.CardItem> items = new ArrayList<>();
            for (MortgageInfo m : list) {
                total += m.outstanding;
                List<MortgageCalculator.Installment> schedule = m.schedule();
                int paid = MortgageCalculator.paidCount(schedule, m.outstanding);
                String freq = getString(m.frequency == MortgageCalculator.Frequency.MONTHLY
                        ? R.string.mortgage_monthly : R.string.mortgage_biweekly);
                String note = paid >= schedule.size()
                        ? getString(R.string.mortgage_item_done)
                        : getString(R.string.mortgage_item_next,
                        DateUtils.formatDay(schedule.get(paid).dueMillis),
                        MoneyFormatter.format(schedule.get(paid).payment));
                items.add(new AccountCardAdapter.CardItem(m.id,
                        getString(R.string.mortgage_item_title, m.shortNumber),
                        getString(R.string.mortgage_item_subtitle, freq, schedule.size(),
                                MoneyFormatter.formatRate(m.annualRate)),
                        MoneyFormatter.format(m.outstanding), note));
            }
            b.progress.setVisibility(View.GONE);
            b.summaryValue.setText(MoneyFormatter.format(total));
            b.emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.submit(items);
        });
        vm.getError().observe(getViewLifecycleOwner(), msg -> {
            b.progress.setVisibility(View.GONE);
            Toast.makeText(requireContext(), getString(R.string.common_error_format, msg),
                    Toast.LENGTH_LONG).show();
        });
        vm.loadMyMortgages();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.common.DateUtils;
import com.tdtu.mobilebanking.common.MoneyFormatter;
import com.tdtu.mobilebanking.common.ui.ScheduleTableAdapter;
import com.tdtu.mobilebanking.common.ui.ScreenHeader;
import com.tdtu.mobilebanking.databinding.FragmentSavingDetailBinding;
import com.tdtu.mobilebanking.databinding.ViewInfoRowBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * C2 - Chi tiết sổ tiết kiệm: lãi suất, lãi mỗi tháng, lãi tạm tính, bảng lãi từng tháng.
 * Tất toán / gửi thêm là giao dịch nên thuộc B4 (đi qua ConfirmTransaction).
 */
public class SavingDetailFragment extends Fragment {

    private static final float[] WEIGHTS = {0.7f, 1.3f, 1.3f, 1.5f};

    private FragmentSavingDetailBinding b;
    private SavingViewModel vm;
    private ScheduleTableAdapter tableAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentSavingDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String accountId = requireArguments().getString("accountId");
        vm = new ViewModelProvider(this).get(SavingViewModel.class);
        ScreenHeader.bind(this, b.header, R.string.saving_detail_title);

        setLabel(b.rowProduct, R.string.saving_product);
        setLabel(b.rowRate, R.string.saving_rate);
        setLabel(b.rowOpened, R.string.saving_opened);
        setLabel(b.rowMaturity, R.string.saving_maturity);
        setLabel(b.rowMonthly, R.string.saving_monthly_profit);
        setLabel(b.rowAccrued, R.string.saving_accrued);
        setLabel(b.rowTotal, R.string.saving_total_profit);
        setLabel(b.rowReceive, R.string.saving_receive);

        ScheduleTableAdapter.bindHeader(b.tableHeader, WEIGHTS,
                getString(R.string.saving_col_month), getString(R.string.saving_col_date),
                getString(R.string.saving_col_profit), getString(R.string.saving_col_cumulative));
        tableAdapter = new ScheduleTableAdapter(WEIGHTS);
        b.table.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.table.setAdapter(tableAdapter);

        vm.getDetail().observe(getViewLifecycleOwner(), s -> render());
        vm.getRateMap().observe(getViewLifecycleOwner(), m -> render());
        vm.getError().observe(getViewLifecycleOwner(), msg -> Toast.makeText(requireContext(),
                getString(R.string.common_error_format, msg), Toast.LENGTH_LONG).show());

        vm.loadRates(false);
        if (accountId != null) vm.loadDetail(accountId);
    }

    private void render() {
        SavingInfo s = vm.getDetail().getValue();
        if (s == null) return;
        Map<String, RateProduct> rates = vm.getRateMap().getValue();
        RateProduct product = rates == null ? null : rates.get(s.productId);
        double rate = s.effectiveRate(product);
        boolean flexible = s.termMonths == 0;

        long monthly = SavingCalculator.monthlyProfit(s.principal, rate);
        long now = System.currentTimeMillis();
        int elapsed = DateUtils.fullMonthsBetween(s.openedAt, now);
        if (!flexible) elapsed = Math.min(elapsed, s.termMonths);
        long accrued = monthly * elapsed;

        b.principal.setText(MoneyFormatter.format(s.principal));
        setValue(b.rowProduct, product != null ? product.name : s.productId);
        setValue(b.rowRate, getString(R.string.saving_rate_per_year, MoneyFormatter.formatRate(rate)));
        setValue(b.rowOpened, DateUtils.formatDay(s.openedAt));
        setValue(b.rowMonthly, MoneyFormatter.format(monthly));
        setValue(b.rowAccrued, MoneyFormatter.format(accrued));

        if (flexible) {
            b.rowMaturity.getRoot().setVisibility(View.GONE);
            b.rowTotal.getRoot().setVisibility(View.GONE);
            b.rowReceive.getRoot().setVisibility(View.GONE);
            b.flexNote.setVisibility(View.VISIBLE);
        } else {
            long total = SavingCalculator.totalProfit(s.principal, rate, s.termMonths);
            setValue(b.rowMaturity, DateUtils.formatDay(
                    SavingCalculator.maturityMillis(s.openedAt, s.termMonths)));
            setValue(b.rowTotal, MoneyFormatter.format(total));
            setValue(b.rowReceive, MoneyFormatter.format(s.principal + total));
        }

        List<ScheduleTableAdapter.Row> rows = new ArrayList<>();
        for (SavingCalculator.ProfitRow r :
                SavingCalculator.schedule(s.principal, rate, s.termMonths, s.openedAt)) {
            int state = r.dateMillis <= now ? ScheduleTableAdapter.STATE_DONE
                    : (r.month == elapsed + 1 ? ScheduleTableAdapter.STATE_NEXT
                    : ScheduleTableAdapter.STATE_NORMAL);
            rows.add(new ScheduleTableAdapter.Row(state, String.valueOf(r.month),
                    DateUtils.formatDay(r.dateMillis),
                    MoneyFormatter.formatPlain(r.profit),
                    MoneyFormatter.formatPlain(r.cumulative)));
        }
        tableAdapter.submit(rows);
    }

    private void setLabel(ViewInfoRowBinding row, int textRes) {
        row.label.setText(textRes);
    }

    private void setValue(ViewInfoRowBinding row, String value) {
        row.value.setText(value);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

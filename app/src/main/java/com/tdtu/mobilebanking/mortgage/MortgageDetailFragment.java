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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.common.AccountPicker;
import com.tdtu.mobilebanking.common.DateUtils;
import com.tdtu.mobilebanking.common.MoneyFormatter;
import com.tdtu.mobilebanking.common.TxNavigator;
import com.tdtu.mobilebanking.common.ui.ScheduleTableAdapter;
import com.tdtu.mobilebanking.common.ui.ScreenHeader;
import com.tdtu.mobilebanking.databinding.FragmentMortgageDetailBinding;
import com.tdtu.mobilebanking.databinding.ViewInfoRowBinding;
import com.tdtu.mobilebanking.common.model.TxRequest;
import com.tdtu.mobilebanking.common.model.TxType;

import java.util.ArrayList;
import java.util.List;

/**
 * C3 - Chi tiết khoản vay: số tiền phải trả mỗi tháng / 2 tuần, lịch trả nợ đầy đủ,
 * và nút thanh toán kỳ tiếp theo (tạo TxRequest MORTGAGE_PAYMENT gửi sang B).
 */
public class MortgageDetailFragment extends Fragment {

    private static final float[] WEIGHTS = {0.6f, 1.3f, 1.3f, 1.2f, 1.6f};

    private FragmentMortgageDetailBinding b;
    private ScheduleTableAdapter tableAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentMortgageDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String accountId = requireArguments().getString("accountId");
        MortgageViewModel vm = new ViewModelProvider(this).get(MortgageViewModel.class);
        ScreenHeader.bind(this, b.header, R.string.mortgage_detail_title);

        label(b.rowPrincipal, R.string.mortgage_principal);
        label(b.rowRate, R.string.mortgage_rate);
        label(b.rowFrequency, R.string.mortgage_frequency);
        label(b.rowInstallment, R.string.mortgage_installment);
        label(b.rowProgress, R.string.mortgage_progress);
        label(b.rowNext, R.string.mortgage_next);
        label(b.rowTotalInterest, R.string.mortgage_total_interest);

        ScheduleTableAdapter.bindHeader(b.tableHeader, WEIGHTS,
                getString(R.string.mortgage_col_index), getString(R.string.mortgage_col_date),
                getString(R.string.mortgage_col_principal), getString(R.string.mortgage_col_interest),
                getString(R.string.mortgage_col_remaining));
        tableAdapter = new ScheduleTableAdapter(WEIGHTS);
        b.table.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.table.setAdapter(tableAdapter);
        b.payButton.setEnabled(false);

        vm.getDetail().observe(getViewLifecycleOwner(), this::render);
        vm.getError().observe(getViewLifecycleOwner(), msg -> Toast.makeText(requireContext(),
                getString(R.string.common_error_format, msg), Toast.LENGTH_LONG).show());
        if (accountId != null) vm.loadDetail(accountId);
    }

    private void render(MortgageInfo m) {
        List<MortgageCalculator.Installment> schedule = m.schedule();
        int total = schedule.size();
        int paid = MortgageCalculator.paidCount(schedule, m.outstanding);
        boolean paidOff = paid >= total;
        String freq = getString(m.frequency == MortgageCalculator.Frequency.MONTHLY
                ? R.string.mortgage_monthly : R.string.mortgage_biweekly);

        b.outstanding.setText(MoneyFormatter.format(m.outstanding));
        b.progressBar.setMax(Math.max(total, 1));
        b.progressBar.setProgress(paid);
        value(b.rowPrincipal, MoneyFormatter.format(m.principal));
        value(b.rowRate, MoneyFormatter.formatRate(m.annualRate) + "/năm");
        value(b.rowFrequency, freq);
        value(b.rowInstallment, MoneyFormatter.format(m.installment()));
        value(b.rowProgress, getString(R.string.mortgage_progress_value, paid, total));
        value(b.rowTotalInterest, MoneyFormatter.format(MortgageCalculator.totalInterest(schedule)));

        // So sánh với hình thức trả còn lại (hàng tháng <-> 2 tuần/lần)
        MortgageCalculator.Frequency other = m.frequency == MortgageCalculator.Frequency.MONTHLY
                ? MortgageCalculator.Frequency.BIWEEKLY : MortgageCalculator.Frequency.MONTHLY;
        b.compare.setText(getString(R.string.mortgage_compare,
                getString(other == MortgageCalculator.Frequency.MONTHLY
                        ? R.string.mortgage_monthly : R.string.mortgage_biweekly),
                MoneyFormatter.format(MortgageCalculator.installment(
                        m.principal, m.annualRate, m.termMonths, other)),
                MortgageCalculator.periods(m.termMonths, other)));

        if (paidOff) {
            value(b.rowNext, "—");
            b.payButton.setText(R.string.mortgage_paid_off);
            b.payButton.setEnabled(false);
        } else {
            MortgageCalculator.Installment next = schedule.get(paid);
            value(b.rowNext, getString(R.string.mortgage_next_value,
                    DateUtils.formatDay(next.dueMillis), MoneyFormatter.format(next.payment)));
            b.payButton.setText(getString(R.string.mortgage_pay_button, next.index,
                    MoneyFormatter.format(next.payment)));
            b.payButton.setEnabled(true);
            b.payButton.setOnClickListener(v -> AccountPicker.pickChecking(requireContext(),
                    source -> pay(m, next, source)));
        }

        List<ScheduleTableAdapter.Row> rows = new ArrayList<>(total);
        for (MortgageCalculator.Installment it : schedule) {
            int state = it.index <= paid ? ScheduleTableAdapter.STATE_DONE
                    : (it.index == paid + 1 ? ScheduleTableAdapter.STATE_NEXT
                    : ScheduleTableAdapter.STATE_NORMAL);
            rows.add(new ScheduleTableAdapter.Row(state, String.valueOf(it.index),
                    DateUtils.formatDay(it.dueMillis),
                    MoneyFormatter.formatPlain(it.principalPart),
                    MoneyFormatter.formatPlain(it.interest),
                    MoneyFormatter.formatPlain(it.remainingAfter)));
        }
        tableAdapter.submit(rows);
        int nextPos = tableAdapter.indexOfNext();
        if (nextPos > 3) b.table.scrollToPosition(nextPos - 2);
    }

    private void pay(MortgageInfo m, MortgageCalculator.Installment next, AccountPicker.Picked source) {
        // B: giảm outstanding đúng bằng principalPart (không trừ phần lãi)
        TxRequest req = new TxRequest.Builder()
                .type(TxType.MORTGAGE_PAYMENT)
                .from(source.id)
                .to(m.id)
                .amount(next.payment)
                .meta("period", String.valueOf(next.index))
                .meta("principalPart", String.valueOf(next.principalPart))
                .meta("interest", String.valueOf(next.interest))
                .meta("dueDate", DateUtils.formatDay(next.dueMillis))
                .build();
        TxNavigator.open(this, req);
    }

    private void label(ViewInfoRowBinding row, int res) {
        row.label.setText(res);
    }

    private void value(ViewInfoRowBinding row, String text) {
        row.value.setText(text);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

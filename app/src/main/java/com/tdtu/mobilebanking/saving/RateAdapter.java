package com.tdtu.mobilebanking.saving;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tdtu.mobilebanking.common.MoneyFormatter;
import com.tdtu.mobilebanking.databinding.ItemRateBinding;

import java.util.ArrayList;
import java.util.List;

/** C2 - Danh sách sản phẩm lãi suất (dùng chung cho màn khách và màn officer). */
public class RateAdapter extends RecyclerView.Adapter<RateAdapter.VH> {

    public interface OnRateClick {
        void onClick(RateProduct product);
    }

    private final List<RateProduct> items = new ArrayList<>();
    private final OnRateClick onClick;

    public RateAdapter(OnRateClick onClick) {
        this.onClick = onClick;
    }

    public void submit(List<RateProduct> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemRateBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RateProduct p = items.get(position);
        h.b.name.setText(p.name);
        h.b.term.setText(p.termText());
        h.b.rate.setText(MoneyFormatter.formatRate(p.annualRate));
        h.b.inactive.setVisibility(p.active ? View.GONE : View.VISIBLE);
        h.itemView.setAlpha(p.active ? 1f : 0.6f);
        h.itemView.setOnClickListener(v -> onClick.onClick(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ItemRateBinding b;

        VH(ItemRateBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}

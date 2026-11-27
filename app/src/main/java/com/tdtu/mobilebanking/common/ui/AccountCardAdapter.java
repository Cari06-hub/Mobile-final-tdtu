package com.tdtu.mobilebanking.common.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tdtu.mobilebanking.databinding.ItemAccountCardBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Component dùng chung: danh sách thẻ tài khoản (tiết kiệm, khoản vay, ...).
 * Màn nào dùng thì tự chuyển dữ liệu của mình sang CardItem.
 */
public class AccountCardAdapter extends RecyclerView.Adapter<AccountCardAdapter.VH> {

    public static final class CardItem {
        public final String id;
        public final String title;
        public final String subtitle;
        public final String amount;
        public final String note;

        public CardItem(String id, String title, String subtitle, String amount, String note) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.amount = amount;
            this.note = note;
        }
    }

    public interface OnItemClick {
        void onClick(CardItem item);
    }

    private final List<CardItem> items = new ArrayList<>();
    private final OnItemClick onClick;

    public AccountCardAdapter(OnItemClick onClick) {
        this.onClick = onClick;
    }

    public void submit(List<CardItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemAccountCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CardItem it = items.get(position);
        h.b.title.setText(it.title);
        h.b.subtitle.setText(it.subtitle);
        h.b.amount.setText(it.amount);
        h.b.note.setText(it.note);
        h.b.note.setVisibility(it.note == null || it.note.isEmpty() ? View.GONE : View.VISIBLE);
        h.itemView.setOnClickListener(v -> onClick.onClick(it));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ItemAccountCardBinding b;

        VH(ItemAccountCardBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}

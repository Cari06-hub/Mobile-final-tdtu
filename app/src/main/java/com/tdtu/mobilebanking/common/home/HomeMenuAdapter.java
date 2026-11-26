package com.tdtu.mobilebanking.common.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.tdtu.mobilebanking.databinding.ItemHomeMenuBinding;

import java.util.List;

/** Lưới chức năng trên trang chủ. Mỗi ô gắn với một route của DeepLinkRouter. */
public class HomeMenuAdapter extends RecyclerView.Adapter<HomeMenuAdapter.VH> {

    public static final class MenuItem {
        final String icon;
        @StringRes final int label;
        final String route;

        public MenuItem(String icon, @StringRes int label, String route) {
            this.icon = icon;
            this.label = label;
            this.route = route;
        }
    }

    public interface OnMenuClick {
        void onClick(String route);
    }

    private final List<MenuItem> items;
    private final OnMenuClick onClick;

    public HomeMenuAdapter(List<MenuItem> items, OnMenuClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemHomeMenuBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MenuItem it = items.get(position);
        h.b.icon.setText(it.icon);
        h.b.label.setText(it.label);
        h.itemView.setOnClickListener(v -> onClick.onClick(it.route));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ItemHomeMenuBinding b;

        VH(ItemHomeMenuBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}

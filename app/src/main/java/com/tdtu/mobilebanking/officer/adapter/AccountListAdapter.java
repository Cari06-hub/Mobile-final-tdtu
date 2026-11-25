package com.tdtu.mobilebanking.officer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.tdtu.mobilebanking.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter danh sách tài khoản trong CustomerDetailFragment.
 * Click vào row → callback để officer khóa / mở.
 */
public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.ViewHolder> {

    public static class AccountRow {
        public String id;
        public String type;
        public String status;
        public long balance;
        public long dailyLimit;
    }

    public interface OnAccountClickListener {
        void onClick(AccountRow account);
    }

    private List<AccountRow> data = new ArrayList<>();
    private final OnAccountClickListener listener;

    public AccountListAdapter(OnAccountClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<AccountRow> rows) {
        data = rows;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AccountRow row = data.get(position);
        h.tvType.setText(mapType(row.type));
        h.tvBalance.setText(formatMoney(row.balance));
        h.tvStatus.setText(row.status);

        boolean locked = "LOCKED".equals(row.status);
        int statusColor = locked
                ? h.itemView.getContext().getColor(R.color.md_theme_error)
                : h.itemView.getContext().getColor(R.color.md_theme_primary);
        // Click toàn bộ card & đổi màu nền badge theo status
        h.tvStatus.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(statusColor));
        h.tvStatus.setTextColor(android.graphics.Color.WHITE);
        h.card.setOnClickListener(v -> listener.onClick(row));
    }

    @Override
    public int getItemCount() { return data.size(); }

    private String mapType(String type) {
        switch (type == null ? "" : type) {
            case "CHECKING":  return "Thanh toán";
            case "SAVING":    return "Tiết kiệm";
            case "MORTGAGE":  return "Vay thế chấp";
            default:          return type;
        }
    }

    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " ₫";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvType, tvBalance, tvStatus;

        ViewHolder(View v) {
            super(v);
            card      = v.findViewById(R.id.cardAccount);
            tvType    = v.findViewById(R.id.tvAccountType);
            tvBalance = v.findViewById(R.id.tvAccountBalance);
            tvStatus  = v.findViewById(R.id.tvAccountStatus);
        }
    }
}

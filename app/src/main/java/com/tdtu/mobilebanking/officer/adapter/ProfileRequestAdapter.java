package com.tdtu.mobilebanking.officer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.tdtu.mobilebanking.databinding.ItemProfileRequestBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Adapter for the list of pending profile-change requests (officer view) */
public class ProfileRequestAdapter extends RecyclerView.Adapter<ProfileRequestAdapter.VH> {

    public interface OnClickListener {
        void onClick(String requestId);
    }

    private final OnClickListener listener;
    private List<DocumentSnapshot> items = new ArrayList<>();
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public ProfileRequestAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<DocumentSnapshot> docs) {
        items = docs != null ? docs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfileRequestBinding b = ItemProfileRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemProfileRequestBinding b;
        VH(ItemProfileRequestBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(DocumentSnapshot doc) {
            // Try to get customer name from requestedChanges or uid
            @SuppressWarnings("unchecked")
            Map<String, String> changes = (Map<String, String>) doc.get("requestedChanges");
            String uid = doc.getString("uid");
            b.tvCustomerName.setText(uid != null ? "Khách: " + uid.substring(0, 8) + "..." : "Khách hàng");
            b.tvRequestType.setText("Yêu cầu sửa hồ sơ");

            Date createdAt = doc.getDate("createdAt");
            b.tvTime.setText(createdAt != null ? SDF.format(createdAt) : "—");

            b.getRoot().setOnClickListener(v -> listener.onClick(doc.getId()));
        }
    }
}

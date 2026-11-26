package com.tdtu.mobilebanking.common.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tdtu.mobilebanking.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Component dùng chung: bảng nhiều cột (lịch lãi tiết kiệm, lịch trả nợ mortgage).
 * Số cột và tỉ lệ độ rộng do màn gọi truyền vào, nên không cần file layout cho từng bảng.
 */
public class ScheduleTableAdapter extends RecyclerView.Adapter<ScheduleTableAdapter.VH> {

    public static final int STATE_NORMAL = 0;
    public static final int STATE_DONE = 1;   // đã qua / đã trả: chữ xám
    public static final int STATE_NEXT = 2;   // kỳ tiếp theo: nền vàng nhạt, chữ đậm

    public static final class Row {
        public final String[] cells;
        public final int state;

        public Row(int state, String... cells) {
            this.cells = cells;
            this.state = state;
        }
    }

    private final float[] weights;
    private final List<Row> rows = new ArrayList<>();

    public ScheduleTableAdapter(float... weights) {
        this.weights = weights;
    }

    public void submit(List<Row> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        notifyDataSetChanged();
    }

    /** Vẽ hàng tiêu đề vào một LinearLayout ngang có sẵn trong layout của màn. */
    public static void bindHeader(LinearLayout header, float[] weights, String... titles) {
        header.removeAllViews();
        for (int i = 0; i < titles.length; i++) {
            TextView tv = makeCell(header.getContext(), weights[i], i == 0);
            tv.setText(titles[i]);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setTextColor(ContextCompat.getColor(header.getContext(), R.color.text_secondary));
            header.addView(tv);
        }
    }

    private static TextView makeCell(Context ctx, float weight, boolean first) {
        TextView tv = new TextView(ctx);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setGravity(first ? Gravity.START : Gravity.END);
        return tv;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int pad = ctx.getResources().getDimensionPixelSize(R.dimen.space_s);
        row.setPadding(pad, pad, pad, pad);
        row.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView[] cells = new TextView[weights.length];
        for (int i = 0; i < weights.length; i++) {
            cells[i] = makeCell(ctx, weights[i], i == 0);
            row.addView(cells[i]);
        }
        return new VH(row, cells);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Row r = rows.get(position);
        Context ctx = h.itemView.getContext();
        int color = ContextCompat.getColor(ctx,
                r.state == STATE_DONE ? R.color.row_done : R.color.text_primary);
        h.itemView.setBackgroundColor(r.state == STATE_NEXT
                ? ContextCompat.getColor(ctx, R.color.row_next_bg) : 0);
        for (int i = 0; i < h.cells.length; i++) {
            h.cells[i].setText(i < r.cells.length ? r.cells[i] : "");
            h.cells[i].setTextColor(color);
            h.cells[i].setTypeface(r.state == STATE_NEXT ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class VH extends RecyclerView.ViewHolder {
        final TextView[] cells;

        VH(LinearLayout row, TextView[] cells) {
            super(row);
            this.cells = cells;
        }
    }

    /** Tìm vị trí hàng STATE_NEXT để cuộn tới. Trả -1 nếu không có. */
    public int indexOfNext() {
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).state == STATE_NEXT) return i;
        return -1;
    }
}

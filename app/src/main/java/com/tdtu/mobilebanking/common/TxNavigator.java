package com.tdtu.mobilebanking.common;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.common.model.TxRequest;

/**
 * Cửa duy nhất để mở ConfirmTransactionFragment của B (hợp đồng mục 2: "Một cửa giao dịch").
 * Các màn của C (tiết kiệm, mortgage, hóa đơn, nạp ĐT, checkout, liên ngân hàng) chỉ tạo
 * TxRequest rồi gọi TxNavigator.open(...). Không màn nào của C tự ghi số dư.
 *
 * Thỏa thuận với B: graph id = nav_transaction, start destination đọc argument "txRequest"
 * (Serializable common.model.TxRequest). Nếu B đổi tên thì chỉ sửa file này.
 *
 * Id graph được tra lúc chạy (getIdentifier) nên khi B chưa merge, app vẫn build được;
 * bấm nút giao dịch chỉ hiện thông báo thay vì crash.
 */
public final class TxNavigator {

    public static final String ARG_TX_REQUEST = "txRequest";
    private static final String GRAPH_NAME = "nav_transaction";

    private TxNavigator() {
    }

    public static void open(@NonNull Fragment from, @NonNull TxRequest request) {
        int graphId = from.getResources().getIdentifier(
                GRAPH_NAME, "id", from.requireContext().getPackageName());
        if (graphId == 0) {
            notReady(from);
            return;
        }
        Bundle args = new Bundle();
        args.putSerializable(ARG_TX_REQUEST, request);
        try {
            NavHostFragment.findNavController(from).navigate(graphId, args);
        } catch (IllegalArgumentException | IllegalStateException e) {
            notReady(from);   // có id nhưng nav_root chưa include graph của B
        }
    }

    private static void notReady(Fragment from) {
        Toast.makeText(from.requireContext(), R.string.common_tx_not_ready, Toast.LENGTH_LONG).show();
    }
}

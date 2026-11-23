package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Stub placeholder — Dev A implements D2–D3.
 * Officer tạo khách hàng mới bằng FirebaseApp phụ (xem kế hoạch phụ lục).
 */
public class CreateCustomerFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        // TODO D2: Implement form tạo khách hàng + FirebaseApp phụ
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText("CreateCustomerFragment — TODO D2");
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}

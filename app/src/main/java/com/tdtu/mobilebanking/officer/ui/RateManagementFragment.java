package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** Stub — Dev C implements (officer chỉnh lãi suất rates/) */
public class RateManagementFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText("RateManagementFragment — TODO Dev C (C2)");
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}

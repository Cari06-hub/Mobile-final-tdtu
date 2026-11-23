package com.tdtu.mobilebanking.kyc.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** Stub — Dev A implements D3 (A7: ML Kit Text Recognition đọc CCCD) */
public class KycCccdFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText("KycCccdFragment (Chụp CCCD) — TODO D3");
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}

package com.tdtu.mobilebanking.kyc.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** Hiển thị kết quả eKYC (VERIFIED / REJECTED) */
public class KycResultFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText("KycResultFragment — TODO D3/D4");
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}

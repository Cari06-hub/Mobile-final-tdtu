package com.tdtu.mobilebanking.kyc.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** Stub — Dev A implements D4 (A4: So khớp embedding + KycVerifier giao dịch lớn) */
public class KycFaceVerifyFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText("KycFaceVerifyFragment (Xác thực giao dịch) — TODO D4");
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}

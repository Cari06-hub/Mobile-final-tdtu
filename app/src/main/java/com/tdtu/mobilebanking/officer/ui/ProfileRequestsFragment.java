package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** Stub — Dev A implements D3 (A6: officer duyệt profileRequests) */
public class ProfileRequestsFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText("ProfileRequestsFragment — TODO D3");
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}

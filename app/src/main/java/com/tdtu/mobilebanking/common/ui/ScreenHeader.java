package com.tdtu.mobilebanking.common.ui;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.tdtu.mobilebanking.databinding.ViewScreenHeaderBinding;

/** Gắn tiêu đề và nút Quay lại cho layout view_screen_header.xml. */
public final class ScreenHeader {

    private ScreenHeader() {
    }

    public static void bind(@NonNull Fragment fragment, @NonNull ViewScreenHeaderBinding header,
                            @StringRes int title) {
        header.title.setText(title);
        header.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(fragment).navigateUp());
    }
}

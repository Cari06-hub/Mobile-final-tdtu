package com.tdtu.mobilebanking.officer.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tdtu.mobilebanking.R;
import com.tdtu.mobilebanking.databinding.FragmentCustomerDetailBinding;
import com.tdtu.mobilebanking.officer.adapter.AccountListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A3 — Officer xem và sửa chi tiết khách hàng:
 * - Thông tin cá nhân (users/{uid}), trạng thái KYC
 * - Sửa thông tin khách hàng trực tiếp (theo rule isOfficer)
 * - Danh sách tài khoản (accounts where ownerId == uid)
 * - Khóa / mở từng tài khoản, sửa hạn mức ngày (CHECKING)
 * - Navigate sang OpenAccountFragment để mở thêm TK
 */
public class CustomerDetailFragment extends Fragment {

    private FragmentCustomerDetailBinding binding;
    private FirebaseFirestore db;
    private String customerId;
    private AccountListAdapter accountAdapter;

    private String curFullName = "";
    private String curPhone = "";
    private String curEmail = "";
    private String curCccd = "";
    private String curDob = "";
    private String curAddress = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            customerId = getArguments().getString("customerId");
        }
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(requireContext(), "Không tìm thấy khách hàng", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        setupRecyclerView();
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnEditCustomer.setOnClickListener(v -> showEditCustomerDialog());
        binding.btnOpenAccount.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("customerId", customerId);
            Navigation.findNavController(v).navigate(R.id.action_customer_detail_to_open_account, args);
        });

        loadCustomerData();
        loadAccounts();
    }

    private void setupRecyclerView() {
        accountAdapter = new AccountListAdapter(this::showAccountActionDialog);
        binding.rvAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAccounts.setAdapter(accountAdapter);
    }

    private void loadCustomerData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(customerId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    if (!doc.exists()) {
                        Snackbar.make(requireView(), "Không tìm thấy người dùng", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    curFullName = safeStr(doc.getString("fullName"));
                    curEmail    = safeStr(doc.getString("email"));
                    curPhone    = safeStr(doc.getString("phone"));
                    curCccd     = safeStr(doc.getString("cccd"));
                    curDob      = safeStr(doc.getString("dob"));
                    curAddress  = safeStr(doc.getString("address"));
                    String kycStatus = safeStr(doc.getString("kycStatus"));

                    binding.tvName.setText(curFullName);
                    binding.tvEmail.setText(curEmail);
                    binding.tvPhone.setText(curPhone);
                    binding.tvCccd.setText("CCCD: " + curCccd);
                    binding.tvDob.setText("Ngày sinh: " + curDob);
                    binding.tvAddress.setText("Địa chỉ: " + curAddress);
                    binding.tvKycStatus.setText("KYC: " + kycStatus);
                    binding.tvUid.setText("UID: " + customerId);

                    int color = "VERIFIED".equals(kycStatus)
                            ? requireContext().getColor(R.color.md_theme_primary)
                            : requireContext().getColor(R.color.md_theme_error);
                    binding.tvKycStatus.setTextColor(color);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Snackbar.make(requireView(), "Lỗi: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void showEditCustomerDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        EditText etName = addEditField(layout, "Họ và tên", curFullName, InputType.TYPE_CLASS_TEXT);
        EditText etPhone = addEditField(layout, "Số điện thoại", curPhone, InputType.TYPE_CLASS_PHONE);
        EditText etCccd = addEditField(layout, "Số CCCD (12 số)", curCccd, InputType.TYPE_CLASS_NUMBER);
        EditText etDob = addEditField(layout, "Ngày sinh (dd/MM/yyyy)", curDob, InputType.TYPE_CLASS_TEXT);
        EditText etAddress = addEditField(layout, "Địa chỉ", curAddress, InputType.TYPE_CLASS_TEXT);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sửa thông tin khách hàng")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String cccd = etCccd.getText().toString().trim();
                    String dob = etDob.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    if (name.isEmpty() || phone.isEmpty() || !cccd.matches("\\d{12}")) {
                        Snackbar.make(requireView(), "Họ tên, SĐT không trống và CCCD phải đủ 12 số",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fullName", name);
                    updates.put("phone", phone);
                    updates.put("cccd", cccd);
                    updates.put("dob", dob);
                    updates.put("address", address);
                    db.collection("users").document(customerId).update(updates)
                            .addOnSuccessListener(v -> {
                                if (!isAdded()) return;
                                Snackbar.make(requireView(), "Đã cập nhật thông tin khách hàng",
                                        Snackbar.LENGTH_SHORT).show();
                                loadCustomerData();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                Snackbar.make(requireView(), "Lỗi cập nhật: " + e.getMessage(),
                                        Snackbar.LENGTH_LONG).show();
                            });
                })
                .show();
    }

    private EditText addEditField(LinearLayout parent, String hint, String value, int inputType) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setText(value);
        et.setInputType(inputType);
        parent.addView(et);
        return et;
    }

    private void loadAccounts() {
        db.collection("accounts")
                .whereEqualTo("ownerId", customerId)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded() || binding == null) return;
                    List<AccountListAdapter.AccountRow> rows = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        AccountListAdapter.AccountRow row = new AccountListAdapter.AccountRow();
                        row.id = doc.getId();
                        row.type = safeStr(doc.getString("type"));
                        row.status = safeStr(doc.getString("status"));
                        Long bal = doc.getLong("balance");
                        row.balance = bal != null ? bal : 0L;
                        Long dl = doc.getLong("dailyLimit");
                        row.dailyLimit = dl != null ? dl : 0L;
                        rows.add(row);
                    }
                    accountAdapter.setData(rows);
                    binding.tvNoAccounts.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(), "Lỗi tải tài khoản: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void showAccountActionDialog(AccountListAdapter.AccountRow account) {
        boolean isActive = "ACTIVE".equals(account.status);
        String toggleAction = isActive ? "Khóa tài khoản" : "Mở tài khoản";
        String newStatus = isActive ? "LOCKED" : "ACTIVE";

        if ("CHECKING".equals(account.type)) {
            String[] items = {toggleAction, "Đổi hạn mức ngày (hiện tại: " + account.dailyLimit + " đ)"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Thao tác tài khoản CHECKING")
                    .setItems(items, (d, which) -> {
                        if (which == 0) {
                            updateAccountStatus(account.id, newStatus);
                        } else {
                            showDailyLimitDialog(account);
                        }
                    })
                    .setNegativeButton("Đóng", null)
                    .show();
        } else {
            String msg = isActive
                    ? "Khóa tài khoản " + account.type + "? Khách sẽ không giao dịch được."
                    : "Mở lại tài khoản " + account.type + " cho khách?";
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(toggleAction)
                    .setMessage(msg)
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton(toggleAction, (dialog, which) -> updateAccountStatus(account.id, newStatus))
                    .show();
        }
    }

    private void showDailyLimitDialog(AccountListAdapter.AccountRow account) {
        EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(account.dailyLimit));
        et.setHint("Hạn mức mới (VND)");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cập nhật hạn mức ngày")
                .setView(et)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    String val = et.getText().toString().trim();
                    try {
                        long newLimit = Long.parseLong(val);
                        if (newLimit <= 0) throw new NumberFormatException();
                        db.collection("accounts").document(account.id)
                                .update("dailyLimit", newLimit)
                                .addOnSuccessListener(v -> {
                                    if (!isAdded()) return;
                                    Snackbar.make(requireView(), "Đã cập nhật hạn mức ngày", Snackbar.LENGTH_SHORT).show();
                                    loadAccounts();
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    Snackbar.make(requireView(), "Lỗi: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                });
                    } catch (NumberFormatException ex) {
                        Snackbar.make(requireView(), "Hạn mức phải là số dương", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void updateAccountStatus(String accountId, String newStatus) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", newStatus);
        db.collection("accounts").document(accountId)
                .update(update)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    String msg = "LOCKED".equals(newStatus) ? "Đã khóa tài khoản" : "Đã mở tài khoản";
                    Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show();
                    loadAccounts();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(), "Lỗi: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

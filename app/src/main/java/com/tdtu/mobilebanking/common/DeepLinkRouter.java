package com.tdtu.mobilebanking.common;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDeepLinkRequest;

/**
 * C1 - Bộ định tuyến dùng chung cho Home, Chatbot (C6) và mọi chỗ cần nhảy sang module khác.
 *
 * Mỗi route ứng với một URI "bankapp://navigate/<route>" (cùng scheme với intent-filter A đặt
 * trong AndroidManifest). Chủ màn hình chỉ cần khai báo
 * <deepLink app:uri="bankapp://navigate/<route>"/> trong nav graph của mình là màn đó tự nhận.
 *
 * Bảng route (hợp đồng kỹ thuật mục 2):
 *   pay_phone                              -> C4 nạp điện thoại
 *   pay_bills                              -> C4 hóa đơn điện/nước
 *   history?month=YYYY-MM&accountId=...    -> B2 lịch sử giao dịch
 *       B khai báo: bankapp://navigate/history?month={month}&amp;accountId={accountId}
 *       (2 argument kiểu string, nullable="true", defaultValue="@null")
 *   transfer                               -> B chuyển tiền nội bộ
 *   branches                               -> C5 bản đồ chi nhánh
 * Route bổ sung:
 *   accounts, interbank, saving, saving/rates, mortgage, booking, chatbot, home
 */
public final class DeepLinkRouter {

    public static final String BASE = "bankapp://navigate/";

    // 5 route chatbot dùng (hợp đồng mục 2)
    public static final String PAY_PHONE = "pay_phone";
    public static final String PAY_BILLS = "pay_bills";
    public static final String HISTORY = "history";
    public static final String TRANSFER = "transfer";
    public static final String BRANCHES = "branches";

    // Route bổ sung
    public static final String HOME = "home";
    public static final String ACCOUNTS = "accounts";
    public static final String INTERBANK = "interbank";
    public static final String SAVING = "saving";
    public static final String SAVING_RATES = "saving/rates";
    public static final String MORTGAGE = "mortgage";
    public static final String BOOKING = "booking";
    public static final String CHATBOT = "chatbot";

    private DeepLinkRouter() {
    }

    /**
     * Điều hướng tới route.
     *
     * @return false nếu chưa có màn hình nào khai báo deepLink này (module chưa merge).
     */
    public static boolean navigate(@NonNull NavController nav, @Nullable String route) {
        if (route == null || route.trim().isEmpty()) return false;
        Uri uri = Uri.parse(BASE + route.trim());
        NavDeepLinkRequest request = NavDeepLinkRequest.Builder.fromUri(uri).build();
        try {
            nav.navigate(request);
            return true;
        } catch (IllegalArgumentException notFound) {
            return false;
        }
    }

    /** Tạo route lịch sử, ví dụ history("2026-11", "acc123") -> "history?month=2026-11&accountId=acc123". */
    public static String history(@Nullable String month, @Nullable String accountId) {
        StringBuilder sb = new StringBuilder(HISTORY);
        char sep = '?';
        if (month != null && !month.isEmpty()) {
            sb.append(sep).append("month=").append(Uri.encode(month));
            sep = '&';
        }
        if (accountId != null && !accountId.isEmpty()) {
            sb.append(sep).append("accountId=").append(Uri.encode(accountId));
        }
        return sb.toString();
    }
}

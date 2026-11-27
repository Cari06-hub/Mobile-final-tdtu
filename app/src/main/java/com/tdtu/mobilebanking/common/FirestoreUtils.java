package com.tdtu.mobilebanking.common;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Đọc field từ DocumentSnapshot an toàn (field thiếu hoặc sai kiểu không làm app crash).
 * Dùng chung cho mọi module của C.
 */
public final class FirestoreUtils {

    private FirestoreUtils() {
    }

    @Nullable
    public static String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    public static long getLong(DocumentSnapshot d, String field) {
        Object o = d.get(field);
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }

    public static int getInt(DocumentSnapshot d, String field) {
        return (int) getLong(d, field);
    }

    public static double getDouble(DocumentSnapshot d, String field) {
        Object o = d.get(field);
        return o instanceof Number ? ((Number) o).doubleValue() : 0d;
    }

    @Nullable
    public static Double getDoubleOrNull(DocumentSnapshot d, String field) {
        Object o = d.get(field);
        return o instanceof Number ? ((Number) o).doubleValue() : null;
    }

    public static String getString(DocumentSnapshot d, String field, String fallback) {
        Object o = d.get(field);
        return o instanceof String ? (String) o : fallback;
    }

    public static boolean getBool(DocumentSnapshot d, String field, boolean fallback) {
        Object o = d.get(field);
        return o instanceof Boolean ? (Boolean) o : fallback;
    }

    /**
     * Chấp nhận Timestamp, Date, số millis, hoặc chuỗi ngày "dd/MM/yyyy" / "yyyy-MM-dd"
     * (màn mở khoản vay của A lưu startDate dạng chuỗi DD/MM/YYYY). Trả 0 nếu không đọc được.
     */
    public static long getMillis(DocumentSnapshot d, String field) {
        Object o = d.get(field);
        if (o instanceof Timestamp) return ((Timestamp) o).toDate().getTime();
        if (o instanceof Date) return ((Date) o).getTime();
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) return parseDay((String) o);
        return 0L;
    }

    public static long parseDay(String s) {
        String v = s.trim();
        String[] patterns = {"dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy"};
        for (String p : patterns) {
            SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
            f.setLenient(false);
            try {
                Date date = f.parse(v);
                if (date != null) return date.getTime();
            } catch (ParseException ignored) {
                // thử mẫu tiếp theo
            }
        }
        return 0L;
    }

    /** 4 ký tự cuối để hiển thị "•••1234". Ưu tiên field accountNumber nếu A có lưu. */
    public static String shortNumber(DocumentSnapshot d) {
        String number = getString(d, "accountNumber", d.getId());
        return number.length() <= 4 ? number : number.substring(number.length() - 4);
    }
}

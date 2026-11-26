package com.tdtu.mobilebanking.seed;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.tdtu.mobilebanking.common.ResultCallback;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * C7 - Dữ liệu mẫu cho demo. Dùng ID cố định nên chạy lại nhiều lần chỉ ghi đè, không nhân bản.
 *
 * Collection được tạo (A thêm Rules: read khi signedIn(), write khi isOfficer()):
 *   rates/{productId}            3 sản phẩm bổ sung (không kỳ hạn, 1 tháng, 24 tháng);
 *                                saving_3m/6m/12m do seed/seed.js của A tạo, ở đây không ghi đè
 *   banks/{code}                 10 ngân hàng (C8 liên ngân hàng)
 *   externalAccounts/{bank_no}   tài khoản mock ở ngân hàng khác, để tra tên người nhận (C8)
 *   bills/{id}                   hóa đơn điện/nước theo mã khách hàng (C4)
 *   flights, movies, hotels, products   cho màn checkout chung (C4)
 *   branches/{id}                ≥ 8 chi nhánh có tọa độ (C5)
 *
 * Chạy bằng tài khoản officer (Rules chỉ cho officer ghi), qua nút debug ở trang chủ.
 * ĐỔI DEMO_LAT/DEMO_LNG thành tọa độ nơi quay video để chi nhánh nằm quanh đó.
 */
public final class SeedData {

    // Mặc định: khu vực ĐH Tôn Đức Thắng, Q.7, TP.HCM
    public static final double DEMO_LAT = 10.7326;
    public static final double DEMO_LNG = 106.6992;

    private SeedData() {
    }

    public static void uploadAll(FirebaseFirestore db, ResultCallback<Integer> cb) {
        WriteBatch batch = db.batch();
        int[] count = {0};
        Writer w = (col, id, data) -> {
            batch.set(db.collection(col).document(id), data);
            count[0]++;
        };
        seedRates(w);
        seedBanks(w);
        seedExternalAccounts(w);
        seedBills(w);
        seedFlights(w);
        seedMovies(w);
        seedHotels(w);
        seedProducts(w);
        seedBranches(w);
        // Một batch tối đa 500 thao tác; ở đây khoảng 90 nên một batch là đủ
        batch.commit()
                .addOnSuccessListener(x -> cb.onSuccess(count[0]))
                .addOnFailureListener(cb::onError);
    }

    private interface Writer {
        void put(String collection, String id, Map<String, Object> data);
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** Thời điểm cách hiện tại `days` ngày, lúc hh:mm. */
    private static Timestamp at(int days, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, days);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return new Timestamp(new Date(c.getTimeInMillis()));
    }

    private static String currentPeriod(int monthOffset) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, monthOffset);
        return String.format(java.util.Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    // ---------------------------------------------------------------- C2
    private static void seedRates(Writer w) {
        // Cùng format với seed/seed.js của A: productName, termMonths, annualRate, minDeposit, status
        Object[][] rows = {
                {"saving_flex", "Tiết kiệm không kỳ hạn", 0, 0.5},
                {"saving_1m", "Tiết kiệm 1 tháng", 1, 4.5},
                {"saving_24m", "Tiết kiệm 24 tháng", 24, 7.8},
        };
        for (Object[] r : rows) {
            w.put("rates", (String) r[0], map("productName", r[1], "termMonths", r[2],
                    "annualRate", r[3], "minDeposit", 1_000_000L, "status", "ACTIVE",
                    "updatedAt", Timestamp.now(), "updatedBy", "seed"));
        }
    }

    // ---------------------------------------------------------------- C8
    private static void seedBanks(Writer w) {
        String[][] banks = {
                {"VCB", "Vietcombank", "Ngân hàng TMCP Ngoại thương Việt Nam"},
                {"BIDV", "BIDV", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam"},
                {"CTG", "VietinBank", "Ngân hàng TMCP Công thương Việt Nam"},
                {"AGR", "Agribank", "Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam"},
                {"TCB", "Techcombank", "Ngân hàng TMCP Kỹ thương Việt Nam"},
                {"MB", "MB Bank", "Ngân hàng TMCP Quân đội"},
                {"ACB", "ACB", "Ngân hàng TMCP Á Châu"},
                {"VPB", "VPBank", "Ngân hàng TMCP Việt Nam Thịnh Vượng"},
                {"TPB", "TPBank", "Ngân hàng TMCP Tiên Phong"},
                {"OCB", "OCB", "Ngân hàng TMCP Phương Đông"},
        };
        int order = 0;
        for (String[] b : banks) {
            w.put("banks", b[0], map("code", b[0], "shortName", b[1], "name", b[2], "order", order++));
        }
    }

    private static void seedExternalAccounts(Writer w) {
        String[][] accs = {
                {"VCB", "0071000123456", "NGUYEN VAN AN"},
                {"VCB", "0071000654321", "TRAN THI BICH"},
                {"BIDV", "31410001234567", "LE HOANG NAM"},
                {"CTG", "101867543210", "PHAM MINH CHAU"},
                {"AGR", "1600205123456", "VO THI DUNG"},
                {"TCB", "19033456789012", "HOANG GIA BAO"},
                {"MB", "0901234567", "DANG THU HA"},
                {"ACB", "12345678", "BUI QUOC KHANH"},
                {"VPB", "234567890", "NGO THANH LAM"},
                {"OCB", "0041100012345", "DO THI MAI"},
        };
        for (String[] a : accs) {
            w.put("externalAccounts", a[0] + "_" + a[1],
                    map("bankCode", a[0], "accountNo", a[1], "holderName", a[2]));
        }
    }

    // ---------------------------------------------------------------- C4
    private static void seedBills(Writer w) {
        // Mã khách hàng dùng để demo: điện PE0100001..3, nước SW0200001..2
        Object[][] bills = {
                {"PE0100001", "ELECTRIC", "EVN HCMC", 0, 685_400L},
                {"PE0100001", "ELECTRIC", "EVN HCMC", -1, 712_900L},
                {"PE0100002", "ELECTRIC", "EVN HCMC", 0, 1_245_000L},
                {"PE0100003", "ELECTRIC", "EVN HCMC", 0, 398_700L},
                {"SW0200001", "WATER", "SAWACO", 0, 156_000L},
                {"SW0200001", "WATER", "SAWACO", -1, 142_500L},
                {"SW0200002", "WATER", "SAWACO", 0, 231_800L},
        };
        for (Object[] b : bills) {
            String period = currentPeriod((Integer) b[3]);
            String id = b[0] + "_" + period;
            w.put("bills", id, map("customerCode", b[0], "type", b[1], "provider", b[2],
                    "period", period, "amount", b[4], "status", "UNPAID",
                    "dueDate", at(10 + (Integer) b[3] * 30, 23, 59)));
        }
    }

    private static void seedFlights(Writer w) {
        Object[][] f = {
                {"FL01", "Vietnam Airlines", "VN213", "SGN", "HAN", 1, 6, 0, 1_890_000L},
                {"FL02", "Vietjet Air", "VJ121", "SGN", "HAN", 1, 9, 30, 1_290_000L},
                {"FL03", "Bamboo Airways", "QH203", "SGN", "HAN", 2, 14, 15, 1_550_000L},
                {"FL04", "Vietnam Airlines", "VN125", "SGN", "DAD", 1, 7, 45, 1_350_000L},
                {"FL05", "Vietjet Air", "VJ623", "SGN", "DAD", 3, 18, 0, 890_000L},
                {"FL06", "Vietnam Airlines", "VN1825", "SGN", "PQC", 2, 10, 20, 1_150_000L},
                {"FL07", "Vietjet Air", "VJ321", "SGN", "CXR", 4, 12, 40, 790_000L},
                {"FL08", "Bamboo Airways", "QH1121", "HAN", "SGN", 5, 16, 30, 1_490_000L},
        };
        for (Object[] r : f) {
            w.put("flights", (String) r[0], map("airline", r[1], "flightNo", r[2], "from", r[3],
                    "to", r[4], "departAt", at((Integer) r[5], (Integer) r[6], (Integer) r[7]),
                    "price", r[8], "seatsLeft", 20));
        }
    }

    private static void seedMovies(Writer w) {
        // Tên phim tự đặt cho demo
        Object[][] m = {
                {"MV01", "Mùa Hè Ở Hội An", "Rạp Sao Mai Q.7", 0, 18, 30, 95_000L},
                {"MV02", "Mùa Hè Ở Hội An", "Rạp Sao Mai Q.7", 0, 21, 0, 105_000L},
                {"MV03", "Biệt Đội Robot Tí Hon", "Rạp Sao Mai Q.7", 1, 10, 0, 75_000L},
                {"MV04", "Biệt Đội Robot Tí Hon", "Rạp Bến Thành Q.1", 1, 15, 30, 85_000L},
                {"MV05", "Đêm Trăng Trên Sông Hàn", "Rạp Bến Thành Q.1", 1, 19, 45, 110_000L},
                {"MV06", "Chuyến Tàu Cuối Cùng", "Rạp Sao Mai Q.7", 2, 20, 15, 100_000L},
                {"MV07", "Chuyến Tàu Cuối Cùng", "Rạp Bến Thành Q.1", 2, 22, 0, 110_000L},
                {"MV08", "Hành Tinh Xanh", "Rạp Sao Mai Q.7", 3, 9, 30, 70_000L},
        };
        for (Object[] r : m) {
            w.put("movies", (String) r[0], map("title", r[1], "cinema", r[2],
                    "showtime", at((Integer) r[3], (Integer) r[4], (Integer) r[5]), "price", r[6]));
        }
    }

    private static void seedHotels(Writer w) {
        Object[][] h = {
                {"HT01", "Sông Hàn Riverside Hotel", "Đà Nẵng", 1_250_000L, 4.5},
                {"HT02", "Phố Cổ Boutique", "Hà Nội", 980_000L, 4.3},
                {"HT03", "Biển Xanh Resort", "Phú Quốc", 2_450_000L, 4.7},
                {"HT04", "Nha Trang Sunrise", "Nha Trang", 1_100_000L, 4.2},
                {"HT05", "Đà Lạt Pine Hill", "Đà Lạt", 850_000L, 4.4},
                {"HT06", "Sài Gòn Central Inn", "TP.HCM", 1_050_000L, 4.1},
        };
        for (Object[] r : h) {
            w.put("hotels", (String) r[0], map("name", r[1], "city", r[2],
                    "pricePerNight", r[3], "rating", r[4]));
        }
    }

    private static void seedProducts(Writer w) {
        Object[][] p = {
                {"PR01", "Tai nghe Bluetooth", "Shop Âm Thanh Số", 590_000L},
                {"PR02", "Bình giữ nhiệt 500ml", "Nhà Bếp Xinh", 189_000L},
                {"PR03", "Balo laptop 15.6 inch", "Túi Ví Việt", 450_000L},
                {"PR04", "Bàn phím cơ", "Gear Corner", 1_290_000L},
                {"PR05", "Sạc dự phòng 10.000mAh", "Phụ Kiện 24h", 350_000L},
                {"PR06", "Sách: Lập trình Android cơ bản", "Nhà Sách Mới", 165_000L},
                {"PR07", "Áo thun cotton", "Thời Trang Basic", 149_000L},
                {"PR08", "Chuột không dây", "Gear Corner", 239_000L},
        };
        for (Object[] r : p) {
            w.put("products", (String) r[0], map("name", r[1], "shop", r[2], "price", r[3]));
        }
    }

    // ---------------------------------------------------------------- C5
    private static void seedBranches(Writer w) {
        // {id, tên, địa chỉ, lệch Bắc (m), lệch Đông (m)} so với DEMO_LAT/LNG
        Object[][] br = {
                {"BR01", "Chi nhánh Tân Phong", "Đường Nguyễn Hữu Thọ, P. Tân Phong, Q.7", 350, -250},
                {"BR02", "PGD Phú Mỹ Hưng", "Đường Nguyễn Văn Linh, Q.7", 1200, 900},
                {"BR03", "PGD Tân Hưng", "Đường Nguyễn Thị Thập, Q.7", 1800, 300},
                {"BR04", "PGD Huỳnh Tấn Phát", "Đường Huỳnh Tấn Phát, Q.7", 900, 2300},
                {"BR05", "PGD Nhà Bè", "Đường Lê Văn Lương, Nhà Bè", -1500, -600},
                {"BR06", "PGD Tân Quy", "Đường Lâm Văn Bền, Q.7", 2400, 1500},
                {"BR07", "PGD Bình Chánh", "Đường Phạm Hùng, Bình Chánh", 1500, -2200},
                {"BR08", "PGD Phước Kiển", "Đường Lê Văn Lương, Nhà Bè", -700, 700},
                {"BR09", "PGD Tân Kiểng", "Đường Trần Xuân Soạn, Q.7", 2900, -100},
                {"BR10", "PGD Phú Thuận", "Đường Phú Thuận, Q.7", 600, 3400},
        };
        double metersPerDegLat = 111_320.0;
        double metersPerDegLng = 111_320.0 * Math.cos(Math.toRadians(DEMO_LAT));
        for (Object[] r : br) {
            double lat = DEMO_LAT + ((Integer) r[3]) / metersPerDegLat;
            double lng = DEMO_LNG + ((Integer) r[4]) / metersPerDegLng;
            w.put("branches", (String) r[0], map("name", r[1], "address", r[2],
                    "lat", lat, "lng", lng, "phone", "1900 1234",
                    "openHours", "08:00 - 17:00 (T2 - T6), 08:00 - 11:30 (T7)"));
        }
    }
}

# Mobile Banking App — TDTU

> Môn: Mobile Apps Development | Nhóm: 3 thành viên

## Giới thiệu

Ứng dụng ngân hàng di động Android với các tính năng:
- 🔐 Xác thực Firebase Auth + eKYC khuôn mặt (ML Kit + TFLite)
- 💳 Tài khoản Checking / Saving / Mortgage
- 💸 Chuyển khoản nội bộ, liên ngân hàng, nạp qua VNPay
- 📱 Thanh toán hóa đơn, nạp điện thoại, mua vé
- 🗺️ Tìm chi nhánh gần nhất (Google Maps + Routes API)
- 🤖 Chatbot AI điều hướng (Gemini via Firebase AI Logic)
- 👮 Màn hình quản lý dành cho Officer

## Cài đặt

### Yêu cầu
- Android Studio Hedgehog trở lên
- Java 11
- minSdk 24

### Bước 1: Clone và mở project
```
git clone <repo-url>
```
Mở Android Studio → Open → chọn thư mục `Mobile-final-tdtu`

### Bước 2: Thêm `google-services.json`
Sao chép file `google-services.json` từ Firebase Console vào `app/google-services.json`
(xem mẫu tại `app/google-services.json.example`)

### Bước 3: Build và chạy
- **Debug**: Run → app (cài lên máy ảo hoặc thật)
- **Release APK**: Build → Generate Signed Bundle/APK → APK → dùng keystore nhóm

### Tài khoản demo (tạo ở D2)
| Role | Email | Password |
|------|-------|----------|
| Officer | officer@bank.tdtu | demo2024 |
| Customer 1 (VERIFIED) | customer1@test.com | demo2024 |
| Customer 2 (VERIFIED) | customer2@test.com | demo2024 |
| Customer 3 (PENDING) | customer3@test.com | demo2024 |

> Bật "Cài ứng dụng không rõ nguồn gốc" trên điện thoại trước khi cài APK release.

## Cấu trúc project

```
app/src/main/java/com/tdtu/mobilebanking/
├── auth/           — Đăng nhập / Đăng ký / Quên mật khẩu (Dev A)
├── officer/        — Quản lý khách hàng, mở tài khoản (Dev A)
├── kyc/            — eKYC khuôn mặt + CCCD (Dev A)
├── account/        — Tài khoản, lịch sử giao dịch (Dev B)
├── transaction/    — ConfirmTransaction, engine (Dev B)
├── otp/            — OTP 2FA qua email (Dev B)
├── payment/        — VNPay sandbox (Dev B)
├── saving/         — Tiết kiệm, lãi suất (Dev C)
├── mortgage/       — Vay thế chấp, lịch trả nợ (Dev C)
├── utility/        — Hóa đơn, nạp ĐT, checkout (Dev C)
├── map/            — Bản đồ chi nhánh (Dev C)
├── chatbot/        — Chatbot Gemini AI (Dev C)
└── common/         — Models, Interfaces, Stubs dùng chung
```

## Phân quyền Git

```
main      ← release (merge từ develop sau D7)
develop   ← integration branch
feature/<module>-<dev>   ← branch làm việc
```

**Merge Captain** xoay vòng mỗi ngày: D1=A, D2=B, D3=C, ...

## Triển khai

Theo yêu cầu của giảng viên, nhóm bàn giao **APK release** thay cho bản phát hành trên Google Play.

**Keystore release**: tạo D1, lưu tại `keystore/mobilebanking-release.jks` (không commit lên Git).  
SHA-1: `C2:FA:66:65:C8:7F:58:0F:36:31:ED:3C:FB:8B:5B:08:F0:1D:73:94`

## Hạn chế (ghi nhận)

- OTP sinh và kiểm tra ở client (Spark plan không có Cloud Functions)
- HashSecret VNPay nằm trong APK (sandbox, chỉ nộp cho giảng viên)
- Hướng phát triển: chuyển sang Blaze + Cloud Functions để bảo mật hơn

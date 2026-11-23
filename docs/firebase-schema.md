# Firestore Schema — Mobile Banking App

> Version: 1.0 — Đóng băng D1  
> Owner: Dev A  
> Thay đổi schema sau D1 phải được cả nhóm đồng ý và A cập nhật tài liệu này.

---

## Collections

### `users/{uid}`

Mô tả: Thông tin người dùng (khách hàng hoặc nhân viên).

| Field | Type | Mô tả | Ghi chú |
|-------|------|--------|---------|
| uid | string | Firebase Auth UID | = document ID |
| role | string | CUSTOMER \| OFFICER | — |
| fullName | string | Họ và tên | Bị khóa sau eKYC VERIFIED |
| cccd | string | Số CCCD (12 chữ số) | Bị khóa sau eKYC VERIFIED |
| dob | string | Ngày sinh ISO | Bị khóa sau eKYC VERIFIED |
| phone | string | Số điện thoại | Khách có thể sửa |
| email | string | Email | Khách có thể sửa |
| address | string | Địa chỉ | Khách có thể sửa |
| kycStatus | string | PENDING \| VERIFIED \| REJECTED | — |
| createdAt | timestamp | Ngày tạo | @ServerTimestamp |

---

### `accounts/{accountId}`

Mô tả: Tài khoản ngân hàng. 3 loại: CHECKING, SAVING, MORTGAGE.

**Trường chung (tất cả loại):**

| Field | Type | Mô tả |
|-------|------|--------|
| id | string | = document ID |
| ownerId | string | UID chủ tài khoản |
| type | string | CHECKING \| SAVING \| MORTGAGE |
| balance | number (long) | Số dư VND, luôn ≥ 0 |
| status | string | ACTIVE \| LOCKED \| CLOSED |
| lastTxId | string | ID giao dịch gần nhất (idempotency) |
| createdAt | timestamp | Ngày mở tài khoản |

**CheckingAccount — trường bổ sung:**

| Field | Type | Mô tả |
|-------|------|--------|
| dailyLimit | number (long) | Hạn mức rút/chuyển mỗi ngày (VND) |

**SavingAccount — trường bổ sung:**

| Field | Type | Mô tả |
|-------|------|--------|
| productId | string | Ref: rates/{productId} |
| principal | number (long) | Tiền gốc ban đầu (VND) |
| termMonths | number | Kỳ hạn (tháng) |
| openedAt | timestamp | Ngày mở |

**MortgageAccount — trường bổ sung:**

| Field | Type | Mô tả |
|-------|------|--------|
| principal | number (long) | Số tiền vay gốc (VND) |
| annualRate | number | Lãi suất năm (%) |
| termMonths | number | Thời hạn vay (tháng) |
| frequency | string | MONTHLY \| BIWEEKLY |
| startDate | string | Ngày bắt đầu (ISO date) |
| outstanding | number (long) | Dư nợ còn lại (VND) |

---

### `transactions/{txId}`

Mô tả: Lịch sử giao dịch. `txId` = `idempotencyKey` trong TxRequest.

| Field | Type | Mô tả |
|-------|------|--------|
| type | string | Enum TxType |
| fromAccountId | string? | null nếu tiền vào từ gateway |
| toAccountId | string? | null nếu tiền ra ngoài |
| amount | number (long) | > 0, VND |
| participants | string[] | [uid người gửi, uid người nhận] — dùng cho security rules |
| meta | map | Thông tin bổ sung (mã hóa đơn, ngân hàng nhận...) |
| createdBy | string | UID người thực hiện |
| createdAt | timestamp | @ServerTimestamp |
| status | string | SUCCESS \| FAILED |

---

### `kyc/{uid}`

Mô tả: Dữ liệu eKYC của khách. Officer ghi, chủ sở hữu đọc.

| Field | Type | Mô tả |
|-------|------|--------|
| embedding | string | Base64 face embedding (TFLite output) |
| photoBase64 | string | Ảnh khuôn mặt nén JPEG ≤ 40KB, Base64 |
| cccdPhotoBase64 | string | Ảnh CCCD nén, Base64 |
| enrolledAt | timestamp | Thời điểm đăng ký |
| enrolledBy | string | UID officer thực hiện |
| modelName | string | Tên model TFLite (ghi để báo cáo) |
| threshold | number | Ngưỡng cosine similarity |

---

### `rates/{productId}`

Mô tả: Sản phẩm tiết kiệm và lãi suất. Officer chỉnh, khách đọc.

| Field | Type | Mô tả |
|-------|------|--------|
| productName | string | Tên sản phẩm |
| termMonths | number | Kỳ hạn (tháng) |
| annualRate | number | Lãi suất năm (%) |
| minDeposit | number (long) | Tiền gửi tối thiểu (VND) |
| updatedAt | timestamp | @ServerTimestamp |

---

### `profileRequests/{reqId}`

Mô tả: Yêu cầu sửa hồ sơ của khách, officer duyệt/từ chối.

| Field | Type | Mô tả |
|-------|------|--------|
| uid | string | UID khách gửi yêu cầu |
| requestedChanges | map | {field: newValue} |
| status | string | PENDING \| APPROVED \| REJECTED |
| reason | string? | Lý do từ chối |
| reviewedBy | string? | UID officer |
| createdAt | timestamp | @ServerTimestamp |
| reviewedAt | timestamp? | @ServerTimestamp |

---

### `config/limits`

Mô tả: Config hệ thống (set thủ công qua console, không sửa code).

| Field | Type | Mô tả |
|-------|------|--------|
| highValueThreshold | number (long) | Ngưỡng giao dịch cần eKYC (mặc định: 10.000.000) |
| maxDailyWithdraw | number (long) | Hạn mức rút mặc định (mặc định: 50.000.000) |

---

## Phi chuẩn hóa có chủ đích

| Nơi phi chuẩn hóa | Lý do |
|---------------------|-------|
| `participants[]` trong transactions | Security Rules cần array-contains để check quyền đọc |
| `lastTxId` trong accounts | Idempotency check trong AB-R Rules mà không cần read thêm |
| `kycStatus` trong users | Validate nhanh trong ConfirmTransaction mà không cần join kyc/ |

---

## Security Rules tóm tắt

| Collection | Customer đọc | Customer ghi | Officer đọc | Officer ghi |
|------------|-------------|--------------|-------------|-------------|
| users/{uid} | Chỉ của mình | phone/email/address | Tất cả | Tất cả |
| accounts/{accId} | Chỉ của mình | Không (AB-R) | Tất cả | Tạo mới |
| transactions/{txId} | Nếu là participant | Qua runTransaction | Tất cả | Không |
| kyc/{uid} | Chỉ của mình | Không | Tất cả | Tất cả |
| rates/{productId} | Tất cả | Không | Tất cả | Tất cả |
| profileRequests/{reqId} | Chỉ của mình | Tạo mới | Tất cả | Duyệt/từ chối |
| config/{docId} | Đọc | Không | Đọc | Không |

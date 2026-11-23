# Demo Accounts — Seed Data D2

> Tạo thủ công trên Firebase Console → Firestore → Add document
> Hoặc dùng script Node.js bên dưới.

---

## Tài khoản demo cần tạo

### 1. Officer account

**Firebase Auth** (Console → Authentication → Add user):
- Email: `officer@bank.tdtu.vn`
- Password: `Officer2024!`

**Firestore** `users/{uid-officer}` (thay uid-officer bằng uid thật sau khi tạo Auth):
```json
{
  "uid": "<<uid-officer>>",
  "fullName": "Trần Thị Officer",
  "email": "officer@bank.tdtu.vn",
  "phone": "0901234567",
  "cccd": "079000000001",
  "dob": "01/01/1990",
  "address": "123 Nguyễn Văn Cừ, Q5, TP.HCM",
  "role": "officer",
  "kycStatus": "VERIFIED",
  "status": "ACTIVE",
  "mustChangePassword": false,
  "createdAt": "<<serverTimestamp>>"
}
```

---

### 2. Customer 1 — VERIFIED (dùng để demo giao dịch)

**Firebase Auth**:
- Email: `customer1@test.com`
- Password: `Customer2024!`

**Firestore** `users/{uid-c1}`:
```json
{
  "uid": "<<uid-c1>>",
  "fullName": "Nguyễn Văn An",
  "email": "customer1@test.com",
  "phone": "0912345678",
  "cccd": "079000000002",
  "dob": "15/03/1995",
  "address": "456 Lê Văn Việt, Q9, TP.HCM",
  "role": "customer",
  "kycStatus": "VERIFIED",
  "status": "ACTIVE",
  "mustChangePassword": false,
  "createdAt": "<<serverTimestamp>>"
}
```

**Firestore** `accounts/{auto-id-1}`:
```json
{
  "ownerId": "<<uid-c1>>",
  "accountNumber": "100000000001",
  "type": "CHECKING",
  "balance": 5000000,
  "currency": "VND",
  "status": "ACTIVE",
  "dailyLimit": 50000000,
  "lastTxId": null,
  "createdBy": "<<uid-officer>>",
  "createdAt": "<<serverTimestamp>>"
}
```

**Firestore** `accountDirectory/100000000001`:
```json
{
  "accountId": "<<auto-id-1>>",
  "ownerId": "<<uid-c1>>",
  "ownerName": "Nguyễn Văn An",
  "type": "CHECKING",
  "status": "ACTIVE"
}
```

**Firestore** `kyc/{uid-c1}`:
```json
{
  "uid": "<<uid-c1>>",
  "embedding": "PLACEHOLDER_EMBEDDING",
  "photoBase64": "PLACEHOLDER_PHOTO",
  "enrolledBy": "<<uid-officer>>",
  "enrolledAt": "<<serverTimestamp>>",
  "modelName": "MobileFaceNet",
  "threshold": 0.75
}
```

---

### 3. Customer 2 — VERIFIED (người nhận chuyển khoản)

**Firebase Auth**:
- Email: `customer2@test.com`
- Password: `Customer2024!`

**Firestore** `users/{uid-c2}`:
```json
{
  "uid": "<<uid-c2>>",
  "fullName": "Lê Thị Bình",
  "email": "customer2@test.com",
  "phone": "0923456789",
  "cccd": "079000000003",
  "dob": "20/07/1998",
  "address": "789 Đinh Tiên Hoàng, Q1, TP.HCM",
  "role": "customer",
  "kycStatus": "VERIFIED",
  "status": "ACTIVE",
  "mustChangePassword": false,
  "createdAt": "<<serverTimestamp>>"
}
```

**Firestore** `accounts/{auto-id-2}`:
```json
{
  "ownerId": "<<uid-c2>>",
  "accountNumber": "100000000002",
  "type": "CHECKING",
  "balance": 2000000,
  "currency": "VND",
  "status": "ACTIVE",
  "dailyLimit": 50000000,
  "lastTxId": null,
  "createdBy": "<<uid-officer>>",
  "createdAt": "<<serverTimestamp>>"
}
```

**Firestore** `accountDirectory/100000000002`:
```json
{
  "accountId": "<<auto-id-2>>",
  "ownerId": "<<uid-c2>>",
  "ownerName": "Lê Thị Bình",
  "type": "CHECKING",
  "status": "ACTIVE"
}
```

---

### 4. Customer 3 — PENDING (chưa KYC)

**Firebase Auth**:
- Email: `customer3@test.com`
- Password: `Customer2024!`

**Firestore** `users/{uid-c3}`:
```json
{
  "uid": "<<uid-c3>>",
  "fullName": "Phạm Minh Cường",
  "email": "customer3@test.com",
  "phone": "0934567890",
  "cccd": "079000000004",
  "dob": "10/11/2000",
  "address": "321 Trần Hưng Đạo, Q5, TP.HCM",
  "role": "customer",
  "kycStatus": "PENDING",
  "status": "ACTIVE",
  "mustChangePassword": false,
  "createdAt": "<<serverTimestamp>>"
}
```

---

## Config document

**Firestore** `config/limits`:
```json
{
  "highValueThreshold": 10000000,
  "maxDailyWithdraw": 50000000
}
```

---

## Rates (sản phẩm tiết kiệm)

**Firestore** `rates/saving_3m`:
```json
{
  "productId": "saving_3m",
  "productName": "Tiết kiệm 3 tháng",
  "termMonths": 3,
  "annualRate": 5.5,
  "minDeposit": 1000000
}
```

**Firestore** `rates/saving_6m`:
```json
{
  "productId": "saving_6m",
  "productName": "Tiết kiệm 6 tháng",
  "termMonths": 6,
  "annualRate": 6.5,
  "minDeposit": 1000000
}
```

**Firestore** `rates/saving_12m`:
```json
{
  "productId": "saving_12m",
  "productName": "Tiết kiệm 12 tháng",
  "termMonths": 12,
  "annualRate": 7.5,
  "minDeposit": 1000000
}
```

---

## Hướng dẫn tạo nhanh

Cách đơn giản nhất:
1. Firebase Console → Authentication → **Add user** (tạo 4 email/password)
2. Copy UID → Firestore → tạo từng document theo JSON trên
3. Thay `<<uid-xxx>>` và `<<auto-id-xxx>>` bằng giá trị thật

> ⚠️ Nhớ tạo `config/limits` trước khi test giao dịch,
> vì ConfirmTransactionFragment đọc `highValueThreshold` từ đây.

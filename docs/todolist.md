# Dev A — Todolist (Cập nhật: 27/11/2026)

## ✅ Đã hoàn thành

### D1 — Setup & Auth
- [x] Project structure (MVVM, ViewBinding, Navigation Component)
- [x] `build.gradle` — Firebase BOM 33.10.0, CameraX, ML Kit, etc.
- [x] `AndroidManifest.xml` — permissions
- [x] `MainActivity.java` — single-activity NavHostFragment
- [x] `google-services.json`
- [x] Auth module: `AuthRepository`, `AuthViewModel`, `LoginFragment`, `RegisterFragment`, `ForgotPasswordFragment`
- [x] Models: `User`, `Account`, `CheckingAccount`, `SavingAccount`, `MortgageAccount`, `TxRequest`, `TxType`, `ProfileRequest`, `ErrorCodes`
- [x] Common contracts: `KycVerifier`, `OtpService`, `TransactionService`, `AccountRepository`
- [x] Stubs: `FakeKycVerifier`, `FakeOtpService`
- [x] Navigation graphs: `nav_root`, `nav_auth`, `nav_officer`, `nav_kyc`
- [x] Themes/Colors/Dimens Material3
- [x] Keystore release (`keystore/mobilebanking-release.jks`)

### D2 — Officer Module & Security Rules
- [x] `firestore.rules` v2 (validDebit, validCredit, isNewTx)
- [x] `OfficerKycHelper.java` — Secondary FirebaseApp, createCustomer, openAccounts
- [x] Officer UI: `OfficerHomeFragment`, `CreateCustomerFragment`, `OpenAccountFragment`
- [x] `docs/firebase-schema.md`, `docs/demo-accounts.md`, `docs/use-case.md`

### Firebase Console
- [x] Firebase project created, app registered
- [x] Email/Password auth enabled
- [x] SHA-1 release fingerprint added
- [x] Firestore database created
- [x] Security Rules deployed
- [x] Seed script run — 4 Auth users + Firestore data
- [x] Composite indexes (4/5 đã tạo thủ công, đang Building)

### D3 — Customer Profile, eKYC, Officer Profile Requests
- [x] A5: `CustomerProfileFragment` — view/edit profile, submit change requests
- [x] A5: `ChangePasswordFragment` — reauthenticate + updatePassword
- [x] A5: `ChangePinFragment` — SHA-256 hash PIN
- [x] A6: `ProfileRequestsFragment` — RecyclerView PENDING requests
- [x] A6: `ProfileRequestDetailFragment` — approve/reject batch write
- [x] A6: `ProfileRequestAdapter`
- [x] A4: `KycFaceEnrollFragment` — CameraX + ML Kit face detection + pseudo-embedding
- [x] A7: `KycCccdFragment` — CameraX + ML Kit TextRecognition, extract CCCD/name/DOB
- [x] A7: `KycResultFragment` — success/failure screen
- [x] `nav_account.xml` created (profile routes)

### Diagrams
- [x] `docs/diagrams.md` — Use Case, Class Diagram, ERD (PlantUML + Mermaid)
- [x] `.drawio` diagram files (use-case-tong-quan, use-case-chi-tiet, class-diagram, erd-collections, erd-schema)

### D4 — Bug Fixes
- [x] Bug 1: `firestore.rules` — thêm `pinHash`, `kycStatus` vào whitelist customer self-update
- [x] Bug 2: `firestore.rules` — cho phép customer tự write `kyc/{uid}` (face enroll, CCCD scan)
- [x] Bug 3: `nav_root.xml` — uncomment `nav_account` include
- [x] Bug 4: `ProfileRequestsFragment.java` — đổi orderBy `ASCENDING` → `DESCENDING` khớp index

### D4 — Code Features
- [x] **A4 Part 2: `KycFaceVerifyFragment.java`** — CameraX + ML Kit + cosine similarity (≥ 0.75) + liveness chớp mắt (`DialogFragment` overlay + `nav_kyc`)
- [x] **`RealKycVerifier.java`** + **`FaceEmbeddingHelper.java`** — implements `KycVerifier` & tách helper dùng chung với `KycFaceEnrollFragment`
- [x] **`fragment_kyc_face_verify.xml`** + **`bg_face_oval.xml`** — layout màn xác thực khuôn mặt
- [x] **A3: `CustomerDetailFragment.java`** — info khách, sửa thông tin khách, danh sách tài khoản, khóa/mở TK, chỉnh `dailyLimit`, mở thêm TK
- [x] **`AccountListAdapter.java`** — RecyclerView adapter cho danh sách tài khoản
- [x] **`fragment_customer_detail.xml`** + **`item_account_row.xml`** + **`bg_badge_rounded.xml`** — layouts
- [x] **`OfficerHomeFragment.java`** + **`fragment_officer_home.xml`** — thêm search customer (email / CCCD / SĐT / họ tên)
- [x] **`CustomerProfileFragment.java`** — thêm điều hướng eKYC, test `RealKycVerifier`, đăng xuất
- [x] **Gradle compile test** — `./gradlew.bat compileDebugJavaWithJavac` pass
- [x] **`signingConfigs.release`** — mở comment trong `app/build.gradle`
- [x] **Merge `origin/Sang` (`06ae336`)** — untrack `app/build/` khỏi Git
- [x] **Git commit D4 + push lên nhánh `Anh`**

### Merge Dev C (Hương) — 27/11/2026
- [x] Merge `origin/Hương` → `Anh` (commit `4be7b4a`): saving, mortgage, home, seed data, common utils, design system
- [x] **fix: gỡ 2 conflict markers** trong `nav_root.xml` và `fragment_customer_profile.xml`
- [x] **fix: dedup `md_theme_*` colors** — xóa duplicate khỏi `colors.xml`, giữ định nghĩa trong `colors_design.xml`
- [x] **fix: sửa style `Widget.Material3.Button.OutlinedButton` → `Widget.App.Button.Outlined`** trong `fragment_customer_profile.xml`
- [x] **fix: thêm rules cho 8 collection SeedData** vào `firestore.rules` (`banks`, `externalAccounts`, `bills`, `flights`, `movies`, `hotels`, `products`, `branches`)
- [x] **Compile test: BUILD SUCCESSFUL** (18s)
- [x] **Push commit `fdefbb8` lên `origin/Anh`**

---

## 🔧 Cần làm tiếp

### Firebase Console (cần làm thủ công trên trình duyệt)
- [ ] **Deploy Security Rules mới** lên Firebase Console — `firestore.rules` đã cập nhật (D4 + SeedData rules)
- [ ] **Tạo index #5** trên Firestore Console: collection `profileRequests`, fields `uid↑, status↑, createdAt↓`
  - Chọn "Create structured index" → Next → Collection: `profileRequests`
  - Field 1: `uid` Ascending
  - Field 2: `status` Ascending
  - Field 3: `createdAt` Descending

### D5 — Hoàn thiện
- [ ] **Build APK release** — `.\gradlew.bat assembleRelease --no-daemon` với JAVA_HOME set
- [ ] **Test trên thiết bị thật** — officer login, customer login, eKYC, sửa/mở/khóa TK, tiết kiệm, khoản vay
- [ ] **Chạy SeedData** trên máy thật (nút debug trên HomeFragment sau khi login officer) để có data demo
- [ ] Update `docs/firebase-schema.md` — thêm field `pinHash`, `mustChangePassword` vào `users/` schema
- [ ] Update `docs/diagrams.md` nếu có thay đổi class/ERD sau D4

### Blocked — Chờ Dev B (Sang)
- [ ] Merge code Dev B vào nhánh Anh (Sang chưa push code mới!)
- [ ] `ConfirmTransactionFragment` integration với `RealKycVerifier`
- [ ] `nav_transaction` include vào `nav_root.xml` (xem comment trong file)
- [ ] Nối `AccountList`, lịch sử giao dịch
- [ ] Test AB-R Rules giao dịch & end-to-end chuyển tiền
- [ ] **Nhắc Sang push code: `git push origin Sang`**

### Blocked — Chờ Dev C (Hương) — phần còn lại
- [ ] `nav_utility` — C4 hóa đơn điện/nước, nạp ĐT, checkout vé/khách sạn
- [ ] `nav_map` — C5 bản đồ chi nhánh (Google Maps)
- [ ] `nav_chatbot` — C6 AI Chatbot Gemini
- [ ] C8 Liên ngân hàng

---

## 📊 Tổng kết tiến độ

| Ngày | Kế hoạch | Trạng thái |
|------|----------|------------|
| D1   | Setup & Auth | ✅ Done |
| D2   | Officer Module & Rules | ✅ Done |
| D3   | Profile, eKYC, Diagrams | ✅ Done |
| D4   | Bug fixes + KycVerify + CustomerDetail + Compile check | ✅ Done |
| Merge C | Hương: Home, Saving, Mortgage, Seed, Common, Design system | ✅ Done (27/11) |
| D5   | Deploy Rules/Indexes + Build APK + Device test + Docs freeze | ⏳ Đang làm |
| D6   | APK release + Device test + Report | ⏳ Pending |
| D7   | Proofread & Submit | ⏳ Pending |

---

## Demo accounts

| Role | Email | Password |
|------|-------|----------|
| Officer | officer@bank.tdtu.vn | Officer2024! |
| Customer 1 | customer1@test.com | Customer2024! |
| Customer 3 | customer3@test.com | Customer2024! |

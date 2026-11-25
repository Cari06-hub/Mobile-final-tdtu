# Dev A — Todolist (Cập nhật: 26/11/2026)

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

---

## 🔧 Cần làm tiếp

### D4 — Code Features (Ưu tiên cao)
- [ ] **A4 Part 2: `KycFaceVerifyFragment.java`** — Replace stub
  - Load `kyc/{uid}.embedding` từ Firestore
  - CameraX → ML Kit FaceDetector → extract landmarks → pseudo-embedding
  - Cosine similarity ≥ 0.75 → verified
  - Liveness: eye-blink detection
  - Implement `RealKycVerifier` (implements `KycVerifier`)
  - Layout: `fragment_kyc_face_verify.xml`

- [ ] **A3: `CustomerDetailFragment.java`** — Replace stub
  - Load `users/{uid}` + `accounts` where `ownerId == uid`
  - Hiển thị info + danh sách accounts
  - Officer: lock/unlock account, navigate to open new account
  - Thêm search customer (by CCCD/phone/name) vào `OfficerHomeFragment`
  - Layout: `fragment_customer_detail.xml`

### Build & Verification
- [ ] **Gradle compile test** — `./gradlew compileDebugJavaWithJavac`
- [ ] **Configure `signingConfigs.release`** trong `app/build.gradle` (line 23 commented)
- [ ] **Deploy composite indexes** trên Firestore Console
- [ ] **Git commit tất cả fixes + push to Anh**

### D5–D6 — Hoàn thiện
- [ ] Update `docs/diagrams.md` và `docs/firebase-schema.md` nếu có field mới
- [ ] Build APK release — `./gradlew assembleRelease`
- [ ] Test trên thiết bị thật — officer login, customer login, eKYC
- [ ] Viết phần báo cáo: Ch.3 (deployment), Ch.4 (UI screenshots)

### Blocked — Chờ Dev B (Sang)
- [ ] Merge code Dev B vào nhánh Anh (Sang chưa push code mới!)
- [ ] `ConfirmTransactionFragment` integration với `KycVerifier`
- [ ] Test end-to-end chuyển tiền
- [ ] Nhắc Sang push code: `git push origin Sang`

### Blocked — Chờ Dev C (Hương)
- [ ] `RateManagementFragment` (Dev C owns) — chưa có code
- [ ] `nav_utility`, `nav_map`, `nav_chatbot` — chưa có code

---

## 📊 Tổng kết tiến độ

| Ngày | Kế hoạch | Trạng thái |
|------|----------|------------|
| D1   | Setup & Auth | ✅ Done |
| D2   | Officer Module & Rules | ✅ Done |
| D3   | Profile, eKYC, Diagrams | ✅ Done |
| D4   | Bug fixes + KycVerify + CustomerDetail | 🔧 Bug fixes done, code features pending |
| D5   | Integration test + APK build | ⏳ Pending |
| D6   | Report + polish | ⏳ Pending |
| D7   | Submit | ⏳ Pending |

/**
 * seed.js — Tự động tạo demo accounts cho Mobile Banking App
 *
 * Cách dùng:
 *   1. Đặt file serviceAccountKey.json vào thư mục seed/
 *   2. cd seed
 *   3. npm install firebase-admin
 *   4. node seed.js
 */

const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const auth = admin.auth();
const db = admin.firestore();

// =============================================
// Demo accounts cần tạo
// =============================================
const DEMO_USERS = [
  {
    email: "officer@bank.tdtu.vn",
    password: "Officer2024!",
    firestoreDoc: {
      fullName: "Trần Thị Officer",
      email: "officer@bank.tdtu.vn",
      phone: "0901234567",
      cccd: "079000000001",
      dob: "01/01/1990",
      address: "123 Nguyễn Văn Cừ, Q5, TP.HCM",
      role: "officer",
      kycStatus: "VERIFIED",
      status: "ACTIVE",
      mustChangePassword: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    },
  },
  {
    email: "customer1@test.com",
    password: "Customer2024!",
    firestoreDoc: {
      fullName: "Nguyễn Văn An",
      email: "customer1@test.com",
      phone: "0912345678",
      cccd: "079000000002",
      dob: "15/03/1995",
      address: "456 Lý Thường Kiệt, Q10, TP.HCM",
      role: "customer",
      kycStatus: "VERIFIED",
      status: "ACTIVE",
      mustChangePassword: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    },
  },
  {
    email: "customer2@test.com",
    password: "Customer2024!",
    firestoreDoc: {
      fullName: "Lê Thị Bình",
      email: "customer2@test.com",
      phone: "0923456789",
      cccd: "079000000003",
      dob: "20/07/1998",
      address: "789 Điện Biên Phủ, Bình Thạnh, TP.HCM",
      role: "customer",
      kycStatus: "VERIFIED",
      status: "ACTIVE",
      mustChangePassword: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    },
  },
  {
    email: "customer3@test.com",
    password: "Customer2024!",
    firestoreDoc: {
      fullName: "Phạm Minh Cường",
      email: "customer3@test.com",
      phone: "0934567890",
      cccd: "079000000004",
      dob: "10/11/2000",
      address: "321 Hoàng Diệu, Q4, TP.HCM",
      role: "customer",
      kycStatus: "PENDING",
      status: "ACTIVE",
      mustChangePassword: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    },
  },
];

// =============================================
// Dữ liệu tĩnh
// =============================================
const CONFIG_LIMITS = {
  highValueThreshold: 10000000,
  maxDailyWithdraw: 50000000,
};

const RATES = [
  {
    id: "saving_3m",
    data: {
      productName: "Tiết kiệm 3 tháng",
      termMonths: 3,
      annualRate: 5.5,
      minDeposit: 1000000,
      status: "ACTIVE",
    },
  },
  {
    id: "saving_6m",
    data: {
      productName: "Tiết kiệm 6 tháng",
      termMonths: 6,
      annualRate: 6.5,
      minDeposit: 1000000,
      status: "ACTIVE",
    },
  },
  {
    id: "saving_12m",
    data: {
      productName: "Tiết kiệm 12 tháng",
      termMonths: 12,
      annualRate: 7.5,
      minDeposit: 1000000,
      status: "ACTIVE",
    },
  },
];

// =============================================
// Logic seed
// =============================================
async function createOrGetUser(email, password) {
  try {
    // Thử tạo mới
    const user = await auth.createUser({ email, password });
    console.log(`  ✅ Tạo Auth user: ${email} → uid: ${user.uid}`);
    return user.uid;
  } catch (err) {
    if (err.code === "auth/email-already-exists") {
      // Đã tồn tại → lấy UID
      const existing = await auth.getUserByEmail(email);
      console.log(`  ⚠️  Auth user đã tồn tại: ${email} → uid: ${existing.uid}`);
      return existing.uid;
    }
    throw err;
  }
}

async function seedUsers() {
  console.log("\n📋 Seeding users...");
  for (const u of DEMO_USERS) {
    try {
      const uid = await createOrGetUser(u.email, u.password);
      await db
        .collection("users")
        .doc(uid)
        .set({ uid, ...u.firestoreDoc }, { merge: true });
      console.log(`  📄 Firestore users/${uid} → OK`);
    } catch (err) {
      console.error(`  ❌ Lỗi với ${u.email}:`, err.message);
    }
  }
}

async function seedConfig() {
  console.log("\n⚙️  Seeding config/limits...");
  await db.collection("config").doc("limits").set(CONFIG_LIMITS, { merge: true });
  console.log("  📄 config/limits → OK");
}

async function seedRates() {
  console.log("\n💰 Seeding rates...");
  for (const r of RATES) {
    await db.collection("rates").doc(r.id).set(r.data, { merge: true });
    console.log(`  📄 rates/${r.id} → OK`);
  }
}

async function main() {
  console.log("🚀 Bắt đầu seed dữ liệu demo...\n");
  try {
    await seedUsers();
    await seedConfig();
    await seedRates();
    console.log("\n✅ Seed xong! Kiểm tra Firestore Console để xác nhận.");
  } catch (err) {
    console.error("\n❌ Lỗi không xử lý được:", err);
  } finally {
    process.exit(0);
  }
}

main();

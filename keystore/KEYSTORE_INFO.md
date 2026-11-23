# Keystore Release — Mobile Banking App TDTU

> ⚠️ File này chứa thông tin nhạy cảm. KHÔNG commit lên Git public. Chia sẻ riêng cho thành viên nhóm.

## Thông tin keystore

| Thông tin | Giá trị |
|-----------|---------|
| **File** | `mobilebanking-release.jks` |
| **Alias** | `mobilebanking` |
| **Store Password** | `banking2024` |
| **Key Password** | `banking2024` |
| **Validity** | 10.000 ngày |
| **Algorithm** | RSA 2048-bit |

## Fingerprints

```
SHA-1:   C2:FA:66:65:C8:7F:58:0F:36:31:ED:3C:FB:8B:5B:08:F0:1D:73:94
SHA-256: AE:E1:30:D7:5F:A1:FA:18:C4:EE:4F:A3:DF:68:85:B1:61:E8:EB:80:28:53:73:2F:6A:3C:1C:95:B3:C0:84:D3
```

## Việc cần làm (D6)

1. Thêm SHA-1 release vào **Google Maps API key** (Google Cloud Console → APIs & Services → Credentials)
2. Thêm SHA-1 release vào **Firebase** (Project Settings → Your apps → Add fingerprint)
3. Nếu bật App Check, thêm SHA-256 vào App Check config

## Cách build APK release (D6)

```bash
# Trong Android Studio: Build → Generate Signed Bundle/APK → APK
# Hoặc qua Gradle:
./gradlew assembleRelease

# File output: app/build/outputs/apk/release/app-release.apk
```

## Cấu hình signingConfig trong app/build.gradle (D6)

```groovy
android {
    signingConfigs {
        release {
            storeFile file('../keystore/mobilebanking-release.jks')
            storePassword 'banking2024'
            keyAlias 'mobilebanking'
            keyPassword 'banking2024'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
        }
    }
}
```

## Phân phối cho nhóm

- **Dev A** giữ file `.jks` gốc
- Chia sẻ cho **Dev B** và **Dev C** qua kênh riêng (không qua Git)
- **Không** commit file `.jks` lên repository (đã thêm vào `.gitignore`)

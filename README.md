# Auto Payment Android

[![Build APK](https://github.com/sinhsac/auto-payment-android/actions/workflows/build.yml/badge.svg)](https://github.com/sinhsac/auto-payment-android/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)

The Android notification listener companion app for **[auto-payment](https://github.com/sinhsac/auto-payment)** — developed by **Hỗ trợ giải pháp số** — enabling **zero-fee, self-hosted automated bank transfer confirmation** without third-party payment gateways.

## Key Features (v1.1.0)

- **Modern Material 3 Mobile-First UI (Jetpack Compose)**:
  - **Dashboard Screen (`Danh sách đơn`)**: Live listener status, total matched VND KPI, instant 1-tap **Sync Now** button, search bar, status filter chips (`All`, `Synced`, `Pending`, `Failed`), order cards with 1-tap retry, and tap-to-inspect full bank notification & server response.
  - **Dedicated Settings Screen (`Cấu hình`)**: Configure **Webhook URL**, **Webhook Secret (`X-Webhook-Secret`)**, **Order Code Prefix** (`AP`), **Known Bank Apps Only** toggle, **Test Webhook**, all device permissions & battery exemption shortcuts, database cleanup, and **Hỗ trợ giải pháp số** app info.
- **Offline-First SQLite History & Automatic Recovery Sync**:
  - Every matched payment is persisted immediately to local SQLite (`PaymentDatabaseHelper`, deduplicated by SHA-256 fingerprint) **before** calling the server.
  - If the server is unreachable or restarting, payments remain safely queued as `PENDING`/`FAILED` and are automatically re-synced by **`SyncPendingWorker`** every 15 minutes (or immediately via the **Sync Now** / **Retry** buttons).
- **Custom Payment Adaptive Icon & Brand Identity**: Vector payment receipt + emerald verification badge launcher icon branded by **Hỗ trợ giải pháp số**.
- **Full `bigText` Support & On-Device Filtering**: Reads `Notification.EXTRA_BIG_TEXT`, `EXTRA_TEXT_LINES`, and `EXTRA_TEXT`, extracts the web-generated order code + credit amount on-device, rejects outgoing debits, and never leaks personal chat notifications.

## Webhook Payload Format

Each matched payment notification is posted as `application/json; charset=utf-8` (with optional `X-Webhook-Secret` header):

```json
{
  "groupKey": "0|com.VCB|...",
  "postTime": "Nov 27, 2026 5:30:00 PM",
  "strPostTime": "2026-11-27 17:30:00",
  "title": "Vietcombank",
  "msg": "SD TK 0123456789 +150,000VND luc 27/11/2026. ND: AP849201",
  "pck": "com.VCB",
  "orderCode": "AP849201",
  "amount": 150000
}
```

## Download or Build APK

- **Prebuilt APK (with QR Code):** Open the **[Releases page](https://github.com/sinhsac/auto-payment-android/releases)** and scan the embedded **QR code** with your Android phone to download and install directly (or grab the latest build from **[GitHub Actions → Artifacts](https://github.com/sinhsac/auto-payment-android/actions/workflows/build.yml)**).
- **Build Locally (JDK 17 + Android SDK 34):**

```bash
git clone https://github.com/sinhsac/auto-payment-android.git
cd auto-payment-android
./gradlew testDebugUnitTest assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

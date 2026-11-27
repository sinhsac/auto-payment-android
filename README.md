# Auto Payment Android

[![Build APK](https://github.com/sinhsac/auto-payment-android/actions/workflows/build.yml/badge.svg)](https://github.com/sinhsac/auto-payment-android/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)

The Android notification listener companion app for **[auto-payment](https://github.com/sinhsac/auto-payment)** — enabling **zero-fee, self-hosted automated bank transfer confirmation** without third-party payment gateways.

## How It Works Together with `auto-payment`

1. The **[auto-payment](https://github.com/sinhsac/auto-payment)** web storefront generates a unique order code (e.g., `AP123456`) for each customer checkout.
2. **Auto Payment Android** runs on the merchant's Android phone and listens to incoming bank/wallet notifications via `NotificationListenerService` (reading full `android.bigText`, `android.textLines`, and `android.text`).
3. **`PaymentNotificationParser`** filters notifications **on-device**:
   - Extracts the order code matching your configured prefix (e.g., `AP\d{6}`).
   - Extracts incoming credit amounts (`+150,000VND`, `+1.290.000 VND`, etc.) while rejecting outgoing debits (`-150,000VND`, debit keywords).
   - Ignores unrelated chat/social notifications so private messages are never sent to your server.
4. **`PushingWorker`** (AndroidX `WorkManager`) posts matched payment payloads as JSON (with optional `X-Webhook-Secret` header and exponential-backoff retries on network/5xx errors) to `POST https://<your-server>/api/webhooks/bank-notify`.
5. The web server marks the order `PAID` and pushes an instant **Server-Sent Events (SSE)** update to complete the customer's checkout screen in real time.

## Key Features

- **In-App Configuration UI (`SettingActivity`)**: Configure your **Webhook URL**, **Webhook Secret (`X-Webhook-Secret`)**, **Order Code Prefix** (`AP`), **Known Bank Apps Only** toggle, and **Enable/Disable Listening** directly on the phone — no code rebuild required.
- **One-Tap Test Webhook & Activity Log**: Send a test webhook payload straight from the app to verify connectivity with your `auto-payment` server and inspect the last 25 matched/forwarded payment events.
- **Full `bigText` Support**: Reads `Notification.EXTRA_BIG_TEXT` first so long Vietnamese bank notifications (Vietcombank, Techcombank, MBBank, VPBank, BIDV, ACB, TPBank, VietinBank, Sacombank, Timo, MoMo, ZaloPay) are never truncated before the order code.
- **Reliable Queueing & Auto-Retry**: Each matched order uses a unique WorkManager job name (`autopay_<orderCode>_<timestamp>`) so simultaneous transfers never overwrite each other, with up to 5 exponential-backoff retries on temporary network issues.
- **Background Keep-Alive & OEM Auto-Start Shortcuts**: Includes `KeepAliveService`, boot restart receiver, battery optimization exemption, and OEM auto-start shortcuts (Xiaomi, Samsung, Oppo, Vivo, Huawei, Honor, Asus, OnePlus, Nokia).

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

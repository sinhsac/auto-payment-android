# Auto Payment Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Gradle](https://img.shields.io/badge/Gradle-8.2-02303A.svg?logo=gradle&logoColor=white)](https://gradle.org/)

An Android notification listener and webhook forwarding app built in Kotlin for automated payment verification workflows. It captures incoming device notifications (such as banking and payment alerts) via Android's `NotificationListenerService`, serializes the notification metadata to JSON, and posts it asynchronously to a backend webhook endpoint using AndroidX WorkManager.

## Key Features

- **Real-Time Notification Listening (`NotifyListenerService`)**: Intercepts incoming `StatusBarNotification` events, skips excluded packages (`Constants.UNSUPPORTED_APPS`), and extracts package name, title, message body, group key, and timestamps into an `XInfo` payload.
- **Network-Aware Webhook Forwarding (`PushingWorker`)**: Serializes notification payloads with Gson and dispatches HTTPS `POST` requests (`application/json; charset=utf-8`) via `WorkManager` with a `NetworkType.CONNECTED` constraint.
- **Persistent Background Service (`KeepAliveService`)**: Runs a `START_STICKY` foreground service (`remoteMessaging` type) with an ongoing status notification to keep the notification listener active.
- **Automatic Restart on Boot & Task Removal (`NotificationServiceRestartReceiver`)**: Listens for `BOOT_COMPLETED`, `QUICKBOOT_POWERON`, and internal reconnect broadcasts to restart `KeepAliveService`.
- **Guided Permission & OEM Auto-Start Setup (`SettingActivity` & `AutoStartHelper`)**: Provides one-tap shortcuts to configure Notification Listener access, battery optimization exemptions, Android 13+ notification permissions, and vendor-specific auto-start screens (Xiaomi/Redmi/Poco, Samsung, Oppo, Vivo, Huawei, Honor, Asus, OnePlus, Nokia, and LeTV).

## Tech Stack

- **Language**: Kotlin 1.9.0 (JVM target 1.8)
- **Platform**: Android (`minSdk = 24`, `compileSdk = 34`, `targetSdk = 34`)
- **Build System**: Gradle 8.2 (Kotlin DSL) & Android Gradle Plugin 8.2.1
- **Libraries**:
  - AndroidX `WorkManager` (`work-runtime-ktx`) for background HTTP dispatch
  - AndroidX `AppCompat`, `Core KTX`, `Lifecycle`, `ConstraintLayout`, `Fragment KTX`, `Preference KTX`
  - Jetpack Compose & Material 3 (configured in Gradle and `ui/theme/`)
  - `Gson` for JSON serialization

## Project Structure

```text
auto-payment-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/hotroso/autopay/
│       │   ├── SettingActivity.kt                        # Launcher UI for permissions & device setup
│       │   ├── common/Constants.kt                       # App constants, excluded packages, OEM intents
│       │   ├── helpers/AutoStartHelper.kt                # Manufacturer-specific auto-start intent helper
│       │   ├── listeners/
│       │   │   ├── NotifyListenerService.kt              # NotificationListenerService -> WorkManager queue
│       │   │   ├── KeepAliveService.kt                   # Foreground keep-alive service
│       │   │   ├── NotificationServiceRestartReceiver.kt # Boot & reconnect BroadcastReceiver
│       │   │   └── NotifyAccessibilityService.kt         # AccessibilityService stub
│       │   ├── models/
│       │   │   ├── XInfo.kt                              # Notification payload model serialized to JSON
│       │   │   └── App.kt                                # App name/package data class
│       │   ├── utils/
│       │   │   ├── NotificationListenerUtil.kt           # Notification access check & settings launcher
│       │   │   └── NotificationUtils.kt                  # Notification title/message extraction & filtering
│       │   ├── workers/
│       │   │   ├── PushingWorker.kt                      # WorkManager Worker sending HTTPS POST JSON
│       │   │   └── HeartbeatWorker.kt                    # WorkManager Worker stub
│       │   └── ui/theme/                                 # Compose Color, Theme, and Type definitions
│       └── res/                                          # Layouts, drawables, strings, and XML configs
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Prerequisites

- **Android Studio** (Hedgehog / Iguana or newer recommended) or Android SDK Command-Line Tools
- **Android SDK Platform 34** (`compileSdk = 34`)
- **JDK 17** (required by Android Gradle Plugin 8.2.1)
- An Android device or emulator running **Android 7.0 (API 24) or higher**

## Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/sinhsac/auto-payment-android.git
   cd auto-payment-android
   ```

2. **Configure your webhook endpoint**

   Update the destination URL in `app/src/main/java/com/hotroso/autopay/workers/PushingWorker.kt` (`httpPost(...)`) to point to your own HTTPS backend endpoint.

   Each captured notification is posted as a JSON object with the following fields from `XInfo`:

   ```json
   {
     "groupKey": "0|com.example.bank|...",
     "postTime": "Nov 27, 2026 10:15:30 AM",
     "strPostTime": "2026-11-27 10:15:30",
     "title": "Transaction Alert",
     "msg": "+100,000 VND ...",
     "pck": "com.example.bank"
   }
   ```

3. **Build and install the debug APK**

   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

   To run unit tests:

   ```bash
   ./gradlew test
   ```

4. **Grant required permissions on the device**

   Launch **Auto Pay** (`SettingActivity`) and configure the four options shown on screen:
   - **Notification Listener Access**: Enables `NotifyListenerService` in Android Settings.
   - **Battery Optimization Exemption**: Requests ignoring battery optimizations so background forwarding is not throttled.
   - **Auto-Start Permission**: Opens OEM auto-start / background startup settings on supported devices.
   - **Lock Screen & Notification Permissions**: Requests `POST_NOTIFICATIONS` and notification policy permissions (Android 13+).

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

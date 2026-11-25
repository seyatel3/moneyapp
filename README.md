<div align="center">

# 💰 MoneyAPP — Персональный финансовый трекер

Native Android app for personal finance tracking. **GMS-free** — works on Huawei Mate X3 (HarmonyOS / AOSP).

Kotlin · Jetpack Compose · Material 3 · Room · Hilt · Vico

[![Version](https://img.shields.io/badge/version-1.11.8-2E7D32?style=for-the-badge)](#releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-03DAC6?style=for-the-badge&logo=jetbrains&logoColor=white)](https://developer.android.com/jetpack/compose)
[![GMS](https://img.shields.io/badge/GMS-free-4CAF50?style=for-the-badge)](#key-design-decisions)
[![License](https://img.shields.io/badge/License-MIT-607D8B?style=for-the-badge)](#license)

> 💜 **Vibe-coded** — приложение написано совместно с ИИ-агентом (вайбкодинг):
> локальный Qwen3.8-27B в сессиях OpenCode, без облака.

</div>

## Features

- 💰 Track income, expenses, and savings
- 📊 Dashboard with KPIs and charts
- 📋 Transactions with filtering and search
- 📝 Monthly budget planning vs actual
- 🎯 Savings goals tracking
- 📈 Analytics and insights
- ☁️ Google Sheets sync (REST API, no GMS)
- 📁 Excel import/export (Apache POI)
- 🔔 Daily reminders (AlarmManager, no FCM)
- 📱 Adaptive UI for foldable devices (Mate X3)

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Architecture:** MVVM + Repository + Clean Architecture
- **Local DB:** Room (SQLite)
- **Sync:** Google Sheets/Drive REST API via Retrofit/OkHttp
- **OAuth2:** WebView flow (no Google Sign-In SDK)
- **Charts:** Vico (Compose-native, GMS-free)
- **DI:** Hilt
- **Navigation:** Navigation Compose
- **Excel:** Apache POI
- **Background:** WorkManager (JobScheduler fallback)
- **Notifications:** AlarmManager + BroadcastReceiver

## Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35 (Android 15)
- Gradle 8.11+

## Setup

### 1. Clone and open

```bash
cd money_app
./gradlew assembleDebug
```

### 2. Google API credentials (optional)

For Google Sheets sync:
1. Create a project in [Google Cloud Console](https://console.cloud.google.com/)
2. Enable **Google Sheets API** and **Google Drive API**
3. Create OAuth 2.0 Client ID (type: Desktop app)
4. Set redirect URI to `moneyapp://oauth2-callback`
5. Enter Client ID and Client Secret in the app Settings

See [GOOGLE_SETUP.md](GOOGLE_SETUP.md) for detailed steps.

### 3. Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Verify no GMS dependencies
./gradlew :app:dependencies | grep gms
# Should return nothing
```

## Project Structure

```
app/src/main/java/ru/moneyapp/
├── MoneyAppApplication.kt          # Hilt application entry
├── data/
│   ├── local/                      # Room database
│   │   ├── dao/                    # DAOs
│   │   ├── entity/                 # Entities
│   │   ├── converters/             # Type converters
│   │   └── mappers/                # Entity ↔ Domain mappers
│   ├── remote/                     # REST API services
│   │   ├── SheetsService.kt
│   │   ├── DriveService.kt
│   │   └── OAuth2Service.kt
│   ├── auth/                       # OAuth2 + token management
│   │   ├── TokenStorage.kt
│   │   ├── OAuth2Models.kt
│   │   └── AuthTokenInterceptor.kt
│   ├── excel/                      # Apache POI import/export
│   │   ├── ExcelImporter.kt
│   │   └── ExcelExporter.kt
│   └── repository/                 # Repository implementations
├── domain/
│   └── model/                      # Domain models
├── di/                             # Hilt modules
├── ui/
│   ├── dashboard/                  # Dashboard screen
│   ├── transactions/               # Transactions screen
│   ├── budget/                     # Budget screen
│   ├── goals/                      # Goals screen
│   ├── analytics/                  # Analytics screen
│   ├── settings/                   # Settings screen
│   ├── auth/                       # OAuth WebView
│   ├── adaptive/                   # Foldable support
│   ├── navigation/                 # Navigation graph
│   └── theme/                      # Theme + colors + typography
├── worker/                         # WorkManager sync jobs
└── notification/                   # AlarmManager notifications
```

## Key Design Decisions

### No GMS
- Zero `com.google.android.gms` dependencies
- No Firebase, no FCM, no Google Sign-In SDK
- Google Sheets sync via raw REST API (Retrofit + OkHttp)
- OAuth2 via embedded WebView
- Push notifications via AlarmManager
- WorkManager uses JobScheduler fallback (works on HarmonyOS)

### Offline-first
- Room is the source of truth
- Everything works without network
- Background sync when connectivity restored
- Conflict resolution: last-write-wins by `updatedAt`

### Foldable support
- Jetpack WindowManager for folding detection
- Two-pane layouts on unfolded Mate X3 (7.85" inner screen)
- Single-pane on folded state (6.4" outer screen)
- Compose adaptive layouts (no SlidingPaneLayout)

## Releases

Подписанные APK для каждой версии публикуются на вкладке **Releases** репозитория.
Берёшь `…-release.apk` и ставишь — всего.

## Установка на другом телефоне

Работает на любом Android 8.0+ (minSdk 26): Huawei/HarmonyOS, стоковые Android,
кастомные ROM — приложение GMS-free, Google-сервисы не нужны. Один APK — все архитектуры.

1. Скачать APK с [Releases](#releases) (последний тег).
2. Открыть файл на телефоне → при запросе разрешить «Установку из неизвестных источников» для браузера/файлового менеджера.
3. Готово.

Нюансы для «чужих» устройств:
- Данные — локально (SQLite), без облака приложение полностью автономно.
- Google Sheets sync опционален: на чужом устройстве вводят **свои** Client ID/Secret (инструкция в [GOOGLE_SETUP.md](GOOGLE_SETUP.md)).
- Передача файла: скачал себе → переслал (Telegram/почта/fileshare).

## License

MIT

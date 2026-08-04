# Finance Tracker — Native Android (Kotlin + Jetpack Compose)

Offline-first personal finance app by eVolva: income, expenses, savings, transfers,
budgets, reports, multi-profile templates, local JSON/CSV backup, and optional
live FX rates. Built with Kotlin, Jetpack Compose, Room, and DataStore.

**Package ID:** `com.financetracker.evolva`  
**Min SDK:** 26 (Android 8.0) · **Target / compile SDK:** 37  
**Version:** see `versionName` in `app/build.gradle.kts` and `AppConstants.APP_VERSION`

## Open / run

1. Install **Android Studio** (current stable) or use the Gradle wrapper from the CLI.
2. Open this repo root and let Gradle sync.
3. Run on an emulator or device with **API 26+**.

CLI (debug APK):

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug   # with a device/emulator attached
```

Release APK (currently signed with the **debug** keystore for sideload beta):

```bash
./gradlew :app:assembleRelease
```

For Play Store / production, replace the release `signingConfig` in
`app/build.gradle.kts` with your upload keystore.

## Toolchain

| Piece | Version (pinned in repo) |
|-------|--------------------------|
| Gradle | 9.6.1 (wrapper) |
| Android Gradle Plugin | 9.3.1 |
| Kotlin / Compose compiler | 2.4.10 |
| Compose BOM | 2025.02.00 |
| JDK | 17 bytecode target (daemon may use JDK 21) |

Create `local.properties` (gitignored) with your SDK path:

```
sdk.dir=/path/to/Android/sdk
```

## What’s in the app

- **Home** — totals, averages, insights, charts  
- **Activity** — transactions (income / expense / savings / transfer), filters, sort, undo  
- **Budget** — category limits, forecast, lockable rows  
- **Report** — monthly breakdown / income vs expense exports  
- **Settings** — profiles & templates, accounts, currency/language/theme, security (password / biometric / auto-lock), **local** JSON/CSV backup  

Google Drive cloud backup code exists but is **gated off** for this beta
(`AppConstants.ENABLE_GOOGLE_DRIVE_BACKUP = false`) until a Google Cloud OAuth
client + SHA-1 are configured. Use local export/import instead.

## Project layout

```
app/src/main/java/com/financetracker/evolva/
  data/
    calc/         Finance / forecast / recurring / insights calculators
    model/        Transactions, budgets, categories, currencies
    db/           Room entities + DAO
    backup/       JSON/CSV backup (shared shape with the web export)
    templates/    Starter profile generators
    prefs/        DataStore settings
  ui/             Compose screens (dashboard, transactions, budget, report, settings)
  MainActivity.kt, FinanceApp.kt
```

## Lint / test

```bash
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
```

Regenerate localized `values-*/strings.xml` from English + `scripts/translations/*.json`:

```bash
python3 scripts/generate_locales.py
```

Missing translation keys fall back to English. Indonesian resources use the
Android folder `values-in` (legacy `in` qualifier); `locales_config.xml` still
lists language tag `id`. Supported in-app languages are listed in
`AppLanguage` / `locales_config.xml` (including Bengali, Zulu, Shan, and Dutch).

## Beta expiry

Builds hard-stop on/after `AppConstants.BETA_EXPIRES_ON` (see About / expired screen).
Bump `versionName`, `versionCode`, and `AppConstants.APP_VERSION` together on each cut.

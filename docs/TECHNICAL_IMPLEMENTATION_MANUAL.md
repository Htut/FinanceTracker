# Finance Tracker — Technical Implementation Manual

**Product:** Finance Tracker (native Android)  
**Package / applicationId:** `com.financetracker.evolva`  
**Version:** `1.0.0-beta` (`versionName` / `AppConstants.APP_VERSION`)  
**versionCode:** `1`  
**Repo layout:** single Gradle module `:app`

This document is for engineers maintaining or extending the app. End-user steps
belong in [USER_MANUAL.md](USER_MANUAL.md). High-level setup also appears in the
root [README.md](../README.md).

---

## 1. Goals and constraints

- Offline-first personal finance; **no required backend**  
- Numbers and backup JSON intended to stay compatible with the original web app  
- Manual DI (no Hilt); Compose UI; Room + DataStore persistence  
- Google Drive / Firebase / server sync are **deferred** (see §12)

---

## 2. Toolchain

| Piece | Version (pinned) | Where |
|-------|------------------|--------|
| Gradle | 9.6.1 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 9.3.1 | root `build.gradle.kts` |
| Kotlin / Compose compiler | 2.4.10 | root `build.gradle.kts` |
| KSP | 2.3.10 | root `build.gradle.kts` |
| Compose BOM | 2025.02.00 | `app/build.gradle.kts` |
| Room | 2.8.4 | `app/build.gradle.kts` |
| minSdk / compileSdk / targetSdk | 26 / 37 / 37 | `app/build.gradle.kts` |
| JVM target | 17 | `app/build.gradle.kts` |

Create gitignored `local.properties`:

```
sdk.dir=/path/to/Android/sdk
```

---

## 3. Build, run, lint, test

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:assembleRelease    # minify + shrink; signed with debug keystore (beta)
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest  # currently NO-SOURCE (no unit tests checked in)
python3 scripts/generate_locales.py
```

Artifacts:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`  
- Release APK: `app/build/outputs/apk/release/app-release.apk`

**Release signing:** `release` uses `signingConfigs.debug` for sideload beta.
Replace with an upload keystore before Play Console production.

---

## 4. Architecture overview

```
FinanceApp (Application)
  ├─ SettingsDataStore          # app-wide: language, theme, password, profile registry
  └─ ActiveProfileSession        # active profile → Room DB + ProfileSettingsStore + FinanceRepository

MainActivity
  └─ MainViewModel (shared across tabs)
       └─ AppNavGraph → Home | Activity | Budget | Report | Settings
            (+ AppLockScreen / BetaExpiredScreen gates)
```

- **Manual composition root:** `FinanceApp.kt`  
- **UI state:** `ui/MainViewModel.kt` + `MainViewModelFactory`  
- **Domain persistence:** `data/repository/FinanceRepository.kt` over Room  
- **Pure calculators:** `data/calc/*` (ported from web JS)  
- **UI:** Jetpack Compose + Material 3 under `ui/`

There is no Hilt/Dagger and no Retrofit service layer beyond a simple FX HTTP call.

---

## 5. Package map

```
app/src/main/java/com/financetracker/evolva/
  FinanceApp.kt
  MainActivity.kt
  data/
    AppConstants.kt
    model/          # Transaction, Budget, Account, currencies, filters, AppLanguage
    db/             # Room entities, DAO, FinanceDatabase (+ migrations)
    repository/     # FinanceRepository
    prefs/          # SettingsDataStore (app-wide)
    profile/        # ActiveProfileSession, per-profile DB + settings
    backup/         # BackupPayload, BackupManager, DriveBackupClient (gated)
    calc/           # FinanceCalculator, ForecastCalculator, RecurringEngine, InsightsCalculator
    templates/      # Sample profile generators
    rates/          # ExchangeRateFetcher (Frankfurter)
    security/       # PasswordHasher, BiometricAuth, DeviceCredentialAuth
    locale/         # LocaleHelper, CategoryLabels, AccountLabels
    receipt/        # ReceiptStore (filesDir/receipts)
    export/         # DetailExport (text/CSV/PDF share)
    notify/         # BudgetAlertNotifier
  ui/
    navigation/     # NavGraph, bottom bar
    dashboard/      # Home
    transactions/   # Activity list + add/edit sheet
    budget/
    report/
    settings/
    accounts/
    lock/           # App lock + beta expired
    components/
    theme/
    widget/         # Glance BudgetGlanceWidget + WidgetUpdater
```

Resources: `app/src/main/res/` (`values`, `values-*`, `xml/locales_config.xml`, …).

---

## 6. Constants and feature flags

`data/AppConstants.kt`:

| Constant | Purpose |
|----------|---------|
| `APP_VERSION` | About label; keep equal to `versionName` |
| `BETA_EXPIRES_ON` | `"2027-07-31"`; `isBetaExpired()` gates UI |
| `ENABLE_GOOGLE_DRIVE_BACKUP` | `false` — hides Drive UI and no-ops Drive APIs |
| `DEFAULT_FILTER_AUTO_CLOSE_SECONDS` | `7` |

When enabling Drive later: set the flag to `true`, add Google Cloud Android OAuth
client for `com.financetracker.evolva` (SHA-1), enable Drive API + `drive.appdata`.

---

## 7. Persistence

### 7.1 App-wide DataStore (`SettingsDataStore`)

Language, theme, app password hash, biometric toggle, view-only, profile list /
active profile id.

Password hashing: unsalted SHA-256 hex (`PasswordHasher`) — suitable for app-lock
UX, not a cryptographic vault.

### 7.2 Per-profile Room DB

`FinanceDatabase` version **5**, `exportSchema = false`.

Tables: `accounts`, `transactions`, `budgets`, `recurring_rules`.

Migration highlights:

| From → To | Change |
|-----------|--------|
| 1 → 2 | `transactions.time` |
| 2 → 3 | `accounts` table; tx `accountId`, `receiptUri`, `currencyCode`, `exchangeRate` |
| 3 → 4 | `transactions.locked` |
| 4 → 5 | `budgets.locked` |

Each profile has its own DB file via `ProfileDatabaseProvider`. Legacy single-DB
users are migrated on first init.

### 7.3 Per-profile preferences (`ProfileSettingsStore`)

Home currency, exchange rates map, custom expense categories, filter auto-close,
budget alerts enabled, auto-lock rule.

---

## 8. Domain model (summary)

**Transaction types:** `INCOME`, `EXPENSE`, `SAVINGS`, `TRANSFER`  
**Transfer direction:** `OUT` / `IN`  
**Accounts:** kinds `CASH`, `BANK`, `EWALLET` (defaults: Cash, Bank, E-Wallet)  
**Date filter:** All / Monthly / Yearly / Range (`DateFilter`)  
**Sort:** date/amount asc/desc (`TransactionSort`)  
**Auto-lock rule:** Off / 7 / 30 / 90 days / Previous months (`AutoLockRule`)  
**Currencies:** `AppCurrency` (~30 ISO codes)  
**Languages:** `AppLanguage` (25 tags) — see user manual §11

Category **storage keys** stay English; UI labels come from string resources via
`CategoryLabels`.

---

## 9. Calculators

| Class | Responsibility | Path |
|-------|----------------|------|
| `FinanceCalculator` | Totals, averages, category spend, budget state, account balance | `data/calc/FinanceCalculator.kt` |
| `ForecastCalculator` | History + projected months; first negative month | `ForecastCalculator.kt` |
| `RecurringEngine` | Backfill active rules through today (no future posting) | `RecurringEngine.kt` |
| `InsightsCalculator` | Localized insight strings for Home | `InsightsCalculator.kt` |

Budget visual states: NONE / OK / WARN (≥ 80%) / OVER.

Keep calculator changes behavior-compatible with the web app when possible.

---

## 10. Backup format

Implemented in `data/backup/BackupModels.kt` + `BackupManager.kt`.

JSON **BackupPayload** (version **2**), conceptually:

```json
{
  "app": "finance-tracker-android",
  "version": 2,
  "exportedAt": "...",
  "currency": "USD",
  "transactions": [ /* TransactionDto */ ],
  "budgets": [ /* BudgetDto */ ],
  "recurring": [ /* RecurringRuleDto */ ],
  "customExpenseCategories": [ "..." ],
  "accounts": [ /* AccountDto */ ],
  "exchangeRates": { "EUR": 0.92 }
}
```

- Unknown JSON fields are ignored (forward compatible with web exports).  
- CSV export/import covers **transactions only** (fixed column set in `BackupManager`).  
- Import merge vs replace is handled in `MainViewModel.importBackup` / `importCsv`.  
- Scope is always the **active profile**.

---

## 11. UI / navigation

- Bottom bar + `NavHost`: dashboard, transactions, budget, report, settings  
- Shared `MainViewModel` instance across destinations  
- Gates in `MainActivity`:
  1. Beta expired → `BetaExpiredScreen`  
  2. Locked session → `AppLockScreen`  
  3. Else → main nav  

Settings uses internal sub-tabs: General, Data, Backup, Security, About.

Glance widget: `BudgetGlanceWidget` + `refreshBudgetWidgets()` after mutations.

Themes: `AppThemeOption` (Classic, Forest, Ocean, Midnight, Amber, Slate).

---

## 12. Networking and deferred cloud

| Capability | Status |
|------------|--------|
| Frankfurter FX (`ExchangeRateFetcher`) | Optional; needs `INTERNET` |
| Local JSON/CSV backup | Supported |
| Google Drive appdata client | **Implemented but gated off** (`ENABLE_GOOGLE_DRIVE_BACKUP`) |
| Firebase / server sync | **Not implemented; deferred** |

Workspace rule `.cursor/rules/deferred-features.mdc`: do not implement Drive OAuth
sync or Firebase until explicitly requested. Prefer `drive.appdata` + manual
backup/restore first when re-enabled.

---

## 13. Localization pipeline

1. Edit English strings in `app/src/main/res/values/strings.xml`.  
2. Update `scripts/translations/<locale>.json` (must cover all English keys).  
3. Run `python3 scripts/generate_locales.py` → writes `values-*/strings.xml`.  
4. Register new languages in:
   - `data/model/AppLanguage.kt`  
   - `res/xml/locales_config.xml`  
   - `LOCALES` in `scripts/generate_locales.py`  

Notes:

- Indonesian resources use folder **`values-in`** (Android legacy qualifier); BCP-47
  tag remains `id`. Mapping is in `RESOURCE_FOLDER` inside `generate_locales.py`.  
- Missing keys in a JSON file fall back to English at generation time (warning printed).  
- Apply runtime locale via `LocaleHelper` / per-app locales.

---

## 14. Security implementation notes

| Concern | Behavior |
|---------|----------|
| App password | Hash in DataStore; verified on unlock |
| Session lock | `MainViewModel.lockSession()` on `MainActivity.onStop` (skipped for config change) |
| Biometric | `BiometricAuth` when enabled + password exists |
| Device credential | Fallback intent path (`DeviceCredentialAuth`) |
| View only | Checked in ViewModel mutation methods |
| Row locks | `transactions.locked` / `budgets.locked`; auto-lock applies rules on load |
| `allowBackup` | `false` in manifest — avoid system cloud backup of finance DB |

---

## 15. Permissions (`AndroidManifest.xml`)

- `INTERNET` — FX fetch  
- `POST_NOTIFICATIONS` — budget alerts  
- `USE_BIOMETRIC` — biometric unlock  
- FileProvider for share exports; `PickVisualMedia` for receipts  

---

## 16. Testing guidance

Current repo has **no** `src/test` / `src/androidTest` sources. Highest value first
tests:

1. `FinanceCalculator` totals / budgetState edge cases  
2. `ForecastCalculator` horizon math  
3. `RecurringEngine` backfill boundaries  
4. `BackupManager` JSON round-trip + CSV parse  

Manual smoke (sideload):

1. Add expense → appears on Home/Activity  
2. Set budget → progress + optional notification  
3. Export JSON → Import merge on another profile  
4. Enable password → kill app → unlock  
5. Switch language → UI strings + suggested currency  

---

## 17. Extending the app (checklist)

**New setting**

1. Persist in `SettingsDataStore` or `ProfileSettingsStore`  
2. Expose `StateFlow` + mutator on `MainViewModel`  
3. Bind UI in the right Settings tab  
4. Add strings + regenerate locales  

**New transaction field**

1. Room entity + migration bump  
2. DTO in `BackupModels` (+ version bump if breaking)  
3. UI sheet + list formatting  
4. Calculator impact review  

**New language**

1. Add `AppLanguage` entry (+ suggested `AppCurrency`)  
2. Add `locales_config` locale  
3. Add `scripts/translations/<tag>.json` with all keys  
4. Append tag to `generate_locales.py` `LOCALES`  
5. Run generator; build; smoke language picker  

**Enable Drive**

1. Provision OAuth Android client + SHA-1  
2. Set `ENABLE_GOOGLE_DRIVE_BACKUP = true`  
3. Verify Settings → Backup Drive section end-to-end  

---

## 18. Related docs

| Doc | Audience |
|-----|----------|
| [USER_MANUAL.md](USER_MANUAL.md) | End users / QA |
| [../README.md](../README.md) | Quick start / toolchain |
| [../AGENTS.md](../AGENTS.md) | Cursor Cloud agent notes (if present) |
| `.cursor/rules/deferred-features.mdc` | Cloud feature policy |

---

## 19. Versioning reminder

On each release cut, bump together:

1. `versionName` / `versionCode` in `app/build.gradle.kts`  
2. `AppConstants.APP_VERSION`  
3. Optionally `BETA_EXPIRES_ON` for a new beta window  

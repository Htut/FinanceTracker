# Finance Tracker — User Manual

**App:** Finance Tracker (eVolva Intelligent Solution)  
**Version covered:** `1.0.0-beta`  
**Platform:** Android 8.0+ (API 26)  
**Package:** `com.financetracker.evolva`

This guide explains how to use Finance Tracker day to day. Everything stays on your
device unless you export a backup file yourself.

---

## 1. What the app does

Finance Tracker is an offline-first personal finance app. You can:

- Record **income**, **expense**, **savings**, and **transfer** transactions  
- Set **monthly category budgets** and see forecasts  
- Review **Home** totals, insights, and charts  
- Generate **monthly reports** and share them as text, CSV, or PDF  
- Use **multiple profiles** (personal + sample templates)  
- Protect the app with a **password** and optional **biometric** unlock  
- Back up and restore with **local JSON / CSV** files  

Google Drive cloud backup is not enabled in this beta. Use local export/import.

**Beta note:** This build stops working on/after **31 July 2027**. Update to a newer
build before then.

---

## 2. First launch

1. Install the APK (or run from Android Studio) on a phone/emulator with Android 8.0+.  
2. Open **Finance Tracker**.  
3. You land on **Home** with an empty personal profile named **My Tracker**.  
4. Optional: open **Settings → Security** and set an app password.  
5. Optional: open **Settings → Data → Sample templates** to create a practice profile
   with sample transactions.

Default wallets are created for you: **Cash**, **Bank**, and **E-Wallet**.

---

## 3. Bottom navigation

| Tab | Purpose |
|-----|---------|
| **Home** | Totals, averages, insights, charts |
| **Activity** | Add / edit / find transactions |
| **Budget** | Category limits and cash-flow forecast |
| **Report** | Month summary and shareable reports |
| **Settings** | Language, currency, profiles, backup, security |

A shared **date filter** (All / Monthly / Yearly / Custom range) appears on Home,
Activity, and Budget. It can auto-collapse after a few seconds (configurable in
Settings → General).

---

## 4. Home

Home shows money at a glance for the current filter:

- Income, expense, savings, transfer net, and overall net  
- Monthly averages where enough history exists  
- Text **insights** (saving rate, top categories, month-over-month, budget status)  
- Charts: expenses by category, income vs expense, savings growth  

Use the chart controls to switch chart types and short presets (for example last
3 or 6 months). You can share a detail summary from Home as text, CSV, or PDF.

---

## 5. Activity (transactions)

### 5.1 Add a transaction

1. Open **Activity**.  
2. Tap **+**.  
3. Choose type: **Income**, **Expense**, **Savings**, or **Transfer**.  
4. For transfers, choose **Sent** or **Received**.  
5. Pick category (or relation for transfers), account, amount, and date.  
6. Optional: time, note, receipt photo, different currency + exchange rate,
   **Repeat monthly**.  
7. Save.

### 5.2 Find and sort

- Search by category, note, or type  
- Filter by type, category, min/max amount  
- Sort by date or amount (ascending / descending)

### 5.3 Edit, delete, undo, lock

- Tap a row to edit (unless it is locked or **View only** is on).  
- Delete from the row actions; a snackbar lets you **Undo**.  
- Use the lock icon to lock a transaction so it cannot be edited or deleted until
  unlocked.

### 5.4 Recurring (“Repeat monthly”)

When you enable **Repeat monthly** on save, the app creates a recurring rule and
backfills months through today on later opens. Manage rules under
**Settings → Data**.

---

## 6. Budget

1. Open **Budget**.  
2. Set a monthly limit per expense category.  
3. Watch progress: healthy, near limit (~80%+), or over budget.  
4. Lock a budget row if you want to prevent accidental edits.  
5. Scroll to the **cash-flow forecast** and choose a 3 / 6 / 12 month horizon.

Optional **budget alerts** (Settings → General) can notify you when a category is
near or over its limit. Android may ask for notification permission.

---

## 7. Report

1. Open **Report**.  
2. Pick a month.  
3. Review income, expense, savings, and transfer net.  
4. Share or export as **text**, **CSV**, or **PDF**.

---

## 8. Settings

### 8.1 General

- **Language** — 25 languages (see §11). Changing language also suggests a regional
  home currency; you can change currency afterward.  
- **Currency** — home currency for the active profile.  
- **Exchange rates** — store rates manually or **fetch live rates** (needs internet).  
- **Budget alerts** — on/off.  
- **Date filter auto-hide** — Off / 5 / 7 / 10 / 15 / 30 seconds.  
- **Theme** — Classic, Forest, Ocean, Midnight, Amber, Slate.

### 8.2 Data

- **Accounts / wallets** — rename, opening balance, archive.  
- **Custom expense types** — add your own expense categories.  
- **Profiles & templates** — switch profile, set up a sample template, delete a
  template profile (My Tracker cannot be deleted).  
- **Recurring rules** — pause, edit, or delete.

Sample templates:

- Student expenses  
- Working professional  
- Budget example (Jan–Jul 2026)  
- Professional (Jan 2025–Jul 2026)

Each template becomes its **own profile** with its own data.

### 8.3 Backup (local files)

Backups apply to the **active profile only**.

| Action | What it does |
|--------|----------------|
| Export JSON | Full backup (transactions, budgets, recurring, accounts, rates, …) |
| Export CSV | Transactions only |
| Import JSON & Merge | Adds / merges without wiping existing rows |
| Import JSON & Replace | Replaces profile data with the file |
| Import CSV Merge / Replace | Transactions only |

Share exported files through Android’s share sheet (email, Drive app, Files, etc.).
Keep a copy somewhere safe.

### 8.4 Security

- **App password** — required on open and when returning to the app.  
- **Biometric unlock** — fingerprint / face (requires a password first, and a
  biometric enrolled on the device).  
- **View only** — blocks adding, editing, and deleting transactions.  
- **Auto-lock records** — automatically locks older Activity rows
  (Off / 7 / 30 / 90 days / Previous months).  
- **Danger zone** — clear all transactions in the active profile (password + device
  confirmation).

### 8.5 About

Shows version, beta expiry, feature summary, and exit.

---

## 9. Home screen widget

Add the Finance Tracker widget from your launcher. It shows **this month’s spend**
and **budget remaining**. It refreshes when data changes and on the system’s
periodic update schedule.

---

## 10. Permissions

| Permission | Why |
|------------|-----|
| Internet | Optional live exchange rates |
| Notifications | Optional budget alerts |
| Biometric | Optional fingerprint / face unlock |

Receipt photos use the system photo picker (no permanent gallery permission).

---

## 11. Languages (25)

| Code | Language |
|------|----------|
| en | English |
| ms | Malay (Bahasa Melayu) |
| my | Myanmar (မြန်မာ) |
| id | Indonesian (Bahasa Indonesia) |
| ta | Tamil (தமிழ்) |
| zh-CN | Chinese (中文) |
| ja | Japanese (日本語) |
| ko | Korean (한국어) |
| ru | Russian (Русский) |
| th | Thai (ไทย) |
| es | Spanish (Español) |
| fr | French (Français) |
| it | Italian (Italiano) |
| vi | Vietnamese (Tiếng Việt) |
| tr | Turkish (Türkçe) |
| fa | Persian (فارسی) |
| de | German (Deutsch) |
| ar | Arabic (العربية) |
| ur | Urdu (اردو) |
| hi | Hindi (हिन्दी) |
| bn | Bengali (বাংলা) |
| zu | Zulu (isiZulu) |
| shn | Shan (လိၵ်ႈတႆး) |
| nl | Dutch (Nederlands) |
| pt | Portuguese (Português) |

---

## 12. Tips

1. Export a JSON backup before large imports or clearing data.  
2. Use a **template profile** to learn the UI without touching My Tracker.  
3. Turn on **View only** when handing the phone to someone else.  
4. Lock important historical rows, or enable **Auto-lock records**.  
5. Set the home currency before entering a long history so totals stay consistent.

---

## 13. Troubleshooting

| Problem | What to try |
|---------|-------------|
| App asks for password every time you leave | Expected if a password is set; unlock with password or biometric |
| Cannot edit a transaction | Check lock icon and View only mode |
| Live rates fail | Need internet; you can enter rates manually |
| Import says invalid | Use a file exported by this app (or compatible web JSON/CSV) |
| Beta expired screen | Install a newer build; this beta ends 31 July 2027 |
| Wrong language | Settings → General → Language |

---

## 14. Support / credits

Product by **Thein Htut / eVolva Intelligent Solution**.  
For development and build instructions, see
[Technical Implementation Manual](TECHNICAL_IMPLEMENTATION_MANUAL.md).

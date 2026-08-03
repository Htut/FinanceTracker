# -*- coding: utf-8 -*-
"""Add About feature strings to all locale JSON files, then regenerate XML."""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
TRANSLATIONS = ROOT / "translations"

EN = {
    "about_tagline": "Track income, spending, budgets, and savings across profiles — offline on your device.",
    "section_app_features": "What you can do",
    "feature_profiles_title": "Profiles & sample templates",
    "feature_profiles_desc": "Keep a personal tracker plus optional sample profiles you can set up, switch, or delete.",
    "feature_currency_title": "Multi-currency & live rates",
    "feature_currency_desc": "Record in 30 currencies with linked forward/inverse rates and optional live FX fetch.",
    "feature_wallets_title": "Wallets & accounts",
    "feature_wallets_desc": "Track cash, bank, and e-wallet balances in one place.",
    "feature_budgets_title": "Budgets & alerts",
    "feature_budgets_desc": "Set category limits and get notified when you are near or over budget.",
    "feature_transactions_title": "Transactions & receipts",
    "feature_transactions_desc": "Add notes, filters, optional receipt photos, and undo after deletes.",
    "feature_recurring_title": "Recurring rules",
    "feature_recurring_desc": "Auto-post monthly income or bills without re-entering them.",
    "feature_reports_title": "Reports & sharing",
    "feature_reports_desc": "Monthly summaries with Text, CSV, or PDF export and share.",
    "feature_security_title": "App lock",
    "feature_security_desc": "Protect the app with a password and device lock when you leave.",
    "feature_widget_title": "Home screen widget",
    "feature_widget_desc": "See this month's spend and budget remaining at a glance.",
    "feature_languages_title": "Languages & themes",
    "feature_languages_desc": "Use the app in 20 languages with multiple visual themes.",
}

LOCALIZED = {
    "ms": {
        "about_tagline": "Jejaki pendapatan, perbelanjaan, belanjawan dan simpanan merentas profil — luar talian pada peranti anda.",
        "section_app_features": "Apa yang anda boleh lakukan",
        "feature_profiles_title": "Profil & templat sampel",
        "feature_profiles_desc": "Simpan penjejak peribadi serta profil sampel pilihan yang boleh disediakan, ditukar atau dipadam.",
        "feature_currency_title": "Berbilang mata wang & kadar langsung",
        "feature_currency_desc": "Rekod dalam 30 mata wang dengan kadar maju/songsang yang dikaitkan dan pilihan tarik FX langsung.",
        "feature_wallets_title": "Dompet & akaun",
        "feature_wallets_desc": "Jejaki baki tunai, bank dan e-dompet di satu tempat.",
        "feature_budgets_title": "Belanjawan & amaran",
        "feature_budgets_desc": "Tetapkan had kategori dan dapat notifikasi bila hampir atau melebihi belanjawan.",
        "feature_transactions_title": "Transaksi & resit",
        "feature_transactions_desc": "Tambah nota, penapis, foto resit pilihan, dan buat asal selepas padam.",
        "feature_recurring_title": "Peraturan berulang",
        "feature_recurring_desc": "Jana pendapatan atau bil bulanan secara automatik tanpa masuk semula.",
        "feature_reports_title": "Laporan & kongsi",
        "feature_reports_desc": "Ringkasan bulanan dengan eksport dan kongsi Teks, CSV atau PDF.",
        "feature_security_title": "Kunci aplikasi",
        "feature_security_desc": "Lindungi aplikasi dengan kata laluan dan kunci peranti bila anda keluar.",
        "feature_widget_title": "Widget skrin utama",
        "feature_widget_desc": "Lihat perbelanjaan bulan ini dan baki belanjawan dengan pantas.",
        "feature_languages_title": "Bahasa & tema",
        "feature_languages_desc": "Guna aplikasi dalam 20 bahasa dengan pelbagai tema visual.",
    },
    "id": {
        "about_tagline": "Lacak pemasukan, pengeluaran, anggaran, dan tabungan lintas profil — offline di perangkat Anda.",
        "section_app_features": "Yang bisa Anda lakukan",
        "feature_profiles_title": "Profil & template contoh",
        "feature_profiles_desc": "Simpan tracker pribadi plus profil contoh opsional yang bisa disiapkan, diganti, atau dihapus.",
        "feature_currency_title": "Multi-mata uang & kurs langsung",
        "feature_currency_desc": "Catat dalam 30 mata uang dengan kurs maju/terbalik yang terhubung dan opsi ambil FX langsung.",
        "feature_wallets_title": "Dompet & akun",
        "feature_wallets_desc": "Lacak saldo tunai, bank, dan e-wallet di satu tempat.",
        "feature_budgets_title": "Anggaran & peringatan",
        "feature_budgets_desc": "Atur batas kategori dan dapatkan notifikasi saat mendekati atau melebihi anggaran.",
        "feature_transactions_title": "Transaksi & kwitansi",
        "feature_transactions_desc": "Tambah catatan, filter, foto kwitansi opsional, dan batalkan setelah hapus.",
        "feature_recurring_title": "Aturan berulang",
        "feature_recurring_desc": "Otomatis catat pemasukan atau tagihan bulanan tanpa memasukkan ulang.",
        "feature_reports_title": "Laporan & berbagi",
        "feature_reports_desc": "Ringkasan bulanan dengan ekspor dan berbagi Teks, CSV, atau PDF.",
        "feature_security_title": "Kunci aplikasi",
        "feature_security_desc": "Lindungi aplikasi dengan kata sandi dan kunci perangkat saat Anda keluar.",
        "feature_widget_title": "Widget layar utama",
        "feature_widget_desc": "Lihat pengeluaran bulan ini dan sisa anggaran sekilas.",
        "feature_languages_title": "Bahasa & tema",
        "feature_languages_desc": "Gunakan aplikasi dalam 20 bahasa dengan berbagai tema visual.",
    },
    "zh-rCN": {
        "about_tagline": "跨档案追踪收入、支出、预算与储蓄——离线保存在您的设备上。",
        "section_app_features": "你可以做什么",
        "feature_profiles_title": "档案与示例模板",
        "feature_profiles_desc": "保留个人账本，并可设置、切换或删除可选示例档案。",
        "feature_currency_title": "多币种与实时汇率",
        "feature_currency_desc": "支持 30 种货币记账，正反向汇率联动，并可拉取实时汇率。",
        "feature_wallets_title": "钱包与账户",
        "feature_wallets_desc": "在一处跟踪现金、银行与电子钱包余额。",
        "feature_budgets_title": "预算与提醒",
        "feature_budgets_desc": "设置类别限额，接近或超预算时收到通知。",
        "feature_transactions_title": "交易与收据",
        "feature_transactions_desc": "支持备注、筛选、可选收据照片，以及删除后撤销。",
        "feature_recurring_title": "定期规则",
        "feature_recurring_desc": "每月自动入账收入或账单，无需重复输入。",
        "feature_reports_title": "报表与分享",
        "feature_reports_desc": "月度汇总，支持文本、CSV、PDF 导出与分享。",
        "feature_security_title": "应用锁",
        "feature_security_desc": "用密码与设备锁屏保护应用，离开时自动上锁。",
        "feature_widget_title": "主屏幕小组件",
        "feature_widget_desc": "一眼查看本月支出与剩余预算。",
        "feature_languages_title": "语言与主题",
        "feature_languages_desc": "支持 20 种语言与多种视觉主题。",
    },
    "my": {
        "about_tagline": "ပရိုဖိုင်များအနှံ့ ဝင်ငွေ၊ အသုံးစရိတ်၊ ဘတ်ဂျက်နှင့် စုဆောင်းငွေကို ခြေရာခံပါ — သင့်စက်တွင် အော့ဖ်လိုင်း။",
        "section_app_features": "သင်လုပ်နိုင်သည်များ",
        "feature_profiles_title": "ပရိုဖိုင်နှင့် နမူနာပုံစံများ",
        "feature_profiles_desc": "ကိုယ်ပိုင်ခြေရာခံမှုနှင့် တည်ဆောက်၊ ပြောင်း၊ ဖျက်နိုင်သော နမူနာပရိုဖိုင်များ။",
        "feature_currency_title": "ငွေကြေးမျိုးစုံနှင့် တိုက်ရိုက်နှုန်း",
        "feature_currency_desc": "ငွေကြေး ၃၀ ဖြင့် မှတ်တမ်းတင်ပြီး ရှေ့/ပြန်လှန်နှုန်းများ ချိတ်ဆက်ကာ တိုက်ရိုက် FX ဆွဲနိုင်သည်။",
        "feature_wallets_title": "ပိုက်ဆံအိတ်နှင့် အကောင့်များ",
        "feature_wallets_desc": "ငွေသား၊ ဘဏ်နှင့် အီး-ပိုက်ဆံအိတ် လက်ကျန်များကို တစ်နေရာတည်းတွင် ကြည့်ရှုပါ။",
        "feature_budgets_title": "ဘတ်ဂျက်နှင့် သတိပေးချက်များ",
        "feature_budgets_desc": "အမျိုးအစားကန့်သတ်များ သတ်မှတ်ပြီး နီးကပ်/ကျော်လွန်သည့်အခါ အကြောင်းကြားချက်ရယူပါ။",
        "feature_transactions_title": "ငွေစာရင်းနှင့် ပြေစာများ",
        "feature_transactions_desc": "မှတ်ချက်၊ စစ်ထုတ်မှု၊ ပြေစာဓာတ်ပုံနှင့် ဖျက်ပြီးနောက် ပြန်ဖျက်သိမ်းနိုင်သည်။",
        "feature_recurring_title": "ထပ်တလဲလဲ စည်းမျဉ်းများ",
        "feature_recurring_desc": "လစဉ် ဝင်ငွေ/ဘေလ်များကို ပြန်မထည့်ဘဲ အလိုအလျောက် မှတ်တမ်းတင်ပါ။",
        "feature_reports_title": "အစီရင်ခံစာနှင့် မျှဝေခြင်း",
        "feature_reports_desc": "လစဉ် အနှစ်ချုပ်ကို စာသား၊ CSV သို့မဟုတ် PDF ဖြင့် ထုတ်ယူ/မျှဝေပါ။",
        "feature_security_title": "အက်ပ်သော့",
        "feature_security_desc": "စကားဝှက်နှင့် စက်သော့ဖြင့် ကာကွယ်ပြီး ထွက်သည့်အခါ သော့ခတ်ပါ။",
        "feature_widget_title": "ပင်မမျက်နှာပြင် ဝစ်ဂျက်",
        "feature_widget_desc": "ဤလ အသုံးစရိတ်နှင့် ကျန်ဘတ်ဂျက်ကို တစ်ချက်ကြည့်ပါ။",
        "feature_languages_title": "ဘာသာစကားနှင့် အပြင်အဆင်များ",
        "feature_languages_desc": "ဘာသာစကား ၂၀ နှင့် အမြင်အပြင်အဆင်များစွာဖြင့် အသုံးပြုနိုင်သည်။",
    },
}


def main() -> None:
    for path in sorted(TRANSLATIONS.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        updates = {**EN, **LOCALIZED.get(path.stem, {})}
        data.update(updates)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(path.stem)
    subprocess.check_call([sys.executable, str(ROOT / "generate_locales.py")])


if __name__ == "__main__":
    main()

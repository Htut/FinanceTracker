# -*- coding: utf-8 -*-
"""Rewrite Templates.kt to resolve note strings via Context at generation time.

Also adds tpl_note_* keys to English strings.xml and translation JSONs
(translated for major locales; English fallback elsewhere via generate_locales).
"""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEMPLATES_KT = ROOT / "app/src/main/java/com/financetracker/evolva/data/templates/Templates.kt"
ENGLISH_XML = ROOT / "app/src/main/res/values/strings.xml"
TRANSLATIONS = ROOT / "scripts/translations"
ACTIVE_SESSION = ROOT / "app/src/main/java/com/financetracker/evolva/data/profile/ActiveProfileSession.kt"

# Unique English notes found in template profiles (stable order for resource ids).
NOTES: list[str] = [
    "Part-time job — campus café",
    "Allowance from parents",
    "Scholarship disbursement",
    "Tutoring side gig",
    "Dorm rent",
    "Phone bill",
    "Groceries",
    "Meal plan top-up",
    "Campus cafeteria",
    "Coffee run",
    "Late-night snack",
    "Bus pass top-up",
    "Ride-hailing",
    "Bike rental",
    "Train ticket",
    "Movie night",
    "Concert ticket",
    "Karaoke with friends",
    "Board game café",
    "Streaming subscription",
    "Music subscription",
    "Stationery",
    "Phone case",
    "Dorm supplies",
    "New clothes",
    "Sneakers",
    "Winter jacket",
    "Pharmacy run",
    "Clinic visit",
    "Vitamins",
    "Haircut",
    "Toiletries",
    "Textbooks & course materials",
    "Printing & supplies",
    "Exam fee",
    "Tuition installment",
    "Weekend trip home",
    "Birthday gift for a friend",
    "Laptop fund",
    "Emergency cushion",
    "Monthly salary",
    "Dividend payout",
    "Weekend consulting gig",
    "Year-end bonus",
    "Electricity & water",
    "Internet & phone",
    "Health insurance premium",
    "Dining out",
    "Work lunch",
    "Weekend brunch",
    "Fuel",
    "Parking",
    "Toll",
    "Bowling night",
    "Karaoke",
    "Cloud storage",
    "Gym app",
    "Home essentials",
    "Electronics accessory",
    "Kitchenware",
    "New outfit",
    "Shoes",
    "Work attire",
    "Pharmacy",
    "Dental checkup",
    "Skincare",
    "Spa",
    "Weekend getaway",
    "Birthday gift / charity donation",
    "EPF / retirement contribution",
    "Emergency fund",
    "Rental income",
    "Performance bonus",
    "Electricity, water & internet",
    "Home & health insurance",
    "Market run",
    "Family dinner",
    "Snacks",
    "Public transit",
    "Family outing",
    "Weekend activity",
    "Streaming rental",
    "Household items",
    "Clothing basics",
    "Home decor",
    "Streaming service",
    "Family trip",
    "Course / certification fee",
    "Emergency fund contribution",
    "Retirement contribution",
]

# Lightweight translations for high-priority locales. Others keep English.
NOTE_I18N: dict[str, dict[str, str]] = {
    "ms": {
        "Part-time job — campus café": "Kerja sambilan — kafe kampus",
        "Allowance from parents": "Elaun daripada ibu bapa",
        "Scholarship disbursement": "Bayaran biasiswa",
        "Tutoring side gig": "Kerja sampingan tutor",
        "Dorm rent": "Sewa asrama",
        "Phone bill": "Bil telefon",
        "Groceries": "Barangan runcit",
        "Meal plan top-up": "Tambah nilai pelan makanan",
        "Campus cafeteria": "Kafeteria kampus",
        "Coffee run": "Kopi",
        "Late-night snack": "Snek lewat malam",
        "Bus pass top-up": "Tambah nilai pas bas",
        "Ride-hailing": "E-hailing",
        "Bike rental": "Sewa basikal",
        "Train ticket": "Tiket kereta api",
        "Movie night": "Malam wayang",
        "Concert ticket": "Tiket konsert",
        "Karaoke with friends": "Karaoke dengan kawan",
        "Board game café": "Kafe permainan papan",
        "Streaming subscription": "Langganan streaming",
        "Music subscription": "Langganan muzik",
        "Stationery": "Alatan tulis",
        "Phone case": "Sarung telefon",
        "Dorm supplies": "Keperluan asrama",
        "New clothes": "Pakaian baharu",
        "Sneakers": "Kasut sukan",
        "Winter jacket": "Jaket musim sejuk",
        "Pharmacy run": "Farmasi",
        "Clinic visit": "Lawatan klinik",
        "Vitamins": "Vitamin",
        "Haircut": "Cukur rambut",
        "Toiletries": "Barangan mandian",
        "Textbooks & course materials": "Buku teks & bahan kursus",
        "Printing & supplies": "Cetakan & bekalan",
        "Exam fee": "Yuran peperiksaan",
        "Tuition installment": "Ansuran yuran pengajian",
        "Weekend trip home": "Pulang ke rumah hujung minggu",
        "Birthday gift for a friend": "Hadiah hari lahir untuk kawan",
        "Laptop fund": "Dana komputer riba",
        "Emergency cushion": "Simpanan kecemasan",
        "Monthly salary": "Gaji bulanan",
        "Dividend payout": "Bayaran dividen",
        "Weekend consulting gig": "Kerja perunding hujung minggu",
        "Year-end bonus": "Bonus akhir tahun",
        "Electricity & water": "Elektrik & air",
        "Internet & phone": "Internet & telefon",
        "Health insurance premium": "Premium insurans kesihatan",
        "Dining out": "Makan luar",
        "Work lunch": "Makan tengah hari kerja",
        "Weekend brunch": "Brunch hujung minggu",
        "Fuel": "Minyak",
        "Parking": "Parking",
        "Toll": "Tol",
        "Bowling night": "Malam boling",
        "Karaoke": "Karaoke",
        "Cloud storage": "Storan awan",
        "Gym app": "Aplikasi gim",
        "Home essentials": "Keperluan rumah",
        "Electronics accessory": "Aksesori elektronik",
        "Kitchenware": "Peralatan dapur",
        "New outfit": "Pakaian baharu",
        "Shoes": "Kasut",
        "Work attire": "Pakaian kerja",
        "Pharmacy": "Farmasi",
        "Dental checkup": "Pemeriksaan gigi",
        "Skincare": "Penjagaan kulit",
        "Spa": "Spa",
        "Weekend getaway": "Percutian hujung minggu",
        "Birthday gift / charity donation": "Hadiah hari lahir / derma",
        "EPF / retirement contribution": "Caruman KWSP / persaraan",
        "Emergency fund": "Dana kecemasan",
        "Rental income": "Pendapatan sewa",
        "Performance bonus": "Bonus prestasi",
        "Electricity, water & internet": "Elektrik, air & internet",
        "Home & health insurance": "Insurans rumah & kesihatan",
        "Market run": "Ke pasar",
        "Family dinner": "Makan malam keluarga",
        "Snacks": "Snek",
        "Public transit": "Pengangkutan awam",
        "Family outing": "Keluar bersama keluarga",
        "Weekend activity": "Aktiviti hujung minggu",
        "Streaming rental": "Sewa streaming",
        "Household items": "Barangan rumah",
        "Clothing basics": "Pakaian asas",
        "Home decor": "Hiasan rumah",
        "Streaming service": "Perkhidmatan streaming",
        "Family trip": "Perjalanan keluarga",
        "Course / certification fee": "Yuran kursus / sijil",
        "Emergency fund contribution": "Caruman dana kecemasan",
        "Retirement contribution": "Caruman persaraan",
    },
    "id": {
        "Part-time job — campus café": "Kerja paruh waktu — kafe kampus",
        "Allowance from parents": "Uang saku dari orang tua",
        "Scholarship disbursement": "Pencairan beasiswa",
        "Tutoring side gig": "Les sampingan",
        "Dorm rent": "Sewa asrama",
        "Phone bill": "Tagihan telepon",
        "Groceries": "Belanja bulanan",
        "Meal plan top-up": "Isi ulang paket makan",
        "Campus cafeteria": "Kantin kampus",
        "Coffee run": "Kopi",
        "Late-night snack": "Camilan malam",
        "Bus pass top-up": "Isi ulang kartu bus",
        "Ride-hailing": "Ojek online",
        "Bike rental": "Sewa sepeda",
        "Train ticket": "Tiket kereta",
        "Movie night": "Nonton bareng",
        "Concert ticket": "Tiket konser",
        "Karaoke with friends": "Karaoke bersama teman",
        "Board game café": "Kafe board game",
        "Streaming subscription": "Langganan streaming",
        "Music subscription": "Langganan musik",
        "Stationery": "Alat tulis",
        "Phone case": "Casing HP",
        "Dorm supplies": "Kebutuhan asrama",
        "New clothes": "Pakaian baru",
        "Sneakers": "Sneakers",
        "Winter jacket": "Jaket musim dingin",
        "Pharmacy run": "Apotek",
        "Clinic visit": "Kunjungan klinik",
        "Vitamins": "Vitamin",
        "Haircut": "Potong rambut",
        "Toiletries": "Perlengkapan mandi",
        "Textbooks & course materials": "Buku teks & materi kuliah",
        "Printing & supplies": "Cetak & perlengkapan",
        "Exam fee": "Biaya ujian",
        "Tuition installment": "Cicilan uang kuliah",
        "Weekend trip home": "Pulang kampung akhir pekan",
        "Birthday gift for a friend": "Hadiah ulang tahun untuk teman",
        "Laptop fund": "Dana laptop",
        "Emergency cushion": "Dana darurat",
        "Monthly salary": "Gaji bulanan",
        "Dividend payout": "Pembagian dividen",
        "Weekend consulting gig": "Konsultasi akhir pekan",
        "Year-end bonus": "Bonus akhir tahun",
        "Electricity & water": "Listrik & air",
        "Internet & phone": "Internet & telepon",
        "Health insurance premium": "Premi asuransi kesehatan",
        "Dining out": "Makan di luar",
        "Work lunch": "Makan siang kerja",
        "Weekend brunch": "Brunch akhir pekan",
        "Fuel": "Bensin",
        "Parking": "Parkir",
        "Toll": "Tol",
        "Bowling night": "Malam bowling",
        "Karaoke": "Karaoke",
        "Cloud storage": "Penyimpanan cloud",
        "Gym app": "Aplikasi gym",
        "Home essentials": "Kebutuhan rumah",
        "Electronics accessory": "Aksesori elektronik",
        "Kitchenware": "Peralatan dapur",
        "New outfit": "Pakaian baru",
        "Shoes": "Sepatu",
        "Work attire": "Pakaian kerja",
        "Pharmacy": "Apotek",
        "Dental checkup": "Periksa gigi",
        "Skincare": "Perawatan kulit",
        "Spa": "Spa",
        "Weekend getaway": "Liburan akhir pekan",
        "Birthday gift / charity donation": "Hadiah ulang tahun / donasi",
        "EPF / retirement contribution": "Iuran pensiun",
        "Emergency fund": "Dana darurat",
        "Rental income": "Pendapatan sewa",
        "Performance bonus": "Bonus kinerja",
        "Electricity, water & internet": "Listrik, air & internet",
        "Home & health insurance": "Asuransi rumah & kesehatan",
        "Market run": "Belanja pasar",
        "Family dinner": "Makan malam keluarga",
        "Snacks": "Camilan",
        "Public transit": "Transportasi umum",
        "Family outing": "Jalan-jalan keluarga",
        "Weekend activity": "Aktivitas akhir pekan",
        "Streaming rental": "Sewa streaming",
        "Household items": "Barang rumah tangga",
        "Clothing basics": "Pakaian dasar",
        "Home decor": "Dekorasi rumah",
        "Streaming service": "Layanan streaming",
        "Family trip": "Perjalanan keluarga",
        "Course / certification fee": "Biaya kursus / sertifikasi",
        "Emergency fund contribution": "Setoran dana darurat",
        "Retirement contribution": "Iuran pensiun",
    },
    "zh-rCN": {
        "Part-time job — campus café": "兼职 — 校园咖啡店",
        "Allowance from parents": "父母给的生活费",
        "Scholarship disbursement": "奖学金发放",
        "Tutoring side gig": "家教兼职",
        "Dorm rent": "宿舍租金",
        "Phone bill": "话费",
        "Groceries": "杂货",
        "Meal plan top-up": "餐卡充值",
        "Campus cafeteria": "校园食堂",
        "Coffee run": "咖啡",
        "Late-night snack": "夜宵",
        "Bus pass top-up": "公交卡充值",
        "Ride-hailing": "网约车",
        "Bike rental": "租自行车",
        "Train ticket": "火车票",
        "Movie night": "电影夜",
        "Concert ticket": "演唱会门票",
        "Karaoke with friends": "和朋友唱K",
        "Board game café": "桌游咖啡馆",
        "Streaming subscription": "流媒体订阅",
        "Music subscription": "音乐订阅",
        "Stationery": "文具",
        "Phone case": "手机壳",
        "Dorm supplies": "宿舍用品",
        "New clothes": "新衣服",
        "Sneakers": "运动鞋",
        "Winter jacket": "冬装外套",
        "Pharmacy run": "药店",
        "Clinic visit": "诊所就诊",
        "Vitamins": "维生素",
        "Haircut": "理发",
        "Toiletries": "洗漱用品",
        "Textbooks & course materials": "教材与课程材料",
        "Printing & supplies": "打印与用品",
        "Exam fee": "考试费",
        "Tuition installment": "学费分期",
        "Weekend trip home": "周末回家",
        "Birthday gift for a friend": "朋友生日礼物",
        "Laptop fund": "笔记本基金",
        "Emergency cushion": "应急储备",
        "Monthly salary": "月薪",
        "Dividend payout": "股息发放",
        "Weekend consulting gig": "周末咨询兼职",
        "Year-end bonus": "年终奖",
        "Electricity & water": "水电费",
        "Internet & phone": "宽带与电话",
        "Health insurance premium": "医疗保险费",
        "Dining out": "外出就餐",
        "Work lunch": "工作午餐",
        "Weekend brunch": "周末早午餐",
        "Fuel": "加油",
        "Parking": "停车",
        "Toll": "过路费",
        "Bowling night": "保龄球之夜",
        "Karaoke": "卡拉OK",
        "Cloud storage": "云存储",
        "Gym app": "健身应用",
        "Home essentials": "家居必需品",
        "Electronics accessory": "电子配件",
        "Kitchenware": "厨具",
        "New outfit": "新衣服",
        "Shoes": "鞋子",
        "Work attire": "工作装",
        "Pharmacy": "药店",
        "Dental checkup": "牙科检查",
        "Skincare": "护肤",
        "Spa": "水疗",
        "Weekend getaway": "周末短途游",
        "Birthday gift / charity donation": "生日礼物 / 慈善捐赠",
        "EPF / retirement contribution": "退休金缴款",
        "Emergency fund": "应急基金",
        "Rental income": "租金收入",
        "Performance bonus": "绩效奖金",
        "Electricity, water & internet": "水电与宽带",
        "Home & health insurance": "房屋与健康保险",
        "Market run": "买菜",
        "Family dinner": "家庭晚餐",
        "Snacks": "零食",
        "Public transit": "公共交通",
        "Family outing": "家庭出游",
        "Weekend activity": "周末活动",
        "Streaming rental": "流媒体租赁",
        "Household items": "家居用品",
        "Clothing basics": "基础衣物",
        "Home decor": "家居装饰",
        "Streaming service": "流媒体服务",
        "Family trip": "家庭旅行",
        "Course / certification fee": "课程 / 认证费用",
        "Emergency fund contribution": "应急基金存入",
        "Retirement contribution": "退休缴款",
    },
}


def note_key(index: int) -> str:
    return f"tpl_note_{index:03d}"


def esc_xml(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("'", "\\'")
        .replace('"', '\\"')
    )


def ensure_english_notes() -> dict[str, str]:
    """Return mapping English note -> resource name; append missing keys to strings.xml."""
    mapping = {note: note_key(i) for i, note in enumerate(NOTES, start=1)}
    text = ENGLISH_XML.read_text(encoding="utf-8")
    if "tpl_note_001" in text:
        return mapping
    block = ["", "    <!-- Template sample transaction notes -->"]
    for note, key in mapping.items():
        block.append(f'    <string name="{key}">{esc_xml(note)}</string>')
    block.append("</resources>")
    text = re.sub(r"\s*</resources>\s*$", "\n" + "\n".join(block) + "\n", text)
    ENGLISH_XML.write_text(text, encoding="utf-8")
    print(f"Added {len(mapping)} tpl_note_* keys to English strings.xml")
    return mapping


def patch_translation_jsons(mapping: dict[str, str]) -> None:
    for path in sorted(TRANSLATIONS.glob("*.json")):
        locale = path.stem
        data = json.loads(path.read_text(encoding="utf-8"))
        locale_map = NOTE_I18N.get(locale, {})
        for note, key in mapping.items():
            data[key] = locale_map.get(note, note)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"notes -> {locale}")


def rewrite_templates_kt(mapping: dict[str, str]) -> None:
    text = TEMPLATES_KT.read_text(encoding="utf-8")

    # Update imports
    if "import android.content.Context" not in text:
        text = text.replace(
            "package com.financetracker.evolva.data.templates\n\n",
            "package com.financetracker.evolva.data.templates\n\n"
            "import android.content.Context\n"
            "import androidx.annotation.StringRes\n",
        )

    # Change data class fields note -> noteRes
    text = text.replace(
        "data class IncomeItem(val category: String, val note: String, val day: Int, val amountRange: IntRange, val prob: Double = 1.0)",
        "data class IncomeItem(val category: String, @StringRes val noteRes: Int, val day: Int, val amountRange: IntRange, val prob: Double = 1.0)",
    )
    text = text.replace(
        "data class FixedExpenseItem(val category: String, val note: String, val day: Int, val amountRange: IntRange)",
        "data class FixedExpenseItem(val category: String, @StringRes val noteRes: Int, val day: Int, val amountRange: IntRange)",
    )
    text = text.replace(
        "data class VariableExpenseGroup(val category: String, val countRange: IntRange, val amountRange: IntRange, val notes: List<String>)",
        "data class VariableExpenseGroup(val category: String, val countRange: IntRange, val amountRange: IntRange, val noteResList: List<Int>)",
    )
    text = text.replace(
        "data class OccasionalExpenseItem(val category: String, val note: String, val day: Int, val amountRange: IntRange, val prob: Double)",
        "data class OccasionalExpenseItem(val category: String, @StringRes val noteRes: Int, val day: Int, val amountRange: IntRange, val prob: Double)",
    )
    text = text.replace(
        "data class SavingsItem(val category: String, val note: String, val day: Int, val amountRange: IntRange, val prob: Double = 1.0)",
        "data class SavingsItem(val category: String, @StringRes val noteRes: Int, val day: Int, val amountRange: IntRange, val prob: Double = 1.0)",
    )

    # Replace English note string literals with R.string.tpl_note_XXX
    # Longer notes first to avoid partial replacements inside other strings (unlikely but safe).
    for note in sorted(mapping.keys(), key=len, reverse=True):
        key = mapping[note]
        text = text.replace(f'"{note}"', f"com.financetracker.evolva.R.string.{key}")

    # Fix VariableExpenseGroup listOf(...) field name already changed; list contents are R.string ints.
    text = text.replace("val notes: List<String>", "val noteResList: List<Int>")  # safety

    # Update buildProfileTransactions signature and note resolution
    old_sig = "fun buildProfileTransactions(profile: SpendingProfile, months: List<YearMonth>): List<Transaction> {"
    new_sig = "fun buildProfileTransactions(context: Context, profile: SpendingProfile, months: List<YearMonth>): List<Transaction> {"
    text = text.replace(old_sig, new_sig)

    text = text.replace("note = item.note,", "note = context.getString(item.noteRes),")
    text = text.replace("note = pick(group.notes),", "note = context.getString(pick(group.noteResList)),")

    # AppTemplate.generate becomes (Context) -> ...
    text = text.replace(
        "val generate: () -> List<Transaction>",
        "val generate: (Context) -> List<Transaction>",
    )
    text = text.replace(
        "generate = { buildProfileTransactions(STUDENT_PROFILE, monthsBack(6)) }",
        "generate = { ctx -> buildProfileTransactions(ctx, STUDENT_PROFILE, monthsBack(6)) }",
    )
    text = text.replace(
        "generate = { buildProfileTransactions(STAFF_PROFILE, monthsBack(6)) }",
        "generate = { ctx -> buildProfileTransactions(ctx, STAFF_PROFILE, monthsBack(6)) }",
    )
    text = text.replace(
        "generate = { buildProfileTransactions(BUDGET_EXAMPLE_PROFILE, fixedMonths(2026, 1, 7)) }",
        "generate = { ctx -> buildProfileTransactions(ctx, BUDGET_EXAMPLE_PROFILE, fixedMonths(2026, 1, 7)) }",
    )
    text = text.replace(
        """generate = {
            buildProfileTransactions(
                STAFF_PROFILE,
                monthRange(YearMonth.of(2025, 1), YearMonth.of(2026, 7))
            )
        }""",
        """generate = { ctx ->
            buildProfileTransactions(
                ctx,
                STAFF_PROFILE,
                monthRange(YearMonth.of(2025, 1), YearMonth.of(2026, 7))
            )
        }""",
    )

    TEMPLATES_KT.write_text(text, encoding="utf-8")
    print("Rewrote Templates.kt for Context-based notes")


def patch_active_session() -> None:
    text = ACTIVE_SESSION.read_text(encoding="utf-8")
    text2 = text.replace("val rows = template.generate()", "val rows = template.generate(appContext)")
    if text2 == text:
        raise SystemExit("Failed to patch ActiveProfileSession.generate call")
    ACTIVE_SESSION.write_text(text2, encoding="utf-8")
    print("Patched ActiveProfileSession.setupTemplate")


def main() -> None:
    missing = []
    src = TEMPLATES_KT.read_text(encoding="utf-8")
    for note in NOTES:
        if f'"{note}"' not in src and f"R.string.{note_key(NOTES.index(note)+1)}" not in src:
            # after rewrite notes become R refs; before rewrite must exist
            if "noteRes" not in src:
                missing.append(note)
    if missing and "noteRes" not in src:
        print("WARNING missing notes in Templates.kt:", missing)

    mapping = ensure_english_notes()
    if "noteRes" not in TEMPLATES_KT.read_text(encoding="utf-8"):
        rewrite_templates_kt(mapping)
        patch_active_session()
    else:
        print("Templates.kt already uses noteRes; skipping rewrite")
    patch_translation_jsons(mapping)
    print("Run: python scripts/generate_locales.py")


if __name__ == "__main__":
    main()

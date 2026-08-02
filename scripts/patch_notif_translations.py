# -*- coding: utf-8 -*-
"""Add notification string translations for all locales."""
import json
from pathlib import Path

NOTIF = {
    "ms": {
        "notif_channel_budget": "Amaran belanjawan",
        "notif_channel_budget_desc": "Amaran apabila kategori hampir atau melebihi had",
        "notif_over_budget": "Melebihi belanjawan: %1$s",
        "notif_near_limit": "Hampir had: %1$s",
        "notif_budget_spent": "%1$s daripada %2$s dibelanja bulan ini",
    },
    "my": {
        "notif_channel_budget": "ဘတ်ဂျက်သတိပေးချက်များ",
        "notif_channel_budget_desc": "အမျိုးအစားဘတ်ဂျက်နီးကပ် သို့မဟုတ် ကျော်လွန်သည့်အခါ သတိပေးသည်",
        "notif_over_budget": "ဘတ်ဂျက်ကျော်: %1$s",
        "notif_near_limit": "ကန့်သတ်နီး: %1$s",
        "notif_budget_spent": "ဤလတွင် %2$s အနက် %1$s သုံးပြီး",
    },
    "id": {
        "notif_channel_budget": "Peringatan anggaran",
        "notif_channel_budget_desc": "Peringatan saat kategori mendekati atau melebihi batas",
        "notif_over_budget": "Melebihi anggaran: %1$s",
        "notif_near_limit": "Mendekati batas: %1$s",
        "notif_budget_spent": "%1$s dari %2$s dibelanjakan bulan ini",
    },
    "zh-rCN": {
        "notif_channel_budget": "预算提醒",
        "notif_channel_budget_desc": "类别预算接近或超出限额时提醒",
        "notif_over_budget": "超出预算：%1$s",
        "notif_near_limit": "接近限额：%1$s",
        "notif_budget_spent": "本月已用 %1$s / %2$s",
    },
    "ja": {
        "notif_channel_budget": "予算アラート",
        "notif_channel_budget_desc": "カテゴリ予算が上限に近づいた／超えたときに通知",
        "notif_over_budget": "予算超過: %1$s",
        "notif_near_limit": "上限間近: %1$s",
        "notif_budget_spent": "今月 %2$s 中 %1$s を使用",
    },
    "ko": {
        "notif_channel_budget": "예산 알림",
        "notif_channel_budget_desc": "카테고리 예산이 한도에 가까워지거나 초과할 때 알림",
        "notif_over_budget": "예산 초과: %1$s",
        "notif_near_limit": "한도 임박: %1$s",
        "notif_budget_spent": "이번 달 %2$s 중 %1$s 사용",
    },
    "es": {
        "notif_channel_budget": "Alertas de presupuesto",
        "notif_channel_budget_desc": "Avisos cuando un presupuesto de categoría está cerca o se excede",
        "notif_over_budget": "Presupuesto excedido: %1$s",
        "notif_near_limit": "Cerca del límite: %1$s",
        "notif_budget_spent": "%1$s de %2$s gastados este mes",
    },
    "fr": {
        "notif_channel_budget": "Alertes budget",
        "notif_channel_budget_desc": "Alertes quand un budget de catégorie approche ou dépasse la limite",
        "notif_over_budget": "Budget dépassé : %1$s",
        "notif_near_limit": "Proche de la limite : %1$s",
        "notif_budget_spent": "%1$s sur %2$s dépensés ce mois",
    },
    "de": {
        "notif_channel_budget": "Budgetwarnungen",
        "notif_channel_budget_desc": "Warnungen, wenn ein Kategoriebudget nahe oder über dem Limit liegt",
        "notif_over_budget": "Budget überschritten: %1$s",
        "notif_near_limit": "Nahe am Limit: %1$s",
        "notif_budget_spent": "%1$s von %2$s diesen Monat ausgegeben",
    },
    "vi": {
        "notif_channel_budget": "Cảnh báo ngân sách",
        "notif_channel_budget_desc": "Cảnh báo khi danh mục gần hoặc vượt hạn mức",
        "notif_over_budget": "Vượt ngân sách: %1$s",
        "notif_near_limit": "Gần hạn mức: %1$s",
        "notif_budget_spent": "Đã chi %1$s / %2$s tháng này",
    },
    "th": {
        "notif_channel_budget": "การแจ้งเตือนงบประมาณ",
        "notif_channel_budget_desc": "แจ้งเตือนเมื่อหมวดใกล้งบหรือเกินงบ",
        "notif_over_budget": "เกินงบ: %1$s",
        "notif_near_limit": "ใกล้งบ: %1$s",
        "notif_budget_spent": "ใช้ไป %1$s จาก %2$s ในเดือนนี้",
    },
    "ar": {
        "notif_channel_budget": "تنبيهات الميزانية",
        "notif_channel_budget_desc": "تنبيهات عند اقتراب أو تجاوز حد فئة",
        "notif_over_budget": "تجاوز الميزانية: %1$s",
        "notif_near_limit": "قرب الحد: %1$s",
        "notif_budget_spent": "أُنفق %1$s من %2$s هذا الشهر",
    },
}

EN = {
    "notif_channel_budget": "Budget alerts",
    "notif_channel_budget_desc": "Alerts when a category budget is near or over its limit",
    "notif_over_budget": "Over budget: %1$s",
    "notif_near_limit": "Near limit: %1$s",
    "notif_budget_spent": "%1$s of %2$s spent this month",
}

td = Path(__file__).resolve().parent / "translations"
for p in td.glob("*.json"):
    data = json.loads(p.read_text(encoding="utf-8"))
    data.update(NOTIF.get(p.stem, EN))
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(p.name)

# -*- coding: utf-8 -*-
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent
en = (root / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
pairs = re.findall(r'<string\s+name="([^"]+)">(.*?)</string>', en, re.DOTALL)
insight_en = {k: v for k, v in pairs if k.startswith("insight_")}


def decode(s: str) -> str:
    return (
        s.replace("&amp;", "&")
        .replace("&quot;", '"')
        .replace("\\'", "'")
        .replace("\\n", "\n")
        .replace('\\"', '"')
    )


insight_en = {k: decode(v) for k, v in insight_en.items()}

ms = {
    "insight_empty": "Tambah pendapatan, perbelanjaan atau simpanan pertama anda untuk melihat cerapan di sini.",
    "insight_avg": "Purata anda memperoleh %1$s dan membelanja %2$s sebulan (merentas %3$d bulan data).",
    "insight_saving_rate": "Anda menyimpan %1$s%% daripada pendapatan anda.",
    "insight_biggest_expense": "Kategori perbelanjaan terbesar anda ialah %1$s sebanyak %2$s.",
    "insight_spend_up": "Perbelanjaan naik %1$s%% berbanding bulan lalu.",
    "insight_spend_down": "Perbelanjaan turun %1$s%% berbanding bulan lalu.",
    "insight_net_negative": "Anda membelanja lebih daripada pendapatan tempoh ini — baki bersih %1$s.",
    "insight_net_positive": "Baki bersih positif sebanyak %1$s.",
    "insight_transfers": "Anda menghantar %1$s dan menerima %2$s dalam pindahan (bersih %3$s).",
    "insight_recurring": "Anda mempunyai %1$d transaksi berulang yang dijana setiap bulan.",
    "insight_over_one": "Anda melebihi belanjawan %1$s bulan ini — %2$s daripada %3$s.",
    "insight_over_many": "Anda melebihi belanjawan pada %1$d kategori bulan ini: %2$s.",
    "insight_near": "Anda hampir mencapai belanjawan %1$s bulan ini — %2$s daripada %3$s.",
}

my = {
    "insight_empty": "ဤနေရာတွင် အကြံပြုချက်များကြည့်ရန် ဝင်ငွေ၊ အသုံးစရိတ် သို့မဟုတ် စုဆောင်းငွေ ပထမဆုံး ထည့်ပါ။",
    "insight_avg": "ပျမ်းမျှအားဖြင့် တစ်လလျှင် %1$s ရရှိပြီး %2$s သုံးစွဲသည် (%3$d လဒေတာအပေါ်အခြေခံ).",
    "insight_saving_rate": "ဝင်ငွေ၏ %1$s%% ကို သင်စုဆောင်းနေသည်။",
    "insight_biggest_expense": "အကြီးဆုံးအသုံးစရိတ်အမျိုးအစားမှာ %1$s ဖြစ်ပြီး %2$s ဖြစ်သည်။",
    "insight_spend_up": "အသုံးစရိတ်သည် ပြီးခဲ့သောလနှင့် နှိုင်းယှဉ်လျှင် %1$s%% တက်လာသည်။",
    "insight_spend_down": "အသုံးစရိတ်သည် ပြီးခဲ့သောလနှင့် နှိုင်းယှဉ်လျှင် %1$s%% ကျဆင်းသည်။",
    "insight_net_negative": "ဤကာလတွင် ဝင်ငွေထက်ပိုသုံးနေသည် — အသားတင်လက်ကျန် %1$s။",
    "insight_net_positive": "အသားတင်လက်ကျန် အပေါင်းလက္ခဏာဖြင့် %1$s ရှိသည်။",
    "insight_transfers": "လွှဲပြောင်းငွေ %1$s ပို့ပြီး %2$s လက်ခံခဲ့သည် (အသားတင် %3$s)။",
    "insight_recurring": "လစဉ်အလိုအလျောက်တင်မည့် ထပ်တလဲလဲငွေစာရင်း %1$d ခု ရှိသည်။",
    "insight_over_one": "ဤလတွင် %1$s ဘတ်ဂျက်ကျော်နေသည် — %3$s အနက် %2$s။",
    "insight_over_many": "ဤလတွင် အမျိုးအစား %1$d ခု ဘတ်ဂျက်ကျော်နေသည်: %2$s။",
    "insight_near": "ဤလတွင် %1$s ဘတ်ဂျက်နီးကပ်နေသည် — %3$s အနက် %2$s။",
}

td = Path(__file__).resolve().parent / "translations"
for p in td.glob("*.json"):
    data = json.loads(p.read_text(encoding="utf-8"))
    if p.stem == "ms":
        data.update(ms)
    elif p.stem == "my":
        data.update(my)
    else:
        for k, v in insight_en.items():
            data.setdefault(k, v)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(p.name, "updated")

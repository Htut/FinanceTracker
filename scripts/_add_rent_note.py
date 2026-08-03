# -*- coding: utf-8 -*-
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "translations"
RENT = {
    "ms": "Sewa",
    "my": "ငှားရမ်းခ",
    "id": "Sewa",
    "zh-rCN": "房租",
    "ja": "家賃",
    "ko": "월세",
    "es": "Alquiler",
    "fr": "Loyer",
    "de": "Miete",
    "vi": "Tiền thuê",
    "th": "ค่าเช่า",
    "ar": "إيجار",
    "hi": "किराया",
    "ru": "Аренда",
    "it": "Affitto",
    "tr": "Kira",
    "fa": "اجاره",
    "ur": "کرایہ",
    "ta": "வாடகை",
}
for path in sorted(ROOT.glob("*.json")):
    data = json.loads(path.read_text(encoding="utf-8"))
    data["tpl_note_091"] = RENT.get(path.stem, "Rent")
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(path.stem)

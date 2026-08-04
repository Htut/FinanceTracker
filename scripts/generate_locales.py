# -*- coding: utf-8 -*-
"""Generate localized values-*/strings.xml from English keys.

Parses ALL <string name="..."> entries from values/strings.xml, loads complete
translations from scripts/translations/<locale>.json, and writes UTF-8 Android
resource files for each locale folder.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent / "app" / "src" / "main" / "res"
ENGLISH_XML = ROOT / "values" / "strings.xml"
TRANSLATIONS_DIR = SCRIPT_DIR / "translations"

LOCALES = [
    "ms",
    "my",
    "id",
    "ta",
    "zh-rCN",
    "ja",
    "ko",
    "ru",
    "th",
    "es",
    "fr",
    "it",
    "vi",
    "tr",
    "fa",
    "de",
    "ar",
    "ur",
    "hi",
]

# Android resource folder qualifiers. Indonesian uses "in" (legacy Java locale),
# while translation JSON keys stay as "id".
RESOURCE_FOLDER = {
    "id": "in",
}

STRING_RE = re.compile(
    r'<string\s+name="([^"]+)">(.*?)</string>',
    re.DOTALL,
)
PLACEHOLDER_RE = re.compile(r"%\d+\$[sd]|%%")


def parse_english(path: Path) -> list[tuple[str, str]]:
    """Return ordered (name, raw_xml_text) pairs; keep entities/escapes as in source."""
    text = path.read_text(encoding="utf-8")
    pairs = STRING_RE.findall(text)
    if not pairs:
        raise SystemExit(f"No <string> entries found in {path}")
    return pairs


def to_android_xml_text(value: str) -> str:
    """Normalize translation text to Android strings.xml conventions.

    - Real newlines become literal \\n
    - Unescaped " and ' become \\" and \\'
    - Bare & becomes &amp; (existing &amp; / &quot; / &lt; / &gt; kept)
    - Existing \\", \\', \\n, %% are left intact (no double-escape)
    """
    # Normalize newlines to Android literal \n sequences
    value = value.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\n")

    # Escape bare ampersands without double-escaping existing entities
    value = re.sub(r"&(?!amp;|lt;|gt;|quot;|apos;|#\d+;|#x[0-9a-fA-F]+;)", "&amp;", value)

    out: list[str] = []
    i = 0
    n = len(value)
    while i < n:
        ch = value[i]
        if ch == "\\" and i + 1 < n:
            nxt = value[i + 1]
            # Keep valid Android / common escapes as-is
            if nxt in {'"', "'", "n", "t", "\\", "@", "?"}:
                out.append(ch)
                out.append(nxt)
                i += 2
                continue
        if ch == '"':
            out.append('\\"')
            i += 1
            continue
        if ch == "'":
            out.append("\\'")
            i += 1
            continue
        out.append(ch)
        i += 1
    return "".join(out)


def placeholders(s: str) -> list[str]:
    return PLACEHOLDER_RE.findall(s)


def placeholder_set(s: str) -> set[str]:
    return set(placeholders(s))


def load_locale(folder: str, required_keys: set[str], english: dict[str, str]) -> dict[str, str]:
    path = TRANSLATIONS_DIR / f"{folder}.json"
    if not path.exists():
        raise SystemExit(f"Missing translation file: {path}")
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise SystemExit(f"{path} must be a JSON object")
    missing = sorted(required_keys - set(data))
    extra = sorted(set(data) - required_keys)
    if missing:
        print(f"warning: {folder}: {len(missing)} missing keys fall back to English, e.g. {missing[:6]}")
        for key in missing:
            data[key] = english[key]
    if "load_template" in data:
        raise SystemExit(f"{folder}: unexpected obsolete key load_template")
    if extra:
        print(f"warning: {folder} has {len(extra)} extra keys (ignored)")
    return {k: str(data[k]) for k in required_keys}


def write_locale_xml(
    folder: str,
    ordered_keys: list[str],
    english: dict[str, str],
    translations: dict[str, str],
) -> Path:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<resources>",
    ]
    for key in ordered_keys:
        eng = english[key]
        raw = translations[key]
        # Soft-check placeholders (positional args may be reordered)
        if placeholder_set(eng) != placeholder_set(raw):
            raise SystemExit(
                f"{folder}/{key}: placeholder mismatch "
                f"en={sorted(placeholder_set(eng))} tr={sorted(placeholder_set(raw))}"
            )
        text = to_android_xml_text(raw)
        lines.append(f'    <string name="{key}">{text}</string>')
    lines.append("</resources>")
    lines.append("")
    res_folder = RESOURCE_FOLDER.get(folder, folder)
    out_dir = ROOT / f"values-{res_folder}"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "strings.xml"
    out_path.write_text("\n".join(lines), encoding="utf-8")
    return out_path


def main() -> int:
    pairs = parse_english(ENGLISH_XML)
    ordered_keys = [k for k, _ in pairs]
    english = {k: v for k, v in pairs}
    key_set = set(ordered_keys)

    if "load_template" in key_set:
        raise SystemExit("English strings.xml unexpectedly contains load_template")

    print(f"English keys: {len(ordered_keys)}")

    written = []
    for folder in LOCALES:
        translations = load_locale(folder, key_set, english)
        # Ensure new critical keys exist
        for must in (
            "section_profiles",
            "fetch_live_rates",
            "cat_food",
            "tpl_student_label",
            "theme_classic",
            "msg_switched_profile",
        ):
            if must not in translations:
                raise SystemExit(f"{folder}: missing required key {must}")
        path = write_locale_xml(folder, ordered_keys, english, translations)
        res_folder = RESOURCE_FOLDER.get(folder, folder)
        written.append((res_folder, path))
        print(f"wrote values-{res_folder}/strings.xml ({len(ordered_keys)} keys)")

    print(f"done: {len(written)} locales, {len(ordered_keys)} keys each")
    return 0


if __name__ == "__main__":
    sys.exit(main())

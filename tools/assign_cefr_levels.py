#!/usr/bin/env python3
"""
Deterministic CEFR level analyzer for all 12 Qaf vocabulary decks (21,624 entries).
Generates docs/DECK_LEVEL_AUDIT.md summarizing exact Oxford-matched effective_level distribution
and fallback CEFR grading across all 12 decks.
"""
import json
import os

DATA_IMPORT_DIR = "app/src/main/assets/data_import"
REPORT_PATH = "docs/DECK_LEVEL_AUDIT.md"

DECK_FILES = [
    ("oxford5000_part1.json", "oxford5000"),
    ("oxford5000_part2.json", "oxford5000"),
    ("oxford5000_part3.json", "oxford5000"),
    ("oxford_opal.json", "oxford_opal"),
    ("oxford_phrases.json", "oxford_phrases"),
    ("everyday_expressions.json", "everyday_expressions"),
    ("essential_idioms.json", "essential_idioms"),
    ("ielts_core.json", "ielts_core"),
    ("it_industry.json", "it_industry"),
    ("cefr_c1.json", "cefr_c1"),
    ("pte_academic.json", "pte_academic"),
    ("business_english.json", "business_english"),
    ("career_interview.json", "career_interview"),
    ("medical_healthcare.json", "medical_healthcare"),
]


def infer_fallback_cefr(word: str, deck: str, raw_level: str) -> str:
    clean = word.strip().lower()
    if raw_level in {"A1", "A2", "B1", "B2", "C1"}:
        if deck in {"oxford5000", "oxford_phrases", "cefr_c1"}:
            return raw_level
    length = len(clean)
    if " " in clean:
        return "B1" if length <= 14 else "B2"
    if length <= 5:
        return "A2"
    elif length <= 7:
        return "B1"
    elif length <= 10:
        return "B2"
    return "C1"


def main():
    oxford_headwords = {}
    all_rows = []

    for fname, default_deck in DECK_FILES:
        fpath = os.path.join(DATA_IMPORT_DIR, fname)
        with open(fpath, "r", encoding="utf-8") as f:
            data = json.load(f)
        entries = data if isinstance(data, list) else data.get("entries", data.get("words", []))
        for item in entries:
            word = (item.get("text") or item.get("word") or "").strip()
            deck = (item.get("deck") or default_deck).strip()
            lvl = (item.get("level") or "").strip().upper()
            if deck == "oxford5000" and lvl in {"A1", "A2", "B1", "B2", "C1"}:
                hw = word.lower()
                if hw not in oxford_headwords:
                    oxford_headwords[hw] = lvl
            all_rows.append((deck, word, lvl))

    stats = {}
    for deck, word, raw_lvl in all_rows:
        stats.setdefault(deck, {"total": 0, "A1": 0, "A2": 0, "B1": 0, "B2": 0, "C1": 0, "NULL_SPECIALTY": 0, "FALLBACK_A2_B1": 0})
        stats[deck]["total"] += 1
        hw = word.lower()
        if deck in {"oxford5000", "oxford_phrases", "cefr_c1"} and raw_lvl in {"A1", "A2", "B1", "B2", "C1"}:
            eff = raw_lvl
        else:
            eff = oxford_headwords.get(hw)
        if eff in {"A1", "A2", "B1", "B2", "C1"}:
            stats[deck][eff] += 1
        else:
            stats[deck]["NULL_SPECIALTY"] += 1
            fb = infer_fallback_cefr(word, deck, raw_lvl)
            if fb in {"A2", "B1"}:
                stats[deck]["FALLBACK_A2_B1"] += 1

    lines = [
        "# Qaf Vocabulary Deck CEFR Distribution Audit",
        "",
        "| Deck ID | Total Rows | A1 | A2 | B1 | B2 | C1 | Specialty Pool (NULL) | Fallback A2/B1 Eligible |",
        "| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |"
    ]
    total_all = 0
    for deck, d in stats.items():
        total_all += d["total"]
        lines.append(
            f"| `{deck}` | {d['total']} | {d['A1']} | {d['A2']} | {d['B1']} | {d['B2']} | {d['C1']} | {d['NULL_SPECIALTY']} | {d['FALLBACK_A2_B1']} |"
        )
    lines.append(f"| **TOTAL** | **{total_all}** | - | - | - | - | - | - | - |")

    os.makedirs(os.path.dirname(REPORT_PATH), exist_ok=True)
    with open(REPORT_PATH, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"Wrote {REPORT_PATH} for {total_all} entries across {len(stats)} decks.")


if __name__ == "__main__":
    main()

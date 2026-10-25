#!/usr/bin/env python3
"""Hydrates CEFR levels, ielts_core definitions and ai_word_details meanings, then validates.

Usage: python tools/hydrate_and_validate_data.py [--validate-only]
"""
import json
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")

D = "app/src/main/assets/data_import"
LV = {"A1", "A2", "B1", "B2", "C1"}
POS_ABBR = {
    "noun": "n.", "verb": "v.", "adjective": "adj.", "adverb": "adv.", "preposition": "prep.",
    "conjunction": "conj.", "pronoun": "pron.", "determiner": "det.", "article": "art.",
    "exclamation": "exclam.", "interjection": "interj.", "number": "num.", "modal verb": "modal v.",
    "auxiliary verb": "aux. v.", "phrasal verb": "phr. v.", "idiom": "idiom",
}
SPELLING = {
    "gasolene": "gasoline", "gramme": "gram", "dorm": "dormitory", "theatre": "theater",
    "centre": "center", "colour": "color", "favour": "favor", "honour": "honor",
    "labour": "labor", "neighbour": "neighbor", "jean": "jeans", "reenforce": "reinforce",
    "instilment": "instill", "phantasy": "fantasy", "reflexion": "reflection", "barque": "bark",
    "scissor": "scissors", "draught": "draft", "co-operative": "cooperative",
    "world-wide": "worldwide", "axe": "ax", "skilful": "skillful", "amongst": "among",
    "inclose": "enclose", "connexion": "connection", "centimetre": "centimeter",
    "waggon": "wagon", "civilisation": "civilization", "cigaret": "cigarette",
    "honourable": "honorable", "fertiliser": "fertilizer",
}
FALLBACK = {
    "living room": "A1", "scissor": "A1", "centimetre": "A1", "l": "A1", "jean": "A1",
    "amongst": "A2", "sightsee": "A2", "handwrite": "A2", "cigaret": "A2", "datum": "A2",
    "gasolene": "A2", "gramme": "A2", "phantasy": "B1", "trash": "B1", "world-wide": "B1",
    "axe": "B1", "paperback": "B1", "rap": "B1", "connexion": "B1", "waggon": "B1",
    "dorm": "B1", "reenforce": "B2", "reflexion": "B2", "draught": "B2", "co-operative": "B2",
    "skilful": "B2", "inclose": "B2", "civilisation": "B2", "honourable": "B2",
    "instilment": "C1", "transient": "B2", "barque": "C1", "butch": "C1", "fertiliser": "B2",
    "sophisticate": "C1", "stave": "C1", "transient ": "B2",
}
OXFORD_FIX = {48: "B2", 234: "A2", 890: "B1", 2059: "B2", 3177: "B1"}


def load(name):
    with open(os.path.join(D, name), encoding="utf-8") as f:
        return json.load(f)


def save(name, data):
    with open(os.path.join(D, name), "w", encoding="utf-8", newline="\r\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def key(e):
    return (e.get("text") or e.get("word") or "").strip().lower()


def deck_files():
    return [f for f in sorted(os.listdir(D)) if f.endswith(".json") and f != "ai_word_details.json"]


def build_ref():
    ref = {}
    for fn in ["oxford5000_part1.json", "oxford5000_part2.json", "oxford5000_part3.json",
               "cefr_c1.json", "ielts_core.json", "business_english.json", "it_industry.json"]:
        for e in load(fn):
            lvl = (e.get("level") or "").strip().upper()
            if lvl in LV and key(e):
                ref.setdefault(key(e), lvl)
    ox = {}
    for fn in ["oxford5000_part1.json", "oxford5000_part2.json", "oxford5000_part3.json"]:
        for e in load(fn):
            lvl = (e.get("level") or "").strip().upper()
            if lvl in LV:
                ox.setdefault(key(e), lvl)
    return ref, ox


def phrase_level(text, ox):
    clean = re.sub(r"[()]", " ", text.lower())
    toks = [t for t in re.split(r"[\s/]+", clean) if t and t not in {"to", "sb", "sth", "etc"}]
    n = len(toks)
    if n <= 2:
        return "A1" if toks and all(ox.get(t) == "A1" for t in toks) else "A2"
    if n == 3:
        return "B1"
    return "B2"


def opal_level(text, ref, ox):
    t = text.strip().lower()
    if t in ref:
        return ref[t]
    if " " in t:
        n = len(t.split())
        return "B1" if n <= 2 else ("B2" if n == 3 else "C1")
    return "B2" if len(t) <= 9 else "C1"


def step_levels():
    ref, ox = build_ref()
    # Oxford outliers
    for fn in ["oxford5000_part1.json", "oxford5000_part2.json"]:
        data = load(fn)
        for e in data:
            if e.get("id") in OXFORD_FIX and not (e.get("level") or "").strip():
                e["level"] = OXFORD_FIX[e["id"]]
        save(fn, data)
    # PTE
    pte = load("pte_academic.json")
    for e in pte:
        w = key(e)
        n = SPELLING.get(w, w)
        alt = n[:-3] + "ize" if n.endswith("ise") else n
        e["level"] = ref.get(w) or ref.get(n) or ref.get(alt) or FALLBACK.get(w) or "B2"
    save("pte_academic.json", pte)
    # OPAL / expressions / idioms
    opal = load("oxford_opal.json")
    for e in opal:
        e["level"] = opal_level(e["text"], ref, ox)
    save("oxford_opal.json", opal)
    for fn in ["everyday_expressions.json", "essential_idioms.json"]:
        data = load(fn)
        for e in data:
            e["level"] = phrase_level(e["text"], ox)
        save(fn, data)


def step_ielts():
    pool = {}
    for fn in deck_files():
        if fn == "ielts_core.json":
            continue
        for e in load(fn):
            if (e.get("definition_en") or "").strip():
                pool.setdefault(key(e), []).append(e)
    ielts = load("ielts_core.json")
    for e in ielts:
        if (e.get("definition_en") or "").strip():
            continue
        cands = pool.get(key(e), [])
        pos = (e.get("pos") or "").lower()
        best = next((c for c in cands if (c.get("pos") or "").lower() == pos and c.get("definition_ar")), None) \
            or next((c for c in cands if c.get("definition_ar")), None) or (cands[0] if cands else None)
        if best:
            e["definition_en"] = best["definition_en"]
            if (best.get("definition_ar") or "").strip():
                e["definition_ar"] = best["definition_ar"]
    save("ielts_core.json", ielts)


def split_ar(s):
    return [x.strip() for x in re.split(r"[طŒ,;ط›]", s or "") if x.strip()][:6]


def step_ai():
    cards = {}
    for fn in deck_files():
        for e in load(fn):
            cards.setdefault(key(e), []).append(e)
    ai = load("ai_word_details.json")
    for row in ai:
        inner = json.loads(row["json"])
        if inner.get("meanings"):
            continue
        meanings, seen = [], set()
        for e in cards.get(row["key"].strip().lower(), []):
            pos = (e.get("pos") or "").strip().lower()
            if pos in seen or not (e.get("definition_en") or "").strip():
                continue
            seen.add(pos)
            ex_en = [x.strip() for x in (e.get("examples_en") or "").split("|") if x.strip()]
            ex_ar = [x.strip() for x in (e.get("examples_ar") or "").split("|") if x.strip()]
            meanings.append({
                "pos": POS_ABBR.get(pos, pos),
                "arabic": split_ar(e.get("arabic")),
                "definition_en": e["definition_en"],
                "definition_ar": e.get("definition_ar") or "",
                "example": {"en": ex_en[0] if ex_en else "", "ar": ex_ar[0] if ex_ar else ""},
            })
        inner["meanings"] = meanings
        row["json"] = json.dumps(inner, ensure_ascii=False)
    save("ai_word_details.json", ai)


def validate():
    ok = True

    def check(name, n):
        nonlocal ok
        print(f"{name}: {n}")
        ok &= n == 0

    for fn in deck_files():
        bad = [e for e in load(fn) if (e.get("level") or "").strip().upper() not in LV]
        check(f"{fn} non-A1..C1 level", len(bad))
    il = load("ielts_core.json")
    check("ielts_core missing definition_en", sum(1 for e in il if not (e.get("definition_en") or "").strip()))
    check("ielts_core missing definition_ar", sum(1 for e in il if not (e.get("definition_ar") or "").strip()))
    ai = load("ai_word_details.json")
    check("ai_word_details empty meanings", sum(1 for r in ai if not json.loads(r["json"]).get("meanings")))
    print("ALL OK" if ok else "VALIDATION FAILED")
    return ok


if __name__ == "__main__":
    if "--validate-only" not in sys.argv:
        step_levels()
        step_ielts()
        step_ai()
    sys.exit(0 if validate() else 1)

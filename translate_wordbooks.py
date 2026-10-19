"""
translate_wordbooks.py
Optimized to match the Android app's exact translation & dictionary output format:
  - "arabic": 2-4 Modern Standard Arabic meanings separated by "، " (Arabic comma)
  - "arabic_source": "ai" (preserves any existing "user" edits)
  - "ipa": US IPA pronunciation between slashes (e.g. "/əˈbændən/")
  - "definition_en": keeps existing definition_en, or generates a clear 1-sentence
                     English definition when empty (e.g. Oxford 5000 / OPAL / Phrases)
  - "examples_en": keeps existing examples_en, or generates 2 natural English example
                   sentences separated by " | " when empty
  - "examples_ar": Arabic translations matching examples_en 1-to-1, separated by " | "

Also includes:
  - Active Gemini models (gemini-2.5-flash, gemini-2.5-flash-lite, gemini-2.0-flash, gemini-2.0-flash-lite)
  - Multi-key rotation (comma-separated keys in GEMINI_API_KEY, supporting AIzaSy..., gsk_..., sk-...)
  - Smart 60s per-(key, model) cooldowns on HTTP 429
  - Automatic Free API fallback (dictionaryapi.dev + Google GTX) on quota exhaustion or with --free-only
  - Atomic cache.json checkpoints so stopping (Ctrl+C) and resuming never loses progress

SETUP:
  1) pip install requests
  2) Set one or more keys (comma-separated supported):
       Windows (PowerShell):  $env:GEMINI_API_KEY="AIzaSyKey1,AIzaSyKey2,gsk_Key3"
       Mac/Linux:             export GEMINI_API_KEY="AIzaSyKey1,AIzaSyKey2"
     Or run without any key using the Free Dictionary + Google GTX engine:
       python translate_wordbooks.py --free-only
  3) Run:
       python translate_wordbooks.py
"""

import argparse
import io
import json
import os
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import requests

# ---------- settings you may change ----------
GEMINI_MODELS = [
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.0-flash",
    "gemini-2.0-flash-lite",
]
GROQ_MODEL = "llama-3.3-70b-versatile"
OPENAI_MODEL = "gpt-4o-mini"
OPENROUTER_MODEL = "google/gemini-2.5-flash"

BATCH = 25                   # matches the Android app's 25-entry batch size
PAUSE = 2.0                  # seconds between AI requests
FREE_PAUSE = 0.5             # seconds between Free API batches
IN_DIR = "."                 # auto-falls back to app/src/main/assets/data_import if not in "."
OUT_DIR = "translated"
CACHE_FILE = "cache.json"

FILES = [
    "oxford5000_part1.json",
    "oxford5000_part2.json",
    "oxford5000_part3.json",
    "oxford_opal.json",
    "oxford_phrases.json",
    "everyday_expressions.json",
    "essential_idioms.json",
]
# ---------------------------------------------

# Ensure UTF-8 output on Windows
if sys.platform == "win32":
    if hasattr(sys.stdout, "buffer"):
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "buffer"):
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

ARABIC_RE = re.compile(r"[\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]")
COOLDOWNS: Dict[str, float] = {}  # "key_idx:model" -> monotonic expiry timestamp


def contains_arabic(text: str) -> bool:
    return bool(text and ARABIC_RE.search(text))


def normalize_arabic_meanings(ar_value) -> List[str]:
    """Normalizes string or list of Arabic meanings into 2-4 clean items."""
    if isinstance(ar_value, str):
        raw_parts = re.split(r"[،,;؛/|]+", ar_value)
    elif isinstance(ar_value, list):
        raw_parts = []
        for item in ar_value:
            if isinstance(item, str):
                raw_parts.extend(re.split(r"[،,;؛/|]+", item))
    else:
        raw_parts = []

    cleaned = []
    seen = set()
    for p in raw_parts:
        s = p.strip()
        if s and contains_arabic(s) and s not in seen:
            seen.add(s)
            cleaned.append(s)
    return cleaned[:4]


def sanitize_key(raw: str) -> str:
    s = raw.strip().strip('"').strip("'").strip("`").strip()
    for prefix in ("GEMINI_API_KEY=", "API_KEY=", "Bearer "):
        if s.lower().startswith(prefix.lower()):
            s = s[len(prefix):].strip().strip('"').strip("'")
    return s


def detect_provider(key: str) -> str:
    if key.startswith("AIza"):
        return "gemini"
    if key.startswith("gsk_"):
        return "groq"
    if key.startswith("sk-or-"):
        return "openrouter"
    if key.startswith("sk-"):
        return "openai"
    if key.startswith("AQ.") or key.startswith("ya29.") or len(key) < 20:
        return "invalid"
    return "gemini"


def load_api_keys() -> List[str]:
    """Loads one or more API keys (comma/newline separated) from env or .env."""
    raw = os.environ.get("GEMINI_API_KEY", "").strip()
    if not raw and os.path.exists(".env"):
        with open(".env", encoding="utf-8") as f:
            for line in f:
                stripped = line.strip()
                if stripped.startswith("GEMINI_API_KEY="):
                    raw = stripped.split("=", 1)[1].strip()
                    break

    if not raw:
        return []

    keys = []
    for part in re.split(r"[,;\r\n]+", raw):
        k = sanitize_key(part)
        if not k or k in ("YOUR_GEMINI_API_KEY", "MY_GEMINI_API_KEY", "null"):
            continue
        provider = detect_provider(k)
        if provider == "invalid":
            print(
                f"  ! Skipping invalid token '{k[:8]}...': Gemini keys start with 'AIzaSy...' and Groq keys start with 'gsk_...'.",
                flush=True,
            )
            continue
        if k not in keys:
            keys.append(k)
    return keys


def resolve_input_dir(in_dir: str) -> str:
    """Checks IN_DIR first, then falls back to app/src/main/assets/data_import."""
    if all(os.path.exists(os.path.join(in_dir, f)) for f in FILES):
        return in_dir
    fallback = os.path.join("app", "src", "main", "assets", "data_import")
    if all(os.path.exists(os.path.join(fallback, f)) for f in FILES):
        print(f"Using source JSON files from: {fallback}", flush=True)
        return fallback
    return in_dir


def key_of(e: dict) -> str:
    return f"{e['text'].strip().lower()}||{e.get('pos', '').strip().lower()}"


def is_cache_complete(c: dict) -> bool:
    """Checks if a cached item already has valid app-compatible fields."""
    if not isinstance(c, dict):
        return False
    ar_list = normalize_arabic_meanings(c.get("ar", []))
    if not ar_list:
        return False
    ex_en = c.get("ex_en", [])
    ex_ar = c.get("ex_ar", [])
    if ex_en and (not ex_ar or not any(contains_arabic(x) for x in ex_ar)):
        return False
    return True


def load_all(in_dir: str) -> Dict[str, List[dict]]:
    data = {}
    for f in FILES:
        path = os.path.join(in_dir, f)
        if not os.path.exists(path):
            sys.exit(f"Missing file: {path}")
        with open(path, encoding="utf-8") as fp:
            data[f] = json.load(fp)
    return data


def save_cache_atomic(cache: dict) -> None:
    tmp_file = f"{CACHE_FILE}.tmp"
    with open(tmp_file, "w", encoding="utf-8") as fp:
        json.dump(cache, fp, ensure_ascii=False, indent=2)
    os.replace(tmp_file, CACHE_FILE)


def strip_code_fences(text: str) -> str:
    cleaned = text.strip()
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE | re.MULTILINE)
    cleaned = re.sub(r"\s*```$", "", cleaned, flags=re.MULTILINE)
    start = cleaned.find("[")
    end = cleaned.rfind("]")
    if start != -1 and end > start:
        cleaned = cleaned[start : end + 1]
    return cleaned.strip()


# ---------- FREE API ENGINE (dictionaryapi.dev + Google GTX) ----------
def gtx_translate_single_word(word: str, pos: str) -> List[str]:
    """Uses Google GTX Dictionary endpoint (dt=t&dt=bd) for POS-matched Arabic synonyms."""
    try:
        r = requests.get(
            "https://translate.googleapis.com/translate_a/single",
            params={"client": "gtx", "sl": "en", "tl": "ar", "dt": ["t", "bd"], "q": word},
            headers={"User-Agent": "Mozilla/5.0"},
            timeout=10,
        )
        if r.status_code != 200:
            return []
        root = r.json()
        meanings: List[str] = []
        target_pos = (pos or "").lower().strip()

        # 1. POS-specific dictionary terms from root[1]
        if isinstance(root, list) and len(root) > 1 and isinstance(root[1], list):
            for pos_block in root[1]:
                if not isinstance(pos_block, list) or len(pos_block) < 2:
                    continue
                block_pos = str(pos_block[0] or "").lower()
                if not target_pos or target_pos in block_pos or block_pos in target_pos:
                    for w in pos_block[1]:
                        if isinstance(w, str) and contains_arabic(w) and w.strip() not in meanings:
                            meanings.append(w.strip())
            if not meanings:
                for pos_block in root[1]:
                    if isinstance(pos_block, list) and len(pos_block) >= 2 and isinstance(pos_block[1], list):
                        for w in pos_block[1]:
                            if isinstance(w, str) and contains_arabic(w) and w.strip() not in meanings:
                                meanings.append(w.strip())

        # 2. Primary translation from root[0]
        if isinstance(root, list) and len(root) > 0 and isinstance(root[0], list):
            primary = "".join(seg[0] for seg in root[0] if isinstance(seg, list) and seg and isinstance(seg[0], str)).strip()
            if contains_arabic(primary) and primary not in meanings:
                meanings.insert(0, primary)

        return meanings[:4]
    except Exception:
        return []


def gtx_translate_sentences_batch(sentences: List[str]) -> List[str]:
    """Translates a list of English sentences to Arabic in a single GTX request."""
    if not sentences:
        return []
    delimiter = "\n ||| \n"
    joined = delimiter.join(sentences)
    try:
        r = requests.post(
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ar&dt=t",
            data={"q": joined},
            headers={"User-Agent": "Mozilla/5.0"},
            timeout=15,
        )
        if r.status_code == 200:
            root = r.json()
            if isinstance(root, list) and root and isinstance(root[0], list):
                full_ar = "".join(seg[0] for seg in root[0] if isinstance(seg, list) and seg and isinstance(seg[0], str))
                parts = [p.strip() for p in re.split(r"\s*\|\|\|\s*", full_ar)]
                if len(parts) == len(sentences):
                    return parts
    except Exception:
        pass

    # Per-sentence fallback
    results = []
    for s in sentences:
        try:
            r = requests.get(
                "https://translate.googleapis.com/translate_a/single",
                params={"client": "gtx", "sl": "en", "tl": "ar", "dt": "t", "q": s},
                headers={"User-Agent": "Mozilla/5.0"},
                timeout=10,
            )
            if r.status_code == 200:
                root = r.json()
                ar = "".join(seg[0] for seg in root[0] if isinstance(seg, list) and seg and isinstance(seg[0], str)).strip()
                results.append(ar)
            else:
                results.append("")
        except Exception:
            results.append("")
    return results


def fetch_free_dict_info(word: str, pos: str) -> Tuple[str, str, List[str]]:
    """Queries dictionaryapi.dev for (ipa, definition_en, examples_en)."""
    if " " in word.strip():
        return "", "", []
    try:
        r = requests.get(
            f"https://api.dictionaryapi.dev/api/v2/entries/en/{requests.utils.quote(word.strip().lower())}",
            headers={"User-Agent": "Mozilla/5.0"},
            timeout=8,
        )
        if r.status_code != 200:
            return "", "", []
        arr = r.json()
        if not isinstance(arr, list) or not arr:
            return "", "", []

        ipa = ""
        def_en = ""
        examples: List[str] = []
        target_pos = (pos or "").lower().strip()

        for entry in arr:
            if not ipa and isinstance(entry.get("phonetic"), str) and entry["phonetic"].strip():
                ipa = entry["phonetic"].strip()
            for ph in entry.get("phonetics", []):
                if isinstance(ph, dict) and ph.get("text"):
                    txt = ph["text"].strip()
                    audio = str(ph.get("audio") or "").lower()
                    if "us" in audio or not ipa:
                        ipa = txt
            for m in entry.get("meanings", []):
                m_pos = str(m.get("partOfSpeech") or "").lower()
                pos_match = not target_pos or target_pos in m_pos or m_pos in target_pos
                for d in m.get("definitions", []):
                    d_text = str(d.get("definition") or "").strip()
                    ex_text = str(d.get("example") or "").strip()
                    if d_text and (pos_match and not def_en or not def_en):
                        def_en = d_text
                    if ex_text and len(examples) < 2 and ex_text not in examples:
                        examples.append(ex_text)
        return ipa, def_en, examples
    except Exception:
        return "", "", []


def fallback_example_sentences(word: str, pos: str) -> List[str]:
    p = (pos or "").lower()
    if "verb" in p:
        return [
            f"We need to {word} before making a final decision.",
            f"Experienced professionals know how to {word} effectively.",
        ]
    if "adj" in p:
        return [
            f"This is a very {word} approach to solving the problem.",
            f"The results were {word} and clear to everyone.",
        ]
    if "adv" in p:
        return [
            f"She handled the situation {word} and professionally.",
            f"The system was {word} designed for daily use.",
        ]
    return [
        f"Understanding \"{word}\" is important in everyday communication.",
        f"Can you use \"{word}\" in a clear sentence?",
    ]


def translate_batch_free_api(items: List[dict]) -> List[dict]:
    """Translates and enriches a batch of items using Free Dictionary + Google GTX."""
    def process_one(item: dict):
        word = item["text"].strip()
        pos = item.get("pos", "").strip()
        ar_list = gtx_translate_single_word(word, pos)
        ipa = item.get("ipa", "").strip()
        def_en = item.get("definition_en", "").strip()
        ex_en = list(item.get("examples_en") or [])

        if not ipa or not def_en or not ex_en:
            dict_ipa, dict_def, dict_ex = fetch_free_dict_info(word, pos)
            if not ipa and dict_ipa:
                ipa = dict_ipa
            if not def_en and dict_def:
                def_en = dict_def
            if not ex_en and dict_ex:
                ex_en = dict_ex

        if not def_en:
            def_en = f"A {pos or 'term'} used in English communication: {word}."
        if not ex_en:
            ex_en = fallback_example_sentences(word, pos)

        return {
            "k": item["k"],
            "ar": ar_list,
            "ipa": ipa,
            "def_en": def_en,
            "ex_en": ex_en,
        }

    with ThreadPoolExecutor(max_workers=6) as pool:
        partial_results = list(pool.map(process_one, items))

    # Batch translate all English examples in one GTX request
    all_examples: List[str] = []
    ranges: List[Tuple[int, int]] = []
    for pr in partial_results:
        start = len(all_examples)
        all_examples.extend(pr["ex_en"])
        ranges.append((start, len(all_examples)))

    translated_examples = gtx_translate_sentences_batch(all_examples)

    out = []
    for idx, pr in enumerate(partial_results):
        s, e = ranges[idx]
        ex_ar = [x.strip() for x in translated_examples[s:e] if x.strip()]
        out.append(
            {
                "k": pr["k"],
                "ar": pr["ar"],
                "ipa": pr["ipa"],
                "def_en": pr["def_en"],
                "ex_en": pr["ex_en"],
                "ex_ar": ex_ar,
            }
        )
    return out


# ---------- AI ENGINE (Gemini + Groq + OpenRouter + OpenAI) ----------
def build_ai_prompt(items: List[dict]) -> str:
    return (
        "You are an expert English-Arabic dictionary editor for Arabic speakers learning English.\n"
        "For EACH item in the input JSON array, return a JSON object with EXACTLY these fields:\n"
        '  "k": the exact same "k" string from input,\n'
        '  "ar": array of 2 to 4 accurate, natural Modern Standard Arabic meanings for this part of speech (pos); '
        "for idioms/phrases give the natural Arabic meaning or equivalent expression,\n"
        '  "ipa": US IPA pronunciation between slashes like "/əˈbændən/" (keep existing if provided),\n'
        '  "def_en": if input "definition_en" is empty, write a clear, concise 1-sentence English definition matching pos; '
        'if input "definition_en" is already non-empty, return "",\n'
        '  "ex_en": if input "examples_en" is empty, provide an array of 2 short, natural English example sentences; '
        'if input "examples_en" is already non-empty, return its exact array,\n'
        '  "ex_ar": array of accurate Arabic translations corresponding 1-to-1 with "ex_en" (same order and length).\n'
        "Return ONLY a valid JSON array, no markdown fences, no commentary.\n\n"
        + json.dumps(items, ensure_ascii=False)
    )


def call_gemini(key: str, model: str, prompt: str) -> requests.Response:
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
    body = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"temperature": 0.2, "responseMimeType": "application/json"},
    }
    return requests.post(
        url,
        headers={"x-goog-api-key": key, "Content-Type": "application/json"},
        json=body,
        timeout=90,
    )


def call_openai_compatible(key: str, provider: str, prompt: str) -> requests.Response:
    if provider == "groq":
        url = "https://api.groq.com/openai/v1/chat/completions"
        model = GROQ_MODEL
    elif provider == "openrouter":
        url = "https://openrouter.ai/api/v1/chat/completions"
        model = OPENROUTER_MODEL
    else:
        url = "https://api.openai.com/v1/chat/completions"
        model = OPENAI_MODEL

    body = {
        "model": model,
        "temperature": 0.2,
        "messages": [
            {"role": "system", "content": "Return ONLY a valid JSON array. Do not wrap in markdown."},
            {"role": "user", "content": prompt},
        ],
    }
    return requests.post(
        url,
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        json=body,
        timeout=90,
    )


def ask(items: List[dict], api_keys: List[str], allow_free_fallback: bool = True) -> Optional[List[dict]]:
    """Translates items using configured AI keys with per-(key, model) cooldowns and optional Free API fallback."""
    if not api_keys:
        if allow_free_fallback:
            return translate_batch_free_api(items)
        return None

    prompt = build_ai_prompt(items)

    for attempt in range(4):
        now = time.monotonic()
        tried_any = False

        for key_idx, key in enumerate(api_keys):
            provider = detect_provider(key)
            models = GEMINI_MODELS if provider == "gemini" else [provider]

            for model in models:
                slot = f"{key_idx}:{model}"
                if COOLDOWNS.get(slot, 0) > now:
                    continue

                tried_any = True
                try:
                    if provider == "gemini":
                        r = call_gemini(key, model, prompt)
                    else:
                        r = call_openai_compatible(key, provider, prompt)

                    if r.status_code == 200:
                        data = r.json()
                        if provider == "gemini":
                            text = data["candidates"][0]["content"]["parts"][0]["text"]
                        else:
                            text = data["choices"][0]["message"]["content"]
                        out = json.loads(strip_code_fences(text))
                        if isinstance(out, list):
                            return out

                    elif r.status_code == 429:
                        COOLDOWNS[slot] = time.monotonic() + 65.0
                        print(f"  [429] Rate limit on key#{key_idx + 1} ({model}) -> 65s cooldown, rotating...", flush=True)
                        continue

                    elif r.status_code in (400, 401, 403, 404):
                        COOLDOWNS[slot] = time.monotonic() + 3600.0
                        print(f"  [{r.status_code}] Skipping key#{key_idx + 1} ({model}): {r.text[:160]}", flush=True)
                        continue

                    elif r.status_code in (500, 502, 503, 504):
                        print(f"  [{r.status_code}] Server busy on {model}, trying next...", flush=True)
                        continue
                except Exception as ex:
                    print(f"  ! Request error on {model}: {ex}", flush=True)

        if allow_free_fallback:
            print("  -> All AI keys/models on cooldown; auto-falling back to Free Dictionary + GTX API for this batch!", flush=True)
            return translate_batch_free_api(items)

        wait_sec = min(10 * (attempt + 1), 45)
        if not tried_any:
            print(f"  All keys cooling down, waiting {wait_sec}s...", flush=True)
        time.sleep(wait_sec)

    if len(items) > 1:
        print(f"  Splitting batch of {len(items)} into two smaller batches...", flush=True)
        mid = len(items) // 2
        r1 = ask(items[:mid], api_keys, allow_free_fallback)
        r2 = ask(items[mid:], api_keys, allow_free_fallback)
        if r1 is not None and r2 is not None:
            return r1 + r2
        return r1 or r2

    return None


def main():
    parser = argparse.ArgumentParser(description="Translate the 7 WordBooks JSON files to match the Android app format.")
    parser.add_argument("--free-only", action="store_true", help="Use Free Dictionary + Google GTX API only (no AI key needed)")
    parser.add_argument("--no-free-fallback", action="store_true", help="Disable automatic Free API fallback when AI keys hit 429")
    parser.add_argument("--batch", type=int, default=BATCH, help=f"Entries per batch (default: {BATCH})")
    parser.add_argument("--in-dir", type=str, default=IN_DIR, help="Directory containing the 7 source JSON files")
    parser.add_argument("--out-dir", type=str, default=OUT_DIR, help="Output directory for translated JSON files")
    args = parser.parse_args()

    api_keys = [] if args.free_only else load_api_keys()
    if not api_keys and not args.free_only:
        print(
            "No GEMINI_API_KEY found in environment or .env -> Running in Free Dictionary + Google GTX mode!\n"
            "(Tip: Set GEMINI_API_KEY='AIzaSy...' to use Gemini with automatic Free API fallback.)",
            flush=True,
        )

    resolved_in_dir = resolve_input_dir(args.in_dir)
    data = load_all(resolved_in_dir)
    cache: dict = json.load(open(CACHE_FILE, encoding="utf-8")) if os.path.exists(CACHE_FILE) else {}

    # Build unique (text, pos) items to translate/enrich
    todo, seen = [], set()
    for entries in data.values():
        for e in entries:
            # Never overwrite user-edited entries
            if e.get("arabic_source") == "user":
                continue
            k = key_of(e)
            if k in seen:
                continue
            if k in cache and is_cache_complete(cache[k]):
                continue
            seen.add(k)

            existing_ex_en = [s.strip() for s in (e.get("examples_en") or "").split("|") if s.strip()]
            todo.append(
                {
                    "k": k,
                    "text": e["text"].strip(),
                    "pos": e.get("pos", "").strip(),
                    "level": e.get("level", "").strip(),
                    "ipa": e.get("ipa", "").strip(),
                    "definition_en": e.get("definition_en", "").strip(),
                    "examples_en": existing_ex_en,
                }
            )

    print(f"{len(cache)} cached, {len(todo)} unique (word, pos) entries left to translate.", flush=True)

    batch_size = max(1, args.batch)
    for i in range(0, len(todo), batch_size):
        batch = todo[i : i + batch_size]
        res = ask(batch, api_keys=api_keys, allow_free_fallback=not args.no_free_fallback)
        if res is None:
            print("Batch failed after retries. Progress is saved in cache.json; run again to resume.", flush=True)
            break

        input_by_k = {item["k"]: item for item in batch}
        saved_in_batch = 0

        for r in res:
            if not isinstance(r, dict) or not r.get("k"):
                continue
            k = r["k"]
            src_item = input_by_k.get(k, {})
            ar_list = normalize_arabic_meanings(r.get("ar", []))
            if not ar_list:
                continue

            ipa_val = (r.get("ipa") or src_item.get("ipa") or "").strip()
            def_en_val = (src_item.get("definition_en") or r.get("def_en") or r.get("definition_en") or "").strip()

            ex_en_raw = src_item.get("examples_en") or r.get("ex_en") or []
            if isinstance(ex_en_raw, str):
                ex_en_list = [s.strip() for s in ex_en_raw.split("|") if s.strip()]
            elif isinstance(ex_en_raw, list):
                ex_en_list = [str(s).strip() for s in ex_en_raw if str(s).strip()]
            else:
                ex_en_list = []

            ex_ar_raw = r.get("ex_ar") or r.get("examples_ar") or []
            if isinstance(ex_ar_raw, str):
                ex_ar_list = [s.strip() for s in ex_ar_raw.split("|") if s.strip()]
            elif isinstance(ex_ar_raw, list):
                ex_ar_list = [str(s).strip() for s in ex_ar_raw if str(s).strip()]
            else:
                ex_ar_list = []

            cache[k] = {
                "ar": ar_list,
                "ipa": ipa_val,
                "def_en": def_en_val,
                "ex_en": ex_en_list,
                "ex_ar": ex_ar_list,
            }
            saved_in_batch += 1

        save_cache_atomic(cache)
        print(f"  {min(i + batch_size, len(todo))}/{len(todo)} processed (+{saved_in_batch} saved)", flush=True)
        time.sleep(PAUSE if api_keys else FREE_PAUSE)

    # Write translated JSON files in the exact schema used by the Android app
    os.makedirs(args.out_dir, exist_ok=True)
    missing = 0
    for f, entries in data.items():
        for e in entries:
            if e.get("arabic_source") == "user" and (e.get("arabic") or "").strip():
                continue
            c = cache.get(key_of(e))
            if c and c.get("ar"):
                ar_clean = normalize_arabic_meanings(c["ar"])
                if ar_clean:
                    # App standard: Arabic comma "، " between meanings
                    e["arabic"] = "، ".join(ar_clean)
                    if not e.get("arabic_source"):
                        e["arabic_source"] = "ai"
                if c.get("ipa") and not (e.get("ipa") or "").strip():
                    e["ipa"] = c["ipa"].strip()
                if c.get("def_en") and not (e.get("definition_en") or "").strip():
                    e["definition_en"] = c["def_en"].strip()
                if c.get("ex_en") and not (e.get("examples_en") or "").strip():
                    e["examples_en"] = " | ".join(c["ex_en"])
                if c.get("ex_ar"):
                    e["examples_ar"] = " | ".join(c["ex_ar"])
            elif not (e.get("arabic") or "").strip():
                missing += 1

        out_path = os.path.join(args.out_dir, f)
        with open(out_path, "w", encoding="utf-8") as fp:
            json.dump(entries, fp, ensure_ascii=False, indent=2)

    print(f"\nWritten 7 app-ready JSON files to '{args.out_dir}/'. Entries still without Arabic: {missing}", flush=True)
    if missing:
        print("Run the script again to finish the remaining entries.", flush=True)


if __name__ == "__main__":
    main()

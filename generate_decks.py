"""
generate_decks.py
1) Copies original JSON files from app/src/main/assets/data_import/ to data_import_backup/
   (ONLY if the backup file does not exist yet). Never modifies files in assets/data_import/.
2) Writes all generated and translated JSON files to generated/ so you can inspect them
   before copying them to app/src/main/assets/data_import/.
3) Uses fixed, stable ID ranges for generated decks:
   - ielts_core: starts at 100001
   - it_industry: starts at 200001
   Re-running continues from generated/<deck>.json without duplicate IDs or duplicate words.
4) Enriches the 7 core JSON files into generated/<file>.json while preserving existing IDs (1..9862)
   and never overwriting entries where arabic_source == "user".

Requirements:
    pip install google-genai
    export GEMINI_API_KEY="your_key_here"   # or place GEMINI_API_KEY=... in .env

Usage:
    python3 generate_decks.py                       # runs both deck generation and translation
    python3 generate_decks.py --task decks          # only generate ielts_core & it_industry
    python3 generate_decks.py --task translate      # only translate the 7 core JSON files
    python3 generate_decks.py --dry-run             # verify backup, ID ranges, and request counts
"""

import argparse
import json
import math
import os
import re
import shutil
import time
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

# ---------- PATHS & ID RANGES ----------
SOURCE_DIR = Path("app/src/main/assets/data_import")
BACKUP_DIR = Path("data_import_backup")
OUTPUT_DIR = Path("generated")

CORE_FILES = [
    "oxford5000_part1.json",
    "oxford5000_part2.json",
    "oxford5000_part3.json",
    "oxford_opal.json",
    "oxford_phrases.json",
    "everyday_expressions.json",
    "essential_idioms.json",
]

DECK_ID_RANGES = {
    "ielts_core": 100001,
    "it_industry": 200001,
}

# Use gemini-flash-latest (with fallback to gemini-3.0-flash)
MODEL_CANDIDATES = ["gemini-flash-latest", "gemini-3.0-flash", "gemini-3.5-flash"]
BATCH_SIZE = 40
SLEEP_BETWEEN_CALLS = 1.2


# ---------- API KEY RESOLUTION ----------
def load_api_key() -> str:
    """Loads GEMINI_API_KEY from environment variable or .env file."""
    env_key = os.environ.get("GEMINI_API_KEY", "").strip()
    if env_key and env_key != "MY_GEMINI_API_KEY":
        return env_key

    for env_file in [Path(".env"), Path(".env.local")]:
        if env_file.exists():
            for line in env_file.read_text(encoding="utf-8").splitlines():
                stripped = line.strip()
                if stripped.startswith("#") or "=" not in stripped:
                    continue
                k, v = stripped.split("=", 1)
                if k.strip() == "GEMINI_API_KEY":
                    val = v.strip().strip('"').strip("'")
                    if val and val != "MY_GEMINI_API_KEY":
                        return val
    return ""


# ---------- STEP 0: SAFE BACKUP & OUTPUT DIR ----------
def ensure_backup_and_output_dirs() -> None:
    """
    Copies each of the 7 original JSON files from app/src/main/assets/data_import/
    to data_import_backup/ ONLY if the backup file does not exist yet.
    Ensures generated/ output directory exists.
    Never modifies any file inside app/src/main/assets/data_import/.
    """
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    for fname in CORE_FILES:
        src_path = SOURCE_DIR / fname
        backup_path = BACKUP_DIR / fname
        if not src_path.exists():
            raise FileNotFoundError(f"Required source file missing: {src_path}")
        if not backup_path.exists():
            shutil.copy2(src_path, backup_path)
            print(f"[backup] Copied original {src_path} -> {backup_path}")
        else:
            print(f"[backup] Backup already exists, untouched: {backup_path}")


# ---------- HELPERS ----------
def extract_json_array(raw_text: str) -> List[dict]:
    text = (raw_text or "").strip()
    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text, flags=re.MULTILINE).strip()
    start = text.find("[")
    end = text.rfind("]")
    if start != -1 and end > start:
        text = text[start : end + 1]
    return json.loads(text)


def ask_json(client, prompt: str, retries: int = 3) -> List[dict]:
    from google.genai import types

    last_err: Optional[Exception] = None
    for attempt in range(retries):
        for model_name in MODEL_CANDIDATES:
            try:
                resp = client.models.generate_content(
                    model=model_name,
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json",
                        temperature=0.3,
                    ),
                )
                return extract_json_array(resp.text)
            except Exception as e:
                last_err = e
                err_str = str(e)
                if "404" in err_str or "not found" in err_str.lower() or "no longer available" in err_str.lower():
                    continue
                break
        wait = 2 ** attempt
        print(f"  ! API error ({last_err}), retrying in {wait}s...")
        time.sleep(wait)
    print(f"  ✗ Failed after {retries} retries: {last_err}")
    return []


def save_json_to_generated(filename: str, rows: List[dict]) -> Path:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out_path = OUTPUT_DIR / filename
    tmp_path = OUTPUT_DIR / f"{filename}.tmp"
    with open(tmp_path, "w", encoding="utf-8") as f:
        json.dump(rows, f, ensure_ascii=False, indent=2)
    tmp_path.replace(out_path)
    return out_path


# ---------- PART 1: GENERATE NEW DECKS IN generated/ ----------
IELTS_TOPICS = [
    ("Academic Writing Task 1 - charts, trends, comparisons", 200),
    ("Academic Writing Task 2 - essays, arguments, linking", 250),
    ("Environment, climate change, energy, sustainability", 200),
    ("Education, research, universities, learning", 200),
    ("Technology, science, innovation, AI", 200),
    ("Health, medicine, lifestyle, psychology", 200),
    ("Society, culture, globalization, demographics", 200),
    ("Economy, business, work, employment, finance", 200),
    ("Government, law, policy, crime, justice", 180),
    ("Media, communication, arts, literature", 180),
    ("Urbanization, transport, housing, infrastructure", 190),
]  # Total target = 2,200 entries -> ceil(2200/40) = 55 API requests

IT_TOPICS = [
    ("Programming fundamentals, algorithms, data structures", 180),
    ("Software engineering, architecture, design patterns, clean code", 160),
    ("Web development - frontend, backend, APIs, HTTP, REST", 160),
    ("Databases, SQL, NoSQL, indexing, transactions, caching", 140),
    ("Cloud computing, DevOps, Docker, Kubernetes, CI/CD", 160),
    ("Cybersecurity, encryption, authentication, vulnerabilities", 140),
    ("Networking, protocols, DNS, TCP/IP, load balancing", 130),
    ("Operating systems, Linux, processes, memory, concurrency", 130),
    ("Mobile development - Android, Kotlin, Jetpack Compose, UI/UX", 140),
    ("Artificial intelligence, machine learning, LLMs, data science", 140),
    ("Git, version control, code review, testing, debugging", 110),
    ("Agile, Scrum, product management, tech workplace phrases", 110),
]  # Total target = 1,700 entries -> 43 API requests


GENERATION_PROMPT = """
Generate {count} unique English vocabulary entries for the deck "{deck}"
focused on the topic: "{topic}".
Target learner: Arabic speaker preparing for {purpose}.

Avoid these already-used words: {avoid}

Return a JSON array of objects with EXACTLY these fields:
- "text": the word or short technical term (lowercase unless acronym like API, SQL)
- "type": "word" | "academic" | "phrase"
- "pos": "noun" | "verb" | "adjective" | "adverb" | "phrase"
- "level": CEFR level "B1" | "B2" | "C1"
- "ipa": US IPA like "/kənˈsɪstənt/"
- "definition_en": clear, concise English definition (1 sentence)
- "examples_en": TWO natural example sentences separated by " | "
- "arabic": natural, accurate Arabic meanings (2-3 synonyms separated by "، ")
- "examples_ar": Arabic translation of the two examples, separated by " | "
"""


def compute_next_id_for_deck(deck_name: str, rows: List[dict]) -> int:
    start_id = DECK_ID_RANGES[deck_name]
    existing_ids = [
        int(r["id"])
        for r in rows
        if str(r.get("id", "")).isdigit() and int(r["id"]) >= start_id
    ]
    return (max(existing_ids) + 1) if existing_ids else start_id


def build_deck(
    client,
    deck_name: str,
    purpose: str,
    topics: List[Tuple[str, int]],
    batch_size: int = BATCH_SIZE,
    limit_batches: Optional[int] = None,
) -> None:
    out_file = OUTPUT_DIR / f"{deck_name}.json"
    start_id = DECK_ID_RANGES[deck_name]

    rows: List[dict] = []
    if out_file.exists():
        rows = json.loads(out_file.read_text(encoding="utf-8"))

    seen_words: Set[str] = {
        str(r.get("text", "")).strip().lower()
        for r in rows
        if r.get("text")
    }
    used_ids: Set[int] = {
        int(r["id"])
        for r in rows
        if str(r.get("id", "")).isdigit()
    }
    next_id = compute_next_id_for_deck(deck_name, rows)

    total_target = sum(c for _, c in topics)
    print(
        f"\n=== Building {deck_name} in {out_file} "
        f"(existing: {len(rows)}/{total_target}, ID range >= {start_id}, next_id: {next_id}) ==="
    )

    batches_run = 0
    for topic, target_count in topics:
        remaining = target_count
        while remaining > 0 and len(rows) < total_target:
            if limit_batches is not None and batches_run >= limit_batches:
                print(f"Reached --limit-batches={limit_batches}, stopping cleanly.")
                return

            n = min(batch_size, remaining, total_target - len(rows))
            avoid_sample = list(seen_words)[-120:]
            prompt = GENERATION_PROMPT.format(
                count=n,
                deck=deck_name,
                topic=topic,
                purpose=purpose,
                avoid=", ".join(avoid_sample) if avoid_sample else "none",
            )
            items = ask_json(client, prompt)
            batches_run += 1
            if not items:
                break

            added = 0
            for it in items:
                word = (it.get("text") or "").strip()
                if not word or word.lower() in seen_words:
                    continue
                while next_id in used_ids:
                    next_id += 1

                seen_words.add(word.lower())
                used_ids.add(next_id)
                rows.append(
                    {
                        "id": next_id,
                        "type": it.get("type", "word"),
                        "deck": deck_name,
                        "text": word,
                        "pos": it.get("pos", ""),
                        "level": it.get("level", "B2"),
                        "ipa": it.get("ipa", ""),
                        "definition_en": it.get("definition_en", ""),
                        "note_en": "",
                        "examples_en": it.get("examples_en", ""),
                        "examples_ar": it.get("examples_ar", ""),
                        "definition_url": "",
                        "audio_url": "",
                        "arabic": it.get("arabic", ""),
                        "arabic_source": "ai" if (it.get("arabic") or "").strip() else "",
                    }
                )
                next_id += 1
                added += 1

            remaining -= n
            save_json_to_generated(f"{deck_name}.json", rows)
            print(f"  [{deck_name}] +{added} (total {len(rows)}/{total_target}) - {topic[:40]}")
            time.sleep(SLEEP_BETWEEN_CALLS)


# ---------- PART 2: TRANSLATE & ENRICH EXISTING 7 FILES INTO generated/ ----------
TRANSLATE_PROMPT = """
You are building an English->Arabic dictionary for Arabic learners.
For each item in the input JSON array, return an object with:
- "id": SAME id as input (keep exact integer/value)
- "arabic": accurate, natural Arabic meanings for this part of speech (2-3 common meanings separated by "، ")
- "ipa": US IPA pronunciation like "/əˈbændən/"
- "definition_en": if input definition_en is empty, write a short clear 1-sentence English definition; otherwise return ""
- "examples_en": if input examples_en is empty, write 2 short natural example sentences separated by " | "; otherwise return ""
- "examples_ar": Arabic translation of the examples (separated by " | ")

Input:
{items}

Return ONLY a JSON array.
"""


def load_rows_for_translation(filename: str) -> List[dict]:
    """
    If generated/<filename> already exists (from a previous checkpoint), resume from it.
    Otherwise read the untouched original from app/src/main/assets/data_import/<filename>.
    """
    generated_path = OUTPUT_DIR / filename
    if generated_path.exists():
        return json.loads(generated_path.read_text(encoding="utf-8"))
    source_path = SOURCE_DIR / filename
    return json.loads(source_path.read_text(encoding="utf-8"))


def translate_file(
    client,
    filename: str,
    batch_size: int = BATCH_SIZE,
    limit_batches: Optional[int] = None,
) -> None:
    rows = load_rows_for_translation(filename)
    # Never overwrite user edits or already-translated entries
    todo = [
        r
        for r in rows
        if not (r.get("arabic") or "").strip()
        and r.get("arabic_source") != "user"
    ]
    print(f"\n=== Translating {filename} -> generated/{filename}: {len(todo)}/{len(rows)} need Arabic ===")
    if not todo:
        # Ensure output file is still written to generated/ so all 7 files exist in generated/
        save_json_to_generated(filename, rows)
        return

    by_id = {str(r["id"]): r for r in rows}
    batches_run = 0
    for i in range(0, len(todo), batch_size):
        if limit_batches is not None and batches_run >= limit_batches:
            print(f"Reached --limit-batches={limit_batches}, stopping cleanly.")
            return

        batch = todo[i : i + batch_size]
        slim = [
            {
                "id": r["id"],
                "text": r["text"],
                "pos": r.get("pos", ""),
                "type": r.get("type", ""),
                "definition_en": r.get("definition_en", ""),
                "examples_en": r.get("examples_en", ""),
            }
            for r in batch
        ]
        result = ask_json(client, TRANSLATE_PROMPT.format(items=json.dumps(slim, ensure_ascii=False)))
        batches_run += 1

        updated_in_batch = 0
        for item in result:
            row = by_id.get(str(item.get("id")))
            if not row:
                continue
            # Never overwrite user-edited Arabic
            if row.get("arabic_source") == "user":
                continue

            arabic_val = (item.get("arabic") or "").strip()
            if arabic_val:
                row["arabic"] = arabic_val
                updated_in_batch += 1
            if item.get("ipa") and not row.get("ipa"):
                row["ipa"] = item["ipa"].strip()
            if not row.get("definition_en") and item.get("definition_en"):
                row["definition_en"] = item["definition_en"].strip()
            if not row.get("examples_en") and item.get("examples_en"):
                row["examples_en"] = item["examples_en"].strip()
            if item.get("examples_ar") and not row.get("examples_ar"):
                row["examples_ar"] = item["examples_ar"].strip()

        save_json_to_generated(filename, rows)
        done = sum(1 for r in rows if (r.get("arabic") or "").strip())
        print(f"  [{filename}] {done}/{len(rows)} translated (+{updated_in_batch} in batch)")
        time.sleep(SLEEP_BETWEEN_CALLS)


def print_request_estimates(batch_size: int) -> None:
    print("\n=== Request Count Breakdown (batch_size = %d) ===" % batch_size)
    ielts_reqs = sum(math.ceil(c / batch_size) for _, c in IELTS_TOPICS)
    it_reqs = sum(math.ceil(c / batch_size) for _, c in IT_TOPICS)
    print(f"  1) Generate ielts_core (2,200 words, IDs 100001..): {ielts_reqs} requests")
    print(f"  2) Generate it_industry (1,700 words, IDs 200001..): {it_reqs} requests")

    translate_total_reqs = 0
    for fname in CORE_FILES:
        rows = load_rows_for_translation(fname)
        todo = sum(1 for r in rows if not (r.get("arabic") or "").strip() and r.get("arabic_source") != "user")
        reqs = math.ceil(todo / batch_size)
        translate_total_reqs += reqs
        print(f"  3) Translate {fname:28s} ({todo:4d}/{len(rows):4d} empty): {reqs:3d} requests")

    print(f"  ------------------------------------------------------------------")
    print(
        f"  Total API requests: {ielts_reqs + it_reqs + translate_total_reqs} "
        f"(Decks: {ielts_reqs + it_reqs}, Translation: {translate_total_reqs})"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate decks and Arabic translations safely into generated/")
    parser.add_argument(
        "--task",
        choices=["all", "decks", "translate"],
        default="all",
        help="Which task to run: all (default), decks (ielts_core & it_industry), or translate (7 core files)",
    )
    parser.add_argument("--batch-size", type=int, default=BATCH_SIZE, help="Items per Gemini request (default: 40)")
    parser.add_argument("--limit-batches", type=int, default=None, help="Stop after N batches per file/deck")
    parser.add_argument("--dry-run", action="store_true", help="Create backup and print request estimates without calling Gemini")
    args = parser.parse_args()

    # Step 1: Always verify source files and create one-time backup in data_import_backup/
    ensure_backup_and_output_dirs()
    print_request_estimates(args.batch_size)

    if args.dry_run:
        print("\n[dry-run] Backup verified and request counts calculated. No API calls made.")
        return

    api_key = load_api_key()
    if not api_key:
        raise SystemExit(
            "\nERROR: GEMINI_API_KEY not found.\n"
            "Pass it via environment variable:\n"
            '  GEMINI_API_KEY="AIza..." python3 generate_decks.py\n'
            "or add GEMINI_API_KEY=AIza... to .env"
        )

    from google import genai

    client = genai.Client(api_key=api_key)

    if args.task in ("all", "decks"):
        build_deck(
            client,
            "ielts_core",
            "the IELTS Academic exam (Band 7+)",
            IELTS_TOPICS,
            batch_size=args.batch_size,
            limit_batches=args.limit_batches,
        )
        build_deck(
            client,
            "it_industry",
            "working in software engineering & IT",
            IT_TOPICS,
            batch_size=args.batch_size,
            limit_batches=args.limit_batches,
        )

    if args.task in ("all", "translate"):
        for fname in CORE_FILES:
            translate_file(
                client,
                fname,
                batch_size=args.batch_size,
                limit_batches=args.limit_batches,
            )

    print("\nAll requested tasks completed! Check outputs in generated/")


if __name__ == "__main__":
    main()

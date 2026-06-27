#!/usr/bin/env python3
"""Focused rhyme-quality regression check for The Top Flow.

This mirrors the app's lightweight rhyme engine closely enough to catch known
ranking regressions without starting Android.
"""

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
CMU_PATH = ROOT / "app" / "src" / "main" / "assets" / "cmudict.dict"

BUCKET_EXACT = 0
BUCKET_NEAR = 1
BUCKET_PHRASE = 2
BUCKET_FALLBACK = 3

COMMON_RHYME_WORDS = [
    "time", "rhyme", "dime", "climb", "lime", "mine", "shine", "line", "fine", "crime",
    "grind", "blind", "kind", "mind", "wind", "sign", "design", "behind",
    "night", "light", "flight", "bright", "might", "tight", "right", "sight",
    "flow", "go", "show", "glow", "throw", "slow", "grow", "blow",
    "way", "play", "say", "stay", "day", "made", "fade", "shade", "gray", "spray", "sway", "delay",
    "heart", "start", "part", "smart", "chart", "art", "hard", "yard",
    "pain", "rain", "chain", "brain", "gain", "train", "plane", "strain",
    "soul", "cold", "gold", "roll", "control", "whole", "goal", "bowl",
    "voice", "choice", "noise", "poise", "boys", "toy", "joy", "ploy",
    "near", "clear", "fear", "dear", "here", "year", "peer", "tear",
    "heat", "beat", "street", "sweet", "fleet", "meet", "seat", "feat",
    "cover", "lover", "hover", "running", "runnin", "proving", "grooving",
    "pullin", "pulling", "coolant", "woolen", "bullet", "couldn't", "shouldn't", "wouldn't",
    "movin", "moving", "proven", "losing", "choosing", "ruin", "fluid", "student",
]

COMMON_RHYME_PHRASES = [
    "keep it movin", "fluid motion", "coolant flow", "pullin through",
    "all day", "same way", "late night", "bright lights",
    "on my mind", "in due time", "stay in line", "chain reaction",
]

VOWEL_PHONES = {
    "AA", "AE", "AH", "AO", "AW", "AY", "EH", "ER", "EY",
    "IH", "IY", "OW", "OY", "UH", "UW",
}

cmu_phones = {}
cmu_rhyme_index = {}
cmu_family_index = {}
cmu_dictionary_words = set()


def normalize_word(word):
    if word is None:
        return ""
    out = re.sub(r"[^a-z']", "", word.lower())
    return out.strip("'")


def normalize_cmu_word(word):
    word = word.split("(", 1)[0]
    if not re.fullmatch(r"[A-Za-z']+", word):
        return ""
    return normalize_word(word)


def is_vowel_phone(phone):
    return re.sub(r"[012]", "", phone or "") in VOWEL_PHONES


def rhyme_key_from_phones(phones):
    return re.sub(r"[012]", "", phones or "").strip()


def cmu_rhyme_part_from_phones(phones):
    parts = phones.strip().split()
    stressed = -1
    for i in range(len(parts) - 1, -1, -1):
        if is_vowel_phone(parts[i]) and (parts[i].endswith("1") or parts[i].endswith("2")):
            stressed = i
            break
    if stressed < 0:
        for i in range(len(parts) - 1, -1, -1):
            if is_vowel_phone(parts[i]):
                stressed = i
                break
    return "" if stressed < 0 else " ".join(parts[stressed:])


def phone_rhyme_info(phones):
    if not phones:
        return None
    rhyme = cmu_rhyme_part_from_phones(phones)
    if not rhyme:
        return None
    parts = rhyme.split()
    vowel = ""
    coda = []
    for part in parts:
        clean = re.sub(r"[012]", "", part)
        if not vowel and is_vowel_phone(part):
            vowel = clean
        elif vowel and not is_vowel_phone(part):
            coda.append(clean)
    if not vowel:
        return None
    syllables = sum(1 for part in phones.split() if is_vowel_phone(part))
    coda_key = " ".join(coda)
    return {
        "rhyme_key": rhyme_key_from_phones(rhyme),
        "vowel_key": vowel,
        "coda_key": coda_key,
        "family_key": vowel + ":" + coda_key,
        "syllable_count": max(1, syllables),
    }


def add_index(index, key, word):
    if not key:
        return
    words = index.setdefault(key, [])
    if len(words) < 240 and word not in words:
        words.append(word)


def add_cmu_entry(word, phones):
    values = cmu_phones.setdefault(word, [])
    if phones not in values:
        values.append(phones)
    rhyme = cmu_rhyme_part_from_phones(phones)
    if rhyme:
        add_index(cmu_rhyme_index, rhyme_key_from_phones(rhyme), word)
    info = phone_rhyme_info(phones)
    if info:
        add_index(cmu_family_index, info["family_key"], word)


def guessed_onset_phones(stem):
    stem = normalize_word(stem)
    if stem.endswith("ll"):
        return "P UH1 L"
    if stem.endswith("l"):
        return "K UW1 L"
    if stem.endswith("v"):
        return "M UW1 V"
    return "AH1"


def slang_phones(word, include_slang=True):
    word = normalize_word(word)
    if not word or not include_slang:
        return ""
    if word.endswith("in'"):
        word = word[:-1]
    if word.endswith("ing") and len(word) > 4:
        return guessed_onset_phones(word[:-3]) + " IH0 N"
    if word.endswith("in") and len(word) > 3:
        return guessed_onset_phones(word[:-2]) + " IH0 N"
    if len(word) > 5 and word.endswith(("ant", "ent", "int", "unt")):
        return guessed_onset_phones(word[:-3]) + " IH0 N"
    if word == "coolant":
        return "K UW1 L IH0 N"
    if word == "pullin":
        return "P UH1 L IH0 N"
    return ""


def guessed_ing_phones(word):
    word = normalize_word(word)
    if word.endswith("ing") and len(word) > 4:
        return guessed_onset_phones(word[:-3]) + " IH0 NG"
    return ""


def load_cmu():
    for line in CMU_PATH.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith(";;;"):
            continue
        parts = line.strip().split(None, 1)
        if len(parts) != 2:
            continue
        word = normalize_cmu_word(parts[0])
        if not word or len(word) > 18:
            continue
        add_cmu_entry(word, parts[1].strip())
        cmu_dictionary_words.add(word)
    for word in COMMON_RHYME_WORDS:
        normalized = normalize_word(word)
        phones = slang_phones(normalized)
        if normalized and phones and normalized not in cmu_dictionary_words:
            add_cmu_entry(normalized, phones)


def candidate_rhyme_word(candidate):
    parts = re.split(r"[^a-z']+", (candidate or "").lower())
    for part in reversed(parts):
        word = normalize_word(part)
        if word:
            return word
    return normalize_word(candidate)


def cmu_rhyme_infos(word):
    word = normalize_word(word)
    out = []
    for phones in cmu_phones.get(word, []):
        info = phone_rhyme_info(phones)
        if info:
            out.append(info)
    guessed_ing = guessed_ing_phones(word)
    if guessed_ing and word not in cmu_dictionary_words:
        info = phone_rhyme_info(guessed_ing)
        if info:
            out.append(info)
    slang = slang_phones(word)
    if slang and word not in cmu_dictionary_words:
        info = phone_rhyme_info(slang)
        if info:
            out.append(info)
    return out


def phone_tail_overlap(a, b):
    x = a.strip().split() if a else []
    y = b.strip().split() if b else []
    overlap = 0
    while overlap < len(x) and overlap < len(y) and x[-1 - overlap] == y[-1 - overlap]:
        overlap += 1
    return overlap


def same_final_phone(a, b):
    x = a.strip().split() if a else []
    y = b.strip().split() if b else []
    return bool(x and y and x[-1] == y[-1])


def hip_hop_near_tail(word):
    word = normalize_word(word)
    if word.endswith("ing") and len(word) > 4:
        word = word[:-3] + "in"
    if len(word) > 5 and word.endswith(("ant", "ent", "int", "unt")):
        word = word[:-3] + "in"
    if word.endswith("in") or word.endswith("en"):
        idx = max(word.rfind("l"), word.rfind("m"), word.rfind("v"))
        return word[idx:min(len(word), idx + 3)] if idx >= 0 else "in"
    return ""


def near_slang_family(a, b):
    x = hip_hop_near_tail(a)
    y = hip_hop_near_tail(b)
    return bool(x and x == y)


def common_rhyme_bias(candidate):
    word = candidate_rhyme_word(candidate)
    return 18 if any(normalize_word(common) == word for common in COMMON_RHYME_WORDS) else 0


def slang_variant_key(word):
    word = normalize_word(word)
    if word.endswith("ing") and len(word) > 4:
        return word[:-3] + "in"
    if word.endswith("in") and len(word) > 3:
        return word
    return ""


def slang_variant_pair(a, b):
    x = slang_variant_key(a)
    y = slang_variant_key(b)
    return bool(x and x == y and normalize_word(a) != normalize_word(b))


def bucket_penalty(bucket):
    return [0, 10, 20, 44][bucket]


def cmu_relation(base_infos, candidate_infos):
    if not base_infos or not candidate_infos:
        return -1
    best = -1
    for a in base_infos:
        for b in candidate_infos:
            if a["vowel_key"] != b["vowel_key"]:
                continue
            if a["rhyme_key"] == b["rhyme_key"]:
                return BUCKET_EXACT
            if a["coda_key"] == b["coda_key"] or phone_tail_overlap(a["rhyme_key"], b["rhyme_key"]) >= 2:
                if abs(a["syllable_count"] - b["syllable_count"]) <= 2:
                    best = BUCKET_NEAR
    return best


def add_candidate(pool, word, bucket):
    if word and (word not in pool or bucket < pool[word]):
        pool[word] = bucket


def candidate_pool_for(base):
    pool = {}
    base_infos = cmu_rhyme_infos(base)
    if base_infos:
        for info in base_infos:
            for word in cmu_rhyme_index.get(info["rhyme_key"], []):
                add_candidate(pool, word, BUCKET_EXACT)
            for word in cmu_family_index.get(info["family_key"], []):
                add_candidate(pool, word, BUCKET_NEAR)
        for word in COMMON_RHYME_WORDS:
            normalized = normalize_word(word)
            if not normalized or normalized == base:
                continue
            relation = cmu_relation(base_infos, cmu_rhyme_infos(normalized))
            if relation >= 0:
                add_candidate(pool, word, relation)
            elif near_slang_family(base, normalized):
                add_candidate(pool, word, BUCKET_NEAR)
        for phrase in COMMON_RHYME_PHRASES:
            rhyme_word = candidate_rhyme_word(phrase)
            relation = cmu_relation(base_infos, cmu_rhyme_infos(rhyme_word))
            if relation >= 0 or near_slang_family(base, rhyme_word):
                add_candidate(pool, phrase, BUCKET_PHRASE)
    return pool


def best_cmu_rhyme_score(base, candidate, base_infos, candidate_infos, bucket):
    best = 0
    for a in base_infos:
        for b in candidate_infos:
            if a["vowel_key"] != b["vowel_key"]:
                continue
            exact = a["rhyme_key"] == b["rhyme_key"]
            overlap = phone_tail_overlap(a["rhyme_key"], b["rhyme_key"])
            slang_compatible = near_slang_family(base, candidate)
            coda_compatible = (
                exact
                or (a["coda_key"] and a["coda_key"] == b["coda_key"])
                or overlap >= 2
                or slang_compatible
            )
            if not coda_compatible:
                continue
            syllable_diff = abs(a["syllable_count"] - b["syllable_count"])
            if not exact and syllable_diff > 2:
                continue
            score = 260 if exact else 122
            score += overlap * 34
            if a["coda_key"] == b["coda_key"]:
                score += 74
            elif same_final_phone(a["coda_key"], b["coda_key"]):
                score += 28
            score += 28 if syllable_diff == 0 else -syllable_diff * 12
            if slang_compatible:
                score += 30
            if slang_variant_pair(base, candidate):
                score += 360
            score += common_rhyme_bias(candidate)
            score -= bucket_penalty(bucket)
            best = max(best, score)
    return best


def score(base, candidate, bucket):
    rhyme_word = candidate_rhyme_word(candidate)
    base_infos = cmu_rhyme_infos(base)
    candidate_infos = cmu_rhyme_infos(rhyme_word)
    if base_infos and candidate_infos:
        value = best_cmu_rhyme_score(base, rhyme_word, base_infos, candidate_infos, bucket)
        return value if value >= 112 else 0
    return 0


def suggest(word, limit=12):
    base = normalize_word(word)
    matches = []
    for candidate, bucket in candidate_pool_for(base).items():
        rhyme_word = candidate_rhyme_word(candidate)
        if rhyme_word == base:
            continue
        value = score(base, candidate, bucket)
        if value > 0:
            matches.append((candidate, value, bucket))
    matches.sort(key=lambda item: (-item[1], item[2], item[0]))
    return matches[:limit]


def words(items):
    return [item[0] for item in items]


def rank(result, word):
    try:
        return words(result).index(word)
    except ValueError:
        return None


def require(name, condition, detail):
    status = "PASS" if condition else "FAIL"
    print(f"{status}: {name} - {detail}")
    return condition


def main():
    load_cmu()
    checks = []
    hover = suggest("hover")
    cover = suggest("cover")
    near = suggest("near")
    moving = suggest("moving")
    running = suggest("running")
    time = suggest("time")

    checks.append(require("hover excludes near/clear", "near" not in words(hover) and "clear" not in words(hover), str(words(hover[:8]))))
    checks.append(require("hover groups with lover", "lover" in words(hover[:8]), str(words(hover[:8]))))
    checks.append(require("cover groups with lover/hover", {"lover", "hover"}.issubset(set(words(cover[:8]))), str(words(cover[:8]))))
    checks.append(require("near groups with clear", "clear" in words(near[:8]), str(words(near[:8]))))
    checks.append(require("moving includes proving/grooving", {"proving", "grooving"}.issubset(set(words(moving[:10]))), str(words(moving[:10]))))
    checks.append(require("moving rejects weak fallback fluid", "fluid" not in words(moving[:10]), str(words(moving[:10]))))
    checks.append(require("running includes runnin", "runnin" in words(running[:8]), str(words(running[:8]))))

    time_words = words(time)
    phrase_pos = rank(time, "in due time")
    rhyme_pos = rank(time, "rhyme")
    checks.append(require(
        "phrase does not outrank strong single word",
        phrase_pos is None or (rhyme_pos is not None and phrase_pos > rhyme_pos),
        str(time_words[:10]),
    ))

    return 0 if all(checks) else 1


if __name__ == "__main__":
    sys.exit(main())

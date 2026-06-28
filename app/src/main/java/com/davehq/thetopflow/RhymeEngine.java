package com.davehq.thetopflow;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class RhymeEngine {
    private static final String TAG = "TopFlow";
    private static final int CACHE_LIMIT = 192;
    private static final int BUCKET_EXACT = 0;
    private static final int BUCKET_NEAR = 1;
    private static final int BUCKET_SLANT = 2;
    private static final int BUCKET_PHRASE = 3;
    private static final int BUCKET_FALLBACK = 4;
    private static final HashSet<String> VOWELS = new HashSet<>();

    static {
        Collections.addAll(VOWELS, "AA", "AE", "AH", "AO", "AW", "AY", "EH", "ER", "EY", "IH", "IY", "OW", "OY", "UH", "UW");
    }

    static final class Options {
        final String strictness;
        final boolean exactOnly;
        final boolean includeSlang;
        final Set<String> removed;
        final String contextBody;

        Options(String strictness, boolean exactOnly, boolean includeSlang, Set<String> removed, String contextBody) {
            this.strictness = strictness == null ? "Balanced" : strictness;
            this.exactOnly = exactOnly;
            this.includeSlang = includeSlang;
            this.removed = removed == null ? new HashSet<>() : removed;
            this.contextBody = contextBody == null ? "" : contextBody;
        }
    }

    private final Context context;
    private final Map<String, ArrayList<String>> phonesByWord = new HashMap<>();
    private final Map<String, ArrayList<String>> exactIndex = new HashMap<>();
    private final Map<String, ArrayList<String>> familyIndex = new HashMap<>();
    private final HashSet<String> dictionaryWords = new HashSet<>();
    private final Object cacheLock = new Object();
    private final LinkedHashMap<String, ArrayList<String>> resultCache = new LinkedHashMap<String, ArrayList<String>>(CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ArrayList<String>> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private final LinkedHashMap<String, ArrayList<PhoneRhymeInfo>> infoCache = new LinkedHashMap<String, ArrayList<PhoneRhymeInfo>>(CACHE_LIMIT * 4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ArrayList<PhoneRhymeInfo>> eldest) {
            return size() > CACHE_LIMIT * 4;
        }
    };
    private final LinkedHashMap<String, String> familyCache = new LinkedHashMap<String, String>(CACHE_LIMIT * 4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_LIMIT * 4;
        }
    };
    private volatile boolean ready = false;
    private volatile boolean loading = false;
    private volatile int generation = 0;

    RhymeEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    boolean isReady() {
        return ready;
    }

    int generation() {
        return generation;
    }

    void loadAsync(Runnable onReady) {
        if (ready || loading) return;
        loading = true;
        new Thread(() -> {
            long start = System.currentTimeMillis();
            load();
            ready = true;
            loading = false;
            clearCache();
            Log.d(TAG, "rhyme engine ready words=" + phonesByWord.size() + " exactKeys=" + exactIndex.size() + " ms=" + (System.currentTimeMillis() - start));
            if (onReady != null) onReady.run();
        }, "TopFlowRhymeLoad").start();
    }

    ArrayList<String> suggest(String word, int limit, int maxCandidates, Options options) {
        ArrayList<String> out = new ArrayList<>();
        String base = normalizeWord(word);
        if (base.isEmpty() || !ready) return out;
        String cacheKey = cacheKey(base, limit, maxCandidates, options);
        synchronized (cacheLock) {
            ArrayList<String> cached = resultCache.get(cacheKey);
            if (cached != null) return new ArrayList<>(cached);
        }
        ArrayList<PhoneRhymeInfo> baseInfos = rhymeInfos(base, options);
        Map<String, Integer> contextHits = maxCandidates > 0 ? Collections.emptyMap() : contextFamilyHits(base, options);
        ArrayList<RhymeMatch> matches = new ArrayList<>();
        ArrayList<RhymeCandidate> candidates = candidatePoolFor(base, options, baseInfos);
        Collections.sort(candidates, (a, b) -> {
            if (a.bucket != b.bucket) return Integer.compare(a.bucket, b.bucket);
            int priority = Integer.compare(commonRhymeBias(b.word), commonRhymeBias(a.word));
            if (priority != 0) return priority;
            return a.word.compareTo(b.word);
        });
        int scored = 0;
        for (RhymeCandidate candidate : candidates) {
            if (Thread.currentThread().isInterrupted()) return out;
            String rhymeWord = candidateRhymeWord(candidate.word);
            if (rhymeWord.equals(base) || isRemoved(candidate.word, options)) continue;
            if (maxCandidates > 0 && scored >= maxCandidates) break;
            scored++;
            int score = rhymeScore(base, candidate.word, candidate.bucket, options, baseInfos, contextHits);
            if (score > 0) matches.add(new RhymeMatch(candidate.word, score, candidate.bucket, commonRhymeBias(candidate.word)));
        }
        Collections.sort(matches, (a, b) -> {
            if (b.score != a.score) return Integer.compare(b.score, a.score);
            if (a.bucket != b.bucket) return Integer.compare(a.bucket, b.bucket);
            if (b.priority != a.priority) return Integer.compare(b.priority, a.priority);
            return a.word.compareTo(b.word);
        });
        for (RhymeMatch match : matches) {
            if (!out.contains(match.word)) out.add(match.word);
            if (out.size() >= limit) break;
        }
        synchronized (cacheLock) {
            resultCache.put(cacheKey, new ArrayList<>(out));
        }
        return out;
    }

    void clearCache() {
        synchronized (cacheLock) {
            resultCache.clear();
            infoCache.clear();
            familyCache.clear();
            generation++;
        }
    }

    private String cacheKey(String base, int limit, int maxCandidates, Options options) {
        String contextKey = maxCandidates > 0 ? "fast" : String.valueOf(options.contextBody.hashCode());
        return base + "|" + limit + "|" + maxCandidates + "|" + generation + "|" + options.strictness
                + "|" + options.exactOnly + "|" + options.includeSlang
                + "|" + options.removed.hashCode() + "|" + contextKey;
    }

    private void load() {
        phonesByWord.clear();
        exactIndex.clear();
        familyIndex.clear();
        dictionaryWords.clear();
        if (!loadPreparedIndex()) loadCmuDictionary();
        addPronunciationOverrides();
        for (String word : COMMON_RHYME_WORDS) {
            String w = normalizeWord(word);
            String phones = slangPhones(w, true);
            if (!w.isEmpty() && !phones.isEmpty() && !dictionaryWords.contains(w)) addEntry(w, phones, true);
        }
    }

    private boolean loadPreparedIndex() {
        try (InputStream raw = context.getAssets().open("rhyme_index.tsv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] parts = line.split("\t", -1);
                if (parts.length < 7) continue;
                String word = normalizeWord(parts[0]);
                String phones = parts[1];
                if (word.isEmpty() || phones.isEmpty()) continue;
                addPhones(word, phones, true);
                addIndexedWord(exactIndex, parts[2], word);
                addIndexedWord(familyIndex, parts[5], word);
            }
            return !phonesByWord.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void loadCmuDictionary() {
        try (InputStream raw = context.getAssets().open("cmudict.dict");
             BufferedReader reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith(";;;")) continue;
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length != 2) continue;
                String word = normalizeCmuWord(parts[0]);
                if (word.isEmpty() || word.length() > 18) continue;
                addEntry(word, parts[1].trim(), true);
            }
        } catch (Exception ignored) {
        }
    }

    private void addPronunciationOverrides() {
        addEntry("your", "Y AO1 R", true);
        addEntry("yours", "Y AO1 R Z", true);
    }

    private void addEntry(String word, String phones, boolean dictionary) {
        addPhones(word, phones, dictionary);
        String rhyme = cmuRhymePartFromPhones(phones);
        if (!rhyme.isEmpty()) addIndexedWord(exactIndex, rhymeKeyFromPhones(rhyme), word);
        PhoneRhymeInfo info = phoneRhymeInfo(phones);
        if (info != null) addIndexedWord(familyIndex, info.familyKey, word);
    }

    private void addPhones(String word, String phones, boolean dictionary) {
        ArrayList<String> list = phonesByWord.get(word);
        if (list == null) {
            list = new ArrayList<>();
            phonesByWord.put(word, list);
        }
        if (!list.contains(phones)) list.add(phones);
        if (dictionary) dictionaryWords.add(word);
    }

    private void addIndexedWord(Map<String, ArrayList<String>> index, String key, String word) {
        if (key == null || key.isEmpty()) return;
        ArrayList<String> words = index.get(key);
        if (words == null) {
            words = new ArrayList<>();
            index.put(key, words);
        }
        if (words.size() < 360 && !words.contains(word)) words.add(word);
    }

    private ArrayList<RhymeCandidate> candidatePoolFor(String base, Options options, ArrayList<PhoneRhymeInfo> baseInfos) {
        HashMap<String, Integer> pool = new HashMap<>();
        if (!baseInfos.isEmpty()) {
            for (PhoneRhymeInfo info : baseInfos) {
                ArrayList<String> exact = exactIndex.get(info.rhymeKey);
                if (exact != null) for (String word : exact) addCandidate(pool, word, BUCKET_EXACT);
                ArrayList<String> family = familyIndex.get(info.familyKey);
                if (family != null) for (String word : family) addCandidate(pool, word, BUCKET_NEAR);
            }
            for (String word : COMMON_RHYME_WORDS) {
                String w = normalizeWord(word);
                if (w.isEmpty() || w.equals(base)) continue;
                int relation = cmuRelation(baseInfos, rhymeInfos(w, options));
                if (relation >= 0) addCandidate(pool, word, relation);
                else if (nearSlangFamily(base, w, options)) addCandidate(pool, word, BUCKET_SLANT);
            }
            for (String phrase : COMMON_RHYME_PHRASES) {
                String w = candidateRhymeWord(phrase);
                if (w.isEmpty() || w.equals(base)) continue;
                int relation = cmuRelation(baseInfos, rhymeInfos(w, options));
                if (relation == BUCKET_EXACT || relation == BUCKET_NEAR || nearSlangFamily(base, w, options)) addCandidate(pool, phrase, BUCKET_PHRASE);
            }
        } else {
            String baseKey = rhymeKey(base, options);
            String baseFamily = phonemeFamily(base, options);
            for (String word : COMMON_RHYME_WORDS) {
                String w = normalizeWord(word);
                if (w.isEmpty() || w.equals(base)) continue;
                String k = rhymeKey(w, options);
                if (k.equals(baseKey) || phonemeFamily(w, options).equals(baseFamily) || nearSlangFamily(base, w, options)) {
                    addCandidate(pool, word, BUCKET_FALLBACK);
                }
            }
        }
        ArrayList<RhymeCandidate> out = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pool.entrySet()) out.add(new RhymeCandidate(entry.getKey(), entry.getValue()));
        return out;
    }

    private void addCandidate(HashMap<String, Integer> pool, String word, int bucket) {
        if (word == null || word.isEmpty()) return;
        Integer existing = pool.get(word);
        if (existing == null || bucket < existing) pool.put(word, bucket);
    }

    private int rhymeScore(String base, String candidate, int bucket, Options options, ArrayList<PhoneRhymeInfo> baseInfos, Map<String, Integer> contextHits) {
        String c = candidateRhymeWord(candidate);
        if (c.isEmpty() || c.equals(base)) return 0;
        ArrayList<PhoneRhymeInfo> candidateInfos = rhymeInfos(c, options);
        if (!baseInfos.isEmpty() && !candidateInfos.isEmpty()) {
            int score = bestCmuRhymeScore(base, c, baseInfos, candidateInfos, bucket, options, contextHits);
            return score >= scoreThreshold(options) ? score : 0;
        }
        if (options.exactOnly) return 0;
        if (!baseInfos.isEmpty() || !candidateInfos.isEmpty()) {
            if (!nearSlangFamily(base, c, options)) return 0;
            int score = 116 + internalRhymeBias(c, options, contextHits) - bucketPenalty(bucket);
            return score >= scoreThreshold(options) ? score : 0;
        }
        String key = rhymeKey(base, options);
        String ck = rhymeKey(c, options);
        String baseFamily = phonemeFamily(base, options);
        String candidateFamily = phonemeFamily(c, options);
        if (!baseFamily.equals(candidateFamily) && !nearSlangFamily(base, c, options)) return 0;
        int score = 0;
        if (ck.equals(key)) score += 120;
        if (baseFamily.equals(candidateFamily)) score += 38;
        if (nearSlangFamily(base, c, options)) score += 24;
        score += commonRhymeBias(candidate);
        score += internalRhymeBias(c, options, contextHits);
        score -= bucketPenalty(bucket);
        score -= Math.abs(c.length() - base.length()) / 2;
        return score >= scoreThreshold(options) ? Math.max(score, 0) : 0;
    }

    private int bestCmuRhymeScore(String base, String candidate, ArrayList<PhoneRhymeInfo> baseInfos, ArrayList<PhoneRhymeInfo> candidateInfos, int bucket, Options options, Map<String, Integer> contextHits) {
        int best = 0;
        for (PhoneRhymeInfo a : baseInfos) {
            for (PhoneRhymeInfo b : candidateInfos) {
                if (!a.vowelKey.equals(b.vowelKey)) continue;
                boolean exact = a.rhymeKey.equals(b.rhymeKey);
                if (options.exactOnly && !exact) continue;
                int overlap = phoneTailOverlap(a.rhymeKey, b.rhymeKey);
                boolean slangCompatible = nearSlangFamily(base, candidate, options);
                boolean codaCompatible = exact || !a.codaKey.isEmpty() && a.codaKey.equals(b.codaKey) || overlap >= 2 || slangCompatible;
                if (!codaCompatible) continue;
                int syllableDiff = Math.abs(a.syllableCount - b.syllableCount);
                if (!exact && syllableDiff > 2) continue;
                int score = exact ? 260 : 122;
                score += overlap * 34;
                if (a.codaKey.equals(b.codaKey)) score += 74;
                else if (sameFinalPhone(a.codaKey, b.codaKey)) score += 28;
                if (syllableDiff == 0) score += 28;
                else score -= syllableDiff * 12;
                if (slangCompatible) score += 30;
                if (slangVariantPair(base, candidate)) score += 360;
                score += commonRhymeBias(candidate);
                score += internalRhymeBias(candidate, options, contextHits);
                score -= bucketPenalty(bucket);
                best = Math.max(best, score);
            }
        }
        return best;
    }

    private int cmuRelation(ArrayList<PhoneRhymeInfo> baseInfos, ArrayList<PhoneRhymeInfo> candidateInfos) {
        if (baseInfos.isEmpty() || candidateInfos.isEmpty()) return -1;
        int best = -1;
        for (PhoneRhymeInfo a : baseInfos) {
            for (PhoneRhymeInfo b : candidateInfos) {
                if (!a.vowelKey.equals(b.vowelKey)) continue;
                if (a.rhymeKey.equals(b.rhymeKey)) return BUCKET_EXACT;
                int syllableDiff = Math.abs(a.syllableCount - b.syllableCount);
                int overlap = phoneTailOverlap(a.rhymeKey, b.rhymeKey);
                if (a.codaKey.equals(b.codaKey) || overlap >= 2) {
                    if (syllableDiff <= 2) best = BUCKET_NEAR;
                } else if (sameFinalPhone(a.codaKey, b.codaKey) && syllableDiff <= 1 && best < 0) {
                    best = BUCKET_SLANT;
                }
            }
        }
        return best;
    }

    private ArrayList<PhoneRhymeInfo> rhymeInfos(String word, Options options) {
        String w = normalizeWord(word);
        String key = w + "|" + options.includeSlang + "|" + generation;
        synchronized (cacheLock) {
            ArrayList<PhoneRhymeInfo> cached = infoCache.get(key);
            if (cached != null) return new ArrayList<>(cached);
        }
        ArrayList<PhoneRhymeInfo> out = new ArrayList<>();
        ArrayList<String> phones = phonesByWord.get(w);
        if (phones != null) {
            for (String p : phones) {
                PhoneRhymeInfo info = phoneRhymeInfo(p);
                if (info != null) out.add(info);
            }
        }
        String guessedIng = guessedIngPhones(w);
        if (!guessedIng.isEmpty() && !dictionaryWords.contains(w)) {
            PhoneRhymeInfo info = phoneRhymeInfo(guessedIng);
            if (info != null) out.add(info);
        }
        String slang = slangPhones(w, options.includeSlang);
        if (!slang.isEmpty() && !dictionaryWords.contains(w)) {
            PhoneRhymeInfo info = phoneRhymeInfo(slang);
            if (info != null) out.add(info);
        }
        synchronized (cacheLock) {
            infoCache.put(key, new ArrayList<>(out));
        }
        return out;
    }

    private PhoneRhymeInfo phoneRhymeInfo(String phones) {
        if (phones == null || phones.trim().isEmpty()) return null;
        String rhyme = cmuRhymePartFromPhones(phones);
        if (rhyme.isEmpty()) return null;
        String[] parts = rhyme.trim().split("\\s+");
        String vowel = "";
        ArrayList<String> coda = new ArrayList<>();
        for (String part : parts) {
            String clean = stripStress(part);
            if (vowel.isEmpty() && isVowelPhone(part)) vowel = clean;
            else if (!vowel.isEmpty() && !isVowelPhone(part)) coda.add(clean);
        }
        if (vowel.isEmpty()) return null;
        int syllables = 0;
        for (String part : splitPhones(phones)) if (isVowelPhone(part)) syllables++;
        String codaKey = join(coda);
        return new PhoneRhymeInfo(rhymeKeyFromPhones(rhyme), vowel, codaKey, vowel + ":" + codaKey, Math.max(1, syllables));
    }

    private String cmuRhymePartFromPhones(String phones) {
        String[] parts = splitPhones(phones);
        int stressed = -1;
        for (int i = parts.length - 1; i >= 0; i--) {
            if (isVowelPhone(parts[i]) && (parts[i].endsWith("1") || parts[i].endsWith("2"))) {
                stressed = i;
                break;
            }
        }
        if (stressed < 0) {
            for (int i = parts.length - 1; i >= 0; i--) {
                if (isVowelPhone(parts[i])) {
                    stressed = i;
                    break;
                }
            }
        }
        if (stressed < 0) return "";
        StringBuilder out = new StringBuilder();
        for (int i = stressed; i < parts.length; i++) {
            if (out.length() > 0) out.append(' ');
            out.append(parts[i]);
        }
        return out.toString();
    }

    private String rhymeKey(String word, Options options) {
        String cmu = cmuRhymeTail(word, options);
        if (!cmu.isEmpty()) return cmu.toLowerCase(Locale.US).replace(" ", "");
        return phoneticTail(word);
    }

    private String cmuRhymeTail(String word, Options options) {
        ArrayList<PhoneRhymeInfo> infos = rhymeInfos(word, options);
        if (!infos.isEmpty()) return infos.get(0).rhymeKey;
        String w = normalizeWord(word);
        if (in(w, "my", "try", "fly", "sky", "high", "why", "lie", "cry", "buy", "eye")) return "AY";
        if (in(w, "day", "way", "play", "say", "stay", "gray", "spray", "sway", "delay")) return "EY";
        if (in(w, "made", "fade", "shade")) return "EY D";
        if (in(w, "pain", "rain", "chain", "brain", "gain", "train", "plane", "strain")) return "EY N";
        if (in(w, "time", "rhyme", "dime", "climb", "lime", "crime")) return "AY M";
        if (in(w, "mine", "shine", "line", "fine", "sign", "design")) return "AY N";
        if (in(w, "night", "light", "flight", "bright", "might", "tight", "right", "sight")) return "AY T";
        if (in(w, "flow", "go", "show", "glow", "throw", "slow", "grow", "blow")) return "OW";
        if (in(w, "yours", "soars", "pores", "doors", "floors")) return "AO R Z";
        if (in(w, "your", "soar", "pore", "door", "floor")) return "AO R";
        if (in(w, "out", "bout", "clout", "shout", "doubt", "about", "route", "scout", "spout", "sprout", "trout", "pout", "stout")) return "AW T";
        return "";
    }

    private String phonemeFamily(String word, Options options) {
        String normalized = normalizeWord(word);
        String cacheKey = normalized + "|" + options.includeSlang + "|" + generation;
        synchronized (cacheLock) {
            String cached = familyCache.get(cacheKey);
            if (cached != null) return cached;
        }
        String family;
        String cmu = cmuRhymeTail(word, options);
        if (!cmu.isEmpty()) family = phonemeFamilyFromPhones(cmu);
        else {
        String sound = lastVowelSound(word);
        if (sound.startsWith("igh") || sound.startsWith("y") || sound.startsWith("i")) family = "AY" + endingConsonantCluster(sound);
        else if (sound.startsWith("ay") || sound.startsWith("ai") || sound.startsWith("ei")) family = "EY" + endingConsonantCluster(sound);
        else if (sound.startsWith("ou") || sound.startsWith("ow")) family = "AW" + endingConsonantCluster(sound);
        else if (sound.startsWith("oo") || sound.startsWith("u")) family = "UW" + endingConsonantCluster(sound);
        else if (sound.startsWith("ee") || sound.startsWith("ea")) family = "IY" + endingConsonantCluster(sound);
        else if (sound.startsWith("o")) family = "O" + endingConsonantCluster(sound);
        else if (sound.startsWith("a")) family = "AH" + endingConsonantCluster(sound);
        else family = sound.length() <= 4 ? sound : sound.substring(0, 4);
        }
        synchronized (cacheLock) {
            familyCache.put(cacheKey, family);
        }
        return family;
    }

    private String phonemeFamilyFromPhones(String phones) {
        PhoneRhymeInfo info = phoneRhymeInfo(phones);
        return info == null ? "" : info.familyKey;
    }

    private String slangPhones(String word, boolean includeSlang) {
        String w = normalizeWord(word);
        if (!includeSlang || w.isEmpty()) return "";
        if (w.endsWith("in'")) w = w.substring(0, w.length() - 1);
        if (w.endsWith("ing") && w.length() > 4) return guessedOnsetPhones(w.substring(0, w.length() - 3)) + " IH0 N";
        if (w.endsWith("in") && w.length() > 3) return guessedOnsetPhones(w.substring(0, w.length() - 2)) + " IH0 N";
        if (w.length() > 5 && (w.endsWith("ant") || w.endsWith("ent") || w.endsWith("int") || w.endsWith("unt"))) return guessedOnsetPhones(w.substring(0, w.length() - 3)) + " IH0 N";
        if ("coolant".equals(w)) return "K UW1 L IH0 N";
        if ("pullin".equals(w)) return "P UH1 L IH0 N";
        return "";
    }

    private String guessedIngPhones(String word) {
        String w = normalizeWord(word);
        if (w.endsWith("ing") && w.length() > 4) return guessedOnsetPhones(w.substring(0, w.length() - 3)) + " IH0 NG";
        return "";
    }

    private String guessedOnsetPhones(String stem) {
        String s = normalizeWord(stem);
        if (s.endsWith("ll")) return "P UH1 L";
        if (s.endsWith("l")) return "K UW1 L";
        if (s.endsWith("v")) return "M UW1 V";
        return "AH1";
    }

    private boolean nearSlangFamily(String a, String b, Options options) {
        if (!options.includeSlang) return false;
        String x = hipHopNearTail(a);
        String y = hipHopNearTail(b);
        return !x.isEmpty() && x.equals(y);
    }

    private String hipHopNearTail(String word) {
        String w = normalizeWord(word);
        if (w.endsWith("ing") && w.length() > 4) w = w.substring(0, w.length() - 3) + "in";
        if (w.length() > 5 && (w.endsWith("ant") || w.endsWith("ent") || w.endsWith("int") || w.endsWith("unt"))) w = w.substring(0, w.length() - 3) + "in";
        if (w.endsWith("in") || w.endsWith("en")) {
            int idx = Math.max(w.lastIndexOf("l"), Math.max(w.lastIndexOf("m"), w.lastIndexOf("v")));
            return idx >= 0 ? w.substring(idx, Math.min(w.length(), idx + 3)) : "in";
        }
        return "";
    }

    private int scoreThreshold(Options options) {
        if ("Strict".equals(options.strictness)) return 178;
        if ("Loose".equals(options.strictness)) return 76;
        return 112;
    }

    private int bucketPenalty(int bucket) {
        if (bucket == BUCKET_EXACT) return 0;
        if (bucket == BUCKET_NEAR) return 12;
        if (bucket == BUCKET_SLANT) return 52;
        if (bucket == BUCKET_PHRASE) return 90;
        return 130;
    }

    private int commonRhymeBias(String candidate) {
        String w = candidateRhymeWord(candidate);
        if ("about".equals(w)) return 101;
        for (int i = 0; i < COMMON_RHYME_WORDS.length; i++) {
            if (normalizeWord(COMMON_RHYME_WORDS[i]).equals(w)) return Math.max(48, 124 - i);
        }
        return 0;
    }

    private Map<String, Integer> contextFamilyHits(String base, Options options) {
        HashMap<String, Integer> hits = new HashMap<>();
        if (options.contextBody.isEmpty()) return hits;
        String[] parts = options.contextBody.toLowerCase(Locale.US).split("[^a-z']+");
        int counted = 0;
        for (String part : parts) {
            String w = normalizeWord(part);
            if (w.isEmpty() || w.equals(base)) continue;
            String family = phonemeFamily(w, options);
            if (family.isEmpty()) continue;
            Integer current = hits.get(family);
            hits.put(family, current == null ? 1 : Math.min(2, current + 1));
            counted++;
            if (counted >= 160) break;
        }
        return hits;
    }

    private int internalRhymeBias(String candidate, Options options, Map<String, Integer> contextHits) {
        if (contextHits == null || contextHits.isEmpty()) return 0;
        String family = phonemeFamily(candidate, options);
        if (family.isEmpty()) return 0;
        Integer hits = contextHits.get(family);
        return hits == null ? 0 : Math.min(2, hits) * 8;
    }

    private boolean isRemoved(String suggestion, Options options) {
        String normalized = normalizeWord(suggestion);
        if (!normalized.isEmpty() && options.removed.contains(normalized)) return true;
        String rhymeWord = candidateRhymeWord(suggestion);
        return !rhymeWord.isEmpty() && options.removed.contains(rhymeWord);
    }

    private boolean slangVariantPair(String a, String b) {
        String x = slangVariantKey(a);
        String y = slangVariantKey(b);
        return !x.isEmpty() && x.equals(y) && !normalizeWord(a).equals(normalizeWord(b));
    }

    private String slangVariantKey(String word) {
        String w = normalizeWord(word);
        if (w.endsWith("ing") && w.length() > 4) return w.substring(0, w.length() - 3) + "in";
        if (w.endsWith("in") && w.length() > 3) return w;
        return "";
    }

    private int phoneTailOverlap(String a, String b) {
        String[] x = splitPhones(a);
        String[] y = splitPhones(b);
        int overlap = 0;
        while (overlap < x.length && overlap < y.length && x[x.length - 1 - overlap].equals(y[y.length - 1 - overlap])) overlap++;
        return overlap;
    }

    private boolean sameFinalPhone(String a, String b) {
        String[] x = splitPhones(a);
        String[] y = splitPhones(b);
        return x.length > 0 && y.length > 0 && x[x.length - 1].equals(y[y.length - 1]);
    }

    private String rhymeKeyFromPhones(String phones) {
        return stripStress(phones).trim();
    }

    private boolean isVowelPhone(String phone) {
        return VOWELS.contains(stripStress(phone));
    }

    private String stripStress(String phone) {
        return phone == null ? "" : phone.replaceAll("[012]", "");
    }

    private String[] splitPhones(String phones) {
        if (phones == null || phones.trim().isEmpty()) return new String[0];
        return phones.trim().split("\\s+");
    }

    private String normalizeCmuWord(String word) {
        if (word == null) return "";
        int variant = word.indexOf('(');
        if (variant >= 0) word = word.substring(0, variant);
        if (!word.matches("[A-Za-z']+")) return "";
        return normalizeWord(word);
    }

    private String normalizeWord(String word) {
        if (word == null) return "";
        String w = word.toLowerCase(Locale.US).replaceAll("[^a-z']", "");
        while (w.startsWith("'")) w = w.substring(1);
        while (w.endsWith("'")) w = w.substring(0, w.length() - 1);
        return w;
    }

    private String candidateRhymeWord(String candidate) {
        if (candidate == null) return "";
        String[] parts = candidate.toLowerCase(Locale.US).split("[^a-z']+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String w = normalizeWord(parts[i]);
            if (!w.isEmpty()) return w;
        }
        return normalizeWord(candidate);
    }

    private String phoneticTail(String word) {
        String w = normalizeWord(word);
        if (w.isEmpty()) return "";
        w = w.replace("ph", "f").replace("gh", "").replace("ck", "k");
        w = w.replace("qu", "kw").replace("x", "ks");
        if (w.endsWith("e") && w.length() > 3) w = w.substring(0, w.length() - 1);
        int vowel = -1;
        for (int i = w.length() - 1; i >= 0; i--) {
            if (isVowel(w.charAt(i))) {
                vowel = i;
                break;
            }
        }
        return vowel < 0 ? tailKey(w) : w.substring(vowel);
    }

    private String tailKey(String word) {
        String w = normalizeWord(word);
        return w.length() <= 3 ? w : w.substring(w.length() - 3);
    }

    private String lastVowelSound(String word) {
        String w = normalizeWord(word);
        if (w.endsWith("e") && w.length() > 3) w = w.substring(0, w.length() - 1);
        for (int i = w.length() - 1; i >= 0; i--) {
            if (isVowel(w.charAt(i))) return w.substring(i, Math.min(w.length(), i + 3));
        }
        return tailKey(w);
    }

    private String endingConsonantCluster(String word) {
        String w = normalizeWord(word);
        int i = w.length() - 1;
        while (i >= 0 && isVowel(w.charAt(i))) i--;
        int end = i;
        while (i >= 0 && !isVowel(w.charAt(i))) i--;
        if (end < 0) return "";
        return w.substring(Math.max(0, i + 1), end + 1);
    }

    private boolean isVowel(char c) {
        return "aeiouy".indexOf(c) >= 0;
    }

    private boolean in(String value, String... values) {
        for (String item : values) if (item.equals(value)) return true;
        return false;
    }

    private String join(ArrayList<String> parts) {
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (out.length() > 0) out.append(' ');
            out.append(part);
        }
        return out.toString();
    }

    private static final class PhoneRhymeInfo {
        final String rhymeKey;
        final String vowelKey;
        final String codaKey;
        final String familyKey;
        final int syllableCount;

        PhoneRhymeInfo(String rhymeKey, String vowelKey, String codaKey, String familyKey, int syllableCount) {
            this.rhymeKey = rhymeKey;
            this.vowelKey = vowelKey;
            this.codaKey = codaKey;
            this.familyKey = familyKey;
            this.syllableCount = syllableCount;
        }
    }

    private static final class RhymeCandidate {
        final String word;
        final int bucket;

        RhymeCandidate(String word, int bucket) {
            this.word = word;
            this.bucket = bucket;
        }
    }

    private static final class RhymeMatch {
        final String word;
        final int score;
        final int bucket;
        final int priority;

        RhymeMatch(String word, int score, int bucket, int priority) {
            this.word = word;
            this.score = score;
            this.bucket = bucket;
            this.priority = priority;
        }
    }

    private static final String[] COMMON_RHYME_WORDS = {
            "my", "try", "fly", "sky", "high", "why", "lie", "cry", "buy", "eye",
            "time", "rhyme", "dime", "climb", "lime", "mine", "shine", "line", "fine", "crime",
            "grind", "blind", "kind", "mind", "wind", "sign", "design", "behind",
            "night", "light", "flight", "bright", "might", "tight", "right", "sight",
            "flow", "go", "show", "glow", "throw", "slow", "grow", "blow",
            "way", "play", "say", "stay", "day", "made", "fade", "shade", "gray", "spray", "sway", "delay",
            "out", "bout", "clout", "shout", "doubt", "about", "route", "scout", "spout", "sprout", "trout", "pout", "stout",
            "yours", "soars", "pores", "doors", "floors", "your", "soar", "pore", "door", "floor",
            "heart", "start", "part", "smart", "chart", "art", "hard", "yard",
            "pain", "rain", "chain", "brain", "gain", "train", "plane", "strain",
            "soul", "cold", "gold", "roll", "control", "whole", "goal", "bowl",
            "voice", "choice", "noise", "poise", "boys", "toy", "joy", "ploy",
            "near", "clear", "fear", "dear", "here", "year", "peer", "tear",
            "heat", "beat", "street", "sweet", "fleet", "meet", "seat", "feat",
            "cover", "lover", "hover", "running", "runnin", "proving", "grooving",
            "pullin", "pulling", "coolant", "woolen", "bullet", "couldn't", "shouldn't", "wouldn't",
            "movin", "moving", "proven", "losing", "choosing", "ruin", "fluid", "student"
    };

    private static final String[] COMMON_RHYME_PHRASES = {
            "keep it movin", "fluid motion", "coolant flow", "pullin through",
            "all day", "same way", "late night", "bright lights",
            "on my mind", "in due time", "stay in line", "chain reaction"
    };
}

package com.davehq.thetopflow.rhyme

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedHashMap
import java.util.zip.CRC32

class RhymeEngine2(context: Context) {
    private val appContext = context.applicationContext
    private var table: CandidateTable? = null
    private val resultCache = object : LinkedHashMap<String, List<RhymeCandidate>>(CACHE_CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<RhymeCandidate>>?): Boolean =
            size > CACHE_CAPACITY
    }

    fun load(): Boolean {
        return runCatching {
            val mapped = RhymeAssetStore.openMappedAsset(appContext, ASSET_NAME).order(ByteOrder.LITTLE_ENDIAN)
            val next = CandidateTable(mapped)
            table = next
            synchronized(resultCache) { resultCache.clear() }
            Log.d(TRACE_TAG, "rhyme_trace stage=v2_candidates_ready rows=${next.rowCount}")
            true
        }.getOrElse { error ->
            table = null
            Log.d(TRACE_TAG, "rhyme_trace stage=v2_candidates_failed reason=${error.javaClass.simpleName}")
            false
        }
    }

    fun suggest(word: String, limit: Int = 8): List<RhymeCandidate> {
        val start = System.nanoTime()
        val normalized = normalize(word)
        val cacheKey = "$normalized:$limit"
        val cached = synchronized(resultCache) { resultCache[cacheKey] }
        val result = if (cached != null) {
            cached
        } else if (normalized.isEmpty()) {
            emptyList()
        } else {
            lookupWithLocalFallback(normalized, limit).also { values ->
                synchronized(resultCache) { resultCache[cacheKey] = values }
            }
        }
        Log.d(
            TRACE_TAG,
            "rhyme_trace stage=v2_suggest chars=${normalized.length} count=${result.size} ms=${(System.nanoTime() - start) / 1_000_000.0}"
        )
        return result
    }

    /** Conservative local OOV bridge for common spoken/rap spellings. */
    private fun lookupWithLocalFallback(normalized: String, limit: Int): List<RhymeCandidate> {
        val current = table ?: return emptyList()
        current.lookup(normalized, limit).takeIf { it.isNotEmpty() }?.let { return it }

        val alternates = buildList {
            when {
                normalized.endsWith("in") && normalized.length > 3 ->
                    add(normalized.dropLast(2) + "ing")
                normalized.endsWith("in'") && normalized.length > 4 ->
                    add(normalized.dropLast(3) + "ing")
            }
            when (normalized) {
                "ima", "imma" -> add("imma")
                "gon" -> add("gonna")
                "wanna" -> add("want")
                "gotta" -> add("got")
                "kinda" -> add("kind")
                "outta" -> add("out")
                "lotta" -> add("lot")
                "wit" -> add("with")
                "tha", "da" -> add("the")
                "cuz", "cos" -> add("because")
                "em" -> add("them")
                "ya" -> add("you")
                "yo" -> add("your")
                "finna" -> add("fix")
                "tryna" -> add("trying")
                "bout" -> add("about")
                "lite" -> add("light")
                "rite" -> add("right")
                "nite" -> add("night")
                "thru" -> add("through")
                "tho" -> add("though")
                "u" -> add("you")
                "ur" -> add("your")
            }
            // Drop trailing 's / 'z for singular lookup
            if (normalized.endsWith("s") && normalized.length > 3 && !normalized.endsWith("ss")) {
                add(normalized.dropLast(1))
            }
        }.distinct().filter { it != normalized && it.isNotBlank() }

        for (alt in alternates) {
            val hit = current.lookup(alt, limit)
            if (hit.isNotEmpty()) return hit
        }
        return emptyList()
    }

    private class CandidateTable(private val data: ByteBuffer) {
        val rowCount: Int
        private val rowTableOffset: Int
        private val stringTableOffset: Int

        init {
            require(data.limit() >= HEADER_SIZE) { "candidate table too small" }
            val magic = ByteArray(MAGIC.size)
            for (index in MAGIC.indices) magic[index] = data.get(index)
            require(magic.contentEquals(MAGIC)) { "bad candidate magic" }
            require(data.getShort(8).toInt() == VERSION) { "unsupported candidate version" }
            rowCount = data.getInt(10)
            rowTableOffset = data.getInt(14)
            stringTableOffset = data.getInt(18)
            require(rowCount >= 25_000) { "candidate row count too low" }
            require(rowTableOffset == HEADER_SIZE) { "bad candidate row offset" }
            require(rowTableOffset + (rowCount * ROW_SIZE) <= stringTableOffset) { "bad candidate string offset" }
            require(data.getInt(22) == checksum()) { "candidate checksum mismatch" }
        }

        fun lookup(word: String, limit: Int): List<RhymeCandidate> {
            var low = 0
            var high = rowCount - 1
            while (low <= high) {
                val mid = (low + high).ushr(1)
                val rowOffset = rowTableOffset + (mid * ROW_SIZE)
                val candidateWord = readString(data.getInt(rowOffset))
                val comparison = candidateWord.compareTo(word)
                if (comparison < 0) {
                    low = mid + 1
                } else if (comparison > 0) {
                    high = mid - 1
                } else {
                    return candidatesFromRow(rowOffset, limit)
                }
            }
            return emptyList()
        }

        private fun candidatesFromRow(rowOffset: Int, limit: Int): List<RhymeCandidate> {
            val count = data.getShort(rowOffset + 4).toInt() and 0xFFFF
            require(count in 1..MAX_CANDIDATES) { "bad candidate count" }
            val out = ArrayList<RhymeCandidate>(minOf(limit, count))
            repeat(minOf(limit, count, MAX_CANDIDATES)) { candidateIndex ->
                val offset = data.getInt(rowOffset + 8 + (candidateIndex * 4))
                val candidate = readString(offset)
                if (candidate.isNotBlank() && candidate !in WEAK_WORDS) {
                    out.add(RhymeCandidate(candidate, RhymeBucket.Perfect, 100 - candidateIndex))
                }
            }
            return out
        }

        private fun checksum(): Int {
            val crc = CRC32()
            for (index in 0 until data.limit()) {
                if (index in 22..25) continue
                crc.update(data.get(index).toInt())
            }
            return crc.value.toInt()
        }

        private fun readString(offset: Int): String {
            require(offset >= stringTableOffset && offset < data.limit()) { "string offset out of range" }
            var end = offset
            while (end < data.limit() && data.get(end).toInt() != 0) end++
            val bytes = ByteArray(end - offset)
            for (index in bytes.indices) bytes[index] = data.get(offset + index)
            return bytes.toString(Charsets.UTF_8)
        }
    }

    private companion object {
        private const val ASSET_NAME = "rhyme_candidates_v2.tfcand"
        private const val HEADER_SIZE = 32
        private const val ROW_SIZE = 56
        private const val VERSION = 2
        private const val MAX_CANDIDATES = 12
        private const val CACHE_CAPACITY = 192
        private const val TRACE_TAG = "rhyme_trace"
        private val MAGIC = "TFCAND2".toByteArray(Charsets.US_ASCII)
        private val WEAK_WORDS = setOf(
            "a", "an", "the", "and", "or", "of", "to", "in", "on", "at", "is", "it", "be", "as", "by",
            "for", "from", "with", "that", "this", "these", "those", "was", "were", "are", "am"
        )

        private fun normalize(word: String): String {
            return word.lowercase().replace(Regex("[^a-z']"), "").trim('\'')
        }
    }
}

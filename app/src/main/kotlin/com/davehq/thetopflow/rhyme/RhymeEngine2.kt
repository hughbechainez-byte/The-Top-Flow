package com.davehq.thetopflow.rhyme

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

class RhymeEngine2(context: Context) {
    private val appContext = context.applicationContext
    private var table: CandidateTable? = null

    fun load(): Boolean {
        return runCatching {
            val mapped = RhymeAssetStore.openMappedAsset(appContext, ASSET_NAME).order(ByteOrder.LITTLE_ENDIAN)
            val next = CandidateTable(mapped)
            table = next
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
        val result = if (normalized.isEmpty()) {
            emptyList()
        } else {
            table?.lookup(normalized, limit).orEmpty()
        }
        Log.d(
            TRACE_TAG,
            "rhyme_trace stage=v2_suggest word=$normalized count=${result.size} ms=${(System.nanoTime() - start) / 1_000_000.0}"
        )
        return result
    }

    private class CandidateTable(private val data: ByteBuffer) {
        val rowCount: Int
        private val rowTableOffset: Int
        private val stringTableOffset: Int
        private val words: Array<String>
        private val rowOffsets: IntArray

        init {
            require(data.limit() >= HEADER_SIZE) { "candidate table too small" }
            val magic = ByteArray(MAGIC.size)
            for (index in MAGIC.indices) magic[index] = data.get(index)
            require(magic.contentEquals(MAGIC)) { "bad candidate magic" }
            require(data.getShort(8).toInt() == VERSION) { "unsupported candidate version" }
            rowCount = data.getInt(10)
            rowTableOffset = data.getInt(14)
            stringTableOffset = data.getInt(18)
            require(rowCount >= 30_000) { "candidate row count too low" }
            require(rowTableOffset == HEADER_SIZE) { "bad candidate row offset" }
            require(rowTableOffset + (rowCount * ROW_SIZE) <= stringTableOffset) { "bad candidate string offset" }
            require(data.getInt(22) == checksum()) { "candidate checksum mismatch" }
            val rows = ArrayList<Pair<String, Int>>(rowCount)
            repeat(rowCount) { index ->
                val rowOffset = rowTableOffset + (index * ROW_SIZE)
                val wordOffset = data.getInt(rowOffset)
                val count = data.getShort(rowOffset + 4).toInt() and 0xFFFF
                require(count in 4..MAX_CANDIDATES) { "bad candidate count" }
                rows.add(readString(wordOffset) to rowOffset)
            }
            rows.sortBy { it.first }
            words = Array(rowCount) { rows[it].first }
            rowOffsets = IntArray(rowCount) { rows[it].second }
        }

        fun lookup(word: String, limit: Int): List<RhymeCandidate> {
            val index = words.binarySearch(word)
            if (index < 0) return emptyList()
            val rowOffset = rowOffsets[index]
            val count = data.getShort(rowOffset + 4).toInt() and 0xFFFF
            val out = ArrayList<RhymeCandidate>(minOf(limit, count))
            repeat(minOf(limit, count, MAX_CANDIDATES)) { candidateIndex ->
                val offset = data.getInt(rowOffset + 8 + (candidateIndex * 4))
                val candidate = readString(offset)
                if (candidate.isNotBlank()) {
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
        private const val ROW_SIZE = 32
        private const val VERSION = 2
        private const val MAX_CANDIDATES = 6
        private const val TRACE_TAG = "rhyme_trace"
        private val MAGIC = "TFCAND2".toByteArray(Charsets.US_ASCII)

        private fun normalize(word: String): String {
            return word.lowercase().replace(Regex("[^a-z']"), "").trim('\'')
        }
    }
}

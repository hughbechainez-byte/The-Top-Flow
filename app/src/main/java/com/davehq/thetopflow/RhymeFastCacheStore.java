package com.davehq.thetopflow;

import android.content.Context;

import com.davehq.thetopflow.rhyme.RhymeAssetStore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;

final class RhymeFastCacheStore {
    private static final String ASSET_NAME = "rhyme_fast_cache_v2.tfcache";
    private static final byte[] MAGIC = "TFCACHE2".getBytes(StandardCharsets.US_ASCII);
    private static final int HEADER_SIZE = 32;
    private static final int ROW_SIZE = 32;
    private static final int ROW_MAGIC_OFFSET = 8;
    private static final int ROW_COUNT_OFFSET = 10;
    private static final int ROW_TABLE_OFFSET_OFFSET = 14;
    private static final int STRING_TABLE_OFFSET_OFFSET = 18;
    private static final int CHECKSUM_OFFSET = 22;
    private static final int WORD_OFFSET_OFFSET = 0;
    private static final int WORD_COUNT_OFFSET = 4;
    private static final int SUGGESTION_OFFSET = 8;
    private static final int MAX_SUGGESTIONS = 6;

    private ByteBuffer mapped;
    private int rowCount;
    private int rowTableOffset;
    private int stringTableOffset;
    private String[] words;
    private int[] rowOffsets;

    boolean load(Context context) {
        try {
            mapped = RhymeAssetStore.openMappedAsset(context, ASSET_NAME);
            mapped.order(ByteOrder.LITTLE_ENDIAN);
            if (!validateHeader()) {
                mapped = null;
                return false;
            }
            if (!validateOffsets()) {
                mapped = null;
                return false;
            }
            ArrayList<RowIndex> rows = new ArrayList<>(rowCount);
            for (int i = 0; i < rowCount; i++) {
                int rowOffset = rowTableOffset + (i * ROW_SIZE);
                int wordOffset = mapped.getInt(rowOffset + WORD_OFFSET_OFFSET);
                int suggestionCount = mapped.getShort(rowOffset + WORD_COUNT_OFFSET) & 0xFFFF;
                if (!isOffsetInStringTable(wordOffset)) return false;
                String word = readString(wordOffset);
                if (word.isEmpty()) return false;
                if (suggestionCount < 4) return false;
                rows.add(new RowIndex(word, rowOffset));
            }
            rows.sort((left, right) -> left.word.compareTo(right.word));
            words = new String[rowCount];
            rowOffsets = new int[rowCount];
            for (int i = 0; i < rowCount; i++) {
                RowIndex row = rows.get(i);
                words[i] = row.word;
                rowOffsets[i] = row.offset;
            }
            return rowCount > 0;
        } catch (Throwable ignore) {
            mapped = null;
            rowOffsets = null;
            words = null;
            return false;
        }
    }

    boolean isLoaded() {
        return mapped != null && rowOffsets != null && words != null && rowCount > 0;
    }

    int rowCount() {
        return isLoaded() ? rowCount : 0;
    }

    String[] lookup(String normalizedWord, int limit) {
        if (!isLoaded() || normalizedWord == null || normalizedWord.isEmpty()) return null;
        int idx = Arrays.binarySearch(words, normalizedWord);
        if (idx < 0) return null;
        int rowOffset = rowOffsets[idx];
        int suggestionCount = mapped.getShort(rowOffset + WORD_COUNT_OFFSET) & 0xFFFF;
        if (suggestionCount <= 0) return null;
        int effective = Math.min(limit, MAX_SUGGESTIONS);
        int outCount = Math.min(effective, suggestionCount);
        String[] suggestions = new String[outCount];
        int found = 0;
        int suggestionStart = rowOffset + SUGGESTION_OFFSET;
        for (int i = 0; i < MAX_SUGGESTIONS && found < outCount; i++) {
            int suggestionOffset = mapped.getInt(suggestionStart + (i * 4));
            if (suggestionOffset == 0 || !isOffsetInStringTable(suggestionOffset)) continue;
            String suggestion = readString(suggestionOffset);
            if (!suggestion.isEmpty()) {
                suggestions[found++] = suggestion;
            }
        }
        if (found == 0) return null;
        if (found < outCount) return Arrays.copyOf(suggestions, found);
        return suggestions;
    }

    private String readString(int offset) {
        int cursor = offset;
        while (cursor < mapped.limit() && mapped.get(cursor) != 0) {
            cursor++;
        }
        int length = cursor - offset;
        if (length <= 0) return "";
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = mapped.get(offset + i);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean validateHeader() {
        if (mapped == null || mapped.limit() < HEADER_SIZE) return false;
        for (int i = 0; i < MAGIC.length; i++) {
            if (mapped.get(i) != MAGIC[i]) return false;
        }
        for (int i = MAGIC.length; i < 8; i++) {
            if (mapped.get(i) != 0) return false;
        }
        if ((mapped.getShort(ROW_MAGIC_OFFSET) & 0xFFFF) != 2) return false;
        rowCount = mapped.getInt(ROW_COUNT_OFFSET);
        rowTableOffset = mapped.getInt(ROW_TABLE_OFFSET_OFFSET);
        stringTableOffset = mapped.getInt(STRING_TABLE_OFFSET_OFFSET);
        int checksum = mapped.getInt(CHECKSUM_OFFSET);
        if (rowCount <= 0 || rowTableOffset < HEADER_SIZE) return false;
        return checksum == computeChecksum();
    }

    private int computeChecksum() {
        CRC32 crc = new CRC32();
        for (int i = 0; i < mapped.limit(); i++) {
            if (i >= CHECKSUM_OFFSET && i < CHECKSUM_OFFSET + 4) {
                continue;
            }
            crc.update(mapped.get(i));
        }
        return (int) crc.getValue();
    }

    private boolean validateOffsets() {
        if (rowTableOffset + (rowCount * ROW_SIZE) > stringTableOffset) return false;
        if (stringTableOffset >= mapped.limit()) return false;
        return stringTableOffset < mapped.limit();
    }

    private boolean isOffsetInStringTable(int offset) {
        return offset >= stringTableOffset && offset < mapped.limit();
    }

    private static final class RowIndex {
        final String word;
        final int offset;

        RowIndex(String word, int offset) {
            this.word = word;
            this.offset = offset;
        }
    }
}

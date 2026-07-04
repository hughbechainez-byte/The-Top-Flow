package com.davehq.thetopflow.rhyme

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.FileOutputStream
import java.io.IOException
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

private const val TAG = "RhymeAssetStore"

object RhymeAssetStore {
    private const val TRACE_TAG = "rhyme_trace"
    private const val FALLBACK_DIR = "rhyme-assets"

    fun openMappedAsset(context: Context, name: String): ByteBuffer {
        val startMs = System.currentTimeMillis()
        return try {
            openMappedFromFd(context.assets, name)
                .also { mapped ->
                    Log.d(TRACE_TAG, "rhyme_asset_map_opened name=$name size=${mapped.limit()}ms=${System.currentTimeMillis() - startMs}")
                }
        } catch (openException: IOException) {
            Log.d(TAG, "openFd failed for $name, using stream copy fallback", openException)
            openMappedFromFallback(context, name)
                .also { mapped ->
                    Log.d(TRACE_TAG, "rhyme_asset_map_fallback name=$name size=${mapped.limit()}ms=${System.currentTimeMillis() - startMs}")
                }
        }
    }

    private fun openMappedFromFd(assetManager: AssetManager, name: String): ByteBuffer {
        val assetFileDescriptor = assetManager.openFd(name)
        assetFileDescriptor.use {
            val start = it.startOffset
            val length = it.length
            val channel = FileInputStream(it.fileDescriptor).channel
            return channel.map(FileChannel.MapMode.READ_ONLY, start, length)
        }
    }

    private fun openMappedFromFallback(context: Context, name: String): ByteBuffer {
        val fallback = File(context.filesDir, "$FALLBACK_DIR/$name")
        if (!fallback.exists() || fallback.length() == 0L) {
            copyAssetToFilesDir(context, name, fallback)
        }
        val channel = FileInputStream(fallback).channel
        return channel.map(FileChannel.MapMode.READ_ONLY, 0, fallback.length())
    }

    private fun copyAssetToFilesDir(context: Context, name: String, destination: File) {
        destination.parentFile?.mkdirs()
        context.assets.open(name).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }
}

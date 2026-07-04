package com.davehq.thetopflow

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class TopFlowUpdate(
    val versionName: String,
    val versionCode: Int,
    val notes: String,
    val apkUrl: String
)

sealed class TopFlowUpdateCheckResult {
    data class UpdateAvailable(val update: TopFlowUpdate) : TopFlowUpdateCheckResult()
    data class NoUpdate(val currentVersionName: String) : TopFlowUpdateCheckResult()
}

class TopFlowUpdateManager(private val context: Context) {
    suspend fun checkForUpdate(): TopFlowUpdateCheckResult = withContext(Dispatchers.IO) {
        val manifest = readText(effectiveManifestUrl())
        val update = parseLatestUpdate(manifest)
        if (update == null) {
            TopFlowUpdateCheckResult.NoUpdate(BuildConfig.VERSION_NAME)
        } else {
            TopFlowUpdateCheckResult.UpdateAvailable(update)
        }
    }

    suspend fun downloadAndInstall(update: TopFlowUpdate) {
        val apk = withContext(Dispatchers.IO) { downloadApk(update) }
        withContext(Dispatchers.Main.immediate) { installApk(apk) }
    }

    private fun effectiveManifestUrl(): String {
        val configured = BuildConfig.UPDATE_MANIFEST_URL.trim()
        if (configured.isBlank()) return FALLBACK_APPCAST_URL
        return if (configured.contains("github.com", ignoreCase = true) &&
            configured.contains("/releases", ignoreCase = true)
        ) {
            FALLBACK_APPCAST_URL
        } else {
            configured
        }
    }

    private fun parseLatestUpdate(rawJson: String): TopFlowUpdate? {
        val root = JSONObject(rawJson)
        val candidates = ArrayList<TopFlowUpdate>()
        root.toUpdateOrNull()?.let(candidates::add)
        val versions = root.optJSONArray("versions")
        if (versions != null) {
            for (index in 0 until versions.length()) {
                versions.optJSONObject(index)?.toUpdateOrNull()?.let(candidates::add)
            }
        }
        return candidates
            .asSequence()
            .filter { it.versionCode > BuildConfig.VERSION_CODE }
            .filter { it.apkUrl.startsWith("https://", ignoreCase = true) }
            .maxByOrNull { it.versionCode }
    }

    private fun JSONObject.toUpdateOrNull(): TopFlowUpdate? {
        val versionCode = optInt("versionCode", 0)
        val apkUrl = optString("apkUrl").trim()
        if (versionCode <= 0 || apkUrl.isBlank()) return null
        return TopFlowUpdate(
            versionName = optString("versionName", versionCode.toString()).trim().ifBlank { versionCode.toString() },
            versionCode = versionCode,
            notes = optString("notes").trim(),
            apkUrl = apkUrl
        )
    }

    private fun readText(url: String): String {
        val connection = openConnection(url)
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun downloadApk(update: TopFlowUpdate): File {
        val dir = File(context.cacheDir, "updates")
        if (!dir.exists() && !dir.mkdirs()) {
            error("Could not prepare update cache.")
        }
        dir.listFiles()?.forEach { file ->
            if (file.isFile && (file.extension == "apk" || file.extension == "part")) {
                file.delete()
            }
        }
        val name = "the-top-flow-${update.versionName.safeFilePart()}.apk"
        val target = File(dir, name)
        val part = File(dir, "$name.part")
        val connection = openConnection(update.apkUrl)
        connection.inputStream.use { input ->
            part.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        if (part.length() < MIN_APK_BYTES || !part.hasApkSignature()) {
            part.delete()
            error("Downloaded update was not a valid APK.")
        }
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
        return target
    }

    private fun installApk(apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.files",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "*/*")
            setRequestProperty("User-Agent", "TheTopFlow/${BuildConfig.VERSION_NAME}")
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            error("Update server returned HTTP $code.")
        }
        return connection
    }

    private fun String.safeFilePart(): String {
        return lowercase(Locale.US).replace(Regex("[^a-z0-9._-]+"), "-").trim('-').ifBlank { "update" }
    }

    private fun File.hasApkSignature(): Boolean {
        inputStream().use { input ->
            return input.read() == 'P'.code && input.read() == 'K'.code
        }
    }

    private companion object {
        private const val FALLBACK_APPCAST_URL = "https://raw.githubusercontent.com/hughbechainez-byte/The-Top-Flow/main/appcast.json"
        private const val NETWORK_TIMEOUT_MS = 20_000
        private const val MIN_APK_BYTES = 64 * 1024
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

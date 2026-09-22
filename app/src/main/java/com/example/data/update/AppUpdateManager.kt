package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _config = MutableStateFlow(UpdateConfig())
    val config: StateFlow<UpdateConfig> = _config.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    fun setGithubRepo(repo: String) {
        val sanitized = repo.trim().removePrefix("https://github.com/").trim('/')
        _config.update { it.copy(githubRepo = sanitized) }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateCheckState.Idle
    }

    suspend fun checkForUpdates(currentVersion: String = "1.1.0"): UpdateCheckState = withContext(Dispatchers.IO) {
        _updateState.value = UpdateCheckState.Checking
        val repo = _config.value.githubRepo
        val url = "https://api.github.com/repos/$repo/releases/latest"

        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "OpenCode-Android-Updater")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "")
                val cleanRemoteVer = tagName.removePrefix("v").trim()
                val releaseTitle = json.optString("name", "Nová verze $tagName")
                val releaseNotes = json.optString("body", "Aktualizace přináší vylepšení výkonu a stability.")
                val publishedAt = json.optString("published_at", "")
                val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

                var downloadUrl = ""
                var apkFileName = "opencode-$cleanRemoteVer.apk"
                var apkSize = 0L

                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            apkFileName = name
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (downloadUrl.isEmpty()) {
                    downloadUrl = htmlUrl
                }

                _config.update { it.copy(lastCheckedTimestamp = System.currentTimeMillis()) }

                if (isNewerVersion(cleanRemoteVer, currentVersion)) {
                    val releaseInfo = AppReleaseInfo(
                        tagName = tagName,
                        versionName = cleanRemoteVer,
                        releaseTitle = releaseTitle,
                        releaseNotes = releaseNotes,
                        publishedAt = publishedAt,
                        downloadUrl = downloadUrl,
                        apkFileName = apkFileName,
                        apkSizeBytes = apkSize,
                        htmlUrl = htmlUrl
                    )
                    val result = UpdateCheckState.UpdateAvailable(releaseInfo)
                    _updateState.value = result
                    return@withContext result
                } else {
                    val result = UpdateCheckState.UpToDate(currentVersion)
                    _updateState.value = result
                    return@withContext result
                }
            } else {
                val errorMsg = if (response.code == 404) {
                    "Žádné vydání (release) nebylo v repozitáři '$repo' zatím nalezeno. Pro vytvoření vydání pushněte tag např. v1.1.0."
                } else {
                    "GitHub API vrátilo kód ${response.code}: ${response.message}"
                }
                val result = UpdateCheckState.Error(errorMsg)
                _updateState.value = result
                return@withContext result
            }
        } catch (e: Exception) {
            val errorMsg = "Nelze se spojit s GitHubem (${e.localizedMessage ?: "Chyba sítě"}). Zkontrolujte internetové připojení."
            val result = UpdateCheckState.Error(errorMsg)
            _updateState.value = result
            return@withContext result
        }
    }

    suspend fun downloadAndInstall(release: AppReleaseInfo): Boolean = withContext(Dispatchers.IO) {
        if (!release.downloadUrl.endsWith(".apk", ignoreCase = true)) {
            // If download URL is GitHub release page, open in browser
            openBrowser(release.htmlUrl)
            return@withContext true
        }

        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, release.apkFileName)

            val request = Request.Builder().url(release.downloadUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                openBrowser(release.htmlUrl)
                return@withContext false
            }

            val body = response.body!!
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastReportPercent = 0
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
                        if (percent != lastReportPercent) {
                            lastReportPercent = percent
                            _updateState.value = UpdateCheckState.Downloading(percent, downloadedBytes, totalBytes)
                        }
                    }
                }
            }

            _updateState.value = UpdateCheckState.ReadyToInstall(apkFile, release)
            launchInstaller(apkFile)
            return@withContext true
        } catch (e: Exception) {
            Log.e("AppUpdater", "Download failed: ${e.message}", e)
            openBrowser(release.htmlUrl)
            return@withContext false
        }
    }

    fun launchInstaller(apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdater", "Failed to launch installer intent: ${e.message}")
        }
    }

    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdater", "Could not open browser: ${e.message}")
        }
    }

    fun simulateNewVersion(version: String = "1.2.0") {
        val simulated = AppReleaseInfo(
            tagName = "v$version",
            versionName = version,
            releaseTitle = "OpenCode IDE $version - Stabilní vydání",
            releaseNotes = """
                ### 🚀 Co je nového ve verzi $version:
                * **YouTube Growth Agent:** Komplexní plán pro zvedání sledovanosti a retence diváků.
                * **Termux CLI Auto-Sync:** Automatické zrcadlení relací z terminálu Termux do grafického IDE.
                * **Project Workspaces:** Samostatné pracovní složky pro každý projekt.
                * **Nativní In-App Updater:** Automatické stahování a instalace aktualizací přímo v aplikaci.
            """.trimIndent(),
            publishedAt = "2026-09-22T15:30:00Z",
            downloadUrl = "https://github.com/${_config.value.githubRepo}/releases/tag/v$version",
            apkFileName = "opencode-v$version.apk",
            apkSizeBytes = 28500000L,
            htmlUrl = "https://github.com/${_config.value.githubRepo}/releases/tag/v$version"
        )
        _updateState.value = UpdateCheckState.UpdateAvailable(simulated)
    }

    /**
     * Compares semantic versions (e.g. 1.2.0 vs 1.1.0)
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return remote != current
        }
    }
}

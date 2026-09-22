package com.example.data.update

import java.io.File

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val downloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val htmlUrl: String
)

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpdateAvailable(val release: AppReleaseInfo) : UpdateCheckState()
    data class UpToDate(val currentVersion: String) : UpdateCheckState()
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateCheckState()
    data class ReadyToInstall(val apkFile: File, val release: AppReleaseInfo) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

data class UpdateConfig(
    val githubRepo: String = "p-p-lukes892/opencode-android",
    val autoCheckOnLaunch: Boolean = true,
    val allowPrereleases: Boolean = false,
    val lastCheckedTimestamp: Long? = null
)

package com.zekr.widget.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = ""
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url")
    val htmlUrl: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList()
)

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
    val downloadUrl: String,
    val releasePageUrl: String,
    val releaseNotes: String
)

object UpdateChecker {
    const val GITHUB_OWNER = "mbadr2200"
    const val GITHUB_REPO = "werd"
    const val RELEASES_PAGE_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.1"
        } catch (_: Exception) {
            "1.1"
        }
    }

    suspend fun checkForUpdate(context: Context): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersionName(context)
        runCatching {
            val connection = (URL(LATEST_RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Werd-Android-App/$currentVersion")
                connectTimeout = 8000
                readTimeout = 8000
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("HTTP $responseCode")
                }

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val release = json.decodeFromString<GitHubRelease>(responseBody)

                val latestClean = normalizeVersion(release.tagName.ifBlank { release.name })
                val currentClean = normalizeVersion(currentVersion)

                val isNewer = isRemoteVersionNewer(currentClean, latestClean)

                // Prefer direct APK asset link if attached to the GitHub Release, fallback to release HTML page
                val apkAssetUrl = release.assets
                    .firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?.browserDownloadUrl

                val targetDownloadUrl = apkAssetUrl?.takeIf { it.isNotBlank() }
                    ?: release.htmlUrl.ifBlank { RELEASES_PAGE_URL }

                AppUpdateInfo(
                    currentVersion = currentClean,
                    latestVersion = latestClean.ifBlank { currentClean },
                    isUpdateAvailable = isNewer,
                    downloadUrl = targetDownloadUrl,
                    releasePageUrl = release.htmlUrl.ifBlank { RELEASES_PAGE_URL },
                    releaseNotes = release.body.trim()
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun normalizeVersion(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()
    }

    internal fun isRemoteVersionNewer(current: String, remote: String): Boolean {
        if (remote.isBlank()) return false
        val currentParts = current.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val remoteParts = remote.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLength = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLength) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}

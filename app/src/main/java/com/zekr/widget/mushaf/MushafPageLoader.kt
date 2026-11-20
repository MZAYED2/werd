package com.zekr.widget.mushaf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object MushafPageLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Memory cache holding up to 12 pages in memory (~20MB max footprint)
    private val memoryCache = object : LruCache<Int, Bitmap>(12) {
        override fun sizeOf(key: Int, value: Bitmap): Int = 1
    }

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, "mushaf_pages_v2")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPageFile(context: Context, page: Int): File {
        return File(getStorageDir(context), String.format(Locale.US, "page_1920_%03d.png", page))
    }

    fun isPageCached(context: Context, page: Int): Boolean {
        val file = getPageFile(context, page)
        return file.exists() && file.length() > 5000L
    }

    private fun getPageUrl(page: Int): String {
        return String.format(Locale.US, "https://files.quran.app/hafs/madani/width_1920/page%03d.png", page)
    }

    private fun getFallbackUrl(page: Int): String {
        return String.format(Locale.US, "https://android.quran.com/data/width_1920/page%03d.png", page)
    }

    suspend fun loadPage(context: Context, page: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (page !in 1..MushafData.TOTAL_PAGES) return@withContext null

        // 1. Check in-memory LRU cache
        memoryCache.get(page)?.let { return@withContext it }

        // 2. Check local disk storage
        val localFile = getPageFile(context, page)
        if (localFile.exists() && localFile.length() > 5000L) {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            if (bitmap != null) {
                memoryCache.put(page, bitmap)
                return@withContext bitmap
            }
        }

        // 3. Download from network with redirect support and save to disk
        val downloaded = downloadPageToDisk(context, page)
        if (downloaded != null) {
            memoryCache.put(page, downloaded)
        }
        return@withContext downloaded
    }

    private fun downloadPageToDisk(context: Context, page: Int): Bitmap? {
        val localFile = getPageFile(context, page)
        val tempFile = File(getStorageDir(context), String.format(Locale.US, "page_%03d.tmp", page))

        val urlsToTry = listOf(getPageUrl(page), getFallbackUrl(page))

        for (urlString in urlsToTry) {
            var connection: HttpURLConnection? = null
            try {
                var currentUrl = urlString
                var redirects = 0
                while (redirects < 4) {
                    val url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 12_000
                        readTimeout = 15_000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "WerdApp-Android")
                    }
                    val code = connection.responseCode
                    if (code in 300..399) {
                        val newLocation = connection.getHeaderField("Location") ?: break
                        connection.disconnect()
                        currentUrl = newLocation
                        redirects++
                    } else if (code == 200) {
                        connection.inputStream.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (tempFile.exists() && tempFile.length() > 5000L) {
                            if (localFile.exists()) localFile.delete()
                            tempFile.renameTo(localFile)
                            return BitmapFactory.decodeFile(localFile.absolutePath)
                        }
                        break
                    } else {
                        break
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            } finally {
                connection?.disconnect()
                if (tempFile.exists()) tempFile.delete()
            }
        }
        return null
    }

    fun preloadAdjacent(context: Context, currentPage: Int) {
        val appContext = context.applicationContext
        scope.launch {
            val pagesToPreload = listOf(currentPage - 1, currentPage + 1, currentPage - 2, currentPage + 2)
                .filter { it in 1..MushafData.TOTAL_PAGES }

            for (page in pagesToPreload) {
                if (memoryCache.get(page) == null && !isPageCached(appContext, page)) {
                    loadPage(appContext, page)
                }
            }
        }
    }
}

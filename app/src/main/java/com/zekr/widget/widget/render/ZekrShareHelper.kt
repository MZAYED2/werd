package com.zekr.widget.widget.render

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.WidgetSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ZekrShareHelper {

    fun buildFormattedShareText(item: ZekrEntity): String {
        val citation = listOf(item.source.trim(), item.reference.trim())
            .filter { it.isNotBlank() }
            .joinToString(" • ")
        return buildString {
            append("«")
            append(item.arabicText.trim())
            append("»")
            if (citation.isNotBlank()) {
                append("\n\n— ")
                append(citation)
            }
            append("\n\nعبر تطبيق وِرد")
        }
    }

    suspend fun shareZekrCard(
        context: Context,
        item: ZekrEntity,
        settings: WidgetSettings
    ) {
        val appContext = context.applicationContext
        val shareText = buildFormattedShareText(item)

        val uri = withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = GlassRenderer.createFullWidgetBitmap(
                    context = appContext,
                    width = 1080,
                    height = 720,
                    item = item,
                    colorPreset = settings.colorPreset,
                    isGradient = settings.isGradient,
                    primaryColor = settings.primaryColor,
                    secondaryColor = settings.secondaryColor,
                    gradientDirection = settings.gradientDirection,
                    opacity = settings.glassOpacity.coerceAtLeast(0.68f),
                    blurGrade = settings.blurGrade,
                    pattern = settings.pattern,
                    patternOpacity = settings.patternOpacity,
                    patternScale = settings.patternScale,
                    font = settings.font,
                    fontSizeSp = settings.fontSizeSp.coerceAtLeast(18),
                    cornerRadius = 56f,
                    showInteractiveBar = false
                )

                val cacheDir = File(appContext.cacheDir, "shared_cards").apply { mkdirs() }
                // Clean old shared files to avoid cache bloat
                cacheDir.listFiles()?.forEach { oldFile ->
                    if (oldFile.isFile) oldFile.delete()
                }

                val outFile = File(cacheDir, "werd_${item.id}_${System.currentTimeMillis()}.png")
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }
                FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    outFile
                )
            }.getOrNull()
        }

        val sendIntent = if (uri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                clipData = ClipData.newRawUri("werd_card", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        }

        val chooser = Intent.createChooser(sendIntent, "مشاركة الذكر").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) {
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }
}

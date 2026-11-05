package com.zekr.widget.widget.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.PatternType

object GlassRenderer {

    fun createGlassBackgroundBitmap(
        width: Int,
        height: Int,
        colorPreset: GlassColorPreset,
        opacity: Float,
        pattern: PatternType,
        cornerRadius: Float = 54f
    ): Bitmap {
        val safeW = if (width <= 0) 600 else width
        val safeH = if (height <= 0) 360 else height
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rect = RectF(2f, 2f, safeW - 2f, safeH - 2f)

        // Base Glass Color with Opacity
        val baseColor = colorPreset.hexColor.toInt()
        val alphaInt = (opacity.coerceIn(0.10f, 0.95f) * 255).toInt()

        val topColor = Color.argb(
            alphaInt,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )
        // Slightly darker tint at the bottom for frosted depth
        val bottomAlpha = (alphaInt * 1.15f).toInt().coerceAtMost(255)
        val bottomColor = Color.argb(
            bottomAlpha,
            (Color.red(baseColor) * 0.85f).toInt(),
            (Color.green(baseColor) * 0.85f).toInt(),
            (Color.blue(baseColor) * 0.85f).toInt()
        )

        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, safeW.toFloat(), safeH.toFloat(),
                topColor, bottomColor,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }

        // Draw Frosted Body
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glassPaint)

        // Draw Pattern Overlay
        val patternColor = if (colorPreset == GlassColorPreset.FROSTED_WHITE) {
            Color.BLACK
        } else {
            Color.WHITE
        }
        val patternAlpha = if (colorPreset == GlassColorPreset.FROSTED_WHITE) 22 else 32
        PatternDrawables.drawPattern(
            canvas = canvas,
            pattern = pattern,
            width = safeW.toFloat(),
            height = safeH.toFloat(),
            patternColor = patternColor,
            alpha = patternAlpha
        )

        // Draw Subtle 1.2dp Glass Highlight Border (Glassmorphism Edge)
        val borderHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            val topBorderColor = Color.argb(90, 255, 255, 255)
            val bottomBorderColor = Color.argb(20, 255, 255, 255)
            shader = LinearGradient(
                0f, 0f, 0f, safeH.toFloat(),
                topBorderColor, bottomBorderColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderHighlightPaint)

        return bitmap
    }
}

package com.zekr.widget.widget.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetFont
import com.zekr.widget.ui.theme.FontHelper

object GlassRenderer {

    fun createGlassBackgroundBitmap(
        width: Int,
        height: Int,
        colorPreset: GlassColorPreset,
        opacity: Float,
        pattern: PatternType,
        patternOpacity: Float = 0.25f,
        patternScale: Float = 1.0f,
        cornerRadius: Float = 54f
    ): Bitmap {
        val safeW = if (width <= 0) 680 else width
        val safeH = if (height <= 0) 380 else height
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rect = RectF(2f, 2f, safeW - 2f, safeH - 2f)

        val baseColor = colorPreset.hexColor.toInt()
        val alphaInt = (opacity.coerceIn(0.10f, 0.95f) * 255).toInt()

        val topColor = Color.argb(
            alphaInt,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )
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

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glassPaint)

        val patternColor = if (colorPreset == GlassColorPreset.FROSTED_WHITE) Color.BLACK else Color.WHITE
        val patternAlpha = (patternOpacity.coerceIn(0.04f, 0.95f) * 255).toInt()
        PatternDrawables.drawPattern(
            canvas = canvas,
            pattern = pattern,
            width = safeW.toFloat(),
            height = safeH.toFloat(),
            patternColor = patternColor,
            alpha = patternAlpha,
            scale = patternScale
        )

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

    fun createFullWidgetBitmap(
        context: Context,
        width: Int,
        height: Int,
        item: ZekrEntity,
        colorPreset: GlassColorPreset,
        opacity: Float,
        pattern: PatternType,
        patternOpacity: Float,
        patternScale: Float,
        font: WidgetFont,
        fontSizeSp: Int,
        cornerRadius: Float = 54f
    ): Bitmap {
        val safeW = if (width <= 0) 680 else width
        val safeH = if (height <= 0) 380 else height
        val bitmap = createGlassBackgroundBitmap(
            width = safeW,
            height = safeH,
            colorPreset = colorPreset,
            opacity = opacity,
            pattern = pattern,
            patternOpacity = patternOpacity,
            patternScale = patternScale,
            cornerRadius = cornerRadius
        )
        val canvas = Canvas(bitmap)

        val textColor = colorPreset.textColor.toInt()
        val accentColor = colorPreset.accentColor.toInt()
        val customTypeface = FontHelper.getTypeface(context, font)

        // 1. Top Header: Type label (Right) & Refresh Hint (Left)
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            textSize = 20f
            typeface = Typeface.DEFAULT
        }

        // Draw hint on left
        canvas.drawText("⟳ انقر للتبديل", 36f, 48f, hintPaint)

        // Draw category on right
        val headerText = item.type.labelArabic
        val headerWidth = headerPaint.measureText(headerText)
        canvas.drawText(headerText, safeW - 36f - headerWidth, 48f, headerPaint)

        // 2. Arabic Zekr Text (Center) with the chosen custom Typeface
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = fontSizeSp * 2.1f
            typeface = customTypeface
            textAlign = Paint.Align.CENTER
        }

        val horizontalPadding = 56
        val maxTextWidth = (safeW - horizontalPadding * 2).coerceAtLeast(200)

        val staticLayout = StaticLayout.Builder.obtain(
            item.arabicText, 0, item.arabicText.length, textPaint, maxTextWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(8f, 1.25f)
            .setIncludePad(false)
            .build()

        val centerY = safeH * 0.52f
        val startY = (centerY - (staticLayout.height / 2f)).coerceAtLeast(64f)

        canvas.save()
        canvas.translate(safeW / 2f, startY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 3. Footer: Source & Reference
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            textSize = 21f
            typeface = customTypeface
            textAlign = Paint.Align.CENTER
        }
        val sourceText = if (item.reference.isNotEmpty()) "${item.source} • ${item.reference}" else item.source
        canvas.drawText(sourceText, safeW / 2f, safeH - 24f, footerPaint)

        return bitmap
    }
}

package com.zekr.widget.widget.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.AdaptiveContrast
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.GradientDirection
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetFont
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.ui.theme.FontHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GlassRenderer {

    fun createGlassBackgroundBitmap(
        width: Int,
        height: Int,
        colorPreset: GlassColorPreset = GlassColorPreset.EMERALD_SAGE,
        isGradient: Boolean = false,
        primaryColor: Long = colorPreset.hexColor,
        secondaryColor: Long = 0xFF021B0F,
        gradientDirection: GradientDirection = GradientDirection.DIAGONAL,
        opacity: Float,
        blurGrade: Float = 0.5f,
        pattern: PatternType,
        patternOpacity: Float = 0.25f,
        patternScale: Float = 1.0f,
        cornerRadius: Float = 54f,
        context: Context? = null
    ): Bitmap {
        val safeW = if (width <= 0) 680 else width
        val safeH = if (height <= 0) 380 else height
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rect = RectF(0f, 0f, safeW.toFloat(), safeH.toFloat())
        val alphaInt = (opacity.coerceIn(0.10f, 0.95f) * 255).toInt()
        val clampedBlur = blurGrade.coerceIn(0f, 1f)

        // Strictly clip canvas to rounded card boundary so patterns never overflow
        val clipPath = Path().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        // Compute perceived luminance for smart contrast
        val lum = if (isGradient) {
            val r1 = (primaryColor shr 16 and 0xFF)
            val g1 = (primaryColor shr 8 and 0xFF)
            val b1 = (primaryColor and 0xFF)
            val r2 = (secondaryColor shr 16 and 0xFF)
            val g2 = (secondaryColor shr 8 and 0xFF)
            val b2 = (secondaryColor and 0xFF)
            (0.299f * (r1 + r2) / 2f + 0.587f * (g1 + g2) / 2f + 0.114f * (b1 + b2) / 2f) / 255f
        } else {
            val r = (primaryColor shr 16 and 0xFF)
            val g = (primaryColor shr 8 and 0xFF)
            val b = (primaryColor and 0xFF)
            (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        }

        val patternColor = if (lum > 0.62f) Color.BLACK else Color.WHITE
        val patternAlpha = (patternOpacity.coerceIn(0f, 0.95f) * 255).toInt()

        // Draw card background base gradient
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (isGradient) {
                val c1 = primaryColor.toInt()
                val c2 = secondaryColor.toInt()
                val col1 = Color.argb(alphaInt, Color.red(c1), Color.green(c1), Color.blue(c1))
                val col2Alpha = (alphaInt * 1.15f).toInt().coerceAtMost(255)
                val col2 = Color.argb(col2Alpha, Color.red(c2), Color.green(c2), Color.blue(c2))

                shader = when (gradientDirection) {
                    GradientDirection.DIAGONAL -> LinearGradient(
                        0f, 0f, safeW.toFloat(), safeH.toFloat(),
                        col1, col2, Shader.TileMode.CLAMP
                    )
                    GradientDirection.VERTICAL -> LinearGradient(
                        0f, 0f, 0f, safeH.toFloat(),
                        col1, col2, Shader.TileMode.CLAMP
                    )
                    GradientDirection.HORIZONTAL -> LinearGradient(
                        0f, 0f, safeW.toFloat(), 0f,
                        col1, col2, Shader.TileMode.CLAMP
                    )
                    GradientDirection.RADIAL -> RadialGradient(
                        safeW / 2f, safeH / 2f,
                        max(safeW, safeH) * 0.72f,
                        col1, col2, Shader.TileMode.CLAMP
                    )
                }
            } else {
                val baseColor = primaryColor.toInt()
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

                shader = LinearGradient(
                    0f, 0f, safeW.toFloat(), safeH.toFloat(),
                    topColor, bottomColor,
                    Shader.TileMode.CLAMP
                )
            }
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glassPaint)

        // Draw the Islamic geometric pattern or motifs (sharp and crisp per user request)
        PatternDrawables.drawPattern(
            canvas = canvas,
            pattern = pattern,
            width = safeW.toFloat(),
            height = safeH.toFloat(),
            patternColor = patternColor,
            alpha = patternAlpha,
            scale = patternScale,
            context = context
        )

        // Frosted glass milky sheen & light dispersion
        if (clampedBlur > 0.01f) {
            val frostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val maxAlpha = (clampedBlur * 0.32f * 255).toInt().coerceIn(0, 255)
                val minAlpha = (clampedBlur * 0.06f * 255).toInt().coerceIn(0, 255)
                val midAlpha = (clampedBlur * 0.18f * 255).toInt().coerceIn(0, 255)
                shader = LinearGradient(
                    0f, 0f, safeW.toFloat(), safeH.toFloat(),
                    intArrayOf(
                        Color.argb(maxAlpha, 255, 255, 255),
                        Color.argb(minAlpha, 255, 255, 255),
                        Color.argb(midAlpha, 255, 255, 255)
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, frostPaint)
        }

        // Frosted glass sandblasted micro-texture (noise/grain stippling)
        if (clampedBlur > 0.05f) {
            val noisePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val step = 4
            val noiseAlphaBase = (clampedBlur * 30).toInt().coerceIn(0, 255)
            if (noiseAlphaBase > 0) {
                var seed = 24601
                for (y in 0 until safeH step step) {
                    for (x in 0 until safeW step step) {
                        seed = (seed * 1103515245 + 12345) and 0x7fffffff
                        val rand = (seed shr 16) and 0xFF
                        if (rand > 170) {
                            val dotAlpha = ((rand - 170) * noiseAlphaBase / 85).coerceIn(0, 255)
                            noisePaint.color = Color.argb(dotAlpha, 255, 255, 255)
                            canvas.drawPoint(x.toFloat(), y.toFloat(), noisePaint)
                        }
                    }
                }
            }
        }

        canvas.restore()

        return bitmap
    }

    /**
     * Ultra-fast O(1) StackBlur algorithm for authentic frosted glass diffusion
     */
    fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return sentBitmap

        val w = sentBitmap.width
        val h = sentBitmap.height
        val pix = IntArray(w * h)
        sentBitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val a = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var asum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(4) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var aoutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        var ainsum: Int

        for (row in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            ainsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            aoutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            asum = 0
            for (step in -radius..radius) {
                p = pix[yi + min(wm, max(step, 0))]
                sir = stack[step + radius]
                sir[0] = (p shr 16) and 0xff
                sir[1] = (p shr 8) and 0xff
                sir[2] = p and 0xff
                sir[3] = (p shr 24) and 0xff
                rbs = r1 - abs(step)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                asum += sir[3] * rbs
                if (step > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    ainsum += sir[3]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    aoutsum += sir[3]
                }
            }
            stackpointer = radius

            for (col in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                a[yi] = dv[asum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                asum -= aoutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                aoutsum -= sir[3]

                if (row == 0) {
                    vmin[col] = min(col + radius + 1, wm)
                }
                p = pix[yw + vmin[col]]

                sir[0] = (p shr 16) and 0xff
                sir[1] = (p shr 8) and 0xff
                sir[2] = p and 0xff
                sir[3] = (p shr 24) and 0xff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                ainsum += sir[3]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                asum += ainsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                aoutsum += sir[3]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                ainsum -= sir[3]

                yi++
            }
            yw += w
        }

        for (col in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            ainsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            aoutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            asum = 0
            yp = -radius * w
            for (step in -radius..radius) {
                yi = max(0, yp) + col
                sir = stack[step + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                sir[3] = a[yi]
                rbs = r1 - abs(step)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                asum += a[yi] * rbs
                if (step > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    ainsum += sir[3]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    aoutsum += sir[3]
                }
                if (step < hm) {
                    yp += w
                }
            }
            yi = col
            stackpointer = radius
            for (row in 0 until h) {
                pix[yi] = (dv[asum] shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                asum -= aoutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                aoutsum -= sir[3]

                if (col == 0) {
                    vmin[row] = min(row + r1, hm) * w
                }
                p = col + vmin[row]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                sir[3] = a[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                ainsum += sir[3]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                asum += ainsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                aoutsum += sir[3]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                ainsum -= sir[3]

                yi += w
            }
        }

        sentBitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return sentBitmap
    }

    fun createFullWidgetBitmap(
        context: Context,
        width: Int,
        height: Int,
        item: ZekrEntity,
        colorPreset: GlassColorPreset = GlassColorPreset.EMERALD_SAGE,
        isGradient: Boolean = false,
        primaryColor: Long = colorPreset.hexColor,
        secondaryColor: Long = 0xFF021B0F,
        gradientDirection: GradientDirection = GradientDirection.DIAGONAL,
        opacity: Float,
        blurGrade: Float = 0.5f,
        pattern: PatternType,
        patternOpacity: Float,
        patternScale: Float,
        font: WidgetFont,
        fontSizeSp: Int,
        cornerRadius: Float = 54f,
        currentRepetition: Int = 0,
        isFavorite: Boolean = false,
        isPinned: Boolean = false,
        showInteractiveBar: Boolean = true,
        widthDp: Float = 0f,
        heightDp: Float = 0f,
        density: Float = 0f
    ): Bitmap {
        val safeW = if (width <= 0) 680 else width
        val safeH = if (height <= 0) 380 else height

        val resolvedDensity = if (density > 0f) density else (context.resources.displayMetrics.density).coerceAtLeast(1f)
        val resolvedWidthDp = if (widthDp > 0f) widthDp else (safeW / resolvedDensity)
        val resolvedHeightDp = if (heightDp > 0f) heightDp else (safeH / resolvedDensity)
        val scaleFactor = (safeW.toFloat() / (resolvedWidthDp * resolvedDensity)).coerceIn(0.2f, 5f)

        val layout = WidgetLayoutGeometry.compute(
            widthDp = resolvedWidthDp,
            heightDp = resolvedHeightDp,
            density = resolvedDensity,
            scaleFactor = scaleFactor,
            hasMultiRep = item.repetition > 1,
            interactive = showInteractiveBar
        )

        val uiScale = min(safeW / 680f, safeH / 380f).coerceIn(0.55f, 2.2f)
        val radiusScale = min(safeW / 680f, safeH / 380f).coerceIn(0.45f, 2.6f)
        val scaledRadius = (cornerRadius * radiusScale).coerceAtMost(min(safeW, safeH) / 2f)
        val bitmap = createGlassBackgroundBitmap(
            width = safeW,
            height = safeH,
            colorPreset = colorPreset,
            isGradient = isGradient,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            gradientDirection = gradientDirection,
            opacity = opacity,
            blurGrade = blurGrade,
            pattern = pattern,
            patternOpacity = patternOpacity,
            patternScale = patternScale,
            cornerRadius = scaledRadius,
            context = context
        )
        val canvas = Canvas(bitmap)

        // Strictly clip all foreground drawing to the rounded card boundary
        val rect = RectF(0f, 0f, safeW.toFloat(), safeH.toFloat())
        val clipPath = Path().apply {
            addRoundRect(rect, scaledRadius, scaledRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        // Opacity-aware contrast: translucent light glass over dark wallpaper needs white text
        val textColor = AdaptiveContrast.textColor(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            isGradient = isGradient,
            glassOpacity = opacity
        ).toInt()
        val accentColor = AdaptiveContrast.accentColor(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            isGradient = isGradient,
            glassOpacity = opacity
        ).toInt()
        val customTypeface = FontHelper.getTypeface(context, font)

        // 1. Arabic Zekr Text (Center) using unified geometry and adaptive font sizing
        val maxTextWidth = layout.maxTextWidthPx.roundToInt().coerceAtLeast(32)
        val availableHeightForZekr = layout.availableTextHeightPx

        val displayText = item.getFormattedArabicText()

        val isCompact = layout.mode == WidgetDisplayMode.COMPACT
        val isTallWidget = layout.mode == WidgetDisplayMode.EXPANDED

        val targetSize = when (layout.mode) {
            WidgetDisplayMode.COMPACT -> (fontSizeSp * 0.95f) * layout.toPx(1f)
            WidgetDisplayMode.MEDIUM -> (fontSizeSp * 1.08f) * layout.toPx(1f)
            WidgetDisplayMode.EXPANDED -> (fontSizeSp * 1.25f) * layout.toPx(1f)
        }
        val minimumSize = when (layout.mode) {
            WidgetDisplayMode.COMPACT -> 11f * layout.toPx(1f)
            WidgetDisplayMode.MEDIUM -> 12.5f * layout.toPx(1f)
            WidgetDisplayMode.EXPANDED -> 14f * layout.toPx(1f)
        }

        val maxLinesAllowed = when (layout.mode) {
            WidgetDisplayMode.COMPACT -> if (layout.heightDp < 55f) 1 else 2
            WidgetDisplayMode.MEDIUM -> 4
            WidgetDisplayMode.EXPANDED -> 7
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = customTypeface
        }

        // Optical centering for all Quranic verses and dhikr ensures visual equilibrium and elegance
        val textAlignment = Layout.Alignment.ALIGN_CENTER

        var low = minimumSize
        var high = targetSize.coerceAtLeast(low)
        var bestLayout = createArabicStaticLayout(
            displayText, textPaint.apply { textSize = low }, maxTextWidth, layout.scaleFactor,
            maxLines = maxLinesAllowed,
            ellipsize = TextUtils.TruncateAt.END,
            alignment = textAlignment
        )

        if (bestLayout.height <= availableHeightForZekr) {
            repeat(8) {
                val mid = (low + high) / 2f
                val candidate = createArabicStaticLayout(
                    displayText, textPaint.apply { textSize = mid }, maxTextWidth, layout.scaleFactor,
                    maxLines = maxLinesAllowed,
                    ellipsize = TextUtils.TruncateAt.END,
                    alignment = textAlignment
                )
                val fits = candidate.height <= availableHeightForZekr &&
                    (0 until candidate.lineCount).all { candidate.getLineWidth(it) <= maxTextWidth + 1.5f }
                if (fits) {
                    low = mid
                    bestLayout = candidate
                } else {
                    high = mid
                }
            }
            textPaint.textSize = low
        } else {
            val fittingLines = (0 until bestLayout.lineCount)
                .count { bestLayout.getLineBottom(it) <= availableHeightForZekr }
            bestLayout = createArabicStaticLayout(
                displayText, textPaint, maxTextWidth, layout.scaleFactor,
                maxLines = fittingLines.coerceIn(1, maxLinesAllowed),
                ellipsize = TextUtils.TruncateAt.END,
                alignment = textAlignment
            )
        }

        val topPadding = layout.textTopPaddingPx
        val extraHeight = (availableHeightForZekr - bestLayout.height).coerceAtLeast(0f)
        val startY = if (isCompact) {
            (safeH - bestLayout.height) / 2f
        } else {
            topPadding + extraHeight * if (isTallWidget) 0.22f else 0.45f
        }
        val startX = (safeW - maxTextWidth) / 2f

        canvas.save()
        if (isCompact) {
            canvas.clipRect(startX, 0f, safeW.toFloat() - startX, safeH.toFloat())
        } else {
            canvas.clipRect(0f, topPadding, safeW.toFloat(), topPadding + availableHeightForZekr)
        }
        canvas.translate(startX, startY)
        bestLayout.draw(canvas)
        canvas.restore()

        if (showInteractiveBar) {
            val barCenterY = layout.barCenterYPx
            val btnRadius = layout.drawnButtonRadiusPx
            val menuPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                strokeCap = Paint.Cap.ROUND
                strokeWidth = btnRadius * .12f
            }
            drawActionButtonPill(canvas, layout.menuCxPx, barCenterY, btnRadius, if (isFavorite) accentColor else textColor, highlighted = isFavorite)
            for (line in -1..1) {
                val y = barCenterY + line * btnRadius * .29f
                canvas.drawLine(layout.menuCxPx - btnRadius * .43f, y, layout.menuCxPx + btnRadius * .43f, y, menuPaint)
            }
            drawActionButtonPill(canvas, layout.tasbeehCxPx, barCenterY, btnRadius, accentColor, highlighted = false)
            drawTasbeehIcon(canvas, layout.tasbeehCxPx, barCenterY, btnRadius * .60f, accentColor)

            if (layout.mode != WidgetDisplayMode.COMPACT) {
                drawActionButtonPill(canvas, layout.openWerdCxPx, barCenterY, btnRadius, textColor, highlighted = false)
                drawWerdLogoIcon(canvas, layout.openWerdCxPx, barCenterY, btnRadius * 1.30f, textColor, context)
            }

            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = layout.toPx(11f)
                typeface = customTypeface
            }
            layout.headerCenterYDp?.let { headerY ->
                val heading = if (layout.hasMultiRep) {
                    "وِرد  ·  ${ZekrRepository.toArabicDigits(currentRepetition.coerceIn(0, item.repetition))}/${ZekrRepository.toArabicDigits(item.repetition)}"
                } else "وِرد"
                val header = createArabicStaticLayout(
                    heading, labelPaint, maxTextWidth, layout.scaleFactor, maxLines = 1,
                    ellipsize = TextUtils.TruncateAt.END, alignment = Layout.Alignment.ALIGN_CENTER
                )
                canvas.save()
                canvas.translate(startX, layout.toPx(headerY) - header.height / 2f)
                header.draw(canvas)
                canvas.restore()
                if (isPinned) {
                    drawPinBadge(canvas, startX + layout.toPx(7f), layout.toPx(headerY), uiScale * .75f, textColor, accentColor)
                }
            }
            val citationWidth = layout.maxCitationWidthPx.roundToInt()
            if (citationWidth >= layout.toPx(23f)) {
                val citation = createArabicStaticLayout(
                    item.source.ifBlank { item.getConciseCitation() },
                    labelPaint, citationWidth, layout.scaleFactor,
                    maxLines = if (layout.barHeightDp >= 40f) 2 else 1,
                    ellipsize = TextUtils.TruncateAt.END
                )
                canvas.save()
                canvas.translate(layout.citationCxPx - citationWidth / 2f, barCenterY - citation.height / 2f)
                citation.draw(canvas)
                canvas.restore()
            }
        } else {
            // Compact Mode: Citation only, full width - enhanced contrast and legibility
            val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(240, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
                textSize = (18f * layout.scaleFactor).coerceIn(12.5f, 23f)
                typeface = customTypeface
                textAlign = Paint.Align.CENTER
                setShadowLayer(3.5f * layout.scaleFactor, 0f, 1f * layout.scaleFactor, Color.argb(160, 0, 0, 0))
            }

            val maxFooterWidth = (safeW - 48f).coerceAtLeast(32f)
            var citationText = item.getConciseCitation()
            if (footerPaint.measureText(citationText) > maxFooterWidth) {
                val count = footerPaint.breakText(citationText, true, maxFooterWidth - 16f, null)
                citationText = citationText.take(count).trimEnd() + "…"
            }
            val footerY = safeH - 18f
            canvas.drawText(citationText, safeW / 2f, footerY, footerPaint)
        }

        canvas.restore()

        return bitmap
    }

    private fun drawActionButtonPill(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        textColor: Int,
        highlighted: Boolean
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (highlighted) {
                Color.argb(65, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            } else {
                Color.argb(34, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            }
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (radius * 0.08f).coerceAtLeast(1f)
            color = if (highlighted) {
                Color.argb(150, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            } else {
                Color.argb(55, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            }
        }
        canvas.drawCircle(cx, cy, radius, borderPaint)
    }

    private fun drawHeartIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        textColor: Int,
        filled: Boolean
    ) {
        val path = Path().apply {
            val topY = cy - size * 0.35f
            val bottomY = cy + size * 0.85f
            moveTo(cx, bottomY)
            cubicTo(
                cx - size * 1.15f, cy + size * 0.25f,
                cx - size * 1.05f, cy - size * 0.85f,
                cx, topY
            )
            cubicTo(
                cx + size * 1.05f, cy - size * 0.85f,
                cx + size * 1.15f, cy + size * 0.25f,
                cx, bottomY
            )
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (filled) {
                style = Paint.Style.FILL
                color = Color.rgb(255, 107, 134)
            } else {
                style = Paint.Style.STROKE
                strokeWidth = (size * 0.20f).coerceAtLeast(1.5f)
                strokeJoin = Paint.Join.ROUND
                color = Color.argb(220, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawShareIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        textColor: Int
    ) {
        val iconColor = Color.argb(220, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.19f).coerceAtLeast(1.4f)
            strokeCap = Paint.Cap.ROUND
            color = iconColor
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = iconColor
        }

        val lx = cx - size * 0.55f
        val rx = cx + size * 0.52f
        val ty = cy - size * 0.58f
        val by = cy + size * 0.58f
        val dotR = (size * 0.24f).coerceAtLeast(2f)

        canvas.drawLine(lx, cy, rx, ty, linePaint)
        canvas.drawLine(lx, cy, rx, by, linePaint)
        canvas.drawCircle(lx, cy, dotR, dotPaint)
        canvas.drawCircle(rx, ty, dotR, dotPaint)
        canvas.drawCircle(rx, by, dotR, dotPaint)
    }

    private fun drawTasbeehIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        accentColor: Int
    ) {
        val color = Color.argb(235, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.14f).coerceAtLeast(1.5f)
            strokeCap = Paint.Cap.ROUND
            this.color = color
        }
        val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        // Symmetrical, authentic Islamic prayer beads (مسبحة) loop:
        val loopRadiusX = size * 0.44f
        val loopRadiusY = size * 0.39f
        val loopCenterY = cy - size * 0.12f

        // Connecting thread loop
        val loopRect = RectF(
            cx - loopRadiusX,
            loopCenterY - loopRadiusY,
            cx + loopRadiusX,
            loopCenterY + loopRadiusY
        )
        canvas.drawOval(loopRect, strokePaint)

        // 8 distinct beads around the perimeter
        val beadCount = 8
        val beadR = size * 0.11f
        for (i in 0 until beadCount) {
            val angleDeg = (i.toFloat() / beadCount) * 360f - 90f
            val rad = Math.toRadians(angleDeg.toDouble())
            val bx = cx + (loopRadiusX * Math.cos(rad)).toFloat()
            val by = loopCenterY + (loopRadiusY * Math.sin(rad)).toFloat()
            canvas.drawCircle(bx, by, beadR, beadPaint)
        }

        // Minaret / top separator bead (مئذنة السبحة)
        val minaretY = loopCenterY + loopRadiusY + (size * 0.08f)
        val minaretR = size * 0.13f
        canvas.drawCircle(cx, minaretY, minaretR, beadPaint)

        // Elegant tassel cords (شرابة السبحة)
        val tasselTopY = minaretY + minaretR * 0.6f
        val tasselBottomY = tasselTopY + size * 0.38f
        val tasselSpread = size * 0.18f

        canvas.drawLine(cx, tasselTopY, cx, tasselBottomY, strokePaint)
        canvas.drawLine(cx, tasselTopY, cx - tasselSpread, tasselBottomY - size * 0.04f, strokePaint)
        canvas.drawLine(cx, tasselTopY, cx + tasselSpread, tasselBottomY - size * 0.04f, strokePaint)

        val tasselDotR = size * 0.06f
        canvas.drawCircle(cx, tasselBottomY, tasselDotR, beadPaint)
        canvas.drawCircle(cx - tasselSpread, tasselBottomY - size * 0.04f, tasselDotR, beadPaint)
        canvas.drawCircle(cx + tasselSpread, tasselBottomY - size * 0.04f, tasselDotR, beadPaint)
    }

    private fun drawWerdLogoIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
        context: Context? = null
    ) {
        if (context != null) {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, com.zekr.widget.R.drawable.ic_werd_logo)?.mutate()
            if (drawable != null) {
                androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, color)
                val half = size / 2f
                drawable.setBounds(
                    (cx - half).roundToInt(),
                    (cy - half).roundToInt(),
                    (cx + half).roundToInt(),
                    (cy + half).roundToInt()
                )
                drawable.draw(canvas)
                return
            }
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.16f).coerceAtLeast(1.5f)
        }
        val r = size * 0.40f
        canvas.drawCircle(cx, cy, r, strokePaint)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, size * 0.13f, fillPaint)
    }

    private fun drawRepetitionPill(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        pillW: Float,
        pillH: Float,
        currentRepetition: Int,
        targetRepetition: Int,
        textColor: Int,
        accentColor: Int,
        typeface: Typeface
    ) {
        val label = if (currentRepetition > 0) {
            "مسبحة ${ZekrRepository.toArabicDigits(currentRepetition)}/${ZekrRepository.toArabicDigits(targetRepetition)}"
        } else {
            "مسبحة (${ZekrRepository.toArabicDigits(targetRepetition)})"
        }
        val pillRect = RectF(
            centerX - pillW / 2f,
            centerY - pillH / 2f,
            centerX + pillW / 2f,
            centerY + pillH / 2f
        )
        val radius = pillH / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(65, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        }
        canvas.drawRoundRect(pillRect, radius, radius, bgPaint)

        // Subtle progress fill inside the capsule
        val progress = (currentRepetition.toFloat() / targetRepetition.coerceAtLeast(1)).coerceIn(0f, 1f)
        if (progress > 0f) {
            canvas.save()
            val pillClip = Path().apply { addRoundRect(pillRect, radius, radius, Path.Direction.CW) }
            canvas.clipPath(pillClip)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.argb(120, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            }
            val fillWidth = pillW * progress
            // Fill right-to-left for Arabic
            canvas.drawRect(
                pillRect.right - fillWidth,
                pillRect.top,
                pillRect.right,
                pillRect.bottom,
                fillPaint
            )
            canvas.restore()
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            color = Color.argb(160, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        }
        canvas.drawRoundRect(pillRect, radius, radius, borderPaint)

        // Draw miniature 3-bead tasbeeh icon on the right side of the pill
        val beadColor = Color.argb(240, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = beadColor
        }
        val iconRightX = pillRect.right - 14f
        canvas.drawCircle(iconRightX, centerY, 3f, beadPaint)
        canvas.drawCircle(iconRightX - 5.5f, centerY - 2.5f, 2.4f, beadPaint)
        canvas.drawCircle(iconRightX - 11f, centerY, 2.2f, beadPaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(250, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            textSize = (pillH * 0.52f).coerceIn(11.5f, 18f)
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        val baseline = centerY + (textPaint.textSize * 0.34f)
        canvas.drawText(label, centerX - 5f, baseline, textPaint)
    }

    private fun drawPinBadge(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float,
        textColor: Int,
        accentColor: Int
    ) {
        val r = 11f * scale
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(48, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
        }
        canvas.drawCircle(cx, cy, r, bg)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = accentColor
        }
        canvas.drawCircle(cx, cy, r * 0.42f, dot)
    }

    /**
     * Builds a StaticLayout with full Arabic RTL, balanced paragraph wrapping,
     * and proper padding for Tashkeel marks without clipping.
     */
    fun createArabicStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        widthPx: Int,
        scaleFactor: Float,
        maxLines: Int = Int.MAX_VALUE,
        ellipsize: TextUtils.TruncateAt? = null,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
    ): StaticLayout {
        val safeWidth = widthPx.coerceAtLeast(32)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
                .setAlignment(alignment)
                .setTextDirection(TextDirectionHeuristics.ANYRTL_LTR)
                .setLineSpacing(2f * scaleFactor, 1.15f)
                .setBreakStrategy(if (ellipsize != null) Layout.BREAK_STRATEGY_SIMPLE else Layout.BREAK_STRATEGY_BALANCED)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setIncludePad(true)

            if (maxLines < Int.MAX_VALUE) {
                builder.setMaxLines(maxLines)
            }
            if (ellipsize != null) {
                builder.setEllipsize(ellipsize)
                builder.setEllipsizedWidth(safeWidth)
            }
            builder.build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text, paint, safeWidth,
                alignment,
                1.15f, 2f * scaleFactor, true
            )
        }
    }
}

package com.zekr.widget.widget.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetFont
import com.zekr.widget.ui.theme.FontHelper
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PatternDrawables {

    fun drawPattern(
        canvas: Canvas,
        pattern: PatternType,
        width: Float,
        height: Float,
        patternColor: Int = Color.WHITE,
        alpha: Int = 40,
        scale: Float = 1.0f,
        context: Context? = null
    ) {
        if (pattern == PatternType.NONE) return

        val safeScale = scale.coerceIn(0.4f, 2.5f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = patternColor
            this.alpha = alpha.coerceIn(5, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * safeScale.coerceAtLeast(0.7f)
        }

        when (pattern) {
            // 9 High Quality Islamic Geometric Patterns from reference
            PatternType.HEXAGRAM_STARS -> drawHexagramStars(canvas, paint, width, height, safeScale)
            PatternType.OCTAGON_SQUARES -> drawOctagonSquares(canvas, paint, width, height, safeScale)
            PatternType.ANDALUSIAN_EIGHT_STAR -> drawAndalusianEightStar(canvas, paint, width, height, safeScale)
            PatternType.HEX_ROSETTE_KITES -> drawHexRosetteKites(canvas, paint, width, height, safeScale)
            PatternType.TWELVE_GIRIH_STAR -> drawTwelveGirihStar(canvas, paint, width, height, safeScale)
            PatternType.ALHAMBRA_KHATAM_STAR -> drawAlhambraKhatamStar(canvas, paint, width, height, safeScale)
            PatternType.MOROCCAN_QUATREFOIL -> drawMoroccanQuatrefoil(canvas, paint, width, height, safeScale)
            PatternType.FLOWER_OF_LIFE -> drawFlowerOfLife(canvas, paint, width, height, safeScale)
            PatternType.CONCENTRIC_SQUARES_STAR -> drawConcentricSquaresStar(canvas, paint, width, height, safeScale)

            // Arabic Calligraphy variants
            PatternType.ARABIC_CALLIGRAPHY,
            PatternType.CALLIGRAPHY_AMBER,
            PatternType.CALLIGRAPHY_BLUE -> drawArabicCalligraphy(canvas, paint, width, height, safeScale, context)

            PatternType.RADIAL_RIPPLES -> drawRadialRipples(canvas, paint, width, height, safeScale)
            PatternType.RADIAL_DOTS -> drawRadialDots(canvas, paint, width, height, safeScale)
            PatternType.SCALE_ARCHES -> drawScaleArches(canvas, paint, width, height, safeScale)
            PatternType.HEX_HONEYCOMB -> drawHexHoneycomb(canvas, paint, width, height, safeScale)
            PatternType.CROSS_STARS -> drawCrossStars(canvas, paint, width, height, safeScale)
            PatternType.INTERLOCKING_RINGS -> drawInterlockingRings(canvas, paint, width, height, safeScale)
            PatternType.ZIGZAG_CHEVRON -> drawZigzagChevron(canvas, paint, width, height, safeScale)
            PatternType.DIAMOND_LATTICE -> drawDiamondLattice(canvas, paint, width, height, safeScale)
            PatternType.MASHRABIYA_STRIPES -> drawMashrabiyaStripes(canvas, paint, width, height, safeScale)
            PatternType.STAR_OCTAGRAM -> drawStarOctagramGrid(canvas, paint, width, height, safeScale)
            PatternType.ANDALUSIAN_ROSETTE -> drawAndalusianRosette(canvas, paint, width, height, safeScale)
            PatternType.STAR_TWELVE -> drawStarTwelveGrid(canvas, paint, width, height, safeScale)
            PatternType.ISLAMIC_ARCH -> drawIslamicArches(canvas, paint, width, height, safeScale)
            PatternType.MOROCCAN_ZELLIGE -> drawMoroccanZellige(canvas, paint, width, height, safeScale)
            PatternType.CRESCENT_GEOMETRY -> drawCrescentPattern(canvas, paint, width, height, safeScale)
            PatternType.MINIMAL_DOTS -> drawMinimalDots(canvas, paint, width, height, safeScale)
            PatternType.NONE -> Unit
        }
    }

    // 1. Hexagram Star Tiling (خاتم سداسي أندلسي)
    private fun drawHexagramStars(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val rOuter = 28f * scale
        val rInner = (rOuter / sqrt(3.0)).toFloat()
        val dx = rOuter * 3f
        val dy = (rOuter * sqrt(3.0)).toFloat()

        var row = -1
        var y = 0f
        while (y < height + dy * 1.5f) {
            val offsetX = if (row % 2 != 0) dx * 0.5f else 0f
            var x = offsetX - dx
            while (x < width + dx * 1.5f) {
                val path = Path()
                for (i in 0 until 12) {
                    val angle = i * Math.PI / 6.0 - Math.PI / 2.0
                    val r = if (i % 2 == 0) rOuter else rInner
                    val px = (x + r * cos(angle)).toFloat()
                    val py = (y + r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)

                for (i in 0 until 12 step 2) {
                    val angle = i * Math.PI / 6.0 - Math.PI / 2.0
                    val x1 = (x + rOuter * cos(angle)).toFloat()
                    val y1 = (y + rOuter * sin(angle)).toFloat()
                    val x2 = (x + (rOuter + rInner) * cos(angle)).toFloat()
                    val y2 = (y + (rOuter + rInner) * sin(angle)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
                x += dx
            }
            y += dy
            row++
        }
    }

    // 2. Octagon & Square Interlaced Mesh (شبكة مثمنات ومربعات)
    private fun drawOctagonSquares(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 48f * scale
        val s = (step / (1.0 + sqrt(2.0))).toFloat()
        val corner = (s / sqrt(2.0)).toFloat()

        var y = -step
        while (y < height + step * 2f) {
            var x = -step
            while (x < width + step * 2f) {
                val path = Path().apply {
                    moveTo(x + corner, y)
                    lineTo(x + step - corner, y)
                    lineTo(x + step, y + corner)
                    lineTo(x + step, y + step - corner)
                    lineTo(x + step - corner, y + step)
                    lineTo(x + corner, y + step)
                    lineTo(x, y + step - corner)
                    lineTo(x, y + corner)
                    close()
                }
                canvas.drawPath(path, paint)
                x += step
            }
            y += step
        }
    }

    // 3. Andalusian 8-Star with Interlocking Kites (نجمة ثمانية موريسكية)
    private fun drawAndalusianEightStar(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 60f * scale
        val rOuter = 24f * scale
        val rInner = 13f * scale
        val dy = step * 0.707f

        var row = -1
        var y = 0f
        while (y < height + step * 1.5f) {
            val offsetX = if (row % 2 != 0) step * 0.5f else 0f
            var x = offsetX - step
            while (x < width + step * 1.5f) {
                val path = Path()
                for (i in 0 until 16) {
                    val angle = i * Math.PI / 8.0 - Math.PI / 2.0
                    val r = if (i % 2 == 0) rOuter else rInner
                    val px = (x + r * cos(angle)).toFloat()
                    val py = (y + r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)

                for (i in 0 until 16 step 2) {
                    val angle = i * Math.PI / 8.0 - Math.PI / 2.0
                    val x1 = (x + rOuter * cos(angle)).toFloat()
                    val y1 = (y + rOuter * sin(angle)).toFloat()
                    val x2 = (x + (rOuter + 10f * scale) * cos(angle)).toFloat()
                    val y2 = (y + (rOuter + 10f * scale) * sin(angle)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
                x += step
            }
            y += dy
            row++
        }
    }

    // 4. Hexagonal Rosette with Kites (زهرة سداسية مورقة)
    private fun drawHexRosetteKites(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val stepR = 34f * scale
        val dx = stepR * 3f
        val dy = (stepR * sqrt(3.0)).toFloat()
        val rOut = 20f * scale
        val rIn = 10f * scale

        var row = -1
        var y = 0f
        while (y < height + dy * 1.5f) {
            val offsetX = if (row % 2 != 0) dx * 0.5f else 0f
            var x = offsetX - dx
            while (x < width + dx * 1.5f) {
                val path = Path()
                for (i in 0 until 12) {
                    val angle = i * Math.PI / 6.0
                    val r = if (i % 2 == 0) rOut else rIn
                    val px = (x + r * cos(angle)).toFloat()
                    val py = (y + r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)

                for (i in 0 until 12 step 2) {
                    val angle = i * Math.PI / 6.0
                    val x1 = (x + rOut * cos(angle)).toFloat()
                    val y1 = (y + rOut * sin(angle)).toFloat()
                    val x2 = (x + (rOut + 12f * scale) * cos(angle)).toFloat()
                    val y2 = (y + (rOut + 12f * scale) * sin(angle)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
                x += dx
            }
            y += dy
            row++
        }
    }

    // 5. 12-Point Girih Star (نجمة اثنا عشرية جيريه)
    private fun drawTwelveGirihStar(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 80f * scale
        val rOuter = 30f * scale
        val rInner = 17f * scale

        var y = 0f
        while (y < height + step * 1.5f) {
            var x = 0f
            while (x < width + step * 1.5f) {
                val path = Path()
                for (i in 0 until 24) {
                    val angle = i * Math.PI / 12.0 - Math.PI / 2.0
                    val r = if (i % 2 == 0) rOuter else rInner
                    val px = (x + r * cos(angle)).toFloat()
                    val py = (y + r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)

                for (i in 0 until 24 step 2) {
                    val angle = i * Math.PI / 12.0 - Math.PI / 2.0
                    val x1 = (x + rOuter * cos(angle)).toFloat()
                    val y1 = (y + rOuter * sin(angle)).toFloat()
                    val x2 = (x + (rOuter + 12f * scale) * cos(angle)).toFloat()
                    val y2 = (y + (rOuter + 12f * scale) * sin(angle)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
                x += step
            }
            y += step
        }
    }

    // 6. Classic Alhambra 8-Point Star (نجمة قصر الحمراء الثمانية - SELECTED)
    private fun drawAlhambraKhatamStar(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 56f * scale
        val rOuter = 24f * scale
        val rInner = 14f * scale
        val half = step * 0.5f

        var y = 0f
        while (y < height + step * 1.5f) {
            var x = 0f
            while (x < width + step * 1.5f) {
                val path = Path()
                for (i in 0 until 16) {
                    val angle = i * Math.PI / 8.0 - Math.PI / 8.0
                    val r = if (i % 2 == 0) rOuter else rInner
                    val px = (x + r * cos(angle)).toFloat()
                    val py = (y + r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)

                // Diamond connector
                val dSize = 7.5f * scale
                val diamond = Path().apply {
                    moveTo(x + half, y + half - dSize)
                    lineTo(x + half + dSize, y + half)
                    lineTo(x + half, y + half + dSize)
                    lineTo(x + half - dSize, y + half)
                    close()
                }
                canvas.drawPath(diamond, paint)

                // Diagonal connectors
                val x1 = (x + rOuter * cos(Math.PI / 4.0)).toFloat()
                val y1 = (y + rOuter * sin(Math.PI / 4.0)).toFloat()
                canvas.drawLine(x1, y1, x + half - dSize, y + half, paint)
                canvas.drawLine(x1, y1, x + half, y + half - dSize, paint)

                x += step
            }
            y += step
        }
    }

    // 7. Moroccan Quatrefoil Trellis (مشبك فاسي مغربي)
    private fun drawMoroccanQuatrefoil(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 44f * scale
        val r = step * 0.28f
        val dy = step * 0.5f

        var row = -1
        var y = 0f
        while (y < height + step * 1.5f) {
            val offsetX = if (row % 2 != 0) step * 0.5f else 0f
            var x = offsetX - step
            while (x < width + step * 1.5f) {
                val path = Path().apply {
                    val rHalf = r * 0.85f
                    // 4 circular clover leaves
                    addCircle(x - rHalf, y, rHalf, Path.Direction.CW)
                    addCircle(x + rHalf, y, rHalf, Path.Direction.CW)
                    addCircle(x, y - rHalf, rHalf, Path.Direction.CW)
                    addCircle(x, y + rHalf, rHalf, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
                x += step
            }
            y += dy
            row++
        }
    }

    // 8. Flower of Life (زهرة الحياة الكلاسيكية)
    private fun drawFlowerOfLife(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val r = 28f * scale
        val dx = (r * sqrt(3.0)).toFloat()
        val dy = r * 1.5f

        var row = -2
        var y = -r
        while (y < height + r * 2f) {
            val offsetX = if (row % 2 != 0) dx * 0.5f else 0f
            var x = offsetX - dx
            while (x < width + dx * 2f) {
                canvas.drawCircle(x, y, r, paint)
                x += dx
            }
            y += dy
            row++
        }
    }

    // 9. Rub el Hizb with Squares & Circles (مربعات ودوائر الختم)
    private fun drawConcentricSquaresStar(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 50f * scale
        val halfSq = 13f * scale
        val rCircle = 4.5f * scale
        val diag = (halfSq * sqrt(2.0)).toFloat()

        var y = 0f
        while (y < height + step * 1.5f) {
            var x = 0f
            while (x < width + step * 1.5f) {
                // Outer square frame
                val boxSize = step - 4f * scale
                val boxRect = RectF(x - boxSize / 2f, y - boxSize / 2f, x + boxSize / 2f, y + boxSize / 2f)
                canvas.drawRoundRect(boxRect, 4f * scale, 4f * scale, paint)

                // Axis-aligned square
                val innerRect = RectF(x - halfSq, y - halfSq, x + halfSq, y + halfSq)
                canvas.drawRect(innerRect, paint)

                // 45-degree rotated square
                val rotSquare = Path().apply {
                    moveTo(x, y - diag)
                    lineTo(x + diag, y)
                    lineTo(x, y + diag)
                    lineTo(x - diag, y)
                    close()
                }
                canvas.drawPath(rotSquare, paint)

                // Concentric circles
                canvas.drawCircle(x, y, rCircle, paint)
                canvas.drawCircle(x, y, rCircle * 0.4f, paint)

                x += step
            }
            y += step
        }
    }

    // 1. Arabic Calligraphy Scattered Letters Pattern (حروفيات)
    private fun drawArabicCalligraphy(
        canvas: Canvas,
        paint: Paint,
        width: Float,
        height: Float,
        scale: Float,
        context: Context?
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            alpha = paint.alpha
            style = Paint.Style.FILL
            textSize = 34f * scale
            typeface = if (context != null) {
                FontHelper.getTypeface(context, WidgetFont.AMIRI)
            } else {
                Typeface.SERIF
            }
        }

        val letters = listOf("ع", "ح", "و", "ي", "ل", "ن", "ر", "س", "ف", "ط", "ك", "ص", "ق", "م", "هـ", "لا", "الله")
        val stepX = 52f * scale
        val stepY = 48f * scale

        var index = 0
        var y = stepY * 0.7f
        var row = 0
        while (y < height + stepY) {
            val offsetX = if (row % 2 == 1) stepX * 0.5f else 0f
            var x = offsetX + stepX * 0.4f
            while (x < width + stepX) {
                val letter = letters[index % letters.size]
                val rotation = ((index * 37) % 40 - 20).toFloat()
                canvas.save()
                canvas.translate(x, y)
                canvas.rotate(rotation)
                canvas.drawText(letter, -10f, 10f, textPaint)
                canvas.restore()
                x += stepX
                index++
            }
            y += stepY
            row++
        }
    }

    // 2. Concentric Radial Wave Ripples (تموجات دائرية)
    private fun drawRadialRipples(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val cx = width * 0.25f
        val cy = height * 0.25f
        val step = 10f * scale
        val maxRadius = sqrt((width * width + height * height).toDouble()).toFloat()

        var r = 12f * scale
        while (r < maxRadius) {
            canvas.drawCircle(cx, cy, r, paint)
            r += step
        }
    }

    // 3. Halftone Stippled Dots Gradient (نقاط متدرجة)
    private fun drawRadialDots(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val dotPaint = Paint(paint).apply { style = Paint.Style.FILL }
        val step = 18f * scale
        var y = step / 2
        while (y < height) {
            var x = step / 2
            while (x < width) {
                val dist = sqrt(((x - width / 2) * (x - width / 2) + (y - height / 2) * (y - height / 2)).toDouble()).toFloat()
                val normalized = (dist / (width * 0.6f)).coerceIn(0f, 1f)
                val dotR = (1.2f + (1f - normalized) * 2.8f) * scale
                canvas.drawCircle(x, y, dotR, dotPaint)
                x += step
            }
            y += step
        }
    }

    // 4. Overlapping Scale / Scallop Arches (قشور سمك وأقواس)
    private fun drawScaleArches(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val radius = 24f * scale
        val stepX = radius * 2
        val stepY = radius * 1.1f

        var y = 0f
        var row = 0
        while (y < height + radius * 2) {
            val offsetX = if (row % 2 == 1) radius else 0f
            var x = offsetX - radius
            while (x < width + radius * 2) {
                val path = Path()
                path.addArc(x, y, x + radius * 2, y + radius * 2, 180f, 180f)
                canvas.drawPath(path, paint)
                x += stepX
            }
            y += stepY
            row++
        }
    }

    // 5. Honeycomb Hexagonal Mesh (خلايا النحل)
    private fun drawHexHoneycomb(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val radius = 18f * scale
        val h = (radius * sqrt(3.0)).toFloat()
        var y = 0f
        var row = 0
        while (y < height + radius * 2) {
            val offsetX = if (row % 2 == 1) radius * 1.5f else 0f
            var x = offsetX
            while (x < width + radius * 2) {
                val path = Path()
                for (i in 0..5) {
                    val angle = Math.PI / 3 * i
                    val px = x + (radius * cos(angle)).toFloat()
                    val py = y + (radius * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
                x += radius * 3f
            }
            y += h / 2f
            row++
        }
    }

    // 6. Starburst Cross Stars (نجوم متقاطعة دقيقة)
    private fun drawCrossStars(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 36f * scale
        var y = step / 2
        while (y < height + step) {
            var x = step / 2
            while (x < width + step) {
                val len = 10f * scale
                // 4-point cross
                canvas.drawLine(x - len, y, x + len, y, paint)
                canvas.drawLine(x, y - len, x, y + len, paint)
                // Diagonal tiny cross
                val diag = len * 0.55f
                canvas.drawLine(x - diag, y - diag, x + diag, y + diag, paint)
                canvas.drawLine(x - diag, y + diag, x + diag, y - diag, paint)
                x += step
            }
            y += step
        }
    }

    // 7. Interlocking Circular Rings (حلقات ودوائر متداخلة)
    private fun drawInterlockingRings(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val radius = 22f * scale
        val step = radius * 1.4f
        var y = radius
        while (y < height + radius) {
            var x = radius
            while (x < width + radius) {
                canvas.drawCircle(x, y, radius, paint)
                x += step
            }
            y += step
        }
    }

    // 8. Zigzag Chevron Waves (تعرجات هندسية)
    private fun drawZigzagChevron(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val stepX = 26f * scale
        val stepY = 16f * scale
        var y = 0f
        while (y < height + stepY) {
            val path = Path()
            path.moveTo(0f, y)
            var x = 0f
            var up = true
            while (x < width + stepX) {
                x += stepX
                val targetY = if (up) y - stepY * 0.45f else y + stepY * 0.45f
                path.lineTo(x, targetY)
                up = !up
            }
            canvas.drawPath(path, paint)
            y += stepY
        }
    }

    // 9. Diagonal Diamond Lattice (شبكة معينات مائلة)
    private fun drawDiamondLattice(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 28f * scale
        var offset = -height
        while (offset < width + height) {
            // Forward diagonal
            canvas.drawLine(offset, 0f, offset + height, height, paint)
            // Backward diagonal
            canvas.drawLine(offset + height, 0f, offset, height, paint)
            offset += step
        }
    }

    // 10. Vertical Mashrabiya Lattice Stripes (مشربية رأسية)
    private fun drawMashrabiyaStripes(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val stepX = 30f * scale
        val dotPaint = Paint(paint).apply { style = Paint.Style.FILL }
        var x = stepX / 2
        while (x < width) {
            canvas.drawLine(x, 0f, x, height, paint)
            var y = 20f * scale
            while (y < height) {
                canvas.drawCircle(x, y, 2.8f * scale, dotPaint)
                canvas.drawLine(x - 6f * scale, y, x + 6f * scale, y, paint)
                y += 35f * scale
            }
            x += stepX
        }
    }

    // 11. Andalusian Star Octagram
    private fun drawStarOctagramGrid(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 70f * scale
        val radius = 22f * scale
        var y = step / 2
        while (y < height + step) {
            var x = step / 2
            while (x < width + step) {
                drawStar(canvas, paint, x, y, 8, radius, radius * 0.52f)
                canvas.drawCircle(x, y, radius * 0.35f, paint)
                x += step
            }
            y += step
        }
    }

    // 12. Andalusian Rosette
    private fun drawAndalusianRosette(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 72f * scale
        val radius = 20f * scale
        var y = step / 2
        while (y < height + step) {
            var x = step / 2
            while (x < width + step) {
                for (i in 0..7) {
                    val angle = Math.PI / 4 * i
                    val cx = x + (radius * 0.5f * cos(angle)).toFloat()
                    val cy = y + (radius * 0.5f * sin(angle)).toFloat()
                    canvas.drawCircle(cx, cy, radius * 0.45f, paint)
                }
                canvas.drawCircle(x, y, radius * 0.25f, paint)
                x += step
            }
            y += step
        }
    }

    // 13. Twelve Point Girih Star
    private fun drawStarTwelveGrid(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 90f * scale
        val radius = 30f * scale
        var y = step / 2
        while (y < height + step) {
            var x = step / 2
            while (x < width + step) {
                drawStar(canvas, paint, x, y, 12, radius, radius * 0.65f)
                canvas.drawCircle(x, y, radius * 0.4f, paint)
                x += step
            }
            y += step
        }
    }

    // 14. Islamic Arches (Mihrab)
    private fun drawIslamicArches(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val stepX = 48f * scale
        val archH = 60f * scale
        var y = 0f
        while (y < height + archH) {
            var x = 0f
            while (x < width + stepX) {
                val path = Path()
                path.moveTo(x, y + archH)
                path.lineTo(x, y + archH * 0.45f)
                path.cubicTo(x, y + archH * 0.1f, x + stepX * 0.5f, y, x + stepX * 0.5f, y)
                path.cubicTo(x + stepX * 0.5f, y, x + stepX, y + archH * 0.1f, x + stepX, y + archH * 0.45f)
                path.lineTo(x + stepX, y + archH)
                canvas.drawPath(path, paint)
                x += stepX
            }
            y += archH * 0.85f
        }
    }

    // 15. Moroccan Zellige Tile
    private fun drawMoroccanZellige(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 56f * scale
        var y = 0f
        while (y < height + step) {
            var x = 0f
            while (x < width + step) {
                val half = step / 2f
                val path1 = Path().apply {
                    addRect(x + half * 0.2f, y + half * 0.2f, x + step - half * 0.2f, y + step - half * 0.2f, Path.Direction.CW)
                }
                val path2 = Path().apply {
                    moveTo(x + half, y)
                    lineTo(x + step, y + half)
                    lineTo(x + half, y + step)
                    lineTo(x, y + half)
                    close()
                }
                canvas.drawPath(path1, paint)
                canvas.drawPath(path2, paint)
                x += step
            }
            y += step
        }
    }

    // 16. Crescent & Constellation Star
    private fun drawCrescentPattern(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 72f * scale
        val radius = 18f * scale
        var y = step / 2
        while (y < height + step) {
            var x = step / 2
            while (x < width + step) {
                val path = Path()
                path.addCircle(x, y, radius, Path.Direction.CW)
                val inner = Path()
                inner.addCircle(x + radius * 0.38f, y - radius * 0.15f, radius * 0.85f, Path.Direction.CW)
                path.op(inner, Path.Op.DIFFERENCE)
                canvas.drawPath(path, paint)
                drawStar(canvas, paint, x + radius * 0.65f, y - radius * 0.35f, 5, radius * 0.38f, radius * 0.18f)
                x += step
            }
            y += step
        }
    }

    // 17. Minimalist Micro-dots
    private fun drawMinimalDots(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val dotPaint = Paint(paint).apply { style = Paint.Style.FILL }
        val step = 28f * scale
        var y = step / 2
        while (y < height) {
            var x = step / 2
            while (x < width) {
                canvas.drawCircle(x, y, 1.8f * scale.coerceAtLeast(0.8f), dotPaint)
                x += step
            }
            y += step
        }
    }

    private fun drawStar(canvas: Canvas, paint: Paint, cx: Float, cy: Float, points: Int, outerR: Float, innerR: Float) {
        val path = Path()
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = i * Math.PI / points - Math.PI / 2
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}

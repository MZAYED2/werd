package com.zekr.widget.widget.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.zekr.widget.data.preferences.PatternType
import kotlin.math.cos
import kotlin.math.sin

object PatternDrawables {

    fun drawPattern(
        canvas: Canvas,
        pattern: PatternType,
        width: Float,
        height: Float,
        patternColor: Int = Color.WHITE,
        alpha: Int = 40,
        scale: Float = 1.0f
    ) {
        if (pattern == PatternType.NONE) return

        val safeScale = scale.coerceIn(0.4f, 2.5f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = patternColor
            this.alpha = alpha.coerceIn(5, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * safeScale.coerceAtLeast(0.8f)
        }

        when (pattern) {
            PatternType.STAR_OCTAGRAM -> drawStarOctagramGrid(canvas, paint, width, height, safeScale)
            PatternType.STAR_TWELVE -> drawStarTwelveGrid(canvas, paint, width, height, safeScale)
            PatternType.ARABESQUE -> drawArabesquePattern(canvas, paint, width, height, safeScale)
            PatternType.ISLAMIC_ARCH -> drawIslamicArches(canvas, paint, width, height, safeScale)
            PatternType.MOROCCAN_ZELLIGE -> drawMoroccanZellige(canvas, paint, width, height, safeScale)
            PatternType.DIAMOND_GIRIH -> drawDiamondGirih(canvas, paint, width, height, safeScale)
            PatternType.FLORAL_DAMASK -> drawFloralDamaskPattern(canvas, paint, width, height, safeScale)
            PatternType.GEOMETRIC_HEX -> drawHexGrid(canvas, paint, width, height, safeScale)
            PatternType.CRESCENT_GEOMETRY -> drawCrescentPattern(canvas, paint, width, height, safeScale)
            PatternType.MINIMAL_DOTS -> drawMinimalDots(canvas, paint, width, height, safeScale)
            PatternType.NONE -> Unit
        }
    }

    private fun drawStarOctagramGrid(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 72f * scale
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

    private fun drawStarTwelveGrid(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 90f * scale
        val radius = 32f * scale
        var y = step / 2
        while (y < height + step) {
            var x = step / 2
            while (x < width + step) {
                drawStar(canvas, paint, x, y, 12, radius, radius * 0.65f)
                canvas.drawCircle(x, y, radius * 0.4f, paint)
                canvas.drawCircle(x, y, radius * 0.85f, paint)
                x += step
            }
            y += step
        }
    }

    private fun drawIslamicArches(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val stepX = 50f * scale
        val archHeight = 65f * scale
        var y = 0f
        while (y < height + archHeight) {
            var x = 0f
            while (x < width + stepX) {
                val path = Path()
                path.moveTo(x, y + archHeight)
                path.lineTo(x, y + archHeight * 0.45f)
                // Pointed Islamic arch top
                path.cubicTo(
                    x, y + archHeight * 0.1f,
                    x + stepX * 0.5f, y,
                    x + stepX * 0.5f, y
                )
                path.cubicTo(
                    x + stepX * 0.5f, y,
                    x + stepX, y + archHeight * 0.1f,
                    x + stepX, y + archHeight * 0.45f
                )
                path.lineTo(x + stepX, y + archHeight)
                canvas.drawPath(path, paint)
                x += stepX
            }
            y += archHeight * 0.85f
        }
    }

    private fun drawMoroccanZellige(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 60f * scale
        var y = 0f
        while (y < height + step) {
            var x = 0f
            while (x < width + step) {
                // Interlocking square and 45-degree diamond
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

    private fun drawDiamondGirih(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val stepX = 64f * scale
        val stepY = 40f * scale
        var y = 0f
        while (y < height + stepY * 2) {
            var x = 0f
            while (x < width + stepX * 2) {
                val path = Path().apply {
                    moveTo(x, y)
                    lineTo(x + stepX / 2f, y - stepY / 2f)
                    lineTo(x + stepX, y)
                    lineTo(x + stepX / 2f, y + stepY / 2f)
                    close()
                }
                canvas.drawPath(path, paint)
                x += stepX
            }
            y += stepY
        }
    }

    private fun drawCrescentPattern(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 75f * scale
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

                // Tiny 5-point star next to crescent
                drawStar(canvas, paint, x + radius * 0.65f, y - radius * 0.35f, 5, radius * 0.38f, radius * 0.18f)
                x += step
            }
            y += step
        }
    }

    private fun drawArabesquePattern(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 64f * scale
        var y = 0f
        while (y < height + step) {
            var x = 0f
            while (x < width + step) {
                val path = Path()
                path.moveTo(x, y + step / 2)
                path.cubicTo(x + step * 0.25f, y, x + step * 0.75f, y, x + step, y + step / 2)
                path.cubicTo(x + step * 0.75f, y + step, x + step * 0.25f, y + step, x, y + step / 2)
                canvas.drawPath(path, paint)
                x += step
            }
            y += step
        }
    }

    private fun drawFloralDamaskPattern(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val step = 80f * scale
        var y = 20f * scale
        while (y < height + step) {
            var x = 20f * scale
            while (x < width + step) {
                val path = Path()
                path.moveTo(x, y)
                path.quadTo(x + 18f * scale, y - 18f * scale, x, y - 30f * scale)
                path.quadTo(x - 18f * scale, y - 18f * scale, x, y)
                path.quadTo(x + 18f * scale, y + 18f * scale, x, y + 30f * scale)
                path.quadTo(x - 18f * scale, y + 18f * scale, x, y)
                canvas.drawPath(path, paint)
                x += step
            }
            y += step
        }
    }

    private fun drawHexGrid(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val radius = 28f * scale
        val h = (radius * kotlin.math.sqrt(3.0)).toFloat()
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

    private fun drawMinimalDots(canvas: Canvas, paint: Paint, width: Float, height: Float, scale: Float) {
        val dotPaint = Paint(paint).apply {
            style = Paint.Style.FILL
        }
        val step = 32f * scale
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

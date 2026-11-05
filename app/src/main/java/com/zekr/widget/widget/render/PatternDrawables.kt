package com.zekr.widget.widget.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
        alpha: Int = 28 // Subtle watermark effect
    ) {
        if (pattern == PatternType.NONE) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = patternColor
            this.alpha = alpha
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }

        when (pattern) {
            PatternType.STAR_OCTAGRAM -> drawStarOctagramGrid(canvas, paint, width, height)
            PatternType.ARABESQUE -> drawArabesquePattern(canvas, paint, width, height)
            PatternType.FLORAL_DAMASK -> drawFloralDamaskPattern(canvas, paint, width, height)
            PatternType.GEOMETRIC_HEX -> drawHexGrid(canvas, paint, width, height)
            PatternType.MINIMAL_DOTS -> drawMinimalDots(canvas, paint, width, height)
            PatternType.NONE -> Unit
        }
    }

    private fun drawStarOctagramGrid(canvas: Canvas, paint: Paint, width: Float, height: Float) {
        val step = 72f
        val radius = 22f
        var y = step / 2
        while (y < height + step) {
            var x = step / 2
            while (x < width + step) {
                drawEightPointStar(canvas, paint, x, y, radius)
                x += step
            }
            y += step
        }
    }

    private fun drawEightPointStar(canvas: Canvas, paint: Paint, cx: Float, cy: Float, radius: Float) {
        val path = Path()
        val numPoints = 8
        val innerRadius = radius * 0.52f

        for (i in 0 until numPoints * 2) {
            val r = if (i % 2 == 0) radius else innerRadius
            val angle = i * Math.PI / numPoints - Math.PI / 2
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)

        // Inner circle
        canvas.drawCircle(cx, cy, innerRadius * 0.6f, paint)
    }

    private fun drawArabesquePattern(canvas: Canvas, paint: Paint, width: Float, height: Float) {
        val step = 64f
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

    private fun drawFloralDamaskPattern(canvas: Canvas, paint: Paint, width: Float, height: Float) {
        val step = 80f
        var y = 20f
        while (y < height + step) {
            var x = 20f
            while (x < width + step) {
                // Petal 1
                val path = Path()
                path.moveTo(x, y)
                path.quadTo(x + 18f, y - 18f, x, y - 30f)
                path.quadTo(x - 18f, y - 18f, x, y)
                // Petal 2
                path.quadTo(x + 18f, y + 18f, x, y + 30f)
                path.quadTo(x - 18f, y + 18f, x, y)
                canvas.drawPath(path, paint)
                x += step
            }
            y += step
        }
    }

    private fun drawHexGrid(canvas: Canvas, paint: Paint, width: Float, height: Float) {
        val radius = 28f
        val h = (radius * kotlin.math.sqrt(3.0)).toFloat()
        var y = 0f
        var row = 0
        while (y < height + radius * 2) {
            val offsetX = if (row % 2 == 1) radius * 1.5f else 0f
            var x = offsetX
            while (x < width + radius * 2) {
                drawHexagon(canvas, paint, x, y, radius)
                x += radius * 3f
            }
            y += h / 2f
            row++
        }
    }

    private fun drawHexagon(canvas: Canvas, paint: Paint, cx: Float, cy: Float, radius: Float) {
        val path = Path()
        for (i in 0..5) {
            val angle = Math.PI / 3 * i
            val px = cx + (radius * cos(angle)).toFloat()
            val py = cy + (radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawMinimalDots(canvas: Canvas, paint: Paint, width: Float, height: Float) {
        val dotPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            alpha = 32
        }
        val step = 32f
        var y = step / 2
        while (y < height) {
            var x = step / 2
            while (x < width) {
                canvas.drawCircle(x, y, 1.8f, dotPaint)
                x += step
            }
            y += step
        }
    }
}

package com.zekr.widget.widget.render

import kotlin.math.min
import kotlin.math.roundToInt

data class WidgetLayout(
    val widthDp: Float,
    val heightDp: Float,
    val density: Float,
    val scaleFactor: Float,
    val showInteractiveBar: Boolean,
    val hasMultiRep: Boolean,

    // Bottom bar dimensions in DP
    val barHeightDp: Float,
    val bottomMarginDp: Float,
    val rightMarginDp: Float,
    val buttonTouchSizeDp: Float,
    val drawnButtonRadiusDp: Float,
    val barCenterYDp: Float,

    // Button Centers in DP (from left=0 to right=widthDp)
    val favCenterXDp: Float,
    val shareCenterXDp: Float,
    val tasbeehCenterXDp: Float,

    // Repetition pill in DP
    val pillCenterXDp: Float,
    val pillCenterYDp: Float,
    val pillWidthDp: Float,
    val pillHeightDp: Float,

    // Zekr Text area in DP
    val textTopPaddingDp: Float,
    val textHorizontalPaddingDp: Float,
    val availableTextHeightDp: Float,
    val maxTextWidthDp: Float,

    // Citation text in DP
    val citationCenterXDp: Float,
    val citationCenterYDp: Float,
    val maxCitationWidthDp: Float
) {
    fun toPx(dp: Float): Float = dp * density * scaleFactor

    val favCxPx: Float get() = toPx(favCenterXDp)
    val shareCxPx: Float get() = toPx(shareCenterXDp)
    val tasbeehCxPx: Float get() = toPx(tasbeehCenterXDp)
    val barCenterYPx: Float get() = toPx(barCenterYDp)
    val drawnButtonRadiusPx: Float get() = toPx(drawnButtonRadiusDp)

    val pillCxPx: Float get() = toPx(pillCenterXDp)
    val pillCyPx: Float get() = toPx(pillCenterYDp)
    val pillWidthPx: Float get() = toPx(pillWidthDp)
    val pillHeightPx: Float get() = toPx(pillHeightDp)

    val textTopPaddingPx: Float get() = toPx(textTopPaddingDp)
    val textHorizontalPaddingPx: Float get() = toPx(textHorizontalPaddingDp)
    val availableTextHeightPx: Float get() = toPx(availableTextHeightDp)
    val maxTextWidthPx: Float get() = toPx(maxTextWidthDp)

    val citationCxPx: Float get() = toPx(citationCenterXDp)
    val citationCyPx: Float get() = toPx(citationCenterYDp)
    val maxCitationWidthPx: Float get() = toPx(maxCitationWidthDp)
}

object WidgetLayoutGeometry {

    fun compute(
        widthDp: Float,
        heightDp: Float,
        density: Float,
        scaleFactor: Float = 1.0f,
        hasMultiRep: Boolean = false
    ): WidgetLayout {
        val safeW = widthDp.coerceAtLeast(120f)
        val safeH = heightDp.coerceAtLeast(70f)
        val showInteractiveBar = safeH >= 85f

        // Touch target sizing: 48dp standard, adapted gracefully on compact or narrow widgets
        val isCompactHeight = safeH < 110f
        val buttonTouchSizeDp = when {
            safeW < 190f -> (safeW * 0.28f).coerceAtLeast(36f)
            isCompactHeight -> 44f
            else -> 48f
        }
        val barHeightDp = if (isCompactHeight) 44f else 48f
        val bottomMarginDp = when {
            safeH > 160f -> 8f
            isCompactHeight -> 2f
            else -> 4f
        }
        val rightMarginDp = if (safeW > 280f) 10f else 6f
        val drawnButtonRadiusDp = (buttonTouchSizeDp * 0.38f).coerceIn(14f, 19f)

        val barCenterYDp = safeH - bottomMarginDp - (barHeightDp / 2f)

        // Three buttons ordered from right to left: Favorite, Share, Tasbeeh
        val favCenterXDp = safeW - rightMarginDp - (buttonTouchSizeDp / 2f)
        val shareCenterXDp = safeW - rightMarginDp - buttonTouchSizeDp - (buttonTouchSizeDp / 2f)
        val tasbeehCenterXDp = safeW - rightMarginDp - (2f * buttonTouchSizeDp) - (buttonTouchSizeDp / 2f)

        // Repetition pill: ONLY when height is generous (>= 150dp) AND widget is wide enough (>= 220dp)
        // In thin or compact heights, repetition is integrated directly with the citation text so text is never overlapped
        val canShowPill = hasMultiRep && safeH >= 150f && safeW >= 220f
        val pillHeightDp = 26f
        val pillWidthDp = 125f.coerceAtMost(safeW - 40f)
        val pillCenterXDp = safeW / 2f
        val pillCenterYDp = barCenterYDp - (barHeightDp / 2f) - (pillHeightDp / 2f) - 4f

        // Citation text occupies the space left of the buttons
        val leftMarginDp = if (safeW > 280f) 16f else 8f
        val buttonsLeftEdgeDp = safeW - rightMarginDp - (3f * buttonTouchSizeDp) - 8f
        val maxCitationWidthDp = (buttonsLeftEdgeDp - leftMarginDp).coerceAtLeast(0f)
        val citationCenterXDp = leftMarginDp + (maxCitationWidthDp / 2f)
        val citationCenterYDp = barCenterYDp

        // Arabic Zekr Text area - guaranteed collision-free
        val reservedBottomDp = when {
            !showInteractiveBar -> 24f
            canShowPill -> barHeightDp + bottomMarginDp + pillHeightDp + 10f
            else -> barHeightDp + bottomMarginDp + (if (isCompactHeight) 2f else 6f)
        }
        val textTopPaddingDp = when {
            safeH > 160f -> 14f
            isCompactHeight -> 4f
            else -> 8f
        }
        val availableTextHeightDp = (safeH - textTopPaddingDp - reservedBottomDp).coerceAtLeast(28f)
        val textHorizontalPaddingDp = (safeW * 0.06f).coerceIn(10f, 24f)
        val maxTextWidthDp = (safeW - (textHorizontalPaddingDp * 2f)).coerceAtLeast(50f)

        return WidgetLayout(
            widthDp = safeW,
            heightDp = safeH,
            density = density.coerceAtLeast(1f),
            scaleFactor = scaleFactor.coerceAtLeast(0.1f),
            showInteractiveBar = showInteractiveBar,
            hasMultiRep = canShowPill,
            barHeightDp = barHeightDp,
            bottomMarginDp = bottomMarginDp,
            rightMarginDp = rightMarginDp,
            buttonTouchSizeDp = buttonTouchSizeDp,
            drawnButtonRadiusDp = drawnButtonRadiusDp,
            barCenterYDp = barCenterYDp,
            favCenterXDp = favCenterXDp,
            shareCenterXDp = shareCenterXDp,
            tasbeehCenterXDp = tasbeehCenterXDp,
            pillCenterXDp = pillCenterXDp,
            pillCenterYDp = pillCenterYDp,
            pillWidthDp = pillWidthDp,
            pillHeightDp = pillHeightDp,
            textTopPaddingDp = textTopPaddingDp,
            textHorizontalPaddingDp = textHorizontalPaddingDp,
            availableTextHeightDp = availableTextHeightDp,
            maxTextWidthDp = maxTextWidthDp,
            citationCenterXDp = citationCenterXDp,
            citationCenterYDp = citationCenterYDp,
            maxCitationWidthDp = maxCitationWidthDp
        )
    }
}

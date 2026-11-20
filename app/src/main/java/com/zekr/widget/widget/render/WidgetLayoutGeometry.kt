package com.zekr.widget.widget.render

enum class WidgetDisplayMode {
    COMPACT,   // heightDp < 75f (1-row thin horizontal bar)
    MEDIUM,    // 75f <= heightDp < 165f (2-row balanced text + citation + bottom buttons)
    EXPANDED   // heightDp >= 165f (3+ rows generous layout with header, large text, citation, buttons)
}

data class WidgetLayout(
    val widthDp: Float,
    val heightDp: Float,
    val density: Float,
    val scaleFactor: Float,
    val mode: WidgetDisplayMode = WidgetDisplayMode.MEDIUM,
    val showInteractiveBar: Boolean,
    val hasMultiRep: Boolean,
    val barHeightDp: Float,
    val bottomMarginDp: Float,
    val rightMarginDp: Float,
    val buttonTouchSizeDp: Float,
    val drawnButtonRadiusDp: Float,
    val barCenterYDp: Float,
    val menuCenterXDp: Float,
    val tasbeehCenterXDp: Float,
    val openWerdCenterXDp: Float = 0f,
    val headerCenterYDp: Float?,
    val textTopPaddingDp: Float,
    val textStartXDp: Float = 0f,
    val availableTextHeightDp: Float,
    val maxTextWidthDp: Float,
    val citationCenterXDp: Float,
    val maxCitationWidthDp: Float
) {
    fun toPx(dp: Float): Float = dp * density * scaleFactor

    val menuCxPx: Float get() = toPx(menuCenterXDp)
    val tasbeehCxPx: Float get() = toPx(tasbeehCenterXDp)
    val openWerdCxPx: Float get() = toPx(openWerdCenterXDp)
    val barCenterYPx: Float get() = toPx(barCenterYDp)
    val drawnButtonRadiusPx: Float get() = toPx(drawnButtonRadiusDp)
    val textTopPaddingPx: Float get() = toPx(textTopPaddingDp)
    val textStartXPx: Float get() = toPx(textStartXDp)
    val availableTextHeightPx: Float get() = toPx(availableTextHeightDp)
    val maxTextWidthPx: Float get() = toPx(maxTextWidthDp)
    val citationCxPx: Float get() = toPx(citationCenterXDp)
    val maxCitationWidthPx: Float get() = toPx(maxCitationWidthDp)
}

object WidgetLayoutGeometry {
    fun compute(
        widthDp: Float,
        heightDp: Float,
        density: Float,
        scaleFactor: Float = 1f,
        hasMultiRep: Boolean = false,
        interactive: Boolean = true
    ): WidgetLayout {
        val width = widthDp.coerceAtLeast(1f)
        val height = heightDp.coerceAtLeast(1f)

        val mode = when {
            height < 75f -> WidgetDisplayMode.COMPACT
            height < 180f -> WidgetDisplayMode.MEDIUM
            else -> WidgetDisplayMode.EXPANDED
        }

        val margin = when (mode) {
            WidgetDisplayMode.COMPACT -> (width * 0.038f).coerceIn(8f, 14f)
            WidgetDisplayMode.MEDIUM -> (width * 0.042f).coerceIn(8f, 18f)
            WidgetDisplayMode.EXPANDED -> (width * 0.048f).coerceIn(12f, 22f)
        }

        val button = if (interactive) {
            when (mode) {
                WidgetDisplayMode.COMPACT -> (height * 0.72f).coerceIn(28f, 40f)
                WidgetDisplayMode.MEDIUM -> (height * 0.34f).coerceIn(34f, 44f)
                WidgetDisplayMode.EXPANDED -> 46f
            }
        } else 0f

        val bottomMargin = if (interactive) {
            when (mode) {
                WidgetDisplayMode.COMPACT -> 0f
                WidgetDisplayMode.MEDIUM -> (height * 0.035f).coerceIn(2f, 8f)
                WidgetDisplayMode.EXPANDED -> (height * 0.035f).coerceIn(6f, 12f)
            }
        } else 0f

        val barCenterY = when (mode) {
            WidgetDisplayMode.COMPACT -> height / 2f
            else -> height - bottomMargin - button / 2f
        }

        val menuCenterX: Float
        val tasbeehCenterX: Float
        val openWerdCenterX: Float
        val textStartX: Float
        val maxTextWidth: Float
        val textTop: Float
        val availableTextHeight: Float
        val headerCenter: Float?
        val maxCitationWidth: Float
        val citationCenterX: Float
        val barHeight: Float

        when (mode) {
            WidgetDisplayMode.COMPACT -> {
                barHeight = 0f
                // Symmetrical, optically centered 1-row widget layout:
                // Menu (☰) is placed on the left:
                menuCenterX = margin + button * 0.5f
                // Tasbeeh (📿) is placed on the right:
                tasbeehCenterX = width - margin - button * 0.5f
                // In single row mode, "فتح وِرد" is placed inside the ☰ menu so text is not compressed:
                openWerdCenterX = 0f

                // Text occupies the central content area with 100% equal left & right margins:
                val sideMargin = margin + button + 10f
                textStartX = sideMargin
                maxTextWidth = (width - 2 * sideMargin).coerceAtLeast(30f)
                textTop = 4f
                availableTextHeight = (height - 8f).coerceAtLeast(20f)

                headerCenter = null
                maxCitationWidth = 0f
                citationCenterX = 0f
            }
            WidgetDisplayMode.MEDIUM -> {
                barHeight = button
                menuCenterX = width - margin - button / 2f
                tasbeehCenterX = width - margin - button * 1.5f
                openWerdCenterX = margin + button / 2f

                headerCenter = null
                textTop = (margin * 0.5f).coerceIn(4f, 9f)
                val textBottom = barCenterY - button / 2f - 2f
                availableTextHeight = (textBottom - textTop).coerceAtLeast(32f)
                textStartX = margin
                maxTextWidth = (width - 2 * margin).coerceAtLeast(50f)

                // Middle area between openWerd on the far left and tasbeeh on the right:
                val leftEdge = margin + button + 8f
                val rightEdge = width - margin - 2 * button - 8f
                maxCitationWidth = (rightEdge - leftEdge).coerceAtLeast(0f)
                citationCenterX = (leftEdge + rightEdge) / 2f
            }
            WidgetDisplayMode.EXPANDED -> {
                barHeight = button
                menuCenterX = width - margin - button / 2f
                tasbeehCenterX = width - margin - button * 1.5f
                openWerdCenterX = margin + button / 2f

                headerCenter = 22f
                textTop = 42f
                val textBottom = barCenterY - button / 2f - 8f
                availableTextHeight = (textBottom - textTop).coerceAtLeast(60f)
                textStartX = margin
                maxTextWidth = (width - 2 * margin).coerceAtLeast(60f)

                val leftEdge = margin + button + 10f
                val rightEdge = width - margin - 2 * button - 10f
                maxCitationWidth = (rightEdge - leftEdge).coerceAtLeast(0f)
                citationCenterX = (leftEdge + rightEdge) / 2f
            }
        }

        return WidgetLayout(
            widthDp = width,
            heightDp = height,
            density = density.coerceAtLeast(1f),
            scaleFactor = scaleFactor.coerceAtLeast(0.1f),
            mode = mode,
            showInteractiveBar = interactive,
            hasMultiRep = hasMultiRep,
            barHeightDp = barHeight,
            bottomMarginDp = bottomMargin,
            rightMarginDp = margin,
            buttonTouchSizeDp = button,
            drawnButtonRadiusDp = button * 0.36f,
            barCenterYDp = barCenterY,
            menuCenterXDp = menuCenterX,
            tasbeehCenterXDp = tasbeehCenterX,
            openWerdCenterXDp = openWerdCenterX,
            headerCenterYDp = headerCenter,
            textTopPaddingDp = textTop,
            textStartXDp = textStartX,
            availableTextHeightDp = availableTextHeight,
            maxTextWidthDp = maxTextWidth,
            citationCenterXDp = citationCenterX,
            maxCitationWidthDp = maxCitationWidth
        )
    }
}

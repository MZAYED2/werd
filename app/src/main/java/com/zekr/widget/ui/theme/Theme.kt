package com.zekr.widget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** App chrome is always dark — never follow system light mode for onSurface. */
private val DarkColorScheme = darkColorScheme(
    primary = Emerald80,
    onPrimary = Color(0xFF062816),
    secondary = EmeraldGrey80,
    onSecondary = Color(0xFF0A1F0C),
    tertiary = Mint80,
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF2F4F7),
    surface = Color(0xFF16181E),
    onSurface = Color(0xFFF2F4F7),
    onSurfaceVariant = Color(0xFFB4BAC6),
    surfaceVariant = Color(0xFF22262F),
    outline = Color(0xFF3A3F4A),
    outlineVariant = Color(0xFF2A2E38)
)

@Composable
fun ZekrWidgetTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Studio UI is painted on dark surfaces; light/dynamic schemes put black text on them.
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

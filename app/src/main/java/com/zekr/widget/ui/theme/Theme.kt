package com.zekr.widget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.zekr.widget.ui.theme.WerdDesignTokens.IvoryJade as Light
import com.zekr.widget.ui.theme.WerdDesignTokens.MidnightJade as Dark

private val DarkColorScheme = darkColorScheme(
    primary = Color(Dark.AccessibleEmerald),
    onPrimary = Color(Dark.Background),
    inversePrimary = Color(Dark.Emerald),
    primaryContainer = Color(Dark.EmeraldSurface),
    onPrimaryContainer = Color(Dark.Text),
    secondary = Color(Dark.SecondaryText),
    onSecondary = Color(Dark.Elevated),
    secondaryContainer = Color(Dark.EmeraldSurface),
    onSecondaryContainer = Color(Dark.Text),
    tertiary = Color(Dark.Gold),
    background = Color(Dark.Background),
    onBackground = Color(Dark.Text),
    surface = Color(Dark.Surface),
    onSurface = Color(Dark.Text),
    onSurfaceVariant = Color(Dark.SecondaryText),
    surfaceVariant = Color(Dark.Elevated),
    surfaceContainer = Color(Dark.Surface),
    surfaceContainerHigh = Color(Dark.Elevated),
    outline = Color(Dark.SecondaryText),
    outlineVariant = Color(Dark.Divider),
    error = Color(Dark.Error)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(Light.Emerald),
    onPrimary = Color(Light.Surface),
    primaryContainer = Color(Light.EmeraldSurface),
    onPrimaryContainer = Color(Light.Text),
    secondary = Color(Light.SecondaryText),
    onSecondary = Color(Light.Surface),
    secondaryContainer = Color(Light.EmeraldSurface),
    onSecondaryContainer = Color(Light.Text),
    tertiary = Color(Light.Gold),
    background = Color(Light.Background),
    onBackground = Color(Light.Text),
    surface = Color(Light.Surface),
    onSurface = Color(Light.Text),
    onSurfaceVariant = Color(Light.SecondaryText),
    surfaceVariant = Color(Light.Elevated),
    surfaceContainer = Color(Light.Elevated),
    surfaceContainerHigh = Color(Light.Elevated),
    outline = Color(Light.SecondaryText),
    outlineVariant = Color(Light.Divider),
    error = Color(Light.Error)
)

// Gold is decorative on light cards. Sources use the standard text role for small-text contrast.
val sourceTextColor: Color
    @Composable get() = MaterialTheme.colorScheme.let {
        if (it.background == Color(Light.Background)) it.onSurface else it.tertiary
    }

@Composable
fun ZekrWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    accent: WerdAccent = WerdAccent.Jade,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = if (accent == WerdAccent.StrongSelection) palette.copy(
            secondaryContainer = palette.primary,
            onSecondaryContainer = palette.onPrimary
        ) else palette,
        typography = Typography,
        content = content
    )
}

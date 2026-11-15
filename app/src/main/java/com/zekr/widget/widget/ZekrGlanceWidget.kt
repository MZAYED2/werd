package com.zekr.widget.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.GradientDirection
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetFont
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.widget.render.GlassRenderer
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Keys stored in each widget's Glance Preferences for reactive recomposition
val KEY_ZEKR_ID = intPreferencesKey("key_current_zekr_id")
val KEY_COLOR_PRESET = stringPreferencesKey("key_color_preset")
val KEY_IS_GRADIENT = booleanPreferencesKey("key_is_gradient")
val KEY_PRIMARY_COLOR = longPreferencesKey("key_primary_color")
val KEY_SECONDARY_COLOR = longPreferencesKey("key_secondary_color")
val KEY_GRADIENT_DIRECTION = stringPreferencesKey("key_gradient_direction")
val KEY_GLASS_OPACITY = floatPreferencesKey("key_glass_opacity")
val KEY_BLUR_GRADE = floatPreferencesKey("key_blur_grade")
val KEY_PATTERN = stringPreferencesKey("key_pattern")
val KEY_PATTERN_OPACITY = floatPreferencesKey("key_pattern_opacity")
val KEY_PATTERN_SCALE = floatPreferencesKey("key_pattern_scale")
val KEY_FONT = stringPreferencesKey("key_font")
val KEY_FONT_SIZE = intPreferencesKey("key_font_size")
val KEY_UPDATE_VERSION = longPreferencesKey("key_update_version")

class ZekrGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    // Recompose at the real widget size. The default single size lets the launcher
    // stretch one bitmap, which squishes the Arabic text on resize.
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        suspend fun updateAllWidgets(context: Context, newSettings: WidgetSettings? = null, specificItemId: Int? = null) {
            val appContext = context.applicationContext
            val prefRepo = WidgetPreferencesRepository(appContext)
            val settings = newSettings ?: prefRepo.getSettings()
            val targetItemId = specificItemId ?: prefRepo.getCurrentItemId()
            val version = System.currentTimeMillis()

            try {
                val manager = GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(ZekrGlanceWidget::class.java)

                for (glanceId in glanceIds) {
                    updateAppWidgetState(appContext, glanceId) { prefs ->
                        prefs[KEY_ZEKR_ID] = targetItemId
                        prefs[KEY_COLOR_PRESET] = settings.colorPreset.id
                        prefs[KEY_IS_GRADIENT] = settings.isGradient
                        prefs[KEY_PRIMARY_COLOR] = settings.primaryColor
                        prefs[KEY_SECONDARY_COLOR] = settings.secondaryColor
                        prefs[KEY_GRADIENT_DIRECTION] = settings.gradientDirection.id
                        prefs[KEY_GLASS_OPACITY] = settings.glassOpacity
                        prefs[KEY_BLUR_GRADE] = settings.blurGrade
                        prefs[KEY_PATTERN] = settings.pattern.id
                        prefs[KEY_PATTERN_OPACITY] = settings.patternOpacity
                        prefs[KEY_PATTERN_SCALE] = settings.patternScale
                        prefs[KEY_FONT] = settings.font.id
                        prefs[KEY_FONT_SIZE] = settings.fontSizeSp
                        prefs[KEY_UPDATE_VERSION] = version
                    }
                    ZekrGlanceWidget().update(appContext, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun shuffleToNext(context: Context) {
            val appContext = context.applicationContext
            val prefRepo = WidgetPreferencesRepository(appContext)
            val settings = prefRepo.getSettings()
            val currentId = prefRepo.getCurrentItemId()

            val nextItem = ZekrRepository.getRandomItem(
                context = appContext,
                includeQuran = settings.includeQuran,
                includeHadith = settings.includeHadith,
                includeAzkar = settings.includeAzkar,
                currentId = currentId
            )
            prefRepo.setCurrentItemId(nextItem.id)
            updateAllWidgets(context = appContext, newSettings = settings, specificItemId = nextItem.id)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext

        provideContent {
            val prefs = currentState<Preferences>()

            // Extract reactive preferences for this widget
            val currentId = prefs[KEY_ZEKR_ID] ?: 1
            val colorStr = prefs[KEY_COLOR_PRESET] ?: GlassColorPreset.EMERALD_SAGE.id
            val colorPreset = GlassColorPreset.values().find { it.id == colorStr } ?: GlassColorPreset.EMERALD_SAGE
            val isGradient = prefs[KEY_IS_GRADIENT] ?: false
            val primaryColor = prefs[KEY_PRIMARY_COLOR] ?: colorPreset.hexColor
            val secondaryColor = prefs[KEY_SECONDARY_COLOR] ?: 0xFF021B0F
            val dirStr = prefs[KEY_GRADIENT_DIRECTION] ?: GradientDirection.DIAGONAL.id
            val gradientDirection = GradientDirection.values().find { it.id == dirStr } ?: GradientDirection.DIAGONAL
            val glassOpacity = prefs[KEY_GLASS_OPACITY] ?: 0.35f
            val blurGrade = prefs[KEY_BLUR_GRADE] ?: 0.5f
            val patternStr = prefs[KEY_PATTERN] ?: PatternType.ARABIC_CALLIGRAPHY.id
            val pattern = when (patternStr) {
                "CUSTOM_IMAGE" -> PatternType.NONE
                else -> PatternType.values().find { it.id == patternStr } ?: PatternType.ARABIC_CALLIGRAPHY
            }
            val patternOpacity = prefs[KEY_PATTERN_OPACITY] ?: 0.28f
            val patternScale = prefs[KEY_PATTERN_SCALE] ?: 1.0f
            val fontStr = prefs[KEY_FONT] ?: WidgetFont.AMIRI.id
            val font = WidgetFont.values().find { it.id == fontStr } ?: WidgetFont.AMIRI
            val fontSizeSp = prefs[KEY_FONT_SIZE] ?: 18

            val item: ZekrEntity = ZekrRepository.getById(appContext, currentId)

            val widgetSize = LocalSize.current
            val (bitmapW, bitmapH) = widgetBitmapPx(
                widthDp = widgetSize.width.value,
                heightDp = widgetSize.height.value,
                density = appContext.resources.displayMetrics.density
            )

            // Bitmap matches the widget aspect, so FillBounds scales both axes together.
            val fullWidgetBitmap = GlassRenderer.createFullWidgetBitmap(
                context = appContext,
                width = bitmapW,
                height = bitmapH,
                item = item,
                colorPreset = colorPreset,
                isGradient = isGradient,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                gradientDirection = gradientDirection,
                opacity = glassOpacity,
                blurGrade = blurGrade,
                pattern = pattern,
                patternOpacity = patternOpacity,
                patternScale = patternScale,
                font = font,
                fontSizeSp = fontSizeSp,
                cornerRadius = 56f
            )

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionRunCallback<ShuffleWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(fullWidgetBitmap),
                        contentDescription = "Zekr Widget",
                        contentScale = ContentScale.FillBounds,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private const val MAX_WIDGET_BITMAP_PIXELS = 700_000L

/** Pixel size for the glass bitmap. Same aspect as the widget, capped so the bitmap stays bindable. */
internal fun widgetBitmapPx(widthDp: Float, heightDp: Float, density: Float): Pair<Int, Int> {
    if (!widthDp.isFinite() || !heightDp.isFinite() ||
        widthDp <= 1f || heightDp <= 1f || density <= 0f
    ) {
        return 680 to 380
    }
    var widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
    var heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)
    val pixels = widthPx.toLong() * heightPx.toLong()
    if (pixels <= MAX_WIDGET_BITMAP_PIXELS) return widthPx to heightPx
    val scale = sqrt(MAX_WIDGET_BITMAP_PIXELS.toDouble() / pixels.toDouble())
    widthPx = (widthPx * scale).roundToInt().coerceAtLeast(1)
    heightPx = (heightPx * scale).roundToInt().coerceAtLeast(1)
    return widthPx to heightPx
}

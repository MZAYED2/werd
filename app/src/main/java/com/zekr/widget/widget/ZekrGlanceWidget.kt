package com.zekr.widget.widget

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
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
val KEY_CURRENT_REP_COUNT = intPreferencesKey("key_current_rep_count")
val KEY_IS_FAVORITE = booleanPreferencesKey("key_is_favorite")
val KEY_IS_PINNED = booleanPreferencesKey("key_is_pinned")
val KEY_UPDATE_VERSION = longPreferencesKey("key_update_version")

class ZekrGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    // Recompose at the real widget size. The default single size lets the launcher
    // stretch one bitmap, which squishes the Arabic text on resize.
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        suspend fun updateAllWidgets(
            context: Context,
            newSettings: WidgetSettings? = null,
            specificItemId: Int? = null
        ) {
            val appContext = context.applicationContext
            val prefRepo = WidgetPreferencesRepository(appContext)
            val settings = newSettings ?: prefRepo.getSettings()
            val targetItemId = specificItemId ?: prefRepo.getCurrentItemId()
            val repCount = prefRepo.getCurrentRepetitionCount()
            val favoriteIds = prefRepo.getFavoriteIds()
            val pinnedId = prefRepo.getPinnedItemId()
            val isFavorite = targetItemId in favoriteIds
            val isPinned = pinnedId != null && pinnedId == targetItemId
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
                        prefs[KEY_CURRENT_REP_COUNT] = repCount
                        prefs[KEY_IS_FAVORITE] = isFavorite
                        prefs[KEY_IS_PINNED] = isPinned
                        prefs[KEY_UPDATE_VERSION] = version
                    }
                    ZekrGlanceWidget().update(appContext, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun shuffleToNext(context: Context, clearPin: Boolean = false) {
            val appContext = context.applicationContext
            val prefRepo = WidgetPreferencesRepository(appContext)
            if (clearPin) {
                prefRepo.setPinnedItemId(null)
            }
            val settings = prefRepo.getSettings()
            val currentId = prefRepo.getCurrentItemId()
            val seenIds = prefRepo.getSeenItemIds()
            val favoriteIds = prefRepo.getFavoriteIds()
            val customItems = prefRepo.getCustomItems()

            val nextResult = ZekrRepository.selectNextFromPool(
                allItems = ZekrRepository.getAllItems(appContext, customItems),
                includeQuran = settings.includeQuran,
                includeHadith = settings.includeHadith,
                includeAzkar = settings.includeAzkar,
                smartTimeContextEnabled = settings.smartTimeContextEnabled,
                favoritesOnlyMode = settings.favoritesOnlyMode,
                favoriteIds = favoriteIds,
                currentId = currentId,
                seenIds = seenIds
            )
            prefRepo.saveShuffleProgress(nextResult.item.id, nextResult.updatedSeenIds)
            updateAllWidgets(context = appContext, newSettings = settings, specificItemId = nextResult.item.id)
        }

        suspend fun handleBodyTap(context: Context) {
            val appContext = context.applicationContext
            val prefRepo = WidgetPreferencesRepository(appContext)
            val currentId = prefRepo.getCurrentItemId()
            val customItems = prefRepo.getCustomItems()
            val currentItem = ZekrRepository.getById(appContext, currentId, customItems)

            val tapResult = prefRepo.recordZekrTap(currentItem)
            if (tapResult.reachedTarget && !tapResult.isPinned) {
                shuffleToNext(appContext, clearPin = false)
            } else {
                updateAllWidgets(context = appContext, specificItemId = currentItem.id)
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val prefRepo = WidgetPreferencesRepository(appContext)
        val customItems = prefRepo.getCustomItems()

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
            val currentRepetition = prefs[KEY_CURRENT_REP_COUNT] ?: 0
            val isFavorite = prefs[KEY_IS_FAVORITE] ?: false
            val isPinned = prefs[KEY_IS_PINNED] ?: false

            val item: ZekrEntity = ZekrRepository.getById(appContext, currentId, customItems)

            val widgetSize = LocalSize.current
            val (bitmapW, bitmapH) = widgetBitmapPx(
                widthDp = widgetSize.width.value,
                heightDp = widgetSize.height.value,
                density = appContext.resources.displayMetrics.density
            )
            val showInteractiveBar = widgetSize.height.value >= 110f

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
                cornerRadius = 56f,
                currentRepetition = currentRepetition,
                isFavorite = isFavorite,
                isPinned = isPinned,
                showInteractiveBar = showInteractiveBar
            )

            val isRtl = appContext.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

            GlanceTheme {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(fullWidgetBitmap),
                        contentDescription = "Zekr Widget",
                        contentScale = ContentScale.FillBounds,
                        modifier = GlanceModifier.fillMaxSize()
                    )

                    if (showInteractiveBar) {
                        Column(modifier = GlanceModifier.fillMaxSize()) {
                            // Main Body Zone: Tap to count repetition / advance
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .defaultWeight()
                                    .clickable(actionRunCallback<TapZekrBodyAction>())
                            ) {}

                            // Bottom Action Bar Zone aligned to physical canvas positions regardless of system LTR/RTL
                            BottomTouchBar(isRtl = isRtl)
                        }
                    } else {
                        // Compact Widget Fallback: single tap anywhere counts/advances
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .clickable(actionRunCallback<TapZekrBodyAction>())
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomTouchBar(isRtl: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRtl) {
            // RTL Row flows Right -> Left: [Favorite, Share, Center, Skip]
            Box(
                modifier = GlanceModifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable(actionRunCallback<ToggleFavoriteWidgetAction>())
            ) {}
            Box(
                modifier = GlanceModifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable(actionRunCallback<ShareWidgetAction>())
            ) {}
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(actionRunCallback<TapZekrBodyAction>())
            ) {}
            Box(
                modifier = GlanceModifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable(actionRunCallback<SkipWidgetAction>())
            ) {}
        } else {
            // LTR Row flows Left -> Right: [Skip, Center, Share, Favorite]
            Box(
                modifier = GlanceModifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable(actionRunCallback<SkipWidgetAction>())
            ) {}
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(actionRunCallback<TapZekrBodyAction>())
            ) {}
            Box(
                modifier = GlanceModifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable(actionRunCallback<ShareWidgetAction>())
            ) {}
            Box(
                modifier = GlanceModifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable(actionRunCallback<ToggleFavoriteWidgetAction>())
            ) {}
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

package com.zekr.widget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.widget.render.GlassRenderer

// Keys stored in each widget's Glance Preferences for reactive recomposition
val KEY_ZEKR_ID = intPreferencesKey("key_current_zekr_id")
val KEY_COLOR_PRESET = stringPreferencesKey("key_color_preset")
val KEY_GLASS_OPACITY = floatPreferencesKey("key_glass_opacity")
val KEY_PATTERN = stringPreferencesKey("key_pattern")
val KEY_PATTERN_OPACITY = floatPreferencesKey("key_pattern_opacity")
val KEY_PATTERN_SCALE = floatPreferencesKey("key_pattern_scale")
val KEY_FONT_SIZE = intPreferencesKey("key_font_size")
val KEY_UPDATE_VERSION = longPreferencesKey("key_update_version")

class ZekrGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

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
                        prefs[KEY_GLASS_OPACITY] = settings.glassOpacity
                        prefs[KEY_PATTERN] = settings.pattern.id
                        prefs[KEY_PATTERN_OPACITY] = settings.patternOpacity
                        prefs[KEY_PATTERN_SCALE] = settings.patternScale
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
            val glassOpacity = prefs[KEY_GLASS_OPACITY] ?: 0.35f
            val patternStr = prefs[KEY_PATTERN] ?: PatternType.STAR_OCTAGRAM.id
            val pattern = PatternType.values().find { it.id == patternStr } ?: PatternType.STAR_OCTAGRAM
            val patternOpacity = prefs[KEY_PATTERN_OPACITY] ?: 0.25f
            val patternScale = prefs[KEY_PATTERN_SCALE] ?: 1.0f
            val fontSizeSp = prefs[KEY_FONT_SIZE] ?: 18

            val item: ZekrEntity = ZekrRepository.getById(appContext, currentId)

            // Render fresh glass bitmap whenever preferences change
            val glassBitmap = GlassRenderer.createGlassBackgroundBitmap(
                width = 680,
                height = 380,
                colorPreset = colorPreset,
                opacity = glassOpacity,
                pattern = pattern,
                patternOpacity = patternOpacity,
                patternScale = patternScale,
                cornerRadius = 56f
            )

            val textColor = Color(colorPreset.textColor)
            val accentColor = Color(colorPreset.accentColor)

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionRunCallback<ShuffleWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    // Glass Canvas Background
                    Image(
                        provider = ImageProvider(glassBitmap),
                        contentDescription = "Glass Background",
                        contentScale = ContentScale.FillBounds,
                        modifier = GlanceModifier.fillMaxSize()
                    )

                    // Widget Interactive Content
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Header: Type Pill & Subtle Tap Hint
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⟳ انقر للتبديل",
                                style = TextStyle(
                                    color = ColorProvider(accentColor.copy(alpha = 0.75f)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = item.type.labelArabic,
                                style = TextStyle(
                                    color = ColorProvider(accentColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Center: Main Arabic Text (Authentic Hadith / Ayah / Zikr)
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.arabicText,
                                style = TextStyle(
                                    color = ColorProvider(textColor),
                                    fontSize = fontSizeSp.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Footer: Exact Source & Authentication
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (item.reference.isNotEmpty()) "${item.source} • ${item.reference}" else item.source,
                                style = TextStyle(
                                    color = ColorProvider(textColor.copy(alpha = 0.82f)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

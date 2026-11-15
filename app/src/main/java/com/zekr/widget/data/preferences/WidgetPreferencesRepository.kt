package com.zekr.widget.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_settings")

object WidgetPreferenceKeys {
    val KEY_FONT = stringPreferencesKey("pref_font")
    val KEY_FONT_SIZE = intPreferencesKey("pref_font_size")
    val KEY_COLOR_PRESET = stringPreferencesKey("pref_color_preset")
    val KEY_IS_GRADIENT = booleanPreferencesKey("is_gradient")
    val KEY_PRIMARY_COLOR = longPreferencesKey("primary_color")
    val KEY_SECONDARY_COLOR = longPreferencesKey("secondary_color")
    val KEY_GRADIENT_DIRECTION = stringPreferencesKey("gradient_direction")
    val KEY_GLASS_OPACITY = floatPreferencesKey("pref_glass_opacity")
    val KEY_BLUR_GRADE = floatPreferencesKey("pref_blur_grade")
    val KEY_PATTERN = stringPreferencesKey("pref_pattern")
    val KEY_PATTERN_OPACITY = floatPreferencesKey("pref_pattern_opacity")
    val KEY_PATTERN_SCALE = floatPreferencesKey("pref_pattern_scale")
    val KEY_INCLUDE_QURAN = booleanPreferencesKey("pref_include_quran")
    val KEY_INCLUDE_HADITH = booleanPreferencesKey("pref_include_hadith")
    val KEY_INCLUDE_AZKAR = booleanPreferencesKey("pref_include_azkar")
    val KEY_SMART_TIME_CONTEXT = booleanPreferencesKey("pref_smart_time_context")
    val KEY_SERVICE_ENABLED = booleanPreferencesKey("pref_service_enabled")
    val KEY_STYLE_ROTATION_ENABLED = booleanPreferencesKey("pref_style_rotation_enabled")
    val KEY_STYLE_ROTATION_HOURS = intPreferencesKey("pref_style_rotation_hours")
    val KEY_CURRENT_ITEM_ID = intPreferencesKey("pref_current_item_id")
    val KEY_CUSTOM_IMAGE_URI = stringPreferencesKey("pref_custom_image_uri")
    val KEY_SEEN_ITEM_IDS = stringPreferencesKey("pref_seen_item_ids")
}

class WidgetPreferencesRepository(private val context: Context) {

    val settingsFlow: Flow<WidgetSettings> = context.dataStore.data.map { prefs ->
        val fontStr = prefs[WidgetPreferenceKeys.KEY_FONT] ?: WidgetFont.AMIRI.id
        val font = WidgetFont.values().find { it.id == fontStr } ?: WidgetFont.AMIRI

        val fontSize = prefs[WidgetPreferenceKeys.KEY_FONT_SIZE] ?: 18

        val colorStr = prefs[WidgetPreferenceKeys.KEY_COLOR_PRESET] ?: GlassColorPreset.EMERALD_SAGE.id
        val colorPreset = GlassColorPreset.values().find { it.id == colorStr } ?: GlassColorPreset.EMERALD_SAGE

        val isGradient = prefs[WidgetPreferenceKeys.KEY_IS_GRADIENT] ?: false
        val primaryColor = prefs[WidgetPreferenceKeys.KEY_PRIMARY_COLOR] ?: colorPreset.hexColor
        val secondaryColor = prefs[WidgetPreferenceKeys.KEY_SECONDARY_COLOR] ?: 0xFF021B0F
        val gradientDirStr = prefs[WidgetPreferenceKeys.KEY_GRADIENT_DIRECTION] ?: GradientDirection.DIAGONAL.name
        val gradientDirection = GradientDirection.values().find { it.name == gradientDirStr || it.id == gradientDirStr } ?: GradientDirection.DIAGONAL

        val opacity = prefs[WidgetPreferenceKeys.KEY_GLASS_OPACITY] ?: 0.35f
        val blurGrade = prefs[WidgetPreferenceKeys.KEY_BLUR_GRADE] ?: 0.5f

        val patternStr = prefs[WidgetPreferenceKeys.KEY_PATTERN] ?: PatternType.STAR_OCTAGRAM.id
        val pattern = when (patternStr) {
            "CUSTOM_IMAGE" -> PatternType.NONE
            else -> PatternType.values().find { it.id == patternStr } ?: PatternType.STAR_OCTAGRAM
        }

        val patternOpacity = prefs[WidgetPreferenceKeys.KEY_PATTERN_OPACITY] ?: 0.25f
        val patternScale = prefs[WidgetPreferenceKeys.KEY_PATTERN_SCALE] ?: 1.0f

        val includeQuran = prefs[WidgetPreferenceKeys.KEY_INCLUDE_QURAN] ?: true
        val includeHadith = prefs[WidgetPreferenceKeys.KEY_INCLUDE_HADITH] ?: true
        val includeAzkar = prefs[WidgetPreferenceKeys.KEY_INCLUDE_AZKAR] ?: true
        val smartTimeContextEnabled = prefs[WidgetPreferenceKeys.KEY_SMART_TIME_CONTEXT] ?: true
        val serviceEnabled = prefs[WidgetPreferenceKeys.KEY_SERVICE_ENABLED] ?: true
        val styleRotationEnabled = prefs[WidgetPreferenceKeys.KEY_STYLE_ROTATION_ENABLED] ?: false
        val styleRotationHours = (prefs[WidgetPreferenceKeys.KEY_STYLE_ROTATION_HOURS] ?: 6)
            .coerceIn(1, 168)
        val customImageUri = prefs[WidgetPreferenceKeys.KEY_CUSTOM_IMAGE_URI]

        WidgetSettings(
            font = font,
            fontSizeSp = fontSize,
            colorPreset = colorPreset,
            isGradient = isGradient,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            gradientDirection = gradientDirection,
            glassOpacity = opacity,
            blurGrade = blurGrade,
            pattern = pattern,
            patternOpacity = patternOpacity,
            patternScale = patternScale,
            customImageUri = customImageUri,
            includeQuran = includeQuran,
            includeHadith = includeHadith,
            includeAzkar = includeAzkar,
            smartTimeContextEnabled = smartTimeContextEnabled,
            serviceEnabled = serviceEnabled,
            styleRotationEnabled = styleRotationEnabled,
            styleRotationHours = styleRotationHours
        )
    }

    val currentItemIdFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] ?: 1
    }

    suspend fun getSettings(): WidgetSettings = settingsFlow.first()

    suspend fun getCurrentItemId(): Int = currentItemIdFlow.first()

    suspend fun getSeenItemIds(): Set<Int> {
        val raw = context.dataStore.data.first()[WidgetPreferenceKeys.KEY_SEEN_ITEM_IDS].orEmpty()
        return parseIdSet(raw)
    }

    suspend fun updateSettings(settings: WidgetSettings) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_FONT] = settings.font.id
            prefs[WidgetPreferenceKeys.KEY_FONT_SIZE] = settings.fontSizeSp
            prefs[WidgetPreferenceKeys.KEY_COLOR_PRESET] = settings.colorPreset.id
            prefs[WidgetPreferenceKeys.KEY_IS_GRADIENT] = settings.isGradient
            prefs[WidgetPreferenceKeys.KEY_PRIMARY_COLOR] = settings.primaryColor
            prefs[WidgetPreferenceKeys.KEY_SECONDARY_COLOR] = settings.secondaryColor
            prefs[WidgetPreferenceKeys.KEY_GRADIENT_DIRECTION] = settings.gradientDirection.name
            prefs[WidgetPreferenceKeys.KEY_GLASS_OPACITY] = settings.glassOpacity
            prefs[WidgetPreferenceKeys.KEY_BLUR_GRADE] = settings.blurGrade
            prefs[WidgetPreferenceKeys.KEY_PATTERN] = settings.pattern.id
            prefs[WidgetPreferenceKeys.KEY_PATTERN_OPACITY] = settings.patternOpacity
            prefs[WidgetPreferenceKeys.KEY_PATTERN_SCALE] = settings.patternScale
            prefs[WidgetPreferenceKeys.KEY_INCLUDE_QURAN] = settings.includeQuran
            prefs[WidgetPreferenceKeys.KEY_INCLUDE_HADITH] = settings.includeHadith
            prefs[WidgetPreferenceKeys.KEY_INCLUDE_AZKAR] = settings.includeAzkar
            prefs[WidgetPreferenceKeys.KEY_SMART_TIME_CONTEXT] = settings.smartTimeContextEnabled
            prefs[WidgetPreferenceKeys.KEY_SERVICE_ENABLED] = settings.serviceEnabled
            prefs[WidgetPreferenceKeys.KEY_STYLE_ROTATION_ENABLED] = settings.styleRotationEnabled
            prefs[WidgetPreferenceKeys.KEY_STYLE_ROTATION_HOURS] =
                settings.styleRotationHours.coerceIn(1, 168)
            if (settings.customImageUri != null) {
                prefs[WidgetPreferenceKeys.KEY_CUSTOM_IMAGE_URI] = settings.customImageUri
            } else {
                prefs.remove(WidgetPreferenceKeys.KEY_CUSTOM_IMAGE_URI)
            }
        }
    }

    suspend fun setCurrentItemId(id: Int) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] = id
            val seen = parseIdSet(prefs[WidgetPreferenceKeys.KEY_SEEN_ITEM_IDS].orEmpty()) + id
            prefs[WidgetPreferenceKeys.KEY_SEEN_ITEM_IDS] = serializeIdSet(seen)
        }
    }

    suspend fun saveShuffleProgress(nextItemId: Int, updatedSeenIds: Set<Int>) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] = nextItemId
            prefs[WidgetPreferenceKeys.KEY_SEEN_ITEM_IDS] = serializeIdSet(updatedSeenIds + nextItemId)
        }
    }

    companion object {
        internal fun parseIdSet(raw: String): Set<Int> {
            if (raw.isBlank()) return emptySet()
            return raw.split(',')
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()
        }

        internal fun serializeIdSet(ids: Set<Int>): String {
            return ids.joinToString(",")
        }
    }
}

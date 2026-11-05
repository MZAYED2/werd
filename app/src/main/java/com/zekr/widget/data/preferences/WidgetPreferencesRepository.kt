package com.zekr.widget.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
    val KEY_GLASS_OPACITY = floatPreferencesKey("pref_glass_opacity")
    val KEY_PATTERN = stringPreferencesKey("pref_pattern")
    val KEY_PATTERN_OPACITY = floatPreferencesKey("pref_pattern_opacity")
    val KEY_PATTERN_SCALE = floatPreferencesKey("pref_pattern_scale")
    val KEY_INCLUDE_QURAN = booleanPreferencesKey("pref_include_quran")
    val KEY_INCLUDE_HADITH = booleanPreferencesKey("pref_include_hadith")
    val KEY_INCLUDE_AZKAR = booleanPreferencesKey("pref_include_azkar")
    val KEY_SERVICE_ENABLED = booleanPreferencesKey("pref_service_enabled")
    val KEY_CURRENT_ITEM_ID = intPreferencesKey("pref_current_item_id")
}

class WidgetPreferencesRepository(private val context: Context) {

    val settingsFlow: Flow<WidgetSettings> = context.dataStore.data.map { prefs ->
        val fontStr = prefs[WidgetPreferenceKeys.KEY_FONT] ?: WidgetFont.AMIRI.id
        val font = WidgetFont.values().find { it.id == fontStr } ?: WidgetFont.AMIRI

        val fontSize = prefs[WidgetPreferenceKeys.KEY_FONT_SIZE] ?: 18

        val colorStr = prefs[WidgetPreferenceKeys.KEY_COLOR_PRESET] ?: GlassColorPreset.EMERALD_SAGE.id
        val colorPreset = GlassColorPreset.values().find { it.id == colorStr } ?: GlassColorPreset.EMERALD_SAGE

        val opacity = prefs[WidgetPreferenceKeys.KEY_GLASS_OPACITY] ?: 0.35f

        val patternStr = prefs[WidgetPreferenceKeys.KEY_PATTERN] ?: PatternType.STAR_OCTAGRAM.id
        val pattern = PatternType.values().find { it.id == patternStr } ?: PatternType.STAR_OCTAGRAM

        val patternOpacity = prefs[WidgetPreferenceKeys.KEY_PATTERN_OPACITY] ?: 0.25f
        val patternScale = prefs[WidgetPreferenceKeys.KEY_PATTERN_SCALE] ?: 1.0f

        val includeQuran = prefs[WidgetPreferenceKeys.KEY_INCLUDE_QURAN] ?: true
        val includeHadith = prefs[WidgetPreferenceKeys.KEY_INCLUDE_HADITH] ?: true
        val includeAzkar = prefs[WidgetPreferenceKeys.KEY_INCLUDE_AZKAR] ?: true
        val serviceEnabled = prefs[WidgetPreferenceKeys.KEY_SERVICE_ENABLED] ?: true

        WidgetSettings(
            font = font,
            fontSizeSp = fontSize,
            colorPreset = colorPreset,
            glassOpacity = opacity,
            pattern = pattern,
            patternOpacity = patternOpacity,
            patternScale = patternScale,
            includeQuran = includeQuran,
            includeHadith = includeHadith,
            includeAzkar = includeAzkar,
            serviceEnabled = serviceEnabled
        )
    }

    val currentItemIdFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] ?: 1
    }

    suspend fun getSettings(): WidgetSettings = settingsFlow.first()

    suspend fun getCurrentItemId(): Int = currentItemIdFlow.first()

    suspend fun updateSettings(settings: WidgetSettings) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_FONT] = settings.font.id
            prefs[WidgetPreferenceKeys.KEY_FONT_SIZE] = settings.fontSizeSp
            prefs[WidgetPreferenceKeys.KEY_COLOR_PRESET] = settings.colorPreset.id
            prefs[WidgetPreferenceKeys.KEY_GLASS_OPACITY] = settings.glassOpacity
            prefs[WidgetPreferenceKeys.KEY_PATTERN] = settings.pattern.id
            prefs[WidgetPreferenceKeys.KEY_PATTERN_OPACITY] = settings.patternOpacity
            prefs[WidgetPreferenceKeys.KEY_PATTERN_SCALE] = settings.patternScale
            prefs[WidgetPreferenceKeys.KEY_INCLUDE_QURAN] = settings.includeQuran
            prefs[WidgetPreferenceKeys.KEY_INCLUDE_HADITH] = settings.includeHadith
            prefs[WidgetPreferenceKeys.KEY_INCLUDE_AZKAR] = settings.includeAzkar
            prefs[WidgetPreferenceKeys.KEY_SERVICE_ENABLED] = settings.serviceEnabled
        }
    }

    suspend fun setCurrentItemId(id: Int) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] = id
        }
    }
}

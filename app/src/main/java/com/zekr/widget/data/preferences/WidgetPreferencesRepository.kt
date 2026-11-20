package com.zekr.widget.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.WirdStats
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.repository.DhikrCategory
import com.zekr.widget.data.repository.ZekrRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    val KEY_FAVORITES_ONLY_MODE = booleanPreferencesKey("pref_favorites_only_mode")
    val KEY_SMART_TIME_CONTEXT = booleanPreferencesKey("pref_smart_time_context")
    val KEY_SERVICE_ENABLED = booleanPreferencesKey("pref_service_enabled")
    val KEY_STYLE_ROTATION_ENABLED = booleanPreferencesKey("pref_style_rotation_enabled")
    val KEY_STYLE_ROTATION_HOURS = intPreferencesKey("pref_style_rotation_hours")
    val KEY_CURRENT_ITEM_ID = intPreferencesKey("pref_current_item_id")
    val KEY_CUSTOM_IMAGE_URI = stringPreferencesKey("pref_custom_image_uri")
    val KEY_SEEN_ITEM_IDS = stringPreferencesKey("pref_seen_item_ids")

    // Interactive widget, Favorites, Pinning, Custom Adhkar & Daily Streak
    val KEY_FAVORITE_IDS = stringPreferencesKey("pref_favorite_ids")
    val KEY_PINNED_ITEM_ID = intPreferencesKey("pref_pinned_item_id")
    val KEY_CURRENT_REP_COUNT = intPreferencesKey("pref_current_rep_count")
    val KEY_LAST_REP_TAP_MS = longPreferencesKey("pref_last_rep_tap_ms")
    val KEY_CUSTOM_ITEMS_JSON = stringPreferencesKey("pref_custom_items_json")
    val KEY_WIRD_STATS_JSON = stringPreferencesKey("pref_wird_stats_json")

    // Smart Tasbeeh State & Persistent Counter
    val KEY_LAST_TASBEEH_ZEKR_ID = intPreferencesKey("pref_last_tasbeeh_zekr_id")
    val KEY_LAST_TASBEEH_COUNT = intPreferencesKey("pref_last_tasbeeh_count")
    val KEY_LAST_TASBEEH_TARGET = intPreferencesKey("pref_last_tasbeeh_target")
    val KEY_LAST_TASBEEH_FREE_MODE = booleanPreferencesKey("pref_last_tasbeeh_free_mode")
    val KEY_TASBEEH_VIBRATION = booleanPreferencesKey("pref_tasbeeh_vibration")
    val KEY_TASBEEH_SOUND = booleanPreferencesKey("pref_tasbeeh_sound")
    val KEY_TASBEEH_DIALOG_SIZE = stringPreferencesKey("pref_tasbeeh_dialog_size")
    val KEY_TASBEEH_CARD_OPACITY = floatPreferencesKey("pref_tasbeeh_card_opacity")
    val KEY_TASBEEH_SCRIM_OPACITY = floatPreferencesKey("pref_tasbeeh_scrim_opacity")
    val KEY_TASBEEH_SAVED_SESSIONS = stringPreferencesKey("pref_tasbeeh_saved_sessions")
    val KEY_TASBEEH_UNDOABLE_COUNT = intPreferencesKey("pref_tasbeeh_undoable_count")
    val KEY_TASBEEH_CUSTOM_TARGET = intPreferencesKey("pref_tasbeeh_custom_target")
    val KEY_TASBEEH_CATEGORY = stringPreferencesKey("pref_tasbeeh_category")
    val KEY_TASBEEH_GOAL_RECORDED = booleanPreferencesKey("pref_tasbeeh_goal_recorded")
    val KEY_TASBEEH_SESSION_COMPLETED = booleanPreferencesKey("pref_tasbeeh_session_completed")
    val KEY_TASBEEH_SEEN_SALAWAT_IDS = stringPreferencesKey("pref_tasbeeh_seen_salawat_ids")
    val KEY_TASBEEH_SEEN_TASBEEH_IDS = stringPreferencesKey("pref_tasbeeh_seen_tasbeeh_ids")
    val KEY_TASBEEH_SEEN_GENERAL_IDS = stringPreferencesKey("pref_tasbeeh_seen_general_ids")

    // Smart Notifications & Reminders
    val KEY_NOTIF_SALAWAT_ENABLED = booleanPreferencesKey("pref_notif_salawat_enabled")
    val KEY_NOTIF_TASBEEH_ENABLED = booleanPreferencesKey("pref_notif_tasbeeh_enabled")
    val KEY_NOTIF_DUA_ENABLED = booleanPreferencesKey("pref_notif_dua_enabled")
    val KEY_NOTIF_CONFIRMED_DUA_DATE = stringPreferencesKey("pref_notif_confirmed_dua_date")
    val KEY_NOTIF_WIRD_ENABLED = booleanPreferencesKey("pref_notif_wird_enabled")
    val KEY_NOTIF_COUNT_PER_DAY = intPreferencesKey("pref_notif_count_per_day")
    val KEY_NOTIF_START_HOUR = intPreferencesKey("pref_notif_start_hour")
    val KEY_NOTIF_END_HOUR = intPreferencesKey("pref_notif_end_hour")
    val KEY_NOTIF_QUIET_HOURS_ENABLED = booleanPreferencesKey("pref_notif_quiet_hours_enabled")
    val KEY_NOTIF_CONFIRMED_SALAWAT_DATE = stringPreferencesKey("pref_notif_confirmed_salawat_date")
    val KEY_NOTIF_CONFIRMED_TASBEEH_DATE = stringPreferencesKey("pref_notif_confirmed_tasbeeh_date")
    val KEY_NOTIF_CONFIRMED_WIRD_DATE = stringPreferencesKey("pref_notif_confirmed_wird_date")
    val KEY_HAS_PROMPTED_PERMISSIONS = booleanPreferencesKey("pref_has_prompted_permissions_v18")

    fun widgetSettingsKey(widgetId: Int) = stringPreferencesKey("widget_instance_settings_$widgetId")
}

@Serializable
enum class ReminderType(val id: String, val titleArabic: String) {
    SALAWAT("salawat", "الصلاة على النبي ﷺ"),
    TASBEEH("tasbeeh", "التسبيح"),
    WIRD("wird", "الورد اليومي المنظم"),
    DUA("dua", "الدعاء المأثور"),
    VERSE("verse", "آية وتدبر"),
    HADITH("hadith", "حديث شريف"),
    DHIKR("dhikr", "أذكار مأثورة")
}

@Serializable
data class NotificationSettings(
    val salawatEnabled: Boolean = true,
    val tasbeehEnabled: Boolean = true,
    val wirdEnabled: Boolean = true,
    val remindersDailyCount: Int = 3,
    val readingEnabled: Boolean = false,
    val readingQuranEnabled: Boolean = true,
    val readingHadithEnabled: Boolean = true,
    val readingAthkarEnabled: Boolean = true,
    val readingDailyCount: Int = 5,
    val lockScreenEnabled: Boolean = false,
    val lockScreenIntervalMinutes: Int = 30,
    val lockScreenQuranEnabled: Boolean = true,
    val lockScreenHadithEnabled: Boolean = true,
    val lockScreenAthkarEnabled: Boolean = true,
    val dailyCount: Int = 3,
    val startHour: Int = 8,
    val endHour: Int = 22,
    val quietHoursEnabled: Boolean = true,
    val duaEnabled: Boolean = false,
    val contentEnabled: Boolean = false
)

data class UndoResult(
    val newCount: Int,
    val canUndoMore: Boolean,
    val updatedStats: WirdStats
)

@Serializable
enum class TasbeehDialogSize(val id: String, val titleAr: String) {
    COMPACT("compact", "مدمج"),
    MEDIUM("medium", "متوسط"),
    LARGE("large", "كبير");

    companion object {
        fun fromId(id: String?): TasbeehDialogSize {
            return values().find { it.id == id } ?: COMPACT
        }
    }
}

@Serializable
data class TasbeehState(
    val zekrId: Int = -1,
    val count: Int = 0,
    val target: Int = 33,
    val isFreeMode: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val category: String = "",
    val isCompleted: Boolean = false,
    val goalRecorded: Boolean = false
)

@Serializable
private data class SavedTasbeehSession(
    val count: Int,
    val target: Int,
    val free: Boolean,
    val undoable: Int,
    val completed: Boolean,
    val recorded: Boolean
)

data class TapProgressResult(
    val newRepCount: Int,
    val reachedTarget: Boolean,
    val isPinned: Boolean,
    val updatedStats: WirdStats
)

@Serializable
data class UserDataBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: WidgetSettings = WidgetSettings(),
    val favoriteIds: List<Int> = emptyList(),
    val pinnedItemId: Int? = null,
    val customItems: List<ZekrEntity> = emptyList(),
    val wirdStats: WirdStats = WirdStats(),
    val tasbeehDialogSize: String = "compact",
    val tasbeehState: TasbeehState = TasbeehState()
)

class WidgetPreferencesRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

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
        val favoritesOnlyMode = prefs[WidgetPreferenceKeys.KEY_FAVORITES_ONLY_MODE] ?: false
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
            favoritesOnlyMode = favoritesOnlyMode,
            smartTimeContextEnabled = smartTimeContextEnabled,
            serviceEnabled = serviceEnabled,
            styleRotationEnabled = styleRotationEnabled,
            styleRotationHours = styleRotationHours
        )
    }

    val currentItemIdFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] ?: 1
    }

    val favoriteIdsFlow: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        parseIdSet(prefs[WidgetPreferenceKeys.KEY_FAVORITE_IDS].orEmpty())
    }

    val pinnedItemIdFlow: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[WidgetPreferenceKeys.KEY_PINNED_ITEM_ID]?.takeIf { it > 0 }
    }

    val currentRepetitionFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] ?: 0).coerceAtLeast(0)
    }

    val customItemsFlow: Flow<List<ZekrEntity>> = context.dataStore.data.map { prefs ->
        decodeCustomItems(prefs[WidgetPreferenceKeys.KEY_CUSTOM_ITEMS_JSON].orEmpty())
    }

    val wirdStatsFlow: Flow<WirdStats> = context.dataStore.data.map { prefs ->
        decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
    }

    val tasbeehDialogSizeFlow: Flow<TasbeehDialogSize> = context.dataStore.data.map { prefs ->
        TasbeehDialogSize.fromId(prefs[WidgetPreferenceKeys.KEY_TASBEEH_DIALOG_SIZE])
    }

    val tasbeehCardOpacityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        (prefs[WidgetPreferenceKeys.KEY_TASBEEH_CARD_OPACITY] ?: 0.94f).coerceIn(0.75f, 1.0f)
    }

    val tasbeehUndoableCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] ?: 0).coerceAtLeast(0)
    }

    val notificationSettingsFlow: Flow<NotificationSettings> = context.dataStore.data.map { prefs ->
        NotificationSettings(
            salawatEnabled = prefs[WidgetPreferenceKeys.KEY_NOTIF_SALAWAT_ENABLED] ?: true,
            tasbeehEnabled = prefs[WidgetPreferenceKeys.KEY_NOTIF_TASBEEH_ENABLED] ?: true,
            wirdEnabled = prefs[WidgetPreferenceKeys.KEY_NOTIF_WIRD_ENABLED] ?: true,
            remindersDailyCount = (prefs[intPreferencesKey("pref_notif_reminders_daily_count")] ?: 3).coerceIn(1, 10),
            readingEnabled = prefs[booleanPreferencesKey("pref_notif_reading_enabled")] ?: false,
            readingQuranEnabled = prefs[booleanPreferencesKey("pref_notif_reading_quran_enabled")] ?: true,
            readingHadithEnabled = prefs[booleanPreferencesKey("pref_notif_reading_hadith_enabled")] ?: true,
            readingAthkarEnabled = prefs[booleanPreferencesKey("pref_notif_reading_athkar_enabled")] ?: true,
            readingDailyCount = (prefs[intPreferencesKey("pref_notif_reading_daily_count")] ?: 5).coerceIn(1, 20),
            lockScreenEnabled = prefs[booleanPreferencesKey("pref_notif_lockscreen_enabled")] ?: false,
            lockScreenIntervalMinutes = (prefs[intPreferencesKey("pref_notif_lockscreen_interval")] ?: 30).coerceIn(15, 180),
            lockScreenQuranEnabled = prefs[booleanPreferencesKey("pref_notif_lockscreen_quran")] ?: true,
            lockScreenHadithEnabled = prefs[booleanPreferencesKey("pref_notif_lockscreen_hadith")] ?: true,
            lockScreenAthkarEnabled = prefs[booleanPreferencesKey("pref_notif_lockscreen_athkar")] ?: true,
            dailyCount = (prefs[WidgetPreferenceKeys.KEY_NOTIF_COUNT_PER_DAY] ?: 3).coerceIn(1, 20),
            startHour = (prefs[WidgetPreferenceKeys.KEY_NOTIF_START_HOUR] ?: 8).coerceIn(0, 23),
            endHour = (prefs[WidgetPreferenceKeys.KEY_NOTIF_END_HOUR] ?: 22).coerceIn(0, 23),
            quietHoursEnabled = prefs[WidgetPreferenceKeys.KEY_NOTIF_QUIET_HOURS_ENABLED] ?: true,
            duaEnabled = prefs[WidgetPreferenceKeys.KEY_NOTIF_DUA_ENABLED] ?: false,
            contentEnabled = prefs[booleanPreferencesKey("pref_notif_content_enabled")] ?: false
        )
    }

    val mushafLastPageFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[intPreferencesKey("pref_mushaf_last_page")] ?: 1).coerceIn(1, 604)
    }

    suspend fun getMushafLastPage(): Int = mushafLastPageFlow.first()

    suspend fun saveMushafLastPage(page: Int) {
        val safePage = page.coerceIn(1, 604)
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey("pref_mushaf_last_page")] = safePage
        }
    }

    val mushafNightModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[booleanPreferencesKey("mushaf_night_mode")] ?: false
    }

    suspend fun saveMushafNightMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("mushaf_night_mode")] = enabled
        }
    }

    val tasbeehStateFlow: Flow<TasbeehState> = context.dataStore.data.map { prefs ->
        TasbeehState(
            zekrId = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] ?: -1,
            count = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] ?: 0,
            target = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_TARGET] ?: 33,
            isFreeMode = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_FREE_MODE] ?: false,
            vibrationEnabled = prefs[WidgetPreferenceKeys.KEY_TASBEEH_VIBRATION] ?: true,
            soundEnabled = prefs[WidgetPreferenceKeys.KEY_TASBEEH_SOUND] ?: true,
            category = prefs[WidgetPreferenceKeys.KEY_TASBEEH_CATEGORY] ?: "",
            isCompleted = prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] ?: false,
            goalRecorded = prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] ?: false
        )
    }

    val hasPromptedPermissionsFlow: kotlinx.coroutines.flow.Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WidgetPreferenceKeys.KEY_HAS_PROMPTED_PERMISSIONS] ?: false
    }

    suspend fun hasPromptedPermissions(): Boolean = hasPromptedPermissionsFlow.first()

    suspend fun setPromptedPermissions(prompted: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_HAS_PROMPTED_PERMISSIONS] = prompted
        }
    }

    suspend fun getTasbeehDialogSize(): TasbeehDialogSize = tasbeehDialogSizeFlow.first()

    suspend fun setTasbeehDialogSize(size: TasbeehDialogSize) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_DIALOG_SIZE] = size.id
        }
    }

    suspend fun getTasbeehCardOpacity(): Float = tasbeehCardOpacityFlow.first()

    suspend fun setTasbeehCardOpacity(opacity: Float) {
        val safe = opacity.coerceIn(0.75f, 1.0f)
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_CARD_OPACITY] = safe
        }
    }

    suspend fun getTasbeehCategory(): String =
        context.dataStore.data.first()[WidgetPreferenceKeys.KEY_TASBEEH_CATEGORY].orEmpty()

    suspend fun getNotificationSettings(): NotificationSettings = notificationSettingsFlow.first()

    suspend fun saveNotificationSettings(settings: NotificationSettings) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_NOTIF_SALAWAT_ENABLED] = settings.salawatEnabled
            prefs[WidgetPreferenceKeys.KEY_NOTIF_TASBEEH_ENABLED] = settings.tasbeehEnabled
            prefs[WidgetPreferenceKeys.KEY_NOTIF_WIRD_ENABLED] = settings.wirdEnabled
            prefs[intPreferencesKey("pref_notif_reminders_daily_count")] = settings.remindersDailyCount.coerceIn(1, 10)
            prefs[booleanPreferencesKey("pref_notif_reading_enabled")] = settings.readingEnabled
            prefs[booleanPreferencesKey("pref_notif_reading_quran_enabled")] = settings.readingQuranEnabled
            prefs[booleanPreferencesKey("pref_notif_reading_hadith_enabled")] = settings.readingHadithEnabled
            prefs[booleanPreferencesKey("pref_notif_reading_athkar_enabled")] = settings.readingAthkarEnabled
            prefs[intPreferencesKey("pref_notif_reading_daily_count")] = settings.readingDailyCount.coerceIn(1, 20)
            prefs[booleanPreferencesKey("pref_notif_lockscreen_enabled")] = settings.lockScreenEnabled
            prefs[intPreferencesKey("pref_notif_lockscreen_interval")] = settings.lockScreenIntervalMinutes.coerceIn(15, 180)
            prefs[booleanPreferencesKey("pref_notif_lockscreen_quran")] = settings.lockScreenQuranEnabled
            prefs[booleanPreferencesKey("pref_notif_lockscreen_hadith")] = settings.lockScreenHadithEnabled
            prefs[booleanPreferencesKey("pref_notif_lockscreen_athkar")] = settings.lockScreenAthkarEnabled
            prefs[WidgetPreferenceKeys.KEY_NOTIF_DUA_ENABLED] = settings.duaEnabled
            prefs[booleanPreferencesKey("pref_notif_content_enabled")] = settings.contentEnabled
            prefs[WidgetPreferenceKeys.KEY_NOTIF_COUNT_PER_DAY] = settings.dailyCount.coerceIn(1, 20)
            prefs[WidgetPreferenceKeys.KEY_NOTIF_START_HOUR] = settings.startHour.coerceIn(0, 23)
            prefs[WidgetPreferenceKeys.KEY_NOTIF_END_HOUR] = settings.endHour.coerceIn(0, 23)
            prefs[WidgetPreferenceKeys.KEY_NOTIF_QUIET_HOURS_ENABLED] = settings.quietHoursEnabled
        }
    }

    suspend fun recordCompletedReadingFromNotification(
        itemType: ContentType,
        today: java.time.LocalDate = java.time.LocalDate.now()
    ) {
        context.dataStore.edit { prefs ->
            val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
            val updated = stats.withRecordedCompletion(itemType, 1, today)
            prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun recordCompletedReadItem(
        itemType: ContentType,
        count: Int = 1,
        today: java.time.LocalDate = java.time.LocalDate.now()
    ) {
        context.dataStore.edit { prefs ->
            val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
            val updated = stats.withRecordedCompletion(itemType, count, today)
            prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun recordCompletedFromNotification(
        type: ReminderType,
        contentType: ContentType,
        notificationId: Int,
        today: java.time.LocalDate = java.time.LocalDate.now()
    ): Boolean {
        var recorded = false
        context.dataStore.edit { prefs ->
            val notifKey = stringPreferencesKey("pref_confirmed_notif_instance_$notificationId")
            if (notificationId > 0 && prefs[notifKey] != null) {
                return@edit
            }
            if (notificationId > 0) {
                prefs[notifKey] = today.toString()
            }
            when (type) {
                ReminderType.SALAWAT -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_SALAWAT_DATE] = today.toString()
                ReminderType.TASBEEH -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_TASBEEH_DATE] = today.toString()
                ReminderType.WIRD -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_WIRD_DATE] = today.toString()
                ReminderType.DUA -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_DUA_DATE] = today.toString()
                else -> prefs[stringPreferencesKey("pref_notif_confirmed_${type.id}_date")] = today.toString()
            }

            val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
            val updated = if (type == ReminderType.TASBEEH || type == ReminderType.SALAWAT) {
                val currentTasbeehCount = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] ?: 0
                prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = currentTasbeehCount + 1
                stats.withRecordedTap(contentType, completedItem = true, today = today)
            } else {
                stats.withRecordedCompletion(contentType, count = 1, today = today)
            }
            prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] = json.encodeToString(updated)
            recorded = true
        }
        return recorded
    }

    suspend fun recordMushafPagesRead(
        pagesCount: Int,
        today: java.time.LocalDate = java.time.LocalDate.now()
    ) {
        if (pagesCount <= 0) return
        context.dataStore.edit { prefs ->
            val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
            val updated = stats.withRecordedCompletion(ContentType.QURAN_AYAH, pagesCount, today)
            prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun confirmNotificationAccomplished(type: ReminderType, dateStr: String) {
        context.dataStore.edit { prefs ->
            when (type) {
                ReminderType.SALAWAT -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_SALAWAT_DATE] = dateStr
                ReminderType.TASBEEH -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_TASBEEH_DATE] = dateStr
                ReminderType.WIRD -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_WIRD_DATE] = dateStr
                ReminderType.DUA -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_DUA_DATE] = dateStr
                else -> prefs[stringPreferencesKey("pref_notif_confirmed_${type.id}_date")] = dateStr
            }
        }
    }

    suspend fun isNotificationConfirmedToday(type: ReminderType, dateStr: String): Boolean {
        val prefs = context.dataStore.data.first()
        val confirmed = when (type) {
            ReminderType.SALAWAT -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_SALAWAT_DATE]
            ReminderType.TASBEEH -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_TASBEEH_DATE]
            ReminderType.WIRD -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_WIRD_DATE]
            ReminderType.DUA -> prefs[WidgetPreferenceKeys.KEY_NOTIF_CONFIRMED_DUA_DATE]
            else -> prefs[stringPreferencesKey("pref_notif_confirmed_${type.id}_date")]
        }
        return confirmed == dateStr
    }

    suspend fun getTasbeehState(): TasbeehState = tasbeehStateFlow.first()

    suspend fun getSettings(): WidgetSettings = settingsFlow.first()

    suspend fun getSettingsForWidget(widgetId: Int): WidgetSettings {
        if (widgetId <= 0) return getSettings()
        val prefs = context.dataStore.data.first()
        val key = WidgetPreferenceKeys.widgetSettingsKey(widgetId)
        val raw = prefs[key]
        if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<WidgetSettings>(raw) }
                .getOrNull()?.let { return it }
        }
        return getSettings()
    }

    suspend fun saveSettingsForWidget(widgetId: Int, settings: WidgetSettings) {
        if (widgetId > 0) {
            context.dataStore.edit { prefs ->
                val key = WidgetPreferenceKeys.widgetSettingsKey(widgetId)
                prefs[key] = json.encodeToString(settings)
            }
        } else {
            updateSettings(settings)
        }
    }

    suspend fun removeSettingsForWidget(widgetId: Int) {
        if (widgetId > 0) {
            context.dataStore.edit { prefs ->
                val key = WidgetPreferenceKeys.widgetSettingsKey(widgetId)
                prefs.remove(key)
            }
        }
    }

    suspend fun getCurrentItemId(): Int = currentItemIdFlow.first()

    suspend fun getFavoriteIds(): Set<Int> = favoriteIdsFlow.first()

    suspend fun getPinnedItemId(): Int? = pinnedItemIdFlow.first()

    suspend fun getCurrentRepetitionCount(): Int = currentRepetitionFlow.first()

    suspend fun getCustomItems(): List<ZekrEntity> = customItemsFlow.first()

    suspend fun getWirdStats(): WirdStats = wirdStatsFlow.first()

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
            prefs[WidgetPreferenceKeys.KEY_FAVORITES_ONLY_MODE] = settings.favoritesOnlyMode
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
            prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] = 0
            prefs[WidgetPreferenceKeys.KEY_LAST_REP_TAP_MS] = 0L
        }
    }

    suspend fun saveShuffleProgress(nextItemId: Int, updatedSeenIds: Set<Int>) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] = nextItemId
            prefs[WidgetPreferenceKeys.KEY_SEEN_ITEM_IDS] = serializeIdSet(updatedSeenIds + nextItemId)
            prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] = 0
            prefs[WidgetPreferenceKeys.KEY_LAST_REP_TAP_MS] = 0L
        }
    }

    suspend fun toggleFavorite(itemId: Int): Boolean {
        var nowFavorite = false
        context.dataStore.edit { prefs ->
            val current = parseIdSet(prefs[WidgetPreferenceKeys.KEY_FAVORITE_IDS].orEmpty())
            val updated = if (itemId in current) {
                nowFavorite = false
                current - itemId
            } else {
                nowFavorite = true
                current + itemId
            }
            prefs[WidgetPreferenceKeys.KEY_FAVORITE_IDS] = serializeIdSet(updated)
        }
        return nowFavorite
    }

    suspend fun setPinnedItemId(itemId: Int?) {
        context.dataStore.edit { prefs ->
            if (itemId != null && itemId > 0) {
                prefs[WidgetPreferenceKeys.KEY_PINNED_ITEM_ID] = itemId
                prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] = itemId
                prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] = 0
                prefs[WidgetPreferenceKeys.KEY_LAST_REP_TAP_MS] = 0L
            } else {
                prefs.remove(WidgetPreferenceKeys.KEY_PINNED_ITEM_ID)
            }
        }
    }

    suspend fun upsertCustomItem(item: ZekrEntity): ZekrEntity {
        var savedItem = item
        context.dataStore.edit { prefs ->
            val existing = decodeCustomItems(prefs[WidgetPreferenceKeys.KEY_CUSTOM_ITEMS_JSON].orEmpty())
            val targetId = if (item.id >= CUSTOM_ID_START) {
                item.id
            } else {
                ((existing.maxOfOrNull { it.id } ?: (CUSTOM_ID_START - 1)) + 1)
                    .coerceAtLeast(CUSTOM_ID_START)
            }
            savedItem = item.copy(
                id = targetId,
                isCustom = true,
                repetition = item.repetition.coerceAtLeast(1)
            )
            val updated = existing.filterNot { it.id == targetId } + savedItem
            prefs[WidgetPreferenceKeys.KEY_CUSTOM_ITEMS_JSON] = json.encodeToString(updated)
        }
        return savedItem
    }

    suspend fun deleteCustomItem(itemId: Int) {
        context.dataStore.edit { prefs ->
            val existing = decodeCustomItems(prefs[WidgetPreferenceKeys.KEY_CUSTOM_ITEMS_JSON].orEmpty())
            val updated = existing.filterNot { it.id == itemId }
            prefs[WidgetPreferenceKeys.KEY_CUSTOM_ITEMS_JSON] = json.encodeToString(updated)
            val favs = parseIdSet(prefs[WidgetPreferenceKeys.KEY_FAVORITE_IDS].orEmpty()) - itemId
            prefs[WidgetPreferenceKeys.KEY_FAVORITE_IDS] = serializeIdSet(favs)
            if (prefs[WidgetPreferenceKeys.KEY_PINNED_ITEM_ID] == itemId) {
                prefs.remove(WidgetPreferenceKeys.KEY_PINNED_ITEM_ID)
            }
        }
    }

    suspend fun updateDailyGoal(goal: Int) {
        val safeGoal = goal.coerceIn(1, 500)
        context.dataStore.edit { prefs ->
            val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
            prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] =
                json.encodeToString(stats.copy(dailyGoal = safeGoal))
        }
    }

    /**
     * Records one tap on the active zekr body (widget or in-app live preview).
     * Increments tasbih taps, advances repetition count, and when target is reached
     * marks the zekr completed in daily stats.
     */
    suspend fun recordZekrTap(
        item: ZekrEntity,
        nowMs: Long = System.currentTimeMillis()
    ): TapProgressResult {
        var result = TapProgressResult(
            newRepCount = 0,
            reachedTarget = false,
            isPinned = false,
            updatedStats = WirdStats()
        )
        context.dataStore.edit { prefs ->
            val target = item.repetition.coerceAtLeast(1)
            val currentRep = (prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] ?: 0).coerceAtLeast(0)
            val nextRep = currentRep + 1
            val reachedTarget = nextRep >= target
            val pinnedId = prefs[WidgetPreferenceKeys.KEY_PINNED_ITEM_ID]?.takeIf { it > 0 }
            val isPinned = pinnedId != null && pinnedId == item.id

            val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
            val updatedStats = stats.withRecordedTap(
                itemType = item.type,
                completedItem = reachedTarget
            )
            prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] = json.encodeToString(updatedStats)

            val storedRep = if (reachedTarget) 0 else nextRep
            prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] = storedRep
            prefs[WidgetPreferenceKeys.KEY_LAST_REP_TAP_MS] = if (storedRep > 0) nowMs else 0L

            result = TapProgressResult(
                newRepCount = storedRep,
                reachedTarget = reachedTarget,
                isPinned = isPinned,
                updatedStats = updatedStats
            )
        }
        return result
    }

    private fun readSavedTasbeehSessions(prefs: Preferences): Map<Int, SavedTasbeehSession> =
        prefs[WidgetPreferenceKeys.KEY_TASBEEH_SAVED_SESSIONS]?.let { saved ->
            runCatching { json.decodeFromString<Map<Int, SavedTasbeehSession>>(saved) }.getOrNull()
        }.orEmpty()

    private fun archiveTasbeehSession(prefs: MutablePreferences, nextId: Int): Map<Int, SavedTasbeehSession> {
        val saved = readSavedTasbeehSessions(prefs)
        val currentId = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] ?: -1
        if (currentId <= 0 || currentId == nextId) return saved
        val session = SavedTasbeehSession(
            count = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] ?: 0,
            target = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_TARGET] ?: 33,
            free = prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_FREE_MODE] ?: false,
            undoable = prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] ?: 0,
            completed = prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] ?: false,
            recorded = prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] ?: false
        )
        val updated = saved + (currentId to session)
        prefs[WidgetPreferenceKeys.KEY_TASBEEH_SAVED_SESSIONS] = json.encodeToString(updated)
        return updated
    }

    suspend fun selectTasbeehItem(item: ZekrEntity): TasbeehState {
        context.dataStore.edit { prefs ->
            if (prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] == item.id) return@edit
            val saved = archiveTasbeehSession(prefs, item.id)[item.id]
            val target = saved?.target ?: item.getEffectiveDocumentedCount() ?: 33
            val widgetCount = if (prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] == item.id) {
                (prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] ?: 0).takeIf { it in 0 until target }
            } else null
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] = item.id
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = saved?.count ?: widgetCount ?: 0
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_TARGET] = target
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_FREE_MODE] = saved?.free ?: false
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] = saved?.undoable ?: 0
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_CATEGORY] = ZekrRepository.classifyDhikr(item).id
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = saved?.completed ?: false
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = saved?.recorded ?: false
        }
        return getTasbeehState()
    }

    suspend fun resetAllTasbeehSessions() {
        context.dataStore.edit { prefs ->
            prefs.remove(WidgetPreferenceKeys.KEY_TASBEEH_SAVED_SESSIONS)
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = 0
            prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] = 0
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] = 0
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = false
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = false
        }
    }

    suspend fun saveTasbeehProgress(
        zekrId: Int,
        count: Int,
        target: Int,
        isFreeMode: Boolean,
        category: String? = null,
        isCompleted: Boolean? = null,
        goalRecorded: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            archiveTasbeehSession(prefs, zekrId)
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] = zekrId
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = count
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_TARGET] = target
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_FREE_MODE] = isFreeMode
            if (category != null) {
                prefs[WidgetPreferenceKeys.KEY_TASBEEH_CATEGORY] = category
            }
            if (isCompleted != null) {
                prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = isCompleted
            }
            if (goalRecorded != null) {
                prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = goalRecorded
            }
            if (count == 0) {
                // If reset to 0, clear undoable taps
                prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] = 0
                if (isCompleted == null) {
                    prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = false
                }
                if (goalRecorded == null) {
                    prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = false
                }
            }
            val currentWidgetId = prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] ?: -1
            if (currentWidgetId == zekrId) {
                prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] = count
                prefs[WidgetPreferenceKeys.KEY_LAST_REP_TAP_MS] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setTasbeehPreferences(vibration: Boolean, sound: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_VIBRATION] = vibration
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_SOUND] = sound
        }
    }

    /**
     * Records one tap in the Tasbeeh counter (either from popup or full screen).
     * Increments daily WirdStats total tasbeeh taps, updates last tasbeeh progress,
     * tracks undoable taps, and syncs widget repetition if the item is currently displayed.
     *
     * Correct Progression Logic:
     * - Marks completedItem = true in WirdStats ONLY ONCE when reaching the target.
     * - Extra taps in the same session increment total taps without inflating completedItemsCount.
     */
    suspend fun recordTasbeehTap(
        item: ZekrEntity,
        newCount: Int,
        target: Int,
        isFreeMode: Boolean
    ): TapProgressResult {
        var result = TapProgressResult(
            newRepCount = newCount,
            reachedTarget = !isFreeMode && newCount >= target,
            isPinned = false,
            updatedStats = WirdStats()
        )
        context.dataStore.edit { prefs ->
            val reachedTarget = !isFreeMode && newCount >= target
            val alreadyRecordedGoal = prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] ?: false
            val isFirstTimeReachingTarget = reachedTarget && !alreadyRecordedGoal

            val pinnedId = prefs[WidgetPreferenceKeys.KEY_PINNED_ITEM_ID]?.takeIf { it > 0 }
            val isPinned = pinnedId != null && pinnedId == item.id

            val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
            val updatedStats = stats.withRecordedTap(
                itemType = item.type,
                completedItem = isFirstTimeReachingTarget
            )
            prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] = json.encodeToString(updatedStats)

            val currentUndoable = prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] ?: 0
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] = currentUndoable + 1

            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] = item.id
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = newCount
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_TARGET] = target
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_FREE_MODE] = isFreeMode

            if (reachedTarget) {
                prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = true
                prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = true
            }

            val currentWidgetId = prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] ?: -1
            if (currentWidgetId == item.id) {
                prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] = newCount
                prefs[WidgetPreferenceKeys.KEY_LAST_REP_TAP_MS] = System.currentTimeMillis()
            }

            result = TapProgressResult(
                newRepCount = newCount,
                reachedTarget = reachedTarget,
                isPinned = isPinned,
                updatedStats = updatedStats
            )
        }
        return result
    }

    /**
     * Undoes the last recorded tasbeeh tap:
     * - Decrements current zekr count and undoable count by 1.
     * - Decrements today's total tasbeeh taps in WirdStats by 1.
     * - If the goal was previously marked completed and count drops below target, revokes completion.
     * - Clamped at 0 (never negative).
     */
    suspend fun undoLastTasbeehTap(
        item: ZekrEntity,
        target: Int,
        isFreeMode: Boolean
    ): UndoResult {
        var result = UndoResult(newCount = 0, canUndoMore = false, updatedStats = WirdStats())
        context.dataStore.edit { prefs ->
            val currentCount = (prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] ?: 0).coerceAtLeast(0)
            val undoable = (prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] ?: 0).coerceAtLeast(0)
            val wasGoalRecorded = prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] ?: false

            if (currentCount > 0 && undoable > 0) {
                val newCount = currentCount - 1
                val newUndoable = undoable - 1
                val revokingCompletion = !isFreeMode && currentCount >= target && newCount < target && wasGoalRecorded

                val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
                val updatedStats = stats.withUndoneTap(
                    itemType = item.type,
                    uncompletedItem = revokingCompletion
                )
                prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] = json.encodeToString(updatedStats)

                prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = newCount
                prefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] = newUndoable

                if (revokingCompletion) {
                    prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = false
                    prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = false
                }

                val currentWidgetId = prefs[WidgetPreferenceKeys.KEY_CURRENT_ITEM_ID] ?: -1
                if (currentWidgetId == item.id) {
                    prefs[WidgetPreferenceKeys.KEY_CURRENT_REP_COUNT] = newCount
                    prefs[WidgetPreferenceKeys.KEY_LAST_REP_TAP_MS] = System.currentTimeMillis()
                }

                result = UndoResult(
                    newCount = newCount,
                    canUndoMore = newUndoable > 0 && newCount > 0,
                    updatedStats = updatedStats
                )
            } else {
                val stats = decodeWirdStats(prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON].orEmpty())
                result = UndoResult(
                    newCount = currentCount,
                    canUndoMore = false,
                    updatedStats = stats
                )
            }
        }
        return result
    }

    /**
     * Smart Tasbeeh Context Resolver:
     * 1. If [reminderCategory] is supplied (e.g. from Salawat or Tasbeeh notification):
     *    - If previous session in that category was ongoing (!isCompleted && count > 0), resume it.
     *    - If previous session was completed or from another category, pick next unseen item in that category deck with count = 0.
     *    - Never mix Salawat with generic Tasbeeh or general Duas.
     * 2. If [displayedItemId] has repetition > 1 or is tasbeeh/salawat (from widget):
     *    - If its saved session was completed, start fresh with count = 0.
     *    - Otherwise resume with saved count.
     * 3. If displayed item is an Ayah or non-tasbeeh:
     *    - Resume last active uncompleted tasbeeh, or advance to next authentic tasbeeh deck item with count = 0.
     */
    suspend fun resolveSmartTasbeehItem(
        context: Context,
        displayedItemId: Int = -1,
        reminderCategory: String? = null
    ): Triple<ZekrEntity, Int, Int> {
        val customItems = getCustomItems()
        val allItems = ZekrRepository.getAllItems(context, customItems)
        val state = getTasbeehState()
        val targetCategory = DhikrCategory.fromId(reminderCategory)

        // CASE 1: Opened from a specific category notification (Salawat, Tasbeeh, General)
        if (targetCategory != null && targetCategory != DhikrCategory.WIRD) {
            val lastCategory = DhikrCategory.fromId(state.category)
            val sameCategory = lastCategory == targetCategory
            val isLastSessionOngoing = sameCategory && !state.isCompleted && state.count > 0 && state.zekrId > 0

            if (isLastSessionOngoing) {
                val ongoingItem = allItems.find { it.id == state.zekrId }
                if (ongoingItem != null && ZekrRepository.classifyDhikr(ongoingItem) == targetCategory) {
                    val target = if (state.target > 0) state.target else (ongoingItem.getEffectiveDocumentedCount() ?: 33)
                    return Triple(ongoingItem, state.count, target)
                }
            }

            // Need next item from category pool
            val seenKey = when (targetCategory) {
                DhikrCategory.SALAWAT -> WidgetPreferenceKeys.KEY_TASBEEH_SEEN_SALAWAT_IDS
                DhikrCategory.TASBEEH -> WidgetPreferenceKeys.KEY_TASBEEH_SEEN_TASBEEH_IDS
                DhikrCategory.GENERAL -> WidgetPreferenceKeys.KEY_TASBEEH_SEEN_GENERAL_IDS
                DhikrCategory.WIRD -> WidgetPreferenceKeys.KEY_SEEN_ITEM_IDS
            }
            val prefs = context.dataStore.data.first()
            val seenIds = parseIdSet(prefs[seenKey].orEmpty())

            val nextResult = ZekrRepository.selectNextForCategory(
                allItems = allItems,
                category = targetCategory,
                seenIds = seenIds,
                currentId = if (sameCategory && state.zekrId > 0) state.zekrId else null
            )

            val target = nextResult.item.getEffectiveDocumentedCount() ?: if (targetCategory == DhikrCategory.TASBEEH) 100 else 33
            context.dataStore.edit { editPrefs ->
                archiveTasbeehSession(editPrefs, nextResult.item.id)
                editPrefs[seenKey] = serializeIdSet(nextResult.updatedSeenIds)
                editPrefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] = nextResult.item.id
                editPrefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = 0
                editPrefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_TARGET] = target
                editPrefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_FREE_MODE] = false
                editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_CATEGORY] = targetCategory.id
                editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = false
                editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = false
                editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] = 0
            }
            return Triple(nextResult.item, 0, target)
        }

        // Widget launch targets the displayed item and preserves its previous count and goal.
        val candidate = allItems.firstOrNull { it.id == displayedItemId }
        val isCandidateTasbeeh = candidate != null && candidate.type != ContentType.QURAN_AYAH &&
            (candidate.isCustom || candidate.type == ContentType.AUTHENTIC_ZEKR || candidate.tasbeehText != null ||
                candidate.repetition > 1 || candidate.category.contains("الصلاة على النبي"))

        if (isCandidateTasbeeh && candidate != null) {
            val selected = selectTasbeehItem(candidate)
            if (!selected.isCompleted) return Triple(candidate, selected.count, selected.target)
            saveTasbeehProgress(
                zekrId = candidate.id,
                count = 0,
                target = selected.target,
                isFreeMode = selected.isFreeMode,
                category = selected.category
            )
            return Triple(candidate, 0, selected.target)
        }

        // CASE 3: Candidate is Quran Ayah or no valid displayed item
        // If last session was NOT completed and count > 0, resume it!
        if (state.zekrId > 0 && !state.isCompleted && state.count > 0) {
            val lastItem = allItems.find { it.id == state.zekrId }
            if (lastItem != null && lastItem.type != ContentType.QURAN_AYAH) {
                val target = if (state.target > 0) state.target else (lastItem.getEffectiveDocumentedCount() ?: 33)
                return Triple(lastItem, state.count, target)
            }
        }

        // If last session was completed or empty, advance to next tasbeeh item fresh
        val tasbeehPool = allItems.filter {
            it.type != ContentType.QURAN_AYAH &&
                (it.repetition > 1 || it.category.contains("التسبيح") || it.tasbeehText != null)
        }.ifEmpty { allItems.filter { it.type != ContentType.QURAN_AYAH } }

        val prefs = context.dataStore.data.first()
        val seenTasbeeh = parseIdSet(prefs[WidgetPreferenceKeys.KEY_TASBEEH_SEEN_TASBEEH_IDS].orEmpty())
        val nextResult = ZekrRepository.selectNextForCategory(
            allItems = tasbeehPool,
            category = DhikrCategory.TASBEEH,
            seenIds = seenTasbeeh,
            currentId = if (state.zekrId > 0) state.zekrId else null
        )

        val target = nextResult.item.getEffectiveDocumentedCount() ?: 100
        context.dataStore.edit { editPrefs ->
            archiveTasbeehSession(editPrefs, nextResult.item.id)
            editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_SEEN_TASBEEH_IDS] = serializeIdSet(nextResult.updatedSeenIds)
            editPrefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] = nextResult.item.id
            editPrefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = 0
            editPrefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_TARGET] = target
            editPrefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_FREE_MODE] = false
            editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_CATEGORY] = DhikrCategory.TASBEEH.id
            editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = false
            editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = false
            editPrefs[WidgetPreferenceKeys.KEY_TASBEEH_UNDOABLE_COUNT] = 0
        }
        return Triple(nextResult.item, 0, target)
    }

    suspend fun shouldPauseAutoShuffleOnUnlock(nowMs: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.dataStore.data.first()
        val pinnedId = prefs[WidgetPreferenceKeys.KEY_PINNED_ITEM_ID]?.takeIf { it > 0 }
        return pinnedId != null
    }

    suspend fun exportUserDataJson(): String {
        val backup = UserDataBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            settings = getSettings(),
            favoriteIds = getFavoriteIds().toList(),
            pinnedItemId = getPinnedItemId(),
            customItems = getCustomItems(),
            wirdStats = getWirdStats(),
            tasbeehDialogSize = getTasbeehDialogSize().id,
            tasbeehState = getTasbeehState()
        )
        return json.encodeToString(backup)
    }

    suspend fun importUserDataJson(rawJson: String): Result<Unit> = runCatching {
        val backup = json.decodeFromString<UserDataBackup>(rawJson)
        context.dataStore.edit { prefs ->
            updateSettings(backup.settings)
            prefs[WidgetPreferenceKeys.KEY_FAVORITE_IDS] = serializeIdSet(backup.favoriteIds.toSet())
            if (backup.pinnedItemId != null && backup.pinnedItemId > 0) {
                prefs[WidgetPreferenceKeys.KEY_PINNED_ITEM_ID] = backup.pinnedItemId
            } else {
                prefs.remove(WidgetPreferenceKeys.KEY_PINNED_ITEM_ID)
            }
            prefs[WidgetPreferenceKeys.KEY_CUSTOM_ITEMS_JSON] = json.encodeToString(backup.customItems)
            prefs[WidgetPreferenceKeys.KEY_WIRD_STATS_JSON] = json.encodeToString(backup.wirdStats)
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_DIALOG_SIZE] = backup.tasbeehDialogSize

            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_ZEKR_ID] = backup.tasbeehState.zekrId
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_COUNT] = backup.tasbeehState.count
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_TARGET] = backup.tasbeehState.target
            prefs[WidgetPreferenceKeys.KEY_LAST_TASBEEH_FREE_MODE] = backup.tasbeehState.isFreeMode
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_VIBRATION] = backup.tasbeehState.vibrationEnabled
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_SOUND] = backup.tasbeehState.soundEnabled
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_CATEGORY] = backup.tasbeehState.category
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_SESSION_COMPLETED] = backup.tasbeehState.isCompleted
            prefs[WidgetPreferenceKeys.KEY_TASBEEH_GOAL_RECORDED] = backup.tasbeehState.goalRecorded
        }
    }

    private fun decodeCustomItems(raw: String): List<ZekrEntity> {
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<ZekrEntity>>(raw) }
            .getOrDefault(emptyList())
    }

    private fun decodeWirdStats(raw: String): WirdStats {
        if (raw.isBlank()) return WirdStats()
        return runCatching { json.decodeFromString<WirdStats>(raw) }
            .getOrDefault(WirdStats())
    }

    companion object {
        const val CUSTOM_ID_START = 10_000
        const val MID_PROGRESS_SHUFFLE_PAUSE_MS = 2L * 60L * 60L * 1000L // 2 hours

        internal fun shouldPauseUnlockShuffle(
            currentRepCount: Int,
            lastTapMs: Long,
            pinnedItemId: Int?,
            nowMs: Long
        ): Boolean {
            return (pinnedItemId != null && pinnedItemId > 0) ||
                (currentRepCount > 0 && (nowMs - lastTapMs) < MID_PROGRESS_SHUFFLE_PAUSE_MS)
        }

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

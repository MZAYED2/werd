package com.zekr.widget.routine.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ConditionLogic(val titleArabic: String) {
    AND("كل الشروط (AND)"),
    OR("أي شرط (OR)")
}

@Serializable
enum class BluetoothEventType(val titleArabic: String) {
    CONNECTED("عند الاتصال بجهاز"),
    CONNECTED_STATE("بقاء الجهاز متصلاً"),
    DISCONNECTED("عند الانفصال عن جهاز")
}

@Serializable
enum class TimeConditionType(val titleArabic: String) {
    EXACT_TIME("ساعة محددة"),
    TIME_WINDOW("فترة زمنية (من - إلى)")
}

@Serializable
enum class ChargingConditionType(val titleArabic: String) {
    CONNECTED("عند توصيل الشاحن"),
    DISCONNECTED("عند فصل الشاحن"),
    CHARGING_STATE("أثناء الشحن (حالة مستمرة)")
}

@Serializable
sealed class RoutineCondition {
    @Serializable
    data class Bluetooth(
        val deviceName: String? = null,
        val deviceMac: String? = null,
        val eventType: BluetoothEventType = BluetoothEventType.CONNECTED
    ) : RoutineCondition()

    @Serializable
    data class Time(
        val timeType: TimeConditionType = TimeConditionType.EXACT_TIME,
        val startHour: Int = 8,
        val startMinute: Int = 0,
        val endHour: Int = 11,
        val endMinute: Int = 0,
        val daysOfWeek: List<Int> = emptyList() // 1 = Monday ... 7 = Sunday, empty = all days
    ) : RoutineCondition()

    @Serializable
    data class Charging(
        val chargingType: ChargingConditionType = ChargingConditionType.CONNECTED
    ) : RoutineCondition()
}

@Serializable
sealed class RoutineAction {
    @Serializable
    data class AudioTrackAction(
        val trackId: String,
        val title: String,
        val subtitle: String
    ) : RoutineAction()

    @Serializable
    data class QuranSurahAction(
        val surahNumber: Int,
        val surahName: String,
        val reciterId: String
    ) : RoutineAction()

    @Serializable
    data class QuranResumeAction(
        val reciterId: String? = null
    ) : RoutineAction()

    @Serializable
    data class QuranStartFromAction(
        val startSurahNumber: Int,
        val startSurahName: String,
        val reciterId: String
    ) : RoutineAction()
}

@Serializable
data class RoutineSettings(
    val cooldownMinutes: Int = 15,
    val oncePerDay: Boolean = false,
    val allowSessionResume: Boolean = true,
    val sessionResumeTimeoutMinutes: Int = 45,
    val useFallbackNotification: Boolean = true
)

@Serializable
data class AudioRoutine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val logic: ConditionLogic = ConditionLogic.AND,
    val conditions: List<RoutineCondition> = emptyList(),
    val actions: List<RoutineAction> = emptyList(),
    val settings: RoutineSettings = RoutineSettings(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastExecutedAt: Long = 0L
)

@Serializable
data class RoutineSessionState(
    val routineId: String,
    val currentActionIndex: Int = 0,
    val completedActionIndices: List<Int> = emptyList(),
    val currentTrackId: String? = null,
    val playbackPositionMs: Long = 0L,
    val pausedAtEpochMs: Long = 0L,
    val isInterrupted: Boolean = false
)

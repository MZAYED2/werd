package com.zekr.widget.routine.data

import android.content.Context
import com.zekr.widget.audio.AudioCatalog
import com.zekr.widget.routine.model.AudioRoutine
import com.zekr.widget.routine.model.BluetoothEventType
import com.zekr.widget.routine.model.ConditionLogic
import com.zekr.widget.routine.model.RoutineAction
import com.zekr.widget.routine.model.RoutineCondition
import com.zekr.widget.routine.model.RoutineSessionState
import com.zekr.widget.routine.model.RoutineSettings
import com.zekr.widget.routine.model.TimeConditionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class RoutineRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("werd_audio_routines_v1", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _routinesFlow = MutableStateFlow<List<AudioRoutine>>(emptyList())
    val routinesFlow: StateFlow<List<AudioRoutine>> = _routinesFlow.asStateFlow()

    init {
        loadRoutines()
    }

    @Synchronized
    fun loadRoutines(): List<AudioRoutine> {
        val initialized = prefs.getBoolean(KEY_TEMPLATES_INITIALIZED, false)
        val rawJson = prefs.getString(KEY_ROUTINES_JSON, null)

        val list = if (rawJson.isNullOrBlank()) {
            if (!initialized) {
                // Routines start empty by default for new users
                prefs.edit()
                    .putBoolean(KEY_TEMPLATES_INITIALIZED, true)
                    .putString(KEY_ROUTINES_JSON, "[]")
                    .apply()
                emptyList()
            } else {
                emptyList()
            }
        } else {
            try {
                json.decodeFromString<List<AudioRoutine>>(rawJson)
            } catch (e: Exception) {
                emptyList()
            }
        }

        _routinesFlow.value = list
        return list
    }

    private fun persistRoutines(list: List<AudioRoutine>) {
        _routinesFlow.value = list
        scope.launch {
            try {
                prefs.edit()
                    .putString(KEY_ROUTINES_JSON, json.encodeToString(list))
                    .apply()
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }

    fun saveRoutine(routine: AudioRoutine) {
        val current = _routinesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == routine.id }
        if (index >= 0) {
            current[index] = routine
        } else {
            current.add(0, routine)
        }
        persistRoutines(current)
    }

    fun deleteRoutine(routineId: String) {
        val current = _routinesFlow.value.filterNot { it.id == routineId }
        persistRoutines(current)
    }

    fun duplicateRoutine(routineId: String) {
        val target = _routinesFlow.value.firstOrNull { it.id == routineId } ?: return
        val duplicate = target.copy(
            id = UUID.randomUUID().toString(),
            name = "${target.name} (نسخة)",
            createdAt = System.currentTimeMillis(),
            lastExecutedAt = 0L
        )
        val current = _routinesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == routineId }
        if (index >= 0) {
            current.add(index + 1, duplicate)
        } else {
            current.add(0, duplicate)
        }
        persistRoutines(current)
    }

    fun toggleRoutine(routineId: String, enabled: Boolean) {
        val current = _routinesFlow.value.map {
            if (it.id == routineId) it.copy(enabled = enabled) else it
        }
        persistRoutines(current)
    }

    fun updateLastExecuted(routineId: String, timestamp: Long) {
        val current = _routinesFlow.value.map {
            if (it.id == routineId) it.copy(lastExecutedAt = timestamp) else it
        }
        persistRoutines(current)
    }

    fun getRoutineById(id: String): AudioRoutine? {
        return _routinesFlow.value.firstOrNull { it.id == id }
    }

    // Session State for interruption & resume
    fun saveSessionState(sessionState: RoutineSessionState) {
        scope.launch {
            try {
                prefs.edit()
                    .putString(KEY_ACTIVE_SESSION, json.encodeToString(sessionState))
                    .apply()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun getSessionState(): RoutineSessionState? {
        val raw = prefs.getString(KEY_ACTIVE_SESSION, null) ?: return null
        return try {
            json.decodeFromString<RoutineSessionState>(raw)
        } catch (e: Exception) {
            null
        }
    }

    fun clearSessionState() {
        scope.launch {
            prefs.edit().remove(KEY_ACTIVE_SESSION).apply()
        }
    }

    /**
     * Initial 3 template routines created using the EXACT SAME data-driven schema.
     * All disabled by default so they do not start automatically without user consent.
     */
    private fun createInitialTemplates(): List<AudioRoutine> {
        val carRoutine = AudioRoutine(
            name = "🚗 روتين العربية",
            enabled = false,
            logic = ConditionLogic.AND,
            conditions = listOf(
                RoutineCondition.Bluetooth(
                    deviceName = null,
                    eventType = BluetoothEventType.CONNECTED
                )
            ),
            actions = listOf(
                RoutineAction.AudioTrackAction(
                    trackId = "dua_riding",
                    title = "دعاء ركوب السيارة أو الدابة",
                    subtitle = "سبحان الذي سخر لنا هذا وما كنا له مقرنين"
                ),
                RoutineAction.QuranResumeAction(
                    reciterId = AudioCatalog.QuranReciter.MINSHAWI.id
                )
            ),
            settings = RoutineSettings(
                cooldownMinutes = 30,
                oncePerDay = false,
                allowSessionResume = true,
                sessionResumeTimeoutMinutes = 45,
                useFallbackNotification = true
            )
        )

        val morningRoutine = AudioRoutine(
            name = "🌅 روتين أذكار الصباح",
            enabled = false,
            logic = ConditionLogic.AND,
            conditions = listOf(
                RoutineCondition.Time(
                    timeType = TimeConditionType.EXACT_TIME,
                    startHour = 6,
                    startMinute = 30,
                    daysOfWeek = emptyList() // All days
                )
            ),
            actions = listOf(
                RoutineAction.AudioTrackAction(
                    trackId = "athkar_morning_evening",
                    title = "أذكار الصباح والمساء",
                    subtitle = "حصن المسلم من أذكار الكتاب والسنة"
                )
            ),
            settings = RoutineSettings(
                cooldownMinutes = 720,
                oncePerDay = true,
                allowSessionResume = false,
                useFallbackNotification = true
            )
        )

        val eveningRoutine = AudioRoutine(
            name = "🌙 روتين أذكار المساء",
            enabled = false,
            logic = ConditionLogic.AND,
            conditions = listOf(
                RoutineCondition.Time(
                    timeType = TimeConditionType.EXACT_TIME,
                    startHour = 17,
                    startMinute = 30,
                    daysOfWeek = emptyList() // All days
                )
            ),
            actions = listOf(
                RoutineAction.AudioTrackAction(
                    trackId = "athkar_morning_evening",
                    title = "أذكار الصباح والمساء",
                    subtitle = "حصن المسلم من أذكار الكتاب والسنة"
                )
            ),
            settings = RoutineSettings(
                cooldownMinutes = 720,
                oncePerDay = true,
                allowSessionResume = false,
                useFallbackNotification = true
            )
        )

        return listOf(carRoutine, morningRoutine, eveningRoutine)
    }

    companion object {
        private const val KEY_ROUTINES_JSON = "key_routines_json"
        private const val KEY_TEMPLATES_INITIALIZED = "key_templates_initialized"
        private const val KEY_ACTIVE_SESSION = "key_active_session"

        @Volatile
        private var instance: RoutineRepository? = null

        fun getInstance(context: Context): RoutineRepository {
            return instance ?: synchronized(this) {
                instance ?: RoutineRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

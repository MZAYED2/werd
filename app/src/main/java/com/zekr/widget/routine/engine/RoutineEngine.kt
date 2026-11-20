package com.zekr.widget.routine.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zekr.widget.MainActivity
import com.zekr.widget.R
import com.zekr.widget.audio.AudioCatalog
import com.zekr.widget.audio.AudioTrack
import com.zekr.widget.audio.WerdAudioPlayerManager
import com.zekr.widget.routine.data.RoutineRepository
import com.zekr.widget.routine.model.AudioRoutine
import com.zekr.widget.routine.model.BluetoothEventType
import com.zekr.widget.routine.model.ChargingConditionType
import com.zekr.widget.routine.model.ConditionLogic
import com.zekr.widget.routine.model.RoutineAction
import com.zekr.widget.routine.model.RoutineCondition
import com.zekr.widget.routine.model.RoutineSessionState
import com.zekr.widget.routine.model.TimeConditionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class RoutineEngineState(
    val activeRoutine: AudioRoutine? = null,
    val currentActionIndex: Int = 0,
    val isExecuting: Boolean = false,
    val statusMessage: String? = null
)

object RoutineEngine {

    private const val NOTIFICATION_CHANNEL_ID = "werd_routine_fallback_channel"
    private const val NOTIFICATION_ID = 8842

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appContext: Context? = null
    private var repository: RoutineRepository? = null

    private val _engineState = MutableStateFlow(RoutineEngineState())
    val engineState: StateFlow<RoutineEngineState> = _engineState.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        repository = RoutineRepository.getInstance(app)

        WerdAudioPlayerManager.init(app)
        setupTrackEndedListener()
        createNotificationChannel(app)
    }

    private fun setupTrackEndedListener() {
        WerdAudioPlayerManager.onTrackEndedListener = {
            val state = _engineState.value
            val active = state.activeRoutine
            if (active != null && state.isExecuting) {
                onCurrentActionEnded(active, state.currentActionIndex)
                true // Handled by RoutineEngine
            } else {
                false // Normal default playback
            }
        }
    }

    private fun onCurrentActionEnded(routine: AudioRoutine, actionIndex: Int) {
        val nextIndex = actionIndex + 1
        if (nextIndex < routine.actions.size) {
            _engineState.value = _engineState.value.copy(currentActionIndex = nextIndex)
            saveCurrentSessionProgress(routine, nextIndex, 0L, isInterrupted = false)
            executeAction(routine, nextIndex, 0L)
        } else {
            // All actions completed
            finishRoutineExecution(routine)
        }
    }

    fun triggerRoutineManually(routine: AudioRoutine) {
        startRoutineExecution(routine, isManualTrigger = true)
    }

    fun onBluetoothEvent(
        eventType: BluetoothEventType,
        deviceName: String?,
        deviceMac: String?
    ) {
        val context = appContext ?: return
        val repo = repository ?: return

        // 1. If a routine is active and Bluetooth disconnects, handle session pause
        if (eventType == BluetoothEventType.DISCONNECTED) {
            val currentState = _engineState.value
            val active = currentState.activeRoutine
            if (active != null && currentState.isExecuting) {
                val hasBtCondition = active.conditions.any { it is RoutineCondition.Bluetooth }
                if (hasBtCondition && active.settings.allowSessionResume) {
                    pauseRoutineOnDisconnect(active, currentState.currentActionIndex)
                    return
                }
            }
        }

        // 2. Evaluate all enabled routines for trigger
        val enabledRoutines = repo.routinesFlow.value.filter { it.enabled }
        val now = System.currentTimeMillis()

        for (routine in enabledRoutines) {
            val canRun = evaluateRoutineConditions(
                routine = routine,
                incomingBtEvent = eventType,
                incomingBtDeviceName = deviceName,
                incomingBtMac = deviceMac,
                isManual = false
            )

            if (canRun && isEligibleForExecution(routine, now)) {
                startRoutineExecution(routine, isManualTrigger = false)
                break // Execute one matching routine at a time to prevent conflicts
            }
        }
    }

    fun onPowerEvent(chargingType: ChargingConditionType) {
        val repo = repository ?: return
        val enabledRoutines = repo.routinesFlow.value.filter { it.enabled }
        val now = System.currentTimeMillis()

        for (routine in enabledRoutines) {
            val canRun = evaluateRoutineConditions(
                routine = routine,
                incomingPowerType = chargingType,
                isManual = false
            )

            if (canRun && isEligibleForExecution(routine, now)) {
                startRoutineExecution(routine, isManualTrigger = false)
                break
            }
        }
    }

    fun onTimeTick() {
        val repo = repository ?: return
        val enabledRoutines = repo.routinesFlow.value.filter { it.enabled }
        val now = System.currentTimeMillis()

        for (routine in enabledRoutines) {
            val canRun = evaluateRoutineConditions(
                routine = routine,
                isTimeTrigger = true,
                isManual = false
            )

            if (canRun && isEligibleForExecution(routine, now)) {
                startRoutineExecution(routine, isManualTrigger = false)
                break
            }
        }
    }

    /**
     * Evaluates whether a routine's conditions are satisfied according to its logic (AND / OR).
     */
    fun evaluateRoutineConditions(
        routine: AudioRoutine,
        incomingBtEvent: BluetoothEventType? = null,
        incomingBtDeviceName: String? = null,
        incomingBtMac: String? = null,
        incomingPowerType: ChargingConditionType? = null,
        isTimeTrigger: Boolean = false,
        isManual: Boolean = false,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        if (isManual) return true
        if (routine.conditions.isEmpty()) return false

        val conditionResults = routine.conditions.map { condition ->
            when (condition) {
                is RoutineCondition.Bluetooth -> {
                    evaluateBluetoothCondition(
                        condition,
                        incomingBtEvent,
                        incomingBtDeviceName,
                        incomingBtMac
                    )
                }
                is RoutineCondition.Time -> {
                    evaluateTimeCondition(condition, calendar, isTimeTrigger)
                }
                is RoutineCondition.Charging -> {
                    evaluateChargingCondition(condition, incomingPowerType)
                }
            }
        }

        return if (routine.logic == ConditionLogic.AND) {
            conditionResults.all { it }
        } else {
            conditionResults.any { it }
        }
    }

    private fun evaluateBluetoothCondition(
        condition: RoutineCondition.Bluetooth,
        incomingEvent: BluetoothEventType?,
        incomingName: String?,
        incomingMac: String?
    ): Boolean {
        if (incomingEvent == null) return false
        if (condition.eventType != incomingEvent) return false

        // If target device is specified, verify name/mac match
        if (!condition.deviceName.isNullOrBlank()) {
            val matches = incomingName?.equals(condition.deviceName, ignoreCase = true) == true ||
                          (!condition.deviceMac.isNullOrBlank() && incomingMac.equals(condition.deviceMac, ignoreCase = true))
            return matches
        }
        return true
    }

    private fun evaluateTimeCondition(
        condition: RoutineCondition.Time,
        calendar: Calendar,
        isTimeTrigger: Boolean
    ): Boolean {
        // Day of week check: Calendar.MONDAY = 2, Calendar.SUNDAY = 1
        // Convert to ISO 1 = Monday ... 7 = Sunday
        val calDay = calendar.get(Calendar.DAY_OF_WEEK)
        val isoDay = when (calDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        if (condition.daysOfWeek.isNotEmpty() && !condition.daysOfWeek.contains(isoDay)) {
            return false
        }

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentMinutesOfDay = currentHour * 60 + currentMinute

        return when (condition.timeType) {
            TimeConditionType.EXACT_TIME -> {
                val targetMinutesOfDay = condition.startHour * 60 + condition.startMinute
                // Match within 1 minute threshold
                Math.abs(currentMinutesOfDay - targetMinutesOfDay) <= 1
            }
            TimeConditionType.TIME_WINDOW -> {
                val startMinutes = condition.startHour * 60 + condition.startMinute
                val endMinutes = condition.endHour * 60 + condition.endMinute
                if (startMinutes <= endMinutes) {
                    currentMinutesOfDay in startMinutes..endMinutes
                } else {
                    // Over midnight window
                    currentMinutesOfDay >= startMinutes || currentMinutesOfDay <= endMinutes
                }
            }
        }
    }

    private fun evaluateChargingCondition(
        condition: RoutineCondition.Charging,
        incomingType: ChargingConditionType?
    ): Boolean {
        if (incomingType != null) {
            return condition.chargingType == incomingType ||
                   (condition.chargingType == ChargingConditionType.CHARGING_STATE && incomingType == ChargingConditionType.CONNECTED)
        }
        // Check current battery status from system
        val context = appContext ?: return false
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        return when (condition.chargingType) {
            ChargingConditionType.CONNECTED -> false
            ChargingConditionType.DISCONNECTED -> false
            ChargingConditionType.CHARGING_STATE -> isCharging
        }
    }

    fun isEligibleForExecution(routine: AudioRoutine, currentTimeMs: Long): Boolean {
        // Cooldown check
        val cooldownMs = routine.settings.cooldownMinutes * 60 * 1000L
        if (routine.lastExecutedAt > 0L && (currentTimeMs - routine.lastExecutedAt) < cooldownMs) {
            return false
        }

        // Once per day check
        if (routine.settings.oncePerDay && routine.lastExecutedAt > 0L) {
            val calLast = Calendar.getInstance().apply { timeInMillis = routine.lastExecutedAt }
            val calNow = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
            val isSameDay = calLast.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                            calLast.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
            if (isSameDay) return false
        }

        return true
    }

    private fun startRoutineExecution(routine: AudioRoutine, isManualTrigger: Boolean) {
        val repo = repository ?: return
        val now = System.currentTimeMillis()

        // Check if there is an interrupted session to resume
        val savedSession = repo.getSessionState()
        val canResume = routine.settings.allowSessionResume &&
                savedSession != null &&
                savedSession.routineId == routine.id &&
                savedSession.isInterrupted &&
                (now - savedSession.pausedAtEpochMs) <= routine.settings.sessionResumeTimeoutMinutes * 60 * 1000L

        val startIndex = if (canResume) {
            savedSession!!.currentActionIndex.coerceIn(0, (routine.actions.size - 1).coerceAtLeast(0))
        } else {
            0
        }

        val startPositionMs = if (canResume) savedSession!!.playbackPositionMs else 0L

        _engineState.value = RoutineEngineState(
            activeRoutine = routine,
            currentActionIndex = startIndex,
            isExecuting = true,
            statusMessage = "جارٍ تشغيل ${routine.name}..."
        )

        // Cancel any fallback notification
        dismissFallbackNotification()

        saveCurrentSessionProgress(routine, startIndex, startPositionMs, isInterrupted = false)

        try {
            executeAction(routine, startIndex, startPositionMs)
        } catch (e: Exception) {
            if (routine.settings.useFallbackNotification) {
                showFallbackNotification(routine)
            }
        }
    }

    private fun executeAction(routine: AudioRoutine, actionIndex: Int, startPositionMs: Long) {
        if (actionIndex >= routine.actions.size) {
            finishRoutineExecution(routine)
            return
        }

        val action = routine.actions[actionIndex]
        val trackToPlay: AudioTrack? = when (action) {
            is RoutineAction.AudioTrackAction -> {
                AudioCatalog.findTrackById(action.trackId)
            }
            is RoutineAction.QuranSurahAction -> {
                val reciter = AudioCatalog.QuranReciter.entries.firstOrNull { it.id == action.reciterId }
                    ?: AudioCatalog.QuranReciter.MINSHAWI
                AudioCatalog.getQuranTracks(reciter).firstOrNull { it.surahNumber == action.surahNumber }
            }
            is RoutineAction.QuranResumeAction -> {
                val reciter = AudioCatalog.QuranReciter.entries.firstOrNull { it.id == action.reciterId }
                    ?: WerdAudioPlayerManager.state.value.selectedReciter
                // Find last played Quran track or default to Surah 1
                val lastTrack = WerdAudioPlayerManager.state.value.currentTrack
                if (lastTrack?.surahNumber != null) {
                    lastTrack
                } else {
                    AudioCatalog.getQuranTracks(reciter).firstOrNull()
                }
            }
            is RoutineAction.QuranStartFromAction -> {
                val reciter = AudioCatalog.QuranReciter.entries.firstOrNull { it.id == action.reciterId }
                    ?: AudioCatalog.QuranReciter.MINSHAWI
                AudioCatalog.getQuranTracks(reciter).firstOrNull { it.surahNumber == action.startSurahNumber }
            }
        }

        if (trackToPlay != null) {
            WerdAudioPlayerManager.playTrackAt(trackToPlay, startPositionMs)
        } else {
            // Track not found, skip to next action
            onCurrentActionEnded(routine, actionIndex)
        }
    }

    private fun pauseRoutineOnDisconnect(routine: AudioRoutine, actionIndex: Int) {
        val currentPosition = WerdAudioPlayerManager.state.value.currentPositionMs
        WerdAudioPlayerManager.togglePlayPause() // Pause playback

        saveCurrentSessionProgress(
            routine = routine,
            actionIndex = actionIndex,
            positionMs = currentPosition,
            isInterrupted = true
        )

        _engineState.value = _engineState.value.copy(
            isExecuting = false,
            statusMessage = "تم إيقاف الروتين مؤقتاً بسبب انقطاع البلوتوث (يمكن الاستكمال)"
        )
    }

    private fun saveCurrentSessionProgress(
        routine: AudioRoutine,
        actionIndex: Int,
        positionMs: Long,
        isInterrupted: Boolean
    ) {
        val completed = (0 until actionIndex).toList()
        val sessionState = RoutineSessionState(
            routineId = routine.id,
            currentActionIndex = actionIndex,
            completedActionIndices = completed,
            currentTrackId = WerdAudioPlayerManager.state.value.currentTrack?.id,
            playbackPositionMs = positionMs,
            pausedAtEpochMs = System.currentTimeMillis(),
            isInterrupted = isInterrupted
        )
        repository?.saveSessionState(sessionState)
    }

    private fun finishRoutineExecution(routine: AudioRoutine) {
        val now = System.currentTimeMillis()
        repository?.updateLastExecuted(routine.id, now)
        repository?.clearSessionState()

        _engineState.value = RoutineEngineState(
            activeRoutine = null,
            currentActionIndex = 0,
            isExecuting = false,
            statusMessage = "اكتمل الروتين بنجاح: ${routine.name}"
        )
    }

    fun stopActiveRoutine() {
        WerdAudioPlayerManager.stopPlayback()
        repository?.clearSessionState()
        _engineState.value = RoutineEngineState(
            activeRoutine = null,
            currentActionIndex = 0,
            isExecuting = false,
            statusMessage = null
        )
        dismissFallbackNotification()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "روتينات وِرد الصوتية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات تشغيل واستكمال الروتينات الصوتية"
                setShowBadge(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun showFallbackNotification(routine: AudioRoutine) {
        val context = appContext ?: return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_werd_logo)
            .setContentTitle("وِرد — روتين جاهز: ${routine.name}")
            .setContentText("اضغط لتشغيل تسلسل الروتين الصوتي الآن")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .addAction(
                android.R.drawable.ic_media_play,
                "تشغيل الروتين",
                pendingOpen
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, notification)
    }

    private fun dismissFallbackNotification() {
        val context = appContext ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
    }
}

package com.zekr.widget.routine

import com.zekr.widget.routine.engine.RoutineEngine
import com.zekr.widget.routine.model.AudioRoutine
import com.zekr.widget.routine.model.BluetoothEventType
import com.zekr.widget.routine.model.ChargingConditionType
import com.zekr.widget.routine.model.ConditionLogic
import com.zekr.widget.routine.model.RoutineAction
import com.zekr.widget.routine.model.RoutineCondition
import com.zekr.widget.routine.model.RoutineSessionState
import com.zekr.widget.routine.model.RoutineSettings
import com.zekr.widget.routine.model.RoutineSummary
import com.zekr.widget.routine.model.TimeConditionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RoutineEngineTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testCreateAndSerializeRoutine() {
        val routine = AudioRoutine(
            name = "روتين اختباري",
            enabled = true,
            logic = ConditionLogic.AND,
            conditions = listOf(
                RoutineCondition.Bluetooth(deviceName = "CarBt", eventType = BluetoothEventType.CONNECTED),
                RoutineCondition.Charging(chargingType = ChargingConditionType.CONNECTED)
            ),
            actions = listOf(
                RoutineAction.AudioTrackAction(trackId = "dua_travel", title = "دعاء السفر", subtitle = "سفر"),
                RoutineAction.QuranResumeAction(reciterId = "minshawi")
            ),
            settings = RoutineSettings(cooldownMinutes = 20, oncePerDay = false)
        )

        val serialized = json.encodeToString(routine)
        val deserialized = json.decodeFromString<AudioRoutine>(serialized)

        assertEquals("روتين اختباري", deserialized.name)
        assertEquals(ConditionLogic.AND, deserialized.logic)
        assertEquals(2, deserialized.conditions.size)
        assertEquals(2, deserialized.actions.size)
        assertEquals(20, deserialized.settings.cooldownMinutes)
    }

    @Test
    fun testSingleConditionEvaluation_Bluetooth() {
        val routine = AudioRoutine(
            name = "بلوتوث السيارة",
            enabled = true,
            logic = ConditionLogic.AND,
            conditions = listOf(
                RoutineCondition.Bluetooth(deviceName = "MyCar", eventType = BluetoothEventType.CONNECTED)
            )
        )

        // Matching device and event
        val matches = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = BluetoothEventType.CONNECTED,
            incomingBtDeviceName = "MyCar"
        )
        assertTrue("Matching device and event should evaluate to true", matches)

        // Non-matching device
        val wrongDevice = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = BluetoothEventType.CONNECTED,
            incomingBtDeviceName = "Headphones"
        )
        assertFalse("Wrong device should evaluate to false", wrongDevice)

        // Wrong event (e.g. Disconnected instead of Connected)
        val wrongEvent = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = BluetoothEventType.DISCONNECTED,
            incomingBtDeviceName = "MyCar"
        )
        assertFalse("Wrong event should evaluate to false", wrongEvent)
    }

    @Test
    fun testMultipleConditions_AND_BluetoothAndCharging() {
        val routine = AudioRoutine(
            name = "شحن وبلوتوث",
            enabled = true,
            logic = ConditionLogic.AND,
            conditions = listOf(
                RoutineCondition.Bluetooth(deviceName = "Car", eventType = BluetoothEventType.CONNECTED),
                RoutineCondition.Charging(chargingType = ChargingConditionType.CONNECTED)
            )
        )

        // When only Bluetooth occurs but charging is not satisfied
        val onlyBt = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = BluetoothEventType.CONNECTED,
            incomingBtDeviceName = "Car",
            incomingPowerType = null
        )
        assertFalse("AND requires both conditions satisfied", onlyBt)

        // When both conditions are provided and satisfied
        val both = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = BluetoothEventType.CONNECTED,
            incomingBtDeviceName = "Car",
            incomingPowerType = ChargingConditionType.CONNECTED
        )
        assertTrue("Both conditions satisfied in AND should evaluate to true", both)
    }

    @Test
    fun testMultipleConditions_OR_BluetoothOrCharging() {
        val routine = AudioRoutine(
            name = "أي شرط",
            enabled = true,
            logic = ConditionLogic.OR,
            conditions = listOf(
                RoutineCondition.Bluetooth(deviceName = "Car", eventType = BluetoothEventType.CONNECTED),
                RoutineCondition.Charging(chargingType = ChargingConditionType.CONNECTED)
            )
        )

        // Only Bluetooth satisfied
        val btOnly = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = BluetoothEventType.CONNECTED,
            incomingBtDeviceName = "Car",
            incomingPowerType = null
        )
        assertTrue("OR requires only one condition to match", btOnly)

        // Only Charging satisfied
        val chargingOnly = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = null,
            incomingPowerType = ChargingConditionType.CONNECTED
        )
        assertTrue("OR requires only one condition to match", chargingOnly)
    }

    /**
     * Requirement: A complex 4th routine completely different from templates:
     * Bluetooth connected AND time between 8 and 11 AM -> Morning Athkar -> Travel Dua -> Quran Resume.
     * Evaluated purely via the general engine without any custom/hardcoded logic!
     */
    @Test
    fun testCustomComplexRoutine_BluetoothAndTimeWindow_MorningAthkarTravelDuaQuranResume() {
        val routine = AudioRoutine(
            name = "روتين الصباح بالسيارة",
            enabled = true,
            logic = ConditionLogic.AND,
            conditions = listOf(
                RoutineCondition.Bluetooth(deviceName = "Car_BT", eventType = BluetoothEventType.CONNECTED),
                RoutineCondition.Time(
                    timeType = TimeConditionType.TIME_WINDOW,
                    startHour = 8,
                    startMinute = 0,
                    endHour = 11,
                    endMinute = 0,
                    daysOfWeek = emptyList() // All days
                )
            ),
            actions = listOf(
                RoutineAction.AudioTrackAction(trackId = "athkar_morning_evening", title = "أذكار الصباح", subtitle = "حصن المسلم"),
                RoutineAction.AudioTrackAction(trackId = "dua_travel", title = "دعاء السفر", subtitle = "دعاء الركوب والسفر"),
                RoutineAction.QuranResumeAction(reciterId = "minshawi")
            ),
            settings = RoutineSettings(cooldownMinutes = 60, allowSessionResume = true, sessionResumeTimeoutMinutes = 45)
        )

        // Simulate 09:30 AM (Inside 8:00 - 11:00 window)
        val calInside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 30)
        }

        val matchInside = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = BluetoothEventType.CONNECTED,
            incomingBtDeviceName = "Car_BT",
            calendar = calInside
        )
        assertTrue("Should evaluate to true when inside 8:00-11:00 window and BT connected", matchInside)

        // Simulate 12:15 PM (Outside window)
        val calOutside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 15)
        }

        val matchOutside = RoutineEngine.evaluateRoutineConditions(
            routine = routine,
            incomingBtEvent = BluetoothEventType.CONNECTED,
            incomingBtDeviceName = "Car_BT",
            calendar = calOutside
        )
        assertFalse("Should evaluate to false when outside time window", matchOutside)

        // Verify summary text generation
        val summary = RoutineSummary.generateSummary(routine)
        assertTrue(summary.contains("Car_BT"))
        assertTrue(summary.contains("دعاء السفر"))
        assertTrue(summary.contains("استكمال القرآن"))
    }

    @Test
    fun testCooldownLogic() {
        val routine = AudioRoutine(
            name = "اختبار Cooldown",
            enabled = true,
            settings = RoutineSettings(cooldownMinutes = 30),
            lastExecutedAt = 1000000L
        )

        // 10 minutes later (Within 30m cooldown) -> Not eligible
        val ineligible = RoutineEngine.isEligibleForExecution(routine, currentTimeMs = 1000000L + (10 * 60 * 1000L))
        assertFalse("Should not be eligible during cooldown period", ineligible)

        // 35 minutes later (Past 30m cooldown) -> Eligible
        val eligible = RoutineEngine.isEligibleForExecution(routine, currentTimeMs = 1000000L + (35 * 60 * 1000L))
        assertTrue("Should be eligible after cooldown elapsed", eligible)
    }

    @Test
    fun testOncePerDayLogic() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 20)
            set(Calendar.HOUR_OF_DAY, 8)
        }
        val executedAt = cal.timeInMillis

        val routine = AudioRoutine(
            name = "مرة يومياً",
            enabled = true,
            settings = RoutineSettings(oncePerDay = true),
            lastExecutedAt = executedAt
        )

        // Same day, 5 hours later
        cal.set(Calendar.HOUR_OF_DAY, 13)
        val sameDayLater = RoutineEngine.isEligibleForExecution(routine, currentTimeMs = cal.timeInMillis)
        assertFalse("Should not trigger twice on the same day when oncePerDay is true", sameDayLater)

        // Next day, 8:00 AM
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val nextDay = RoutineEngine.isEligibleForExecution(routine, currentTimeMs = cal.timeInMillis)
        assertTrue("Should trigger on the next day", nextDay)
    }

    @Test
    fun testSessionResumeStateTracking() {
        val session = RoutineSessionState(
            routineId = "routine_123",
            currentActionIndex = 1,
            completedActionIndices = listOf(0),
            currentTrackId = "dua_travel",
            playbackPositionMs = 42000L,
            pausedAtEpochMs = 1000000L,
            isInterrupted = true
        )

        val serialized = json.encodeToString(session)
        val deserialized = json.decodeFromString<RoutineSessionState>(serialized)

        assertEquals("routine_123", deserialized.routineId)
        assertEquals(1, deserialized.currentActionIndex)
        assertEquals(42000L, deserialized.playbackPositionMs)
        assertTrue(deserialized.isInterrupted)
    }

    @Test
    fun testRoutineSummaryArabicFormatting() {
        val routine = AudioRoutine(
            name = "روتين العربية",
            logic = ConditionLogic.AND,
            conditions = listOf(
                RoutineCondition.Bluetooth(deviceName = "سيارتي", eventType = BluetoothEventType.CONNECTED)
            ),
            actions = listOf(
                RoutineAction.AudioTrackAction(trackId = "dua_travel", title = "دعاء السفر", subtitle = "حصن المسلم"),
                RoutineAction.QuranResumeAction(reciterId = "minshawi")
            )
        )

        val summary = RoutineSummary.generateSummary(routine)
        assertTrue("Summary must describe conditions", summary.contains("بلوتوث «سيارتي»"))
        assertTrue("Summary must describe actions in order", summary.contains("1. دعاء السفر ➔ 2. استكمال القرآن من آخر موضع"))
    }
}

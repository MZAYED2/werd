package com.zekr.widget.notification

import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.preferences.NotificationSettings
import com.zekr.widget.data.preferences.ReminderType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefRepo = WidgetPreferencesRepository(applicationContext)
            val settings = prefRepo.notificationSettingsFlow.first()

            // 1. Respect Do Not Disturb (DND) without requesting bypass
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val filter = manager.currentInterruptionFilter
            if (filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            ) {
                // Device is in Do Not Disturb mode (Priority only, Alarms only, or Total silence)
                return Result.success()
            }

            // 2. Respect configured Quiet Hours
            if (settings.quietHoursEnabled) {
                val nowTime = LocalTime.now()
                val start = LocalTime.of(settings.startHour.coerceIn(0, 23), 0)
                val end = LocalTime.of(settings.endHour.coerceIn(0, 23), 0)
                val inQuiet = if (start.isBefore(end)) {
                    nowTime >= start && nowTime < end
                } else {
                    nowTime >= start || nowTime < end
                }
                if (inQuiet) return Result.success()
            }

            val snoozedId = inputData.getString(NotificationHelper.EXTRA_REMINDER_TYPE)
            val todayStr = LocalDate.now().toString()

            val candidates = mutableListOf<ReminderType>()

            // Follow-up reminders
            if (settings.salawatEnabled) candidates.add(ReminderType.SALAWAT)
            if (settings.tasbeehEnabled) candidates.add(ReminderType.TASBEEH)
            if (settings.wirdEnabled) candidates.add(ReminderType.WIRD)

            // Direct reading notifications
            if (settings.readingEnabled || settings.contentEnabled) {
                if (settings.readingQuranEnabled) candidates.add(ReminderType.VERSE)
                if (settings.readingHadithEnabled) candidates.add(ReminderType.HADITH)
                if (settings.readingAthkarEnabled) candidates.add(ReminderType.DHIKR)
                if (settings.duaEnabled) candidates.add(ReminderType.DUA)
            }

            val filtered = candidates.filter { type ->
                !prefRepo.isNotificationConfirmedToday(type, todayStr) &&
                    if (snoozedId != null) type.id == snoozedId
                    else !NotificationScheduler.isSnoozed(applicationContext, type)
            }

            val chosen = filtered.randomOrNull()
            if (chosen != null) {
                val notifId = NotificationHelper.reminderId(chosen)
                if (manager.activeNotifications.none { it.id == notifId }) {
                    NotificationHelper.showReminder(applicationContext, chosen)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

/**
 * WERD-071: Rotating silent notification for continuous Athkar on Lock Screen.
 */
class LockScreenAthkarWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefRepo = WidgetPreferencesRepository(applicationContext)
            val settings = prefRepo.notificationSettingsFlow.first()

            if (!settings.lockScreenEnabled) {
                NotificationHelper.cancelLockScreenAthkar(applicationContext)
                return Result.success()
            }

            // Respect DND
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val filter = manager.currentInterruptionFilter
            if (filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            ) {
                return Result.success()
            }

            // Respect Quiet Hours
            if (settings.quietHoursEnabled) {
                val nowTime = LocalTime.now()
                val start = LocalTime.of(settings.startHour.coerceIn(0, 23), 0)
                val end = LocalTime.of(settings.endHour.coerceIn(0, 23), 0)
                val inQuiet = if (start.isBefore(end)) {
                    nowTime >= start && nowTime < end
                } else {
                    nowTime >= start || nowTime < end
                }
                if (inQuiet) return Result.success()
            }

            val customItems = prefRepo.getCustomItems()
            val allItems = ZekrRepository.getAllItems(applicationContext, customItems)

            val enabledTypes = mutableSetOf<ContentType>()
            if (settings.lockScreenQuranEnabled) enabledTypes.add(ContentType.QURAN_AYAH)
            if (settings.lockScreenHadithEnabled) enabledTypes.add(ContentType.SAHIH_HADITH)
            if (settings.lockScreenAthkarEnabled) {
                enabledTypes.add(ContentType.AUTHENTIC_ZEKR)
            }

            val eligible = allItems.filter { it.type in enabledTypes }
            val chosen = eligible.randomOrNull() ?: allItems.firstOrNull()

            if (chosen != null) {
                NotificationHelper.showLockScreenAthkar(applicationContext, chosen)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

object NotificationScheduler {
    const val UNIQUE_REMINDERS_WORK = "werd_smart_reminders_periodic"
    const val UNIQUE_LOCKSCREEN_WORK = "werd_lockscreen_athkar_periodic"

    fun sync(context: Context, settings: NotificationSettings) {
        val appContext = context.applicationContext
        val anyReminder = settings.salawatEnabled || settings.tasbeehEnabled || settings.wirdEnabled ||
            settings.readingEnabled || settings.contentEnabled || settings.duaEnabled

        if (!anyReminder) {
            cancel(appContext)
        } else {
            val totalDaily = if (settings.readingEnabled) {
                settings.readingDailyCount.coerceIn(1, 20)
            } else {
                settings.dailyCount.coerceIn(1, 20)
            }

            val intervalMinutes = when {
                totalDaily <= 1 -> 720L
                totalDaily == 2 -> 360L
                totalDaily == 3 -> 240L
                totalDaily == 4 -> 180L
                totalDaily in 5..6 -> 120L
                totalDaily in 7..10 -> 60L
                totalDaily in 11..15 -> 45L
                else -> 30L
            }

            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                UNIQUE_REMINDERS_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        syncLockScreen(context, settings)
    }

    fun syncLockScreen(context: Context, settings: NotificationSettings) {
        val appContext = context.applicationContext
        if (!settings.lockScreenEnabled) {
            WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_LOCKSCREEN_WORK)
            NotificationHelper.cancelLockScreenAthkar(appContext)
            return
        }

        val intervalMinutes = settings.lockScreenIntervalMinutes.toLong().coerceIn(15L, 180L)
        val periodicRequest = PeriodicWorkRequestBuilder<LockScreenAthkarWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_LOCKSCREEN_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )

        // Trigger immediate one-time display so the lock screen is active right away
        val immediateRequest = OneTimeWorkRequestBuilder<LockScreenAthkarWorker>().build()
        WorkManager.getInstance(appContext).enqueue(immediateRequest)
    }

    fun snooze(context: Context, type: ReminderType) {
        snoozePreferences(context).edit().putLong(type.id, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30)).apply()
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(workDataOf(NotificationHelper.EXTRA_REMINDER_TYPE to type.id))
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            snoozeWorkName(type),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelSnooze(context: Context, type: ReminderType) {
        snoozePreferences(context).edit().remove(type.id).apply()
        WorkManager.getInstance(context).cancelUniqueWork(snoozeWorkName(type))
    }

    fun isSnoozed(context: Context, type: ReminderType): Boolean =
        snoozePreferences(context).getLong(type.id, 0L) > System.currentTimeMillis()

    private fun snoozePreferences(context: Context) =
        context.getSharedPreferences("werd_reminder_snoozes", Context.MODE_PRIVATE)

    private fun snoozeWorkName(type: ReminderType) = "werd_reminder_snooze_${type.id}"

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_REMINDERS_WORK)
        ReminderType.entries.forEach { cancelSnooze(context, it) }
    }
}

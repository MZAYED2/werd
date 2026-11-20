package com.zekr.widget.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zekr.widget.data.preferences.NotificationSettings
import com.zekr.widget.data.preferences.ReminderType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private fun NotificationSettings.isEnabled(type: ReminderType): Boolean = when (type) {
    ReminderType.SALAWAT -> salawatEnabled
    ReminderType.TASBEEH -> tasbeehEnabled
    ReminderType.WIRD -> wirdEnabled
    ReminderType.DUA -> duaEnabled
    ReminderType.VERSE, ReminderType.HADITH, ReminderType.DHIKR -> contentEnabled
}

class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefRepo = WidgetPreferencesRepository(applicationContext)
            val settings = prefRepo.notificationSettingsFlow.first()

            val snoozedId = inputData.getString(NotificationHelper.EXTRA_REMINDER_TYPE)
            val now = java.time.LocalDateTime.now()
            if (settings.quietHoursEnabled && (now.hour >= 23 || now.hour < 7)) {
                return Result.success()
            }
            val day = now.toLocalDate().toString()
            val candidates = ReminderType.entries.filter { type ->
                settings.isEnabled(type) &&
                    !prefRepo.isNotificationConfirmedToday(type, day) &&
                    if (snoozedId != null) type.id == snoozedId
                    else !NotificationScheduler.isSnoozed(applicationContext, type)
            }
            val chosen = candidates.randomOrNull()
            if (chosen != null) {
                val manager = applicationContext.getSystemService(android.app.NotificationManager::class.java)
                val id = NotificationHelper.reminderId(chosen)
                if (manager.activeNotifications.none { it.id == id }) {
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

object NotificationScheduler {
    const val UNIQUE_WORK_NAME = "werd_smart_reminders_periodic"

    fun sync(context: Context, settings: NotificationSettings) {
        val appContext = context.applicationContext
        val anyEnabled = ReminderType.entries.any(settings::isEnabled)
        if (!anyEnabled) {
            cancel(appContext)
            return
        }

        val intervalMinutes = when {
            settings.dailyCount <= 1 -> 720L
            settings.dailyCount == 2 -> 360L
            settings.dailyCount == 3 -> 240L
            settings.dailyCount == 4 -> 180L
            settings.dailyCount in 5..6 -> 120L
            settings.dailyCount in 7..10 -> 60L
            settings.dailyCount in 11..15 -> 45L
            else -> 30L
        }

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
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
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        ReminderType.entries.forEach { cancelSnooze(context, it) }
    }
}

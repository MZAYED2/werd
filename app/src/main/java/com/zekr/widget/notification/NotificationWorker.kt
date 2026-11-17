package com.zekr.widget.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zekr.widget.data.preferences.NotificationSettings
import com.zekr.widget.data.preferences.ReminderType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefRepo = WidgetPreferencesRepository(applicationContext)
            val settings = prefRepo.notificationSettingsFlow.first()

            // If none of the reminders are enabled, finish work
            if (!settings.salawatEnabled && !settings.tasbeehEnabled && !settings.wirdEnabled) {
                return Result.success()
            }

            // Check quiet hours (11:00 PM to 07:00 AM)
            if (settings.quietHoursEnabled) {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (currentHour >= 23 || currentHour < 7) {
                    return Result.success()
                }
            }

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val candidateReminders = mutableListOf<ReminderType>()

            if (settings.salawatEnabled && !prefRepo.isNotificationConfirmedToday(ReminderType.SALAWAT, todayStr)) {
                candidateReminders.add(ReminderType.SALAWAT)
            }
            if (settings.tasbeehEnabled && !prefRepo.isNotificationConfirmedToday(ReminderType.TASBEEH, todayStr)) {
                candidateReminders.add(ReminderType.TASBEEH)
            }
            if (settings.wirdEnabled && !prefRepo.isNotificationConfirmedToday(ReminderType.WIRD, todayStr)) {
                candidateReminders.add(ReminderType.WIRD)
            }

            if (candidateReminders.isNotEmpty()) {
                // Pick one reminder to show for this interval
                val chosen = candidateReminders.shuffled().first()
                NotificationHelper.showReminder(applicationContext, chosen)
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
        val anyEnabled = settings.salawatEnabled || settings.tasbeehEnabled || settings.wirdEnabled
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

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

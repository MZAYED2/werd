package com.zekr.widget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.zekr.widget.data.preferences.ReminderType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != NotificationHelper.ACTION_CONFIRM_REMINDER &&
            action != NotificationHelper.ACTION_SNOOZE_REMINDER
        ) return

        val typeId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_TYPE)
        val type = ReminderType.entries.firstOrNull { it.id == typeId } ?: return
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        if (notificationId > 0) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        val appContext = context.applicationContext
        if (action == NotificationHelper.ACTION_SNOOZE_REMINDER) {
            NotificationScheduler.snooze(appContext, type)
            return
        }

        NotificationScheduler.cancelSnooze(appContext, type)
        val date = LocalDate.now().toString()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetPreferencesRepository(appContext).confirmNotificationAccomplished(type, date)
            } finally {
                pending.finish()
            }
        }
    }
}

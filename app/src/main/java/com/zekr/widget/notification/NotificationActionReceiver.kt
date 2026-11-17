package com.zekr.widget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.zekr.widget.data.preferences.ReminderType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == NotificationHelper.ACTION_CONFIRM_REMINDER) {
            val typeId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_TYPE) ?: return
            val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
            val type = ReminderType.values().find { it.id == typeId } ?: return

            // Dismiss the notification immediately
            if (notifId > 0) {
                NotificationManagerCompat.from(context).cancel(notifId)
            }

            val appContext = context.applicationContext
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            CoroutineScope(Dispatchers.IO).launch {
                val prefRepo = WidgetPreferencesRepository(appContext)
                prefRepo.confirmNotificationAccomplished(type, todayStr)
            }

            Toast.makeText(
                context,
                "تقبّل الله طاعتكم وأثابكم خيراً ✓",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

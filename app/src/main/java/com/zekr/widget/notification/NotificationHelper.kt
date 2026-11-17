package com.zekr.widget.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zekr.widget.MainActivity
import com.zekr.widget.R
import com.zekr.widget.data.preferences.ReminderType
import com.zekr.widget.ui.tasbeeh.TasbeehDialogActivity

object NotificationHelper {
    const val CHANNEL_ID = "werd_smart_reminders"
    const val ACTION_CONFIRM_REMINDER = "com.zekr.widget.action.CONFIRM_REMINDER"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تذكيرات ورد الذكية"
            val descriptionText = "تذكيرات دورية بالصلاة على النبي ﷺ والتسبيح والورد القرآني"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminder(context: Context, type: ReminderType) {
        createNotificationChannel(context)

        val notifId = when (type) {
            ReminderType.SALAWAT -> 2001
            ReminderType.TASBEEH -> 2002
            ReminderType.WIRD -> 2003
        }

        val (title, text) = when (type) {
            ReminderType.SALAWAT -> Pair(
                "الصلاة على النبي ﷺ",
                "هل صليت على النبي اليوم؟ اللهم صلّ وسلم وبارك على نبينا محمد"
            )
            ReminderType.TASBEEH -> Pair(
                "تذكير التسبيح",
                "هل سبّحت اليوم؟ سبحان الله وبحمده، ألا بذكر الله تطمئن القلوب"
            )
            ReminderType.WIRD -> Pair(
                "الورد القرآني",
                "هل قرأت وردك اليوم؟ رتّل وارتقِ بآيات الذكر الحكيم"
            )
        }

        // Tap content -> Open App or Tasbeeh
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "تم ✓" (Accomplished for today, dismisses without fake count)
        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CONFIRM_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, type.id)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 100,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "فتح السبحة 📿" (Opens Tasbeeh popup)
        val tasbeehIntent = Intent(context, TasbeehDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tasbeehPendingIntent = PendingIntent.getActivity(
            context,
            notifId + 200,
            tasbeehIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "تم ✓", confirmPendingIntent)
            .addAction(0, "فتح السبحة 📿", tasbeehPendingIntent)

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }
}

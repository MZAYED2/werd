package com.zekr.widget.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.provider.Settings
import com.zekr.widget.service.FloatingTasbeehService
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.R
import com.zekr.widget.data.preferences.ReminderType

object NotificationHelper {
    const val CHANNEL_ID = "werd_smart_reminders"
    const val ACTION_SNOOZE_REMINDER = "com.zekr.widget.action.SNOOZE_REMINDER"
    const val ACTION_CONFIRM_REMINDER = "com.zekr.widget.action.CONFIRM_REMINDER"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_CONTENT_ID = "extra_content_id"
    const val EXTRA_CONTENT_KIND = "extra_content_kind"

    fun contentId(type: ReminderType): Int = when (type) {
        ReminderType.SALAWAT -> 12
        ReminderType.TASBEEH -> 10
        ReminderType.DUA -> 3
        ReminderType.VERSE -> 2
        ReminderType.HADITH -> 8
        ReminderType.DHIKR -> 131
        ReminderType.WIRD -> 0
    }

    fun reminderId(type: ReminderType): Int = when (type) {
        ReminderType.SALAWAT -> 2001
        ReminderType.TASBEEH -> 2002
        ReminderType.WIRD -> 2003
        ReminderType.DUA -> 2004
        ReminderType.VERSE -> 2005
        ReminderType.HADITH -> 2006
        ReminderType.DHIKR -> 2007
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تذكيرات ورد الذكية"
            val descriptionText = "تذكيرات دورية بالصلاة على النبي ﷺ والتسبيح والورد القرآني"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminder(context: Context, type: ReminderType) {
        createNotificationChannel(context)

        val notifId = reminderId(type)
        val contentId = contentId(type)
        val item = if (contentId > 0) ZekrRepository.getById(context, contentId, emptyList()) else null
        val tasbeeh = type == ReminderType.TASBEEH || type == ReminderType.SALAWAT
        val text = when {
            type == ReminderType.WIRD -> "حان موعد وردك اليومي المنظم. افتح المصحف وأكمل من موضعك المحفوظ."
            item == null -> "ابدأ قراءة وردك أو أكمل من موضعك المحفوظ."
            tasbeeh -> item.getEffectiveTasbeehText()
            else -> item.arabicText
        }
        val openLabel = when (type) {
            ReminderType.SALAWAT -> "افتح السبحة"
            ReminderType.TASBEEH -> "سبّح الآن"
            ReminderType.WIRD -> "افتح الورد اليومي"
            ReminderType.DUA -> "اقرأ الدعاء"
            ReminderType.VERSE -> "تدبر الآية"
            ReminderType.HADITH -> "اقرأ الحديث"
            ReminderType.DHIKR -> "اقرأ الذكر"
        }
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

        val snoozeIntent = Intent(confirmIntent).setAction(ACTION_SNOOZE_REMINDER)
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 300,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = Intent(context, ReminderOpenActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_REMINDER_TYPE, type.id)
            .putExtra(EXTRA_CONTENT_KIND, type.id)
            .putExtra(EXTRA_CONTENT_ID, contentId)
        val actionIntent = PendingIntent.getActivity(context, notifId, open, pendingFlags)

        // Using PendingIntent.getActivity collapses the notification shade instantly on tap (WERD-069)
        // ReminderOpenActivity launches FloatingTasbeehService directly without opening MainActivity
        val bodyIntent = actionIntent
        val openActionIntent = actionIntent

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_werd_reminder)
            .setContentTitle(type.titleArabic)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setContentIntent(bodyIntent)
            .addAction(0, openLabel, openActionIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(0, "تم", confirmPendingIntent)
            .addAction(0, "ذكرني لاحقاً", snoozePendingIntent)



        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }
}

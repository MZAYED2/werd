package com.zekr.widget.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zekr.widget.R
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.ReminderType
import com.zekr.widget.data.repository.ZekrRepository

object NotificationHelper {
    const val CHANNEL_READING = "werd_reading_notifications_v2"
    const val CHANNEL_REMINDERS = "werd_followup_reminders_v2"
    const val CHANNEL_LOCKSCREEN = "werd_lockscreen_athkar_v2"

    const val LOCKSCREEN_NOTIFICATION_ID = 3001

    const val ACTION_SNOOZE_REMINDER = "com.zekr.widget.action.SNOOZE_REMINDER"
    const val ACTION_CONFIRM_REMINDER = "com.zekr.widget.action.CONFIRM_REMINDER"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_CONTENT_ID = "extra_content_id"
    const val EXTRA_CONTENT_KIND = "extra_content_kind"

    fun isDirectReading(type: ReminderType): Boolean = when (type) {
        ReminderType.VERSE, ReminderType.HADITH, ReminderType.DHIKR, ReminderType.DUA -> true
        ReminderType.SALAWAT, ReminderType.TASBEEH, ReminderType.WIRD -> false
    }

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

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Remove legacy channels that might retain old sound settings
            runCatching { manager.deleteNotificationChannel("werd_smart_reminders") }
            runCatching { manager.deleteNotificationChannel("werd_reading_notifications_v1") }

            // 1. Direct Reading Channel (Verses, Hadiths, Athkar)
            val readingChannel = NotificationChannel(
                CHANNEL_READING,
                "قراءات وِرد (آيات وأحاديث وأذكار)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات قراءة الآيات والأحاديث والأذكار اليومية المباشرة"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setBypassDnd(false)
                }
            }
            manager.createNotificationChannel(readingChannel)

            // 2. Follow-up Reminders Channel
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "تذكيرات وِرد الذكية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تذكيرات دورية بالصلاة على النبي ﷺ والتسبيح والورد القرآني"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setBypassDnd(false)
                }
            }
            manager.createNotificationChannel(remindersChannel)

            // 3. Lock Screen Continuous Athkar Channel (WERD-071)
            val lockscreenChannel = NotificationChannel(
                CHANNEL_LOCKSCREEN,
                "أذكار شاشة القفل",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "عرض أذكار وآيات مختارة على شاشة القفل بشكل صامت ومستمر"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setBypassDnd(false)
                }
            }
            manager.createNotificationChannel(lockscreenChannel)
        }
    }

    fun showReminder(context: Context, type: ReminderType, customItem: ZekrEntity? = null) {
        createNotificationChannels(context)

        val notifId = reminderId(type)
        val contentId = customItem?.id ?: contentId(type)
        val item = customItem ?: if (contentId > 0) ZekrRepository.getById(context, contentId, emptyList()) else null
        val tasbeeh = type == ReminderType.TASBEEH || type == ReminderType.SALAWAT
        val text = when {
            type == ReminderType.WIRD -> "حان موعد وردك القرآني. افتح المصحف الشريف وأكمل من موضعك المحفوظ."
            item == null -> "ابدأ قراءة وردك أو أكمل من موضعك المحفوظ."
            tasbeeh -> item.getEffectiveTasbeehText()
            else -> item.arabicText
        }
        val openLabel = when (type) {
            ReminderType.SALAWAT -> "افتح السبحة"
            ReminderType.TASBEEH -> "سبّح الآن"
            ReminderType.WIRD -> "افتح المصحف"
            ReminderType.DUA -> "اقرأ الدعاء"
            ReminderType.VERSE -> "تدبر الآية"
            ReminderType.HADITH -> "اقرأ الحديث"
            ReminderType.DHIKR -> "اقرأ الذكر"
        }

        val itemTypeName = item?.type?.name ?: if (type == ReminderType.WIRD) ContentType.QURAN_AYAH.name else ""

        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CONFIRM_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, type.id)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
            putExtra(EXTRA_CONTENT_ID, contentId)
            putExtra(EXTRA_CONTENT_KIND, itemTypeName)
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

        val channelId = if (isDirectReading(type)) CHANNEL_READING else CHANNEL_REMINDERS

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_werd_reminder)
            .setContentTitle(type.titleArabic)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setContentIntent(actionIntent)
            .addAction(0, openLabel, actionIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(0, "تم", confirmPendingIntent)
            .addAction(0, "ذكرني لاحقاً", snoozePendingIntent)

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    /**
     * WERD-071: Continuous Silent Athkar notification for Lock Screen.
     * Purely read-only, no buttons, no contentIntent, no completion counting on display.
     */
    fun showLockScreenAthkar(context: Context, item: ZekrEntity) {
        createNotificationChannels(context)

        val title = when (item.type) {
            ContentType.QURAN_AYAH -> "وِرد · آية وتدبر"
            ContentType.SAHIH_HADITH -> "وِرد · حديث شريف"
            ContentType.AUTHENTIC_ZEKR -> "وِرد · ذكر مبارك"
        }
        val text = item.arabicText
        val subtext = item.source.takeIf { it.isNotBlank() } ?: item.category

        val builder = NotificationCompat.Builder(context, CHANNEL_LOCKSCREEN)
            .setSmallIcon(R.drawable.ic_werd_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(subtext)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(true)
            .setSound(null)
            .setVibrate(null)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(null)

        runCatching {
            NotificationManagerCompat.from(context).notify(LOCKSCREEN_NOTIFICATION_ID, builder.build())
        }
    }

    fun cancelLockScreenAthkar(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(LOCKSCREEN_NOTIFICATION_ID)
        }
    }
}

package com.zekr.widget.notification

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zekr.widget.MainActivity
import com.zekr.widget.data.preferences.ReminderType
import com.zekr.widget.service.FloatingTasbeehService
import com.zekr.widget.ui.tasbeeh.TasbeehDialogActivity
import com.zekr.widget.widget.WidgetMenuActivity

class ReminderOpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = ReminderType.entries.firstOrNull {
            it.id == intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_TYPE)
        }
        if (type != null) open(type)
        finish()
    }

    private fun open(type: ReminderType) {
        val id = intent.getIntExtra(NotificationHelper.EXTRA_CONTENT_ID, NotificationHelper.contentId(type))
        val notificationId = NotificationHelper.reminderId(type)
        val target = when (type) {
            ReminderType.WIRD -> Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_NAV_TAB, MainActivity.NAV_MUSHAF)
            ReminderType.SALAWAT, ReminderType.TASBEEH -> {
                if (Settings.canDrawOverlays(this)) {
                    val overlay = Intent(this, FloatingTasbeehService::class.java)
                        .putExtra(FloatingTasbeehService.EXTRA_ITEM_ID, id)
                        .putExtra(FloatingTasbeehService.EXTRA_DISMISS_NOTIFICATION, notificationId)
                    if (runCatching { ContextCompat.startForegroundService(this, overlay) }.isSuccess) {
                        NotificationManagerCompat.from(this).cancel(notificationId)
                        return
                    }
                }
                Toast.makeText(
                    this,
                    "السبحة العائمة تحتاج إذن الظهور فوق التطبيقات. يمكنك تفعيله من خيارات السبحة.",
                    Toast.LENGTH_LONG
                ).show()
                Intent(this, TasbeehDialogActivity::class.java)
                    .putExtra(TasbeehDialogActivity.EXTRA_ZEKR_ID, id)
            }
            ReminderType.DUA, ReminderType.VERSE, ReminderType.HADITH, ReminderType.DHIKR ->
                Intent(this, WidgetMenuActivity::class.java)
                    .putExtra(WidgetMenuActivity.EXTRA_ITEM_ID, id)
                    .putExtra(WidgetMenuActivity.EXTRA_SHOW_DETAILS, true)
        }
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(target)
        NotificationManagerCompat.from(this).cancel(notificationId)
    }
}

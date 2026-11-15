package com.zekr.widget.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.widget.ZekrGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenLockReceiver : BroadcastReceiver() {

    companion object {
        private var lastTriggerTime = 0L
        private const val DEBOUNCE_MS = 800L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_SCREEN_ON &&
            action != Intent.ACTION_SCREEN_OFF &&
            action != Intent.ACTION_USER_PRESENT
        ) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < DEBOUNCE_MS) {
            return
        }
        lastTriggerTime = now

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefRepo = WidgetPreferencesRepository(appContext)
                val settings = prefRepo.getSettings()
                if (settings.serviceEnabled) {
                    ZekrGlanceWidget.shuffleToNext(appContext)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

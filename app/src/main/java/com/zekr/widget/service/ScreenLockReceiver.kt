package com.zekr.widget.service

import android.app.KeyguardManager
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
        internal const val DEBOUNCE_MS = 3_000L

        internal fun shouldTriggerOnAction(action: String?, isKeyguardLocked: Boolean): Boolean {
            return when (action) {
                Intent.ACTION_USER_PRESENT -> true
                Intent.ACTION_SCREEN_ON -> !isKeyguardLocked
                else -> false
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isKeyguardLocked = keyguardManager?.isKeyguardLocked ?: true

        if (!shouldTriggerOnAction(action, isKeyguardLocked)) {
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
                if (settings.serviceEnabled && !prefRepo.shouldPauseAutoShuffleOnUnlock()) {
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

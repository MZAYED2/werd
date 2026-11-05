package com.zekr.widget.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                val prefRepo = WidgetPreferencesRepository(appContext)
                val settings = prefRepo.getSettings()
                if (settings.serviceEnabled) {
                    ScreenLockService.start(appContext)
                }
            }
        }
    }
}

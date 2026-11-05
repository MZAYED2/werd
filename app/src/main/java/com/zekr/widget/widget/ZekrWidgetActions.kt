package com.zekr.widget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository

class ShuffleWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val prefRepo = WidgetPreferencesRepository(appContext)
        val settings = prefRepo.getSettings()
        val currentId = prefRepo.getCurrentItemId()

        val nextItem = ZekrRepository.getRandomItem(
            context = appContext,
            includeQuran = settings.includeQuran,
            includeHadith = settings.includeHadith,
            includeAzkar = settings.includeAzkar,
            currentId = currentId
        )

        prefRepo.setCurrentItemId(nextItem.id)

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[KEY_ZEKR_ID] = nextItem.id
        }
        ZekrGlanceWidget().update(context, glanceId)
    }
}

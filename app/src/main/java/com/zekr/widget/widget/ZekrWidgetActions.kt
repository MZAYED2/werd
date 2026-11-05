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
            prefs[KEY_COLOR_PRESET] = settings.colorPreset.id
            prefs[KEY_GLASS_OPACITY] = settings.glassOpacity
            prefs[KEY_PATTERN] = settings.pattern.id
            prefs[KEY_PATTERN_OPACITY] = settings.patternOpacity
            prefs[KEY_PATTERN_SCALE] = settings.patternScale
            prefs[KEY_FONT_SIZE] = settings.fontSizeSp
            prefs[KEY_UPDATE_VERSION] = System.currentTimeMillis()
        }
        ZekrGlanceWidget().update(context, glanceId)
    }
}

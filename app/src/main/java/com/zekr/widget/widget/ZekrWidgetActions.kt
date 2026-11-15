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
        val seenIds = prefRepo.getSeenItemIds()

        val nextResult = ZekrRepository.selectNextFromPool(
            allItems = ZekrRepository.getItems(appContext),
            includeQuran = settings.includeQuran,
            includeHadith = settings.includeHadith,
            includeAzkar = settings.includeAzkar,
            smartTimeContextEnabled = settings.smartTimeContextEnabled,
            currentId = currentId,
            seenIds = seenIds
        )

        prefRepo.saveShuffleProgress(nextResult.item.id, nextResult.updatedSeenIds)

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[KEY_ZEKR_ID] = nextResult.item.id
            prefs[KEY_COLOR_PRESET] = settings.colorPreset.id
            prefs[KEY_IS_GRADIENT] = settings.isGradient
            prefs[KEY_PRIMARY_COLOR] = settings.primaryColor
            prefs[KEY_SECONDARY_COLOR] = settings.secondaryColor
            prefs[KEY_GRADIENT_DIRECTION] = settings.gradientDirection.id
            prefs[KEY_GLASS_OPACITY] = settings.glassOpacity
            prefs[KEY_BLUR_GRADE] = settings.blurGrade
            prefs[KEY_PATTERN] = settings.pattern.id
            prefs[KEY_PATTERN_OPACITY] = settings.patternOpacity
            prefs[KEY_PATTERN_SCALE] = settings.patternScale
            prefs[KEY_FONT] = settings.font.id
            prefs[KEY_FONT_SIZE] = settings.fontSizeSp
            prefs[KEY_UPDATE_VERSION] = System.currentTimeMillis()
        }
        ZekrGlanceWidget().update(context, glanceId)
    }
}

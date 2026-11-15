package com.zekr.widget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.widget.render.ZekrShareHelper

class TapZekrBodyAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ZekrGlanceWidget.handleBodyTap(context.applicationContext)
    }
}

class SkipWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ZekrGlanceWidget.shuffleToNext(context.applicationContext, clearPin = true)
    }
}

class ToggleFavoriteWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val prefRepo = WidgetPreferencesRepository(appContext)
        val currentId = prefRepo.getCurrentItemId()
        prefRepo.toggleFavorite(currentId)
        ZekrGlanceWidget.updateAllWidgets(appContext, specificItemId = currentId)
    }
}

class ShareWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val prefRepo = WidgetPreferencesRepository(appContext)
        val settings = prefRepo.getSettings()
        val currentId = prefRepo.getCurrentItemId()
        val customItems = prefRepo.getCustomItems()
        val item = ZekrRepository.getById(appContext, currentId, customItems)
        ZekrShareHelper.shareZekrCard(appContext, item, settings)
    }
}

class ShuffleWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ZekrGlanceWidget.shuffleToNext(context.applicationContext, clearPin = true)
    }
}

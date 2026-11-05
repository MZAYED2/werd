package com.zekr.widget.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.zekr.widget.data.repository.ZekrRepository

val KEY_ZEKR_ID = intPreferencesKey("key_current_zekr_id")
val KEY_COUNT = intPreferencesKey("key_current_count")

class IncrementCountAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentId = prefs[KEY_ZEKR_ID] ?: 1
            val item = ZekrRepository.getById(currentId)
            val current = prefs[KEY_COUNT] ?: 0
            if (current < item.targetCount) {
                prefs[KEY_COUNT] = current + 1
            }
        }
        ZekrGlanceWidget().update(context, glanceId)
    }
}

class NextZekrAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentId = prefs[KEY_ZEKR_ID] ?: 1
            prefs[KEY_ZEKR_ID] = ZekrRepository.getNextId(currentId)
            prefs[KEY_COUNT] = 0
        }
        ZekrGlanceWidget().update(context, glanceId)
    }
}

class PreviousZekrAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentId = prefs[KEY_ZEKR_ID] ?: 1
            prefs[KEY_ZEKR_ID] = ZekrRepository.getPreviousId(currentId)
            prefs[KEY_COUNT] = 0
        }
        ZekrGlanceWidget().update(context, glanceId)
    }
}

class ResetCountAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[KEY_COUNT] = 0
        }
        ZekrGlanceWidget().update(context, glanceId)
    }
}

package com.zekr.widget.widget

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository

val KEY_LOCK_ZEKR_ID = intPreferencesKey("key_lock_zekr_id")
val KEY_LOCK_TASBEEH_COUNT = intPreferencesKey("key_lock_tasbeeh_count")
val KEY_LOCK_UPDATE_TIME = longPreferencesKey("key_lock_update_time")

/**
 * Ultra-Compact Lock Screen Widget designed specifically for Keyguard slots
 * on Samsung One UI 6.1/7 (Galaxy S24/S25) and Android 14/15/16.
 * Supports background 1-tap fast tasbeeh without unlocking the phone!
 */
class LockScreenZekrGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        suspend fun updateAll(
            context: Context,
            specificItemId: Int? = null,
            specificCount: Int? = null
        ) {
            val appContext = context.applicationContext
            val prefRepo = WidgetPreferencesRepository(appContext)
            val currentItemId = specificItemId ?: prefRepo.getCurrentItemId()
            val tasbeehState = prefRepo.getTasbeehState()
            val currentCount = specificCount ?: if (tasbeehState.zekrId == currentItemId) {
                tasbeehState.count
            } else {
                prefRepo.getCurrentRepetitionCount()
            }
            val time = System.currentTimeMillis()

            try {
                val manager = GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(LockScreenZekrGlanceWidget::class.java)
                for (id in glanceIds) {
                    updateAppWidgetState(appContext, id) { prefs ->
                        prefs[KEY_LOCK_ZEKR_ID] = currentItemId
                        prefs[KEY_LOCK_TASBEEH_COUNT] = currentCount
                        prefs[KEY_LOCK_UPDATE_TIME] = time
                    }
                    LockScreenZekrGlanceWidget().update(appContext, id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun updateAllWidgets(
            context: Context,
            specificItemId: Int? = null,
            specificCount: Int? = null
        ) = updateAll(context, specificItemId, specificCount)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val prefRepo = WidgetPreferencesRepository(appContext)
        val customItems = prefRepo.getCustomItems()

        provideContent {
            val prefs = currentState<Preferences>()
            val zekrId = prefs[KEY_LOCK_ZEKR_ID] ?: 1
            val count = prefs[KEY_LOCK_TASBEEH_COUNT] ?: 0

            val item: ZekrEntity = ZekrRepository.getById(appContext, zekrId, customItems)
            val countStr = ZekrRepository.toArabicDigits(count)
            val citation = item.getConciseCitation(maxChars = 26)

            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Fast Tasbeeh Tap Circle (1-tap without unlocking phone)
                    Box(
                        modifier = GlanceModifier
                            .width(52.dp)
                            .fillMaxHeight()
                            .clickable(actionRunCallback<LockScreenTasbeehTapAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = countStr,
                                style = TextStyle(
                                    color = ColorProvider(android.graphics.Color.WHITE),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Text(
                                text = "تسبيح",
                                style = TextStyle(
                                    color = ColorProvider(android.graphics.Color.parseColor("#A5D6A7")),
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Right Side: Zekr Text and Citation (Tap to shuffle to next zekr)
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(actionRunCallback<LockScreenShuffleAction>()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.getFormattedArabicText(),
                            maxLines = 2,
                            style = TextStyle(
                                color = ColorProvider(android.graphics.Color.WHITE),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.End
                            )
                        )
                        if (citation.isNotEmpty()) {
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = citation,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(android.graphics.Color.parseColor("#E0E0E0")),
                                    fontSize = 9.5.sp,
                                    textAlign = TextAlign.End
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class LockScreenTasbeehTapAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val prefRepo = WidgetPreferencesRepository(appContext)

        val customItems = prefRepo.getCustomItems()
        val currentId = prefRepo.getCurrentItemId()
        val item = ZekrRepository.getById(appContext, currentId, customItems)

        val state = prefRepo.getTasbeehState()
        val currentCount = if (state.zekrId == currentId) state.count else prefRepo.getCurrentRepetitionCount()
        val nextCount = currentCount + 1
        val target = if (item.repetition > 1) item.repetition else 33
        val reached = nextCount >= target

        // Trigger subtle haptic on lock screen tap
        triggerHaptic(appContext, strong = reached)

        prefRepo.recordTasbeehTap(
            item = item,
            newCount = nextCount,
            target = target,
            isFreeMode = false
        )

        // Update Lock Screen & Home Screen widgets
        LockScreenZekrGlanceWidget.updateAll(appContext, specificItemId = item.id, specificCount = nextCount)
        ZekrGlanceWidget.updateAllWidgets(appContext)
    }

    private fun triggerHaptic(context: Context, strong: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val duration = if (strong) 50L else 22L
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (strong) 60L else 25L)
                }
            }
        } catch (_: Exception) {}
    }
}

class LockScreenShuffleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        ZekrGlanceWidget.shuffleToNext(appContext, clearPin = false)
    }
}

package com.zekr.widget.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.GradientPreset
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.widget.ZekrGlanceWidget
import java.util.concurrent.TimeUnit

/**
 * Advances pattern + background (solid preset or gradient preset) one step,
 * then refreshes all Glance widgets.
 */
class StyleRotationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefRepo = WidgetPreferencesRepository(applicationContext)
            val settings = prefRepo.getSettings()
            if (!settings.styleRotationEnabled) {
                StyleRotationScheduler.cancel(applicationContext)
                return Result.success()
            }

            val rotated = StyleRotator.next(settings)
            prefRepo.updateSettings(rotated)
            ZekrGlanceWidget.updateAllWidgets(applicationContext, rotated)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

object StyleRotator {
    private val rotatablePatterns: List<PatternType> =
        PatternType.values().filter { it != PatternType.NONE }

    fun next(settings: WidgetSettings): WidgetSettings {
        val nextPattern = nextPattern(settings.pattern)
        return if (settings.isGradient) {
            nextGradientBackground(settings).copy(pattern = nextPattern)
        } else {
            nextSolidBackground(settings).copy(pattern = nextPattern)
        }
    }

    private fun nextPattern(current: PatternType): PatternType {
        if (rotatablePatterns.isEmpty()) return current
        val index = rotatablePatterns.indexOf(current).let { if (it < 0) 0 else it }
        return rotatablePatterns[(index + 1) % rotatablePatterns.size]
    }

    private fun nextSolidBackground(settings: WidgetSettings): WidgetSettings {
        val presets = GlassColorPreset.values()
        val index = presets.indexOf(settings.colorPreset).let { if (it < 0) 0 else it }
        val next = presets[(index + 1) % presets.size]
        return settings.copy(
            colorPreset = next,
            primaryColor = next.hexColor
        )
    }

    private fun nextGradientBackground(settings: WidgetSettings): WidgetSettings {
        val presets = GradientPreset.values()
        val index = presets.indexOfFirst {
            it.startColor == settings.primaryColor && it.endColor == settings.secondaryColor
        }.let { if (it < 0) 0 else it }
        val next = presets[(index + 1) % presets.size]
        return settings.copy(
            primaryColor = next.startColor,
            secondaryColor = next.endColor,
            gradientDirection = next.defaultDirection
        )
    }
}

object StyleRotationScheduler {
    const val UNIQUE_WORK_NAME = "style_rotation_periodic"

    fun sync(context: Context, enabled: Boolean, hours: Int) {
        val appContext = context.applicationContext
        if (!enabled) {
            cancel(appContext)
            return
        }
        val safeHours = hours.coerceIn(1, 168).toLong()
        val request = PeriodicWorkRequestBuilder<StyleRotationWorker>(
            safeHours,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

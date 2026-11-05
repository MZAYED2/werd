package com.zekr.widget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.widget.render.GlassRenderer
import kotlinx.coroutines.flow.first

val KEY_ZEKR_ID = intPreferencesKey("key_current_zekr_id")

class ZekrGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        suspend fun shuffleToNext(context: Context) {
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
            ZekrGlanceWidget().updateAll(appContext)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val prefRepo = WidgetPreferencesRepository(appContext)
        val settings = prefRepo.getSettings()
        val currentId = prefRepo.getCurrentItemId()
        val item: ZekrEntity = ZekrRepository.getById(appContext, currentId)

        // Generate Glassmorphic Bitmap Canvas with selected pattern and opacity
        val glassBitmap = GlassRenderer.createGlassBackgroundBitmap(
            width = 680,
            height = 380,
            colorPreset = settings.colorPreset,
            opacity = settings.glassOpacity,
            pattern = settings.pattern,
            cornerRadius = 56f
        )

        val textColor = Color(settings.colorPreset.textColor)
        val accentColor = Color(settings.colorPreset.accentColor)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionRunCallback<ShuffleWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    // Glass Canvas Background
                    Image(
                        provider = ImageProvider(glassBitmap),
                        contentDescription = "Glass Background",
                        contentScale = ContentScale.FillBounds,
                        modifier = GlanceModifier.fillMaxSize()
                    )

                    // Widget Interactive Content
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Header: Type Pill & Subtle Tap Hint
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⟳ انقر للتبديل",
                                style = TextStyle(
                                    color = ColorProvider(accentColor.copy(alpha = 0.75f)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = item.type.labelArabic,
                                style = TextStyle(
                                    color = ColorProvider(accentColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Center: Main Arabic Text (Authentic Hadith / Ayah / Zikr)
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.arabicText,
                                style = TextStyle(
                                    color = ColorProvider(textColor),
                                    fontSize = settings.fontSizeSp.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Footer: Exact Source & Authentication
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (item.reference.isNotEmpty()) "${item.source} • ${item.reference}" else item.source,
                                style = TextStyle(
                                    color = ColorProvider(textColor.copy(alpha = 0.82f)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

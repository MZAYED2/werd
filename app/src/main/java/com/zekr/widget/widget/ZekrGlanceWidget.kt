package com.zekr.widget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import com.zekr.widget.data.repository.ZekrRepository

class ZekrGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val currentId = prefs[KEY_ZEKR_ID] ?: 1
            val currentCount = prefs[KEY_COUNT] ?: 0
            val item = ZekrRepository.getById(currentId)

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF0F3E2C))
                        .cornerRadius(20.dp)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header: Category & Counter Status
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.category.titleArabic,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF81C784)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "$currentCount / ${item.targetCount}",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFE8F5E9)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Zekr Arabic Text
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
                                    color = ColorProvider(Color.White),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Control Buttons Row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                text = "◀",
                                onClick = actionRunCallback<PreviousZekrAction>(),
                                modifier = GlanceModifier.width(44.dp).height(38.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Button(
                                text = "ذكر ($currentCount)",
                                onClick = actionRunCallback<IncrementCountAction>(),
                                modifier = GlanceModifier.defaultWeight().height(38.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Button(
                                text = "▶",
                                onClick = actionRunCallback<NextZekrAction>(),
                                modifier = GlanceModifier.width(44.dp).height(38.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Button(
                                text = "↺",
                                onClick = actionRunCallback<ResetCountAction>(),
                                modifier = GlanceModifier.width(36.dp).height(38.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

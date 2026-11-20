package com.zekr.widget.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.data.model.WirdStats
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.repository.ZekrRepository

@Composable
fun DailyWirdTabContent(
    stats: WirdStats,
    allItems: List<ZekrEntity> = emptyList(),
    openGuidedQuran: Boolean = false,
    onGuidedQuranOpened: () -> Unit = {},
    onUpdateDailyGoal: (Int) -> Unit
) {
    val palette = MaterialTheme.colorScheme
    val today = stats.todayRecord()
    val currentStreak = stats.currentStreak()
    val bestStreak = maxOf(stats.bestStreak, currentStreak)
    val weekly = stats.weeklySummary()
    val goal = stats.dailyGoal.coerceAtLeast(1)
    val rawProgress = (today.completedCount.toFloat() / goal).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "daily_wird_ring"
    )
    val goalReached = today.completedCount >= goal

    val listState = rememberLazyListState()
    LaunchedEffect(openGuidedQuran) {
        if (openGuidedQuran) listState.scrollToItem(0)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "guided-wird") { GuidedWirdCard(allItems, openGuidedQuran, onGuidedQuranOpened) }
        // 1. Hero Daily Consistency Ring Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(
                    1.dp,
                    if (goalReached) palette.tertiary.copy(alpha = 0.55f)
                    else palette.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Calm Progress Ring
                    Box(
                        modifier = Modifier.size(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val ringColors = if (goalReached) {
                            listOf(palette.tertiary, palette.primary)
                        } else {
                            listOf(palette.primary, palette.primary)
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 12.dp.toPx()
                            drawArc(
                                color = palette.outlineVariant,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            if (animatedProgress > 0f) {
                                drawArc(
                                    brush = Brush.sweepGradient(ringColors),
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedProgress,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${ZekrRepository.toArabicDigits(today.completedCount)} / ${ZekrRepository.toArabicDigits(goal)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = if (goalReached) palette.tertiary else palette.onSurface
                            )
                            Text(
                                text = if (goalReached) "أتممت ورد اليوم ✓" else "ذكر اليوم",
                                fontSize = 11.sp,
                                color = if (goalReached) palette.primary else palette.onSurfaceVariant
                            )
                        }
                    }

                    // Streak & Tasbih Taps Stats Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniPill(
                            label = "سلسلة المواظبة",
                            value = "${ZekrRepository.toArabicDigits(currentStreak)} ${if (currentStreak == 1) "يوم" else "أيام"}",
                            accentColor = Color(0xFFF59E0B)
                        )
                        StatMiniPill(
                            label = "إجمالي تسبيحات اليوم",
                            value = "${ZekrRepository.toArabicDigits(today.totalTasbihTaps)} تسبيحة",
                            accentColor = palette.primary
                        )
                        StatMiniPill(
                            label = "أطول سلسلة",
                            value = "${ZekrRepository.toArabicDigits(bestStreak)} ${if (bestStreak == 1) "يوم" else "أيام"}",
                            accentColor = Color(0xFF60A5FA)
                        )
                    }
                }
            }
        }

        // 2. 7-Day Weekly Consistency Row
        item {
            ModernSectionCard(
                title = "المواظبة الأسبوعية",
                subtitle = "يكفي إتمام ذكر واحد يومياً للحفاظ على سلسلة المواظبة"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekly.forEach { day ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (day.isToday) palette.primaryContainer.copy(alpha = 0.35f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 4.dp.toPx()
                                    drawArc(
                                        color = palette.outlineVariant.copy(alpha = 0.5f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                    if (day.progress > 0f) {
                                        drawArc(
                                            color = if (day.progress >= 1f) palette.tertiary else palette.primary,
                                            startAngle = -90f,
                                            sweepAngle = 360f * day.progress,
                                            useCenter = false,
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                                Text(
                                    text = ZekrRepository.toArabicDigits(day.completedCount),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.completedCount > 0) palette.onSurface else palette.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }

                            Text(
                                text = day.arabicDayShort,
                                fontSize = 10.sp,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (day.isToday) palette.primary else palette.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Today's Content Breakdown
        item {
            ModernSectionCard(
                title = "تفاصيل ورد اليوم",
                subtitle = "الأذكار والآيات التي أتممتها عبر الويدجت أو المعاينة"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BreakdownBox(
                        title = "آيات قرآنية",
                        count = today.quranCompleted,
                        color = palette.primary,
                        modifier = Modifier.weight(1f)
                    )
                    BreakdownBox(
                        title = "أحاديث نبوية",
                        count = today.hadithCompleted,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                    BreakdownBox(
                        title = "أذكار وأدعية",
                        count = today.azkarCompleted,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Configurable Daily Goal
        item {
            ModernSectionCard(
                title = "الهدف اليومي للوِرد",
                subtitle = "اختر عدد الأذكار المكتملة التي تطمح إليها كل يوم"
            ) {
                val goalOptions = listOf(5, 10, 20, 33, 50)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(goalOptions) { option ->
                        FilterChip(
                            selected = stats.dailyGoal == option,
                            onClick = { onUpdateDailyGoal(option) },
                            label = {
                                Text(
                                    text = "${ZekrRepository.toArabicDigits(option)} أذكار",
                                    fontWeight = if (stats.dailyGoal == option) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniPill(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BreakdownBox(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = ZekrRepository.toArabicDigits(count),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

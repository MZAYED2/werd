package com.zekr.widget.ui.tasbeeh

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.WirdStats
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.GradientDirection
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.ui.theme.FontHelper
import com.zekr.widget.ui.theme.ZekrWidgetTheme
import com.zekr.widget.widget.ZekrGlanceWidget
import com.zekr.widget.widget.render.PatternDrawables
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TasbeehDialogActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ZEKR_ID = "extra_zekr_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialZekrId = intent.getIntExtra(EXTRA_ZEKR_ID, -1)

        setContent {
            ZekrWidgetTheme(darkTheme = true, dynamicColor = false) {
                TasbeehDialogScreen(
                    initialZekrId = initialZekrId,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

/**
 * Floating Popup Dialog over the Home Screen or App
 */
@Composable
fun TasbeehDialogScreen(
    initialZekrId: Int = -1,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f)),
        contentAlignment = Alignment.Center
    ) {
        // Scrim layer: click outside dialog card to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )

        // Floating Card with matching widget styles and pattern
        TasbeehViewContent(
            initialZekrId = initialZekrId,
            isDialog = true,
            onDismiss = onDismiss
        )
    }
}

/**
 * Full Page Screen used in Bottom Navigation inside the application
 */
@Composable
fun TasbeehScreen(
    settings: WidgetSettings? = null,
    onBackToStudio: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115)),
        contentAlignment = Alignment.TopCenter
    ) {
        TasbeehViewContent(
            initialZekrId = -1,
            isDialog = false,
            injectedSettings = settings,
            onDismiss = onBackToStudio
        )
    }
}

@Composable
private fun TasbeehViewContent(
    initialZekrId: Int = -1,
    isDialog: Boolean = true,
    injectedSettings: WidgetSettings? = null,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val prefRepo = remember { WidgetPreferencesRepository(context.applicationContext) }

    var settings by remember { mutableStateOf(injectedSettings ?: WidgetSettings()) }
    var currentItem by remember { mutableStateOf<ZekrEntity?>(null) }
    var currentCount by remember { mutableIntStateOf(0) }
    var selectedTarget by remember { mutableIntStateOf(33) }
    var isCustomFreeMode by remember { mutableStateOf(false) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var isPressedAnim by remember { mutableStateOf(false) }

    // Collect daily stats for instant sync of "إجمالي تسبيحات اليوم"
    val wirdStats by prefRepo.wirdStatsFlow.collectAsState(initial = remember { WirdStats() })
    val totalTasbeehsToday = wirdStats.todayRecord().totalTasbihTaps

    // Smart Tasbeeh Context initialization:
    // 1. If displayed item is repetitive zekr, open on it with saved progress.
    // 2. If Ayah or non-tasbeeh text, open on the last zekr the user was making tasbeeh with.
    // 3. If no previous zekr, open on default "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ".
    // 4. Do not abruptly change the zekr if the widget updates in the background.
    LaunchedEffect(initialZekrId) {
        val loadedSettings = injectedSettings ?: prefRepo.getSettings()
        settings = loadedSettings

        val (resolvedItem, savedCount, resolvedTarget) = prefRepo.resolveSmartTasbeehItem(
            context = context,
            displayedItemId = initialZekrId
        )
        currentItem = resolvedItem
        currentCount = savedCount
        selectedTarget = resolvedTarget

        val state = prefRepo.getTasbeehState()
        isCustomFreeMode = state.isFreeMode
        vibrationEnabled = state.vibrationEnabled
        soundEnabled = state.soundEnabled
    }

    fun triggerHaptic(strong: Boolean = false) {
        if (!vibrationEnabled) return
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
                    if (strong) {
                        val timings = longArrayOf(0, 50, 60, 65)
                        val amplitudes = intArrayOf(0, 255, 0, 255)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        vibrator.vibrate(VibrationEffect.createOneShot(24, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (strong) 130 else 25)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClickSound() {
        if (soundEnabled) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    fun onPerformTap() {
        isPressedAnim = true
        val target = if (isCustomFreeMode) Int.MAX_VALUE else selectedTarget
        val nextCount = currentCount + 1
        currentCount = nextCount

        val reached = !isCustomFreeMode && nextCount >= target
        triggerHaptic(strong = reached)
        playClickSound()

        val item = currentItem
        if (item != null) {
            scope.launch {
                prefRepo.recordTasbeehTap(
                    item = item,
                    newCount = nextCount,
                    target = target,
                    isFreeMode = isCustomFreeMode
                )
                ZekrGlanceWidget.updateAllWidgets(context)
            }
        }
    }

    fun resetCounter() {
        currentCount = 0
        triggerHaptic(strong = false)
        val item = currentItem
        if (item != null) {
            scope.launch {
                prefRepo.saveTasbeehProgress(
                    zekrId = item.id,
                    count = 0,
                    target = selectedTarget,
                    isFreeMode = isCustomFreeMode
                )
                ZekrGlanceWidget.updateAllWidgets(context)
            }
        }
    }

    fun shuffleNextZekr() {
        scope.launch {
            val seenIds = prefRepo.getSeenItemIds()
            val favIds = prefRepo.getFavoriteIds()
            val custom = prefRepo.getCustomItems()
            val all = ZekrRepository.getAllItems(context, custom)

            val tasbeehPool = all.filter {
                it.type != ContentType.QURAN_AYAH &&
                    (it.repetition > 1 || it.category == "التسبيح" || it.category.contains("أذكار"))
            }.ifEmpty { all }

            val nextResult = ZekrRepository.selectNextFromPool(
                allItems = tasbeehPool,
                includeQuran = false,
                includeHadith = true,
                includeAzkar = true,
                smartTimeContextEnabled = false,
                favoritesOnlyMode = false,
                favoriteIds = favIds,
                currentId = currentItem?.id,
                seenIds = seenIds
            )
            currentItem = nextResult.item
            val baseRep = nextResult.item.repetition.coerceAtLeast(33)
            selectedTarget = baseRep
            currentCount = 0
            prefRepo.saveTasbeehProgress(nextResult.item.id, 0, baseRep, isCustomFreeMode)
            ZekrGlanceWidget.updateAllWidgets(context)
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressedAnim) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "tasbeeh_btn_scale",
        finishedListener = { isPressedAnim = false }
    )

    // Dynamic widget styling: Colors, Gradients, Typography, Patterns
    val zekrFont = FontHelper.getComposeFontFamily(settings.font)
    val textColor = Color(settings.getAdaptiveTextColor())
    val accentColor = Color(settings.getAdaptiveAccentColor())
    val primaryColor = Color(settings.primaryColor)
    val secondaryColor = Color(settings.secondaryColor)

    val backgroundBrush = remember(settings.primaryColor, settings.secondaryColor, settings.isGradient, settings.gradientDirection) {
        if (settings.isGradient) {
            when (settings.gradientDirection) {
                GradientDirection.VERTICAL -> Brush.verticalGradient(
                    listOf(primaryColor.copy(alpha = 0.94f), secondaryColor.copy(alpha = 0.97f))
                )
                GradientDirection.HORIZONTAL -> Brush.horizontalGradient(
                    listOf(primaryColor.copy(alpha = 0.94f), secondaryColor.copy(alpha = 0.97f))
                )
                GradientDirection.RADIAL -> Brush.radialGradient(
                    listOf(primaryColor.copy(alpha = 0.97f), secondaryColor.copy(alpha = 0.94f))
                )
                else -> Brush.linearGradient(
                    listOf(primaryColor.copy(alpha = 0.94f), secondaryColor.copy(alpha = 0.97f))
                )
            }
        } else {
            Brush.linearGradient(
                listOf(primaryColor.copy(alpha = 0.94f), primaryColor.copy(alpha = 0.98f))
            )
        }
    }

    val buttonRadialBrush = remember(primaryColor, secondaryColor) {
        Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.95f),
                secondaryColor.copy(alpha = 0.98f)
            )
        )
    }

    val item = currentItem
    val effectiveTarget = if (isCustomFreeMode) 1 else selectedTarget.coerceAtLeast(1)
    val progress = if (isCustomFreeMode) 0f else (currentCount.toFloat() / effectiveTarget).coerceIn(0f, 1f)
    val isCompleted = !isCustomFreeMode && currentCount >= effectiveTarget

    val containerModifier = if (isDialog) {
        Modifier
            .fillMaxWidth(0.93f)
            .heightIn(max = 700.dp)
            .wrapContentHeight()
    } else {
        Modifier
            .fillMaxSize()
    }

    Card(
        modifier = containerModifier,
        shape = if (isDialog) RoundedCornerShape(30.dp) else RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDialog) Color.Transparent else Color(0xFF0F1115)),
        border = if (isDialog) BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDialog) 24.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            // Authentic Islamic Pattern rendered directly behind the content
            if (settings.pattern != PatternType.NONE) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawIntoCanvas { canvas ->
                        PatternDrawables.drawPattern(
                            canvas = canvas.nativeCanvas,
                            pattern = settings.pattern,
                            width = size.width,
                            height = size.height,
                            patternColor = textColor.toArgb(),
                            alpha = (settings.patternOpacity * 255).roundToInt().coerceIn(0, 255),
                            scale = settings.patternScale,
                            context = context
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = if (isDialog) 18.dp else 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: Header + Reset + (Close Button if dialog)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isDialog && onDismiss != null) {
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(textColor.copy(alpha = 0.12f))
                        ) {
                            Text(
                                text = "✕",
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(38.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📿",
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (isDialog) "مسبحة الذكر" else "السبحة الإلكترونية",
                            color = textColor,
                            fontFamily = zekrFont,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { resetCounter() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(textColor.copy(alpha = 0.12f))
                    ) {
                        Text(
                            text = "↺",
                            color = accentColor,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Total Tasbeehs Today Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✨",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "إجمالي تسبيحات اليوم: ${ZekrRepository.toArabicDigits(totalTasbeehsToday)}",
                        color = accentColor,
                        fontFamily = zekrFont,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zekr Arabic Text & Citation Card
                if (item != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.30f)),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.arabicText,
                                color = textColor,
                                fontFamily = zekrFont,
                                fontSize = 19.5.sp,
                                lineHeight = 31.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.getConciseCitation(),
                                color = textColor.copy(alpha = 0.75f),
                                fontFamily = zekrFont,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val defaultRep = item?.repetition?.coerceAtLeast(1) ?: 33
                    val targets = listOf(
                        defaultRep to if (defaultRep > 1) "الذكر ($defaultRep)" else "٣٣",
                        33 to "٣٣",
                        100 to "١٠٠",
                        -1 to "حر ∞"
                    ).distinctBy { it.first }

                    targets.forEach { (count, label) ->
                        val isSelected = if (count == -1) isCustomFreeMode else (!isCustomFreeMode && selectedTarget == count)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.32f)
                                    else textColor.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor
                                    else textColor.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (count == -1) {
                                        isCustomFreeMode = true
                                    } else {
                                        isCustomFreeMode = false
                                        selectedTarget = count
                                    }
                                    val currentIt = currentItem
                                    if (currentIt != null) {
                                        scope.launch {
                                            prefRepo.saveTasbeehProgress(
                                                zekrId = currentIt.id,
                                                count = currentCount,
                                                target = if (count == -1) 1 else count,
                                                isFreeMode = count == -1
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) accentColor else textColor.copy(alpha = 0.75f),
                                fontFamily = zekrFont,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Big Circular Interactive Tasbeeh Button matching widget colors
                Box(
                    modifier = Modifier
                        .size(195.dp)
                        .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                        .shadow(16.dp, shape = CircleShape, spotColor = accentColor)
                        .clip(CircleShape)
                        .background(buttonRadialBrush)
                        .border(2.5.dp, accentColor.copy(alpha = 0.60f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = accentColor)
                        ) { onPerformTap() },
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress Arc
                    if (!isCustomFreeMode && progress > 0f) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            val strokeW = 7.5.dp.toPx()
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(accentColor, accentColor.copy(alpha = 0.65f), accentColor)
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Count Numbers & Label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = ZekrRepository.toArabicDigits(currentCount),
                            color = textColor,
                            fontFamily = zekrFont,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (isCustomFreeMode) "تسبيحة"
                            else "من أصل ${ZekrRepository.toArabicDigits(selectedTarget)}",
                            color = accentColor,
                            fontFamily = zekrFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Completion Celebration Banner
                AnimatedVisibility(
                    visible = isCompleted,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.90f)),
                        border = BorderStroke(1.2.dp, accentColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "✨ تم بحمد الله إتمام الذكر!",
                                color = accentColor,
                                fontFamily = zekrFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Utility Row: Sound/Vibration toggles & Next zekr
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Vibration Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (vibrationEnabled) accentColor.copy(alpha = 0.22f)
                                    else textColor.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (vibrationEnabled) accentColor.copy(alpha = 0.6f)
                                    else textColor.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val nextVibe = !vibrationEnabled
                                    vibrationEnabled = nextVibe
                                    scope.launch {
                                        prefRepo.setTasbeehPreferences(nextVibe, soundEnabled)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (vibrationEnabled) "📳 اهتزاز" else "📳 معطل",
                                color = if (vibrationEnabled) accentColor else textColor.copy(alpha = 0.6f),
                                fontFamily = zekrFont,
                                fontSize = 11.5.sp
                            )
                        }

                        // Sound Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (soundEnabled) accentColor.copy(alpha = 0.22f)
                                    else textColor.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (soundEnabled) accentColor.copy(alpha = 0.6f)
                                    else textColor.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val nextSound = !soundEnabled
                                    soundEnabled = nextSound
                                    scope.launch {
                                        prefRepo.setTasbeehPreferences(vibrationEnabled, nextSound)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (soundEnabled) "🔊 صوت" else "🔇 صامت",
                                color = if (soundEnabled) accentColor else textColor.copy(alpha = 0.6f),
                                fontFamily = zekrFont,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    // Next Zekr Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(textColor.copy(alpha = 0.10f))
                            .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                            .clickable { shuffleNextZekr() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ذكر آخر ‹",
                            color = textColor,
                            fontFamily = zekrFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

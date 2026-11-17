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
import com.zekr.widget.data.preferences.TasbeehDialogSize
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
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
    val context = LocalContext.current
    val prefRepo = remember { WidgetPreferencesRepository(context.applicationContext) }
    val dialogSize by prefRepo.tasbeehDialogSizeFlow.collectAsState(initial = TasbeehDialogSize.COMPACT)

    val cardWidthFraction = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 0.84f
        TasbeehDialogSize.MEDIUM -> 0.88f
        TasbeehDialogSize.LARGE -> 0.92f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Scrim layer: click outside dialog card to dismiss; consume clicks inside card
        Box(
            modifier = Modifier
                .fillMaxWidth(cardWidthFraction)
                .wrapContentHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* no-op: consume clicks on the card */ }
        ) {
            TasbeehViewContent(
                initialZekrId = initialZekrId,
                isDialog = true,
                dialogSize = dialogSize,
                onDismiss = onDismiss
            )
        }
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
            dialogSize = TasbeehDialogSize.LARGE,
            injectedSettings = settings,
            onDismiss = onBackToStudio
        )
    }
}

@Composable
private fun TasbeehViewContent(
    initialZekrId: Int = -1,
    isDialog: Boolean = true,
    dialogSize: TasbeehDialogSize = TasbeehDialogSize.COMPACT,
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
    val accentColor = Color(settings.getAdaptiveAccentColor())
    val primaryColor = Color(settings.primaryColor)
    val secondaryColor = Color(settings.secondaryColor)

    // Calming, luxurious dark glass background tinted delicately with widget theme
    val backgroundBrush = remember(primaryColor, secondaryColor, settings.isGradient) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF13161C).copy(alpha = 0.94f),
                primaryColor.copy(alpha = 0.16f),
                Color(0xFF0F1116).copy(alpha = 0.97f)
            )
        )
    }

    // Subtle button background - dark frosted glass with gentle rim
    val buttonBgBrush = remember(primaryColor) {
        Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.07f),
                Color(0xFF181B22).copy(alpha = 0.85f)
            )
        )
    }

    val item = currentItem
    val effectiveTarget = if (isCustomFreeMode) 1 else selectedTarget.coerceAtLeast(1)
    val progress = if (isCustomFreeMode) 0f else (currentCount.toFloat() / effectiveTarget).coerceIn(0f, 1f)
    val isCompleted = !isCustomFreeMode && currentCount >= effectiveTarget

    // Proportional dimensions based on selected TasbeehDialogSize
    val maxCardHeight = if (isDialog) {
        when (dialogSize) {
            TasbeehDialogSize.COMPACT -> 495.dp
            TasbeehDialogSize.MEDIUM -> 585.dp
            TasbeehDialogSize.LARGE -> 675.dp
        }
    } else {
        Modifier
    }

    val circleButtonSize = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 122.dp
        TasbeehDialogSize.MEDIUM -> 146.dp
        TasbeehDialogSize.LARGE -> 165.dp
    }

    val counterFontSize = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 32.sp
        TasbeehDialogSize.MEDIUM -> 38.sp
        TasbeehDialogSize.LARGE -> 44.sp
    }

    val zekrFontSize = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 15.5.sp
        TasbeehDialogSize.MEDIUM -> 17.sp
        TasbeehDialogSize.LARGE -> 18.5.sp
    }

    val zekrLineHeight = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 23.sp
        TasbeehDialogSize.MEDIUM -> 26.sp
        TasbeehDialogSize.LARGE -> 28.sp
    }

    val zekrMaxLines = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 3
        TasbeehDialogSize.MEDIUM -> 4
        TasbeehDialogSize.LARGE -> 5
    }

    val verticalContentPadding = if (isDialog) {
        when (dialogSize) {
            TasbeehDialogSize.COMPACT -> 10.dp
            TasbeehDialogSize.MEDIUM -> 14.dp
            TasbeehDialogSize.LARGE -> 18.dp
        }
    } else 20.dp

    val sectionSpacing = if (isDialog) {
        when (dialogSize) {
            TasbeehDialogSize.COMPACT -> 6.dp
            TasbeehDialogSize.MEDIUM -> 9.dp
            TasbeehDialogSize.LARGE -> 12.dp
        }
    } else 12.dp

    val containerModifier = if (isDialog) {
        Modifier
            .fillMaxWidth()
            .heightIn(max = when (dialogSize) {
                TasbeehDialogSize.COMPACT -> 505.dp
                TasbeehDialogSize.MEDIUM -> 590.dp
                TasbeehDialogSize.LARGE -> 680.dp
            })
            .wrapContentHeight()
    } else {
        Modifier.fillMaxSize()
    }

    Card(
        modifier = containerModifier,
        shape = if (isDialog) RoundedCornerShape(26.dp) else RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDialog) Color.Transparent else Color(0xFF0F1115)),
        border = if (isDialog) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDialog) 12.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            // Subtle, delicate Islamic geometry watermark
            if (settings.pattern != PatternType.NONE) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawIntoCanvas { canvas ->
                        val subtleAlpha = (settings.patternOpacity * 0.22f * 255).roundToInt().coerceIn(10, 32)
                        PatternDrawables.drawPattern(
                            canvas = canvas.nativeCanvas,
                            pattern = settings.pattern,
                            width = size.width,
                            height = size.height,
                            patternColor = android.graphics.Color.WHITE,
                            alpha = subtleAlpha,
                            scale = settings.patternScale,
                            context = context
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = verticalContentPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: Close (if dialog) + Header + Size Switcher + Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isDialog && onDismiss != null) {
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Text(
                                text = "✕",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }

                    // Dialog Size Switcher (Compact / Medium / Large)
                    if (isDialog) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            TasbeehDialogSize.values().forEach { s ->
                                val isSelected = dialogSize == s
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) accentColor.copy(alpha = 0.22f) else Color.Transparent)
                                        .clickable {
                                            scope.launch {
                                                prefRepo.setTasbeehDialogSize(s)
                                            }
                                        }
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = s.titleAr,
                                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.55f),
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "📿", fontSize = 17.sp)
                            Text(
                                text = "السبحة الإلكترونية",
                                color = Color.White.copy(alpha = 0.95f),
                                fontFamily = zekrFont,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { resetCounter() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = "↺",
                            color = accentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Total Tasbeehs Today Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(text = "✨", fontSize = 11.5.sp)
                    Text(
                        text = "إجمالي تسبيحات اليوم: ${ZekrRepository.toArabicDigits(totalTasbeehsToday)}",
                        color = accentColor.copy(alpha = 0.90f),
                        fontFamily = zekrFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Zekr Arabic Text & Citation Card
                if (item != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.arabicText,
                                color = Color.White.copy(alpha = 0.96f),
                                fontFamily = zekrFont,
                                fontSize = zekrFontSize,
                                lineHeight = zekrLineHeight,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                maxLines = zekrMaxLines,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Highly legible citation text
                            Text(
                                text = item.getConciseCitation(),
                                color = Color.White.copy(alpha = 0.82f),
                                fontFamily = zekrFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

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
                                .padding(horizontal = 2.5.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.16f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor.copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp)
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
                                .padding(horizontal = 8.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.65f),
                                fontFamily = zekrFont,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Elegant Minimalist Tasbeeh Button matching widget colors
                Box(
                    modifier = Modifier
                        .size(circleButtonSize)
                        .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                        .clip(CircleShape)
                        .background(buttonBgBrush)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = accentColor)
                        ) { onPerformTap() },
                    contentAlignment = Alignment.Center
                ) {
                    // Thin Refined Circular Progress Arc
                    if (!isCustomFreeMode && progress > 0f) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                            val strokeW = 3.2.dp.toPx()
                            drawArc(
                                color = accentColor.copy(alpha = 0.85f),
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
                            color = Color.White,
                            fontFamily = zekrFont,
                            fontSize = counterFontSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = if (isCustomFreeMode) "تسبيحة"
                            else "من أصل ${ZekrRepository.toArabicDigits(selectedTarget)}",
                            color = accentColor.copy(alpha = 0.85f),
                            fontFamily = zekrFont,
                            fontSize = 10.5.sp,
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
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "✨ تم بحمد الله إتمام الذكر!",
                                color = accentColor,
                                fontFamily = zekrFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Bottom Utility Row: Sound/Vibration toggles & Next zekr
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Vibration Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (vibrationEnabled) accentColor.copy(alpha = 0.16f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    1.dp,
                                    if (vibrationEnabled) accentColor.copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    vibrationEnabled = !vibrationEnabled
                                    scope.launch {
                                        prefRepo.setTasbeehPreferences(vibrationEnabled, soundEnabled)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (vibrationEnabled) "📳 اهتزاز" else "📴 صامت",
                                color = if (vibrationEnabled) accentColor else Color.White.copy(alpha = 0.45f),
                                fontFamily = zekrFont,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Sound Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (soundEnabled) accentColor.copy(alpha = 0.16f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    1.dp,
                                    if (soundEnabled) accentColor.copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    soundEnabled = !soundEnabled
                                    scope.launch {
                                        prefRepo.setTasbeehPreferences(vibrationEnabled, soundEnabled)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (soundEnabled) "🔊 صوت" else "🔈 كتم",
                                color = if (soundEnabled) accentColor else Color.White.copy(alpha = 0.45f),
                                fontFamily = zekrFont,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Next Zekr Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .clickable { shuffleNextZekr() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ذكر آخر ‹",
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = zekrFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

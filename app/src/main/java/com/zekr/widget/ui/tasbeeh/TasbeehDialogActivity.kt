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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.WirdStats
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.TasbeehDialogSize
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.ui.theme.FontHelper
import com.zekr.widget.ui.theme.ZekrWidgetTheme
import com.zekr.widget.widget.LockScreenZekrGlanceWidget
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
 * - Distinct Scrim (dimmed backdrop behind dialog).
 * - Smooth Draggable Card (clamped to safe screen boundaries).
 * - Transparent card container so text/icons stay 100% opaque, background opacity is isolated.
 */
@Composable
fun TasbeehDialogScreen(
    initialZekrId: Int = -1,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefRepo = remember { WidgetPreferencesRepository(context.applicationContext) }
    val dialogSize by prefRepo.tasbeehDialogSizeFlow.collectAsState(initial = TasbeehDialogSize.COMPACT)
    val cardOpacity by prefRepo.tasbeehCardOpacityFlow.collectAsState(initial = 0.94f)

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Re-center when size changes to avoid jumping outside boundaries
    LaunchedEffect(dialogSize) {
        offsetX = 0f
        offsetY = 0f
    }

    val cardWidthFraction = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 0.82f
        TasbeehDialogSize.MEDIUM -> 0.88f
        TasbeehDialogSize.LARGE -> 0.92f
    }

    // Scrim Layer (52% black dim)
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
        // Draggable Card Container clamped to screen insets
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .fillMaxWidth(cardWidthFraction)
                .wrapContentHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* Consume clicks to prevent dismissing dialog */ }
        ) {
            TasbeehViewContent(
                initialZekrId = initialZekrId,
                isDialog = true,
                dialogSize = dialogSize,
                cardOpacity = cardOpacity,
                onDragDelta = { dx, dy ->
                    offsetX = (offsetX + dx).coerceIn(-220f, 220f)
                    offsetY = (offsetY + dy).coerceIn(-340f, 340f)
                },
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
    val context = LocalContext.current
    val prefRepo = remember { WidgetPreferencesRepository(context.applicationContext) }
    val cardOpacity by prefRepo.tasbeehCardOpacityFlow.collectAsState(initial = 0.94f)

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
            cardOpacity = cardOpacity,
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
    cardOpacity: Float = 0.94f,
    onDragDelta: ((Float, Float) -> Unit)? = null,
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

    // Dialog state for Custom Target and Hadith Virtue Details
    var showCustomTargetDialog by remember { mutableStateOf(false) }
    var customTargetInput by remember { mutableStateOf("") }
    var showVirtueInfoDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Collect daily stats & undoable count for real-time synchronization
    val wirdStats by prefRepo.wirdStatsFlow.collectAsState(initial = remember { WirdStats() })
    val undoableCount by prefRepo.tasbeehUndoableCountFlow.collectAsState(initial = 0)
    val totalTasbeehsToday = wirdStats.todayRecord().totalTasbihTaps
    val canUndo = undoableCount > 0 && currentCount > 0

    // Smart Tasbeeh Context initialization:
    // 1. If displayed item is repetitive zekr, open on it with saved progress & authentic count.
    // 2. If Ayah or non-tasbeeh text, open on the last zekr the user was making tasbeeh with.
    // 3. If no previous zekr, open on default "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ".
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
                LockScreenZekrGlanceWidget.updateAllWidgets(context, specificItemId = item.id, specificCount = nextCount)
            }
        }
    }

    fun performUndo() {
        val item = currentItem ?: return
        if (!canUndo) return
        triggerHaptic(strong = false)
        playClickSound()
        scope.launch {
            val result = prefRepo.undoLastTasbeehTap(
                item = item,
                target = selectedTarget,
                isFreeMode = isCustomFreeMode
            )
            currentCount = result.newCount
            ZekrGlanceWidget.updateAllWidgets(context)
            LockScreenZekrGlanceWidget.updateAllWidgets(context, specificItemId = item.id, specificCount = result.newCount)
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
                LockScreenZekrGlanceWidget.updateAllWidgets(context, specificItemId = item.id, specificCount = 0)
            }
        }
    }

    fun cycleNextSize() {
        val nextSize = when (dialogSize) {
            TasbeehDialogSize.COMPACT -> TasbeehDialogSize.MEDIUM
            TasbeehDialogSize.MEDIUM -> TasbeehDialogSize.LARGE
            TasbeehDialogSize.LARGE -> TasbeehDialogSize.COMPACT
        }
        scope.launch {
            prefRepo.setTasbeehDialogSize(nextSize)
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
                    (it.repetition > 1 || it.category == "التسبيح" || it.tasbeehText != null || it.category.contains("أذكار"))
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
            val docCount = nextResult.item.getEffectiveDocumentedCount()
            val baseRep = docCount ?: 33
            selectedTarget = baseRep
            currentCount = 0
            prefRepo.saveTasbeehProgress(nextResult.item.id, 0, baseRep, isCustomFreeMode)
            ZekrGlanceWidget.updateAllWidgets(context)
            LockScreenZekrGlanceWidget.updateAllWidgets(context, specificItemId = nextResult.item.id, specificCount = 0)
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

    // Isolated Card Opacity on background brush:
    // Texts and icons remain 100% full opacity, while the card background has the user-chosen opacity (default 94%).
    val effectiveCardOpacity = cardOpacity.coerceIn(0.75f, 1.0f)
    val backgroundBrush = remember(primaryColor, secondaryColor, settings.isGradient, effectiveCardOpacity) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF13161C).copy(alpha = effectiveCardOpacity),
                primaryColor.copy(alpha = (effectiveCardOpacity * 0.22f)),
                Color(0xFF0F1116).copy(alpha = effectiveCardOpacity)
            )
        )
    }

    val buttonBgBrush = remember(primaryColor) {
        Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color(0xFF181B22).copy(alpha = 0.90f)
            )
        )
    }

    val item = currentItem
    val docCount = item?.getEffectiveDocumentedCount()
    val effectiveTarget = if (isCustomFreeMode) 1 else selectedTarget.coerceAtLeast(1)
    val progress = if (isCustomFreeMode) 0f else (currentCount.toFloat() / effectiveTarget).coerceIn(0f, 1f)
    val isCompleted = !isCustomFreeMode && currentCount >= effectiveTarget

    // Proportional dimensions for truly compact and scalable layouts
    val circleButtonSize = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 98.dp
        TasbeehDialogSize.MEDIUM -> 132.dp
        TasbeehDialogSize.LARGE -> 155.dp
    }

    val counterFontSize = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 27.sp
        TasbeehDialogSize.MEDIUM -> 34.sp
        TasbeehDialogSize.LARGE -> 42.sp
    }

    val zekrFontSize = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 14.5.sp
        TasbeehDialogSize.MEDIUM -> 16.5.sp
        TasbeehDialogSize.LARGE -> 18.sp
    }

    val zekrLineHeight = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 21.sp
        TasbeehDialogSize.MEDIUM -> 25.sp
        TasbeehDialogSize.LARGE -> 27.sp
    }

    val zekrMaxLines = when (dialogSize) {
        TasbeehDialogSize.COMPACT -> 2
        TasbeehDialogSize.MEDIUM -> 3
        TasbeehDialogSize.LARGE -> 5
    }

    val verticalContentPadding = if (isDialog) {
        when (dialogSize) {
            TasbeehDialogSize.COMPACT -> 8.dp
            TasbeehDialogSize.MEDIUM -> 12.dp
            TasbeehDialogSize.LARGE -> 16.dp
        }
    } else 18.dp

    val sectionSpacing = if (isDialog) {
        when (dialogSize) {
            TasbeehDialogSize.COMPACT -> 4.dp
            TasbeehDialogSize.MEDIUM -> 8.dp
            TasbeehDialogSize.LARGE -> 11.dp
        }
    } else 12.dp

    val containerModifier = if (isDialog) {
        Modifier
            .fillMaxWidth()
            .heightIn(max = when (dialogSize) {
                TasbeehDialogSize.COMPACT -> 390.dp
                TasbeehDialogSize.MEDIUM -> 515.dp
                TasbeehDialogSize.LARGE -> 640.dp
            })
            .wrapContentHeight()
    } else {
        Modifier.fillMaxSize()
    }

    Card(
        modifier = containerModifier,
        shape = if (isDialog) RoundedCornerShape(26.dp) else RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDialog) Color.Transparent else Color(0xFF0F1115)),
        border = if (isDialog) BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDialog) 12.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            // Subtle, non-distracting Islamic geometry pattern
            if (settings.pattern != PatternType.NONE) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawIntoCanvas { canvas ->
                        val subtleAlpha = (settings.patternOpacity * 0.18f * 255).roundToInt().coerceIn(8, 28)
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
                    .padding(horizontal = 14.dp, vertical = verticalContentPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Draggable Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isDialog && onDragDelta != null) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        onDragDelta(dragAmount.x, dragAmount.y)
                                    }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDialog) {
                        // Subtle Drag Handle Pill at the very top
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.28f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Top Actions Bar: Close + Size Cycle Button + Opacity/Settings + Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Close Button
                    if (isDialog && onDismiss != null) {
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Text(
                                text = "✕",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(30.dp))
                    }

                    // Center: Single Size Toggle Button or Screen Title
                    if (isDialog) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Single button cycling Compact -> Medium -> Large -> Compact
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.18f))
                                    .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                                    .clickable { cycleNextSize() }
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "📐 ${dialogSize.titleAr}",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Card Opacity Slider Toggle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                                    .clickable { showSettingsSheet = !showSettingsSheet }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "🌓 ${(effectiveCardOpacity * 100).roundToInt()}%",
                                    color = Color.White.copy(alpha = 0.80f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
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
                                color = Color.White,
                                fontFamily = zekrFont,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Reset Counter Button
                    IconButton(
                        onClick = { resetCounter() },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = "↺",
                            color = accentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expandable Card Opacity Slider Row (When toggled or in Large size)
                AnimatedVisibility(visible = showSettingsSheet || (dialogSize == TasbeehDialogSize.LARGE && isDialog)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "عتامة خلفية البطاقة",
                                color = Color.White.copy(alpha = 0.80f),
                                fontSize = 11.sp,
                                fontFamily = zekrFont
                            )
                            Text(
                                text = "${(effectiveCardOpacity * 100).roundToInt()}%",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = effectiveCardOpacity,
                            onValueChange = { scope.launch { prefRepo.setTasbeehCardOpacity(it) } },
                            valueRange = 0.75f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Total Tasbeehs Today Badge
                if (dialogSize != TasbeehDialogSize.COMPACT || isDialog) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "✨", fontSize = 10.sp)
                        Text(
                            text = "إجمالي تسبيحات اليوم: ${ZekrRepository.toArabicDigits(totalTasbeehsToday)}",
                            color = accentColor,
                            fontFamily = zekrFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(sectionSpacing))
                }

                // Zekr Formula Card (Separating Repeatable Zekr from Hadith Virtue)
                if (item != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = if (dialogSize == TasbeehDialogSize.COMPACT) 5.dp else 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Primary Repeatable Tasbeeh Formula
                            Text(
                                text = item.getEffectiveTasbeehText(),
                                color = Color.White,
                                fontFamily = zekrFont,
                                fontSize = zekrFontSize,
                                lineHeight = zekrLineHeight,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                maxLines = zekrMaxLines,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Citation & Virtue Details Trigger
                            Row(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showVirtueInfoDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.getConciseCitation(),
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontFamily = zekrFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ⓘ",
                                    color = accentColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Reorganized Targets: Documented Authentic Count + Quick 33/100 + Custom + Free
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val targetPills = mutableListOf<Pair<Int, String>>()

                    if (docCount != null && docCount > 0) {
                        targetPills.add(docCount to "الأصل (${ZekrRepository.toArabicDigits(docCount)})")
                    }

                    if (docCount != 33) {
                        targetPills.add(33 to "٣٣")
                    }

                    if (docCount != 100) {
                        targetPills.add(100 to "١٠٠")
                    }

                    targetPills.add(-2 to "مخصص")
                    targetPills.add(-1 to "حر ∞")

                    targetPills.forEach { (targetVal, label) ->
                        val isSelected = when (targetVal) {
                            -1 -> isCustomFreeMode
                            -2 -> !isCustomFreeMode && selectedTarget !in listOf(docCount ?: -99, 33, 100)
                            else -> !isCustomFreeMode && selectedTarget == targetVal
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.18f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor.copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(7.dp)
                                )
                                .clickable {
                                    when (targetVal) {
                                        -1 -> {
                                            isCustomFreeMode = true
                                        }
                                        -2 -> {
                                            customTargetInput = selectedTarget.toString()
                                            showCustomTargetDialog = true
                                        }
                                        else -> {
                                            isCustomFreeMode = false
                                            selectedTarget = targetVal
                                        }
                                    }
                                    val currentIt = currentItem
                                    if (currentIt != null && targetVal != -2) {
                                        scope.launch {
                                            prefRepo.saveTasbeehProgress(
                                                zekrId = currentIt.id,
                                                count = currentCount,
                                                target = if (targetVal == -1) 1 else targetVal,
                                                isFreeMode = targetVal == -1
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 6.5.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.70f),
                                fontFamily = zekrFont,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Tasbeeh Center Area: Counter Circle with Undo Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Undo Button (Disabled when no taps can be undone)
                    IconButton(
                        onClick = { performUndo() },
                        enabled = canUndo,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (canUndo) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f))
                            .border(1.dp, if (canUndo) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.04f), CircleShape)
                    ) {
                        Text(
                            text = "↶",
                            color = if (canUndo) accentColor else Color.White.copy(alpha = 0.25f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Main Counter Circle
                    Box(
                        modifier = Modifier
                            .size(circleButtonSize)
                            .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                            .clip(CircleShape)
                            .background(buttonBgBrush)
                            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = accentColor)
                            ) { onPerformTap() },
                        contentAlignment = Alignment.Center
                    ) {
                        // Thin Refined Circular Progress Arc
                        if (!isCustomFreeMode && progress > 0f) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                val strokeW = 3.dp.toPx()
                                drawArc(
                                    color = accentColor.copy(alpha = 0.90f),
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Count Digits & Target Label
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

                            Text(
                                text = if (isCustomFreeMode) "تسبيحة"
                                else "من ${ZekrRepository.toArabicDigits(selectedTarget)}",
                                color = accentColor.copy(alpha = 0.90f),
                                fontFamily = zekrFont,
                                fontSize = if (dialogSize == TasbeehDialogSize.COMPACT) 9.5.sp else 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Next Zekr Shuffle Icon
                    IconButton(
                        onClick = { shuffleNextZekr() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    ) {
                        Text(
                            text = "‹",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Completion Banner
                AnimatedVisibility(
                    visible = isCompleted,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "✨ تم بحمد الله إتمام الذكر!",
                                color = accentColor,
                                fontFamily = zekrFont,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Bottom Controls: Sound / Vibration Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
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
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (vibrationEnabled) "📳 اهتزاز" else "📴 صامت",
                                color = if (vibrationEnabled) accentColor else Color.White.copy(alpha = 0.45f),
                                fontFamily = zekrFont,
                                fontSize = 10.sp,
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
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (soundEnabled) "🔊 صوت" else "🔈 كتم",
                                color = if (soundEnabled) accentColor else Color.White.copy(alpha = 0.45f),
                                fontFamily = zekrFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom Target Input Dialog
    if (showCustomTargetDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTargetDialog = false },
            title = {
                Text(
                    text = "تحديد هدف التسبيح",
                    color = Color.White,
                    fontFamily = zekrFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "أدخل عدد التسبيحات المطلوب:",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        fontFamily = zekrFont
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTargetInput,
                        onValueChange = { customTargetInput = it.filter { ch -> ch.isDigit() }.take(5) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.30f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = customTargetInput.toIntOrNull()
                        if (num != null && num > 0) {
                            selectedTarget = num
                            isCustomFreeMode = false
                            val currentIt = currentItem
                            if (currentIt != null) {
                                scope.launch {
                                    prefRepo.saveTasbeehProgress(
                                        zekrId = currentIt.id,
                                        count = currentCount,
                                        target = num,
                                        isFreeMode = false
                                    )
                                }
                            }
                        }
                        showCustomTargetDialog = false
                    }
                ) {
                    Text(text = "تأكيد", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTargetDialog = false }) {
                    Text(text = "إلغاء", color = Color.White.copy(alpha = 0.60f))
                }
            },
            containerColor = Color(0xFF1A1D24),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Hadith Virtue and Full Context Information Dialog
    if (showVirtueInfoDialog && item != null) {
        AlertDialog(
            onDismissRequest = { showVirtueInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "📖", fontSize = 18.sp)
                    Text(
                        text = "تخريج وفضل الذكر",
                        color = Color.White,
                        fontFamily = zekrFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Full Text (Wrapped in Quran brackets if Ayah)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "النص الكامل:",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.getFormattedArabicText(),
                                color = Color.White,
                                fontFamily = zekrFont,
                                fontSize = 15.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }

                    // Virtue / Hadith Context (if available)
                    if (!item.virtueText.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.10f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "فضل الذكر وسياق الحديث:",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.virtueText,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontFamily = zekrFont,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    // Documented Count (if authentic)
                    if (docCount != null) {
                        Text(
                            text = "العدد الشرعي المنقول: ${ZekrRepository.toArabicDigits(docCount)} مرات",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "العدد الشرعي: غير محدد بلفظ الحديث (تسبيح مطلق)",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.5.sp
                        )
                    }

                    // Source and Reference
                    Text(
                        text = "المصدر: ${item.source}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "التخريج: ${item.reference}",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 11.5.sp
                    )

                    if (item.englishMeaning.isNotBlank()) {
                        Text(
                            text = item.englishMeaning,
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVirtueInfoDialog = false }) {
                    Text(text = "إغلاق", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF191C22),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

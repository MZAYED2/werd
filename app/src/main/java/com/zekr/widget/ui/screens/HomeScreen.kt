package com.zekr.widget.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.zekr.widget.data.model.WirdStats
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.GradientDirection
import com.zekr.widget.data.preferences.GradientPreset
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetFont
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.data.repository.AppUpdateInfo
import com.zekr.widget.data.repository.UpdateChecker
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.service.ScreenLockService
import com.zekr.widget.service.StyleRotationScheduler
import com.zekr.widget.ui.animation.bouncyClickable
import com.zekr.widget.ui.animation.shimmerGleam
import com.zekr.widget.ui.components.ColorPickerDialog
import com.zekr.widget.ui.theme.FontHelper
import com.zekr.widget.ui.theme.ZekrWidgetTheme
import kotlinx.coroutines.flow.flowOf
import com.zekr.widget.widget.ZekrGlanceWidget
import com.zekr.widget.widget.render.GlassRenderer
import com.zekr.widget.widget.render.PatternDrawables
import com.zekr.widget.widget.render.ZekrShareHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ColorPickerTarget {
    NONE,
    PRIMARY_SOLID,
    GRADIENT_START,
    GRADIENT_END
}

private val TABS = listOf(
    "النقوش",
    "الزجاج والألوان",
    "الخط",
    "المكتبة",
    "الوِرد اليومي",
    "الإعدادات"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isWidgetConfigMode: Boolean = false,
    widgetId: Int = -1,
    onSaveAndClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current
    val prefRepo = remember(isPreview) { if (isPreview) null else WidgetPreferencesRepository(context) }
    val initialSettings by (prefRepo?.settingsFlow ?: remember { flowOf(WidgetSettings()) }).collectAsState(initial = WidgetSettings())
    val savedItemId by (prefRepo?.currentItemIdFlow ?: remember { flowOf(1) }).collectAsState(initial = 1)
    val favoriteIds by (prefRepo?.favoriteIdsFlow ?: remember { flowOf(emptySet()) }).collectAsState(initial = emptySet())
    val pinnedItemId by (prefRepo?.pinnedItemIdFlow ?: remember { flowOf(null) }).collectAsState(initial = null)
    val currentRepCount by (prefRepo?.currentRepetitionFlow ?: remember { flowOf(0) }).collectAsState(initial = 0)
    val customItems by (prefRepo?.customItemsFlow ?: remember { flowOf(emptyList()) }).collectAsState(initial = emptyList())
    val wirdStats by (prefRepo?.wirdStatsFlow ?: remember { flowOf(WirdStats()) }).collectAsState(initial = WirdStats())
    val notifSettings by (prefRepo?.notificationSettingsFlow ?: remember { flowOf(com.zekr.widget.data.preferences.NotificationSettings()) }).collectAsState(initial = com.zekr.widget.data.preferences.NotificationSettings())

    var localSettings by remember { mutableStateOf<WidgetSettings?>(null) }
    LaunchedEffect(widgetId) {
        if (widgetId > 0 && !isPreview && prefRepo != null) {
            localSettings = prefRepo.getSettingsForWidget(widgetId)
        }
    }
    val settings = localSettings ?: initialSettings

    var previewItemId by rememberSaveable { mutableIntStateOf(1) }
    var hasInitializedItem by remember { mutableStateOf(false) }
    val pendingLocalIds = remember { ArrayDeque<Int>() }
    LaunchedEffect(savedItemId) {
        if (savedItemId <= 0) return@LaunchedEffect
        if (!hasInitializedItem) {
            previewItemId = savedItemId
            hasInitializedItem = true
            return@LaunchedEffect
        }
        if (pendingLocalIds.remove(savedItemId)) return@LaunchedEffect
        previewItemId = savedItemId
    }

    val hazeState = remember { HazeState() }

    val allItems = remember(customItems, settings) {
        ZekrRepository.getAllItems(context, customItems)
    }
    val currentItem = remember(previewItemId, customItems, settings) {
        ZekrRepository.getById(context, previewItemId, customItems)
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedBottomNav by rememberSaveable { mutableIntStateOf(0) }
    var colorPickerTarget by remember { mutableStateOf(ColorPickerTarget.NONE) }

    val currentVersionName = remember(isPreview) {
        if (isPreview) "1.1" else UpdateChecker.getCurrentVersionName(context)
    }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var isUpdateBannerDismissed by rememberSaveable { mutableStateOf(false) }

    fun triggerUpdateCheck(resetBannerDismissal: Boolean = false) {
        if (isPreview || isCheckingUpdate) return
        isCheckingUpdate = true
        updateError = null
        if (resetBannerDismissal) {
            isUpdateBannerDismissed = false
        }
        scope.launch {
            UpdateChecker.checkForUpdate(context)
                .onSuccess { info ->
                    updateInfo = info
                }
                .onFailure {
                    updateError = "تعذّر التحقق من وجود تحديث حالياً"
                }
            isCheckingUpdate = false
        }
    }

    LaunchedEffect(isPreview) {
        if (!isPreview) {
            triggerUpdateCheck()
        }
    }

    fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // Launch or stop Foreground Service when setting changes
    LaunchedEffect(settings.serviceEnabled, isPreview) {
        if (!isPreview) {
            if (settings.serviceEnabled) {
                ScreenLockService.start(context)
            } else {
                ScreenLockService.stop(context)
            }
        }
    }

    // Schedule / cancel periodic pattern + background rotation
    LaunchedEffect(
        settings.styleRotationEnabled,
        settings.styleRotationHours,
        isPreview
    ) {
        if (!isPreview) {
            StyleRotationScheduler.sync(
                context = context,
                enabled = settings.styleRotationEnabled,
                hours = settings.styleRotationHours
            )
        }
    }

    // Schedule / sync periodic smart reminders
    LaunchedEffect(notifSettings, isPreview) {
        if (!isPreview) {
            com.zekr.widget.notification.NotificationScheduler.sync(context, notifSettings)
        }
    }

    var syncJob by remember { mutableStateOf<Job?>(null) }

    fun updateAndSync(newSettings: WidgetSettings, immediate: Boolean = false) {
        localSettings = newSettings
        if (isPreview) return
        syncJob?.cancel()
        syncJob = scope.launch {
            if (!immediate) {
                delay(220)
            }
            if (widgetId > 0) {
                prefRepo?.saveSettingsForWidget(widgetId, newSettings)
                ZekrGlanceWidget.updateWidgetInstance(context, widgetId, newSettings)
            } else {
                prefRepo?.updateSettings(newSettings)
                ZekrGlanceWidget.updateAllWidgets(context, newSettings)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Color(0xFF0F1115),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ورد",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFFF2F4F7)
                                )
                                if (isWidgetConfigMode) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.18f))
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (widgetId > 0) "إعدادات الويدجت #$widgetId" else "إعدادات الويدجت",
                                            color = Color(0xFF6EE7B7),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "استوديو الويدجت الإسلامي",
                                fontSize = 11.sp,
                                color = Color(0xFFB4BAC6)
                            )
                        }
                    },
                    actions = {
                        if (isWidgetConfigMode) {
                            Button(
                                onClick = {
                                    syncJob?.cancel()
                                    if (!isPreview) {
                                        scope.launch {
                                            if (widgetId > 0) {
                                                prefRepo?.saveSettingsForWidget(widgetId, settings)
                                                ZekrGlanceWidget.updateWidgetInstance(context, widgetId, settings)
                                            } else {
                                                prefRepo?.updateSettings(settings)
                                                ZekrGlanceWidget.updateAllWidgets(context, settings)
                                            }
                                            onSaveAndClose()
                                        }
                                    } else {
                                        onSaveAndClose()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("تم وحفظ ✓", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        } else {
                            val todayCompleted = wirdStats.todayRecord().completedCount
                            val goal = wirdStats.dailyGoal.coerceAtLeast(1)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.padding(end = 10.dp)
                            ) {
                                // Daily Wird Progress Pill
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.14f))
                                        .border(
                                            1.dp,
                                            Color(0xFF10B981).copy(alpha = 0.42f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedTabIndex = 4 }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(color = Color(0xFF34D399))
                                        }
                                    }
                                    Text(
                                        text = "${ZekrRepository.toArabicDigits(todayCompleted)}/${ZekrRepository.toArabicDigits(goal)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0F1115),
                        titleContentColor = Color(0xFFF2F4F7),
                        actionIconContentColor = Color(0xFFF2F4F7)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF14181F),
                    contentColor = Color(0xFFF2F4F7),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedBottomNav == 0,
                        onClick = { selectedBottomNav = 0 },
                        icon = {
                            Text(
                                text = "📱",
                                fontSize = 18.sp
                            )
                        },
                        label = {
                            Text(
                                text = "الويدجت",
                                fontWeight = if (selectedBottomNav == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF10B981),
                            selectedTextColor = Color(0xFF10B981),
                            unselectedIconColor = Color(0xFF8B95A5),
                            unselectedTextColor = Color(0xFF8B95A5),
                            indicatorColor = Color(0xFF10B981).copy(alpha = 0.20f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedBottomNav == 1,
                        onClick = { selectedBottomNav = 1 },
                        icon = {
                            Text(
                                text = "📿",
                                fontSize = 18.sp
                            )
                        },
                        label = {
                            Text(
                                text = "السبحة",
                                fontWeight = if (selectedBottomNav == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF10B981),
                            selectedTextColor = Color(0xFF10B981),
                            unselectedIconColor = Color(0xFF8B95A5),
                            unselectedTextColor = Color(0xFF8B95A5),
                            indicatorColor = Color(0xFF10B981).copy(alpha = 0.20f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = selectedBottomNav,
                animationSpec = tween(durationMillis = 200),
                label = "bottom_nav_crossfade",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { currentNavTab ->
                if (currentNavTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                // Option B: Dismissible Frosted-Glass Update Banner above the Widget Preview
                val availableUpdate = updateInfo?.takeIf { it.isUpdateAvailable }
                AnimatedVisibility(
                    visible = availableUpdate != null && !isUpdateBannerDismissed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (availableUpdate != null) {
                        UpdateAvailableGlassBanner(
                            latestVersion = availableUpdate.latestVersion,
                            onDownloadClick = { openExternalUrl(availableUpdate.downloadUrl) },
                            onDismiss = { isUpdateBannerDismissed = true }
                        )
                    }
                }

                // Dedicated Widget Preview Section: Displays the card resting cleanly on the dark app background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    fun shufflePreviewAndWidgets(clearPin: Boolean) {
                        if (isPreview) {
                            val next = ZekrRepository.getRandomItem(
                                context = context,
                                includeQuran = settings.includeQuran,
                                includeHadith = settings.includeHadith,
                                includeAzkar = settings.includeAzkar,
                                smartTimeContextEnabled = settings.smartTimeContextEnabled,
                                favoritesOnlyMode = settings.favoritesOnlyMode,
                                favoriteIds = favoriteIds,
                                customItems = customItems,
                                currentId = previewItemId
                            )
                            previewItemId = next.id
                        } else {
                            scope.launch {
                                if (clearPin) {
                                    prefRepo?.setPinnedItemId(null)
                                }
                                val seenIds = prefRepo?.getSeenItemIds().orEmpty()
                                val result = ZekrRepository.selectNextFromPool(
                                    allItems = allItems,
                                    includeQuran = settings.includeQuran,
                                    includeHadith = settings.includeHadith,
                                    includeAzkar = settings.includeAzkar,
                                    smartTimeContextEnabled = settings.smartTimeContextEnabled,
                                    favoritesOnlyMode = settings.favoritesOnlyMode,
                                    favoriteIds = favoriteIds,
                                    currentId = previewItemId,
                                    seenIds = seenIds
                                )
                                previewItemId = result.item.id
                                pendingLocalIds.add(result.item.id)
                                prefRepo?.saveShuffleProgress(result.item.id, result.updatedSeenIds)
                                ZekrGlanceWidget.updateAllWidgets(context, settings, result.item.id)
                            }
                        }
                    }

                    LiveGlassWidgetPreview(
                        currentItem = currentItem,
                        settings = settings,
                        hazeState = hazeState,
                        currentRepetition = currentRepCount,
                        isFavorite = currentItem.id in favoriteIds,
                        isPinned = pinnedItemId == currentItem.id,
                        onBodyTap = {
                            shufflePreviewAndWidgets(clearPin = false)
                        },
                        onOpenTasbeeh = {
                            selectedBottomNav = 1
                        },
                        onToggleFavorite = {
                            if (!isPreview) {
                                scope.launch {
                                    prefRepo?.toggleFavorite(currentItem.id)
                                    ZekrGlanceWidget.updateAllWidgets(context, settings, currentItem.id)
                                }
                            }
                        },
                        onShare = {
                            if (!isPreview) {
                                scope.launch {
                                    ZekrShareHelper.shareZekrCard(context, currentItem, settings)
                                }
                            }
                        },
                        onSkip = { shufflePreviewAndWidgets(clearPin = true) }
                    )

                    // Pin Widget to Home Screen Button below Preview
                    Button(
                        onClick = {
                            val manager = context.getSystemService(android.appwidget.AppWidgetManager::class.java)
                            val provider = android.content.ComponentName(context, com.zekr.widget.widget.ZekrWidgetReceiver::class.java)
                            if (manager?.isRequestPinAppWidgetSupported == true) {
                                manager.requestPinAppWidget(provider, null, null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141F30)),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "➕ إضافة الويدجيت للشاشة الرئيسية",
                            color = Color(0xFF93C5FD),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Pinned Tab Navigation Bar
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = Color(0xFF0F1115),
                    divider = {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )
                    },
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        }
                    }
                ) {
                    TABS.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFB4BAC6)
                                )
                            }
                        )
                    }
                }

                // R1: Independent Scrolling Tab Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Crossfade(
                        targetState = selectedTabIndex,
                        animationSpec = tween(durationMillis = 180),
                        label = "tab_crossfade"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> PatternsTabContent(
                                settings = settings,
                                onUpdateSettings = { updateAndSync(it) }
                            )
                            1 -> GlassColorsTabContent(
                                settings = settings,
                                onUpdateSettings = { updateAndSync(it) },
                                onOpenColorPicker = { colorPickerTarget = it }
                            )
                            2 -> TypographyTabContent(
                                settings = settings,
                                onUpdateSettings = { updateAndSync(it) }
                            )
                            3 -> LibraryTabContent(
                                allItems = allItems,
                                favoriteIds = favoriteIds,
                                pinnedItemId = pinnedItemId,
                                settings = settings,
                                onToggleFavoritesOnlyMode = { enabled ->
                                    updateAndSync(settings.copy(favoritesOnlyMode = enabled), immediate = true)
                                },
                                onToggleFavorite = { item ->
                                    if (!isPreview) {
                                        scope.launch {
                                            prefRepo?.toggleFavorite(item.id)
                                            ZekrGlanceWidget.updateAllWidgets(context, settings)
                                        }
                                    }
                                },
                                onTogglePin = { item ->
                                    if (!isPreview) {
                                        scope.launch {
                                            val newPinId = if (pinnedItemId == item.id) null else item.id
                                            prefRepo?.setPinnedItemId(newPinId)
                                            if (newPinId != null) {
                                                previewItemId = newPinId
                                                pendingLocalIds.add(newPinId)
                                            }
                                            ZekrGlanceWidget.updateAllWidgets(context, settings, newPinId)
                                        }
                                    }
                                },
                                onShareItem = { item ->
                                    if (!isPreview) {
                                        scope.launch {
                                            ZekrShareHelper.shareZekrCard(context, item, settings)
                                        }
                                    }
                                },
                                onSaveCustomItem = { savedItem ->
                                    if (!isPreview) {
                                        scope.launch {
                                            val persisted = prefRepo?.upsertCustomItem(savedItem)
                                            if (persisted != null) {
                                                prefRepo.setPinnedItemId(persisted.id)
                                                previewItemId = persisted.id
                                                pendingLocalIds.add(persisted.id)
                                                ZekrGlanceWidget.updateAllWidgets(context, settings, persisted.id)
                                            }
                                        }
                                    }
                                },
                                onDeleteCustomItem = { item ->
                                    if (!isPreview) {
                                        scope.launch {
                                            prefRepo?.deleteCustomItem(item.id)
                                            ZekrGlanceWidget.updateAllWidgets(context, settings)
                                        }
                                    }
                                }
                            )
                            4 -> DailyWirdTabContent(
                                stats = wirdStats,
                                onUpdateDailyGoal = { newGoal ->
                                    if (!isPreview) {
                                        scope.launch {
                                            prefRepo?.updateDailyGoal(newGoal)
                                        }
                                    }
                                }
                            )
                            5 -> SettingsContentTabContent(
                                settings = settings,
                                onUpdateSettings = { updateAndSync(it) },
                                notifSettings = notifSettings,
                                onUpdateNotifSettings = { newNotif ->
                                    if (!isPreview) {
                                        scope.launch {
                                            prefRepo?.saveNotificationSettings(newNotif)
                                        }
                                    }
                                },
                                onTestReminder = {
                                    if (!isPreview) {
                                        val active = listOfNotNull(
                                            com.zekr.widget.data.preferences.ReminderType.SALAWAT.takeIf { notifSettings.salawatEnabled },
                                            com.zekr.widget.data.preferences.ReminderType.TASBEEH.takeIf { notifSettings.tasbeehEnabled },
                                            com.zekr.widget.data.preferences.ReminderType.WIRD.takeIf { notifSettings.wirdEnabled }
                                        )
                                        val target = active.firstOrNull() ?: com.zekr.widget.data.preferences.ReminderType.SALAWAT
                                        com.zekr.widget.notification.NotificationHelper.showReminder(context, target)
                                    }
                                },
                                currentVersionName = currentVersionName,
                                updateInfo = updateInfo,
                                isCheckingUpdate = isCheckingUpdate,
                                updateError = updateError,
                                onCheckForUpdate = { triggerUpdateCheck(resetBannerDismissal = true) },
                                onOpenUrl = { openExternalUrl(it) }
                            )
                        }
                    }
                }
            }
        } else {
            com.zekr.widget.ui.tasbeeh.TasbeehScreen(
                settings = settings,
                onBackToStudio = { selectedBottomNav = 0 }
            )
        }
    }
}
}

    // Color Picker Dialog Instance
    if (colorPickerTarget != ColorPickerTarget.NONE) {
        val initialPickerColor = when (colorPickerTarget) {
            ColorPickerTarget.PRIMARY_SOLID,
            ColorPickerTarget.GRADIENT_START -> settings.primaryColor
            ColorPickerTarget.GRADIENT_END -> settings.secondaryColor
            ColorPickerTarget.NONE -> settings.primaryColor
        }
        val pickerTitle = when (colorPickerTarget) {
            ColorPickerTarget.PRIMARY_SOLID -> "اختيار لون الزجاج المخصص"
            ColorPickerTarget.GRADIENT_START -> "تحديد لون بداية التدرج"
            ColorPickerTarget.GRADIENT_END -> "تحديد لون نهاية التدرج"
            ColorPickerTarget.NONE -> "اختيار لون مخصص"
        }

        ColorPickerDialog(
            initialColor = initialPickerColor,
            title = pickerTitle,
            onDismiss = { colorPickerTarget = ColorPickerTarget.NONE },
            onColorSelected = { selectedColor ->
                when (colorPickerTarget) {
                    ColorPickerTarget.PRIMARY_SOLID -> {
                        updateAndSync(
                            settings.copy(
                                isGradient = false,
                                primaryColor = selectedColor
                            ),
                            immediate = true
                        )
                    }
                    ColorPickerTarget.GRADIENT_START -> {
                        updateAndSync(
                            settings.copy(
                                isGradient = true,
                                primaryColor = selectedColor
                            ),
                            immediate = true
                        )
                    }
                    ColorPickerTarget.GRADIENT_END -> {
                        updateAndSync(
                            settings.copy(
                                isGradient = true,
                                secondaryColor = selectedColor
                            ),
                            immediate = true
                        )
                    }
                    ColorPickerTarget.NONE -> {}
                }
            }
        )
    }
}

/**
 * Tab 0: Islamic Patterns & Arabesques
 */
@Composable
fun PatternsTabContent(
    settings: WidgetSettings,
    onUpdateSettings: (WidgetSettings) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ModernSectionCard(
                title = "النقوش والزخارف الإسلامية",
                subtitle = "اختر من 9 نقوش هندسية ملكية أو نقوش خطية متقنة"
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PatternType.values()) { pattern ->
                        val isSelected = pattern == settings.pattern
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "pattern_card_scale"
                        )
                        val borderWidth by animateDpAsState(
                            targetValue = if (isSelected) 2.5.dp else 1.dp,
                            label = "pattern_card_border"
                        )

                        Card(
                            modifier = Modifier
                                .width(128.dp)
                                .height(90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .bouncyClickable {
                                    onUpdateSettings(settings.copy(pattern = pattern))
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(pattern.previewBgColor)
                            ),
                            border = BorderStroke(
                                borderWidth,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                if (pattern.vectorResId != null) {
                                    Image(
                                        painter = painterResource(id = pattern.vectorResId),
                                        contentDescription = pattern.titleArabic,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                            .graphicsLayer { alpha = 0.85f }
                                    )
                                } else if (pattern == PatternType.NONE) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "بدون نقش",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                } else {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawIntoCanvas { composeCanvas ->
                                            PatternDrawables.drawPattern(
                                                canvas = composeCanvas.nativeCanvas,
                                                pattern = pattern,
                                                width = size.width,
                                                height = size.height,
                                                patternColor = android.graphics.Color.WHITE,
                                                alpha = 115,
                                                scale = 0.65f,
                                                context = context
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.75f),
                                                    Color.Black.copy(alpha = 0.92f)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pattern.titleArabic,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وضوح النقش الإسلامي",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(settings.patternOpacity * 100).toInt()}%",
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.patternOpacity,
                    onValueChange = { newOpacity ->
                        onUpdateSettings(settings.copy(patternOpacity = newOpacity))
                    },
                    valueRange = 0f..0.85f
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حجم وتكبير النقش",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.1f", settings.patternScale)}x",
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.patternScale,
                    onValueChange = { newScale ->
                        onUpdateSettings(settings.copy(patternScale = newScale))
                    },
                    valueRange = 0.5f..2.2f
                )
            }
        }
    }
}

/**
 * Tab 1: Glass Colors, Presets, Gradients & Custom Color Picker
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassColorsTabContent(
    settings: WidgetSettings,
    onUpdateSettings: (WidgetSettings) -> Unit,
    onOpenColorPicker: (ColorPickerTarget) -> Unit
) {
    val configuration = LocalConfiguration.current
    val compact = configuration.screenWidthDp < 380
    val contentPadding = if (compact) 12.dp else 16.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(contentPadding),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
    ) {
        item {
            ModernSectionCard(
                title = "طابع ولون الزجاج",
                subtitle = "اختر لوناً أحادياً أو تدرجاً ملكياً، أو خصّص لوناً يدوياً"
            ) {
                // Segmented Switcher: Solid vs Gradient
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isSolid = !settings.isGradient
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSolid) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .bouncyClickable {
                                if (settings.isGradient) {
                                    onUpdateSettings(settings.copy(isGradient = false))
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (compact) "أحادي" else "ألوان أحادية",
                            fontSize = if (compact) 12.sp else 13.sp,
                            fontWeight = if (isSolid) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSolid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    val isGrad = settings.isGradient
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isGrad) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .bouncyClickable {
                                if (!settings.isGradient) {
                                    onUpdateSettings(settings.copy(isGradient = true))
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (compact) "تدرج" else "تدرجات لونية",
                            fontSize = if (compact) 12.sp else 13.sp,
                            fontWeight = if (isGrad) FontWeight.Bold else FontWeight.Medium,
                            color = if (isGrad) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!settings.isGradient) {
                    val matchesPreset = GlassColorPreset.values().any {
                        settings.colorPreset == it && settings.primaryColor == it.hexColor
                    }

                    // Fixed-height row so selection scale does not jump layout
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(GlassColorPreset.values()) { preset ->
                            val isSelected = settings.colorPreset == preset && settings.primaryColor == preset.hexColor
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.08f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "solid_color_scale"
                            )
                            val borderWidth by animateDpAsState(
                                targetValue = if (isSelected) 3.dp else 1.dp,
                                label = "solid_color_border"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .bouncyClickable {
                                        onUpdateSettings(
                                            settings.copy(
                                                isGradient = false,
                                                colorPreset = preset,
                                                primaryColor = preset.hexColor
                                            )
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.size(52.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .clip(CircleShape)
                                            .background(Color(preset.hexColor))
                                            .border(
                                                width = borderWidth,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.White.copy(alpha = 0.28f)
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Text(
                                    text = preset.titleArabic,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onOpenColorPicker(ColorPickerTarget.PRIMARY_SOLID) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(settings.primaryColor))
                                    .border(
                                        width = if (matchesPreset) 1.dp else 2.dp,
                                        color = if (matchesPreset) {
                                            Color.White.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (matchesPreset) {
                                    if (compact) "تخصيص لون" else "تخصيص لون يدوي"
                                } else {
                                    if (compact) "لون مخصص" else "تعديل اللون المخصص"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(GradientPreset.values()) { preset ->
                            val isSelected = settings.primaryColor == preset.startColor &&
                                settings.secondaryColor == preset.endColor
                            val borderWidth by animateDpAsState(
                                targetValue = if (isSelected) 2.5.dp else 1.dp,
                                label = "gradient_preset_border"
                            )

                            Card(
                                modifier = Modifier
                                    .width(if (compact) 118.dp else 136.dp)
                                    .height(64.dp)
                                    .bouncyClickable {
                                        onUpdateSettings(
                                            settings.copy(
                                                isGradient = true,
                                                primaryColor = preset.startColor,
                                                secondaryColor = preset.endColor,
                                                gradientDirection = preset.defaultDirection
                                            )
                                        )
                                    },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    borderWidth,
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.15f)
                                    }
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(preset.startColor), Color(preset.endColor))
                                            )
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.titleArabic,
                                        fontSize = if (compact) 10.sp else 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "اتجاه التدرج",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradientDirection.values().forEach { dir ->
                            val isSelected = settings.gradientDirection == dir
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onUpdateSettings(settings.copy(gradientDirection = dir))
                                },
                                label = {
                                    Text(
                                        text = "${dir.symbol} ${dir.titleArabic}",
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenColorPicker(ColorPickerTarget.GRADIENT_START) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(settings.primaryColor))
                                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                )
                                Text(
                                    text = if (compact) "البداية" else "لون البداية",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onOpenColorPicker(ColorPickerTarget.GRADIENT_END) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(settings.secondaryColor))
                                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                )
                                Text(
                                    text = if (compact) "النهاية" else "لون النهاية",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            ModernSectionCard(
                title = "درجة شفافية الزجاج",
                subtitle = "تحكم في نسبة انعكاس ووضوح الخلفية الزجاجية"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مستوى التعتيم الزجاجي",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${(settings.glassOpacity * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = settings.glassOpacity,
                    onValueChange = { newOpacity ->
                        onUpdateSettings(settings.copy(glassOpacity = newOpacity))
                    },
                    valueRange = 0.15f..0.90f
                )
            }
        }

        item {
            ModernSectionCard(
                title = "درجة ضبابية الزجاج",
                subtitle = "تمويه الخلفية من صافٍ إلى مصنفر بالكامل"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مستوى التمويه",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${(settings.blurGrade * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = settings.blurGrade,
                    onValueChange = { newBlur ->
                        onUpdateSettings(settings.copy(blurGrade = newBlur))
                    },
                    valueRange = 0.0f..1.0f
                )

                Spacer(modifier = Modifier.height(8.dp))

                val blurPresets = listOf(
                    0.0f to "0%",
                    0.25f to "25%",
                    0.50f to "50%",
                    0.75f to "75%",
                    1.0f to "100%"
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    blurPresets.forEach { (grade, label) ->
                        val isSelected = ((settings.blurGrade * 100).toInt() == (grade * 100).toInt())
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onUpdateSettings(settings.copy(blurGrade = grade))
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Expanded Arabic Typography Selection & Widget Text Spacing
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypographyTabContent(
    settings: WidgetSettings,
    onUpdateSettings: (WidgetSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ModernSectionCard(
                title = "نوع الخط العربي",
                subtitle = "قرآني، مخطوط، كوفي، أدبي، عصري، تقني، وزخرفي — اختر ما يناسب ذوقك"
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetFont.values().forEach { font ->
                        val isSelected = font == settings.font
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateSettings(settings.copy(font = font)) },
                            label = {
                                Text(
                                    text = font.titleArabic,
                                    fontFamily = FontHelper.getComposeFontFamily(font),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حجم الخط في الويدجت",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${settings.fontSizeSp} sp",
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.fontSizeSp.toFloat(),
                    onValueChange = { newSize ->
                        onUpdateSettings(settings.copy(fontSizeSp = newSize.toInt()))
                    },
                    valueRange = 14f..26f,
                    steps = 11
                )
            }
        }
    }
}

/**
 * Option B: Slim, dismissible frosted-glass banner displayed above the live widget preview
 * when a newer GitHub Release is detected.
 */
@Composable
fun UpdateAvailableGlassBanner(
    latestVersion: String,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentGold = Color(0xFFD4AF37)
    val accentEmerald = Color(0xFF10B981)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF162220).copy(alpha = 0.92f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    accentGold.copy(alpha = 0.65f),
                    accentEmerald.copy(alpha = 0.45f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentGold.copy(alpha = 0.16f))
                    .border(1.dp, accentGold.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⬇",
                    color = accentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "يتوفر تحديث جديد لتطبيق ورد ($latestVersion)",
                    color = Color(0xFFF5F7FA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
                Text(
                    text = "حمّل أحدث نسخة مباشرة من GitHub",
                    color = Color(0xFFB4BAC6),
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onDownloadClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentGold,
                    contentColor = Color(0xFF0F1115)
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "تحديث الآن",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = Color(0xFFB4BAC6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tab 3: Content Categories Filter, Screen Lock Shuffle & App Updates
 */
@Composable
fun SettingsContentTabContent(
    settings: WidgetSettings,
    onUpdateSettings: (WidgetSettings) -> Unit,
    notifSettings: com.zekr.widget.data.preferences.NotificationSettings = com.zekr.widget.data.preferences.NotificationSettings(),
    onUpdateNotifSettings: (com.zekr.widget.data.preferences.NotificationSettings) -> Unit = {},
    onTestReminder: () -> Unit = {},
    currentVersionName: String = "1.1",
    updateInfo: AppUpdateInfo? = null,
    isCheckingUpdate: Boolean = false,
    updateError: String? = null,
    onCheckForUpdate: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ModernSectionCard(
                title = "التنبيهات والتذكيرات الذكية",
                subtitle = "تذكيرات دورية لطيفة مع خيار «تم» لتأكيد الإنجاز و«فتح السبحة»"
            ) {
                // 1. Salawat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الصلاة على النبي ﷺ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "«هل صليت على النبي اليوم؟ اللهم صلّ وسلم على نبينا محمد»",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifSettings.salawatEnabled,
                        onCheckedChange = {
                            onUpdateNotifSettings(notifSettings.copy(salawatEnabled = it))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Tasbeeh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تذكير التسبيح اليومي",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "«هل سبّحت اليوم؟ سبحان الله وبحمده، ألا بذكر الله تطمئن القلوب»",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifSettings.tasbeehEnabled,
                        onCheckedChange = {
                            onUpdateNotifSettings(notifSettings.copy(tasbeehEnabled = it))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Wird
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تذكير الورد القرآني",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "«هل قرأت وردك اليوم؟ رتّل وارتقِ بآيات الذكر الحكيم»",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifSettings.wirdEnabled,
                        onCheckedChange = {
                            onUpdateNotifSettings(notifSettings.copy(wirdEnabled = it))
                        }
                    )
                }

                val anyNotifActive = notifSettings.salawatEnabled || notifSettings.tasbeehEnabled || notifSettings.wirdEnabled
                if (anyNotifActive) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "التكرار اليومي للتذكيرات",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${notifSettings.dailyCount} مرات يومياً",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = notifSettings.dailyCount.toFloat(),
                        onValueChange = {
                            onUpdateNotifSettings(notifSettings.copy(dailyCount = it.toInt()))
                        },
                        valueRange = 1f..5f,
                        steps = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ساعات الهدوء (11 م - 7 ص)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إيقاف التنبيهات تلقائياً أثناء ساعات النوم والراحة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifSettings.quietHoursEnabled,
                            onCheckedChange = {
                                onUpdateNotifSettings(notifSettings.copy(quietHoursEnabled = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onTestReminder,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "🔔 تجربة إشعار تذكيري الآن",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                    }
                }
            }
        }

        item {
            ModernSectionCard(
                title = "المحتوى المعروض",
                subtitle = "مأخوذ حصراً من القرآن الكريم وصحيحي البخاري ومسلم"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val canToggleQuran = !settings.includeQuran || settings.includeHadith || settings.includeAzkar
                    FilterChip(
                        selected = settings.includeQuran,
                        onClick = {
                            if (canToggleQuran) {
                                onUpdateSettings(settings.copy(includeQuran = !settings.includeQuran))
                            }
                        },
                        label = { Text("القرآن الكريم") }
                    )
                    val canToggleHadith = !settings.includeHadith || settings.includeQuran || settings.includeAzkar
                    FilterChip(
                        selected = settings.includeHadith,
                        onClick = {
                            if (canToggleHadith) {
                                onUpdateSettings(settings.copy(includeHadith = !settings.includeHadith))
                            }
                        },
                        label = { Text("الحديث الصحيح") }
                    )
                    val canToggleAzkar = !settings.includeAzkar || settings.includeQuran || settings.includeHadith
                    FilterChip(
                        selected = settings.includeAzkar,
                        onClick = {
                            if (canToggleAzkar) {
                                onUpdateSettings(settings.copy(includeAzkar = !settings.includeAzkar))
                            }
                        },
                        label = { Text("الأذكار المأثورة") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FilterChip(
                    selected = settings.favoritesOnlyMode,
                    onClick = {
                        onUpdateSettings(settings.copy(favoritesOnlyMode = !settings.favoritesOnlyMode))
                    },
                    label = { Text("قصر الويدجت على المفضلة فقط ♥") }
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.04f))
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تخصيص الأذكار حسب الوقت",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "إعطاء الأولوية لأذكار الصباح صباحاً، والمساء عصراً، والنوم والحفظ ليلاً مع إخفاء أذكار الوقت المعاكس",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.smartTimeContextEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(settings.copy(smartTimeContextEnabled = enabled))
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.04f))
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "التبديل التلقائي عند فتح قفل الشاشة",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تحديث الذكر مرة واحدة فقط عند فتح قفل الهاتف دون تكرار قبل إتمام القائمة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.serviceEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(settings.copy(serviceEnabled = enabled))
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.04f))
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تدوير النقوش والخلفية تلقائياً",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تبديل النقش ولون الخلفية معاً كل عدد معيّن من الساعات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.styleRotationEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettings(settings.copy(styleRotationEnabled = enabled))
                            }
                        )
                    }

                    if (settings.styleRotationEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "كل كم ساعة؟",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val hourOptions = listOf(1, 2, 3, 6, 12, 24)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(hourOptions) { hours ->
                                FilterChip(
                                    selected = settings.styleRotationHours == hours,
                                    onClick = {
                                        onUpdateSettings(
                                            settings.copy(styleRotationHours = hours)
                                        )
                                    },
                                    label = {
                                        Text(
                                            if (hours == 1) "ساعة" else "$hours ساعات"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            val isUpdateAvailable = updateInfo?.isUpdateAvailable == true
            val accentGold = Color(0xFFD4AF37)
            val accentEmerald = Color(0xFF10B981)

            ModernSectionCard(
                title = "إصدار التطبيق والتحديثات",
                subtitle = "الإصدار الحالي: v$currentVersionName"
            ) {
                // Status row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isUpdateAvailable) accentGold.copy(alpha = 0.12f)
                            else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isUpdateAvailable) accentGold.copy(alpha = 0.45f)
                            else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val statusTitle = when {
                            isCheckingUpdate -> "جاري التحقق من التحديثات..."
                            isUpdateAvailable -> "يتوفر تحديث جديد (v${updateInfo.latestVersion}) 🎉"
                            updateError != null -> updateError
                            updateInfo != null -> "أنت تستخدم أحدث إصدار من التطبيق ✓"
                            else -> "تحقق من توفر إصدارات أحدث على GitHub"
                        }
                        val statusColor = when {
                            isUpdateAvailable -> accentGold
                            updateError != null -> Color(0xFFFCA5A5)
                            updateInfo != null -> accentEmerald
                            else -> Color(0xFFE4E7EC)
                        }
                        Text(
                            text = statusTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = statusColor
                        )

                        if (isUpdateAvailable && updateInfo.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = updateInfo.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD0D5DD),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isUpdateAvailable) {
                        Button(
                            onClick = { onOpenUrl(updateInfo.downloadUrl) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentGold,
                                contentColor = Color(0xFF0F1115)
                            )
                        ) {
                            Text(
                                text = "تحميل التحديث (v${updateInfo.latestVersion})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onCheckForUpdate,
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isCheckingUpdate) "جاري الفحص..." else "التحقق من التحديثات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onOpenUrl(updateInfo?.releasePageUrl ?: UpdateChecker.RELEASES_PAGE_URL)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Text(
                            text = "صفحة GitHub",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFFE4E7EC)
                        )
                    }
                }
            }
        }

        item {
            var showImportDialog by remember { mutableStateOf(false) }
            var importJsonText by remember { mutableStateOf("") }
            var importStatus by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefRepo = remember { WidgetPreferencesRepository(context.applicationContext) }

            ModernSectionCard(
                title = "النسخ الاحتياطي واستعادة البيانات",
                subtitle = "حفظ إعداداتك ومفضلاتك وسجل التسبيحات ونقلها لأي جهاز آخر"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val jsonStr = prefRepo.exportUserDataJson()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Werd Settings Backup", jsonStr)
                                clipboard?.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "تم نسخ بيانات الإعدادات للحافظة بنجاح ✓", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Text("📤 نسخ احتياطي", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF6EE7B7))
                    }

                    OutlinedButton(
                        onClick = {
                            showImportDialog = true
                            importStatus = null
                            importJsonText = ""
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Text("📥 استعادة نسخة", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
                    }
                }

                if (showImportDialog) {
                    AlertDialog(
                        onDismissRequest = { showImportDialog = false },
                        title = { Text("استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("الصق كود النسخة الاحتياطية (JSON) المستخرج سابقاً:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = importJsonText,
                                    onValueChange = { importJsonText = it },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    placeholder = { Text("الصق النص هنا...") },
                                    maxLines = 6
                                )
                                if (importStatus != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(importStatus!!, color = Color(0xFFF87171), fontSize = 12.sp)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = prefRepo.importUserDataJson(importJsonText)
                                        if (res.isSuccess) {
                                            android.widget.Toast.makeText(context, "تمت استعادة الإعدادات بنجاح ✓", android.widget.Toast.LENGTH_LONG).show()
                                            showImportDialog = false
                                        } else {
                                            importStatus = "تعذّر قراءة كود النسخة الاحتياطية، تأكد من صحة النص"
                                        }
                                    }
                                }
                            ) { Text("استعادة الآن") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportDialog = false }) { Text("إلغاء") }
                        }
                    )
                }
            }
        }

        item {
            ModernSectionCard(
                title = "ويدجيت شاشة القفل والسبحة السريعة",
                subtitle = "مدعومة رسمياً على Samsung One UI 6.1 و One UI 7 (Galaxy S24 / S25 Ultra) وأندرويد المدعوم"
            ) {
                Text(
                    text = "• تتيح لك شاشة القفل قراءة الذكر والتسبيح بنقرة واحدة سريعة مع اهتزاز تفاعلي دون الحاجة لفتح قفل الهاتف نهائياً.\n• لإضافتها على هواتف سامسونج: اضغط مطولاً على شاشة القفل ثم اختر «إضافة ويدجيت» أسفل الساعة واختر «ورد» (Werd).\n• على الأجهزة التي لا تدعم ويدجيت شاشة القفل من النظام، يمكن إضافة الويدجيت المصغر على الشاشة الرئيسية.",
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFD0D5DD)
                )
            }
        }
    }
}

/**
 * Modern Frosted Glass Card Container with subtle specular borders and spring interactions
 */
@Composable
fun ModernSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val compact = configuration.screenWidthDp < 380
    val cardPadding = if (compact) 12.dp else 16.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16181E)
        ),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF2F4F7),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB4BAC6),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
            content()
        }
    }
}

/**
 * R2: Pinned Always-Visible Live Glass Widget Preview
 * Synchronously reflects every styling, color, pattern, and typography choice in real time.
 */
@Composable
fun LiveGlassWidgetPreview(
    currentItem: ZekrEntity,
    settings: WidgetSettings,
    hazeState: HazeState,
    currentRepetition: Int = 0,
    isFavorite: Boolean = false,
    isPinned: Boolean = false,
    onBodyTap: () -> Unit = {},
    onOpenTasbeeh: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onShare: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current

    val bitmap: Bitmap = remember(
        settings.colorPreset,
        settings.isGradient,
        settings.primaryColor,
        settings.secondaryColor,
        settings.gradientDirection,
        settings.glassOpacity,
        settings.blurGrade,
        settings.pattern,
        settings.patternOpacity,
        settings.patternScale
    ) {
        GlassRenderer.createGlassBackgroundBitmap(
            width = 680,
            height = 380,
            colorPreset = settings.colorPreset,
            isGradient = settings.isGradient,
            primaryColor = settings.primaryColor,
            secondaryColor = settings.secondaryColor,
            gradientDirection = settings.gradientDirection,
            opacity = settings.glassOpacity,
            blurGrade = settings.blurGrade,
            pattern = settings.pattern,
            patternOpacity = settings.patternOpacity,
            patternScale = settings.patternScale,
            cornerRadius = 56f,
            context = context
        )
    }

    val targetTextColor = Color(settings.getAdaptiveTextColor())
    val targetAccentColor = Color(settings.getAdaptiveAccentColor())

    val animatedTextColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "preview_text_color"
    )

    // Phone Home Screen Wallpaper Container (the backdrop behind the widget)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
    ) {
        // 1. Wallpaper Backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        ) {
            WallpaperBackdrop()
        }

        // 2. Realistic floating widget card with genuine backdrop blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(22.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeDefaults.style(
                        backgroundColor = Color.Transparent,
                        blurRadius = (settings.blurGrade * 42).coerceAtLeast(0f).dp,
                        noiseFactor = (settings.blurGrade * 0.10f).coerceAtLeast(0f)
                    )
                )
                .bouncyClickable { onBodyTap() }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .shimmerGleam(
                        highlightColor = Color.White.copy(alpha = 0.22f),
                        durationMillis = 2800
                    )
            )

            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(animatedTextColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "مثبّت",
                        color = targetAccentColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Fluid Animated Content + Interactive Bottom Bar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = currentItem,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) { height -> height / 3 } + fadeIn())
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { height -> -height / 3 } + fadeOut()
                            )
                    },
                    label = "zekr_animated_transition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { item ->
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val density = LocalDensity.current
                        val boxMaxH = this.maxHeight
                        val boxMaxW = this.maxWidth
                        val heightScale = (boxMaxH / 108.dp).coerceIn(0.72f, 1.12f)
                        val widthScale = (boxMaxW / 280.dp).coerceIn(0.78f, 1.08f)
                        val fitScale = minOf(heightScale, widthScale)
                        val fittedFontSp = (settings.fontSizeSp * fitScale).coerceIn(12f, settings.fontSizeSp.toFloat() + 2f)
                        val calculatedLineHeightSp = fittedFontSp * 1.38f
                        val lineHeightDp = with(density) { calculatedLineHeightSp.sp.toDp() }
                        val maxFitLines = if (lineHeightDp.value > 0) {
                            (boxMaxH / lineHeightDp).toInt().coerceIn(1, 7)
                        } else 4

                        Text(
                            text = item.arabicText,
                            color = animatedTextColor,
                            fontSize = fittedFontSp.sp,
                            fontFamily = FontHelper.getComposeFontFamily(settings.font),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            lineHeight = calculatedLineHeightSp.sp,
                            maxLines = maxFitLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Repetition Progress Pill (shown when repetition > 1)
                if (currentItem.repetition > 1) {
                    val safeCurrent = currentRepetition.coerceIn(0, currentItem.repetition)
                    val progress = (safeCurrent.toFloat() / currentItem.repetition.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val label = if (safeCurrent > 0) {
                        "مسبحة ${ZekrRepository.toArabicDigits(safeCurrent)}/${ZekrRepository.toArabicDigits(currentItem.repetition)}"
                    } else {
                        "مسبحة (${ZekrRepository.toArabicDigits(currentItem.repetition)})"
                    }
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(animatedTextColor.copy(alpha = 0.16f))
                            .border(1.dp, targetAccentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .bouncyClickable { onOpenTasbeeh() }
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .fillMaxWidth(progress)
                                .background(targetAccentColor.copy(alpha = 0.35f))
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "📿",
                                fontSize = 10.sp
                            )
                            Text(
                                text = label,
                                color = animatedTextColor,
                                fontFamily = FontHelper.getComposeFontFamily(settings.font),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Interactive Bottom Action Bar: [Favorite, Share, Tasbeeh] --- Citation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFavorite) Color(0xFFFF5C7A).copy(alpha = 0.26f)
                                    else animatedTextColor.copy(alpha = 0.14f)
                                )
                                .border(
                                    1.dp,
                                    if (isFavorite) Color(0xFFFF768E).copy(alpha = 0.6f)
                                    else animatedTextColor.copy(alpha = 0.22f),
                                    CircleShape
                                )
                                .clickable { onToggleFavorite() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "المفضلة",
                                tint = if (isFavorite) Color(0xFFFF6B86) else animatedTextColor.copy(alpha = 0.9f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(animatedTextColor.copy(alpha = 0.14f))
                                .border(1.dp, animatedTextColor.copy(alpha = 0.22f), CircleShape)
                                .clickable { onShare() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "مشاركة",
                                tint = animatedTextColor.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(targetAccentColor.copy(alpha = 0.20f))
                                .border(1.dp, targetAccentColor.copy(alpha = 0.50f), CircleShape)
                                .clickable { onOpenTasbeeh() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📿",
                                fontSize = 13.sp
                            )
                        }
                    }

                    Text(
                        text = currentItem.getConciseCitation(),
                        color = animatedTextColor.copy(alpha = 0.88f),
                        fontFamily = FontHelper.getComposeFontFamily(settings.font),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Realistic Smartphone Wallpaper Backdrop for previewing genuine frosted glass backdrop blur
 */
@Composable
fun WallpaperBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Deep atmospheric sunset sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E293B),
                    Color(0xFF334155),
                    Color(0xFF475569)
                )
            )
        )

        // 2. Glowing celestial sun/moon disc (distinct luminous shape that visibly blurs under the card)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD54F),
                    Color(0xFFFFB74D).copy(alpha = 0.85f),
                    Color(0xFFFF7043).copy(alpha = 0.40f),
                    Color.Transparent
                ),
                center = Offset(w * 0.78f, h * 0.28f),
                radius = w * 0.30f
            ),
            center = Offset(w * 0.78f, h * 0.28f),
            radius = w * 0.30f
        )

        // 3. Ambient emerald/teal light accent on the opposite corner
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4DB6AC).copy(alpha = 0.45f),
                    Color(0xFF00897B).copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = Offset(w * 0.18f, h * 0.75f),
                radius = w * 0.38f
            ),
            center = Offset(w * 0.18f, h * 0.75f),
            radius = w * 0.38f
        )

        // 4. Mountain/dune landscape silhouette across the bottom
        val mountainPath = Path().apply {
            moveTo(0f, h * 0.68f)
            cubicTo(w * 0.25f, h * 0.52f, w * 0.55f, h * 0.80f, w, h * 0.58f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = mountainPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1E1B4B).copy(alpha = 0.85f), Color(0xFF0F0E26))
            )
        )
    }
}

// =========================================================================
// Live Compose Previews (Interactive & Design Preview Modes)
// =========================================================================

@Preview(
    name = "استوديو ورد - التطبيق كاملاً (Live App View)",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun HomeScreenLivePreview() {
    ZekrWidgetTheme(darkTheme = true) {
        HomeScreen()
    }
}

@Preview(
    name = "بطاقة الويدجت التفاعلية (Interactive Widget Card)",
    showBackground = true,
    backgroundColor = 0xFF0F1115
)
@Composable
fun LiveGlassWidgetCardPreview() {
    ZekrWidgetTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LiveGlassWidgetPreview(
                currentItem = ZekrRepository.getById(LocalContext.current, 1),
                settings = WidgetSettings(),
                hazeState = remember { HazeState() },
                currentRepetition = 1,
                isFavorite = true,
                onBodyTap = {}
            )
        }
    }
}

@Preview(
    name = "وضع تعديل الويدجت (Widget Config Mode)",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun HomeScreenConfigModePreview() {
    ZekrWidgetTheme(darkTheme = true) {
        HomeScreen(isWidgetConfigMode = true)
    }
}


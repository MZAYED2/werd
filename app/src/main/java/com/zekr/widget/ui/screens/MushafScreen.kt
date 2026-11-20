package com.zekr.widget.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.mushaf.MushafData
import com.zekr.widget.mushaf.MushafPageLoader
import com.zekr.widget.mushaf.PageHeaderInfo
import kotlinx.coroutines.launch

@Composable
fun MushafScreen(
    initialPage: Int = 1,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { WidgetPreferencesRepository(context) }

    val savedLastPage by repo.mushafLastPageFlow.collectAsState(initial = initialPage)
    val savedBookmarkPage by repo.mushafBookmarkPageFlow.collectAsState(initial = 0)
    val readingTheme by repo.mushafReadingThemeFlow.collectAsState(initial = 0)

    val startPage = remember(savedLastPage, initialPage) {
        if (initialPage > 1) initialPage else savedLastPage
    }

    val pagerState = rememberPagerState(
        initialPage = (startPage - 1).coerceIn(0, MushafData.TOTAL_PAGES - 1)
    ) {
        MushafData.TOTAL_PAGES
    }

    val currentPageNumber = pagerState.currentPage + 1
    val headerInfo = remember(currentPageNumber) {
        MushafData.getPageHeaderInfo(currentPageNumber)
    }

    // Auto-save last read page to DataStore and preload adjacent
    LaunchedEffect(currentPageNumber) {
        repo.saveMushafLastPage(currentPageNumber)
        MushafPageLoader.preloadAdjacent(context, currentPageNumber)
    }

    // Auto-restore screen brightness on exit
    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    var showToolbars by remember { mutableStateOf(true) }
    var showIndexDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showConfirmWirdDialog by remember { mutableStateOf(false) }

    val isBookmarkedHere = savedBookmarkPage == currentPageNumber

    if (showIndexDialog) {
        MushafIndexDialog(
            currentPage = currentPageNumber,
            onSelectPage = { targetPage ->
                showIndexDialog = false
                scope.launch {
                    pagerState.scrollToPage(targetPage - 1)
                }
            },
            onDismiss = { showIndexDialog = false }
        )
    }

    if (showJumpDialog) {
        MushafJumpDialog(
            currentPage = currentPageNumber,
            onJumpToPage = { targetPage ->
                showJumpDialog = false
                scope.launch {
                    pagerState.scrollToPage(targetPage - 1)
                }
            },
            onDismiss = { showJumpDialog = false }
        )
    }

    if (showConfirmWirdDialog) {
        MushafConfirmWirdDialog(
            currentPage = currentPageNumber,
            onConfirmPages = { pagesCount ->
                showConfirmWirdDialog = false
                scope.launch {
                    repo.recordMushafPagesRead(pagesCount)
                    Toast.makeText(
                        context,
                        "تقبل الله · تم تسجيل قراءة ${ZekrRepository.toArabicDigits(pagesCount)} صفحة في وِردك اليومي",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onDismiss = { showConfirmWirdDialog = false }
        )
    }

    val mushafBgColor = when (readingTheme) {
        1 -> Color(0xFF101713) // Midnight Jade
        2 -> Color(0xFFF5EFE6) // Warm Sepia
        else -> MaterialTheme.colorScheme.background // Pristine Paper
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mushafBgColor)
    ) {
        // Smart & Compact Top Header
        AnimatedVisibility(
            visible = showToolbars,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MushafTopBar(
                headerInfo = headerInfo,
                readingTheme = readingTheme,
                isBookmarkedHere = isBookmarkedHere,
                savedBookmarkPage = savedBookmarkPage,
                onBack = onBack,
                onCycleTheme = {
                    val nextTheme = (readingTheme + 1) % 3
                    scope.launch { repo.saveMushafReadingTheme(nextTheme) }
                },
                onToggleBookmark = {
                    scope.launch {
                        if (isBookmarkedHere) {
                            repo.saveMushafBookmark(0)
                            Toast.makeText(context, "تمت إزالة الفاصل", Toast.LENGTH_SHORT).show()
                        } else {
                            repo.saveMushafBookmark(currentPageNumber)
                            Toast.makeText(
                                context,
                                "تم وضع الفاصل في صفحة ${ZekrRepository.toArabicDigits(currentPageNumber)} 🔖",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onJumpToBookmark = {
                    if (savedBookmarkPage in 1..MushafData.TOTAL_PAGES) {
                        scope.launch { pagerState.animateScrollToPage(savedBookmarkPage - 1) }
                    }
                },
                onOpenIndex = { showIndexDialog = true },
                onOpenJump = { showJumpDialog = true },
                onOpenConfirmWird = { showConfirmWirdDialog = true }
            )
        }

        // Center: Interactive HD Quran Pager (Swipe right -> next page)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                reverseLayout = false,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val pageNumber = pageIndex + 1
                MushafPageView(
                    pageNumber = pageNumber,
                    readingTheme = readingTheme,
                    isBookmarkedHere = savedBookmarkPage == pageNumber,
                    onToggleToolbars = { showToolbars = !showToolbars },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Compact Bottom Navigation Controls (Sits right above the app's bottom bar!)
        AnimatedVisibility(
            visible = showToolbars,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MushafBottomBar(
                currentPage = currentPageNumber,
                headerInfo = headerInfo,
                onPreviousPage = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onNextPage = {
                    scope.launch {
                        if (pagerState.currentPage < MushafData.TOTAL_PAGES - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                onOpenJump = { showJumpDialog = true },
                onConfirmWird = { showConfirmWirdDialog = true }
            )
        }
    }
}

@Composable
private fun MushafTopBar(
    headerInfo: PageHeaderInfo,
    readingTheme: Int,
    isBookmarkedHere: Boolean,
    savedBookmarkPage: Int,
    onBack: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleBookmark: () -> Unit,
    onJumpToBookmark: () -> Unit,
    onOpenIndex: () -> Unit,
    onOpenJump: () -> Unit,
    onOpenConfirmWird: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah & Juz Info
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = headerInfo.surahName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    text = headerInfo.juzName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Quick Jump to Bookmark (if bookmark is on another page)
            if (savedBookmarkPage > 0 && !isBookmarkedHere) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD4AF37).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable(onClick = onJumpToBookmark)
                        .padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("🔖", fontSize = 11.sp)
                        Text(
                            text = "ص ${ZekrRepository.toArabicDigits(savedBookmarkPage)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                    }
                }
            }

            // Bookmark Toggle Button on Current Page
            Surface(
                shape = CircleShape,
                color = if (isBookmarkedHere) Color(0xFFD4AF37).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, if (isBookmarkedHere) Color(0xFFD4AF37).copy(alpha = 0.6f) else Color.Transparent),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleBookmark)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (isBookmarkedHere) "🔖" else "🏷️", fontSize = 16.sp)
                }
            }

            Spacer(Modifier.width(4.dp))

            // Page Number Badge with quick Search & Jump
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenJump)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "بحث برقم الصفحة",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "ص ${headerInfo.pageNumberArabic}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Reading Theme Cycle (☀️ -> 🌙 -> 📜)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCycleTheme)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (readingTheme) {
                        1 -> "🌙"
                        2 -> "📜"
                        else -> "☀️"
                    }
                    Text(icon, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.width(4.dp))

            // Index Dialog (📑) - Penultimate Button on the Left
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenIndex)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📑", fontSize = 16.sp)
                }
            }

            Spacer(Modifier.width(4.dp))

            // Confirm Wird Button (✓) - Last Button on the Left
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenConfirmWird)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "تسجيل الورد اليومي",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MushafBottomBar(
    currentPage: Int,
    headerInfo: PageHeaderInfo,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenJump: () -> Unit,
    onConfirmWird: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onPreviousPage,
                enabled = currentPage > 1,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("« السابقة", fontSize = 11.sp)
            }

            // Clickable Page Number chip that opens Search & Jump Dialog!
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenJump)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "بحث بالصفحة",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "صفحة ${headerInfo.pageNumberArabic} من ٦٠٤",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            FilledTonalButton(
                onClick = onNextPage,
                enabled = currentPage < MushafData.TOTAL_PAGES,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("التالية »", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MushafPageView(
    pageNumber: Int,
    readingTheme: Int,
    isBookmarkedHere: Boolean,
    onToggleToolbars: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(pageNumber) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageNumber) { mutableStateOf(true) }
    var isError by remember(pageNumber) { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(pageNumber, reloadTrigger) {
        isLoading = true
        isError = false
        val loaded = MushafPageLoader.loadPage(context, pageNumber)
        if (loaded != null) {
            bitmap = loaded
            isError = false
        } else {
            isError = true
        }
        isLoading = false
    }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3.5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    // High-contrast, crystal-clear Dark Mode using human luminance weights
    val nightModeColorFilter = remember {
        ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(
                    -0.3465f, -0.6803f, -0.1321f, 0f, 288.4f,
                    -0.3310f, -0.6498f, -0.1262f, 0f, 282.1f,
                    -0.3172f, -0.6228f, -0.1210f, 0f, 268.3f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    // Gentle Warm Sepia Mode (paper-like reading in soft ambient light)
    val sepiaColorFilter = remember {
        ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(
                    0.92f, 0f, 0f, 0f, 18f,
                    0f, 0.89f, 0f, 0f, 12f,
                    0f, 0f, 0.78f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    val activeFilter = when (readingTheme) {
        1 -> nightModeColorFilter
        2 -> sepiaColorFilter
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleToolbars() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offset = Offset.Zero
                    }
                )
            }
            .transformable(state = transformState, enabled = scale > 1f),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "صفحة المصحف $pageNumber",
                contentScale = ContentScale.Fit,
                colorFilter = activeFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            // Ribbon Bookmark Indicator on Current Page
            if (isBookmarkedHere) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                    color = Color(0xFFD4AF37),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🔖", fontSize = 11.sp)
                        Text(
                            text = "فاصل الورد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        } else if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "جاري تحميل صفحة ${ZekrRepository.toArabicDigits(pageNumber)} بدقة فائقة...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isError) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📖", fontSize = 32.sp)
                    Text(
                        text = "صفحة ${ZekrRepository.toArabicDigits(pageNumber)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "تأكد من الاتصال بالإنترنت لتحميل الصفحة للمرة الأولى. بعد التحميل، ستُحفظ الصفحة تلقائياً على جهازك للقراءة بدون إنترنت في أي وقت.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { reloadTrigger++ }) {
                        Text("إعادة المحاولة")
                    }
                }
            }
        }
    }
}

private fun normalizeArabicDigits(input: String): String {
    val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    var result = input
    for (i in arabicChars.indices) {
        result = result.replace(arabicChars[i], ('0' + i))
    }
    return result
}

@Composable
private fun MushafIndexDialog(
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "فهرس المصحف الشريف",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "اختر سورة أو جزءًا للانتقال الفوري",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Tab Switcher (Surahs vs Juz)
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("فهرس السور (١١٤)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("فهرس الأجزاء (٣٠)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // Search bar for Surahs
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن سورة أو رقمها...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    val normalizedQuery = remember(searchQuery) {
                        normalizeArabicDigits(searchQuery.trim())
                    }

                    val filteredSurahs = remember(normalizedQuery) {
                        if (normalizedQuery.isBlank()) MushafData.SURAHS
                        else MushafData.SURAHS.filter {
                            it.nameArabic.contains(normalizedQuery) ||
                            it.number.toString() == normalizedQuery ||
                            it.startPage.toString() == normalizedQuery
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            val isCurrentSurah = remember(currentPage, surah) {
                                val nextSurah = MushafData.SURAHS.getOrNull(surah.number)
                                val nextStart = nextSurah?.startPage ?: (MushafData.TOTAL_PAGES + 1)
                                currentPage in surah.startPage until nextStart
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectPage(surah.startPage) },
                                color = if (isCurrentSurah) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                                },
                                border = if (isCurrentSurah) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                } else null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = ZekrRepository.toArabicDigits(surah.number),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "سورة ${surah.nameArabic}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${if (surah.isMakki) "مكية" else "مدنية"} · ${ZekrRepository.toArabicDigits(surah.totalVerses)} آية",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "ص ${ZekrRepository.toArabicDigits(surah.startPage)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Juz list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(MushafData.JUZ_LIST) { juz ->
                            val isCurrentJuz = remember(currentPage, juz) {
                                val nextJuz = MushafData.JUZ_LIST.getOrNull(juz.number)
                                val nextStart = nextJuz?.startPage ?: (MushafData.TOTAL_PAGES + 1)
                                currentPage in juz.startPage until nextStart
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectPage(juz.startPage) },
                                color = if (isCurrentJuz) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                                },
                                border = if (isCurrentJuz) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                } else null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = ZekrRepository.toArabicDigits(juz.number),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = juz.nameArabic,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "ص ${ZekrRepository.toArabicDigits(juz.startPage)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MushafJumpDialog(
    currentPage: Int,
    onJumpToPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf(currentPage.toString()) }
    val normalizedInput = remember(inputText) { normalizeArabicDigits(inputText.trim()) }
    val pageNum = normalizedInput.toIntOrNull()
    val isPageValid = pageNum != null && pageNum in 1..MushafData.TOTAL_PAGES

    // If input is text (not number), search for Surahs
    val matchedSurahs = remember(normalizedInput) {
        if (pageNum == null && normalizedInput.length >= 2) {
            MushafData.SURAHS.filter { it.nameArabic.contains(normalizedInput) }
        } else {
            emptyList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "البحث والانتقال في المصحف",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "أدخل رقم الصفحة (١ - ٦٠٤) أو اكتب اسم السورة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input Field supporting both Western & Arabic-Indic digits as well as text
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.take(20) },
                    placeholder = { Text("مثال: ٤٥ أو البقرة أو الكهف...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Page Number Info or Error
                if (isPageValid) {
                    val info = remember(pageNum) { MushafData.getPageHeaderInfo(pageNum!!) }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = info.surahName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = info.juzName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "صفحة ${ZekrRepository.toArabicDigits(pageNum!!)}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (pageNum != null) {
                    Text(
                        text = "يرجى إدخال رقم صفحة صحيح بين ١ و ٦٠٤",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // If text matched Surahs, display them
                if (matchedSurahs.isNotEmpty()) {
                    Text(
                        text = "السور المطابقة:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        matchedSurahs.take(4).forEach { surah ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onJumpToPage(surah.startPage) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "سورة ${surah.nameArabic} (${if (surah.isMakki) "مكية" else "مدنية"})",
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "ص ${ZekrRepository.toArabicDigits(surah.startPage)} ↗",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Jump Shortcuts
                Text(
                    text = "انتقال سريع:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shortcuts = listOf(
                        "البداية" to 1,
                        "الربع" to 151,
                        "المنتصف" to 302,
                        "جزء عم" to 582
                    )
                    shortcuts.forEach { (label, targetPage) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onJumpToPage(targetPage) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("ص ${ZekrRepository.toArabicDigits(targetPage)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = { if (isPageValid) onJumpToPage(pageNum!!) },
                        enabled = isPageValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انتقال")
                    }
                }
            }
        }
    }
}

@Composable
private fun MushafConfirmWirdDialog(
    currentPage: Int,
    onConfirmPages: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pagesReadCount by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد إتمام ورد القراءة اليوم") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "أحسنت! كم صفحة قرأت في وردك اليوم لإضافتها إلى إحصائيات المواظبة؟",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 4, 10, 20).forEach { count ->
                        FilterChip(
                            selected = pagesReadCount == count,
                            onClick = { pagesReadCount = count },
                            label = { Text("${ZekrRepository.toArabicDigits(count)} ص") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("عدد الصفحات: ${ZekrRepository.toArabicDigits(pagesReadCount)}", fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { if (pagesReadCount > 1) pagesReadCount-- }) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { if (pagesReadCount < 604) pagesReadCount++ }) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmPages(pagesReadCount) }) {
                Text("تسجيل الإنجاز ✓")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

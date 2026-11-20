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
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.mushaf.MushafData
import com.zekr.widget.mushaf.MushafPageLoader
import com.zekr.widget.mushaf.PageHeaderInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafScreen(
    initialPage: Int = 1,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { WidgetPreferencesRepository(context) }

    val savedLastPage by repo.mushafLastPageFlow.collectAsState(initial = initialPage)
    val isNightMode by repo.mushafNightModeFlow.collectAsState(initial = false)
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

    val mushafBgColor = if (isNightMode) Color(0xFF131B17) else MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = showToolbars,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MushafTopBar(
                    headerInfo = headerInfo,
                    isNightMode = isNightMode,
                    onBack = onBack,
                    onToggleNightMode = {
                        scope.launch {
                            repo.saveMushafNightMode(!isNightMode)
                        }
                    },
                    onOpenIndex = { showIndexDialog = true },
                    onOpenJump = { showJumpDialog = true },
                    onOpenConfirmWird = { showConfirmWirdDialog = true }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showToolbars,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MushafBottomBar(
                    currentPage = currentPageNumber,
                    savedLastPage = savedLastPage,
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
                    onJumpToSaved = {
                        scope.launch {
                            pagerState.animateScrollToPage(savedLastPage - 1)
                        }
                    },
                    onConfirmWird = { showConfirmWirdDialog = true }
                )
            }
        },
        containerColor = mushafBgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(mushafBgColor)
        ) {
            // HorizontalPager with reverseLayout = false: in RTL, dragging towards right advances to next page!
            HorizontalPager(
                state = pagerState,
                reverseLayout = false,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val pageNumber = pageIndex + 1
                MushafPageView(
                    pageNumber = pageNumber,
                    isNightMode = isNightMode,
                    onToggleToolbars = { showToolbars = !showToolbars },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun MushafTopBar(
    headerInfo: PageHeaderInfo,
    isNightMode: Boolean,
    onBack: () -> Unit,
    onToggleNightMode: () -> Unit,
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
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prominent Back Button
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "الرجوع",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Surah & Juz Info
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = headerInfo.surahName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = headerInfo.juzName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Page Number Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.clickable(onClick = onOpenJump)
            ) {
                Text(
                    text = "صفحة ${headerInfo.pageNumberArabic}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            // Actions: Day/Night Toggle, Jump, Index, Confirm Wird
            IconButton(onClick = onToggleNightMode) {
                Text(if (isNightMode) "☀️" else "🌙", fontSize = 16.sp)
            }
            IconButton(onClick = onOpenJump) {
                Text("🔢", fontSize = 15.sp)
            }
            IconButton(onClick = onOpenIndex) {
                Text("📑", fontSize = 15.sp)
            }
            IconButton(onClick = onOpenConfirmWird) {
                Text("✓", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MushafBottomBar(
    currentPage: Int,
    savedLastPage: Int,
    headerInfo: PageHeaderInfo,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onJumpToSaved: () -> Unit,
    onConfirmWird: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Page Turning Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onPreviousPage,
                    enabled = currentPage > 1,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("« الصفحة السابقة", fontSize = 11.5.sp)
                }

                Text(
                    text = "صفحة ${headerInfo.pageNumberArabic} من ٦٠٤",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilledTonalButton(
                    onClick = onNextPage,
                    enabled = currentPage < MushafData.TOTAL_PAGES,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("الصفحة التالية »", fontSize = 11.5.sp)
                }
            }

            if (currentPage != savedLastPage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onJumpToSaved,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "متابعة من موضعك السابق (ص ${ZekrRepository.toArabicDigits(savedLastPage)})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Button(
                onClick = onConfirmWird,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("تأكيد إتمام ورد القراءة اليوم", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MushafPageView(
    pageNumber: Int,
    isNightMode: Boolean,
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

    val nightModeColorFilter = remember {
        ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(
                    -0.843f, 0f, 0f, 0f, 234f,
                    0f, -0.796f, 0f, 0f, 230f,
                    0f, 0f, -0.765f, 0f, 218f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
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
                colorFilter = if (isNightMode) nightModeColorFilter else null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "جاري تحميل صفحة ${ZekrRepository.toArabicDigits(pageNumber)}...",
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

@Composable
private fun MushafIndexDialog(
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("فهرس المصحف الشريف", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("فهرس السور (١١٤)") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("فهرس الأجزاء (٣٠)") }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن سورة...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    val filteredSurahs = remember(searchQuery) {
                        if (searchQuery.isBlank()) MushafData.SURAHS
                        else MushafData.SURAHS.filter {
                            it.nameArabic.contains(searchQuery.trim()) ||
                            it.number.toString() == searchQuery.trim()
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectPage(surah.startPage) },
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = ZekrRepository.toArabicDigits(surah.number),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "سورة ${surah.nameArabic}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (surah.isMakki) "مكية" else "مدنية",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "ص ${ZekrRepository.toArabicDigits(surah.startPage)}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(MushafData.JUZ_LIST) { juz ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectPage(juz.startPage) },
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = juz.nameArabic,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "ص ${ZekrRepository.toArabicDigits(juz.startPage)}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun MushafJumpDialog(
    currentPage: Int,
    onJumpToPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputPageText by remember { mutableStateOf(currentPage.toString()) }
    val pageNum = inputPageText.toIntOrNull()
    val isValid = pageNum != null && pageNum in 1..MushafData.TOTAL_PAGES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الانتقال إلى صفحة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "أدخل رقم الصفحة من ١ إلى ٦٠٤:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = inputPageText,
                    onValueChange = { inputPageText = it.filter(Char::isDigit).take(3) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (pageNum != null && isValid) {
                    val info = remember(pageNum) { MushafData.getPageHeaderInfo(pageNum) }
                    Text(
                        text = "${info.surahName} · ${info.juzName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (inputPageText.isNotBlank()) {
                    Text(
                        text = "يرجى إدخال رقم صحيح بين ١ و ٦٠٤",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onJumpToPage(pageNum!!) },
                enabled = isValid
            ) {
                Text("انتقال")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
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

package com.zekr.widget.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.audio.AudioCatalog
import com.zekr.widget.audio.AudioTrack
import com.zekr.widget.audio.WerdAudioPlayerManager
import com.zekr.widget.routine.ui.RoutineListSection

internal class DemoPlayback(
    val title: String,
    val ready: Boolean,
    val playing: Boolean,
    val engaged: Boolean,
    val error: Boolean,
    val position: Int,
    val duration: Int,
    val speed: Float,
    val repeating: Boolean,
    val timerActive: Boolean,
    val toggle: () -> Unit,
    val seek: (Int) -> Unit,
    val persist: () -> Unit,
    val previous: (() -> Unit)?,
    val next: (() -> Unit)?,
    val changeSpeed: () -> Unit,
    val toggleRepeat: () -> Unit,
    val toggleTimer: () -> Unit
)

@Composable
internal fun rememberDemoPlayback(): DemoPlayback {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        WerdAudioPlayerManager.init(context)
    }
    val state by WerdAudioPlayerManager.state.collectAsState()

    val track = state.currentTrack
    val title = track?.let { "${it.title} · ${it.reciterOrSpeaker}" } ?: "لم يتم اختيار مقطع"

    return remember(state) {
        DemoPlayback(
            title = title,
            ready = state.isReady,
            playing = state.isPlaying,
            engaged = state.isSessionActive,
            error = state.isError,
            position = state.currentPositionMs.toInt(),
            duration = state.durationMs.toInt(),
            speed = 1.0f,
            repeating = state.isRepeating,
            timerActive = state.sleepTimerEndRealtimeMs > 0L,
            toggle = { WerdAudioPlayerManager.togglePlayPause() },
            seek = { WerdAudioPlayerManager.seekTo(it.toLong()) },
            persist = {},
            previous = { WerdAudioPlayerManager.playPrevious() },
            next = { WerdAudioPlayerManager.playNext() },
            changeSpeed = {},
            toggleRepeat = { WerdAudioPlayerManager.toggleRepeat() },
            toggleTimer = { WerdAudioPlayerManager.setSleepTimerMinutes(15) }
        )
    }
}

@Composable
internal fun AudioWirdScreen(playback: DemoPlayback?) {
    val state by WerdAudioPlayerManager.state.collectAsState()
    val activeTrack = state.currentTrack
    var quranSearchQuery by remember { mutableStateOf("") }
    var athkarSearchQuery by remember { mutableStateOf("") }
    var selectedAthkarCategory by remember { mutableStateOf("الكل") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Player Card
        item {
            ModernSectionCard(
                title = "مشغّل الصوتيات والأوراد",
                subtitle = "بث صوتي مباشر مستمر بالخلفية مع دعم قفل الشاشة وإشعار الوسائط الرسمي"
            ) {
                if (activeTrack != null) {
                    ActiveTrackCard(state = state)
                } else {
                    Text(
                        "اختر سورة أو ذكراً من القوائم أدناه لبدء الاستماع",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 1: Holy Quran Recitations (Murattal) - Full 114 Surahs
        item {
            val allQuranTracks = AudioCatalog.getQuranTracks(state.selectedReciter)
            val quranTracks = remember(allQuranTracks, quranSearchQuery) {
                if (quranSearchQuery.isBlank()) allQuranTracks
                else allQuranTracks.filter {
                    it.title.contains(quranSearchQuery.trim()) ||
                    it.surahNumber.toString() == quranSearchQuery.trim()
                }
            }

            ModernSectionCard(
                title = "القرآن الكريم كاملًا (١١٤ سورة)",
                subtitle = "تلاوات مرتلة بروابط بث أصلية مباشرة من QuranicAudio.com"
            ) {
                Text(
                    "اختر القارئ:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioCatalog.QuranReciter.entries.forEach { reciter ->
                        FilterChip(
                            selected = state.selectedReciter == reciter,
                            onClick = { WerdAudioPlayerManager.setReciter(reciter) },
                            label = { Text(reciter.displayName) },
                            leadingIcon = if (state.selectedReciter == reciter) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = quranSearchQuery,
                    onValueChange = { quranSearchQuery = it },
                    placeholder = {
                        Text(
                            "ابحث في السور (مثال: الفاتحة، الكهف، ١١٢)...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (quranSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { quranSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "عدد السور المعروضة: ${quranTracks.size} من ١١٤ سورة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))

                quranTracks.take(15).forEach { track ->
                    val isCurrent = activeTrack?.id == track.id
                    TrackListItem(
                        track = track,
                        isSelected = isCurrent,
                        isPlaying = isCurrent && state.isPlaying,
                        onClick = {
                            if (isCurrent) {
                                WerdAudioPlayerManager.togglePlayPause()
                            } else {
                                WerdAudioPlayerManager.playTrack(track)
                            }
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                }

                if (quranTracks.size > 15) {
                    Text(
                        text = "يوجد ${quranTracks.size - 15} سورة إضافية، استخدم خانة البحث أعلاه للوصول السريع إلى أي سورة بالاسم أو الرقم.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Section 2: Authentic Human Athkar & Duas
        item {
            val categories = remember {
                listOf(
                    "الكل",
                    "أذكار اليوم والليلة",
                    "أذكار الصلاة",
                    "تفريج الكروب",
                    "السفر والتنقل",
                    "أدعية الأحوال",
                    "الاستغفار والتسبيح",
                    "الأدعية المأثورة"
                )
            }

            val filteredAthkar = remember(athkarSearchQuery, selectedAthkarCategory) {
                AudioCatalog.AthkarTracks.filter { track ->
                    val matchesCategory = selectedAthkarCategory == "الكل" || track.category == selectedAthkarCategory
                    val matchesQuery = athkarSearchQuery.isBlank() ||
                            track.title.contains(athkarSearchQuery.trim()) ||
                            track.subtitle.contains(athkarSearchQuery.trim())
                    matchesCategory && matchesQuery
                }
            }

            ModernSectionCard(
                title = "أذكار وأدعية الكتاب والسنة (بصوت بشري هادئ)",
                subtitle = "بصوت الشيخ حمد الدريهم من دار الإسلام (IslamHouse.com) - حصن المسلم"
            ) {
                // Search field for Athkar
                OutlinedTextField(
                    value = athkarSearchQuery,
                    onValueChange = { athkarSearchQuery = it },
                    placeholder = {
                        Text(
                            "ابحث في الأدعية والأذكار (مثال: الصباح، السفر، النوم، الاستخارة)...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (athkarSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { athkarSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedAthkarCategory == cat,
                            onClick = { selectedAthkarCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "عدد الأدعية المعروضة: ${filteredAthkar.size} من ${AudioCatalog.AthkarTracks.size} باباً محققاً",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))

                filteredAthkar.forEach { track ->
                    val isCurrent = activeTrack?.id == track.id
                    TrackListItem(
                        track = track,
                        isSelected = isCurrent,
                        isPlaying = isCurrent && state.isPlaying,
                        onClick = {
                            if (isCurrent) {
                                WerdAudioPlayerManager.togglePlayPause()
                            } else {
                                WerdAudioPlayerManager.playTrack(track)
                            }
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // Section 3: Verified Audio Library Status
        item {
            ModernSectionCard(
                title = "موسوعة الأذكار والأدعية المحققة",
                subtitle = "توثيق التسميات والمطابقة الصوتية الشاملة"
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "تم تدقيق وتصحيح مطابقة كافة الملفات الصوتية بنسبة ١٠٠٪؛ بحيث يتطابق عنوان كل دعاء مع محتواه الصوتي المسموع بصوت الشيخ حمد الدريهم من كتاب حصن المسلم.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "تشمل المكتبة الصوتية الآن ٣٣ باباً رئيسياً تشمل أذكار الصباح والمساء، أذكار النوم، الاستيقاظ، صلاة الاستخارة، أذكار ما بعد الصلاة، تفريج الهموم والكروب، قضاء الدين، السفر، الاستغفار، الصلاة على النبي ﷺ، والتسبيح.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 4: Smart Audio Routine Builder (روتيناتي الصوتية - في نهاية قسم الصوتيات)
        item {
            RoutineListSection()
        }
    }
}

@Composable
private fun ActiveTrackCard(state: com.zekr.widget.audio.PlayerUiState) {
    val track = state.currentTrack ?: return
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    if (showSleepTimerDialog) {
        var selectedMinutes by remember { mutableIntStateOf(30) }
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("مؤقت النوم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "إيقاف تشغيل الصوتيات تلقائياً بعد انقضاء الوقت:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60).forEach { mins ->
                            FilterChip(
                                selected = selectedMinutes == mins,
                                onClick = { selectedMinutes = mins },
                                label = { Text("$mins د") }
                            )
                        }
                    }
                    Text("أو اختر مدة مخصصة: $selectedMinutes دقيقة", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = it.toInt() },
                        valueRange = 5f..120f,
                        steps = 22
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    WerdAudioPlayerManager.setSleepTimerMinutes(selectedMinutes)
                    showSleepTimerDialog = false
                }) {
                    Text("تفعيل المؤقت")
                }
            },
            dismissButton = {
                if (state.sleepTimerEndRealtimeMs > 0L) {
                    TextButton(onClick = {
                        WerdAudioPlayerManager.cancelSleepTimer()
                        showSleepTimerDialog = false
                    }) {
                        Text("إلغاء المؤقت", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("إغلاق")
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${track.reciterOrSpeaker} · ${track.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "المصدر: ${track.sourceName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        if (state.isError) {
            Text(
                text = "تعذر تشغيل المقطع: ${state.errorMessage ?: "تأكد من الاتصال بالإنترنت"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Seekbar
        val duration = state.durationMs.coerceAtLeast(1L).toFloat()
        val position = state.currentPositionMs.coerceIn(0L, state.durationMs).toFloat()
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        Slider(
            value = if (isDragging) sliderPosition else position,
            valueRange = 0f..duration,
            onValueChange = {
                isDragging = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                WerdAudioPlayerManager.seekTo(sliderPosition.toLong())
                isDragging = false
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(state.currentPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatTime(state.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Primary Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = { WerdAudioPlayerManager.playPrevious() }) {
                Text("⏮", fontSize = 16.sp)
            }
            Spacer(Modifier.width(14.dp))
            Button(
                onClick = { WerdAudioPlayerManager.togglePlayPause() },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    if (state.isPlaying) "إيقاف مؤقت ⏸" else "تشغيل ▶",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            FilledTonalIconButton(onClick = { WerdAudioPlayerManager.playNext() }) {
                Text("⏭", fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = { WerdAudioPlayerManager.stopPlayback() },
                modifier = Modifier.size(40.dp)
            ) {
                Text("⏹", fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
            }
        }

        // Secondary Options: Repeat, Sleep Timer & Quran Continuous Scope
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = state.isRepeating,
                onClick = { WerdAudioPlayerManager.toggleRepeat() },
                label = { Text("تكرار") }
            )
            FilterChip(
                selected = state.sleepTimerEndRealtimeMs > 0L,
                onClick = { showSleepTimerDialog = true },
                label = {
                    val labelText = if (state.sleepTimerEndRealtimeMs > 0L) {
                        val mins = state.sleepTimerRemainingSeconds / 60
                        val secs = state.sleepTimerRemainingSeconds % 60
                        "مؤقت: $mins:${secs.toString().padStart(2, '0')}"
                    } else "مؤقت النوم"
                    Text(labelText)
                }
            )
            if (track.surahNumber != null) {
                FilterChip(
                    selected = state.quranPlayScope == com.zekr.widget.audio.QuranPlayScope.CONTINUOUS_TO_END,
                    onClick = {
                        val next = if (state.quranPlayScope == com.zekr.widget.audio.QuranPlayScope.CONTINUOUS_TO_END) {
                            com.zekr.widget.audio.QuranPlayScope.SINGLE_SURAH
                        } else {
                            com.zekr.widget.audio.QuranPlayScope.CONTINUOUS_TO_END
                        }
                        WerdAudioPlayerManager.setQuranPlayScope(next)
                    },
                    label = {
                        Text(if (state.quranPlayScope == com.zekr.widget.audio.QuranPlayScope.CONTINUOUS_TO_END) "تلاوة متتالية لنهاية المصحف" else "سورة واحدة")
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackListItem(
    track: AudioTrack,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${track.reciterOrSpeaker} · المصدر: ${track.sourceName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(if (isPlaying) "⏸" else "▶", fontSize = 14.sp)
            }
        }
    }
}

@Composable
internal fun AudioMiniPlayer(playback: DemoPlayback, onOpen: () -> Unit) {
    val state by WerdAudioPlayerManager.state.collectAsState()
    if (!state.isSessionActive) return
    val track = state.currentTrack ?: return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = track.reciterOrSpeaker,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { WerdAudioPlayerManager.togglePlayPause() },
                modifier = Modifier.size(38.dp)
            ) {
                Text(if (state.isPlaying) "⏸" else "▶", fontSize = 16.sp)
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = { WerdAudioPlayerManager.stopPlayback() },
                modifier = Modifier.size(34.dp)
            ) {
                Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

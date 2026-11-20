package com.zekr.widget.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
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
            engaged = track != null,
            error = state.isError,
            position = state.currentPositionMs.toInt(),
            duration = state.durationMs.toInt(),
            speed = state.playbackSpeed,
            repeating = state.isRepeating,
            timerActive = state.sleepTimerEndRealtimeMs > 0L,
            toggle = { WerdAudioPlayerManager.togglePlayPause() },
            seek = { WerdAudioPlayerManager.seekTo(it.toLong()) },
            persist = {},
            previous = { WerdAudioPlayerManager.playPrevious() },
            next = { WerdAudioPlayerManager.playNext() },
            changeSpeed = { WerdAudioPlayerManager.cycleSpeed() },
            toggleRepeat = { WerdAudioPlayerManager.toggleRepeat() },
            toggleTimer = { WerdAudioPlayerManager.toggleSleepTimer() }
        )
    }
}

@Composable
internal fun AudioWirdScreen(playback: DemoPlayback?) {
    val state by WerdAudioPlayerManager.state.collectAsState()
    val activeTrack = state.currentTrack
    var quranSearchQuery by remember { mutableStateOf("") }

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
            ModernSectionCard(
                title = "أذكار وأدعية الكتاب والسنة (بصوت بشري هادئ)",
                subtitle = "بصوت الشيخ حمد الدريهم من دار الإسلام (IslamHouse.com) - حصن المسلم"
            ) {
                AudioCatalog.AthkarTracks.forEach { track ->
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

        // Section 3: Remaining Collections (Verified status)
        item {
            ModernSectionCard(
                title = "مجموعات الأذكار والأدعية الأخرى",
                subtitle = "حالة التوثيق والتحقق الصوتي"
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
                            "تم توفير ١٢ باباً رئيساً محققاً بصوت الشيخ حمد الدريهم من حصن المسلم (الاستيقاظ، الصباح والمساء، النوم، دبر الصلاة، الاستغفار، الصلاة على النبي ﷺ، التسبيح، السفر، دخول وخروج المنزل، الاستخارة، والهم والكرب).",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "الأبواب التخصصية المتبقية (الرقية المطولة، تفاصيل مناسك الحج، والأدعية النادرة) ما زالت قيد المراجعة والتحقق الصوتي لضمان خلوها من أي مؤثرات وتوثيق تراخيصها الشرعية، وستُضاف تباعاً بإذن الله.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTrackCard(state: com.zekr.widget.audio.PlayerUiState) {
    val track = state.currentTrack ?: return

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
            Spacer(Modifier.width(16.dp))
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
            Spacer(Modifier.width(16.dp))
            FilledTonalIconButton(onClick = { WerdAudioPlayerManager.playNext() }) {
                Text("⏭", fontSize = 16.sp)
            }
        }

        // Secondary Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { WerdAudioPlayerManager.cycleSpeed() },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("${state.playbackSpeed}×")
            }
            FilterChip(
                selected = state.isRepeating,
                onClick = { WerdAudioPlayerManager.toggleRepeat() },
                label = { Text("تكرار") }
            )
            FilterChip(
                selected = state.sleepTimerEndRealtimeMs > 0L,
                onClick = { WerdAudioPlayerManager.toggleSleepTimer() },
                label = {
                    val labelText = if (state.sleepTimerEndRealtimeMs > 0L) {
                        val mins = state.sleepTimerRemainingSeconds / 60
                        val secs = state.sleepTimerRemainingSeconds % 60
                        "مؤقت: $mins:${secs.toString().padStart(2, '0')}"
                    } else "مؤقت ١٥ د"
                    Text(labelText)
                }
            )
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
            Spacer(Modifier.width(12.dp))
            FilledIconButton(
                onClick = { WerdAudioPlayerManager.togglePlayPause() },
                modifier = Modifier.size(40.dp)
            ) {
                Text(if (state.isPlaying) "⏸" else "▶", fontSize = 16.sp)
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

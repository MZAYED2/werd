package com.zekr.widget.audio

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.zekr.widget.service.WerdAudioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isRepeating: Boolean = false,
    val sleepTimerEndRealtimeMs: Long = 0L,
    val sleepTimerRemainingSeconds: Long = 0L,
    val selectedReciter: AudioCatalog.QuranReciter = AudioCatalog.QuranReciter.MINSHAWI
)

object WerdAudioPlayerManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private val prefs by lazy {
        appContext?.getSharedPreferences("werd_audio_playback_v2", Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app

        val savedReciterId = prefs?.getString("quran_reciter", AudioCatalog.QuranReciter.MINSHAWI.id)
        val reciter = AudioCatalog.QuranReciter.entries.firstOrNull { it.id == savedReciterId }
            ?: AudioCatalog.QuranReciter.MINSHAWI
        val savedTrackId = prefs?.getString("last_track_id", null)
        val restoredTrack = savedTrackId?.let { AudioCatalog.findTrackById(it) }
            ?: AudioCatalog.getQuranTracks(reciter).firstOrNull()

        val savedPosition = prefs?.getLong("last_position_ms", 0L) ?: 0L

        _state.update {
            it.copy(
                selectedReciter = reciter,
                currentTrack = restoredTrack,
                currentPositionMs = savedPosition
            )
        }

        connectController(app)
    }

    private fun connectController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, WerdAudioService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val mediaController = controllerFuture.get()
                controller = mediaController
                setupPlayerListener(mediaController)
                syncFromPlayer(mediaController)
            } catch (e: Exception) {
                _state.update { it.copy(isError = true, errorMessage = e.localizedMessage) }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    progressJob?.cancel()
                    persistState()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val duration = if (player.duration > 0) player.duration else 0L
                val isReady = playbackState == Player.STATE_READY
                val isLoading = playbackState == Player.STATE_BUFFERING
                _state.update {
                    it.copy(
                        isReady = isReady,
                        isLoading = isLoading,
                        durationMs = duration
                    )
                }
                if (playbackState == Player.STATE_ENDED) {
                    _state.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                    persistState()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId
                if (id != null) {
                    val track = AudioCatalog.findTrackById(id)
                    _state.update { it.copy(currentTrack = track, isError = false) }
                    persistState()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isError = true,
                        isLoading = false,
                        isPlaying = false,
                        errorMessage = error.localizedMessage
                    )
                }
            }
        })
    }

    private fun syncFromPlayer(player: Player) {
        val activeMediaId = player.currentMediaItem?.mediaId
        val activeTrack = activeMediaId?.let { AudioCatalog.findTrackById(it) } ?: _state.value.currentTrack
        val isPlaying = player.isPlaying
        val duration = if (player.duration > 0) player.duration else 0L
        val position = player.currentPosition.coerceAtLeast(0L)
        val speed = player.playbackParameters.speed
        val repeat = player.repeatMode == Player.REPEAT_MODE_ONE

        _state.update {
            it.copy(
                currentTrack = activeTrack,
                isPlaying = isPlaying,
                isReady = player.playbackState == Player.STATE_READY,
                isLoading = player.playbackState == Player.STATE_BUFFERING,
                durationMs = duration,
                currentPositionMs = position,
                playbackSpeed = speed,
                isRepeating = repeat
            )
        }

        if (isPlaying) startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                controller?.let { c ->
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    val dur = if (c.duration > 0) c.duration else _state.value.durationMs
                    _state.update {
                        it.copy(currentPositionMs = pos, durationMs = dur)
                    }
                }
                delay(400)
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        val c = controller ?: return
        _state.update {
            it.copy(
                currentTrack = track,
                isLoading = true,
                isError = false,
                errorMessage = null
            )
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.reciterOrSpeaker)
                    .setSubtitle(track.subtitle)
                    .setDescription(track.sourceName)
                    .build()
            )
            .build()

        c.setMediaItem(mediaItem)
        c.prepare()
        c.play()
        persistState()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE || c.currentMediaItem == null) {
                val track = _state.value.currentTrack ?: return
                playTrack(track)
            } else {
                c.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val c = controller ?: return
        c.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun cycleSpeed() {
        val c = controller ?: return
        val currentSpeed = _state.value.playbackSpeed
        val newSpeed = when (currentSpeed) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            else -> 1.0f
        }
        c.setPlaybackSpeed(newSpeed)
        _state.update { it.copy(playbackSpeed = newSpeed) }
    }

    fun toggleRepeat() {
        val c = controller ?: return
        val isRepeating = _state.value.isRepeating
        val newMode = if (isRepeating) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        c.repeatMode = newMode
        _state.update { it.copy(isRepeating = !isRepeating) }
    }

    fun toggleSleepTimer() {
        val currentEnd = _state.value.sleepTimerEndRealtimeMs
        if (currentEnd > 0L) {
            sleepTimerJob?.cancel()
            _state.update { it.copy(sleepTimerEndRealtimeMs = 0L, sleepTimerRemainingSeconds = 0L) }
        } else {
            val durationMs = 15 * 60 * 1000L
            val endMs = SystemClock.elapsedRealtime() + durationMs
            _state.update { it.copy(sleepTimerEndRealtimeMs = endMs, sleepTimerRemainingSeconds = 15 * 60L) }
            sleepTimerJob?.cancel()
            sleepTimerJob = scope.launch {
                while (true) {
                    val remaining = (endMs - SystemClock.elapsedRealtime()) / 1000L
                    if (remaining <= 0L) {
                        _state.update { it.copy(sleepTimerEndRealtimeMs = 0L, sleepTimerRemainingSeconds = 0L) }
                        controller?.pause()
                        break
                    }
                    _state.update { it.copy(sleepTimerRemainingSeconds = remaining) }
                    delay(1000)
                }
            }
        }
    }

    fun setReciter(reciter: AudioCatalog.QuranReciter) {
        _state.update { it.copy(selectedReciter = reciter) }
        prefs?.edit()?.putString("quran_reciter", reciter.id)?.apply()
    }

    fun playNext() {
        val reciter = _state.value.selectedReciter
        val allTracks = AudioCatalog.getAllTracks(reciter)
        val currentIndex = allTracks.indexOfFirst { it.id == _state.value.currentTrack?.id }
        if (currentIndex in 0 until allTracks.lastIndex) {
            playTrack(allTracks[currentIndex + 1])
        }
    }

    fun playPrevious() {
        val reciter = _state.value.selectedReciter
        val allTracks = AudioCatalog.getAllTracks(reciter)
        val currentIndex = allTracks.indexOfFirst { it.id == _state.value.currentTrack?.id }
        if (currentIndex > 0) {
            playTrack(allTracks[currentIndex - 1])
        }
    }

    private fun persistState() {
        prefs?.edit()?.apply {
            _state.value.currentTrack?.id?.let { putString("last_track_id", it) }
            putLong("last_position_ms", _state.value.currentPositionMs)
            putString("quran_reciter", _state.value.selectedReciter.id)
            apply()
        }
    }
}

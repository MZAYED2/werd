package com.zekr.widget.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.zekr.widget.R
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.ui.theme.WerdDesignTokens
import com.zekr.widget.ui.theme.loadWerdAppearance
import com.zekr.widget.widget.ZekrGlanceWidget
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime
import kotlin.math.abs

class FloatingTasbeehService : Service() {
    companion object {
        const val EXTRA_ITEM_ID = "overlay_item_id"
        const val EXTRA_DISMISS_NOTIFICATION = "overlay_dismiss_notification"
        private const val STOP = "com.zekr.widget.CLOSE_FLOATING_TASBEEH"
        private const val CHANNEL = "werd_floating_tasbeeh"
        private const val NOTIFICATION = 2090
        private const val INACTIVITY_AUTO_COLLAPSE_MS = 30_000L
    }

    private enum class Mode { Edge, Compact, Expanded }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val operations = Mutex()
    private val repo by lazy { WidgetPreferencesRepository(applicationContext) }
    private val location by lazy { getSharedPreferences("werd_floating_position", MODE_PRIVATE) }
    private val overlayContext by lazy {
        if (Build.VERSION.SDK_INT >= 30) {
            val displayManager = getSystemService(android.hardware.display.DisplayManager::class.java)
            val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            if (display == null) this else createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        } else this
    }
    private val manager by lazy { overlayContext.getSystemService(WindowManager::class.java) }
    private var view: View? = null
    private var counterView: TextView? = null
    private lateinit var window: WindowManager.LayoutParams

    private var allTasbeehItems: List<ZekrEntity> = emptyList()
    private var item: ZekrEntity? = null
    private var mode = Mode.Compact
    private var count = 0
    private var target = 33
    private var free = false
    private var requestedId = 0

    private var inactivityJob: Job? = null
    private var edgeFadeJob: Job? = null

    private val appearancePreferences by lazy { getSharedPreferences("werd_appearance", MODE_PRIVATE) }
    private val onAppearanceChanged = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        if (view != null) moveTo(mode)
    }

    override fun onCreate() {
        super.onCreate()
        appearancePreferences.registerOnSharedPreferenceChangeListener(onAppearanceChanged)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (view != null) moveTo(mode)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == STOP) {
            dismissCompletely()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "اسمح بالظهور فوق التطبيقات لتشغيل السبحة العائمة.", Toast.LENGTH_LONG).show()
            dismissCompletely()
            return START_NOT_STICKY
        }

        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "السبحة العائمة", NotificationManager.IMPORTANCE_MIN).apply {
                description = "تشغيل السبحة فوق التطبيقات"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
        val close = PendingIntent.getService(
            this, NOTIFICATION, Intent(this, javaClass).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        startForeground(
            NOTIFICATION,
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_werd_reminder)
                .setContentTitle("السبحة العائمة")
                .setContentText("انقر للتحكم أو الإغلاق")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .setOngoing(true)
                .addAction(0, "إغلاق نهائي", close)
                .build()
        )

        val id = intent.getIntExtra(EXTRA_ITEM_ID, 0)
        val reminder = intent.getIntExtra(EXTRA_DISMISS_NOTIFICATION, 0)
        if (id <= 0 && item == null) {
            dismissCompletely()
            return START_NOT_STICKY
        }

        scope.launch {
            operations.withLock {
                if (allTasbeehItems.isEmpty()) {
                    allTasbeehItems = ZekrRepository.getAllItems(applicationContext, repo.getCustomItems())
                        .filter { it.type != ContentType.QURAN_AYAH || it.tasbeehText != null }
                }
                val targetId = if (id > 0) id else allTasbeehItems.firstOrNull()?.id ?: 10
                requestedId = targetId
                val selected = allTasbeehItems.firstOrNull { it.id == targetId }
                    ?: allTasbeehItems.firstOrNull()
                if (selected == null) {
                    dismissCompletely()
                    return@withLock
                }
                val state = repo.selectTasbeehItem(selected)
                item = selected
                count = state.count
                target = state.target
                free = state.isFreeMode
                // Launch directly into Compact mode (the primary everyday tasbeeh interface)
                if (moveTo(Mode.Compact) && reminder > 0) {
                    notifications.cancel(reminder)
                }
                resetInactivityTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    @Suppress("DEPRECATION")
    private fun screenWidth() = if (Build.VERSION.SDK_INT >= 30) {
        manager.currentWindowMetrics.bounds.width()
    } else resources.displayMetrics.widthPixels

    @Suppress("DEPRECATION")
    private fun screenHeight() = if (Build.VERSION.SDK_INT >= 30) {
        manager.currentWindowMetrics.bounds.height()
    } else resources.displayMetrics.heightPixels

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        if (mode == Mode.Edge) return
        inactivityJob = scope.launch {
            delay(INACTIVITY_AUTO_COLLAPSE_MS)
            if (isActive && mode != Mode.Edge) {
                moveTo(Mode.Edge)
            }
        }
    }

    private fun startEdgeFade() {
        edgeFadeJob?.cancel()
        setEdgeAlpha(0.85f)
        edgeFadeJob = scope.launch {
            delay(15_000L) // 15 seconds -> 45%
            if (!isActive || mode != Mode.Edge) return@launch
            setEdgeAlpha(0.45f)
            delay(15_000L) // 30 seconds total -> 20%
            if (!isActive || mode != Mode.Edge) return@launch
            setEdgeAlpha(0.20f)
        }
    }

    private fun setEdgeAlpha(alpha: Float) {
        view?.alpha = alpha
    }

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    private fun moveTo(next: Mode): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            dismissCompletely()
            return false
        }
        val old = view
        mode = next

        val width = when (next) {
            Mode.Expanded -> minOf(dp(324), screenWidth() - dp(16))
            Mode.Compact -> dp(256)
            Mode.Edge -> dp(28)
        }.coerceAtLeast(1)

        val height = when (next) {
            Mode.Edge -> dp(76)
            else -> WindowManager.LayoutParams.WRAP_CONTENT
        }

        val windowFlags = if (next == Mode.Edge) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }

        if (!::window.isInitialized) {
            window = WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                windowFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                val isRight = location.getBoolean("right", true)
                x = when (next) {
                    Mode.Compact -> {
                        val defaultX = if (isRight) (screenWidth() - width - dp(12)).coerceAtLeast(0) else dp(12)
                        location.getInt("compact_x", defaultX)
                    }
                    Mode.Expanded -> {
                        val defaultX = ((screenWidth() - width) / 2).coerceAtLeast(0)
                        location.getInt("expanded_x", defaultX)
                    }
                    Mode.Edge -> {
                        if (isRight) (screenWidth() - width).coerceAtLeast(0) else 0
                    }
                }
                y = when (next) {
                    Mode.Compact -> location.getInt("compact_y", dp(220))
                    Mode.Expanded -> location.getInt("expanded_y", dp(180))
                    Mode.Edge -> location.getInt("y", dp(220))
                }
                title = "Werd Smart Edge Tasbeeh"
            }
        } else {
            window.width = width
            window.height = height
            window.flags = windowFlags
            val isRight = location.getBoolean("right", true)
            when (next) {
                Mode.Compact -> {
                    val defaultX = if (isRight) (screenWidth() - width - dp(12)).coerceAtLeast(0) else dp(12)
                    val savedX = location.getInt("compact_x", defaultX)
                    window.x = savedX.coerceIn(dp(6), (screenWidth() - width - dp(6)).coerceAtLeast(0))
                    val savedY = location.getInt("compact_y", dp(220))
                    window.y = savedY.coerceIn(dp(40), (screenHeight() - dp(180)).coerceAtLeast(dp(40)))
                }
                Mode.Expanded -> {
                    val defaultX = ((screenWidth() - width) / 2).coerceAtLeast(0)
                    val savedX = location.getInt("expanded_x", defaultX)
                    window.x = savedX.coerceIn(dp(6), (screenWidth() - width - dp(6)).coerceAtLeast(0))
                    val savedY = location.getInt("expanded_y", dp(180))
                    window.y = savedY.coerceIn(dp(40), (screenHeight() - dp(240)).coerceAtLeast(dp(40)))
                }
                Mode.Edge -> {
                    window.x = if (isRight) (screenWidth() - width).coerceAtLeast(0) else 0
                    window.y = location.getInt("y", dp(220)).coerceIn(dp(40), (screenHeight() - dp(120)).coerceAtLeast(dp(60)))
                }
            }
        }

        val replacement = render()
        return try {
            if (old != null && old.isAttachedToWindow) {
                manager.removeViewImmediate(old)
                view = null
            }
            window.alpha = 1.0f
            manager.addView(replacement, window)
            view = replacement

            if (next == Mode.Edge) {
                inactivityJob?.cancel()
                startEdgeFade()
            } else {
                edgeFadeJob?.cancel()
                resetInactivityTimer()
            }
            true
        } catch (_: Exception) {
            dismissCompletely()
            false
        }
    }

    private fun render(): View {
        val systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val local = LocalTime.now()
        val dark = loadWerdAppearance(this).isDark(systemDark, local.hour * 60 + local.minute)
        val fill = if (dark) WerdDesignTokens.MidnightJade.Surface else WerdDesignTokens.IvoryJade.Surface
        val elevated = if (dark) WerdDesignTokens.MidnightJade.Elevated else WerdDesignTokens.IvoryJade.Elevated
        val textColor = if (dark) WerdDesignTokens.MidnightJade.Text else WerdDesignTokens.IvoryJade.Text
        val secondaryTextColor = if (dark) WerdDesignTokens.MidnightJade.SecondaryText else WerdDesignTokens.IvoryJade.SecondaryText
        val accent = if (dark) WerdDesignTokens.MidnightJade.AccessibleEmerald else WerdDesignTokens.IvoryJade.Emerald

        fun background(radius: Int, strokeWidth: Int = 1) = GradientDrawable().apply {
            setColor(fill.toInt())
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(strokeWidth), accent.toInt())
        }

        fun label(value: String, size: Float, isBold: Boolean = false) = TextView(overlayContext).apply {
            text = value
            textSize = size
            setTextColor(textColor.toInt())
            gravity = Gravity.CENTER
            if (isBold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        fun iconBtn(value: String, isAccent: Boolean = false, press: () -> Unit) = Button(overlayContext).apply {
            text = value
            textSize = 14f
            setTextColor(if (isAccent) accent.toInt() else textColor.toInt())
            backgroundTintList = null
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((accent.toInt() and 0x00FFFFFF) or 0x24000000),
                GradientDrawable().apply {
                    setColor(elevated.toInt())
                    cornerRadius = dp(10).toFloat()
                    if (isAccent) setStroke(dp(1), accent.toInt())
                },
                null
            )
            setPadding(dp(4), dp(2), dp(4), dp(2))
            isAllCaps = false
            setOnClickListener {
                resetInactivityTimer()
                press()
            }
        }

        fun textBtn(value: String, isAccent: Boolean = false, press: () -> Unit) = Button(overlayContext).apply {
            text = value
            textSize = 13f
            setTextColor(if (isAccent) accent.toInt() else textColor.toInt())
            backgroundTintList = null
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((accent.toInt() and 0x00FFFFFF) or 0x24000000),
                GradientDrawable().apply {
                    setColor(elevated.toInt())
                    cornerRadius = dp(10).toFloat()
                    if (isAccent) setStroke(dp(1), accent.toInt())
                },
                null
            )
            isAllCaps = false
            setOnClickListener {
                resetInactivityTimer()
                press()
            }
        }

        // ==========================================
        // 1. MODE 1: SMART EDGE
        // ==========================================
        if (mode == Mode.Edge) {
            val isRight = location.getBoolean("right", true)
            val chevron = if (isRight) "‹" else "›"
            return LinearLayout(overlayContext).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                minimumWidth = dp(28)
                minimumHeight = dp(76)
                background = GradientDrawable().apply {
                    setColor(elevated.toInt())
                    if (isRight) {
                        cornerRadii = floatArrayOf(
                            dp(16).toFloat(), dp(16).toFloat(),
                            0f, 0f,
                            0f, 0f,
                            dp(16).toFloat(), dp(16).toFloat()
                        )
                    } else {
                        cornerRadii = floatArrayOf(
                            0f, 0f,
                            dp(16).toFloat(), dp(16).toFloat(),
                            dp(16).toFloat(), dp(16).toFloat(),
                            0f, 0f
                        )
                    }
                    setStroke(dp(1), accent.toInt())
                }
                contentDescription = "مقبض الحافة للسبحة العائمة"
                addView(label(chevron, 22f, isBold = true).apply {
                    setTextColor(accent.toInt())
                })

                var downX = 0f
                var downY = 0f
                var originX = 0
                var originY = 0
                var isMoved = false

                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            setEdgeAlpha(1.0f)
                            edgeFadeJob?.cancel()
                            originX = window.x
                            originY = window.y
                            downX = event.rawX
                            downY = event.rawY
                            isMoved = false
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - downX
                            val dy = event.rawY - downY
                            if (abs(dx) > dp(6) || abs(dy) > dp(6)) {
                                isMoved = true
                                window.x = (originX + dx.toInt()).coerceIn(
                                    0,
                                    (screenWidth() - window.width).coerceAtLeast(0)
                                )
                                window.y = (originY + dy.toInt()).coerceIn(
                                    dp(40),
                                    (screenHeight() - dp(120)).coerceAtLeast(dp(40))
                                )
                                view?.let { if (it.isAttachedToWindow) manager.updateViewLayout(it, window) }
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isMoved) {
                                val newIsRight = (window.x + window.width / 2) >= screenWidth() / 2
                                val finalX = if (newIsRight) (screenWidth() - window.width).coerceAtLeast(0) else 0
                                window.x = finalX
                                location.edit()
                                    .putBoolean("right", newIsRight)
                                    .putInt("y", window.y)
                                    .apply()
                                moveTo(Mode.Edge)
                            } else {
                                moveTo(Mode.Compact)
                            }
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            startEdgeFade()
                            true
                        }
                        else -> false
                    }
                }
            }
        }

        // ==========================================
        // 2. MODE 2: COMPACT (Everyday Tasbeeh)
        // ==========================================
        if (mode == Mode.Compact) {
            return LinearLayout(overlayContext).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(12), dp(6), dp(12), dp(10))
                background = background(20, strokeWidth = 1)

                // Outside touch detection -> collapse to Edge
                setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                        moveTo(Mode.Edge)
                        true
                    } else false
                }

                // 1. Dedicated Top Drag Pill Handle
                val dragHandle = LinearLayout(overlayContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(0, dp(2), 0, dp(4))
                    contentDescription = "اسحب لتحريك السبحة"

                    val pill = View(overlayContext).apply {
                        background = GradientDrawable().apply {
                            setColor(accent.toInt().let { (it and 0x00FFFFFF) or 0x60000000 })
                            cornerRadius = dp(4).toFloat()
                        }
                    }
                    addView(pill, LinearLayout.LayoutParams(dp(54), dp(5)))
                }
                drag(dragHandle, modeToSave = Mode.Compact)
                addView(dragHandle, LinearLayout.LayoutParams(-1, -2))

                // 2. Top row: Edge button, 2-line clickable dhikr title (opens Expanded), Expand button
                val topRow = LinearLayout(overlayContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    gravity = Gravity.CENTER_VERTICAL

                    val edgeBtn = iconBtn("⇥") { moveTo(Mode.Edge) }.apply {
                        contentDescription = "تصغير للحافة"
                    }

                    val dhikrTitle = label(item?.getEffectiveTasbeehText().orEmpty(), 14f, isBold = true).apply {
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        gravity = Gravity.CENTER
                        setPadding(dp(6), dp(2), dp(6), dp(2))
                        contentDescription = "انقر لقراءة الذكر كاملاً"
                        setOnClickListener {
                            resetInactivityTimer()
                            moveTo(Mode.Expanded)
                        }
                    }

                    val expandBtn = iconBtn("⤢") { moveTo(Mode.Expanded) }.apply {
                        contentDescription = "توسيع السبحة"
                    }

                    addView(edgeBtn, LinearLayout.LayoutParams(dp(36), dp(36)))
                    addView(dhikrTitle, LinearLayout.LayoutParams(0, -2, 1f))
                    addView(expandBtn, LinearLayout.LayoutParams(dp(36), dp(36)))
                }
                addView(topRow, LinearLayout.LayoutParams(-1, -2))

                // Middle: Large, clearly readable counter button for active counting
                val primaryCountBtn = Button(overlayContext).apply {
                    text = counterText()
                    textSize = 28f
                    setTextColor(accent.toInt())
                    backgroundTintList = null
                    background = android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf((accent.toInt() and 0x00FFFFFF) or 0x30000000),
                        GradientDrawable().apply {
                            setColor(elevated.toInt())
                            cornerRadius = dp(16).toFloat()
                            setStroke(dp(1), accent.toInt())
                        },
                        null
                    )
                    minHeight = dp(66)
                    contentDescription = "تسبيح"
                    setOnClickListener {
                        incrementCount()
                    }
                }
                counterView = primaryCountBtn
                addView(primaryCountBtn, LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, dp(6), 0, dp(6))
                })

                // Bottom control row: Undo (↶), Prev (›), Next (‹)
                val bottomRow = LinearLayout(overlayContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    gravity = Gravity.CENTER

                    val undoBtn = iconBtn("↶") { undoCount() }.apply {
                        contentDescription = "تراجع عن تسبيحة"
                    }
                    val prevBtn = iconBtn("›") { cycleDhikr(forward = false) }.apply {
                        contentDescription = "الذكر السابق"
                    }
                    val nextBtn = iconBtn("‹") { cycleDhikr(forward = true) }.apply {
                        contentDescription = "الذكر التالي"
                    }

                    addView(undoBtn, LinearLayout.LayoutParams(dp(56), dp(36)).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    })
                    addView(prevBtn, LinearLayout.LayoutParams(dp(56), dp(36)).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    })
                    addView(nextBtn, LinearLayout.LayoutParams(dp(56), dp(36)).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    })
                }
                addView(bottomRow, LinearLayout.LayoutParams(-1, -2))
            }
        }

        // ==========================================
        // 3. MODE 3: EXPANDED (Full Control Interface)
        // ==========================================
        return LinearLayout(overlayContext).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(10), dp(16), dp(14))
            background = background(22, strokeWidth = 1)

            // Outside touch detection -> collapse to Edge
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    moveTo(Mode.Edge)
                    true
                } else false
            }

            // Top drag grip & window control row
            val topRow = LinearLayout(overlayContext).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL

                val grip = View(overlayContext).apply {
                    background = GradientDrawable().apply {
                        setColor(accent.toInt().let { (it and 0x00FFFFFF) or 0x60000000 })
                        cornerRadius = dp(4).toFloat()
                    }
                    contentDescription = "اسحب لتحريك السبحة"
                }
                val gripContainer = LinearLayout(overlayContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    addView(grip, LinearLayout.LayoutParams(dp(48), dp(5)))
                }
                drag(gripContainer, modeToSave = Mode.Expanded)

                val categoryBadge = label(item?.category?.ifBlank { "التسبيح" } ?: "التسبيح", 12f).apply {
                    setTextColor(secondaryTextColor.toInt())
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                }

                val minimizeBtn = iconBtn("⤡") { moveTo(Mode.Compact) }.apply {
                    contentDescription = "تصغير إلى كبسولة"
                }
                val edgeBtn = iconBtn("⇥") { moveTo(Mode.Edge) }.apply {
                    contentDescription = "إرسال للحافة"
                }
                val closeBtn = iconBtn("✕", isAccent = true) { dismissCompletely() }.apply {
                    contentDescription = "إغلاق نهائي"
                }

                addView(gripContainer, LinearLayout.LayoutParams(-2, -2))
                addView(categoryBadge, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    setMargins(dp(6), 0, dp(6), 0)
                })
                addView(minimizeBtn, LinearLayout.LayoutParams(dp(36), dp(36)))
                addView(edgeBtn, LinearLayout.LayoutParams(dp(36), dp(36)))
                addView(closeBtn, LinearLayout.LayoutParams(dp(36), dp(36)))
            }
            addView(topRow, LinearLayout.LayoutParams(-1, -2))

            // Dhikr text row with previous (›) and next (‹) controls, and scrollable text
            val dhikrRow = LinearLayout(overlayContext).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))

                val prevBtn = iconBtn("›") { cycleDhikr(forward = false) }.apply {
                    contentDescription = "الذكر السابق"
                }
                val dhikrText = label(item?.getEffectiveTasbeehText().orEmpty(), 17f, isBold = true).apply {
                    setPadding(dp(6), dp(4), dp(6), dp(4))
                }
                val textScrollView = android.widget.ScrollView(overlayContext).apply {
                    isVerticalScrollBarEnabled = true
                    addView(dhikrText)
                }

                val nextBtn = iconBtn("‹") { cycleDhikr(forward = true) }.apply {
                    contentDescription = "الذكر التالي"
                }

                addView(prevBtn, LinearLayout.LayoutParams(dp(36), dp(36)))
                addView(textScrollView, LinearLayout.LayoutParams(0, dp(90), 1f))
                addView(nextBtn, LinearLayout.LayoutParams(dp(36), dp(36)))
            }
            addView(dhikrRow, LinearLayout.LayoutParams(-1, -2))

            // Source attribution if present
            item?.source?.takeIf { it.isNotBlank() }?.let { src ->
                addView(label(src, 12f).apply {
                    setTextColor(secondaryTextColor.toInt())
                    setPadding(0, 0, 0, dp(6))
                }, LinearLayout.LayoutParams(-1, -2))
            }

            // Prominent Central Counter Button
            val primaryCountBtn = Button(overlayContext).apply {
                text = counterText()
                textSize = 32f
                setTextColor(accent.toInt())
                backgroundTintList = null
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf((accent.toInt() and 0x00FFFFFF) or 0x30000000),
                    GradientDrawable().apply {
                        setColor(elevated.toInt())
                        cornerRadius = dp(18).toFloat()
                        setStroke(dp(1), accent.toInt())
                    },
                    null
                )
                minHeight = dp(76)
                contentDescription = "تسبيحة"
                setOnClickListener {
                    incrementCount()
                }
            }
            counterView = primaryCountBtn
            addView(primaryCountBtn, LinearLayout.LayoutParams(-1, -2))

            // Secondary controls: Undo, Reset, Goal toggle (33 / 100 / ∞)
            addView(LinearLayout(overlayContext).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(0, dp(8), 0, dp(4))

                addView(textBtn("↶ تراجع") { undoCount() }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(textBtn("↺ إعادة") { resetCount() }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(textBtn(if (free) "هدف: حر ∞" else "هدف: $target") { cycleTarget() }, LinearLayout.LayoutParams(0, -2, 1f))
            }, LinearLayout.LayoutParams(-1, -2))

            // Bottom action buttons: Return to Edge, Minimize, Close Completely
            addView(LinearLayout(overlayContext).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(0, dp(4), 0, 0)

                addView(textBtn("الحافة") { moveTo(Mode.Edge) }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(textBtn("تصغير") { moveTo(Mode.Compact) }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(textBtn("إغلاق نهائي", isAccent = true) { dismissCompletely() }, LinearLayout.LayoutParams(0, -2, 1f))
            }, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun counterText(): String = if (free) {
        ZekrRepository.toArabicDigits(count)
    } else {
        "${ZekrRepository.toArabicDigits(count)} من ${ZekrRepository.toArabicDigits(target)}"
    }

    private fun incrementCount() {
        val current = item ?: return
        triggerHaptic()
        resetInactivityTimer()
        scope.launch {
            operations.withLock {
                count++
                repo.recordTasbeehTap(current, count, target, free)
                counterView?.text = counterText()
                ZekrGlanceWidget.updateAllWidgets(applicationContext)
            }
        }
    }

    private fun undoCount() {
        val current = item ?: return
        if (count <= 0) return
        triggerHaptic()
        resetInactivityTimer()
        scope.launch {
            operations.withLock {
                val result = repo.undoLastTasbeehTap(current, target, free)
                count = result.newCount
                counterView?.text = counterText()
                ZekrGlanceWidget.updateAllWidgets(applicationContext)
            }
        }
    }

    private fun resetCount() {
        val current = item ?: return
        triggerHaptic()
        resetInactivityTimer()
        scope.launch {
            operations.withLock {
                count = 0
                repo.saveTasbeehProgress(current.id, 0, target, free, current.category)
                counterView?.text = counterText()
                ZekrGlanceWidget.updateAllWidgets(applicationContext)
            }
        }
    }

    private fun cycleTarget() {
        val current = item ?: return
        triggerHaptic()
        resetInactivityTimer()
        when {
            !free && target == 33 -> { target = 100; free = false }
            !free && target == 100 -> { free = true }
            else -> { target = 33; free = false }
        }
        scope.launch {
            operations.withLock {
                repo.saveTasbeehProgress(current.id, count, target, free, current.category)
                counterView?.text = counterText()
                if (view != null) moveTo(mode)
            }
        }
    }

    private fun cycleDhikr(forward: Boolean) {
        if (allTasbeehItems.isEmpty()) return
        triggerHaptic()
        resetInactivityTimer()
        scope.launch {
            operations.withLock {
                val currentIndex = allTasbeehItems.indexOfFirst { it.id == item?.id }
                val nextIndex = if (forward) {
                    if (currentIndex < 0 || currentIndex >= allTasbeehItems.lastIndex) 0 else currentIndex + 1
                } else {
                    if (currentIndex <= 0) allTasbeehItems.lastIndex else currentIndex - 1
                }
                val newItem = allTasbeehItems[nextIndex]
                val state = repo.selectTasbeehItem(newItem)
                item = newItem
                count = state.count
                target = state.target
                free = state.isFreeMode
                requestedId = newItem.id
                if (view != null) moveTo(mode)
                ZekrGlanceWidget.updateAllWidgets(applicationContext)
            }
        }
    }

    private fun drag(handle: View, modeToSave: Mode) {
        var originX = 0
        var originY = 0
        var downX = 0f
        var downY = 0f
        var moved = false
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    setEdgeAlpha(1.0f)
                    inactivityJob?.cancel()
                    edgeFadeJob?.cancel()
                    originX = window.x
                    originY = window.y
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    moved = moved || abs(dx) > dp(5) || abs(dy) > dp(5)
                    if (moved) {
                        window.x = (originX + dx.toInt()).coerceIn(
                            dp(6),
                            (screenWidth() - window.width - dp(6)).coerceAtLeast(0)
                        )
                        window.y = (originY + dy.toInt()).coerceIn(
                            dp(40),
                            (screenHeight() - (view?.height ?: dp(140)) - dp(80)).coerceAtLeast(dp(40))
                        )
                        view?.let { if (it.isAttachedToWindow) manager.updateViewLayout(it, window) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (moved) {
                        if (modeToSave == Mode.Compact) {
                            val isRight = window.x + window.width / 2 >= screenWidth() / 2
                            location.edit()
                                .putInt("compact_x", window.x)
                                .putInt("compact_y", window.y)
                                .putBoolean("right", isRight)
                                .apply()
                        } else if (modeToSave == Mode.Expanded) {
                            location.edit()
                                .putInt("expanded_x", window.x)
                                .putInt("expanded_y", window.y)
                                .apply()
                        }
                        resetInactivityTimer()
                        view?.let { if (it.isAttachedToWindow) manager.updateViewLayout(it, window) }
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        handle.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dismissCompletely() {
        inactivityJob?.cancel()
        edgeFadeJob?.cancel()
        try {
            view?.let {
                if (it.isAttachedToWindow) manager.removeViewImmediate(it)
            }
            view = null
        } catch (_: Exception) {}

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            val notifications = getSystemService(NotificationManager::class.java)
            notifications?.cancel(NOTIFICATION)
        } catch (_: Exception) {}

        stopSelf()
    }

    override fun onDestroy() {
        appearancePreferences.unregisterOnSharedPreferenceChangeListener(onAppearanceChanged)
        scope.cancel()
        dismissCompletely()
        super.onDestroy()
    }
}

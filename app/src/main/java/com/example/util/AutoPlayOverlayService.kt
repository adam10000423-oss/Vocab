package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

/** Keeps an active flashcard autoplay session alive and optionally mirrors it in a small overlay. */
class AutoPlayOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var showOverlay = false
    private var currentWord = ""
    private var currentDefinition = ""
    private var showingBack = false
    private var themeMode = "SYSTEM"
    private var overlayX: Int? = null
    private var overlayY: Int? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        activeInstance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                removeOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE, ACTION_START, null -> {
                currentWord = intent?.getStringExtra(EXTRA_WORD).orEmpty()
                currentDefinition = intent?.getStringExtra(EXTRA_DEFINITION).orEmpty()
                showingBack = intent?.getBooleanExtra(EXTRA_IS_BACK, false) ?: false
                showOverlay = intent?.getBooleanExtra(EXTRA_SHOW_OVERLAY, false) ?: false
                themeMode = intent?.getStringExtra(EXTRA_THEME_MODE) ?: "SYSTEM"
                startForeground(NOTIFICATION_ID, buildNotification())
                refreshOverlay()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshOverlay()
    }

    override fun onDestroy() {
        removeOverlay()
        if (activeInstance === this) activeInstance = null
        super.onDestroy()
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setContentTitle(if (showingBack) "自動播放 · 背面" else "自動播放 · 正面")
        .setContentText(displayText())
        .setStyle(NotificationCompat.BigTextStyle().bigText(displayText()))
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "學習卡自動播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "在離開 Vocab 時維持學習卡朗讀"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun displayText(): String = if (showingBack) {
        currentDefinition.ifBlank { currentWord }
    } else currentWord

    private fun refreshOverlay() {
        val allowed = showOverlay && !appVisible && Settings.canDrawOverlays(this)
        if (!allowed) {
            removeOverlay()
            return
        }
        removeOverlay()

        val density = resources.displayMetrics.density
        val dark = when (themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (11 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(if (dark) Color.rgb(25, 25, 25) else Color.WHITE)
                setStroke((1 * density).toInt().coerceAtLeast(1), if (dark) Color.rgb(70, 70, 70) else Color.rgb(220, 220, 220))
            }
            elevation = 12 * density
            contentDescription = "Vocab 自動播放浮動視窗"
            setOnClickListener {
                startActivity(
                    Intent(this@AutoPlayOverlayService, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }
        }
        container.addView(TextView(this).apply {
            text = displayText()
            setTextColor(if (dark) Color.WHITE else Color.BLACK)
            textSize = if (showingBack) 17f else 21f
            maxLines = 3
            setPadding(0, (4 * density).toInt(), 0, 0)
        })

        val params = WindowManager.LayoutParams(
            (238 * density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = overlayX ?: (resources.displayMetrics.widthPixels - width - 12 * density).toInt().coerceAtLeast(0)
            y = overlayY ?: (72 * density).toInt()
            x = x.coerceIn(0, (resources.displayMetrics.widthPixels - width).coerceAtLeast(0))
            y = y.coerceIn(0, (resources.displayMetrics.heightPixels - 120 * density).toInt().coerceAtLeast(0))
        }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        container.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) dragging = true
                    if (dragging) {
                        params.x = (startX + dx).toInt().coerceIn(0, (resources.displayMetrics.widthPixels - params.width).coerceAtLeast(0))
                        params.y = (startY + dy).toInt().coerceIn(0, (resources.displayMetrics.heightPixels - view.height).coerceAtLeast(0))
                        overlayX = params.x
                        overlayY = params.y
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        runCatching {
            windowManager.addView(container, params)
            overlayView = container
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
    }

    companion object {
        private const val CHANNEL_ID = "learning_autoplay"
        private const val NOTIFICATION_ID = 4103
        private const val ACTION_START = "com.example.action.AUTOPLAY_START"
        private const val ACTION_UPDATE = "com.example.action.AUTOPLAY_UPDATE"
        private const val ACTION_STOP = "com.example.action.AUTOPLAY_STOP"
        private const val EXTRA_WORD = "word"
        private const val EXTRA_DEFINITION = "definition"
        private const val EXTRA_IS_BACK = "is_back"
        private const val EXTRA_SHOW_OVERLAY = "show_overlay"
        private const val EXTRA_THEME_MODE = "theme_mode"

        @Volatile private var activeInstance: AutoPlayOverlayService? = null
        @Volatile private var appVisible: Boolean = true

        fun update(
            context: Context,
            word: String,
            definition: String,
            isBack: Boolean,
            showOverlay: Boolean,
            themeMode: String = "SYSTEM"
        ) {
            val intent = Intent(context, AutoPlayOverlayService::class.java)
                .setAction(ACTION_UPDATE)
                .putExtra(EXTRA_WORD, word)
                .putExtra(EXTRA_DEFINITION, definition)
                .putExtra(EXTRA_IS_BACK, isBack)
                .putExtra(EXTRA_SHOW_OVERLAY, showOverlay)
                .putExtra(EXTRA_THEME_MODE, themeMode)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutoPlayOverlayService::class.java))
        }

        fun setAppVisible(visible: Boolean) {
            appVisible = visible
            activeInstance?.refreshOverlay()
        }
    }
}

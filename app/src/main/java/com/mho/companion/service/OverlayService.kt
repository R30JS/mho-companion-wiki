package com.mho.companion.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mho.companion.MhoApp
import com.mho.companion.R
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.Monster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Lightweight permission-based reference bubble. Uses plain Android views instead of a
 * ComposeView so it has no lifecycle-owner requirements while running above the game.
 */
class OverlayService : Service() {

    companion object {
        const val EXTRA_MONSTER_ID = "monster_id"
        const val ACTION_STOP = "com.mho.companion.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "mho_overlay"
        private const val NOTIFICATION_ID = 4102

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private var bubbleView: TextView? = null
    private var panelView: LinearLayout? = null
    private var expanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        createNotificationChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is required.", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }

        val requestedId = intent?.getStringExtra(EXTRA_MONSTER_ID)
        serviceScope.launch {
            val content = (application as MhoApp).container.contentRepository.ensureLoaded()
            val monster = content?.monster(requestedId) ?: content?.monsters?.monsters?.firstOrNull()
            if (monster == null) {
                stopSelf()
                return@launch
            }
            showBubble(monster)
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mho)
            .setContentTitle("MHO Companion overlay")
            .setContentText("Monster weakness panel is running")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stopIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hunt overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monster weakness bubble shown above the game."
        }
        manager.createNotificationChannel(channel)
    }

    private fun showBubble(monster: Monster) {
        removeViews()
        expanded = false

        val accent = elementColor(monster.weakness.primary)
        val size = dp(58)
        val bubble = TextView(this).apply {
            text = monster.name.take(1).uppercase()
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            contentDescription = "${monster.name} weakness overlay"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent)
                setStroke(dp(2), Color.parseColor("#0D1117"))
            }
            elevation = dp(8).toFloat()
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(14)
            y = dp(180)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        bubble.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = (startX + (event.rawX - touchX).toInt()).coerceAtLeast(0)
                    params.y = (startY + (event.rawY - touchY).toInt()).coerceAtLeast(0)
                    runCatching { windowManager.updateViewLayout(bubble, params) }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - touchX) + abs(event.rawY - touchY)
                    if (moved < dp(12)) {
                        togglePanel(monster, params)
                    }
                    true
                }

                else -> false
            }
        }

        val added = runCatching { windowManager.addView(bubble, params) }.isSuccess
        if (!added) {
            Toast.makeText(this, "Could not show the overlay.", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }
        bubbleView = bubble
    }

    private fun togglePanel(monster: Monster, anchor: WindowManager.LayoutParams) {
        if (expanded) {
            removePanel()
            return
        }

        val accent = elementColor(monster.weakness.primary)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#F2161B22"))
                setStroke(dp(1), accent)
            }
            elevation = dp(10).toFloat()
        }

        panel.addView(
            textView(
                text = monster.name,
                color = Color.WHITE,
                sp = 16f,
                bold = true
            )
        )
        panel.addView(
            textView(
                text = "Weak: ${monster.weakness.primary.label}  ·  ${monster.weakness.secondary.label}",
                color = accent,
                sp = 12f
            )
        )
        panel.addView(
            textView(
                text = monster.title.ifBlank { "Target monster" },
                color = Color.parseColor("#8B949E"),
                sp = 11f
            )
        )

        val topParts = monster.hitzones
            .sortedByDescending { it.sever + it.blunt + it.ranged + maxOf(it.sever, it.blunt, it.ranged) * 2 }
            .take(3)
        if (topParts.isNotEmpty()) {
            panel.addView(spacer(dp(6)))
            panel.addView(
                textView(
                    text = "BEST PARTS",
                    color = Color.parseColor("#8B949E"),
                    sp = 9f,
                    bold = true
                )
            )
            topParts.forEach { zone ->
                panel.addView(
                    textView(
                        text = "• ${zone.part}   S${zone.sever}  B${zone.blunt}  R${zone.ranged}",
                        color = Color.parseColor("#E6EDF3"),
                        sp = 12f
                    )
                )
            }
        }

        panel.addView(spacer(dp(8)))
        panel.addView(
            textView(
                text = "Tap the bubble to close · drag to move",
                color = Color.parseColor("#6E7681"),
                sp = 9f
            )
        )

        val panelWidth = dp(252)
        val screenWidth = resources.displayMetrics.widthPixels
        val panelParams = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = anchor.x.coerceIn(dp(8), (screenWidth - panelWidth - dp(8)).coerceAtLeast(dp(8)))
            y = anchor.y + dp(68)
        }

        val added = runCatching { windowManager.addView(panel, panelParams) }.isSuccess
        if (added) {
            panelView = panel
            expanded = true
        }
    }

    private fun textView(text: String, color: Int, sp: Float, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(1), 0, dp(1))
        }

    private fun spacer(height: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, height)
    }

    private fun removePanel() {
        panelView?.let { view -> runCatching { windowManager.removeView(view) } }
        panelView = null
        expanded = false
    }

    private fun removeViews() {
        bubbleView?.let { view -> runCatching { windowManager.removeView(view) } }
        bubbleView = null
        removePanel()
    }

    private fun elementColor(element: Element): Int = when (element) {
        Element.FIRE -> 0xFFFF5722.toInt()
        Element.WATER -> 0xFF03A9F4.toInt()
        Element.THUNDER -> 0xFFFFC107.toInt()
        Element.ICE -> 0xFF00E5FF.toInt()
        Element.DRAGON -> 0xFF9C27B0.toInt()
        Element.NONE -> 0xFF8B949E.toInt()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        isRunning = false
        removeViews()
        serviceScope.cancel()
        super.onDestroy()
    }
}

package com.mho.companion.ui.screens.bestiary

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mho.companion.ui.components.BulletLine
import com.mho.companion.ui.components.ElementBadge
import com.mho.companion.ui.components.EmptyState
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HitzoneLegend
import com.mho.companion.ui.components.HitzoneTable
import com.mho.companion.ui.components.HunterButton
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.components.ThreatBadge
import com.mho.companion.ui.theme.AccentGreen
import com.mho.companion.ui.theme.AccentOrange
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HuntModeScreen(
    viewModel: BestiaryViewModel,
    monsterId: String,
    onBack: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = findActivity(view.context)?.window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(monsterId) {
        elapsed = 0L
        while (true) {
            delay(1000L)
            elapsed += 1L
        }
    }

    val data = content
    val monster = data?.monster(monsterId)
    val accent = monster?.weakness?.primary?.accent() ?: AccentOrange

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            data == null -> LoadingPane(modifier = Modifier.align(Alignment.Center))

            monster == null -> EmptyState(
                title = "Monster not found",
                body = "Go back and pick another target.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(text = "HUNT MODE", color = accent)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "KEEP SCREEN ON",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = onBack, modifier = Modifier.width(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Exit hunt mode",
                                tint = Color.White
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = monster.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        text = monster.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ElementBadge(element = monster.weakness.primary, filled = true)
                        ElementBadge(element = monster.weakness.secondary)
                        Spacer(Modifier.width(4.dp))
                        ThreatBadge(threat = monster.threat)
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HUNT TIME",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = formatHuntTime(elapsed),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = accent
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PRIMARY WEAKNESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = monster.weakness.primary.label.uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = accent
                            )
                        }
                    }
                }

                item {
                    HitzoneLegend()
                }

                item {
                    HitzoneTable(
                        hitzones = monster.hitzones,
                        scale = 1.15f,
                        showNotes = false
                    )
                }

                item {
                    GlassCard(accent = accent) {
                        Text(
                            text = "BEST STRATEGY",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Spacer(Modifier.height(5.dp))
                        monster.strategy.take(2).forEach { line ->
                            BulletLine(
                                text = line,
                                accent = accent,
                                color = Color.White
                            )
                        }
                    }
                }

                item {
                    HunterButton(
                        text = "Exit hunt mode",
                        onClick = onBack,
                        accent = accent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "HIGH CONTRAST · DO NOT DISTURB · SCREEN STAYS AWAKE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatHuntTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun findActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

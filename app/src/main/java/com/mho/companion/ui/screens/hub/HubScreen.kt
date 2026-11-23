package com.mho.companion.ui.screens.hub

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mho.companion.data.repository.SyncState
import com.mho.companion.domain.model.Adventurer
import com.mho.companion.domain.model.Content
import com.mho.companion.domain.model.Monster
import com.mho.companion.domain.model.RedeemCode
import com.mho.companion.domain.usecase.ResetSchedule
import com.mho.companion.domain.usecase.TeamSynergy
import com.mho.companion.domain.usecase.countdownText
import com.mho.companion.ui.components.BulletLine
import com.mho.companion.ui.components.CopyGlyph
import com.mho.companion.ui.components.ElementBadge
import com.mho.companion.ui.components.ExpandIndicator
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HunterButton
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.Monogram
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.components.RoleBadge
import com.mho.companion.ui.components.ScreenHeader
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.StarRating
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.AccentGreen
import com.mho.companion.ui.theme.AccentOrange
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GlassSurfaceHigh
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.WeakRed
import com.mho.companion.ui.theme.accent
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private data class PreHuntItem(val id: String, val label: String, val hint: String)

private val PreHuntItems = listOf(
    PreHuntItem("canteen", "Canteen meal (Well-Fed)", "Eat before every hunt; buffs last until you cart."),
    PreHuntItem("element", "Check weapon element", "Match the monster's primary weakness."),
    PreHuntItem("traps", "Trap + Tranq Bomb", "Two traps and two tranqs for a capture."),
    PreHuntItem("healing", "Mega Potions & First Aid", "10 Mega Potions and 3 First Aid Med+."),
    PreHuntItem("ammo", "Whetstone / Ammo / Coatings", "Restock ranged ammo and coatings."),
    PreHuntItem("buddy", "Buddy skills", "Bring a heal or cleanse Support buddy.")
)

@Composable
fun HubScreen(
    viewModel: HubViewModel,
    onOpenSettings: () -> Unit,
    onOpenBuilds: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val checked by viewModel.checklist.collectAsStateWithLifecycle(initialValue = emptySet<String>())
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            kicker = "Hunter Hub",
            title = "Field Terminal",
            actions = {
                IconButton(onClick = { viewModel.sync() }) {
                    if (syncState is SyncState.Syncing) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Sync data",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        val data = content
        if (data == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                LoadingPane()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ResetTimersCard() }
                item { SyncBanner(syncState) }
                item { CodesCard(codes = data.codes.codes, claimSteps = data.codes.claimSteps) }
                item {
                    PreHuntChecklistCard(
                        checked = checked,
                        onToggle = { id, value -> viewModel.toggleChecklist(id, value) }
                    )
                }
                item {
                    TeamSynergyCard(
                        content = data,
                        onOpenBuilds = onOpenBuilds
                    )
                }
                item {
                    Text(
                        text = "Fan-made companion. MHO Companion Wiki is not affiliated with Capcom or the Monster Hunter Outlanders team.",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResetTimersCard() {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000L)
        }
    }
    val snapshot = remember(now) { ResetSchedule.snapshot(now) }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE HH:mm", Locale.US) }

    GlassCard(accent = AccentCyan) {
        SectionHeader(title = "Reset countdown") {
            Text(
                text = "DEVICE TIME",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimerBlock(
                label = "DAILY RESET",
                value = snapshot.dailyLeft.countdownText(),
                caption = snapshot.dailyAt.format(formatter),
                accent = AccentCyan,
                modifier = Modifier.weight(1f)
            )
            TimerBlock(
                label = "WEEKLY RESET",
                value = snapshot.weeklyLeft.countdownText(),
                caption = snapshot.weeklyAt.format(formatter),
                accent = AccentOrange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimerBlock(
    label: String,
    value: String,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x66161B22))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = accent,
            maxLines = 1
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = TextMuted
        )
    }
}

@Composable
private fun SyncBanner(state: SyncState) {
    when (state) {
        is SyncState.Done -> {
            GlassCard(accent = AccentGreen, padding = 12.dp) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentGreen
                )
            }
        }

        is SyncState.Error -> {
            GlassCard(accent = WeakRed, padding = 12.dp) {
                Text(
                    text = "Sync: ${state.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WeakRed
                )
            }
        }

        else -> Unit
    }
}

@Composable
private fun CodesCard(codes: List<RedeemCode>, claimSteps: List<String>) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    var showSteps by rememberSaveable { mutableStateOf(false) }
    val activeCount = codes.count { it.status.equals("ACTIVE", ignoreCase = true) }

    fun copyCode(code: String) {
        clipboard.setText(AnnotatedString(code))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Toast.makeText(context, "$code copied", Toast.LENGTH_SHORT).show()
    }

    GlassCard(accent = AccentGreen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title = "Active redeem codes", modifier = Modifier.weight(1f))
            Pill(text = "$activeCount ACTIVE", color = AccentGreen)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap COPY, then paste in Menu > Settings > Redeem Code.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))

        codes.forEachIndexed { index, item ->
            val active = item.status.equals("ACTIVE", ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.code,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (active) TextPrimary else TextMuted
                        )
                        if (!active) {
                            Spacer(Modifier.width(6.dp))
                            Pill(text = item.status.uppercase(), color = TextMuted)
                        }
                    }
                    Text(
                        text = item.rewards.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    val expiry = item.expires
                    if (expiry != null) {
                        Text(
                            text = "Expires $expiry",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                            color = TextMuted
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                HunterButton(
                    text = "Copy",
                    onClick = { copyCode(item.code) },
                    accent = AccentGreen,
                    enabled = active,
                    leading = { CopyGlyph(color = if (active) AccentGreen else TextMuted) }
                )
            }
            if (index != codes.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlassBorder.copy(alpha = 0.35f))
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSteps = !showSteps }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "How to claim",
                style = MaterialTheme.typography.titleSmall,
                color = AccentCyan
            )
            Spacer(Modifier.weight(1f))
            ExpandIndicator(expanded = showSteps, tint = AccentCyan)
        }
        AnimatedVisibility(visible = showSteps) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                claimSteps.forEachIndexed { index, step ->
                    BulletLine(text = step, index = index + 1, accent = AccentCyan)
                }
            }
        }
    }
}

@Composable
private fun PreHuntChecklistCard(
    checked: Set<String>,
    onToggle: (String, Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    GlassCard(accent = AccentGreen) {
        SectionHeader(title = "Pre-hunt checklist") {
            Text(
                text = "${checked.count { id -> PreHuntItems.any { it.id == id } }}/${PreHuntItems.size} READY",
                style = MaterialTheme.typography.labelSmall,
                color = if (checked.size >= PreHuntItems.size) AccentGreen else TextMuted
            )
        }
        Spacer(Modifier.height(4.dp))
        PreHuntItems.forEach { item ->
            val isChecked = item.id in checked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle(item.id, !isChecked)
                    }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(21.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isChecked) AccentGreen.copy(alpha = 0.18f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isChecked) AccentGreen else GlassBorder,
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isChecked) TextSecondary else TextPrimary,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (!isChecked) {
                        Text(
                            text = item.hint,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                            color = TextMuted
                        )
                    }
                }
            }
        }
        if (checked.size >= PreHuntItems.size) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Loadout ready. Good hunting, hunter.",
                style = MaterialTheme.typography.bodySmall,
                color = AccentGreen
            )
        }
    }
}

@Composable
private fun TeamSynergyCard(content: Content, onOpenBuilds: () -> Unit) {
    val monsters = content.monsters.monsters
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    if (monsters.isEmpty()) return

    val selected = monsters.firstOrNull { it.id == selectedId } ?: monsters.first()
    val recommendation = remember(selected, content.characters.characters) {
        TeamSynergy.recommend(selected, content.characters.characters)
    }
    val accent = recommendation.primaryElement.accent()

    GlassCard(accent = accent) {
        SectionHeader(title = "Smart team synergy") {
            Pill(text = "1 ASSAULT · 1 DISRUPTOR · 1 SUPPORT", color = accent)
        }
        Spacer(Modifier.height(8.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurfaceHigh)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { menuOpen = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TARGET MONSTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = selected.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                ElementBadge(element = selected.weakness.primary, filled = true)
                Spacer(Modifier.width(6.dp))
                ExpandIndicator(expanded = menuOpen)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                monsters.forEach { monster ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = monster.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                ElementBadge(element = monster.weakness.primary)
                            }
                        },
                        onClick = {
                            selectedId = monster.id
                            menuOpen = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Weak to ${recommendation.primaryElement.label} · ${recommendation.onElementCount}/3 picks on-element",
            style = MaterialTheme.typography.bodySmall,
            color = accent
        )
        Spacer(Modifier.height(4.dp))

        recommendation.picks.forEach { pick ->
            AdventurerPickRow(pick.adventurer, pick.reason)
        }

        Spacer(Modifier.height(8.dp))
        HunterButton(
            text = "Open adventurer builds",
            onClick = onOpenBuilds,
            accent = accent,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdventurerPickRow(adventurer: Adventurer, reason: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Monogram(
            text = adventurer.name,
            accent = adventurer.element.accent(),
            size = 36.dp
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = adventurer.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(Modifier.width(6.dp))
                RoleBadge(role = adventurer.role)
            }
            Text(
                text = "${adventurer.element.label} · ${adventurer.weaponType.label} · $reason",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                color = TextSecondary
            )
        }
        StarRating(rating = adventurer.rating, fontSize = 10)
    }
}

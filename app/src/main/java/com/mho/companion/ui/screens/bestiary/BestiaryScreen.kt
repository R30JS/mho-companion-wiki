package com.mho.companion.ui.screens.bestiary

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.Monster
import com.mho.companion.domain.model.WishlistItem
import com.mho.companion.ui.components.AddWishlistDialog
import com.mho.companion.ui.components.ElementBadge
import com.mho.companion.ui.components.ExpandIndicator
import com.mho.companion.ui.components.FilterRow
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HunterChip
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.Monogram
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.components.ScreenHeader
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.ThreatBadge
import com.mho.companion.ui.components.ThumbZone
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.AccentGreen
import com.mho.companion.ui.theme.AccentOrange
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GoldStar
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent

@Composable
fun BestiaryScreen(
    viewModel: BestiaryViewModel,
    onOpenMonster: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle(initialValue = emptyList<WishlistItem>())

    var query by rememberSaveable { mutableStateOf("") }
    var elementFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var wishlistExpanded by rememberSaveable { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf<WishlistItem?>(null) }

    val selectedElement = elementFilter?.let { name ->
        runCatching { Element.valueOf(name) }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            kicker = "Monster Bestiary",
            title = "Hunt Board",
            actions = {
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
            val monsters = remember(data, selectedElement, query) {
                val needle = query.trim()
                data.monsters.monsters
                    .filter { monster ->
                        selectedElement == null ||
                            monster.weakness.primary == selectedElement ||
                            monster.weakness.secondary == selectedElement
                    }
                    .filter { monster ->
                        needle.isBlank() ||
                            listOf(
                                monster.name,
                                monster.title,
                                monster.habitat,
                                monster.weakness.primary.label,
                                monster.weakness.secondary.label,
                                monster.ailments.joinToString(" "),
                                monster.hitzones.joinToString(" ") { it.part }
                            ).any { it.contains(needle, ignoreCase = true) }
                    }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    WishlistCard(
                        items = wishlist,
                        expanded = wishlistExpanded,
                        onToggle = { wishlistExpanded = !wishlistExpanded },
                        onAdjust = { id, delta -> viewModel.adjustHave(id, delta) },
                        onEditTarget = { editingTarget = it },
                        onRemove = { viewModel.remove(it) }
                    )
                }
                if (monsters.isEmpty()) {
                    item {
                        GlassCard {
                            Text(
                                text = "No monsters match this filter.",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Clear the weakness chip or try another search.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(items = monsters, key = { it.id }) { monster ->
                        MonsterCard(monster = monster, onClick = { onOpenMonster(monster.id) })
                    }
                }
            }

            ThumbZone(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search monster, part, ailment…",
                filterRows = {
                    FilterRow {
                        HunterChip(
                            label = "ALL TARGETS",
                            selected = selectedElement == null,
                            onClick = { elementFilter = null }
                        )
                        Element.entries.filter { it != Element.NONE }.forEach { element ->
                            HunterChip(
                                label = "WEAK ${element.label.uppercase()}",
                                selected = selectedElement == element,
                                accent = element.accent(),
                                onClick = { elementFilter = element.name }
                            )
                        }
                    }
                }
            )
        }
    }

    val targetItem = editingTarget
    if (targetItem != null) {
        AddWishlistDialog(
            label = targetItem.label,
            source = targetItem.source,
            initialTarget = targetItem.target,
            title = "Update target",
            confirmLabel = "Update",
            onDismiss = { editingTarget = null },
            onConfirm = { qty ->
                viewModel.setTarget(targetItem.id, qty)
                editingTarget = null
            }
        )
    }
}

@Composable
private fun WishlistCard(
    items: List<WishlistItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdjust: (String, Int) -> Unit,
    onEditTarget: (WishlistItem) -> Unit,
    onRemove: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val done = items.count { it.done }
    val progress = if (items.isEmpty()) 0f else items.map { it.progress }.average().toFloat()

    GlassCard(accent = GoldStar) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Farming wishlist", modifier = Modifier.weight(1f))
            Text(
                text = if (items.isEmpty()) "EMPTY" else "$done/${items.size} READY",
                style = MaterialTheme.typography.labelSmall,
                color = if (items.isEmpty()) TextMuted else AccentGreen
            )
            Spacer(Modifier.width(6.dp))
            ExpandIndicator(expanded = expanded, tint = GoldStar)
        }

        if (items.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Open a monster or weapon and tap Add to track farming targets.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GlassBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(GoldStar)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (item.done) TextSecondary else TextPrimary
                                )
                                if (item.source.isNotBlank()) {
                                    Text(
                                        text = item.source,
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                                        color = TextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                            StepButton(label = "−") {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAdjust(item.id, -1)
                            }
                            Text(
                                text = "${item.have}/${item.target}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                color = if (item.done) AccentGreen else TextPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onEditTarget(item) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            StepButton(label = "+") {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAdjust(item.id, 1)
                            }
                            IconButton(
                                onClick = { onRemove(item.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove ${item.label}",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Tap the have/target number to edit the target.",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0x33161B22))
            .border(1.dp, GlassBorder, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = AccentCyan
        )
    }
}

@Composable
private fun MonsterCard(monster: Monster, onClick: () -> Unit) {
    val accent = monster.weakness.primary.accent()
    val bestPart = monster.hitzones.maxByOrNull { it.sever + it.blunt + it.ranged }
    GlassCard(accent = accent, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(text = monster.name, accent = accent, size = 46.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = monster.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = monster.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ElementBadge(element = monster.weakness.primary, filled = true)
                    ElementBadge(element = monster.weakness.secondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                ThreatBadge(threat = monster.threat)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = monster.habitat,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bestPart != null) {
                Text(
                    text = "WEAK SPOT · ${bestPart.part.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                Spacer(Modifier.width(8.dp))
            }
            monster.ailments.take(2).forEach { ailment ->
                Pill(text = ailment, color = AccentOrange)
                Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "open →",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

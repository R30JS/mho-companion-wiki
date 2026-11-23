package com.mho.companion.ui.screens.bestiary

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mho.companion.domain.usecase.TeamSynergy
import com.mho.companion.ui.components.AddWishlistDialog
import com.mho.companion.ui.components.BulletLine
import com.mho.companion.ui.components.ElementBadge
import com.mho.companion.ui.components.EmptyState
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HitzoneLegend
import com.mho.companion.ui.components.HitzoneTable
import com.mho.companion.ui.components.HunterButton
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.Monogram
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.components.RoleBadge
import com.mho.companion.ui.components.ScreenHeader
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.StarRating
import com.mho.companion.ui.components.ThreatBadge
import com.mho.companion.ui.theme.AccentOrange
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent

@Composable
fun MonsterDetailScreen(
    viewModel: BestiaryViewModel,
    monsterId: String,
    onBack: () -> Unit,
    onStartHunt: (String) -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    var wishlistTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val data = content
    val monster = data?.monster(monsterId)
    val accent = monster?.weakness?.primary?.accent() ?: AccentOrange

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            kicker = "Monster file",
            title = monster?.name ?: "Monster",
            onBack = onBack
        )

        if (data == null) {
            LoadingPane(modifier = Modifier.weight(1f))
        } else if (monster == null) {
            EmptyState(
                title = "Monster not found",
                body = "This file may have been removed by a data update.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    GlassCard(accent = accent) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Monogram(text = monster.name, accent = accent, size = 58.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = monster.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = monster.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ElementBadge(element = monster.weakness.primary, filled = true)
                                    ElementBadge(element = monster.weakness.secondary)
                                    Spacer(Modifier.width(2.dp))
                                    ThreatBadge(threat = monster.threat)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = monster.habitat,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                            color = TextMuted
                        )
                        if (monster.ailments.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                monster.ailments.forEach { ailment ->
                                    Pill(text = ailment, color = AccentOrange)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HunterButton(
                            text = "Hunt Screen Mode · keep screen on",
                            onClick = { onStartHunt(monster.id) },
                            accent = accent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(title = "Fight strategy")
                        Spacer(Modifier.height(6.dp))
                        monster.strategy.forEach { line ->
                            BulletLine(text = line, accent = accent, color = TextPrimary)
                        }
                    }
                }

                item {
                    GlassCard(accent = accent) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader(
                                title = "Body part hitzones",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "SEV · BLUNT · RNG",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        HitzoneLegend()
                        Spacer(Modifier.height(7.dp))
                        HitzoneTable(hitzones = monster.hitzones)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Red = weakest · yellow = medium · gray = immune. Aim for 3-dot parts.",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                            color = TextMuted
                        )
                    }
                }

                item {
                    val recommendation = remember(monster, data.characters.characters) {
                        TeamSynergy.recommend(monster, data.characters.characters)
                    }
                    GlassCard(accent = accent) {
                        SectionHeader(title = "Suggested team") {
                            Text(
                                text = "1 / 1 / 1",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        if (recommendation.picks.isEmpty()) {
                            Text(
                                text = "No adventurer data is available for this target.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            recommendation.picks.forEach { pick ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Monogram(
                                        text = pick.adventurer.name,
                                        accent = pick.adventurer.element.accent(),
                                        size = 34.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = pick.adventurer.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            RoleBadge(role = pick.role)
                                        }
                                        Text(
                                            text = "${pick.adventurer.element.label} · ${pick.adventurer.weaponType.label} · ${pick.reason}",
                                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                                            color = TextSecondary
                                        )
                                    }
                                    StarRating(rating = pick.adventurer.rating, fontSize = 10)
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard(accent = accent) {
                        SectionHeader(title = "Drop materials") {
                            Text(
                                text = "${monster.materials.size} ENTRIES",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        monster.materials.forEach { material ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = material.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Rarity ${material.rarity} · ${material.source}",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                                        color = TextMuted
                                    )
                                }
                                HunterButton(
                                    text = "Add",
                                    accent = accent,
                                    onClick = { wishlistTarget = material.name to 1 }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tracked materials appear in the Farming Wishlist at the top of the Bestiary.",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }

    val target = wishlistTarget
    if (target != null && monster != null) {
        AddWishlistDialog(
            label = target.first,
            source = "Drop · ${monster.name}",
            initialTarget = target.second,
            onDismiss = { wishlistTarget = null },
            onConfirm = { qty ->
                viewModel.addWishlist(
                    label = target.first,
                    source = "Drop · ${monster.name}",
                    target = qty
                )
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "${target.first} added to wishlist", Toast.LENGTH_SHORT).show()
                wishlistTarget = null
            }
        )
    }
}

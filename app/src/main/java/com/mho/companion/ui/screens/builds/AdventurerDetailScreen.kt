package com.mho.companion.ui.screens.builds

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.MaterialRequirement
import com.mho.companion.ui.components.AddWishlistDialog
import com.mho.companion.ui.components.AttackTypeBadge
import com.mho.companion.ui.components.BulletLine
import com.mho.companion.ui.components.ElementBadge
import com.mho.companion.ui.components.EmptyState
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HunterButton
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.Monogram
import com.mho.companion.ui.components.RoleBadge
import com.mho.companion.ui.components.ScreenHeader
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.StarRating
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent

@Composable
fun AdventurerDetailScreen(
    viewModel: BuildsViewModel,
    adventurerId: String,
    onBack: () -> Unit,
    onOpenWeapon: (Element, String) -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    var wishlistMaterial by remember { mutableStateOf<MaterialRequirement?>(null) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val data = content
    val adventurer = data?.adventurer(adventurerId)
    val weapon = adventurer?.let { data?.weaponFor(it) }
    val accent = adventurer?.element?.accent() ?: AccentCyan

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            kicker = "Adventurer profile",
            title = adventurer?.name ?: "Adventurer",
            onBack = onBack
        )

        if (data == null) {
            LoadingPane(modifier = Modifier.weight(1f))
        } else if (adventurer == null) {
            EmptyState(
                title = "Adventurer not found",
                body = "This profile may have been removed by a data update.",
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
                            Monogram(text = adventurer.name, accent = accent, size = 58.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = adventurer.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RoleBadge(role = adventurer.role)
                                    Spacer(Modifier.width(6.dp))
                                    ElementBadge(element = adventurer.element, filled = true)
                                }
                                Spacer(Modifier.height(3.dp))
                                StarRating(rating = adventurer.rating, fontSize = 12)
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(
                            text = adventurer.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                item {
                    GlassCard(accent = accent) {
                        SectionHeader(title = "Recommended weapon") {
                            AttackTypeBadge(type = adventurer.weaponType.attackType)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = adventurer.weaponName,
                            style = MaterialTheme.typography.titleLarge,
                            color = accent
                        )
                        Text(
                            text = "${adventurer.weaponType.label}" +
                                if (weapon != null) "  ·  ATK ${weapon.attack}  ·  ELEM ${weapon.elementValue}" else "",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .clickable {
                                    val id = weapon?.id
                                        ?: "${adventurer.element.name.lowercase()}-${adventurer.weaponType.code}"
                                    onOpenWeapon(adventurer.element, id)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "Open crafting details  →",
                                style = MaterialTheme.typography.titleSmall,
                                color = AccentCyan
                            )
                        }
                        if (weapon != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "MATERIALS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            weapon.materials.forEach { material ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${material.name} x${material.qty}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    HunterButton(
                                        text = "Add",
                                        accent = accent,
                                        onClick = { wishlistMaterial = material }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(title = "Skill rotation")
                        Spacer(Modifier.height(6.dp))
                        if (adventurer.rotation.isEmpty()) {
                            Text(
                                text = "No rotation recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            adventurer.rotation.forEachIndexed { index, step ->
                                BulletLine(
                                    text = step,
                                    index = index + 1,
                                    accent = accent,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(title = "Field tips")
                        Spacer(Modifier.height(6.dp))
                        if (adventurer.tips.isEmpty()) {
                            Text(
                                text = "No tips recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            adventurer.tips.forEach { tip ->
                                BulletLine(
                                    text = tip,
                                    accent = accent,
                                    color = TextPrimary
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Ratings are seed estimates from the bundled wiki data, not official tier lists.",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    val material = wishlistMaterial
    if (material != null) {
        AddWishlistDialog(
            label = material.name,
            source = "Forge material · ${adventurer?.weaponName.orEmpty()}",
            initialTarget = material.qty,
            onDismiss = { wishlistMaterial = null },
            onConfirm = { qty ->
                viewModel.addWishlist(
                    label = material.name,
                    source = "Forge · ${adventurer?.weaponName.orEmpty()}",
                    target = qty
                )
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "${material.name} added to wishlist", Toast.LENGTH_SHORT).show()
                wishlistMaterial = null
            }
        )
    }
}

package com.mho.companion.ui.screens.builds

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.MaterialRequirement
import com.mho.companion.domain.model.WeaponType
import com.mho.companion.ui.components.AddWishlistDialog
import com.mho.companion.ui.components.AttackTypeBadge
import com.mho.companion.ui.components.ElementBadge
import com.mho.companion.ui.components.EmptyState
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HunterButton
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.LoadoutDialog
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.components.ScreenHeader
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.StatBlock
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.AccentOrange
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent

@Composable
fun BuildDetailScreen(
    viewModel: BuildsViewModel,
    element: Element,
    weaponId: String,
    onBack: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    var showLoadoutDialog by remember { mutableStateOf(false) }
    var wishlistMaterial by remember { mutableStateOf<MaterialRequirement?>(null) }

    val data = content
    val elementBuild = data?.elementBuild(element)
    val weapon = elementBuild?.weapons?.firstOrNull { it.id == weaponId }
    val accent = element.accent()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            kicker = "${element.label} build",
            title = weapon?.name ?: "Weapon build",
            onBack = onBack
        )

        if (data == null) {
            LoadingPane(modifier = Modifier.weight(1f))
        } else if (elementBuild == null || weapon == null) {
            EmptyState(
                title = "Build not found",
                body = "This weapon may have been removed by a data update. Go back and pick another build.",
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
                            ElementBadge(element = element, filled = true)
                            Spacer(Modifier.width(6.dp))
                            AttackTypeBadge(type = weapon.weaponType.attackType)
                            Spacer(Modifier.weight(1f))
                            Pill(
                                text = weapon.tier.uppercase(),
                                color = if (weapon.tier.equals("Apex", true)) AccentOrange else AccentCyan
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = weapon.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = weapon.weaponType.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            StatBlock(label = "Attack", value = "${weapon.attack}", accent = accent)
                            StatBlock(label = "Element", value = "${weapon.elementValue}", accent = accent)
                            StatBlock(label = "Tier", value = weapon.tier, accent = accent)
                        }
                    }
                }

                item {
                    GlassCard(accent = accent) {
                        SectionHeader(title = "Element kit")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = elementBuild.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        val visage = elementBuild.visageSet
                        if (visage != null) {
                            Text(
                                text = "VISAGE SET · ${visage.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                            Text(
                                text = visage.bonus,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = visage.pieces.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        elementBuild.coreVisages.forEach { core ->
                            Text(
                                text = "CORE · ${core.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                            Text(
                                text = core.effect,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        elementBuild.buddies.forEach { buddy ->
                            Text(
                                text = "BUDDY · ${buddy.name} (${buddy.role})",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                            Text(
                                text = buddy.skill,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(title = "Armor skills")
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = weapon.armorSkills.joinToString("  ·  "),
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentCyan
                        )
                        Spacer(Modifier.height(10.dp))
                        SectionHeader(title = "Hunter insight")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = weapon.insight,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = TextPrimary
                        )
                    }
                }

                item {
                    GlassCard(accent = accent) {
                        SectionHeader(title = "Crafting materials") {
                            Pill(
                                text = "${weapon.materials.sumOf { it.qty }} TOTAL",
                                color = accent
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        weapon.materials.forEach { material ->
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
                                        text = "Need x${material.qty}",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                                        color = TextMuted
                                    )
                                }
                                HunterButton(
                                    text = "Add",
                                    accent = accent,
                                    onClick = { wishlistMaterial = material }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "FORGE PATH",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = weapon.forgePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                item {
                    HunterButton(
                        text = "Save as custom loadout",
                        onClick = { showLoadoutDialog = true },
                        accent = AccentCyan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "Materials can be tracked in the Bestiary tab wishlist.",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }

    if (showLoadoutDialog) {
        LoadoutDialog(
            presetElement = element,
            presetWeaponType = weapon?.weaponType ?: WeaponType.LONG_SWORD,
            presetWeaponName = weapon?.name.orEmpty(),
            onDismiss = { showLoadoutDialog = false },
            onSave = { loadout ->
                viewModel.saveLoadout(loadout)
                showLoadoutDialog = false
                Toast.makeText(context, "Loadout saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    val material = wishlistMaterial
    if (material != null) {
        AddWishlistDialog(
            label = material.name,
            source = "Forge material · ${weapon?.name.orEmpty()}",
            initialTarget = material.qty,
            onDismiss = { wishlistMaterial = null },
            onConfirm = { qty ->
                viewModel.addWishlist(
                    label = material.name,
                    source = "Forge · ${weapon?.name.orEmpty()}",
                    target = qty
                )
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "${material.name} added to wishlist", Toast.LENGTH_SHORT).show()
                wishlistMaterial = null
            }
        )
    }
}

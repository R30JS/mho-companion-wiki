package com.mho.companion.ui.screens.builds

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mho.companion.domain.model.Adventurer
import com.mho.companion.domain.model.Content
import com.mho.companion.domain.model.CustomLoadout
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.ElementBuild
import com.mho.companion.domain.model.Role
import com.mho.companion.domain.model.WeaponBuild
import com.mho.companion.domain.model.WeaponType
import com.mho.companion.ui.components.AttackTypeBadge
import com.mho.companion.ui.components.ConfirmDialog
import com.mho.companion.ui.components.ElementBadge
import com.mho.companion.ui.components.FilterRow
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HunterChip
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.LoadoutDialog
import com.mho.companion.ui.components.Monogram
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.components.RoleBadge
import com.mho.companion.ui.components.ScreenHeader
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.StarRating
import com.mho.companion.ui.components.ThumbZone
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.AccentOrange
import com.mho.companion.ui.theme.AccentViolet
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GlassSurfaceHigh
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent

@Composable
fun BuildsScreen(
    viewModel: BuildsViewModel,
    onOpenBuild: (Element, String) -> Unit,
    onOpenAdventurer: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val loadouts by viewModel.customLoadouts.collectAsStateWithLifecycle(initialValue = emptyList<CustomLoadout>())

    var segment by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var elementFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var weaponFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var roleFilter by rememberSaveable { mutableStateOf<String?>(null) }

    var showLoadoutDialog by remember { mutableStateOf(false) }
    var editingLoadout by remember { mutableStateOf<CustomLoadout?>(null) }
    var deletingLoadout by remember { mutableStateOf<CustomLoadout?>(null) }

    val selectedElement = elementFilter?.let { name ->
        runCatching { Element.valueOf(name) }.getOrNull()
    }
    val selectedWeapon = weaponFilter?.let { name ->
        runCatching { WeaponType.valueOf(name) }.getOrNull()
    }
    val selectedRole = roleFilter?.let { name ->
        runCatching { Role.valueOf(name) }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            kicker = "Builds & Adventurers",
            title = "Loadout Lab",
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

        SegmentedTabs(
            selected = segment,
            onSelect = {
                segment = it
                query = ""
                weaponFilter = null
                roleFilter = null
            }
        )
        Spacer(Modifier.height(8.dp))

        val data = content
        if (data == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                LoadingPane()
            }
        } else {
            val builds = remember(data, selectedElement, selectedWeapon, query) {
                filterBuilds(data, selectedElement, selectedWeapon, query)
            }
            val adventurers = remember(data, selectedElement, selectedWeapon, selectedRole, query) {
                filterAdventurers(data, selectedElement, selectedWeapon, selectedRole, query)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (segment == 0) {
                    item {
                        LoadoutsCard(
                            loadouts = loadouts,
                            onNew = {
                                editingLoadout = null
                                showLoadoutDialog = true
                            },
                            onEdit = {
                                editingLoadout = it
                                showLoadoutDialog = true
                            },
                            onDelete = { deletingLoadout = it }
                        )
                    }
                    if (builds.isEmpty()) {
                        item {
                            EmptyBuilds()
                        }
                    } else {
                        items(items = builds, key = { it.weapon.id }) { pair ->
                            BuildCard(
                                elementBuild = pair.element,
                                weapon = pair.weapon,
                                onOpen = { onOpenBuild(pair.element.element, pair.weapon.id) }
                            )
                        }
                    }
                } else {
                    if (adventurers.isEmpty()) {
                        item {
                            EmptyAdventurers()
                        }
                    } else {
                        items(items = adventurers, key = { it.id }) { adventurer ->
                            AdventurerCard(
                                adventurer = adventurer,
                                weapon = data.weaponFor(adventurer),
                                onOpen = { onOpenAdventurer(adventurer.id) },
                                onOpenWeapon = { weaponId -> onOpenBuild(adventurer.element, weaponId) }
                            )
                        }
                    }
                }
            }

            ThumbZone(
                query = query,
                onQueryChange = { query = it },
                placeholder = if (segment == 0) "Search weapon, skill, visage…" else "Search adventurer, role, weapon…",
                filterRows = {
                    FilterRow {
                        HunterChip(
                            label = "ALL ELEMENTS",
                            selected = selectedElement == null,
                            onClick = { elementFilter = null }
                        )
                        Element.entries.filter { it != Element.NONE }.forEach { element ->
                            HunterChip(
                                label = element.label.uppercase(),
                                selected = selectedElement == element,
                                accent = element.accent(),
                                onClick = { elementFilter = element.name }
                            )
                        }
                    }
                    FilterRow {
                        HunterChip(
                            label = "ALL WEAPONS",
                            selected = selectedWeapon == null,
                            onClick = { weaponFilter = null }
                        )
                        WeaponType.entries.forEach { weapon ->
                            HunterChip(
                                label = weapon.label,
                                selected = selectedWeapon == weapon,
                                onClick = { weaponFilter = weapon.name }
                            )
                        }
                    }
                    if (segment == 1) {
                        FilterRow {
                            HunterChip(
                                label = "ALL ROLES",
                                selected = selectedRole == null,
                                onClick = { roleFilter = null }
                            )
                            Role.entries.forEach { role ->
                                HunterChip(
                                    label = role.label.uppercase(),
                                    selected = selectedRole == role,
                                    accent = roleAccent(role),
                                    onClick = { roleFilter = role.name }
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    if (showLoadoutDialog) {
        LoadoutDialog(
            initial = editingLoadout,
            onDismiss = {
                showLoadoutDialog = false
                editingLoadout = null
            },
            onSave = { loadout ->
                viewModel.saveLoadout(loadout)
                showLoadoutDialog = false
                editingLoadout = null
            }
        )
    }

    deletingLoadout?.let { loadout ->
        ConfirmDialog(
            title = "Delete loadout?",
            body = "\"${loadout.name}\" will be removed from this device.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteLoadout(loadout.id)
                deletingLoadout = null
            },
            onDismiss = { deletingLoadout = null }
        )
    }
}

private fun roleAccent(role: Role): Color = when (role) {
    Role.ASSAULT -> AccentOrange
    Role.DISRUPTOR -> AccentViolet
    Role.SUPPORT -> com.mho.companion.ui.theme.AccentGreen
}

private data class BuildPair(val element: ElementBuild, val weapon: WeaponBuild)

private fun filterBuilds(
    content: Content,
    element: Element?,
    weapon: WeaponType?,
    query: String
): List<BuildPair> {
    val needle = query.trim()
    return content.builds.elements.flatMap { elementBuild ->
        elementBuild.weapons
            .filter { weapon == null || it.weaponType == weapon }
            .map { BuildPair(elementBuild, it) }
    }.filter { pair ->
        (element == null || pair.element.element == element) &&
            (
                needle.isBlank() ||
                    listOf(
                        pair.weapon.name,
                        pair.weapon.weaponType.label,
                        pair.weapon.tier,
                        pair.element.element.label,
                        pair.element.visageSet?.name.orEmpty(),
                        pair.weapon.armorSkills.joinToString(" ")
                    ).any { it.contains(needle, ignoreCase = true) }
                )
    }
}

private fun filterAdventurers(
    content: Content,
    element: Element?,
    weapon: WeaponType?,
    role: Role?,
    query: String
): List<Adventurer> {
    val needle = query.trim()
    return content.characters.characters
        .filter { element == null || it.element == element }
        .filter { weapon == null || it.weaponType == weapon }
        .filter { role == null || it.role == role }
        .filter { adventurer ->
            needle.isBlank() ||
                listOf(
                    adventurer.name,
                    adventurer.role.label,
                    adventurer.element.label,
                    adventurer.weaponType.label,
                    adventurer.weaponName,
                    adventurer.summary
                ).any { it.contains(needle, ignoreCase = true) }
        }
        .sortedWith(compareByDescending<Adventurer> { it.rating }.thenBy { it.name })
}

@Composable
private fun SegmentedTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceHigh)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        listOf("Builds", "Adventurers").forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) AccentCyan.copy(alpha = 0.14f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (active) AccentCyan.copy(alpha = 0.45f) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) AccentCyan else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun LoadoutsCard(
    loadouts: List<CustomLoadout>,
    onNew: () -> Unit,
    onEdit: (CustomLoadout) -> Unit,
    onDelete: (CustomLoadout) -> Unit
) {
    GlassCard(accent = AccentViolet) {
        SectionHeader(title = "My custom loadouts") {
            Text(
                text = "NEW +",
                style = MaterialTheme.typography.labelSmall,
                color = AccentViolet,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onNew)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        if (loadouts.isEmpty()) {
            Text(
                text = "Save your own element/weapon combination and notes. Stored locally, included in backups.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } else {
            loadouts.sortedByDescending { it.createdAt }.forEach { loadout ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEdit(loadout) }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = loadout.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ElementBadge(element = loadout.element)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = loadout.weaponType.label,
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = { onDelete(loadout) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete loadout",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildCard(
    elementBuild: ElementBuild,
    weapon: WeaponBuild,
    onOpen: () -> Unit
) {
    val accent = elementBuild.element.accent()
    GlassCard(accent = accent, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ElementBadge(element = elementBuild.element, filled = true)
            Spacer(Modifier.width(6.dp))
            AttackTypeBadge(type = weapon.weaponType.attackType)
            Spacer(Modifier.weight(1f))
            Pill(
                text = weapon.tier.uppercase(),
                color = if (weapon.tier.equals("Apex", true)) AccentOrange else AccentCyan
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = weapon.name,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "${weapon.weaponType.label}  ·  ATK ${weapon.attack}  ·  ELEM ${weapon.elementValue}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.2.sp
            ),
            color = accent
        )
        Spacer(Modifier.height(7.dp))
        val visage = elementBuild.visageSet
        if (visage != null) {
            KeyLine(label = "VISAGE", value = visage.name, accent = accent)
        }
        val cores = elementBuild.coreVisages.joinToString(" · ") { it.name }
        if (cores.isNotBlank()) {
            KeyLine(label = "CORE", value = cores)
        }
        val buddies = elementBuild.buddies.joinToString(" · ") { it.name }
        if (buddies.isNotBlank()) {
            KeyLine(label = "BUDDY", value = buddies)
        }
        if (weapon.armorSkills.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = weapon.armorSkills.joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                color = AccentCyan
            )
        }
        if (weapon.insight.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = weapon.insight,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Open materials  →",
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}

@Composable
private fun KeyLine(label: String, value: String, accent: Color = TextPrimary) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = accent
        )
    }
}

@Composable
private fun AdventurerCard(
    adventurer: Adventurer,
    weapon: WeaponBuild?,
    onOpen: () -> Unit,
    onOpenWeapon: (String) -> Unit
) {
    val accent = adventurer.element.accent()
    GlassCard(accent = accent, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(text = adventurer.name, accent = accent, size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = adventurer.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    RoleBadge(role = adventurer.role)
                }
                Text(
                    text = "${adventurer.element.label} · ${adventurer.weaponType.label}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                    color = TextSecondary
                )
            }
            StarRating(rating = adventurer.rating, fontSize = 11)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = adventurer.summary,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = adventurer.weaponName,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenWeapon(weapon?.id ?: "${adventurer.element.name.lowercase()}-${adventurer.weaponType.code}") }
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "crafting →",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        if (adventurer.rotation.isNotEmpty()) {
            Text(
                text = "Rotation: ${adventurer.rotation.first()}",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyBuilds() {
    GlassCard {
        Text(
            text = "No builds match this filter.",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Text(
            text = "Clear the element or weapon chip below, or try another search.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun EmptyAdventurers() {
    GlassCard {
        Text(
            text = "No adventurers match this filter.",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Text(
            text = "Try a different element, weapon, or role.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

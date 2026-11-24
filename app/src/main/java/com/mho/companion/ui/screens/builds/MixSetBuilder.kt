package com.mho.companion.ui.screens.builds

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mho.companion.domain.model.ArmorPiece
import com.mho.companion.domain.model.ArmorSkill
import com.mho.companion.domain.model.ArmorSlot
import com.mho.companion.domain.model.Content
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.MaterialRequirement
import com.mho.companion.domain.model.MixSetPiece
import com.mho.companion.domain.model.SavedMixSet
import com.mho.companion.domain.model.WeaponBuild
import com.mho.companion.domain.usecase.MixSetCalculator
import com.mho.companion.domain.usecase.MixSetTotals
import com.mho.companion.domain.usecase.MixSkillTotal
import com.mho.companion.ui.components.ConfirmDialog
import com.mho.companion.ui.components.ExpandIndicator
import com.mho.companion.ui.components.FilterRow
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HunterButton
import com.mho.companion.ui.components.HunterChip
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.StarRating
import com.mho.companion.ui.components.StatBlock
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.AccentGreen
import com.mho.companion.ui.theme.AccentViolet
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GlassSurfaceHigh
import com.mho.companion.ui.theme.GoldStar
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent
import java.util.UUID

private enum class MixSlot(val label: String, val armorSlot: ArmorSlot?) {
    WEAPON("Weapon", null),
    HEAD("Head", ArmorSlot.HEAD),
    CHEST("Chest", ArmorSlot.CHEST),
    ARMS("Arms", ArmorSlot.ARMS),
    WAIST("Waist", ArmorSlot.WAIST),
    LEGS("Legs", ArmorSlot.LEGS)
}

private data class WeaponChoice(
    val element: Element,
    val weapon: WeaponBuild
)

private data class PickerEntry(
    val id: String,
    val name: String,
    val subtitle: String,
    val element: Element,
    val rarity: Int,
    val skills: List<ArmorSkill>,
    val materials: List<MaterialRequirement>
) {
    val maxSkillLevel: Int get() = skills.maxOfOrNull { it.level } ?: 0
}

@Composable
fun MixSetBuilderSection(
    content: Content,
    mixSets: List<SavedMixSet>,
    modifier: Modifier = Modifier,
    onSaveMixSet: (SavedMixSet) -> Unit,
    onDeleteMixSet: (String) -> Unit,
    onAddMaterials: (List<Pair<String, Int>>, String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedWeaponId by remember { mutableStateOf<String?>(null) }
    var selectedArmorIds by remember { mutableStateOf<Map<ArmorSlot, String>>(emptyMap()) }
    var editingMixSet by remember { mutableStateOf<SavedMixSet?>(null) }
    var currentName by remember { mutableStateOf("") }
    var pickerSlot by remember { mutableStateOf<MixSlot?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var deletingMixSet by remember { mutableStateOf<SavedMixSet?>(null) }
    var summaryExpanded by rememberSaveable { mutableStateOf(true) }

    val weaponChoices = remember(content) {
        content.builds.elements.flatMap { elementBuild ->
            elementBuild.weapons.map { WeaponChoice(elementBuild.element, it) }
        }
    }
    val selectedWeaponChoice = weaponChoices.firstOrNull { it.weapon.id == selectedWeaponId }
    val selectedArmors = remember(content, selectedArmorIds) {
        MixSlot.entries.mapNotNull { slot ->
            slot.armorSlot?.let { armorSlot -> content.armor(selectedArmorIds[armorSlot]) }
        }
    }
    val totals = remember(selectedWeaponChoice, selectedArmors) {
        MixSetCalculator.totals(selectedWeaponChoice?.weapon, selectedArmors)
    }

    val loadMixSet: (SavedMixSet) -> Unit = { saved ->
        editingMixSet = saved
        currentName = saved.name
        selectedWeaponId = saved.weaponId
        selectedArmorIds = saved.pieces.mapNotNull { piece ->
            content.armor(piece.armorId)?.let { piece.slot to it.id }
        }.toMap()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Mix-Set Builder",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Build six slots, filter armor skills, and watch live totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            MixSummaryCard(
                totals = totals,
                expanded = summaryExpanded,
                onToggle = { summaryExpanded = !summaryExpanded }
            )
        }

        item {
            Column {
                SectionHeader(title = "Six-slot mix")
                Spacer(Modifier.height(7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MixSlot.entries.chunked(2).forEach { rowSlots ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowSlots.forEach { slot ->
                                val armor: ArmorPiece? = slot.armorSlot?.let { content.armor(selectedArmorIds[it]) }
                                SlotCard(
                                    modifier = Modifier.weight(1f),
                                    slot = slot,
                                    armor = armor,
                                    weapon = if (slot == MixSlot.WEAPON) selectedWeaponChoice?.weapon else null,
                                    element = if (slot == MixSlot.WEAPON) selectedWeaponChoice?.element else armor?.elementRes,
                                    rarity = when {
                                        slot == MixSlot.WEAPON && selectedWeaponChoice != null ->
                                            if (selectedWeaponChoice.weapon.tier.equals("Apex", true)) 5 else 3
                                        armor != null -> armor.rarity
                                        else -> 0
                                    },
                                    skills = if (slot == MixSlot.WEAPON) {
                                        MixSetCalculator.weaponSkills(selectedWeaponChoice?.weapon)
                                    } else {
                                        armor?.skills.orEmpty()
                                    },
                                    onClick = { pickerSlot = slot },
                                    onClear = {
                                        if (slot == MixSlot.WEAPON) {
                                            selectedWeaponId = null
                                        } else {
                                            slot.armorSlot?.let { armorSlot ->
                                                selectedArmorIds = selectedArmorIds - armorSlot
                                            }
                                        }
                                    }
                                )
                            }
                            if (rowSlots.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HunterButton(
                    text = "Save Mix-Set",
                    onClick = { showSaveDialog = true },
                    enabled = totals.equippedCount > 0,
                    accent = AccentCyan,
                    modifier = Modifier.fillMaxWidth()
                )
                HunterButton(
                    text = "Add All Materials to Wishlist",
                    onClick = {
                        if (totals.materials.isNotEmpty()) {
                            val sourceName = currentName.ifBlank { "Custom mix" }
                            onAddMaterials(
                                totals.materials.map { it.name to it.qty },
                                "Mix-set · $sourceName"
                            )
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(
                                context,
                                "${totals.materials.size} materials added to wishlist",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = totals.materials.isNotEmpty(),
                    accent = AccentGreen,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (mixSets.isNotEmpty()) {
            item {
                GlassCard(accent = AccentViolet) {
                    SectionHeader(title = "Saved mix-sets") {
                        Pill(text = "${mixSets.size}", color = AccentViolet)
                    }
                    Spacer(Modifier.height(4.dp))
                    mixSets.sortedByDescending { it.createdAt }.forEach { saved ->
                        SavedMixSetRow(
                            mixSet = saved,
                            onLoad = { loadMixSet(saved) },
                            onDelete = { deletingMixSet = saved }
                        )
                    }
                }
            }
        }
    }

    pickerSlot?.let { slot ->
        val entries = remember(content, slot) {
            if (slot == MixSlot.WEAPON) {
                weaponChoices.map { choice ->
                    val weapon = choice.weapon
                    PickerEntry(
                        id = weapon.id,
                        name = weapon.name,
                        subtitle = "${choice.element.label} · ${weapon.weaponType.label} · ATK ${weapon.attack} · ELEM ${weapon.elementValue}",
                        element = choice.element,
                        rarity = if (weapon.tier.equals("Apex", true)) 5 else 3,
                        skills = MixSetCalculator.weaponSkills(weapon),
                        materials = weapon.materials
                    )
                }
            } else {
                content.armorsFor(slot.armorSlot ?: ArmorSlot.HEAD).map { armor ->
                    PickerEntry(
                        id = armor.id,
                        name = armor.name,
                        subtitle = "${armor.elementRes.label} RES · ${armor.rarity}★",
                        element = armor.elementRes,
                        rarity = armor.rarity,
                        skills = armor.skills,
                        materials = armor.materials
                    )
                }
            }
        }
        val selectedId = if (slot == MixSlot.WEAPON) {
            selectedWeaponId
        } else {
            slot.armorSlot?.let { selectedArmorIds[it] }
        }
        PickerDialog(
            title = "Choose ${slot.label}",
            entries = entries,
            selectedId = selectedId,
            onSelect = { entry ->
                if (slot == MixSlot.WEAPON) {
                    selectedWeaponId = entry.id
                } else {
                    slot.armorSlot?.let { armorSlot ->
                        selectedArmorIds = selectedArmorIds + (armorSlot to entry.id)
                    }
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                pickerSlot = null
            },
            onDismiss = { pickerSlot = null }
        )
    }

    if (showSaveDialog) {
        MixSetNameDialog(
            initialName = currentName.ifBlank { "Mix-Set ${mixSets.size + 1}" },
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                val pieces = MixSlot.entries.mapNotNull { slot ->
                    val armorSlot = slot.armorSlot ?: return@mapNotNull null
                    selectedArmorIds[armorSlot]?.let { MixSetPiece(armorSlot, it) }
                }
                val saved = SavedMixSet(
                    id = editingMixSet?.id ?: UUID.randomUUID().toString(),
                    name = name,
                    weaponId = selectedWeaponChoice?.weapon?.id,
                    weaponName = selectedWeaponChoice?.weapon?.name.orEmpty(),
                    pieces = pieces,
                    createdAt = editingMixSet?.createdAt ?: System.currentTimeMillis()
                )
                onSaveMixSet(saved)
                editingMixSet = saved
                currentName = name
                showSaveDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Mix-set saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    deletingMixSet?.let { saved ->
        ConfirmDialog(
            title = "Delete mix-set?",
            body = "\"${saved.name}\" will be removed from this device. Wishlist items stay intact.",
            confirmLabel = "Delete",
            onConfirm = {
                onDeleteMixSet(saved.id)
                if (editingMixSet?.id == saved.id) editingMixSet = null
                deletingMixSet = null
            },
            onDismiss = { deletingMixSet = null }
        )
    }
}

@Composable
private fun MixSummaryCard(
    totals: MixSetTotals,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    GlassCard(accent = AccentCyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                SectionHeader(title = "Live skill summary")
                Text(
                    text = "${totals.equippedCount}/6 slots equipped · ${totals.skills.size} effects",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                ExpandIndicator(expanded = expanded)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StatBlock(label = "Slots", value = "${totals.equippedCount}/6", accent = AccentCyan)
            StatBlock(
                label = "Rarity",
                value = if (totals.maxRarity > 0) "★".repeat(totals.maxRarity) else "—",
                accent = GoldStar
            )
            StatBlock(label = "Materials", value = "${totals.totalMaterialCount}", accent = AccentGreen)
        }
        if (totals.resistance.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = totals.resistance.entries
                    .filter { it.value > 0 }
                    .joinToString("  ·  ") { "${it.key.label.uppercase()} ${it.value}" },
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                color = TextMuted
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            if (totals.skills.isEmpty()) {
                Text(
                    text = "Pick a weapon or armor piece to see live skill totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                totals.skills.forEach { skill ->
                    SkillTotalRow(skill = skill)
                }
            }
        }
    }
}

@Composable
private fun SkillTotalRow(skill: MixSkillTotal) {
    val activeColor = if (skill.isMaxed) GoldStar else AccentCyan
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = skill.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (skill.isMaxed) GoldStar else TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Lv.${skill.level}/${skill.maxLevel}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = activeColor
            )
            if (skill.isMaxed) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "MAX",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldStar
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(skill.maxLevel) { index ->
                Text(
                    text = "★",
                    fontSize = 11.sp,
                    color = if (index < skill.level) activeColor else TextMuted.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: MixSlot,
    armor: ArmorPiece?,
    weapon: WeaponBuild?,
    element: Element?,
    rarity: Int,
    skills: List<ArmorSkill>,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedName = weapon?.name ?: armor?.name
    val selected = selectedName != null
    val accent = element?.accent() ?: AccentCyan
    GlassCard(
        modifier = modifier,
        accent = if (selected) accent else null,
        onClick = onClick,
        padding = 10.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = slot.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accent else TextMuted
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                Text(
                    text = "CLEAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = selectedName ?: "Tap to choose",
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (selected) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (weapon != null) {
                    "${weapon.weaponType.label} · ATK ${weapon.attack}"
                } else {
                    "${armor?.elementRes?.label.orEmpty()} RES"
                },
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.15.sp),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (rarity > 0) {
                Spacer(Modifier.height(2.dp))
                StarRating(rating = rarity, fontSize = 9)
            }
            if (skills.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = skills.take(2).joinToString(" · ") { "${it.skillName} ${it.level}" },
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PickerDialog(
    title: String,
    entries: List<PickerEntry>,
    selectedId: String?,
    onSelect: (PickerEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var skillFilter by remember { mutableStateOf<String?>(null) }
    var sortBySkill by remember { mutableStateOf(true) }
    val allSkills = remember(entries) {
        entries.flatMap { it.skills }.map { it.skillName }.distinct().sorted()
    }
    val filtered = remember(entries, skillFilter, sortBySkill) {
        val base = entries.filter { entry ->
            skillFilter == null || entry.skills.any { it.skillName == skillFilter }
        }
        if (sortBySkill) {
            base.sortedWith(
                compareByDescending<PickerEntry> { it.maxSkillLevel }
                    .thenByDescending { it.rarity }
                    .thenBy { it.name }
            )
        } else {
            base.sortedWith(compareByDescending<PickerEntry> { it.rarity }.thenBy { it.name })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.mho.companion.ui.theme.GlassSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(460.dp)) {
                FilterRow {
                    HunterChip(
                        label = "ALL SKILLS",
                        selected = skillFilter == null,
                        onClick = { skillFilter = null }
                    )
                    allSkills.forEach { skill ->
                        HunterChip(
                            label = skill.uppercase(),
                            selected = skillFilter == skill,
                            onClick = {
                                skillFilter = if (skillFilter == skill) null else skill
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HunterChip(
                        label = "SKILL LV ↓",
                        selected = sortBySkill,
                        onClick = { sortBySkill = true }
                    )
                    HunterChip(
                        label = "RARITY ↓",
                        selected = !sortBySkill,
                        onClick = { sortBySkill = false }
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${filtered.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No armor matches this skill filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items = filtered, key = { it.id }) { entry ->
                            PickerRow(
                                entry = entry,
                                selected = entry.id == selectedId,
                                onClick = { onSelect(entry) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AccentCyan) }
        }
    )
}

@Composable
private fun PickerRow(
    entry: PickerEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = entry.element.accent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurfaceHigh.copy(alpha = 0.62f))
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.7f) else GlassBorder.copy(alpha = 0.55f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = entry.subtitle,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.15.sp),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.skills.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.skills.joinToString("  ·  ") { "${it.skillName} ${it.level}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            StarRating(rating = entry.rarity, fontSize = 10)
            if (selected) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = AccentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "EQUIPPED",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedMixSetRow(
    mixSet: SavedMixSet,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onLoad)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mixSet.name,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = listOfNotNull(
                    mixSet.weaponName.takeIf { it.isNotBlank() },
                    "${mixSet.pieces.size}/5 armor"
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.15.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "LOAD",
            style = MaterialTheme.typography.labelSmall,
            color = AccentViolet,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete mix-set",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MixSetNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.mho.companion.ui.theme.GlassSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Save mix-set") },
        text = {
            Column {
                Text(
                    text = "Name this armor and weapon combination. It is stored locally and included in JSON backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Mix-set name") },
                    singleLine = true,
                    colors = mixFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = if (name.isNotBlank()) AccentCyan else TextSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun mixFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = GlassBorder,
    cursorColor = AccentCyan,
    focusedLabelColor = AccentCyan,
    unfocusedLabelColor = TextSecondary
)

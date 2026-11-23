package com.mho.companion.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mho.companion.domain.model.CustomLoadout
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.WeaponType
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GlassSurface
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent
import java.util.UUID

@Composable
private fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = GlassBorder,
    cursorColor = AccentCyan,
    focusedLabelColor = AccentCyan,
    unfocusedLabelColor = TextSecondary
)

@Composable
fun AddWishlistDialog(
    label: String,
    source: String,
    initialTarget: Int = 1,
    title: String = "Add to wishlist",
    confirmLabel: String = "Add",
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var target by remember { mutableStateOf(initialTarget.coerceAtLeast(1).toString()) }
    val parsed = target.toIntOrNull()
    val valid = parsed != null && parsed in 1..999

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title) },
        text = {
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                if (source.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(source, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = target,
                    onValueChange = { value ->
                        if (value.length <= 3 && value.all { it.isDigit() }) target = value
                    },
                    label = { Text("Target quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    parsed?.let { amount ->
                        if (amount in 1..999) onConfirm(amount)
                    }
                },
                enabled = valid
            ) { Text(confirmLabel, color = if (valid) AccentCyan else TextSecondary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun LoadoutDialog(
    initial: CustomLoadout? = null,
    presetElement: Element = Element.NONE,
    presetWeaponType: WeaponType = WeaponType.LONG_SWORD,
    presetWeaponName: String = "",
    onDismiss: () -> Unit,
    onSave: (CustomLoadout) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: presetWeaponName) }
    var weaponName by remember { mutableStateOf(initial?.weaponName ?: presetWeaponName) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var element by remember { mutableStateOf(initial?.element ?: presetElement) }
    var weaponType by remember { mutableStateOf(initial?.weaponType ?: presetWeaponType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(if (initial == null) "New custom loadout" else "Edit loadout") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Loadout name") },
                    singleLine = true,
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = weaponName,
                    onValueChange = { weaponName = it },
                    label = { Text("Weapon") },
                    singleLine = true,
                    colors = fieldColors()
                )
                SectionHeader(title = "Element")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Element.entries.forEach { entry ->
                        HunterChip(
                            label = entry.label,
                            selected = element == entry,
                            accent = entry.accent(),
                            onClick = { element = entry }
                        )
                    }
                }
                SectionHeader(title = "Weapon type")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WeaponType.entries.forEach { entry ->
                        HunterChip(
                            label = entry.label,
                            selected = weaponType == entry,
                            onClick = { weaponType = entry }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / rotation") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )
                Text(
                    text = "Saved locally on this device. Export a backup from Settings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            CustomLoadout(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                element = element,
                                weaponType = weaponType,
                                weaponName = weaponName.trim(),
                                notes = notes.trim(),
                                createdAt = initial?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Save", color = if (name.isNotBlank()) AccentCyan else TextSecondary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    danger: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) androidx.compose.ui.graphics.Color(0xFFFF7B72) else AccentCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

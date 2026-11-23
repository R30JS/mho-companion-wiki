package com.mho.companion.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mho.companion.data.repository.SyncState
import com.mho.companion.domain.model.CustomLoadout
import com.mho.companion.domain.model.WishlistItem
import com.mho.companion.ui.components.ConfirmDialog
import com.mho.companion.ui.components.ElementBadge
import com.mho.companion.ui.components.ExpandIndicator
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HunterButton
import com.mho.companion.ui.components.KeyValueRow
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.ScreenHeader
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.AccentGreen
import com.mho.companion.ui.theme.AccentViolet
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GlassSurfaceHigh
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.WeakRed

private val ModuleOrder = listOf("codes", "builds", "characters", "monsters", "guides")

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle(initialValue = emptyList<WishlistItem>())
    val loadouts by viewModel.customLoadouts.collectAsStateWithLifecycle(initialValue = emptyList<CustomLoadout>())
    val checklist by viewModel.checklist.collectAsStateWithLifecycle(initialValue = emptySet<String>())
    val overlayPermission by viewModel.overlayPermission.collectAsStateWithLifecycle()
    val overlayRunning by viewModel.overlayRunning.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedMonsterId by rememberSaveable { mutableStateOf<String?>(null) }
    var monsterMenuOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.import(uri)
    }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshOverlayState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshOverlayState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(message) {
        val text = message
        if (!text.isNullOrBlank()) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }
    }

    val data = content
    val monsters = data?.monsters?.monsters.orEmpty()
    val selectedMonster = monsters.firstOrNull { it.id == selectedMonsterId } ?: monsters.firstOrNull()
    val remoteBase = data?.versions?.remoteBaseUrl.orEmpty()
    val syncing = syncState is SyncState.Syncing

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            kicker = "Companion settings",
            title = "Field Kit",
            onBack = onBack
        )

        if (data == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LoadingPane()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    GlassCard(accent = AccentCyan) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader(
                                title = "Data modules",
                                modifier = Modifier.weight(1f)
                            )
                            Pill(
                                text = if (remoteBase.isBlank()) "OFFLINE" else "REMOTE",
                                color = if (remoteBase.isBlank()) TextMuted else AccentGreen
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (remoteBase.isBlank()) {
                                "Remote sync is not configured. All six modules are bundled in the APK and work offline."
                            } else {
                                remoteBase
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        ModuleOrder.forEach { name ->
                            val version = data.installedVersions[name] ?: 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "v$version",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = AccentCyan
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HunterButton(
                                text = "Sync now",
                                onClick = { viewModel.sync() },
                                accent = AccentCyan,
                                enabled = !syncing,
                                leading = {
                                    if (syncing) {
                                        CircularProgressIndicator(
                                            color = AccentCyan,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.width(14.dp).height(14.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.width(16.dp).height(16.dp)
                                        )
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (syncing) "Checking remote versions…" else "Assets always stay available offline.",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        when (val state = syncState) {
                            is SyncState.Done -> {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentGreen
                                )
                            }

                            is SyncState.Error -> {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WeakRed
                                )
                            }

                            else -> Unit
                        }
                    }
                }

                item {
                    GlassCard(accent = AccentGreen) {
                        SectionHeader(title = "Backup & restore")
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "Export a single JSON file with your checklist, farming wishlist, and custom loadouts. Content modules are not included.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(text = "${checklist.size} CHECKED", color = AccentGreen)
                            Pill(text = "${wishlist.size} WISHLIST", color = AccentGreen)
                            Pill(text = "${loadouts.size} LOADOUTS", color = AccentGreen)
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HunterButton(
                                text = "Export JSON",
                                onClick = { exportLauncher.launch("mho-companion-backup.json") },
                                accent = AccentGreen,
                                modifier = Modifier.weight(1f)
                            )
                            HunterButton(
                                text = "Import JSON",
                                onClick = {
                                    importLauncher.launch(
                                        arrayOf(
                                            "application/json",
                                            "text/plain",
                                            "application/octet-stream"
                                        )
                                    )
                                },
                                accent = AccentCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Import replaces local user data. Module cache and settings stay intact.",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                            color = TextMuted
                        )
                    }
                }

                item {
                    GlassCard(accent = AccentViolet) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader(
                                title = "Floating hunt overlay",
                                modifier = Modifier.weight(1f)
                            )
                            Pill(
                                text = when {
                                    overlayRunning -> "RUNNING"
                                    overlayPermission -> "READY"
                                    else -> "PERMISSION"
                                },
                                color = when {
                                    overlayRunning -> AccentGreen
                                    overlayPermission -> AccentViolet
                                    else -> WeakRed
                                }
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "A draggable bubble sits above the game and expands into a weakness and hitzone card. It needs the Android overlay permission and runs as a foreground service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(9.dp))
                        Text(
                            text = "OVERLAY TARGET",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlassSurfaceHigh)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .clickable { monsterMenuOpen = true }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedMonster?.name ?: "No monster data",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                selectedMonster?.let { monster ->
                                    ElementBadge(element = monster.weakness.primary, filled = true)
                                    Spacer(Modifier.width(6.dp))
                                }
                                ExpandIndicator(expanded = monsterMenuOpen)
                            }
                            DropdownMenu(
                                expanded = monsterMenuOpen,
                                onDismissRequest = { monsterMenuOpen = false }
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
                                            selectedMonsterId = monster.id
                                            monsterMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        when {
                            !overlayPermission -> {
                                HunterButton(
                                    text = "Grant overlay permission",
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        overlayPermissionLauncher.launch(intent)
                                    },
                                    accent = WeakRed,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            overlayRunning -> {
                                HunterButton(
                                    text = "Stop overlay",
                                    onClick = { viewModel.stopOverlay() },
                                    accent = WeakRed,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            else -> {
                                HunterButton(
                                    text = "Start overlay",
                                    onClick = {
                                        selectedMonster?.let { monster ->
                                            viewModel.startOverlay(monster.id)
                                        }
                                    },
                                    accent = AccentViolet,
                                    enabled = selectedMonster != null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Android 13+ may hide the foreground notification until notification permission is allowed. The overlay bubble still works.",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                            color = TextMuted
                        )
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(title = "About")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "MHO Companion Wiki · 1.0.0",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        KeyValueRow(label = "Content", value = "Bundled JSON modules + optional remote sync")
                        KeyValueRow(label = "Storage", value = "DataStore, local-only")
                        KeyValueRow(label = "Reset", value = "Daily 05:00 · weekly Monday 05:00")
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = "Fan-made companion. Not affiliated with Capcom, Niantic, or the Monster Hunter Outlanders team. Code rewards and drop data are community references; verify in-game.",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                            color = TextMuted
                        )
                    }
                }

                item {
                    GlassCard(accent = WeakRed) {
                        SectionHeader(title = "Danger zone")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Clear checklist progress, the farming wishlist, and every custom loadout on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        HunterButton(
                            text = "Reset local data",
                            onClick = { confirmReset = true },
                            accent = WeakRed,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset local data?",
            body = "This removes checklist progress, the farming wishlist, and all custom loadouts. Export a backup first if you want to keep them.",
            confirmLabel = "Reset",
            onConfirm = {
                viewModel.clearLocalData()
                confirmReset = false
            },
            onDismiss = { confirmReset = false }
        )
    }
}

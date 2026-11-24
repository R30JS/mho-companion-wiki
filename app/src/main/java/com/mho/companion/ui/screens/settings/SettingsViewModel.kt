package com.mho.companion.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mho.companion.MhoApp
import com.mho.companion.data.repository.SyncState
import com.mho.companion.domain.model.Content
import com.mho.companion.domain.model.CustomLoadout
import com.mho.companion.domain.model.SavedMixSet
import com.mho.companion.domain.model.WishlistItem
import com.mho.companion.service.OverlayService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val context = getApplication<Application>()
    private val container = (app as MhoApp).container

    val content: StateFlow<Content?> = container.contentRepository.content
    val syncState: StateFlow<SyncState> = container.contentRepository.syncState
    val wishlist: Flow<List<WishlistItem>> = container.userDataRepository.wishlist
    val customLoadouts: Flow<List<CustomLoadout>> = container.userDataRepository.customLoadouts
    val mixSets: Flow<List<SavedMixSet>> = container.userDataRepository.mixSets
    val checklist: Flow<Set<String>> = container.userDataRepository.checklist

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _overlayPermission = MutableStateFlow(Settings.canDrawOverlays(context))
    val overlayPermission: StateFlow<Boolean> = _overlayPermission.asStateFlow()

    private val _overlayRunning = MutableStateFlow(OverlayService.isRunning)
    val overlayRunning: StateFlow<Boolean> = _overlayRunning.asStateFlow()

    fun refreshOverlayState() {
        _overlayPermission.value = Settings.canDrawOverlays(context)
        _overlayRunning.value = OverlayService.isRunning
    }

    fun sync() {
        viewModelScope.launch {
            val result = container.contentRepository.sync()
            _message.value = result.message
        }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            container.backupManager.export(uri)
                .onSuccess { payload ->
                    _message.value = "Backup saved: ${payload.wishlist.size} wishlist items, ${payload.customLoadouts.size} loadouts, ${payload.mixSets.size} mix-sets."
                }
                .onFailure { error ->
                    _message.value = "Export failed: ${error.message ?: "unknown error"}"
                }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            container.backupManager.import(uri)
                .onSuccess { payload ->
                    _message.value = "Backup restored: ${payload.wishlist.size} wishlist items, ${payload.customLoadouts.size} loadouts, ${payload.mixSets.size} mix-sets."
                }
                .onFailure { error ->
                    _message.value = "Import failed: ${error.message ?: "invalid file"}"
                }
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            container.userDataRepository.clearAll()
            _message.value = "Checklist, wishlist, loadouts and mix-sets reset."
        }
    }

    fun startOverlay(monsterId: String) {
        if (!Settings.canDrawOverlays(context)) {
            _message.value = "Grant the overlay permission first."
            return
        }
        val intent = Intent(context, OverlayService::class.java)
            .putExtra(OverlayService.EXTRA_MONSTER_ID, monsterId)
        ContextCompat.startForegroundService(context, intent)
        viewModelScope.launch {
            delay(350)
            refreshOverlayState()
            _message.value = if (OverlayService.isRunning) "Overlay bubble is active." else "Overlay did not start."
        }
    }

    fun stopOverlay() {
        context.stopService(Intent(context, OverlayService::class.java))
        _overlayRunning.value = false
    }

    fun consumeMessage() {
        _message.value = null
    }
}

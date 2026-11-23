package com.mho.companion.ui.screens.hub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mho.companion.MhoApp
import com.mho.companion.data.repository.SyncState
import com.mho.companion.domain.model.Content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HubViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as MhoApp).container

    val content: StateFlow<Content?> = container.contentRepository.content
    val syncState: StateFlow<SyncState> = container.contentRepository.syncState
    val checklist: Flow<Set<String>> = container.userDataRepository.checklist

    fun toggleChecklist(id: String, checked: Boolean) {
        viewModelScope.launch {
            container.userDataRepository.setChecklistItem(id, checked)
        }
    }

    fun sync() {
        viewModelScope.launch {
            container.contentRepository.sync()
        }
    }
}

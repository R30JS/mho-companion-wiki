package com.mho.companion.ui.screens.builds

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mho.companion.MhoApp
import com.mho.companion.domain.model.Content
import com.mho.companion.domain.model.CustomLoadout
import com.mho.companion.domain.model.SavedMixSet
import com.mho.companion.domain.model.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BuildsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as MhoApp).container

    val content: StateFlow<Content?> = container.contentRepository.content
    val wishlist: Flow<List<WishlistItem>> = container.userDataRepository.wishlist
    val customLoadouts: Flow<List<CustomLoadout>> = container.userDataRepository.customLoadouts
    val mixSets: Flow<List<SavedMixSet>> = container.userDataRepository.mixSets

    fun addWishlistBatch(materials: List<Pair<String, Int>>, source: String) {
        viewModelScope.launch {
            container.userDataRepository.addWishlistBatch(materials, source)
        }
    }

    fun saveMixSet(mixSet: SavedMixSet) {
        viewModelScope.launch {
            container.userDataRepository.saveMixSet(mixSet)
        }
    }

    fun deleteMixSet(id: String) {
        viewModelScope.launch {
            container.userDataRepository.deleteMixSet(id)
        }
    }

    fun addWishlist(label: String, source: String, target: Int) {
        viewModelScope.launch {
            container.userDataRepository.addWishlist(label, source, target)
        }
    }

    fun saveLoadout(loadout: CustomLoadout) {
        viewModelScope.launch {
            container.userDataRepository.saveLoadout(loadout)
        }
    }

    fun deleteLoadout(id: String) {
        viewModelScope.launch {
            container.userDataRepository.deleteLoadout(id)
        }
    }
}

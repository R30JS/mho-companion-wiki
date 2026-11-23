package com.mho.companion.ui.screens.bestiary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mho.companion.MhoApp
import com.mho.companion.domain.model.Content
import com.mho.companion.domain.model.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BestiaryViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as MhoApp).container

    val content: StateFlow<Content?> = container.contentRepository.content
    val wishlist: Flow<List<WishlistItem>> = container.userDataRepository.wishlist

    fun addWishlist(label: String, source: String, target: Int) {
        viewModelScope.launch {
            container.userDataRepository.addWishlist(label, source, target)
        }
    }

    fun adjustHave(id: String, delta: Int) {
        viewModelScope.launch {
            container.userDataRepository.adjustWishlistHave(id, delta)
        }
    }

    fun setTarget(id: String, target: Int) {
        viewModelScope.launch {
            container.userDataRepository.setWishlistTarget(id, target)
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            container.userDataRepository.removeWishlist(id)
        }
    }
}

package com.mho.companion.ui.screens.guides

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mho.companion.MhoApp
import com.mho.companion.domain.model.Content
import kotlinx.coroutines.flow.StateFlow

class GuidesViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as MhoApp).container

    val content: StateFlow<Content?> = container.contentRepository.content
}

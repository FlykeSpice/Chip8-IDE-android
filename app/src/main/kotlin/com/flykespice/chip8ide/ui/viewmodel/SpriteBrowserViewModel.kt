package com.flykespice.chip8ide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flykespice.chip8ide.data.Chip8IdeManager
import com.flykespice.chip8ide.data.IdeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpriteBrowserViewModel(private val chip8IdeManager: Chip8IdeManager) : ViewModel() {
    private var _sprites = MutableStateFlow(emptyList<Pair<String, BooleanArray>>())
    val sprites get() = _sprites.asStateFlow()

    init {
        viewModelScope.launch {
            _sprites.value = chip8IdeManager.getSprites { reason, line ->
                chip8IdeManager.setIdeState(IdeState.error(reason, line))
            }
        }
    }
}
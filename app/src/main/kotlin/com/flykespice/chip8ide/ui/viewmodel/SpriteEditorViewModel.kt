package com.flykespice.chip8ide.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.flykespice.chip8ide.data.Chip8IdeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpriteEditorViewModel(private val chip8IdeManager: Chip8IdeManager) : ViewModel() {
    private var _editingSprite = MutableStateFlow(BooleanArray(0))
    val editingSprite get() = _editingSprite.asStateFlow()

    fun edit(x: Int, y: Int, value: Boolean) {
        val copy = _editingSprite.value.copyOf()
        copy[x*8+y] = value
        _editingSprite.value = copy
    }

    fun resizeHeight(h: Int) {
        val new = BooleanArray(8*h)
        _editingSprite.value.copyInto(new)
        _editingSprite.value = new
    }

    fun submit(label: String) {
        chip8IdeManager.updateSprite(label, _editingSprite.value)
    }
}
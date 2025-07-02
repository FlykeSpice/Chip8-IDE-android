package com.flykespice.chip8ide.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.flykespice.chip8ide.data.Chip8IdeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpriteEditorViewModel(private val chip8IdeManager: Chip8IdeManager) : ViewModel() {
    private var _editingSprite = MutableStateFlow(BooleanArray(0))
    val editingSprite get() = _editingSprite.asStateFlow()

    private var _label = MutableStateFlow("")
    val label get() = _label.asStateFlow()

    fun loadSprite(label: String, sprite: BooleanArray) {
        _label.value = label
        _editingSprite.value = sprite.copyOf()
    }

    fun edit(x: Int, y: Int, value: Boolean) {
        val copy = _editingSprite.value.copyOf()
        copy[(y*8)+x] = value
        _editingSprite.value = copy
    }

    fun changeLabel(label: String) {
        _label.value = label
    }

    fun resizeHeight(h: Int) {
        val new = BooleanArray(8*h)
        _editingSprite.value.copyInto(new)
        _editingSprite.value = new
    }

    fun submit() {
        chip8IdeManager.updateSprite(_label.value, _editingSprite.value)
    }
}
package com.flykespice.chip8ide.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.flykespice.chip8ide.data.Chip8IdeManager

class EditorViewModel(private val chip8IdeManager: Chip8IdeManager) : ViewModel() {
    val code get() = chip8IdeManager.code

    fun updateCode(code: String) {
        chip8IdeManager.updateCode(code)
    }
}
package com.flykespice.chip8ide.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.flykespice.chip8ide.data.Chip8IdeManager

class EmulatorViewModel(private val chip8IdeManager: Chip8IdeManager) : ViewModel() {
    val framebuffer get() = chip8IdeManager.frameBuffer
    val paused get() = chip8IdeManager.paused

    fun pause(flag: Boolean) {
        chip8IdeManager.pause(flag)
    }

    fun reset() {
        chip8IdeManager.chip8.reset()
    }

    fun setKey(key: Int, flag: Boolean) {
        chip8IdeManager.chip8.key[key] = flag
    }
}
package com.flykespice.chip8ide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flykespice.chip8ide.data.Chip8IdeManager
import kotlinx.coroutines.launch
import java.io.OutputStream

class ScaffoldViewModel(private val chip8IdeManager: Chip8IdeManager) : ViewModel() {
    val ideState get() = chip8IdeManager.ideState

    fun assemble() {
        viewModelScope.launch {
            chip8IdeManager.assemble()
        }
    }

    fun load(text: String) {
        viewModelScope.launch {
            chip8IdeManager.loadCode(text)
        }
    }

    fun importROM(data: ByteArray) {
        viewModelScope.launch {
            chip8IdeManager.importROM(data)
        }
    }

    fun save(outputStream: OutputStream) {
        chip8IdeManager.save(outputStream)
    }

    fun export(outputStream: OutputStream) {
        chip8IdeManager.export(outputStream)
    }
}
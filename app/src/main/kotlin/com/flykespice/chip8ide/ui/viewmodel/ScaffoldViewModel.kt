package com.flykespice.chip8ide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flykespice.chip8ide.data.Chip8IdeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream

class ScaffoldViewModel(private val chip8IdeManager: Chip8IdeManager) : ViewModel() {
    val ideState get() = chip8IdeManager.ideState

    //This counter is used to track changes when a new project(code) is loaded and cleared
    //it's needed by the EditorScreen to force its recomposition because of reasons
    private var _projectChanged = MutableStateFlow(0)
    val projectChanged get() = _projectChanged.asStateFlow()

    fun assemble() {
        viewModelScope.launch {
            chip8IdeManager.assemble()
        }
    }

    fun new() {
        viewModelScope.launch {
            chip8IdeManager.new()
            _projectChanged.value += 1
        }
    }

    fun load(text: String) {
        viewModelScope.launch {
            chip8IdeManager.loadCode(text)
            _projectChanged.value += 1
        }
    }

    fun importROM(data: IntArray) {
        viewModelScope.launch {
            chip8IdeManager.importROM(data)
            _projectChanged.value += 1
        }
    }

    fun save(outputStream: OutputStream) {
        chip8IdeManager.save(outputStream)
    }

    fun export(outputStream: OutputStream) {
        chip8IdeManager.export(outputStream)
    }
}
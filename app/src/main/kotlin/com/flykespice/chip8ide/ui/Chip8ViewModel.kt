package com.flykespice.chip8ide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flykespice.chip8ide.chip8.Chip8Assembler
import com.flykespice.chip8ide.data.Chip8IdeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class IdeState(val message: String) {
    class idle: IdeState(message = "")
    class assembling: IdeState("Assembling...")
    class success: IdeState("Successfully assembled!")
    data class error(val reason: String, val line: Int): IdeState("Error: $reason at $line")
}

class Chip8ViewModel(
    private val chip8IdeManager: Chip8IdeManager,
    private val onChooseFile: (Array<String>) -> Unit,
    private val onSaveFile: (String) -> Unit,
    private val onExportFile: (String) -> Unit
) : ViewModel() {
    private var _ideState = MutableStateFlow<IdeState>(IdeState.idle())
    val ideState get() = _ideState.asStateFlow()

    private var _sprites = MutableStateFlow(emptyList<Pair<String, BooleanArray>>())
    val sprites get() = _sprites.asStateFlow()

    val paused get() = chip8IdeManager.paused
    val frameBuffer get() = chip8IdeManager.frameBuffer
    val code get() = chip8IdeManager.code

    private val chip8 = chip8IdeManager.chip8
    fun pause(flag: Boolean) = chip8.pause(flag)
    fun stop() = chip8.stop()
    fun reset() = chip8.reset()

    fun setKey(key: Int, flag: Boolean) {
        chip8IdeManager.chip8.key[key] = flag
    }

    fun updateCode(code: String) {
        viewModelScope.launch {
            //TODO: Update repository with new code and state
            chip8IdeManager.update(code)
        }
    }

    fun assemble() {
        viewModelScope.launch {
            _ideState.value = IdeState.assembling()

            try {
                val result = chip8IdeManager.assemble()
                _ideState.value = IdeState.success()
            } catch (parsingError: Chip8Assembler.ParsingError) {
                _ideState.value = IdeState.error(parsingError.message, parsingError.line)
            }
        }
    }

    fun setIdeState(ideState: IdeState) {
        _ideState.value = ideState
    }

    fun chooseFile() {
        viewModelScope.launch {
            stop()
            onChooseFile(arrayOf("application/*", "text/plain"))
        }
    }

    fun save(filename: String) {
        viewModelScope.launch {
            stop()
            onSaveFile(filename)
        }
    }

    fun export(filename: String) {
        viewModelScope.launch {
            stop()
            try {
                assemble()
                onExportFile(filename)
            } catch (parsingError: Chip8Assembler.ParsingError) {
                _ideState.value = IdeState.error(parsingError.message, parsingError.line)
            }
        }
    }

    fun new() {
        viewModelScope.launch {
            stop()
            chip8IdeManager.update("")
        }
    }

    fun updateSprite(label: String, sprite: BooleanArray) {
        viewModelScope.launch {
            if (code.value.lines().any { it.startsWith("$label:") })
                chip8IdeManager.updateSprite(label, sprite)
            else
                chip8IdeManager.createNewSprite(label, sprite)
        }
    }

    fun getSprites() {
        viewModelScope.launch {
            _sprites.value = chip8IdeManager.getSprites(
                onError = { reason, line -> _ideState.value = IdeState.error(reason, line) }
            )
        }
    }
}
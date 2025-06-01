package com.flykespice.chip8ide.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flykespice.chip8ide.chip8.Assembler
import com.flykespice.chip8ide.data.Chip8IdeManager
import kotlinx.coroutines.launch

sealed class IdeState {
    class idle: IdeState()
    class assembling: IdeState()
    class success: IdeState()
    data class error(val message: String, val line: Int): IdeState()
}

class Chip8ViewModel(
    private val chip8IdeManager: Chip8IdeManager,
    private val onChooseFile: (Array<String>) -> Unit,
    private val onSaveFile: (String) -> Unit,
    private val onExportFile: (String) -> Unit
) : ViewModel() {
    var ideState = mutableStateOf<IdeState>(IdeState.idle())
    var filename: String = ""

    val paused get() = chip8IdeManager.paused
    val frameBuffer get() = chip8IdeManager.frameBuffer
    val code get() = chip8IdeManager.code

    fun run() = chip8IdeManager.run()
    fun pause(flag: Boolean) = chip8IdeManager.pause(flag)
    fun stop() = chip8IdeManager.stop()
    fun reset() = chip8IdeManager.reset()

    fun setKey(key: Int, flag: Boolean) = chip8IdeManager.setKey(key, flag)

    fun updateCode(code: String) {
        viewModelScope.launch {
            //TODO: Update repository with new code and state
            chip8IdeManager.update(code)
        }
    }

    fun assemble() {
        viewModelScope.launch {
            ideState.value = IdeState.assembling()

            try {
                val result = chip8IdeManager.assemble()
                ideState.value = IdeState.success()
            } catch (parsingError: Assembler.ParsingError) {
                ideState.value = IdeState.error(parsingError.message, parsingError.line)
            }
        }
    }

    fun showError(message: String, line: Int) {
        ideState.value = IdeState.error(message, line)
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
            } catch (parsingError: Assembler.ParsingError) {
                ideState.value = IdeState.error(parsingError.message, parsingError.line)
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
            if (code.value.lines().any {it.startsWith("$label:")})
                chip8IdeManager.updateSprite(label, sprite)
            else
                chip8IdeManager.createNewSprite(label, sprite)
        }
    }
}
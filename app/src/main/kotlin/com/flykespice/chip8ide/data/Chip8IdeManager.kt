package com.flykespice.chip8ide.data

import com.flykespice.chip8ide.chip8.Assembler
import com.flykespice.chip8ide.chip8.Chip8
import com.flykespice.chip8ide.chip8.Disassembler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList

//Manager for Chip8's Ide
class Chip8IdeManager(onSoundStateChange: (Boolean) -> Unit) {
    private val chip8 = Chip8(
        onScreenUpdate = { _frameBuffer.value = it },
        onSoundStateChange = onSoundStateChange
    )

    private lateinit var  rom: IntArray

    private val _code = MutableStateFlow("")
    val code = _code.asStateFlow()

    private val _paused = MutableStateFlow(chip8.paused)
    val paused = _paused.asStateFlow()

    private val _realMode = MutableStateFlow(false)
    val realMode = _realMode.asStateFlow()

    private val _clockRate = MutableStateFlow(chip8.clockRate)
    val clockRate = _clockRate.asStateFlow()

    private val _frameBuffer = MutableStateFlow(BooleanArray(64*32))
    val frameBuffer = _frameBuffer.asStateFlow()

    fun load(text: String) {
        stop()
        pause(true)
        update(text)
        reset()
    }

    fun load(binary: IntArray) {
        stop()
        pause(true)
        chip8.load(binary)
        _code.value = Disassembler.disassemble(binary)
        reset()
    }

    fun update(code: String) {
        _code.value = code
    }

    /**
     * Assemble the code
     * @throws Assembler.ParsingError if code contains incorrect syntax
     */
    fun assemble(): IntArray {
        rom = Assembler.assemble(_code.value)
        chip8.load(rom)

        return rom
    }

    fun run() = chip8.run()

    fun pause(flag: Boolean) {
        chip8.pause(flag)
        _paused.value = flag
    }

    fun stop() = chip8.stop()

    fun reset() = chip8.run()

    private var keyState = BooleanArray(16)
    init {
        chip8.pollKeys = {keyState}
    }

    fun setKey(key: Int, flag: Boolean) {
        keyState[key] = flag
    }

    fun setClockRate(clockRate: Int) {
        chip8.clockRate = clockRate
        _clockRate.value = clockRate
    }

    fun setRealMode(flag: Boolean) {
        _realMode.value = flag
        pause(true)
        chip8.setOriginalMode(flag)
    }

    //Save as assembly file
    fun save(stream: OutputStream) {
        (stream as FileOutputStream).run {
            channel.truncate(0)
            write(code.value.toByteArray())
        }
    }

    fun export(stream: OutputStream) {
        (stream as FileOutputStream).run {
            channel.truncate(0)

            val out = rom.map { it.toByte() }.toByteArray()
            write(out)
        }
    }

    fun updateSprite(label: String, spriteData: BooleanArray) {
        val lines = code.value.lines()
        val lineLabel = lines.indexOfFirst { it.startsWith("$label:") }

        require(lineLabel != -1) {
            "label $label must be present in the code"
        }

        require(lineLabel > 0) {
            "Line must be preceded by a .sprite directive"
        }

        if (!lines[lineLabel-1].startsWith(".sprite"))
            throw IllegalStateException("Label doesn't precede a .sprite directive")

        val newLines = ArrayList(lines.slice(0..lineLabel))

        var newLiteral = 0
        for (i in spriteData.indices) {
            val pos = i % 8

            if (i != 0 && pos == 0) {
                newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))
                newLiteral = 0
            }

            if (spriteData[i])
                newLiteral = newLiteral or (1 shl (7-pos))
        }
        newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))

        //Now merge the remaining lines
        var remainingHeight = lines[lineLabel-1].split(" ")[1].toInt()
        for (l in lineLabel+1 until lines.size) {
            val line = lines[l]
            newLines.add(line)

            if (!line.startsWith("db"))
                continue

            val literals = line
                .substringAfter("db")
                .substringBefore(';')
                .split(',')

            if (literals.size > remainingHeight) {
                newLines.removeLast()
                newLines.add("db "+literals.slice(remainingHeight until literals.size).joinToString(","))

                remainingHeight = 0
            } else {
                newLines.removeLast()
                remainingHeight -= literals.size
            }

            if (remainingHeight <= 0) {
                if (l <= lines.size-1)
                    newLines.addAll(lines.slice(l+1 until lines.size))
                break
            }
        }

        if (remainingHeight > 0)
            throw IllegalStateException("TODO")

        _code.value = newLines.joinToString("\n")
    }

    fun createNewSprite(label: String, sprite: BooleanArray) {
        val lines = _code.value.lines()
        val newLines = ArrayList(lines)

        newLines.add("")
        newLines.add(".sprite ${sprite.size / 8}")
        newLines.add("$label:")

        var newLiteral = 0
        for (i in sprite.indices) {
            val pos = i % 8

            if (i != 0 && pos == 0) {
                newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))
                newLiteral = 0
            }

            if (sprite[i])
                newLiteral = newLiteral or (1 shl (7-pos))
        }
        newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))

        _code.value = newLines.joinToString("\n")
    }
}
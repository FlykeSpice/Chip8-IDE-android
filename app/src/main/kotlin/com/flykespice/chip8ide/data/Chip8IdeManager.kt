package com.flykespice.chip8ide.data

import com.flykespice.chip8ide.chip8.Chip8
import com.flykespice.chip8ide.chip8.Chip8Assembler
import com.flykespice.chip8ide.chip8.Chip8Disassembler
import com.flykespice.chip8ide.chip8.decodeLiteral
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.OutputStream

sealed class IdeState(val message: String) {
    class idle: IdeState(message = "")
    class assembling: IdeState("Assembling...")
    class success: IdeState("Successfully assembled!")
    data class error(val reason: String, val line: Int): IdeState("Error: $reason at $line")
}

/**
 * Class that manages all the IDE operations regarding the working project, it houses all the necessary states to be consumed by the respective viewmodels
 */
class Chip8IdeManager {

    val chip8 = Chip8(
        onScreenUpdate = { _frameBuffer.value = it.copyOf() }
    )

    private lateinit var rom: IntArray

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

    private val _ideState = MutableStateFlow<IdeState>(IdeState.idle())
    val ideState get() = _ideState.asStateFlow()

    fun setIdeState(ideState: IdeState) {
        _ideState.value = ideState
    }

    fun new() {
        chip8.stop()
        pause(true)
        chip8.reset()
        _code.value = ""
    }

    fun loadCode(text: String) {
        chip8.stop()
        pause(true)
        updateCode(text)
        chip8.reset()
    }

    fun importROM(binary: IntArray) {
        chip8.stop()
        pause(true)
        chip8.load(binary)
        _code.value = Chip8Disassembler.disassemble(binary)
        chip8.reset()
    }

    fun updateCode(code: String) {
        _code.value = code
    }

    /**
     * Assemble the code
     * @throws Chip8Assembler.ParsingError if code contains incorrect syntax
     */
    fun assemble() {
        chip8.stop()
        pause(true)
        try {
            _ideState.value = IdeState.assembling()
            rom = Chip8Assembler.assemble(_code.value)
            chip8.load(rom)
            _ideState.value = IdeState.success()
        } catch (e: Chip8Assembler.ParsingError) {
            _ideState.value = IdeState.error(e.message, e.line)
        }
    }

    fun pause(flag: Boolean) {
        chip8.pause(flag)
        _paused.value = flag
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
        stream.use {
            it.write(code.value.toByteArray())
        }
    }

    fun export(stream: OutputStream) {
        stream.use {
            it.write(rom.map { it.toByte() }.toByteArray())
        }
    }

    fun updateSprite(label: String, sprite: BooleanArray) {
        val lines = code.value.lines()
        val lineLabel = lines.indexOfFirst { it.startsWith("$label:") }

        if (lineLabel == -1) {
            createNewSprite(label, sprite)
            return
        }

        if (lineLabel == 0 || !lines[lineLabel-1].startsWith(".sprite"))
            throw IllegalStateException("Label $label doesn't precede a .sprite directive")

        val newLines = ArrayList(lines.slice(0..lineLabel))

        var newLiteral = 0
        for (i in sprite.indices) {
            if (i != 0 && (i % 8) == 0) {
                newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))
                newLiteral = 0
            }

            newLiteral = (newLiteral shl 1) or (if (sprite[i]) 1 else 0)
        }
        newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))

        //Now merge the remaining lines
        var remainingBytes = lines[lineLabel-1].split(" ")[1].toInt()
        for (l in lineLabel+1 until lines.size) {
            val line = lines[l]

            if (!line.startsWith("db")) {
                newLines.add(line)
                continue
            }

            val literals = line
                .substringAfter("db")
                .substringBefore(';')
                .split(',')

            if (literals.size > remainingBytes) {
                newLines.add("db "+literals.slice(remainingBytes until literals.size).joinToString(","))
                remainingBytes = 0
            } else {
                remainingBytes -= literals.size
            }

            if (remainingBytes == 0) {
                if (l != lines.lastIndex)
                    newLines.addAll(lines.slice(l+1 until lines.size))
                break
            }
        }

        if (remainingBytes > 0)
            TODO("Deal with the exceptional case when we're at the end of code and there is less line than needed for .sprite directive")

        //Adjust the .sprite directive to new height
        newLines[lineLabel-1] = ".sprite ${sprite.size/8}"

        _code.value = newLines.joinToString("\n")
    }

    private fun createNewSprite(label: String, sprite: BooleanArray) {
        val lines = _code.value.lines()
        val newLines = ArrayList(lines)

        newLines.add("")
        newLines.add(".sprite ${sprite.size / 8}")
        newLines.add("$label:")

        var newLiteral = 0
        for (i in sprite.indices) {
            if (i != 0 && (i % 8) == 0) {
                newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))
                newLiteral = 0
            }

            newLiteral = (newLiteral shl 1) or (if (sprite[i]) 1 else 0)
        }
        newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))

        _code.value = newLines.joinToString("\n")
    }

    fun getSprites(): List<Pair<String, BooleanArray>> {

        fun error(reason: String, line: Int) = setIdeState(IdeState.error(reason, line))

        val pair = _code.value.lines()
            .mapIndexed { index, line -> index+1 to line.substringBefore(';').trim() }
            .filter { it.second.isNotBlank() }
        
        val lines   = pair.map { it.second }
        val indexes = pair.map { it.first }

        val sprites = ArrayList<Pair<String, BooleanArray>>()

        var l = 0
        while (l < pair.size) {
            val line = lines[l]

            if (!line.startsWith(".sprite")) {
                l++
                continue
            }

            val numRows = try {
                line.split(" ")[1].toInt()
            } catch (_: IndexOutOfBoundsException) {
                error(".sprite must be accompanied by a parameter specifying number of rows", indexes[l])
                return sprites
            } catch (e: NumberFormatException) {
                error(".sprite: ${e.message}", indexes[l])
                return sprites
            }

            if (numRows < 1 || numRows > 15) {
                error(".sprite number of rows must be a number in the range 1..15", indexes[l])
                return sprites
            }

            if (++l >= lines.size) {
                error(".sprite used at the end of a line", indexes[l])
                return sprites
            }

            if (!lines[l].matches(Regex("${Chip8Assembler.identifierRegex}:\\s*"))) {
                error(".sprite must be immediately followed by a valid label", indexes[l])
                return sprites
            }

            val label = lines[l].substringBefore(':')
            val rows = ArrayList<BooleanArray>(numRows)
            while (++l < lines.size && lines[l].startsWith("db") && rows.size < numRows) {
                val literals = try {
                    lines[l]
                        .removePrefix("db")
                        .replace(" ", "")
                        .split(',')
                        .filter { it.isNotBlank() }
                        .map { it.decodeLiteral()!! }
                } catch (_: NullPointerException) {
                    error(".sprite: db contain invalid literal(s)", indexes[l])
                    return sprites
                }

                literals.forEach { literal ->
                    val row = BooleanArray(8)

                    for(bit in 0..7) {
                        row[bit] = (literal and (1 shl bit)) != 0
                    }

                    rows.add(row)
                }
            }

            if (rows.size < numRows) {
                error("Insufficient sprite data rows, it was specified $numRows but only found ${rows.size}", indexes[l-1])
                return sprites
            } else if (rows.size > numRows) {
                rows.dropLast(rows.size-numRows)
            }

            sprites.add(Pair(label, BooleanArray(rows.size*8) { rows[it/8][it%8] } ))
        }

        return sprites
    }

}
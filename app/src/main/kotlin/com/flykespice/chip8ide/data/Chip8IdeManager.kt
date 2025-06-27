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
        onScreenUpdate = { _frameBuffer.value = it }
    )

    private lateinit var rom: ByteArray

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

    fun loadCode(text: String) {
        chip8.stop()
        pause(true)
        updateCode(text)
        chip8.reset()
    }

    fun importROM(binary: ByteArray) {
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
            it.write(rom)
        }
    }

    fun updateSprite(label: String, sprite: BooleanArray) {
        val lines = code.value.lines()
        val lineLabel = lines.indexOfFirst { it.startsWith("$label:") }

        require(lineLabel != -1) {
            "Label $label must be present in the code"
        }

        if (lineLabel == 0 || !lines[lineLabel-1].startsWith(".sprite"))
            throw IllegalStateException("Label $label doesn't precede a .sprite directive")

        val newLines = ArrayList(lines.slice(0..lineLabel))

        var newLiteral = 0
        for (i in sprite.indices) {

            if (i != 0 && (i % 7) == 0) {
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
            if (i != 0 && (i % 8) == 0) {
                newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))
                newLiteral = 0
            }

            newLiteral = (newLiteral shl 1) or (if (sprite[i]) 1 else 0)
        }
        newLines.add("db 0b"+newLiteral.toString(2).padStart(8,'0'))

        _code.value = newLines.joinToString("\n")
    }

    fun getSprites(onError: (String, Int) -> Unit): List<Pair<String, BooleanArray>> {

        fun Int.adjustForBlanks(): Int {
            var result = 0
            var index = 0

            val lines = _code.value.lines().map { it.substringBefore(';').trim() }

            if (this >= lines.size) {
                return lines.size
            }

            while (index != this) {
                if (lines[index].isNotBlank())
                    index++

                result++
            }
            return result
        }

        val code = _code.value.lines().map { it.substringBefore(';').trim() }.filter { it.isNotBlank() }

        val sprites = ArrayList<Pair<String, BooleanArray>>()

        var i = 0
        outer@ while (i < code.size) {
            val line = code[i]

            if (!line.startsWith(".sprite")) {
                i++
                continue
            }

            val numRows: Int = try {
                line.split(" ")[1].toInt()
            } catch (_: IndexOutOfBoundsException) {
                onError(".sprite must be accompanied by a parameter specifying number of rows", i.adjustForBlanks())
                break
            } catch (e: NumberFormatException) {
                onError(".sprite: ${e.message}", i.adjustForBlanks())
                break
            }

            if (++i >= code.size) {
                onError(".sprite is used at the end of a line", i.adjustForBlanks())
                break
            }

            if (!code[i].matches(Regex("${Chip8Assembler.identifierRegex}:"))) {
                onError(".sprite must be followed by a label", i.adjustForBlanks())
                break
            }

            val label = code[i].substringBefore(':')
            val rows = ArrayList<BooleanArray>(numRows)
            while (++i < code.size && code[i].startsWith("db") && rows.size < numRows) {

                val literals = try {
                    code[i]
                        .removePrefix("db")
                        .replace(" ", "")
                        .split(',')
                        .filter { it.isNotBlank() }
                        .map { it.decodeLiteral()!! }
                } catch (_: NullPointerException) {
                    onError(".sprite: db must contain a literal", i.adjustForBlanks())
                    break@outer
                }

                for (literal in literals) {
                    val row = BooleanArray(8)

                    for(bit in 0..7) {
                        row[bit] = (literal and (1 shl bit)) != 0
                    }

                    rows.add(row)
                }
            }

            if (rows.size < numRows) {
                onError("Insufficient sprite data rows, it was specified $numRows but only found ${rows.size}", i.adjustForBlanks())
                break
            }

            sprites.add(Pair(label, BooleanArray(rows.size*8) { rows[it / 8][it%8] } ))
        }

        return sprites
    }

}
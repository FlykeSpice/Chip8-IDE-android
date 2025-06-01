package com.flykespice.chip8ide.chip8

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Chip8(
    val onScreenUpdate: (BooleanArray) -> Unit,
    val onSoundStateChange: (Boolean) -> Unit
) {

    private object CpuRegisters {
        var v = IntArray(16)
        var I: Int = 0

        val stack = ArrayList<Int>(16)
        var pc: Int = 0

        var dt: Int = 0
        var st: Int = 0

        fun reset() {
            v.fill(0)
            I = 0
            pc = 0x200

            dt = 0
            st = 0

            stack.clear()
        }
    }

    private var key = BooleanArray(16)

    private var loaded: Boolean = false
    var paused: Boolean = true
        private set

    private val ram = IntArray(0x1000)
    private var rom = IntArray(0)


    private var originalMode = false

    companion object {
        const val FPS = 1.0/60

        private val digits = intArrayOf(
            0xF0,0x90,0x90,0x90,0xF0, //0
            0x20,0x60,0x20,0x20,0x70, //1
            0xF0,0x10,0xF0,0x80,0xF0, //2
            0xF0,0x10,0xF0,0x10,0xF0, //3
            0x90,0x90,0xF0,0x10,0x10, //4
            0xF0,0x80,0xF0,0x10,0xF0, //5
            0xF0,0x80,0xF0,0x90,0xF0, //6
            0xF0,0x10,0x20,0x40,0x40, //7
            0xF0,0x90,0xF0,0x90,0xF0, //8
            0xF0,0x90,0xF0,0x10,0xF0, //9
            0xF0,0x90,0xF0,0x90,0x90, //A
            0xE0,0x90,0xE0,0x90,0xE0, //B
            0xF0,0x80,0x80,0x80,0xF0, //C
            0xE0,0x90,0x90,0x90,0xE0, //D
            0xF0,0x80,0xF0,0x80,0xF0, //E
            0xF0,0x80,0xF0,0x80,0x80  //F
        )

        //https://www.laurencescotford.net/2020/07/25/chip-8-on-the-cosmac-vip-instruction-index/
        private val cosmacInstructionTime: Map<String, Duration> = mapOf(
            "0nnn" to 45.4f,
            "00E0" to 45.4f,
            "00EE" to 45.4f,
            "1nnn" to 54.48f,
            "2nnn" to 118.04f,

            "3xkk" to 45.4f,
            "4xkk" to 45.4f,
            "5xy0" to 63.56f,
            "6xkk" to 27.24f,
            "7xkk" to 45.4f,

            "8xy0" to 54.48f,

            "8xy1" to 199.76f,
            "8xy2" to 199.76f,
            "8xy3" to 199.76f,
            "8xy4" to 199.76f,
            "8xy5" to 199.76f,
            "8xy6" to 199.76f,
            "8xy7" to 199.76f,
            "8xyE" to 199.76f,

            "9xy0" to 63.56f,

            "Annn" to 54.48f,
            "Bnnn" to 108.96f,
            "Cxkk" to 163.44f,

            "Dxyn" to 9039.141f, //Average

            "Ex9E" to 63.56f,
            "ExA1" to 63.56f,

            "Fx07" to 45.4f,
            "Fx0A" to 0.0f,//85515.44f, //Best case
            "Fx15" to 45.4f,
            "Fx18" to 45.4f,
            "Fx1E" to 54.48f, //Best case

            "Fx29" to 72.64f,

            "Fx33" to 799.04f, //Worst case

            "Fx55" to 675.3251f, //Average
            "Fx65" to 675.3251f, //Average
        ).mapValues { with(Duration) { it.value.toDouble().microseconds } }

        val mnemonics = mapOf(
            "00E0" to "CLS",
            "00EE" to "RET",
            "0nnn" to "SYS addr",
            "1nnn" to "JP addr",
            "2nnn" to "CALL addr",
            "3xkk" to "SE Vx, byte",
            "4xkk" to "SNE Vx, byte",
            "5xy0" to "SE Vx, Vy",
            "6xkk" to "LD Vx, byte",
            "7xkk" to "ADD Vx, byte",
            "8xy0" to "LD Vx, Vy",
            "8xy1" to "OR Vx, Vy",
            "8xy2" to "AND Vx, Vy",
            "8xy3" to "XOR Vx, Vy",
            "8xy4" to "ADD Vx, Vy",
            "8xy5" to "SUB Vx, Vy",
            "8xy6" to "SHR Vx",
            "8xy7" to "SUBN Vx, Vy",
            "8xyE" to "SHL Vx",
            "9xy0" to "SNE Vx, Vy",
            "Annn" to "LD I, addr",
            "Bnnn" to "JP V0, addr",
            "Cxkk" to "RND Vx, byte",
            "Dxyn" to "DRW Vx, Vy, nibble",
            "Ex9E" to "SKP Vx",
            "ExA1" to "SKNP Vx",
            "Fx07" to "LD Vx, DT",
            "Fx0A" to "LD Vx, K",
            "Fx15" to "LD DT, Vx",
            "Fx18" to "LD ST, Vx",
            "Fx1E" to "ADD I, Vx",
            "Fx29" to "LD F, Vx",
            "Fx33" to "LD B, Vx",
            "Fx55" to "LD [I], Vx",
            "Fx65" to "LD Vx, [I]",
        )
    }

    //Encode chip-8 instruction mnemonics to regex
    private fun opcodeToPattern(opcode : String) =
        opcode
            .replace("x","\\p{XDigit}")
            .replace("nnn", "\\p{XDigit}{3}")
            .replace("kk", "\\p{XDigit}{2}")
            .replace("y","\\p{XDigit}")
            .replace("n", "\\p{XDigit}")

    private val display = BooleanArray(64*32)

    var clockRate = 500
        set(value) {
            require(value > 0) {
                "Value but be positive and nonzero for clock rate"
            }

            field = value
        }

    fun reset() {
        CpuRegisters.reset()

        ram.fill(0)
        display.fill(false)

        for(i in digits.indices)
            ram[i] = digits[i]

        for(i in rom.indices)
            ram[0x200+i] = rom[i]

        key.fill(false)

        keyPressed = -1

        microseconds = 0.0f
        lastPc = 0x200
    }

    fun load(_rom : IntArray) {
        if (mainJob.isActive) {
            //throw IllegalStateException("Attempt to load rom while thread is running. You should stop() first before loading")
            mainJob.cancel()
        }

        rom = _rom.copyOf()
        loaded = true
        reset()
    }

    /*
     * Set original emulation mode
     */
    fun setOriginalMode(flag: Boolean) {
        originalMode = flag
        reset()
    }

    private var microseconds: Float = 0.0f
    private var lastPc = 0x200

    //Runs at 60hz to draw a new frame
    fun updateFrame() {
        if (CpuRegisters.dt > 0)
            CpuRegisters.dt--

        key = pollKeys()
        keyPressed = key.indexOfFirst { it }

        //Cosmac-VIP emulation mode?
        if (!originalMode) {
            val period = 1.0 / clockRate
            val insPerFrame = (FPS / period).toInt()

            repeat(insPerFrame) {
                decode(ram[CpuRegisters.pc].shl(8) + ram[CpuRegisters.pc + 1])
                CpuRegisters.pc += 2
            }
        } else {
            microseconds += 16666.666f
            microseconds = microseconds.coerceAtMost(16666.666f)

            while (microseconds > 0) {
                val timeSpent = decode(ram[CpuRegisters.pc].shl(8) + ram[CpuRegisters.pc + 1]).inWholeMicroseconds

                if (timeSpent == 0L && keyPressed == -1) {
                    microseconds = 0.0f
                    CpuRegisters.pc += 2
                    return
                }

                microseconds -= timeSpent
                lastPc = CpuRegisters.pc
                CpuRegisters.pc += 2
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val singleThreadContext = newSingleThreadContext("Chip-8 thread")
    private suspend fun launchMainJob() = coroutineScope {
        launch {
            while (isActive) {
                if (CpuRegisters.dt > 0)
                    CpuRegisters.dt--

                if (CpuRegisters.st > 0)
                    CpuRegisters.st--
                else
                    onSoundStateChange(false)

                onScreenUpdate(display.copyOf())
                delay(16)
            }
        }

        launch {
            //var elapsed: Duration = 0.seconds
            var duration: Duration
            val clockDuration = (1.toDouble() / clockRate.toDouble()).seconds
            while (isActive) {
                key = pollKeys()
                keyPressed = key.indexOfFirst { it }

                duration = decode(ram[CpuRegisters.pc].shl(8) + ram[CpuRegisters.pc + 1])
                CpuRegisters.pc += 2

                delay(if (originalMode) duration else clockDuration)
            }
        }
    }

    //@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    //private val scope = CoroutineScope(Dispatchers.Main)
    fun run() {
        //try { scope.cancel() } catch (_: IllegalStateException) {}
        paused = true
        reset()
        //launchJob()
    }

    private var mainJob: Job  = Job()
    @OptIn(DelicateCoroutinesApi::class)
    fun pause(flag: Boolean) {
        paused = flag
        if (paused && mainJob.isActive) {
            mainJob.cancel()
        } else if (!paused && !mainJob.isActive) {
            mainJob = GlobalScope.launch(singleThreadContext) { launchMainJob() }
        }
    }

    fun stop() {
        if (mainJob.isActive)
            mainJob.cancel()
    }

    var pollKeys: () -> BooleanArray = { BooleanArray(16) }

    private var keyPressed = -1

    private val patterns = mnemonics.keys.map { it to opcodeToPattern(it) }

    private fun decode(opcode : Int): Duration {

        val pattern = patterns.firstOrNull { (_, pattern) ->
            Pattern.matches(
                pattern,
                opcode.toString(16).uppercase().padStart(4,'0'))
        }?.first

        val nnn = opcode and 0xFFF
        val x   = (opcode shr 2*4) and 0xF
        val y   = (opcode shr 1*4) and 0xF
        val kk  = opcode and 0xFF
        val n   = opcode and 0xF

        val v     = CpuRegisters.v
        val stack = CpuRegisters.stack

        var I  = CpuRegisters.I
        var pc = CpuRegisters.pc
        var dt = CpuRegisters.dt
        var st = CpuRegisters.st
        var vf = CpuRegisters.v[0xF] != 0

        when(pattern) {
            "00E0" -> { display.fill(false)}
            "00EE" -> { pc = try { stack.removeAt(stack.size-1) } catch (_: IndexOutOfBoundsException) { 0 }}
            "1nnn" -> { pc = nnn-2}
            "2nnn" -> { stack.add(pc); pc = nnn-2}
            "3xkk" -> { if(v[x] == kk) pc += 2}
            "4xkk" -> { if(v[x] != kk) pc += 2}
            "5xy0" -> { if(v[x] == v[y]) pc += 2}
            "6xkk" -> { v[x] = kk}
            "7xkk" -> { v[x] = (v[x] + kk) and 0xFF}
            "8xy0" -> { v[x] = v[y]}
            "8xy1" -> { v[x] = v[x] or v[y]}
            "8xy2" -> { v[x] = v[x] and v[y]}
            "8xy3" -> { v[x] = v[x] xor v[y]}
            "8xy4" -> { vf = (v[x] + v[y]) > 0xFF; v[x] = (v[x] + v[y]) and 0xFF}
            "8xy5" -> { vf = v[x] > v[y]; v[x] -= v[y]; if(v[x] < 0) v[x] = 0xFF - abs(v[x])-1}
            "8xy6" -> { vf = v[x].takeLowestOneBit() != 0; v[x] = (v[x] shr 1) and 0xFF}
            "8xy7" -> { vf = v[y] > v[x]; v[x] = v[y]-v[x]; if(v[x] < 0) v[x] = 0xFF - abs(v[x])-1}
            "8xyE" -> { vf = (v[x] and 0x80) != 0; v[x] = (v[x] shl 1) and 0xFF}
            "9xy0" -> { if(v[x] != v[y]) pc += 2}
            "Annn" -> { I = nnn}
            "Bnnn" -> { pc = ((nnn + v[0]) and 0xFFF) - 2}
            "Cxkk" -> { v[x] = Random.nextInt(0..0xFF) and kk}

            "Dxyn" -> {
                var erased = false
                val X = v[x] % 64; var Y = v[y] % 32

                for(i in 0 until n) {
                    val row = ram[I + i]

                    if(Y >= 32)
                        break

                    for (j in 0..7) {
                        if(X+j >= 64)
                            break

                        val position = X+j + (Y*64)
                        val oldPixel = display[position]

                        display[position] = oldPixel xor ((row and (1 shl 7-j)) != 0)

                        if(!erased) erased = oldPixel && !display[position] //Collision?
                    }
                    Y++
                }
                vf = erased
            }

            "Ex9E" -> { if(key[v[x]]) pc += 2}
            "ExA1" -> { if(!key[v[x]]) pc += 2}

            "Fx07" -> { v[x] = dt}
            "Fx0A" -> { if(keyPressed != -1) v[x] = keyPressed else pc -= 2}
            "Fx15" -> { dt = v[x]}
            "Fx18" -> {
                st = v[x]
                onSoundStateChange(true)
            }
            "Fx1E" -> { I = (I + v[x]) and 0xFFF}

            //Digit sprites located right at the start of memory(ram)
            "Fx29" -> { I = (v[x] * 5) and 0xFFF}

            "Fx33" -> { val bcd = v[x].toString().padStart(3,'0'); for(i in bcd.indices) ram[I + i] = bcd[i].digitToInt()}
            "Fx55" -> { for(i in 0..x) ram[I + i] = v[i]}
            "Fx65" -> { for(i in 0..x) v[i] = ram[I + i]}
        }
        
        CpuRegisters.apply {
            v[0xf] = if (vf) 1 else 0
            
            this.I = I
            this.pc = pc
            this.dt = dt
            this.st = st
        }

        return cosmacInstructionTime[pattern] ?: with(Duration) { 40.microseconds }
    }
}
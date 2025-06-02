package com.flykespice.chip8ide.chip8

import com.flykespice.chip8ide.chip8.Chip8.Companion.opcodeToPattern
import java.util.regex.Pattern

object Disassembler {
    //Encode chip-8 instruction mnemonics to regex
    private data class Instruction(val addr: Int, val opcode: Int, var mnemonic: String)

    private data class DisassemblyInfo(
        val instructions: ArrayList<Instruction> = ArrayList(),
        var drawData: ArrayList<Pair<Int, Int>> = ArrayList(),
        var data: ArrayList<Int> = ArrayList(),
        var calls: ArrayList<Int> = ArrayList(),
        var jumps: ArrayList<Int> = ArrayList()
    )

    private fun recursiveDisassemble(
        rom: IntArray,
        startingAddress: Int,
        info: DisassemblyInfo
    ) {
        // TODO:
        //  Go through all the instruction flow, inclusive all the possible branches, until reached the point where the code self-loop.
        //  If reached a branch that can't be computed at compile time, let it be unknown

        var pc = startingAddress
        val (instructions, drawData, data, calls, jumps) = info

        loop@ while (true) {
            if (((pc-0x200)+1) > rom.lastIndex) //Out of bounds
                return

            if (instructions.any { it.addr == pc })
                return

            val opcode = (rom[pc-0x200] shl 8) + rom[(pc-0x200)+1]

            lateinit var pair: Map.Entry<String, String>
            try {
                pair = Chip8.mnemonics.firstNotNullOf {
                    if (Pattern.matches(
                            it.key.opcodeToPattern(),
                            opcode.toString(16).uppercase().padStart(4, '0')
                        )
                    ) it else null
                }
            } catch (_: NoSuchElementException) {
                //Chip-8 ignores invalid opcodes so do we, next
                pc += 2
                continue@loop
            }

            val nnn = opcode and 0xFFF
            val x = (opcode shr 2*4) and 0xF
            val y = (opcode shr 1*4) and 0xF
            val kk = opcode and 0xFF
            val n = opcode and 0xF

            var mnemonic = pair.value

            val oldPC = pc
            var underConditional = false

            when(pair.key) {
                "1nnn" -> { //JP nnn
                    //TODO: Check whether it self-loops (jump an address already disassembled), if so stop disassembling
                    instructions.lastOrNull()?.run {
                        val conditionals = listOf("SE", "SNE", "SKP", "SKNP")
                        if (conditionals.any { this.mnemonic.startsWith(it) }) {
                            underConditional = true
                            mnemonic = "\t" + mnemonic
                        }
                    }

                    if (nnn !in jumps)
                        jumps.add(nnn)

                    mnemonic = mnemonic.replace("addr", "J${jumps.indexOf(nnn)}")

                    if (!underConditional)
                        pc = nnn-2
                }

                "2nnn" -> { //CALL nnn
                    if(nnn !in calls) calls.add(nnn)
                    mnemonic = mnemonic.replace("addr", "call_${calls.indexOf(nnn)}")

                    if (instructions.all { it.addr != nnn }) {
                        recursiveDisassemble(rom, nnn, info)
                    }
                }

                "00EE" -> { //RET
                    mnemonic += "\n"
                }

                /*"Annn", "Bnnn" -> {
                    if(nnn-0x200 in rom.indices) {
                        if(nnn !in data) data.add(nnn)

                        mnemonic = mnemonic.replace("addr", "data${data.indexOf(nnn)}")
                    }
                }*/
            }

            instructions.add(
                Instruction(
                    addr = oldPC,
                    opcode = opcode,
                    mnemonic = mnemonic.replace("byte", '#' + kk.toString(16))
                        .replace("addr", '#' + nnn.toString(16))
                        .replace("nibble", '#' + n.toString(16))
                        .replace("x", x.toString(16))
                        .replace("y", y.toString(16))
                )
            )

            if (pair.key == "00EE")
                return

            if(underConditional) {
                //Now disassemble another branch
                recursiveDisassemble(rom, nnn, info)
            }

            pc += 2
        }
    }

    fun disassemble(rom: IntArray): String {
        val info = DisassemblyInfo()
        recursiveDisassemble(rom, 0x200, info)

        //TODO: Post process: Annotate as data directive those regions in the rom not disassembled
        val (instructions, drawData, data, calls, jumps) = info

        info.instructions.sortBy { it.addr }

        var str = ""

        var dataN = 0
        for(i in instructions.indices) {
            val ins = instructions[i]

            if(ins.addr in calls)
                str += "\ncall_${calls.indexOf(ins.addr)}:\n"

            if(ins.addr in jumps)
                str += "\nJ${jumps.indexOf(ins.addr)}:\n"

            str += ins.mnemonic.padEnd(25) + ";${ins.addr.toString(16)} ${ins.opcode.toString(16)}" + "\n"

            val afterAddr = ins.addr+2 - 0x200
            val untilAddr = try { instructions[i+1].addr - 0x200 } catch(_: Throwable) { rom.size }
            var n = 1
            if(afterAddr < untilAddr)
            {
                str += "data${dataN++}:\ndb"
                for(j in afterAddr until untilAddr) {
                    if(n++ % 0xf == 0) str += "\ndb"

                    str += " #${rom[j].toString(16).padStart(2,'0')},"
                }
                str += "\n"
            }
        }

        return str
    }
}
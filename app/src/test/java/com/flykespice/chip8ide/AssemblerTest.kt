package com.flykespice.chip8ide

import com.flykespice.chip8ide.chip8.Assembler
import junit.framework.TestCase.assertEquals

import org.junit.Test

val code =
    """
        vermins: equ 10
        ld v0, vertical
        
        ld v0, K
        ld dt, v0
        vertical: ld v1, dt
        sne v1, #0
        jp vertical
    """.trimIndent()

val assembledCode = intArrayOf(
    0xf0,0x0a, //ld v0, k
    0xf0,0x15, //ld dt, v0
    0xf1,0x07, //ld v1, dt
    0x41,0x00, //sne v1, #0
    0x12,0x04, //jp 0x004
)

class AssemblerTest {

    @Test
    fun assemble() {
        val result = Assembler.assemble(code)

        result.onFailure {
            println("ERROR:" + it.message)
        }
        assert(result.isSuccess)

        val assembled = result.getOrNull()!!

        for (i in assembled.indices step 2) {
            println("0x${((assembled[i] shl 8) + assembled[i+1]).toString(16)}")
        }

        assert(assembled.contentEquals(assembledCode))
    }
}
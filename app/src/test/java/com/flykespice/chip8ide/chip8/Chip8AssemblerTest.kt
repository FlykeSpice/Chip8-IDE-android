package com.flykespice.chip8ide.chip8

import junit.framework.TestCase.assertTrue

import org.junit.Test

private val code =
    """
        ld v0, K
        ld dt, v0
        vertical: ld v1, dt
        sne v1, #0
        jp vertical
    """.trimIndent()

private val expectedAssembledCode = intArrayOf(
    0xf0,0x0a, //ld v0, k
    0xf0,0x15, //ld dt, v0
    0xf1,0x07, //ld v1, dt
    0x41,0x00, //sne v1, #0
    0x12,0x04, //jp 0x004
).map { it.toByte() }.toByteArray()

class Chip8AssemblerTest {

    @Test
    fun assemble_returnsCorrectAssembledCode() {
        val result = Chip8Assembler.assemble(code)
        assertTrue(expectedAssembledCode.contentEquals(result))
    }
}
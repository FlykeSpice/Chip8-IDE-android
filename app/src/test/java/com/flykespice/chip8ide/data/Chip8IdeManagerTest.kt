package com.flykespice.chip8ide.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val test =
    """
        .sprite 4
        foo:
        db 0b00111100
        db 0b01111110
        db 0b01111110
        db 0b00111100
        
        .sprite 4
        foo:
        db 0b00111100
        db 0b01111110
        db 0b01111110
        db 0b00111100
    """.trimIndent()

private val expected = booleanArrayOf(
    false, false, true, true, true, true, false, false,
    false, true,  true, true, true, true, true,  false,
    false, true,  true, true, true, true, true,  false,
    false, false, true, true, true, true, false, false
)


private val test1 =
    """
        .sprite 4
        bar:
        db #3c, #7e, #7e, #3c
    """.trimIndent()

class Chip8IdeManagerTest {
    private val chip8IdeManager = Chip8IdeManager()
    @Test
    fun getSprites() {
        chip8IdeManager.updateCode(test)
        val result = chip8IdeManager.getSprites { _,_ -> }

        assertEquals(2, result.size)
        assertEquals( 4, result[0].second.size / 8)
        assertTrue(result[0].second.contentEquals(expected))
    }
}
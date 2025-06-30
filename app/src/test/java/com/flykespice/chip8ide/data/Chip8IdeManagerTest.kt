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
        val result = chip8IdeManager.getSprites()

        assertEquals(2, result.size)
        assertEquals( 4, result[0].second.size / 8)
        assertTrue(result[0].second.contentEquals(expected))
    }

    @Test
    fun getSprites_errors_onInsufficientSpriteData() {
        chip8IdeManager.setIdeState(IdeState.idle())
        chip8IdeManager.updateCode(
            """
                .sprite 15
                foo:
                db #ff, #ff
            """.trimIndent()
        )

        val test = chip8IdeManager.getSprites()
        assertTrue(test.isEmpty())
        assertTrue(chip8IdeManager.ideState.value is IdeState.error)
    }
}
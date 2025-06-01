package com.flykespice.chip8ide.ui


import junit.framework.TestCase.assertEquals
import org.junit.Test

import com.flykespice.chip8ide.ui.getSprites
import org.junit.Assert.assertArrayEquals

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


private val test1 =
    """
        .sprite 4
        bar:
        db #3c, #7e, #7e, #3c
    """.trimIndent()
val expected = arrayOf(
    booleanArrayOf(false, false, true, true, true, true, false, false),
    booleanArrayOf(false, true,  true, true, true, true, true,  false),
    booleanArrayOf(false, true,  true, true, true, true, true,  false),
    booleanArrayOf(false, false, true, true, true, true, false, false)
)

internal class SpriteEditorKtTest {
    @Test
    fun getSpritesTest() {
        val result = getSprites(test, onError = { msg, line -> println("$msg at $line") } )

        assertEquals(2, result.size)
    }

    @Test
    fun getSpritesTest1() {
    }
}
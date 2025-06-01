package com.flykespice.chip8ide

import com.flykespice.chip8ide.ui.expected
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun regexTest() {
        val foo = "aaaabbbbcccc"

        val result = Regex("a*(?<named>(b*))c*").matchEntire(foo)

        if(result != null) {
            println(result.groups["named"])
        }

    }
}
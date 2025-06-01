package com.flykespice.chip8ide

import kotlinx.coroutines.*
import org.junit.Test

import org.junit.Assert.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class DumbTest {
    class Foo(val _update: Foo.() -> Unit) {
        companion object {
            var n = 0
        }
        inline var N
            get() = n
            set(v) {n = v}

        fun update() = _update()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun test() {
        val singleContext = newSingleThreadContext("single")
        var foo = IntArray(100) {it}

        var duration = measureTime {
            runBlocking {
                foo.forEach { println(it) }
            }
        }

        println("Sequential forEach = ${duration.inWholeMilliseconds}")

        duration = measureTime {
            runBlocking {
                foo.forEach { launch(singleContext) { println(it) } }
            }
        }

        println("Concurrent forEach = ${duration.inWholeMilliseconds}")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun test2() {
        var time = measureTime {
            repeat(10) {
                if(arrayOf("a","b","c").any {it in "abc"})
                    println("true")
                else
                    println("false")
            }
        }
        println("Time on literal array = ${time.inWholeMilliseconds}")

        time = measureTime {
            repeat(10) {
                val array = arrayOf("a","b","c")
                if(array.any {it in "abc"})
                    println("true")
                else
                    println("false")
            }
        }
        println("Time on variable array = ${time.inWholeMilliseconds}")

    }
}
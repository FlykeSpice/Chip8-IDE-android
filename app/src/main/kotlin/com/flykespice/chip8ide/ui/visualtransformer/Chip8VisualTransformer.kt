package com.flykespice.chip8ide.ui.visualtransformer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.flykespice.chip8ide.chip8.Assembler

private fun String.indexOfOrNull(char: Char, startIndex: Int): Int? {
    val index = this.indexOf(char, startIndex)

    return if (index != -1) index else null
}

private fun String.nextNonWhiteSpace(index: Int): Int {
    var _index = index

    try {
        while (this[_index].isWhitespace())
            _index++
    } catch (_: StringIndexOutOfBoundsException) { return this.length }

    return _index
}

private val table = Assembler.table.map { Regex(it.second.pattern + "(\\s|$)", RegexOption.IGNORE_CASE) }

data class Chip8SyntaxStyle(
    val comments: Color,
    val literals: Color,
    val instructions: Color,
    val directives: Color,
    val labels: Color
) {
    companion object {
        val Light = Chip8SyntaxStyle(
            comments = Color.Green,
            literals = Color.Yellow,
            instructions = Color.Blue,
            directives = Color.Cyan,
            labels = Color.Cyan
        )
    }
}

fun String.toChip8SyntaxAnnotatedString(chip8SyntaxStyle: Chip8SyntaxStyle = Chip8SyntaxStyle.Light): AnnotatedString {
    val styles = ArrayList<AnnotatedString.Range<SpanStyle>>()

    val labels = this.lines()
        .filter { it.substringBefore(';').substringBefore(':').isNotEmpty() }
        .map { it.substringBefore(';').substringBefore(':') }

    var index = -1
    while (++index < this.length) {
        index = this.nextNonWhiteSpace(index)
        if (index == this.length)
            break

        val nextNewline = this.indexOfOrNull('\n', index) ?: this.length

        //Skip labels
        val labelIndex = this.indexOf(':', index)
        if (labelIndex != -1 && labelIndex < nextNewline) {
            index = labelIndex
            continue
        }

        when {
            //Comments
            this[index] == ';' -> {
                styles.add(AnnotatedString.Range(SpanStyle(color = chip8SyntaxStyle.comments), index, nextNewline))
                index = nextNewline
                continue
            }

            this.startsWith("db", index) -> {
                index = nextNewline
                continue
            }
        }

        val instructionMatch = table.firstNotNullOfOrNull { pattern ->
            pattern.matchAt(this.subSequence(index, nextNewline), 0)
        }

        instructionMatch?.let { match ->

            styles.add(
                AnnotatedString.Range(
                    SpanStyle(color = chip8SyntaxStyle.instructions),
                    index,
                    index + match.value.length
                )
            )

            for (operand in listOf("nnn", "kk", "n")) {
                try {
                    match.groups[operand]?.let { group ->
                        styles.add(
                            AnnotatedString.Range(
                                SpanStyle(color = if (group.value in labels) chip8SyntaxStyle.labels else chip8SyntaxStyle.literals),
                                index + group.range.first,
                                index + group.range.last + 1
                            )
                        )
                    }
                } catch (_: IllegalArgumentException) { /*Ignore*/ }
            }

            index += match.value.length
        }

            /*                //Literals
                val literal = Assembler.literal.matchAt(text.subSequence(index, newlineIndex), 0)

                if (literal != null) {
                    styles.add(
                        AnnotatedString.Range(
                            SpanStyle(color = Color.Yellow),
                            text.text.indexOf(literal.value, index),
                            index + literal.value.length
                        )
                    )

                    index += literal.value.length
                    break
                }*/

    }

    return AnnotatedString(this, spanStyles = styles)
}
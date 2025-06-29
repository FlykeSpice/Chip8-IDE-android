package com.flykespice.chip8ide.chip8


/*private*/ fun String.decodeLiteral(): Int? {
    return try {
        if (this.startsWith('#')) {
            this.removePrefix("#").toInt(16)
        } else if (this.startsWith("0b")) {
            this.removePrefix("0b").toInt(2)
        } else {
            this.toInt()
        }
    } catch (_: NumberFormatException) { null }
}

private fun String.evalExpression(labels: Map<String, Int>): Int? {
    val tokens = this.removePrefix("(").removeSuffix(")").split("+","-","*").toMutableList()

    fun getNextTokenOrNull(): Int? {
        val token = tokens.removeFirstOrNull() ?: return null

        return if (Chip8Assembler.identifierRegex.matches(token)) {
            labels[token]
        } else {
            token.decodeLiteral()
        }
    }

    var result = getNextTokenOrNull() ?: return null

    val operators = this.filter { it in "+-*" }
    for (operator in operators) {
        val operand = getNextTokenOrNull() ?: return null

        when (operator) {
            '+' -> result += operand
            '-' -> result -= operand
            '*' -> result *= operand
        }
    }

    return result
}


object Chip8Assembler {
    //Operands regex
    val identifierRegex = Regex("\\.?[a-z_][a-z_0-9]*")
    private val literalRegex = Regex("(#\\p{XDigit}{1,3})|(0b[01]+)|(\\d+)")

    //keywords that can't be used as identifiers
    private val reservedKeywords = listOf(
        "equ",
        "v0", "v1", "v2", "v3", "v4", "v5", "v6","v7", "v8", "v9", "va", "vb", "vc", "vd", "ve", "vf",
        "i",
        "k",
        "dt",
        "db",
        "sprite"
    )

    //FIXME: Java/Kotlin regex doesn't supports recursive match, needs to manually check recursion
    private val expressionRegex = Regex("\\(\\s*($literalRegex|$identifierRegex)\\s*([+\\-*]\\s*($literalRegex|$identifierRegex)\\s*)+\\)")

    private fun operandToPattern(name: String, pattern: String) = "(?<$name>($pattern))"

    private fun mnemonicToPattern(mnemonic: String) =
        mnemonic
            .replace("byte", operandToPattern("kk","$literalRegex|((?!((v\\p{XDigit})|dt|k|i)(\\s|;|$))$identifierRegex)|$expressionRegex"))
            .replace("addr", operandToPattern("nnn","$literalRegex|((?!((v\\p{XDigit})|dt|k|i)(\\s|;|$))$identifierRegex)|$expressionRegex"))
            .replace("vx", "v" + operandToPattern("x","\\p{XDigit}{1}"))
            .replace("vy", "v" + operandToPattern("y","\\p{XDigit}{1}"))
            .replace("nibble", operandToPattern("n", "$literalRegex|$expressionRegex"))
            .replace("[i]", "\\[i\\]")
            .replace(" ", "\\s+")

    /*private*/ val table = Chip8.mnemonics.mapValues { (_, value) -> Regex(mnemonicToPattern(value.lowercase())) }.toList()

    data class ParsingError(override val message: String, val line: Int): Throwable(message)

    /**
     * Attempts to assemble the input text, returning the assembled binary as [IntArray]
     * @throws [Chip8Assembler.ParsingError] if any error occurred (invalid input)
     */
    fun assemble(code: String): IntArray {
        //First process labels
        val labels = HashMap<String, Int>()

        val pendingLabels = ArrayList<String>()

        val rom = ArrayList<Int>()

        //Preprocess labels
        var byteCount = 0x200
        var lastLabel = ""

        code.lines().forEachIndexed { index, _line ->
            var line = _line.lowercase().substringBefore(';').trimStart().trimEnd()
            val index = index+1

            if (line.isBlank())
                return@forEachIndexed

            var label = line.substringBefore(':').takeIf { it != line }

            if (label != null) {
                if (label in reservedKeywords)
                    throw ParsingError("\"$label\" is a reserved keyword, can't be used as label", index)
                else if (!identifierRegex.matches(label))
                    throw ParsingError("label $label isn't a valid identifier", index)

                if (label.startsWith('.')) {
                    if (lastLabel == "")
                        throw ParsingError("local label $label must be defined after a global label", index)

                    // Come up with a unique identifier for this local label by appending it with the parent label name
                    label += lastLabel
                } else if (label in labels.keys) {
                    throw ParsingError("label $label has already been defined before", index)
                } else {
                    lastLabel = label
                }

                pendingLabels.add(label)
            }

            line = line.substringAfter(':').trimStart()

            table.find { it.second.matches(line) }?.let {
                pendingLabels.forEach { labels[it] = byteCount }
                pendingLabels.clear()

                byteCount += 2
            }

            when {
                line.startsWith("db") -> {
                    pendingLabels.forEach { labels[it] = byteCount }
                    pendingLabels.clear()

                    val size = line.removePrefix("db").split(',').filter { it.isNotBlank() }.size
                    byteCount += size
                }

                line.startsWith("equ") -> {
                    if (label == line) {
                        throw ParsingError("Missing label for equ directive", index)
                    }

                    val value = line.split(Regex("\\s+")).getOrNull(1)
                        ?: throw ParsingError("No value specified for equ operand", index)

                    lastLabel = pendingLabels.removeLastOrNull() ?: throw ParsingError("equ must have a label defined", index)

                    labels[lastLabel] = value.decodeLiteral() ?: throw ParsingError("equ's $value must be a valid literal", index)

                    lastLabel = pendingLabels.lastOrNull { !it.startsWith('.') } ?: ""
                }
            }
        }

        if (pendingLabels.isNotEmpty())
            throw ParsingError("labels touch end of code", code.lines().lastIndex+1)

        code.lines().forEachIndexed { index, line ->
            lastLabel = line.substringBefore(':')
                .takeIf { it != line && !it.startsWith('.')} ?: lastLabel

            val line = line.lowercase().substringBefore(';').trimStart().trimEnd()
                .substringAfter(':').trimStart() //Ignore the labels
            val index = index+1

            if (line.isBlank() || line.startsWith(".sprite"))
                return@forEachIndexed

            var match: MatchResult? = null
            var opcode = ""
            for ((op, pattern) in table) {
                match = pattern.matchEntire(line)
                if(match != null) {
                    opcode = op
                    break
                }
            }

            if (match != null) {
                val ops = listOf("nnn", "x", "y", "kk", "n")

                ops.forEach { operand ->
                    try {
                        match.groups.get(operand)?.let {
                            var pattern = it.value

                            if (operand != "x" && operand != "y") {
                                if (identifierRegex.matches(pattern)) {
                                    //Check if local label
                                    if (pattern.startsWith('.'))
                                        pattern += lastLabel

                                    if (pattern !in labels.keys)
                                        throw ParsingError("label $pattern is used but undefined", index)

                                    pattern = labels[pattern]!!.toString(16).uppercase()
                                } else if (expressionRegex.matches(pattern)) {
                                    pattern = pattern.evalExpression(labels)?.toString(16)?.uppercase()
                                        ?: throw ParsingError("expression $pattern is malformed", index)

                                    if (pattern.toInt(16) < 0) {
                                        throw ParsingError("expression that results into a negative number aren't allowed", index)
                                    }
                                }
                                else if (pattern.decodeLiteral() != null) {
                                    pattern = pattern.decodeLiteral()!!.toString(16).uppercase()
                                } else {
                                    throw ParsingError("malformed literal $pattern",index)
                                }

                                pattern = pattern.padStart(3, '0')
                                pattern = when (operand) {
                                    "nnn" -> pattern.takeLast(3)
                                    "kk" -> pattern.takeLast(2)
                                    "n" -> pattern.takeLast(1)
                                    else -> ""
                                }
                            }


                            opcode = opcode.replace(operand, pattern)
                        }
                    } catch(_: IllegalArgumentException) {} //Ignore
                }

                val opcode = opcode
                    .replace("y", "0") //y is sometimes undocumented/unused, so we default them to zero
                    .toInt(16)

                rom.add((opcode shr 8) and 0xff)
                rom.add(opcode and 0xff)
            }
            else if (line.startsWith("db")) {
                val values = line.removePrefix("db").replace(Regex(" +"), "").split(',').filter {it.isNotBlank()}

                values.forEach {
                    val decoded = it.decodeLiteral()
                        ?: throw ParsingError("malformed operand $it", index+1)

                    rom.add(decoded and 0xff)
                }
            }
            else if (!line.startsWith("equ")) {
                throw ParsingError("Unrecognized instruction/directive", index)
            }
        }

        return rom.toIntArray()
    }
}
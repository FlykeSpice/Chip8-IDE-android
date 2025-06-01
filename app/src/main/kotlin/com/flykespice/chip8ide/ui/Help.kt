package com.flykespice.chip8ide.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flykespice.chip8ide.ui.theme.Chip8IDETheme
import com.flykespice.chip8ide.ui.visualtransformer.toChip8SyntaxAnnotatedString

private object HelpStyle {
    inline val body @Composable get() = MaterialTheme.typography.bodyMedium
    inline val title @Composable get() = MaterialTheme.typography.headlineMedium
    inline val subtitle @Composable get() = MaterialTheme.typography.titleLarge
}

private data class InstructionInfo(val syntax: String, val desc: String, val example: String? = null)

private val instructionsInfo = listOf(
    InstructionInfo(
        syntax = "0nnn - SYS addr",
        desc =
    """
    Jump to a machine code routine at nnn.
    
    This instruction is only used on the old computers on which Chip-8 was originally implemented. It is ignored by modern interpreters.
    """),

    InstructionInfo(
        syntax = "00E0 - CLS",
        desc =
    """
    Clear the display.
    """),

    InstructionInfo(
        syntax = "00EE - RET",
        desc =
    """
    Return from a subroutine.
    
    The interpreter sets the program counter to the address at the top of the stack, then subtracts 1 from the stack pointer.
    """),

    InstructionInfo(
        syntax = "1nnn - JP addr",
        desc =
    """
    Jump to location nnn.
    
    The interpreter sets the program counter to nnn.
    """),

    InstructionInfo(
        syntax = "2nnn - CALL addr",
        desc =
    """
    Call subroutine at nnn.
    
    The interpreter increments the stack pointer, then puts the current PC on the top of the stack. The PC is then set to nnn.
    """),

    InstructionInfo(
        syntax = "3xkk - SE Vx, byte",
        desc =
    """
    Skip next instruction if Vx = kk.
    
    The interpreter compares register Vx to kk, and if they are equal, increments the program counter by 2.
    """),

    InstructionInfo(
        syntax = "4xkk - SNE Vx, byte",
        desc =
    """
    Skip next instruction if Vx != kk.
    
    The interpreter compares register Vx to kk, and if they are not equal, increments the program counter by 2.
    """),

    InstructionInfo(
        syntax = "5xy0 - SE Vx, Vy",
        desc =
    """
    Skip next instruction if Vx = Vy.
    
    The interpreter compares register Vx to register Vy, and if they are equal, increments the program counter by 2.
    """),

    InstructionInfo(
        syntax = "6xkk - LD Vx, byte",
        desc =
    """
    Set Vx = kk.
    
    The interpreter puts the value kk into register Vx.
    """),

    InstructionInfo(
        syntax = "7xkk - ADD Vx, byte",
        desc =
    """
    Set Vx = Vx + kk.
    
    Adds the value kk to the value of register Vx, then stores the result in Vx.
    """),

    InstructionInfo(
        syntax = "8xy0 - LD Vx, Vy",
        desc =
    """
    Set Vx = Vy.
    
    Stores the value of register Vy in register Vx.
    """),

    InstructionInfo(
        syntax = "8xy1 - OR Vx, Vy",
        desc =
    """
    Set Vx = Vx OR Vy.
    
    Performs a bitwise OR on the values of Vx and Vy, then stores the result in Vx. A bitwise OR compares the corrseponding bits from two values, and if either bit is 1, then the same bit in the result is also 1. Otherwise, it is 0.
    """),

    InstructionInfo(
        syntax = "8xy2 - AND Vx, Vy",
        desc =
    """
    Set Vx = Vx AND Vy.
    
    Performs a bitwise AND on the values of Vx and Vy, then stores the result in Vx. A bitwise AND compares the corrseponding bits from two values, and if both bits are 1, then the same bit in the result is also 1. Otherwise, it is 0.
    """),

    InstructionInfo(
        syntax = "8xy3 - XOR Vx, Vy",
        desc =
    """
    Set Vx = Vx XOR Vy.
    
    Performs a bitwise exclusive OR on the values of Vx and Vy, then stores the result in Vx. An exclusive OR compares the corrseponding bits from two values, and if the bits are not both the same, then the corresponding bit in the result is set to 1. Otherwise, it is 0.
    """),

    InstructionInfo(
        syntax = "8xy4 - ADD Vx, Vy",
        desc =
    """
    Set Vx = Vx + Vy, set VF = carry.
    
    The values of Vx and Vy are added together. If the result is greater than 8 bits (i.e., > 255,) VF is set to 1, otherwise 0. Only the lowest 8 bits of the result are kept, and stored in Vx.
    """),

    InstructionInfo(
        syntax = "8xy5 - SUB Vx, Vy",
        desc =
    """
    Set Vx = Vx - Vy, set VF = NOT borrow.
    
    If Vx > Vy, then VF is set to 1, otherwise 0. Then Vy is subtracted from Vx, and the results stored in Vx.
    """),

    InstructionInfo(
        syntax = "8xy6 - SHR Vx {, Vy}",
        desc =
    """
    Set Vx = Vx SHR 1.
    
    If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is divided by 2.
    """),

    InstructionInfo(
        syntax = "8xy7 - SUBN Vx, Vy",
        desc =
    """
    Set Vx = Vy - Vx, set VF = NOT borrow.
    
    If Vy > Vx, then VF is set to 1, otherwise 0. Then Vx is subtracted from Vy, and the results stored in Vx.
    """),

    InstructionInfo(
        syntax = "8xyE - SHL Vx {, Vy}",
        desc =
    """
    Set Vx = Vx SHL 1.
    
    If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is multiplied by 2.
    """),

    InstructionInfo(
        syntax = "9xy0 - SNE Vx, Vy",
        desc =
    """
    Skip next instruction if Vx != Vy.
    
    The values of Vx and Vy are compared, and if they are not equal, the program counter is increased by 2.
    """),

    InstructionInfo(
        syntax = "Annn - LD I, addr",
        desc =
    """
    Set I = nnn.
    
    The value of register I is set to nnn.
    """),

    InstructionInfo(
        syntax = "Bnnn - JP V0, addr",
        desc =
    """
    Jump to location nnn + V0.
    
    The program counter is set to nnn plus the value of V0.
    """),

    InstructionInfo(
        syntax = "Cxkk - RND Vx, byte",
        desc =
    """
    Set Vx = random byte AND kk.
    
    The interpreter generates a random number from 0 to 255, which is then ANDed with the value kk. The results are stored in Vx. See instruction 8xy2 for more information on AND.
    """),

    InstructionInfo(
        syntax = "Dxyn - DRW Vx, Vy, nibble",
        desc =
    """
    Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision.
    
    The interpreter reads n bytes from memory, starting at the address stored in I. These bytes are then displayed as sprites on screen at coordinates (Vx, Vy). Sprites are XORed onto the existing screen. If this causes any pixels to be erased, VF is set to 1, otherwise it is set to 0. If the sprite is positioned so part of it is outside the coordinates of the display, it wraps around to the opposite side of the screen. See instruction 8xy3 for more information on XOR, and section 2.4, Display, for more information on the Chip-8 screen and sprites.
    """),

    InstructionInfo(
        syntax = "Ex9E - SKP Vx",
        desc =
    """
    Skip next instruction if key with the value of Vx is pressed.
    
    Checks the keyboard, and if the key corresponding to the value of Vx is currently in the down position, PC is increased by 2.
    """),

    InstructionInfo(
        syntax = "ExA1 - SKNP Vx",
        desc =
    """
    Skip next instruction if key with the value of Vx is not pressed.
    
    Checks the keyboard, and if the key corresponding to the value of Vx is currently in the up position, PC is increased by 2.
    """),

    InstructionInfo(
        syntax = "Fx07 - LD Vx, DT",
        desc =
    """
    Set Vx = delay timer value.
    
    The value of DT is placed into Vx.
    """),

    InstructionInfo(
        syntax = "Fx0A - LD Vx, K",
        desc =
    """
    Wait for a key press, store the value of the key in Vx.
    
    All execution stops until a key is pressed, then the value of that key is stored in Vx.
    """),

    InstructionInfo(
        syntax = "Fx15 - LD DT, Vx",
        desc =
    """
    Set delay timer = Vx.
    
    DT is set equal to the value of Vx.
    """),

    InstructionInfo(
        syntax = "Fx18 - LD ST, Vx",
        desc =
    """
    Set sound timer = Vx.
    
    ST is set equal to the value of Vx.
    """),

    InstructionInfo(
        syntax = "Fx1E - ADD I, Vx",
        desc =
    """
    Set I = I + Vx.
    
    The values of I and Vx are added, and the results are stored in I.
    """),

    InstructionInfo(
        syntax = "Fx29 - LD F, Vx",
        desc =
    """
    Set I = location of sprite for digit Vx.
    
    The value of I is set to the location for the hexadecimal sprite corresponding to the value of Vx. See section 2.4, Display, for more information on the Chip-8 hexadecimal font.
    """),

    InstructionInfo(
        syntax = "Fx33 - LD B, Vx",
        desc =
    """
    Store BCD representation of Vx in memory locations I, I+1, and I+2.
    
    The interpreter takes the decimal value of Vx, and places the hundreds digit in memory at location in I, the tens digit at location I+1, and the ones digit at location I+2.
    """),

    InstructionInfo(
        syntax = "Fx55 - LD [I], Vx",
        desc =
    """
    Store registers V0 through Vx in memory starting at location I.
    
    The interpreter copies the values of registers V0 through Vx into memory, starting at the address in I.
    """),

    InstructionInfo(
        syntax = "Fx65 - LD Vx, [I]",
        desc =
    """
    Read registers V0 through Vx from memory starting at location I.
    
    The interpreter reads values from memory starting at location I into registers V0 through Vx.
    """),
)

private val directives = listOf(
    InstructionInfo(
        syntax = ";<comment>",
        desc =
        """
    Declares that anything after the semicolon(;) is a comment and should be ignored by the assembler.
    """.trimIndent()
    ),

    InstructionInfo(
        syntax = ".db <literal>[, <literal>...]",
        desc =
    """
    Declares one or a row of byte literals as data.
   
    You can prefix the directive with a label so you can reference it on a instruction.
    """.trimIndent(),
        example =
    """
    data1: .db 0xFF ;One byte of literal as data
    
    data2: .db 0xFF, 0x10, 0x11 ;three byte of literals as data
    
    data3: .db 0b11, 2, 0x1f ; you can also mix other literals representations
    """.trimIndent()
    ),

    InstructionInfo(
        syntax = "<label> .equ <literal>",
        desc =
    """
    Declares a macro that is replaced with the underlying literal during assembly phase,
    such macros can then be used on places where only literals are allowed.
    
    You *must* prefix it with a label to identify the macro otherwise assembly will fail.
    """.trimIndent(),
        example =
    """
    damage: .equ 30 ;damage amount by the player
    
    .equ 0xFF ;error wont assemble, must be preceded by a label
    """.trimIndent()
    ),

    InstructionInfo(
        syntax = ".sprite <height>",

        desc =
    """
    MUST BE FOLLOWED BY A LABEL
    
    Designates that the data following the label is a sprite of height <height> pixels tall.
    
    Sprites in Chip-8 are of fixed width 8 and their height can go up to 15, so each byte of the sprite data is a row of pixels.
    
    When a label is marked by this directive, its sprite content will be shown on the Sprite Editor so that you can view or edit it there.
    """.trimIndent(),

        example =
    """
    ;Sprite data for a 8x5 ball each byte (row) is represented by the binary literal
    .sprite 5 ;5 = the sprite is 5 pixels tall (5 rows of byte)
    ball: ;A label must follow .sprite directive or it's an error
    ;Each bit from a byte represents a pixel
    .db 0b00011000
    .db 0b00111100
    .db 0b01111110
    ,db 0b00111100
    .db 0b00011000
    """.trimIndent()
    )
)

@Composable
private fun HelpIntroduction() {
    val scrollState = rememberScrollState()

    Column {
        Text("Introduction", style = HelpStyle.title)

        Text("\nThe editor is where you will code your chip-8 programs\n", style = HelpStyle.body)
        Text("A program is composed of multiple lines, each of which specifies either a label, a statement, comment or a combination of either.", style = HelpStyle.body)

        SyntaxBlock(
            "label: statement ;comment"
        )

        Text("Each field is optional, you can leave a line blank or only having a comment", style = HelpStyle.body)

        SyntaxBlock(
            """
                data: .db #22, #ff ;line containing a label and directive
                
                data2: ;line containing only a label
                .db #22 ;line containing only a directive
                
                ;this line contains only this comment
                loop: ld va, 33 ;line containing a label, instruction and comment
                jp loop ;line containing only an instruction
            """.trimIndent()
        )

        Text("Each field will further be explained in the next sections", style = HelpStyle.body)
    }
}

@Composable
private fun HelpLiteral() {
    val scrollState = rememberScrollState()

    Column (Modifier.verticalScroll(scrollState)) {
        Text("Literals", style = MaterialTheme.typography.headlineMedium)

        Text("\nA literal is a constant value that is used on instructions that takes a address/byte/nibble as one of their operand and directives as well.\n", style = HelpStyle.body)
        Text("A literal can be specified in the following ways:\n", style = HelpStyle.body)

        Text("binary", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))
        Text("A binary literal is represented by the prefix 0b followed by a row of one and zeroes.\n", style = HelpStyle.body)

        Text("Its main application is representing sprites in the code as a column of bitfield for being more readable.\n", style = HelpStyle.body)

        SyntaxBlock(
            """
                ;Sprite data for a 8x5 ball 
                .db 0b00011000
                .db 0b00111100
                .db 0b01111110
                ,db 0b00111100
                .db 0b00011000
                
                ;store in vx the lower 2 bits from the random value
                rnd Va, 0b11
            """.trimIndent()
        )

        Text("\n", style = HelpStyle.body)

        Text("hexadecimal", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text("A hexadecimal literal is represented by either prefix # or 0x followed by a row of hexadecimal digits (case insensitive).\n", style = HelpStyle.body)

        SyntaxBlock(
            """
                ld Vx, #ff ;loading FF (255) into register vx
                ld Vx, 0xff ;the very same operation but using the prefix 0x
                
                ld vx, #FF ;the very same operation but with capitalized digits
            """.trimIndent()
        )

        Text("\n", style = HelpStyle.body)

        Text("decimal", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        Text("A decimal is represented by a row of decimal digits (only) without any prefix", style = HelpStyle.body)

        SyntaxBlock(
            """
                ld vx, 24 ;This will load a value corresponding to the hexadecimal 0x18 (not 0x24!)
                ld vb, 2f ;This will error out. Don't mix up hex digits with decimal digits
            """.trimIndent()
        )

        Text("\n", style = HelpStyle.body)

        Text("expression", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        Text("A expression is a math operation between literals that returns a new literal, it is represented by the following format:\n", style = HelpStyle.body)

        Text("(<literal> op <literal> [op <literal>...])\n", style = HelpStyle.body)
        Text("Where <literal> can be any of the above representations, and op any of the following supported math operations: +/-/*", style = HelpStyle.body)
        Text("You can also use a label as <literal> operand that the assembler will replace it with the value it represents")
        
        SyntaxBlock(
            """
                ld va, (0xA*2) ;Assign va the value of A multiplied by two
                ld vb, (2+2+2) ;Assign vb the value 6
                ; An example of using labels in parenthesis expressions:
                ; The register 'I' will be assigned the address pointed by the label sprite_rows plus 4
                ld I, (sprite_rows+4)
            """.trimIndent()
        )

        Text("\n", style = HelpStyle.body)
    }
}

@Preview (uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HelpLiteralPreview() {
    Chip8IDETheme {
        Surface {
            HelpLiteral()
        }
    }
}

@Composable
private fun HelpInstructions() {
    val scrollState = rememberScrollState()

    Column (Modifier.verticalScroll(scrollState)) {
        Text("How to read instruction reference", style = HelpStyle.title)

        Text("\nInstructions are presented on the following format:", style = HelpStyle.body)
        Text("<address> - <instruction>", Modifier.offset(20.dp), style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(20.dp))

        Text("<Opcode>", style = HelpStyle.subtitle)

        Text("Opcodes are the two bytes (16-bit) words that the instructions mnemonics are assembled into, they are what compose the assembled program binary", style = HelpStyle.body)
        Text("\nAll instructions but CLS contain their arguments encoded into the opcode, on the Instruction Reference page the places where they go are represented with a combination of either nnn, x, y, kk or n (which are explained in the section below).", style = HelpStyle.body)

        Spacer(Modifier.height(20.dp))

        Text("<Instruction>", style = HelpStyle.subtitle)

        Text("Instructions instructs the interpreter to do something, all of them but CLS take one or two arguments.", style = HelpStyle.body)
        Text("\nThe arguments are:", style = HelpStyle.body)
        Text(" addr - a 12-bit address, can be either a label or literal", style = HelpStyle.body)
        Text(" byte - a byte literal", style = HelpStyle.body)
        Text(" Vx/Vy - a register, the x/y must be replaced with a hex digit in the range 0-F", style = HelpStyle.body)
        Text(" n - A nibble literal (4-bit number in the range 0-14), only used by DRW", style = HelpStyle.body)
    }
}

@Composable
private fun SyntaxBlock(code: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Black
    ) {
        Text(
            text = code.toChip8SyntaxAnnotatedString().text,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SyntaxDocumentation(format: String, desc: String, example: String? = null) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(format)

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))

                    Text(desc)

                    if (example != null) {
                        Spacer(Modifier.height(10.dp))
                        SyntaxBlock(example)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstructionReference(onClickReturn: () -> Unit = {}, title: String, instructions: List<InstructionInfo>) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("$title reference") },
                navigationIcon = { IconButton(onClick = onClickReturn) { Icon(Icons.Default.ArrowBack, "Return to previous page") }},
                scrollBehavior = scrollBehavior
            )
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            item {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(30.dp))
            }

            items(instructions) { (syntax, desc, example) ->
                SyntaxDocumentation(format = syntax, desc = desc, example = example)

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HelpLabel() {
    Column {
        Text("Label", style = HelpStyle.title)

        Text("\nA label is an identifier that serves as an alias to a memory location to where it is placed in the code or, in the special case of an equ directive, an alias to a literal.\n", style = HelpStyle.body)

        Text("The identifier can contain letters, underscore and numbers (you must not start with a number and only contain numbers), followed by a two-colon(:) suffix\n", style = HelpStyle.body)

        SyntaxBlock(
            """
                ;A line with only a label, the label will reference the data declared on the next line
                sprites:
                .db #2a, #ff, #ff
                .db #80, #33, #ff
                .db #77, #bb, #aa
                
                ;A single label can also reference instructions
                procedure:
                add v1, v2
                ret
                
                ;You can also continue after the label than the next line.
                sprite2: .db #ff. #ff. #ff
                
                ;.equ *must* always precede a label or it won't compile
                count: .equ 3
                
                ld v2, count ;on assembly count is replaced with the literal 3
                
                load I, sprites
                loop:
                drw v0,v1,3 ;Now draw the sprite data starting at address in I
                add I, 3
                sub v3, 1
                se v3, 0
                jp loop ;jump at the instruction being pointed by the label loop
            """.trimIndent()
        )
    }
}

@Preview (uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HelpLabelPreview() {
    Chip8IDETheme {
        Surface {
            HelpLabel()
        }
    }
}

@Composable
fun HelpIndex() {
    val navController = rememberNavController()

    data class Page(val headline: String, val desc: String, val route: String)
    
    val pages = listOf(
        Page("Introduction", "Introduction on the IDE and its assembly language (you should start here)", "intro"),
        Page("Literals", "About the literals supported", "literals"),
        Page("Labels", "About the labels", "labels"),
        Page("Instructions", "About the instructions", "instructions")
    )

    val referencePages = listOf(
        Page("Directives", "directives supported", "directives"),
        Page("Instructions", "instruction reference", "instructions_reference"),
    )

    NavHost(
        modifier = Modifier.safeContentPadding(),
        navController = navController,
        startDestination = "index",
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        composable("index") {
            Column {
                Text("Help", style = MaterialTheme.typography.headlineMedium)

                for (page in pages) {
                    ListItem(
                        modifier = Modifier.clickable { navController.navigate(page.route) },
                        headlineContent = { Text(page.headline) },
                        supportingContent = { Text(page.desc) },
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text("references", style = MaterialTheme.typography.titleLarge)
                for (page in referencePages) {
                    ListItem(
                        modifier = Modifier.clickable { navController.navigate(page.route) },
                        headlineContent = { Text(page.headline) },
                        supportingContent = { Text(page.desc) },
                    )
                }
            }
        }

        composable("intro") {
            HelpIntroduction()
        }

        composable("literals") {
            HelpLiteral()
        }

        composable("labels") {
            HelpLabel()
        }

        composable("instructions") {
            HelpInstructions()
        }

        composable("directives") {
            InstructionReference(
                onClickReturn = navController::popBackStack,
                title = "Directives",
                instructions = directives
            )
        }

        composable("instructions_reference") {
            InstructionReference(
                onClickReturn = navController::popBackStack,
                title = "Instructions",
                instructions = instructionsInfo
            )
        }


    }
}




/*

        Previews

*/

@Preview (uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun InstructionReferencePreview() {
    Chip8IDETheme {
        Surface(Modifier.fillMaxSize()) {
            InstructionReference(onClickReturn = {}, title = "Instructions", instructionsInfo)
        }
    }
}

@Preview (uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DirectiveReferencePreview() {
    Chip8IDETheme {
        Surface(Modifier.fillMaxSize()) {
            InstructionReference(onClickReturn = {}, title = "Directives", directives)
        }
    }
}

@Preview (uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HelpIndexPreview() {
    Chip8IDETheme {
        Surface(Modifier.fillMaxSize()) {
            HelpIndex()
        }
    }
}


@Preview (uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HelpInstructionsPreview() {
    Chip8IDETheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            HelpInstructions()
        }
    }
}
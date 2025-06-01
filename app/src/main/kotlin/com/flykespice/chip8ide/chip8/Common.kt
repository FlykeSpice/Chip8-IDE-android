package com.flykespice.chip8ide.chip8

//Common variables shared by Assembler and Disassembler utilities
object Common {
    val mnemonics = mapOf(
        "00E0" to "CLS",
        "00EE" to "RET",
        "0nnn" to "SYS addr",
        "1nnn" to "JP addr",
        "2nnn" to "CALL addr",
        "3xkk" to "SE Vx, byte",
        "4xkk" to "SNE Vx, byte",
        "5xy0" to "SE Vx, Vy",
        "6xkk" to "LD Vx, byte",
        "7xkk" to "ADD Vx, byte",
        "8xy0" to "LD Vx, Vy",
        "8xy1" to "OR Vx, Vy",
        "8xy2" to "AND Vx, Vy",
        "8xy3" to "XOR Vx, Vy",
        "8xy4" to "ADD Vx, Vy",
        "8xy5" to "SUB Vx, Vy",
        "8xy6" to "SHR Vx",
        "8xy7" to "SUBN Vx, Vy",
        "8xyE" to "SHL Vx",
        "9xy0" to "SNE Vx, Vy",
        "Annn" to "LD I, addr",
        "Bnnn" to "JP V0, addr",
        "Cxkk" to "RND Vx, byte",
        "Dxyn" to "DRW Vx, Vy, nibble",
        "Ex9E" to "SKP Vx",
        "ExA1" to "SKNP Vx",
        "Fx07" to "LD Vx, DT",
        "Fx0A" to "LD Vx, K",
        "Fx15" to "LD DT, Vx",
        "Fx18" to "LD ST, Vx",
        "Fx1E" to "ADD I, Vx",
        "Fx29" to "LD F, Vx",
        "Fx33" to "LD B, Vx",
        "Fx55" to "LD [I], Vx",
        "Fx65" to "LD Vx, [I]",
    )

    private val digits = intArrayOf(
        0xF0,0x90,0x90,0x90,0xF0, //0
        0x20,0x60,0x20,0x20,0x70, //1
        0xF0,0x10,0xF0,0x80,0xF0, //2
        0xF0,0x10,0xF0,0x10,0xF0, //3
        0x90,0x90,0xF0,0x10,0x10, //4
        0xF0,0x80,0xF0,0x10,0xF0, //5
        0xF0,0x80,0xF0,0x90,0xF0, //6
        0xF0,0x10,0x20,0x40,0x40, //7
        0xF0,0x90,0xF0,0x90,0xF0, //8
        0xF0,0x90,0xF0,0x10,0xF0, //9
        0xF0,0x90,0xF0,0x90,0x90, //A
        0xE0,0x90,0xE0,0x90,0xE0, //B
        0xF0,0x80,0x80,0x80,0xF0, //C
        0xE0,0x90,0x90,0x90,0xE0, //D
        0xF0,0x80,0xF0,0x80,0xF0, //E
        0xF0,0x80,0xF0,0x80,0x80  //F
    )
}
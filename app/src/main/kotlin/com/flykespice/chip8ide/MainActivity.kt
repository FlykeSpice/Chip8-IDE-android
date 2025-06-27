package com.flykespice.chip8ide

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.flykespice.chip8ide.data.Chip8IdeManager
import com.flykespice.chip8ide.ui.HelpIndex
import com.flykespice.chip8ide.ui.MainScreen
import com.flykespice.chip8ide.ui.SettingItem
import com.flykespice.chip8ide.ui.SettingsScreen
import com.flykespice.chip8ide.ui.theme.Chip8IDETheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.FileNotFoundException

private fun ByteArray.toBooleanArray(): BooleanArray {
    val booleanArray = BooleanArray(8*this.size)
    var index = 0

    for (byte in this) {
        for (bit in 7 downTo 0) {
            booleanArray[index++] = (byte.toInt() and (1 shl bit)) != 0
        }
    }

    return booleanArray
}

private fun AudioTrack.errorToString(error: Int) =
    when(error) {
        AudioTrack.ERROR -> "Generic Error"
        AudioTrack.ERROR_BAD_VALUE -> "Bad value entered"
        AudioTrack.ERROR_INVALID_OPERATION -> "Invalid Operation"
        AudioTrack.ERROR_DEAD_OBJECT -> "Object is not longer invalid and needs to be recreated"
        else -> throw IllegalArgumentException("AudioTrack: error $error isn't a valid error code")
    }

private fun BooleanArray.toByteArray(): ByteArray {
    val byteArray = ArrayList<Byte>(this.size/8)

    var binary = ""
    for (i in this.indices) {
        binary = (if (this[i]) "1" else "0")+binary

        if (binary.length >= 8) {
            byteArray.add(binary.toInt(2).toByte())
            binary = ""
        }
    }

    return byteArray.toByteArray()
}


class MainActivity : ComponentActivity() {
    var currentSpriteLabel: String? = null
    var currentSpriteData = BooleanArray(0)

    lateinit var audioTrack: AudioTrack
    lateinit var audioBuffer: ByteArray

    val chip8IdeManager: Chip8IdeManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        val inputStream = resources.openRawResource(R.raw.beep_sound)
        audioBuffer = inputStream.readBytes()
        inputStream.close()

        audioTrack = AudioTrack.Builder()
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(44100)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(audioBuffer.size)
            .build()

        audioTrack.write(audioBuffer, 0, audioBuffer.size)

        val result = audioTrack.setLoopPoints(0, (audioBuffer.size / 2) / 2, -1)
        if (result != AudioTrack.SUCCESS)
            throw IllegalStateException("AudioTrack: ${audioTrack.errorToString(result)}")

        audioTrack.pause()

        lifecycleScope.launch {
            chip8IdeManager.chip8.beepState.collect { state ->
                if (state && audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING)
                    audioTrack.play()
                else if (!state && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING)
                    audioTrack.pause()
            }
        }

        try {
            val file = applicationContext.openFileInput("code")
            chip8IdeManager.loadCode(file.readBytes().decodeToString())
            file.close()
        } catch (_: FileNotFoundException) { /*ignore*/ }

        //TODO: Save and reload current Sprite being edited if any
//        try {
//            var file = applicationContext.openFileInput("sprite_label")
//            currentSpriteLabel = file.readBytes().decodeToString()
//            file.close()
//
//            file = applicationContext.openFileInput("sprite_data")
//            currentSpriteData = file.readBytes().toBooleanArray()
//            file.close()
//
//        } catch (_: FileNotFoundException) { currentSpriteLabel = null }

        setContent {
            val backstack = remember { mutableStateListOf("main") }

            Chip8IDETheme {
                NavDisplay(
                    backStack = backstack,
                    onBack = { if (backstack.size > 1) backstack.removeLastOrNull() }
                ) { key ->
                    when (key) {
                        "main" -> NavEntry(key) {
                            MainScreen(
                                startDestination = if (currentSpriteLabel != null) "graphics/editor" else "editor",
                                onNavigateToOuter = backstack::add,
                                currentSpriteLabel = currentSpriteLabel,
                                currentSpriteData = currentSpriteData
                            )
                        }

                        "help" -> NavEntry(key) {
                            Surface(Modifier.fillMaxSize()) {
                                HelpIndex()
                            }
                        }

                        "settings" -> NavEntry(key) {
                            val realMode = chip8IdeManager.realMode.collectAsState()
                            val clockRate = chip8IdeManager.clockRate.collectAsState()

                            val settings = remember {
                                listOf(
                                    SettingItem(
                                        name = "Real emulation mode",
                                        desc = "Check to make Chip-8 emulator behave like it used on original hardware",
                                        value = realMode,
                                        onValueChange = {
                                            chip8IdeManager.setRealMode(it as Boolean)
                                        }
                                    ),
                                    SettingItem(
                                        name = "Chip-8 clock rate",
                                        desc = "The clock rate Chip-8 interpreter runs at. This option doesn't takes effect in Real emulation mode.",
                                        value = clockRate,
                                        enabled = derivedStateOf { !realMode.value },
                                        onValueChange = {
                                            chip8IdeManager.setClockRate(it as Int)
                                        }
                                    )
                                )
                            }

                            Surface(Modifier.fillMaxSize()) {
                                SettingsScreen(settings = settings, onPressBack = backstack::removeLastOrNull)
                            }
                        }

                        else -> throw IllegalStateException("Uknown navigated destination $key")
                    }
                }
            }
        }
    }

    override fun onPause() {
        var file = applicationContext.openFileOutput("code", 0)
        file.write(chip8IdeManager.code.value.encodeToByteArray())
        file.close()
        audioTrack.stop()

//        if (currentSpriteLabel != null) {
//            file = applicationContext.openFileOutput("sprite_label", 0)
//            file.write(currentSpriteLabel!!.encodeToByteArray())
//            file.close()
//
//            file = applicationContext.openFileOutput("sprite_data", 0)
//            file.write(currentSpriteData.toByteArray())
//            file.close()
//        } else {
//            applicationContext.deleteFile("sprite_label")
//            applicationContext.deleteFile("sprite_data")
//        }

        super.onPause()
    }

    override fun onDestroy() {
        var file = applicationContext.openFileOutput("code", 0)
        file.write(chip8IdeManager.code.value.encodeToByteArray())
        file.close()

//        if (currentSpriteLabel != null) {
//            file = applicationContext.openFileOutput("sprite_label", 0)
//            file.write(currentSpriteLabel!!.encodeToByteArray())
//            file.close()
//
//            file = applicationContext.openFileOutput("sprite_data", 0)
//            file.write(currentSpriteData.toByteArray())
//            file.close()
//        } else {
//            applicationContext.deleteFile("sprite_label")
//            applicationContext.deleteFile("sprite_data")
//        }
        audioTrack.stop()

        super.onDestroy()
    }
}


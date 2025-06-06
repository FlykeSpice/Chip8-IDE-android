package com.flykespice.chip8ide

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flykespice.chip8ide.data.Chip8Application
import com.flykespice.chip8ide.ui.HelpIndex
import com.flykespice.chip8ide.ui.MainScreen
import com.flykespice.chip8ide.ui.SettingItem
import com.flykespice.chip8ide.ui.SettingsScreen
import com.flykespice.chip8ide.ui.theme.Chip8IDETheme
import kotlinx.collections.immutable.toImmutableList
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

    lateinit var chip8Application: Chip8Application

    var currentSpriteLabel: String? = null
    var currentSpriteData = BooleanArray(0)

    lateinit var audioTrack: AudioTrack
    lateinit var audioBuffer: ByteArray

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

        chip8Application = (application as Chip8Application)
        chip8Application.provideInstance(activityResultRegistry) {
            //TODO: Defer pause to when AudioTracks stops playing
            if (it && audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING)
                audioTrack.play()
           else if (!it && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING)
                audioTrack.pause()
        }

        try {
            val file = applicationContext.openFileInput("code")
            chip8Application.chip8IdeManager.load(file.readBytes().decodeToString())
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
            val navController = rememberNavController()

            Chip8IDETheme {
                NavHost(
                    navController = navController,
                    startDestination = "main",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Up) }
                ) {
                    composable("main") {
                        MainScreen(
                            chip8ViewModel = chip8Application.chip8ViewModel,
                            //startDestination = if (currentSpriteLabel != null) "graphics/editor" else "editor",
                            onNavigate = navController::navigate,
                            currentSpriteLabel = currentSpriteLabel,
                            currentSpriteData = currentSpriteData
                        )
                    }

                    composable("help") {
                        Surface(Modifier.fillMaxSize()) {
                            HelpIndex()
                        }
                    }

                    composable("settings") {
                        val realMode = chip8Application.chip8IdeManager.realMode.collectAsState()
                        val clockRate = chip8Application.chip8IdeManager.clockRate.collectAsState()

                        val settings = remember {
                            listOf(
                                SettingItem(
                                    name = "Real emulation mode",
                                    desc = "Check to make Chip-8 emulator behave like it used on original hardware",
                                    value = realMode,
                                    onValueChange = {
                                        chip8Application.chip8IdeManager.setRealMode(it as Boolean)
                                    }
                                ),
                                SettingItem(
                                    name = "Chip-8 clock rate",
                                    desc = "The clock rate Chip-8 interpreter runs at. This option doesn't takes effect in Real emulation mode.",
                                    value = clockRate,
                                    enabled = derivedStateOf { !realMode.value },
                                    onValueChange = {
                                        chip8Application.chip8IdeManager.setClockRate(it as Int)
                                    }
                                )
                            ).toImmutableList()
                        }

                        Surface(Modifier.fillMaxSize()) {
                            SettingsScreen(settings = settings, onPressBack = navController::popBackStack)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        var file = applicationContext.openFileOutput("code", 0)
        file.write(chip8Application.chip8IdeManager.code.value.encodeToByteArray())
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
        file.write(chip8Application.chip8IdeManager.code.value.encodeToByteArray())
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


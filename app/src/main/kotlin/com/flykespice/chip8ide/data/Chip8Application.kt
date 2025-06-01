package com.flykespice.chip8ide.data

import android.app.Application
import android.media.AudioTrack
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import com.flykespice.chip8ide.ui.Chip8ViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class Chip8Application : Application() {

    private lateinit var launcherOpener: ActivityResultLauncher<Array<String>>
    private lateinit var launcherSaver: ActivityResultLauncher<String>
    private lateinit var launcherExporter: ActivityResultLauncher<String>

    lateinit var chip8IdeManager: Chip8IdeManager// = Chip8IdeManager(onSoundStateChange = onSoundStateChange)
        private set

    lateinit var chip8ViewModel: Chip8ViewModel
        private set

    fun provideInstance(activityResultRegistry: ActivityResultRegistry, onSoundStateChange: (Boolean) -> Unit) {
        chip8IdeManager = Chip8IdeManager(onSoundStateChange = onSoundStateChange)

        launcherOpener = activityResultRegistry.register("opener", ActivityResultContracts.OpenDocument()) { uri ->
            if(uri != null) {
                val stream = contentResolver.openInputStream(uri)
                if(stream != null) {
                    val bytes = stream.readBytes()
                    stream.close()

                    val text = bytes.decodeToString()
                    if("\uFFFD" !in text) {
                        chip8IdeManager.load(text)
                    } else {
                        val data = IntArray(bytes.size)
                        bytes.forEachIndexed { i, byte -> data[i] = byte.toInt() and 0xFF}

                        chip8IdeManager.load(data)
                    }
                }
            }
        }

        launcherSaver = activityResultRegistry.register("saver", ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if(uri != null) {
                val stream = contentResolver.openOutputStream(uri)

                if(stream != null) {
                    chip8IdeManager.save(stream)
                    stream.close()
                }
            }
        }

        launcherExporter = activityResultRegistry.register("exporter", ActivityResultContracts.CreateDocument("application/*")) { uri ->
            if(uri != null) {
                val stream = contentResolver.openOutputStream(uri)

                if(stream != null) {
                    chip8IdeManager.export(stream)
                    stream.close()
                }
            }
        }

        chip8ViewModel = Chip8ViewModel(
            chip8IdeManager,
            onChooseFile = { mimes -> launcherOpener.launch(mimes) },
            onSaveFile = {name -> launcherSaver.launch(name)},
            onExportFile = {name -> launcherExporter.launch(name)}
        )

    }
}
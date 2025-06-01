package com.flykespice.chip8ide.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flykespice.chip8ide.ui.theme.Chip8IDETheme

@Preview
@Composable
private fun EmulatorUIPreview() {
    Chip8IDETheme {
        val frameBuffer = remember { mutableStateOf(BooleanArray(64*32)) }
        val paused by remember { mutableStateOf(false) }

        EmulatorUI(frameBuffer = frameBuffer,
            paused = paused,
            reset = {},
            pause = {},
            chooseFile = {},
            setKey = {_, _ -> },
            paddingValues = PaddingValues(0.dp)
        )
    }
}

@Composable
fun EmulatorUI(chip8ViewModel: Chip8ViewModel, paddingValues: PaddingValues) {
    val paused = chip8ViewModel.paused.collectAsState()
    val frameBuffer = chip8ViewModel.frameBuffer.collectAsState()

    EmulatorUI(
        frameBuffer = frameBuffer,
        paused = paused.value,
        reset = chip8ViewModel::reset,
        pause = chip8ViewModel::pause,
        setKey = chip8ViewModel::setKey,
        chooseFile = chip8ViewModel::chooseFile,
        paddingValues = paddingValues
    )
}

@Composable
private fun EmulatorUI(
    frameBuffer: State<BooleanArray>,
    paused: Boolean,
    reset: () -> Unit,
    pause: (Boolean) -> Unit,
    setKey: (Int, Boolean) -> Unit,
    chooseFile: () -> Unit,
    paddingValues: PaddingValues
) {
    Surface(modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val canvasModifier = remember {
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 50.dp)
            }

            Canvas(modifier = canvasModifier) {
                drawRect(color = Color.Black, size = size)

                val pixelWith: Float = size.width / 64
                val pixelHeight: Float = size.height / 32

                val frameBuffer = frameBuffer.value

                val pixelSize = Size(pixelWith, pixelHeight)
                for (i in frameBuffer.indices) {
                    if (frameBuffer[i])
                        drawRect(
                            color = Color.Gray,
                            topLeft = Offset((i % 64) * pixelWith, (i / 64).toInt() * pixelHeight),
                            size = pixelSize
                        )
                }
            }

            Surface(modifier = Modifier.weight(0.1f)) {
                EmulationControls(paused = paused, reset = reset, pause = pause, chooseFile = chooseFile)
            }

            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxSize()
                    .padding(all = 26.dp)
            ) {
                EmulationKeys(setKey)
            }
        }
    }
}

@Composable
private fun EmulationControls (
    paused: Boolean,
    reset: () -> Unit,
    pause: (Boolean) -> Unit,
    chooseFile: () -> Unit
) {

    Row {

        Button(onClick = chooseFile) {
            Icon(Icons.Default.FolderOpen, "")
        }

        Spacer(modifier = Modifier.padding(10.dp))

        Button(onClick = { pause(!paused) }) {

            Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
        }

        Spacer(modifier = Modifier.padding(10.dp))

        Button(onClick = reset) {
            Icon(Icons.Default.Refresh, "Reset")
        }

        //Button(onClick = { /*TODO: Open emulation settings*/ }) { Icon(Icons.Default.Settings,"")}

        /*Text("Clock:", style = MaterialTheme.typography.headlineSmall)

        Surface(color = Color.LightGray) {
            var temp by remember { mutableStateOf(chip8.clockRate.toString()) }
            val focusManager = LocalFocusManager.current
            BasicTextField(
                value = temp,
                onValueChange = {temp = it},
                //label = {Text("Clock:")},
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.7f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions (onDone = { chip8.clockRate = temp.toInt(); focusManager.clearFocus()}),
                textStyle = MaterialTheme.typography.headlineSmall
            )
        }*/
    }
}

@Composable
private fun EmulationKeys(setKey: (Int, Boolean) -> Unit) {
    val interactions = remember { Array(16){ MutableInteractionSource() }}

    Column {
        val keys = "123C\n456D\n789E\nA0BF"

        for (row in keys.lines()) {
            Row (modifier = Modifier.weight(1f)) {
                for (key in row) {
                    val key = key.digitToInt(16)
                    val interaction = interactions[key]

                    Button(modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(all = 4.dp),
                        onClick = {},
                        interactionSource = interaction
                    ) {
                        Text(text = key.toString(16).uppercase(), style = MaterialTheme.typography.headlineMedium)
                    }

                    LaunchedEffect(interaction) {
                        interaction.interactions.collect {
                            when (it) {
                                is PressInteraction.Press -> { setKey(key, true)}
                                is PressInteraction.Release, is PressInteraction.Cancel -> { setKey(key, false)}
                            }
                        }
                    }
                }
            }
        }

    }
}
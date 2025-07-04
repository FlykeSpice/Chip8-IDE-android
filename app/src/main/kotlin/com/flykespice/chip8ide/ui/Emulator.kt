package com.flykespice.chip8ide.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flykespice.chip8ide.R
import com.flykespice.chip8ide.ui.theme.Chip8IDETheme

@Preview
@Composable
private fun EmulatorUIPreview() {
    Chip8IDETheme {
        val frameBuffer by remember { mutableStateOf(BooleanArray(64*32)) }
        val paused by remember { mutableStateOf(false) }

        EmulatorUI(
            framebuffer = frameBuffer,
            paused = paused,
            onClickReset = {},
            onClickPause = {},
            setKey = {_, _ -> },
            paddingValues = PaddingValues(0.dp)
        )
    }
}

@Composable
fun EmulatorUI(
    framebuffer: BooleanArray,
    paused: Boolean,
    onClickReset: () -> Unit,
    onClickPause: (Boolean) -> Unit,
    setKey: (Int, Boolean) -> Unit,
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

                val pixelSize = Size(pixelWith, pixelHeight)
                for (i in framebuffer.indices) {
                    if (framebuffer[i])
                        drawRect(
                            color = Color.Gray,
                            topLeft = Offset((i % 64) * pixelWith, (i / 64) * pixelHeight),
                            size = pixelSize
                        )
                }
            }

            Surface(modifier = Modifier.weight(0.1f)) {
                EmulationControls(paused = paused, onClickReset = onClickReset, onClickPause = onClickPause)
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
    onClickReset: () -> Unit,
    onClickPause: (Boolean) -> Unit
) {

    Row {
        Button(onClick = { onClickPause(!paused) }) {
            Icon(painterResource(if (paused) R.drawable.play_arrow_24px else R.drawable.pause_24px), contentDescription = null)
        }

        Spacer(modifier = Modifier.padding(10.dp))

        Button(onClick = onClickReset) {
            Icon(painterResource(R.drawable.refresh_24px), "Reset")
        }
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
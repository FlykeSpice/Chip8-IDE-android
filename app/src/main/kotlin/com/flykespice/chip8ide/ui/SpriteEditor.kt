package com.flykespice.chip8ide.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flykespice.chip8ide.R
import com.flykespice.chip8ide.data.Chip8IdeManager
import com.flykespice.chip8ide.ui.theme.Chip8IDETheme

/* TODO: Refactor this.... */
@Composable
fun SpriteEditorBrowser(
    sprites: List<Pair<String, BooleanArray>>,
    modifier: Modifier = Modifier,
    onClicked: (String) -> Unit
) {
    LazyVerticalGrid(
        modifier = modifier,
        //contentPadding = PaddingValues(20.dp),
        columns = GridCells.Fixed(2)
    ) {
        items(sprites) { (label, sprite) ->
            Column(
                modifier = Modifier
                    .clickable { onClicked(label) }
                    .padding(20.dp)
            ) {
                SpriteEditorCanvas(
                    sprite = sprite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(MaterialTheme.shapes.medium)
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 4.dp)
                    //color = Color.White
                )
            }
        }
    }
}


@Composable
fun SpriteEditorScreen(
    label: String,
    sprite: BooleanArray,
    onSubmit : (String, BooleanArray) -> Unit,
    //onLabelChanged: (String) -> Unit
) {
    var label by remember { mutableStateOf(label) }
    val sprite = remember { mutableStateOf(sprite) }
    var pencil by remember { mutableStateOf(false) }

    var openedResizeDialog by remember { mutableStateOf(false) }
    if (openedResizeDialog) {
        ResizeDialog(
            sprite = sprite.value,
            onResize = {
                sprite.value = sprite.value.copyOf(it*8)
                openedResizeDialog = false
            },
            onCancel = { openedResizeDialog = false }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.fillMaxHeight(0.2f))

        Row(Modifier.fillMaxWidth()) {
            var editLabelMode by remember { mutableStateOf(false) }

            if (editLabelMode) {
                var tempLabel by remember { mutableStateOf(label) }
                val focusRequester = remember { FocusRequester() }

                OutlinedTextField(
                    tempLabel,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .weight(3f),
                    onValueChange = { tempLabel = it },
                    textStyle = MaterialTheme.typography.headlineMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {label = tempLabel; editLabelMode = false}
                    ),
                    singleLine = true
                )

                LaunchedEffect(true) { focusRequester.requestFocus() }
            } else {
                Text(
                    label,
                    modifier = Modifier.weight(3f),
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            IconButton(
                onClick = {
                    editLabelMode = !editLabelMode
                },
                modifier = Modifier.weight(0.3f)
            ) {
                Icon(painterResource(R.drawable.edit_24px), "click to edit label's name")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(alignment = Alignment.CenterHorizontally),
            //shape = MaterialTheme.shapes.medium
        ) {

            val onClicked = remember {
                { x: Int, y: Int ->
                    sprite.value = sprite.value
                        .copyOf()
                        .apply {
                            Log.d("onClicked", "x = $x y = $y")
                            this[(y*8)+x] = pencil
                        }
                }
            }

            SpriteEditorCanvas(
                sprite = sprite.value,
                modifier = Modifier.fillMaxSize(),
                onClicked = onClicked
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row (
            Modifier
                .align(alignment = Alignment.CenterHorizontally)
                .height(50.dp)
                .padding(horizontal = 10.dp)
        ){
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = pencil,
                    onClick = { pencil = true },
                    shape = MaterialTheme.shapes.medium) {
                    Icon(painterResource(R.drawable.brush_24px), "Brush mode")
                }

                SegmentedButton(
                    selected = !pencil,
                    onClick = { pencil = false },
                    shape = MaterialTheme.shapes.medium) {
                    Icon(painterResource(R.drawable.delete_24px), "Delete mode")
                }
            }

            IconButton(onClick = {openedResizeDialog = true}) {
                Icon(painterResource(R.drawable.aspect_ratio_24px), "Resize the sprite")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
            onClick = { onSubmit(label, sprite.value.copyOf()) }
        ) {
            Text("Submit to Code")
        }
    }
}

@Composable
private fun ResizeDialog(
    sprite: BooleanArray,
    onResize: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var height by remember { mutableIntStateOf(sprite.size / 8) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Resize Sprite") },
        text = {
            TextField(
                value = height.toString(),
                onValueChange = {
                    height = if (it.isEmpty()) 0 else it.toInt()
                    isError = height == 0 || height > 15
                },
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                label = {
                    if (height == 0) {
                        Text("Height must be non-zero")
                    } else if (height > 15) {
                        Text("Height must be below or equal 15")
                    }
                }
            )
        },
        confirmButton = { TextButton(onClick = { onResize(height) })  { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = Color.Red) } }
    )
}

/*TODO: Keep aspect ratio when rendering*/
@Composable
private fun SpriteEditorCanvas(
    sprite: BooleanArray,
    modifier: Modifier = Modifier,
    onClicked: ((Int, Int) -> Unit)? = null, //Tap or Drag
    onZoom: ((Float) -> Unit)? = null //Unused for now
) {
    var canvaSize by remember { mutableStateOf(Size(1.0f, 1.0f)) }
    var pixelSize by remember { mutableStateOf(Size(1.0f, 1.0f)) }

    var minSize by remember { mutableFloatStateOf(1.0f) } //Need to be initialized 1 or else exception on zero division
    var yOffset by remember { mutableFloatStateOf(1.0f) }
    var xOffset by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(canvaSize, sprite.size) {
        val spriteHeight = sprite.size / 8

        pixelSize = Size(canvaSize.width / 8, canvaSize.height / spriteHeight)

        minSize = kotlin.math.min(pixelSize.height, pixelSize.width)
        yOffset = kotlin.math.max((canvaSize.height / 2.0f) - (minSize*spriteHeight / 2.0f), 0.0f)
        xOffset = kotlin.math.max((canvaSize.width / 2.0f) - ((minSize*8) / 2.0f), 0.0f)
    }

    val _modifier = remember (modifier, sprite.size) {
        if (onClicked != null) {
            modifier
                //.fillMaxSize()
                .pointerInput(pixelSize) {
                    detectDragGestures { change, dragAmount ->

                        if ((change.position.y + dragAmount.y) <= yOffset || (change.position.x + dragAmount.x) <= xOffset) {
                            return@detectDragGestures
                        }

                        val pixelX = (((change.position.x + dragAmount.x)-xOffset) / minSize).toInt()
                        val pixelY = (((change.position.y + dragAmount.y)-yOffset) / minSize).toInt()

                        if (pixelX !in 0 until 8 || pixelY !in 0 until (sprite.size / 8))
                            return@detectDragGestures

                        onClicked(pixelX, pixelY)
                    }

                    detectTapGestures { offset ->

                        if (offset.y <= yOffset || offset.x <= xOffset)
                            return@detectTapGestures

                        val pixelX = ((offset.x-xOffset) / minSize).toInt()
                        val pixelY = ((offset.y-yOffset) / minSize).toInt()

                        if (pixelX !in 0 until 8 || pixelY !in 0 until (sprite.size / 8))
                            return@detectTapGestures

                        onClicked(pixelX, pixelY)
                    }
                }
        } else {
            modifier
        }
    }

    Canvas( modifier = _modifier ) {
        if (canvaSize != size) {
            canvaSize = size
        }

        drawRect(color = Color.Black, size = size)

        val pixelSize = Size(minSize, minSize)
        for (i in sprite.indices) {
            if (sprite[i]) {
                drawRect(
                    color = Color.LightGray,
                    topLeft = Offset(((i % 8) * minSize) + xOffset, ((i / 8) * minSize) + yOffset),
                    size = pixelSize
                )
            }
        }
    }
}

@Composable
fun NewSpriteDialog(onCreate: (String, Int) -> Unit, onCancel: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    val isHeightError = remember(height) { (height.toIntOrNull() == null) || height.toInt() <= 0 || height.toInt() > 15 }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("New Sprite") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    isError = label.isBlank(),
                    supportingText = {
                        if (label.isBlank()) {
                            Text("Label must not be empty or blank")
                        }
                    }
                )

                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height") },
                    isError = isHeightError,
                    supportingText = {
                        if (height.toIntOrNull() == null) {
                            Text("Height must be a valid number")
                        } else if (height.toInt() <= 0) {
                            Text("height value must be higher than 0")
                        } else if (height.toInt() > 15) {
                            Text("Height must be between 1..15")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(label, height.toInt()) },
                enabled = label.isNotBlank() && !isHeightError
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onCancel)  { Text("Cancel", color = Color.Red) } }
    )
}

@Preview
@Composable
private fun SpriteEditorPreview() {
    val test = booleanArrayOf(
        false, false, true, true, true, false, false, false,
        false, true,  true, true, true, true,  false, false,
        false, true,  true, true, true, true,  false, false,
        false, true,  true, true, true, true,  false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
        false, false, true, true, true, false, false, false,
    )

    Chip8IDETheme {
        Surface (Modifier.fillMaxSize()) {
            SpriteEditorScreen(label = "sprite_test_foo", sprite = test, onSubmit = {_, _ ->})
        }
    }
}

@Preview
@Composable
private fun SpriteEditorBrowserPreview() {
    val test =
        """
                .sprite 2
                foo:
                db 0b00111100
                db 0b00111100
                
                .sprite 3
                bar:
                db 0b00111100
                db 0b00111100
                db 0b00111100
                
                .sprite 4
                test:
                db 0b00111100
                db 0b00111100
                db 0b00111100
                db 0b00111100
        """.trimIndent()

    val chip8IdeManager = Chip8IdeManager({_ -> })
    chip8IdeManager.update(test)

    Chip8IDETheme {
        Surface (modifier = Modifier.fillMaxSize()) {
            SpriteEditorBrowser(
                sprites = chip8IdeManager.getSprites(onError = {_,_ ->}),
                modifier = Modifier.fillMaxSize(),
                onClicked = {}
            )
        }
    }
}
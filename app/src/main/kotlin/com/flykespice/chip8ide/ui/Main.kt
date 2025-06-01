package com.flykespice.chip8ide.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flykespice.chip8ide.data.Chip8IdeManager
import com.flykespice.chip8ide.ui.visualtransformer.toChip8SyntaxAnnotatedString
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

@Composable
private fun MainSnackbar(
    snackbarHostState: SnackbarHostState,
    navController: NavController, ideState: MutableState<IdeState>,
    paddingValues: PaddingValues
) {

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            //.offset(y = (-100).dp)
            .consumeWindowInsets(paddingValues)
            .imePadding()
    )

    when (val state = ideState.value) {
        is IdeState.idle -> {}

        is IdeState.assembling -> LaunchedEffect(snackbarHostState) {
            snackbarHostState.showSnackbar("Assembling...")
        }

        is IdeState.success -> LaunchedEffect(snackbarHostState) {
            snackbarHostState.showSnackbar("Successfully assembled!")
            ideState.value = IdeState.idle()
        }

        is IdeState.error -> LaunchedEffect(snackbarHostState) {
            snackbarHostState.showSnackbar("Error: ${state.message} at ${state.line}")
            ideState.value = IdeState.idle()
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
private fun MainScreenPreview() {
    val dummy = MutableStateFlow(null)
    val chip8ViewModel = Chip8ViewModel(Chip8IdeManager({}), {}, {}, {})
    MainScreen(chip8ViewModel = chip8ViewModel, onNavigate = {})
}

private enum class OpenedDialog { newFile, openFile, exportFile, saveFile, newSprite, none }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    chip8ViewModel: Chip8ViewModel,
    startDestination: String = "editor",
    onNavigate: (String) -> Unit,
    currentSpriteLabel: String? = null,
    currentSpriteData: BooleanArray = BooleanArray(0),
    onSpriteEdit: (String, BooleanArray) -> Unit = {_, _ ->}
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    var openedDialog by remember { mutableStateOf(OpenedDialog.none) }

    var currentLabel by remember { mutableStateOf<String?>(currentSpriteLabel) }
    var currentSprite by remember { mutableStateOf(currentSpriteData) }

    when (openedDialog) {
        OpenedDialog.none -> {}

        OpenedDialog.openFile -> {
        }

        OpenedDialog.newFile -> {
            AlertDialog(onDismissRequest = { openedDialog = OpenedDialog.none }) {
                Surface(modifier = Modifier.wrapContentSize(), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(20.dp)) {
                        Text("WARNING", style = MaterialTheme.typography.titleLarge)

                        Spacer(modifier = Modifier.height(5.dp))

                        Text("This will erase all your current work\nproceed?")

                        TextButton(
                            onClick = {
                                chip8ViewModel.new()
                                openedDialog = OpenedDialog.none
                                navController.navigate("editor")
                            }) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }

        OpenedDialog.saveFile -> {
            SaveDialog(
                confirm = chip8ViewModel::save,
                close = { openedDialog = OpenedDialog.none }
            )
        }

        OpenedDialog.exportFile -> {
            SaveDialog(
                confirm = chip8ViewModel::export,
                close = { openedDialog = OpenedDialog.none }
            )
        }

        OpenedDialog.newSprite -> {
            NewSpriteDialog(
                onCreate = { label, height ->
                    currentLabel = label
                    currentSprite = BooleanArray(8*height)
                    openedDialog = OpenedDialog.none
                    navController.navigate("graphics/editor")
                },
                onCancel = { openedDialog = OpenedDialog.none }
            )
        }
    }

    var searchMatches: List<Pair<Int,Int>> = listOf()
    var searchCurrent by remember { mutableStateOf(0) }

    val currentDestination = navController.currentBackStackEntryAsState()

    val destinations = remember {
        listOf(
            "editor",
            "graphics",
            "emulator"
        )
    }


    Scaffold(
        topBar = {
            MainTopAppBar(
                currentDestination.value?.destination?.route ?: "editor",
                onClickOpen = { chip8ViewModel.chooseFile(); navController.navigate("editor") },
                onSearch = { keyword ->

                    if (keyword.isEmpty()) {
                        searchMatches = listOf()
                        return@MainTopAppBar
                    }

                    val code = chip8ViewModel.code.value
                    val matches = ArrayList<Pair<Int,Int>>()

                    var i = 0
                    while (i < code.length) {
                        i = code.indexOf(keyword, i)

                        if (i == -1)
                            break

                        matches.add(i to i+keyword.length)
                        i += keyword.length
                    }


                    val old = if (searchMatches.isNotEmpty()) searchMatches[searchCurrent].first else -1
                    searchMatches = matches

                    searchCurrent = matches.indexOfFirst { it.first > old }.takeIf { it != -1 } ?: 0
                },
                onSearchNext = { if (searchMatches.isNotEmpty()) searchCurrent = (searchCurrent+1) % searchMatches.size },
                onSearchPrev = { if (searchMatches.isNotEmpty()) searchCurrent = (searchCurrent-1) % searchMatches.size },
                onOpenDialog = { openedDialog = it  },
                onClickHelp = { onNavigate("help") },
                onClickSettings = { onNavigate("settings") }
            )
        },
        bottomBar = { MainBottomAppBar(navController) },
        snackbarHost = { MainSnackbar(snackbarHostState, navController, chip8ViewModel.ideState, PaddingValues(0.dp)) },
        floatingActionButton = {
            MainFloatingActionButton(destination = currentDestination.value?.destination?.route, onClicked = {
                val destination = currentDestination.value!!.destination.route
                if (destination == "editor") {
                    chip8ViewModel.assemble()
                } else if (destination == "graphics") {
                    //Prompt dialog to create
                    openedDialog = OpenedDialog.newSprite
                }
            })
        }
    ) { paddingValues ->

        //Need to hoist here because stupid NavHost keeps resetting when I put it inside NavHost.
        val editorScrollState = rememberScrollState()

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                val previousDestination = navController.previousBackStackEntry?.destination?.route ?: "editor"
                val currentDestination = currentDestination.value?.destination?.route ?: "editor"

                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.run {
                    if (destinations.indexOf(currentDestination) >= destinations.indexOf(previousDestination)) {
                        Left
                    } else {
                        Right
                    }
                })
            },
            exitTransition = {
                val previousDestination = navController.previousBackStackEntry?.destination?.route ?: "editor"
                val currentDestination = currentDestination.value?.destination?.route ?: "editor"

                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.run {
                    if (destinations.indexOf(currentDestination) >= destinations.indexOf(previousDestination)) {
                        Right
                    } else {
                        Left
                    }
                })
            }
        ) {

            composable("editor") {
                val code = chip8ViewModel.code.collectAsState()

                var textFieldValue by remember { mutableStateOf(TextFieldValue(code.value)) }

                LaunchedEffect(searchCurrent) {

                    if (searchMatches.isEmpty())
                        return@LaunchedEffect

                    val text = code.value
                    val (begin, end) = searchMatches[searchCurrent]
                    val line = text.slice(0..begin).lines().size

                    val lineHeight = editorScrollState.maxValue / text.lines().size
                    editorScrollState.scrollTo( abs( (lineHeight * line) - (4*lineHeight) ) )

                    textFieldValue = textFieldValue.copy(selection = TextRange(begin, end))
                }

                EditorScreen(
                    textField = textFieldValue,
                    scrollState = editorScrollState,
                    onValueChange = { textFieldValue = it; chip8ViewModel.updateCode(textFieldValue.text) },
                    styleText = { it.toChip8SyntaxAnnotatedString() },
                    paddingValues =  paddingValues
                )
            }

            composable("graphics") {

//                if (navController.previousBackStackEntry?.destination?.route == "graphics/editor") {
//                    currentLabel = null
//                } else if (currentLabel != null) {
//                    navController.navigate("graphics/editor")
//                    return@composable
//                }

                val code by chip8ViewModel.code.collectAsState()

                val sprites = remember {
                    getSprites(
                        _code = code,
                        onError = { err, line ->
                            chip8ViewModel.ideState.value = IdeState.error(err, line)
                            Log.e("", "sprite error: $err at $line")
                        }
                    )
                }

                Surface(Modifier.padding(paddingValues)) {

                    val sprites = remember(sprites) { sprites.toImmutableList() }
                    SpriteEditorBrowser(
                        sprites = sprites,
                        modifier = Modifier.fillMaxSize(),
                        onClicked = { label ->
                            currentLabel = label
                            currentSprite = sprites.find { it.first == label }!!.second

                            navController.navigate("graphics/editor")
                        }
                    )
                }
            }

            composable("graphics/editor") {
                //Go to editor with
                if (currentLabel == null)
                    throw IllegalStateException("Navigated to graphics/editor composable without a label or sprite")

                SpriteEditorScreen(
                    label = currentLabel!!,
                    sprite = currentSprite,
                    onSubmit = chip8ViewModel::updateSprite
                )
            }

            composable("emulator") {
                chip8ViewModel.stop()
                EmulatorUI(chip8ViewModel = chip8ViewModel, paddingValues = paddingValues)
            }

        }
    }
}

@Composable
private fun MainFloatingActionButton(destination: String?, onClicked: () -> Unit) {
    val map = mapOf(
        "editor" to Icons.Filled.PlayArrow,
        "graphics" to Icons.Filled.Add
    )

    if (destination == null || destination !in map.keys)
        return

    FloatingActionButton(onClick = onClicked) {
        Icon(map[destination]!!, "")
    }
}
@Composable
private fun MainDropDownMenu(
    expanded: MutableState<Boolean>,
    onClickOpen: () -> Unit,
    onOpenDialog: (OpenedDialog) -> Unit
) {
    DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
        DropdownMenuItem(text = {Text("New")}, onClick = { onOpenDialog(OpenedDialog.newFile); expanded.value = false })
        DropdownMenuItem(text = {Text("Open Rom")}, onClick = { onClickOpen(); expanded.value = false })
        DropdownMenuItem(text = {Text("Save") }, onClick = { onOpenDialog(OpenedDialog.saveFile); expanded.value = false }, /*enabled = isNotEmpty*/)
        DropdownMenuItem(text = {Text("Export") }, onClick = { onOpenDialog(OpenedDialog.exportFile); expanded.value = false },/* enabled = isNotEmpty*/)

        //DropdownMenuItem(text = {Text("Save As") }, onClick = {  })
    }
}

@Preview
@Composable
private fun SaveDialogPreview() {
    SaveDialog(
        confirm = {},
        close = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveDialog(confirm: (String) -> Unit, close: () -> Unit) {
    var filename by remember { mutableStateOf("") }
    val confirmEnabled = remember(filename) { filename != "" }

    AlertDialog(onDismissRequest = { filename = filename; close() }) {
        Surface (modifier = Modifier.wrapContentSize(), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp)) {
                TextField(value = filename, onValueChange = {filename = it}, singleLine = true)

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        confirm(filename)
                        close() },
                    enabled = confirmEnabled
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    destination: String,
    onClickOpen: () -> Unit,
    onOpenDialog: (OpenedDialog) -> Unit,
    onClickHelp: () -> Unit,
    onSearch: (String) -> Unit,
    onSearchNext: () -> Unit,
    onSearchPrev: () -> Unit,
    onClickSettings: () -> Unit
) {
    val expandedMenu = remember { mutableStateOf(false) }
    var searchMode by remember(destination) { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }

    Column {
        TopAppBar(
            title = {

                if (searchMode) {
                    TextField(
                        searchKeyword,
                        onValueChange = {
                            searchKeyword = it
                            onSearch(searchKeyword)
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                } else {
                    Text("Chip-8 IDE")
                }
            },
            navigationIcon = {
                IconButton(onClick = { expandedMenu.value = !expandedMenu.value }) {
                    Icon(Icons.Filled.Menu, contentDescription = null)
                }
            },
            actions = {

                if (searchMode) {
                    IconButton(onClick = onSearchPrev) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous match")
                    }

                    IconButton(onClick = onSearchNext) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match")
                    }
                } else if (destination == "editor") {
                    //Search bar
                    IconButton(onClick = {searchMode = true}) {
                        Icon(Icons.Filled.Search, contentDescription = "Search for keyword")
                    }
                }

                if (searchMode) {
                    IconButton(onClick = { searchMode = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel search")
                    }
                } else if (destination == "editor") {
                    // RowScope here, so these icons will be placed horizontally
                    IconButton(onClick = onClickHelp) {
                        Icon(Icons.Filled.QuestionMark, contentDescription = "Help")
                    }
                } else if (destination == "emulator") {
                    IconButton(onClick = onClickSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        )

        MainDropDownMenu(
            expanded = expandedMenu,
            onClickOpen = onClickOpen,
            onOpenDialog = onOpenDialog
        )
    }
}

@Composable
private fun MainBottomAppBar(navController: NavController) {

    val destinations = remember {
        listOf(
            "editor" to Icons.Filled.Edit,
            "graphics" to Icons.Filled.Face,
            "emulator" to Icons.Filled.PlayArrow
        )
    }

    val backstackState = navController.currentBackStackEntryAsState()

    NavigationBar {
        for((destination, icon) in destinations) {
            NavigationBarItem(
                selected = backstackState.value?.destination?.route == destination,
                onClick = {
                    navController.navigate(destination)
                },
                icon = {Icon(icon, "")},
                label = {Text(destination)})
        }
    }
}

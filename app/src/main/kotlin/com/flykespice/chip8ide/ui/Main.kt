package com.flykespice.chip8ide.ui

import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flykespice.chip8ide.R
import com.flykespice.chip8ide.data.IdeState
import com.flykespice.chip8ide.ui.viewmodel.EditorViewModel
import com.flykespice.chip8ide.ui.viewmodel.EmulatorViewModel
import com.flykespice.chip8ide.ui.viewmodel.ScaffoldViewModel
import com.flykespice.chip8ide.ui.viewmodel.SpriteBrowserViewModel
import com.flykespice.chip8ide.ui.viewmodel.SpriteEditorViewModel
import com.flykespice.chip8ide.ui.visualtransformer.toChip8SyntaxAnnotatedString
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

@Composable
private fun MainSnackbar(
    ideState: IdeState,
    paddingValues: PaddingValues
) {
    val snackbarHostState = remember { SnackbarHostState() }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            //.offset(y = (-100).dp)
            .consumeWindowInsets(paddingValues)
            .imePadding()
    )

    LaunchedEffect(ideState) {
        if (ideState !is IdeState.idle) {
            snackbarHostState.showSnackbar(ideState.message)
        }
    }
}

/*@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
private fun MainScreenPreview() {
    val chip8ViewModel = Chip8ViewModel(Chip8IdeManager({}), {}, {}, {})
    MainScreen(chip8ViewModel = chip8ViewModel, onNavigate = {})
}*/

private enum class OpenedDialog { newFile, openFile, exportFile, saveFile, newSprite, none }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    startDestination: String = "editor",
    onNavigateToOuter: (String) -> Unit,
    currentSpriteLabel: String? = null,
    currentSpriteData: BooleanArray = BooleanArray(0)
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    var openedDialog by remember { mutableStateOf(OpenedDialog.none) }
    var openedFileName by remember { mutableStateOf("") }

    var currentLabel by remember { mutableStateOf(currentSpriteLabel) }
    var currentSprite by remember { mutableStateOf(currentSpriteData) }

    val scaffoldViewModel = koinViewModel<ScaffoldViewModel>()

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null)

            openedFileName = ""
            if (cursor != null) {
                cursor.moveToFirst()
                openedFileName = cursor.getString(0)
                cursor.close()
            }

            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) {
                val data = stream.readBytes()
                stream.close()

                val text = data.decodeToString()
                if("\uFFFD" !in text) {
                    scaffoldViewModel.load(text)
                } else {
                    scaffoldViewModel.importROM(data)
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val stream = context.contentResolver.openOutputStream(uri)

            if(stream != null) {
                scaffoldViewModel.save(stream)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/*")) { uri ->
        if (uri != null) {
            val stream = context.contentResolver.openOutputStream(uri)

            if (stream != null) {
                scaffoldViewModel.export(stream)
            }
        }
    }

    when (openedDialog) {
        OpenedDialog.none -> {}

        OpenedDialog.openFile -> {
            openLauncher.launch(arrayOf("application/*", "text/plain"))
            openedDialog = OpenedDialog.none
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
                                openedDialog = OpenedDialog.openFile
                                navController.navigate("editor")
                            }) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }

        OpenedDialog.saveFile -> {
            saveLauncher.launch(openedFileName)
            openedDialog = OpenedDialog.none
        }

        OpenedDialog.exportFile -> {
            exportLauncher.launch(openedFileName)
            openedDialog = OpenedDialog.none
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

    val currentDestination = navController.currentBackStackEntryAsState()

    val destinations = remember {
        listOf(
            "editor",
            "graphics",
            "emulator"
        )
    }

    var searchKeyword by remember { mutableStateOf("") }
    val searchMatches = remember { mutableStateListOf<Pair<Int,Int>>() }
    var searchCurrent by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            MainTopAppBar(
                currentDestination.value?.destination?.route ?: "editor",
                onClickOpen = { openedDialog = OpenedDialog.openFile; navController.navigate("editor") },
                onSearch = { searchKeyword = it },
                onSearchNext = { if (searchMatches.isNotEmpty()) searchCurrent = (searchCurrent+1) % searchMatches.size },
                onSearchPrev = { if (searchMatches.isNotEmpty()) searchCurrent = (searchCurrent-1) % searchMatches.size },
                onOpenDialog = { openedDialog = it  },
                onClickHelp = { onNavigateToOuter("help") },
                onClickSettings = { onNavigateToOuter("settings") }
            )
        },
        bottomBar = { MainBottomAppBar(navController) },
        snackbarHost = {
            val ideState by scaffoldViewModel.ideState.collectAsState()
            MainSnackbar(
                ideState = ideState,
                paddingValues = PaddingValues(0.dp)
            )
        },
        floatingActionButton = {
            MainFloatingActionButton(destination = currentDestination.value?.destination?.route, onClicked = {
                val destination = currentDestination.value!!.destination.route
                if (destination == "editor") {
                    scaffoldViewModel.assemble()
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
                val editorViewModel = koinViewModel<EditorViewModel>()
                val code by editorViewModel.code.collectAsState()

                var textFieldValue by remember { mutableStateOf(TextFieldValue(code)) }

                LaunchedEffect(searchKeyword) {
                    val old = if (searchMatches.isNotEmpty()) searchMatches[searchCurrent].first else -1
                    searchMatches.clear()

                    if (searchKeyword.isEmpty()) {
                        return@LaunchedEffect
                    }

                    var i = 0
                    while (i < code.length) {
                        i = code.indexOf(searchKeyword, i)

                        if (i == -1)
                            break

                        searchMatches.add(i to i+searchKeyword.length)
                        i += searchKeyword.length
                    }

                    searchCurrent = searchMatches.indexOfFirst { it.first > old }.takeIf { it != -1 } ?: 0
                }

                LaunchedEffect(searchCurrent) {

                    if (searchMatches.isEmpty())
                        return@LaunchedEffect

                    val text = code
                    val (begin, end) = searchMatches[searchCurrent]
                    val line = text.slice(0..begin).lines().size

                    val lineHeight = editorScrollState.maxValue / text.lines().size
                    editorScrollState.scrollTo( abs( (lineHeight * line) - (4*lineHeight) ) )

                    textFieldValue = textFieldValue.copy(selection = TextRange(begin, end))
                }

                EditorScreen(
                    textField = textFieldValue,
                    scrollState = editorScrollState,
                    onValueChange = { textFieldValue = it; editorViewModel.updateCode(textFieldValue.text) },
                    styleText = { it.toChip8SyntaxAnnotatedString() },
                    paddingValues =  paddingValues
                )
            }

            composable("graphics") {
                val spriteBrowserViewModel = koinViewModel<SpriteBrowserViewModel>()
                val sprites by spriteBrowserViewModel.sprites.collectAsState()

                Surface(Modifier.padding(paddingValues)) {
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
                val spriteViewModel = koinViewModel<SpriteEditorViewModel>()

                //Go to editor with
                if (currentLabel == null)
                    throw IllegalStateException("Navigated to graphics/editor composable without a label or sprite")

                SpriteEditorScreen(
                    label = currentLabel!!,
                    sprite = currentSprite,
                    onClickSubmit = { spriteViewModel.submit(currentLabel!!) }
                )
            }

            composable("emulator") {
                val emulatorViewModel = koinViewModel<EmulatorViewModel>()

                val framebuffer by emulatorViewModel.framebuffer.collectAsState()
                val paused by emulatorViewModel.paused.collectAsState()

                EmulatorUI(
                    framebuffer = framebuffer,
                    paused = paused,
                    onClickReset = emulatorViewModel::reset,
                    onClickPause = emulatorViewModel::pause,
                    setKey = emulatorViewModel::setKey,
                    chooseFile = {},
                    paddingValues = paddingValues
                )
            }

        }
    }
}

@Composable
private fun MainFloatingActionButton(destination: String?, onClicked: () -> Unit) {
    val map = mapOf(
        "editor" to painterResource(R.drawable.play_arrow_24px),
        "graphics" to painterResource(R.drawable.add_24px)
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
                    Icon(painterResource(R.drawable.menu_24px), contentDescription = null)
                }
            },
            actions = {

                if (searchMode) {
                    IconButton(onClick = onSearchPrev) {
                        Icon(painterResource(R.drawable.keyboard_arrow_up_24px), contentDescription = "Previous match")
                    }

                    IconButton(onClick = onSearchNext) {
                        Icon(painterResource(R.drawable.keyboard_arrow_down_24px), contentDescription = "Next match")
                    }
                } else if (destination == "editor") {
                    //Search bar
                    IconButton(onClick = {searchMode = true}) {
                        Icon(painterResource(R.drawable.search_24px), contentDescription = "Search for keyword")
                    }
                }

                if (searchMode) {
                    IconButton(onClick = { searchMode = false }) {
                        Icon(painterResource(R.drawable.close_24px), contentDescription = "Cancel search")
                    }
                } else if (destination == "editor") {
                    // RowScope here, so these icons will be placed horizontally
                    IconButton(onClick = onClickHelp) {
                        Icon(painterResource(R.drawable.question_mark_24px), contentDescription = "Help")
                    }
                } else if (destination == "emulator") {
                    IconButton(onClick = onClickSettings) {
                        Icon(painterResource(R.drawable.settings_24px), contentDescription = "Settings")
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

    val destinations = listOf(
        "editor" to painterResource(R.drawable.editor),
        "graphics" to painterResource(R.drawable.brush_24px),
        "emulator" to painterResource(R.drawable.play_arrow_24px)
    )

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

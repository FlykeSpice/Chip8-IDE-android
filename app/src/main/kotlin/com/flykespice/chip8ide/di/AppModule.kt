package com.flykespice.chip8ide.di

import com.flykespice.chip8ide.data.Chip8IdeManager
import com.flykespice.chip8ide.ui.viewmodel.EditorViewModel
import com.flykespice.chip8ide.ui.viewmodel.EmulatorViewModel
import com.flykespice.chip8ide.ui.viewmodel.ScaffoldViewModel
import com.flykespice.chip8ide.ui.viewmodel.SpriteBrowserViewModel
import com.flykespice.chip8ide.ui.viewmodel.SpriteEditorViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::Chip8IdeManager)

    viewModelOf(::ScaffoldViewModel)
    viewModelOf(::EditorViewModel)
    viewModelOf(::SpriteBrowserViewModel)
    viewModelOf(::SpriteEditorViewModel)
    viewModelOf(::EmulatorViewModel)
}
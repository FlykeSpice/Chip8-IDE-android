package com.flykespice.chip8ide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.flykespice.chip8ide.R
import com.flykespice.chip8ide.ui.theme.Chip8IDETheme

data class SettingItem(
    val name: String,
    val desc: String,
    val value: State<Any>,
    val enabled: State<Boolean>? = null,
    val onValueChange: (Any) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: List<SettingItem>, onPressBack: () -> Unit) {

    var openedDialogSetting by remember { mutableStateOf<SettingItem?>(null) }

    if (openedDialogSetting != null) {
        var value by remember { mutableStateOf(openedDialogSetting!!.value.value) }

        InputDialog(
            value = (value as Int).toString(),
            onValueChange = { value = if (it.isNotEmpty()) it.toInt() else 0 },
            onDismissRequest = { openedDialogSetting = null },
            onClickConfirm = { openedDialogSetting!!.onValueChange(value); openedDialogSetting = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onPressBack) {
                        Icon(painterResource(R.drawable.arrow_back_24px), "Return")
                    }
                }
            )
        }
    ) {
        val listItemColorsDisabled = ListItemDefaults.colors(
            headlineColor = ListItemDefaults.colors().disabledHeadlineColor,
            supportingColor = ListItemDefaults.colors().disabledHeadlineColor,
            trailingIconColor = ListItemDefaults.colors().disabledTrailingIconColor
        )

        Column(Modifier.padding(it)) {
            settings.forEachIndexed { index, (name, desc, state, enabled, onValueChange) ->
                val settingModifier = remember(enabled?.value) {
                    if (enabled?.value != false) {
                        when (state.value) {
                            is Boolean -> Modifier.clickable { onValueChange(!(state.value as Boolean)) }
                            is Int -> Modifier.clickable { openedDialogSetting = settings[index] }
                            else -> Modifier
                        }
                    } else {
                        Modifier
                    }
                }

                ListItem(
                    modifier = settingModifier,
                    headlineContent = { Text(name) },
                    supportingContent = { Text(desc) },
                    trailingContent = {

                        when (state.value) {
                            is Boolean -> Switch(
                                checked = state.value as Boolean,
                                enabled = enabled?.value != false,
                                onCheckedChange = onValueChange
                            )

                            is Int -> Text((state.value as Int).toString(), style = MaterialTheme.typography.titleLarge)
                            else -> throw IllegalArgumentException("The type value of this ${state.value} is not supported as setting entry")
                        }
                    },
                    colors = if (enabled?.value != false) ListItemDefaults.colors() else listItemColorsDisabled
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onClickConfirm: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(Modifier.wrapContentSize()) {
            Column(Modifier.padding(20.dp)) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                TextButton(onClick = onClickConfirm) {
                    Text("Confirm")
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    val realMode = remember { mutableStateOf(false) }
    val clockRate = remember { mutableIntStateOf(300) }

    val settings = listOf(
        SettingItem(
            name = "Real emulation mode",
            desc = "Check to make Chip-8 emulator behave like it used on original hardware",
            value = realMode,
            onValueChange = {
                realMode.value = it as Boolean
            }
        ),
        SettingItem(
            name = "Chip-8 clock rate",
            desc = "The clock rate Chip-8 interpreter runs at.",//\nThis option doesn't takes effect in Real emulation mode.",
            value = clockRate,
            onValueChange = {
                clockRate.intValue = it as Int
            }
        )
    )

    Chip8IDETheme {
        Surface {
            SettingsScreen(settings = settings, onPressBack = {})
        }
    }
}
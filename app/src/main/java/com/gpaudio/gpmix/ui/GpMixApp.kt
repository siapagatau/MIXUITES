@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gpaudio.gpmix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpaudio.gpmix.MixerViewModel
import com.gpaudio.gpmix.R

private val PadKeys = listOf(
    Key.One, Key.Two, Key.Three, Key.Four, Key.Q, Key.W, Key.E, Key.R,
    Key.A, Key.S, Key.D, Key.F, Key.Z, Key.X, Key.C, Key.V,
)

@Composable
fun GpMixApp(
    vm: MixerViewModel,
    onToggleEngine: () -> Unit,
    media: MediaActions,
    settings: SettingsActions,
    onSavePreset: (String) -> Unit,
    onLoadPreset: () -> Unit,
) {
    GpTheme(vm.theme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!vm.engineReady) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text(stringResource(R.string.engine_error_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.engine_error_text, vm.engineError ?: "?"))
                }
                return@Surface
            }
            Content(vm, onToggleEngine, media, settings, onSavePreset, onLoadPreset)
        }
    }
}

@Composable
private fun Content(
    vm: MixerViewModel,
    onToggleEngine: () -> Unit,
    media: MediaActions,
    settings: SettingsActions,
    onSavePreset: (String) -> Unit,
    onLoadPreset: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val focus = remember { FocusRequester() }

    // Pesan sekali-tampil dari ViewModel.
    val msg = vm.message
    if (msg != null) {
        val text = if (msg.arg != null) stringResource(msg.resId, msg.arg) else stringResource(msg.resId)
        LaunchedEffect(msg) {
            snack.showSnackbar(text)
            vm.dismissMessage()
        }
    }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) { } }


    BackHandler(enabled = tab != 0) { tab = 0 }

    Scaffold(
        modifier = Modifier
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (e.isCtrlPressed || e.isMetaPressed) {
                    when (e.key) {
                        Key.Z -> { if (e.isShiftPressed) vm.redo() else vm.undo(); true }
                        Key.Y -> { vm.redo(); true }
                        Key.S -> { vm.saveNow(); true }
                        else -> false
                    }
                } else if (!e.isAltPressed) {
                    val i = PadKeys.indexOf(e.key)
                    if (i >= 0 && tab == 1) { vm.triggerPad(i); true } else false
                } else false
            },
        containerColor = Console.bg,
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (tab == 0) {
                MixerScreen(vm, onToggleEngine, { tab = 1 }, { tab = 2 })
            } else {
                Column(Modifier.fillMaxSize()) {
                    SubHeader(
                        stringResource(if (tab == 1) R.string.tab_pads else R.string.tab_settings),
                        stringResource(R.string.tab_mixer),
                    ) { tab = 0 }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (tab == 1) MediaScreen(vm, media) else SettingsScreen(vm, settings)
                    }
                }
            }
        }
    }

    vm.selectedChannel?.let { id -> ChannelSettings(vm, id) }
    vm.editingInsert?.let { (id, slot) -> EffectEditor(vm, id, slot, onSavePreset, onLoadPreset) }
}

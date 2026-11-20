package com.gpaudio.gpmix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpaudio.gpmix.MixerViewModel
import com.gpaudio.gpmix.R
import com.gpaudio.gpmix.model.ChKind
import java.util.Locale

/**
 * Layar mixer: toolbar di atas, strip di tengah, status bar di bawah (seperti manual GP Mix).
 * Semua strip digambar lengkap seperti di manual pada ukuran rancangan, lalu seluruh area strip diperkecil
 * secara seragam agar pas di layar tanpa terpotong dan tanpa scroll (mode "Fit", bawaan). Menu Zoom di
 * toolbar memperbesar ke ukuran yang lebih mudah disentuh; saat itu area strip bisa digeser.
 */
@Composable
fun MixerScreen(
    vm: MixerViewModel,
    onToggleEngine: () -> Unit,
    onOpenMedia: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scene = vm.project.scene
    val strips = scene.channels.filter { it.kind != ChKind.MASTER } + listOfNotNull(scene.master)
    var zoom by rememberSaveable { mutableStateOf(0f) }   // 0 = Fit
    Column(modifier.fillMaxSize().background(Console.bg)) {
        Toolbar(vm, zoom, { zoom = it }, onToggleEngine, onOpenMedia, onOpenSettings)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val pad = 4.dp
            val gapD = 3.dp
            val n = strips.size.coerceAtLeast(1)
            val availW = maxWidth - pad * 2
            val availH = maxHeight - pad * 2
            val hd = designHeight(vm.hiddenSections)
            val sH = availH / hd
            val sW = availW / (MinDesignW * n + gapD * (n - 1))
            val fitS = minOf(1f, sH, sW).coerceAtLeast(0.2f)
            // Batas keterbacaan: kalau "Fit" membuat strip lebih kecil dari ini (landscape / banyak channel),
            // pakai ukuran ini dan biarkan area strip bisa digeser, daripada huruf menjadi terlalu kecil.
            val scroll = zoom > 0f || fitS < MinLegibleScale
            val s = if (zoom > 0f) zoom else maxOf(fitS, MinLegibleScale)
            val wd = if (zoom > 0f) 104.dp
            else ((availW / s - gapD * (n - 1)) / n).coerceIn(if (scroll) 104.dp else MinDesignW, MaxDesignW)
            val hdEff = if (zoom > 0f) hd else maxOf(hd, availH / s)
            val content: @Composable () -> Unit = {
                ScaledBox(s) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gapD)) {
                        for (c in strips) ConsoleStrip(vm, c, wd, hdEff)
                    }
                }
            }
            if (scroll) {
                Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState()).padding(pad)) { content() }
            } else {
                Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { content() }
            }
        }
        StatusBar(vm, onOpenSettings)
    }
}

private val MinDesignW = 96.dp
private val MaxDesignW = 140.dp
private const val MinLegibleScale = 0.6f

/** Menggambar isi pada ukuran aslinya lalu menskalakannya; ukuran yang dilaporkan sudah terskala. */
@Composable
private fun ScaledBox(scale: Float, content: @Composable () -> Unit) {
    Layout(
        content = {
            Box(Modifier.graphicsLayer(scaleX = scale, scaleY = scale, transformOrigin = TransformOrigin(0f, 0f))) { content() }
        },
    ) { measurables, _ ->
        val p = measurables[0].measure(Constraints())
        layout((p.width * scale).roundToInt(), (p.height * scale).roundToInt()) { p.place(0, 0) }
    }
}

@Composable
private fun Toolbar(
    vm: MixerViewModel, zoom: Float, onZoom: (Float) -> Unit,
    onToggleEngine: () -> Unit, onOpenMedia: () -> Unit, onOpenSettings: () -> Unit,
) {
    val ui = LocalUi.current
    val p = vm.project
    var addMenu by remember { mutableStateOf(false) }
    var sceneMenu by remember { mutableStateOf(false) }
    var viewMenu by remember { mutableStateOf(false) }
    var zoomMenu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val sets = p.settings
    Column(Modifier.fillMaxWidth().background(Console.bar)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = ui.barPadH, vertical = ui.barPadV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BarTitle("GP Mix " + vm.limits.name)
            val run = stringResource(if (vm.running) R.string.stop else R.string.start)
            BarBtn(run, vm.running, onToggleEngine, run, onColor = Console.green)
            BarSep()
            BarBtn("↶", false, { vm.undo() }, stringResource(R.string.undo), enabled = vm.canUndo, toggle = false)
            BarBtn("↷", false, { vm.redo() }, stringResource(R.string.redo), enabled = vm.canRedo, toggle = false)
            BarSep()
            Box {
                BarBtn(stringResource(R.string.add_channel) + " ▾", false, { addMenu = true }, stringResource(R.string.add_channel), toggle = false)
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    listOf(ChKind.AUDIO to R.string.kind_input, ChKind.AUX to R.string.kind_aux, ChKind.GROUP to R.string.kind_group).forEach { (k, label) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { addMenu = false; vm.addChannel(k) })
                    }
                }
            }
            BarSep()
            BarBtn("<", false, { vm.selectScene(p.activeScene - 1) }, stringResource(R.string.scene), toggle = false)
            Box {
                Field(p.scene.name, { sceneMenu = true }, stringResource(R.string.cd_scene, p.scene.name), Modifier.width(120.dp), h = ui.btnH)
                DropdownMenu(expanded = sceneMenu, onDismissRequest = { sceneMenu = false }) {
                    p.scenes.forEachIndexed { i, s ->
                        DropdownMenuItem(
                            text = { Text((if (i == p.activeScene) "✓ " else "    ") + s.name) },
                            onClick = { sceneMenu = false; vm.selectScene(i) },
                        )
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.scene_new)) }, onClick = { sceneMenu = false; dialog = "new" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.scene_duplicate)) }, onClick = { sceneMenu = false; vm.duplicateScene() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.scene_rename)) }, onClick = { sceneMenu = false; dialog = "rename" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.scene_delete)) }, onClick = { sceneMenu = false; dialog = "delete" })
                }
            }
            BarBtn(">", false, { vm.selectScene(p.activeScene + 1) }, stringResource(R.string.scene), toggle = false)
            BarBtn(stringResource(R.string.store), false, { vm.saveNow() }, stringResource(R.string.store), toggle = false)
            BarBtn("New", false, { dialog = "new" }, stringResource(R.string.scene_new), toggle = false)
            BarSep()
            BarBtn("Pads", false, onOpenMedia, "Sample Pads", toggle = false)
            BarBtn("Player", false, onOpenMedia, "Player", toggle = false)
            BarBtn(
                "Ducking", sets.duckingEnabled, { vm.setSettings { it.copy(duckingEnabled = !it.duckingEnabled) } },
                "Ducking", onColor = Console.amber,
            )
            BarSep()
            Box {
                BarBtn(stringResource(R.string.view_menu) + " ▾", false, { viewMenu = true }, stringResource(R.string.view_menu), toggle = false)
                DropdownMenu(expanded = viewMenu, onDismissRequest = { viewMenu = false }) {
                    StripSectionKeys.forEach { k ->
                        val shown = k !in vm.hiddenSections
                        DropdownMenuItem(
                            text = { Text((if (shown) "✓ " else "    ") + k.replaceFirstChar { it.uppercase() }) },
                            onClick = { vm.toggleSection(k) },
                        )
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.show_all)) }, onClick = { viewMenu = false; vm.showAllSections() })
                }
            }
            Box {
                BarBtn(if (zoom <= 0f) "Zoom: Fit ▾" else "Zoom: ${(zoom * 100).roundToInt()}% ▾", false, { zoomMenu = true }, "Zoom", toggle = false)
                DropdownMenu(expanded = zoomMenu, onDismissRequest = { zoomMenu = false }) {
                    listOf(0f, 0.6f, 0.8f, 1f, 1.3f).forEach { z ->
                        DropdownMenuItem(
                            text = { Text((if (z == zoom) "✓ " else "    ") + (if (z <= 0f) "Fit" else "${(z * 100).roundToInt()}%")) },
                            onClick = { zoomMenu = false; onZoom(z) },
                        )
                    }
                }
            }
            BarBtn(stringResource(R.string.setup_btn), false, onOpenSettings, stringResource(R.string.setup_btn), toggle = false)
        }
        HLine()
    }
    SceneDialogs(vm, dialog) { dialog = null }
}

@Composable
private fun SceneDialogs(vm: MixerViewModel, dialog: String?, close: () -> Unit) {
    when (dialog) {
        "new", "rename" -> {
            var name by remember { mutableStateOf(if (dialog == "rename") vm.project.scene.name else "") }
            val isNew = dialog == "new"
            AlertDialog(
                onDismissRequest = close,
                title = { Text(stringResource(if (isNew) R.string.scene_new else R.string.scene_rename)) },
                text = { OutlinedTextField(name, { name = it.take(40) }, singleLine = true, label = { Text(stringResource(R.string.name)) }) },
                confirmButton = {
                    GpTextButton(onClick = { if (isNew) vm.addScene(name) else vm.renameScene(name); close() }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = { GpTextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
            )
        }
        "delete" -> AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(R.string.scene_delete)) },
            text = { Text(stringResource(R.string.scene_delete_confirm, vm.project.scene.name)) },
            confirmButton = { GpTextButton(onClick = { vm.deleteScene(); close() }, enabled = vm.project.scenes.size > 1) { Text(stringResource(R.string.delete)) } },
            dismissButton = { GpTextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun StatusBar(vm: MixerViewModel, onOpenSettings: () -> Unit) {
    val i = vm.info
    val ui = LocalUi.current
    Column(Modifier.fillMaxWidth().background(Console.bar)) {
        HLine()
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = ui.barPadH, vertical = ui.barPadV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val dev = if (vm.running && i.sampleRate > 0)
                String.format(Locale.US, "%.1f kHz · buf %d · %.1f ms", i.sampleRate / 1000f, i.bufferFrames, i.latencyMs)
            else "—"
            Text(dev, fontSize = ui.caption, color = Console.label, maxLines = 1)
            Text("XR ${i.xruns}", fontSize = ui.caption, color = if (i.xruns > 0) Console.red else Console.label)
            Text("BPM ${vm.project.settings.tempo.toInt()}", fontSize = ui.caption, color = Console.label)
            if (vm.captureActive) Text("APP", fontSize = ui.caption, color = Console.blue)
            Text(vm.project.name, fontSize = ui.caption, color = Console.label, maxLines = 1)
            val rec = vm.recording
            BarBtn(
                "● REC", rec, { if (rec) vm.stopRecording() else vm.startRecording() },
                stringResource(if (rec) R.string.rec_stop else R.string.rec_start), onColor = Console.red, small = true,
            )
            BarBtn("RETRO", false, { vm.saveRetro() }, stringResource(R.string.retro_save), toggle = false, small = true)
        }
    }
}

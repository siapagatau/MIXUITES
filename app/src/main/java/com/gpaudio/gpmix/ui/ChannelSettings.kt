@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gpaudio.gpmix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gpaudio.gpmix.MixerViewModel
import com.gpaudio.gpmix.R
import com.gpaudio.gpmix.model.ChKind

/**
 * Pengaturan lengkap satu channel. Memakai panel yang sama dengan strip di mixer, jadi tampilannya
 * selaras; berguna terutama saat layar terlalu kecil untuk menampilkan semua bagian strip.
 */
@Composable
fun ChannelSettings(vm: MixerViewModel, id: Int) {
    val c = vm.project.scene.channel(id) ?: return
    var name by remember(id) { mutableStateOf(c.name) }
    var confirm by remember { mutableStateOf(false) }
    val master = c.kind == ChKind.MASTER
    val audio = c.kind == ChKind.AUDIO
    val commit = { vm.editChannel(id) { it.copy(name = name.trim().ifEmpty { it.name }) } }
    val close = {
        if (name.trim().isNotEmpty() && name.trim() != c.name) commit()
        vm.selectedChannel = null
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Console.bg) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(kindLabel(c.kind).uppercase(), fontSize = LocalUi.current.caption, color = Console.label, letterSpacing = 1.sp)
                        BasicTextField(
                            name, { name = it.take(40) }, singleLine = true,
                            textStyle = TextStyle(color = Console.text, fontSize = LocalUi.current.title, fontWeight = FontWeight.Bold),
                            cursorBrush = SolidColor(Console.blue),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Console.field)
                                .border(1.dp, Console.fieldBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp)
                                .semantics { contentDescription = "Nama" },
                        )
                    }
                    MiniBtn("OK", false, { commit() }, stringResource(R.string.ok), toggle = false, h = LocalUi.current.btnH)
                    MiniBtn("✕ " + stringResource(R.string.close), false, close, stringResource(R.string.close), toggle = false, h = LocalUi.current.btnH)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until 8) {
                        Box(
                            Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(6.dp)).background(ChannelColors[i])
                                .border(if (c.color == i) 3.dp else 0.dp, Color.White, RoundedCornerShape(6.dp))
                                .clickable { vm.editChannel(id) { it.copy(color = i) } }
                                .semantics { contentDescription = "Color ${i + 1}" },
                        )
                    }
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val pw = Modifier.width(160.dp)
                    if (audio) Panel("PRESET", pw.height(StripH.PRESET)) { PresetBody(vm, c, true) }
                    if (!master) Panel("INPUT", pw.height(StripH.INPUT)) { InputBody(vm, c) }
                    if (!master) {
                        Panel("FILTER", pw.height(StripH.FILTER)) { FilterBody(vm, c) }
                        Panel("SLOPE", pw.height(104.dp)) { SlopeRows(vm, c.id, c.hpfSlope, c.lpfSlope) }
                    }
                    Panel("INSERT", pw.height(176.dp)) { InsertBody(vm, c) }
                    if (!master) {
                        Panel("SENDS", pw.height(176.dp), Console.blue) { SendsBody(vm, c) }
                        Panel("OUTPUT", pw.height(StripH.OUTPUT)) { OutputBody(vm, c) }
                        Panel("PAN", pw.height(StripH.PAN)) { PanBody(vm, c, 100.dp) }
                    }
                }
                if (!master) {
                    MiniBtn(stringResource(R.string.channel_delete), false, { confirm = true }, stringResource(R.string.channel_delete),
                        Modifier.width(160.dp), onColor = Console.red, toggle = false, h = LocalUi.current.btnH)
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.channel_delete)) },
            text = { Text(stringResource(R.string.channel_delete_confirm, c.name)) },
            confirmButton = { GpTextButton(onClick = { confirm = false; vm.removeChannel(c.id) }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { GpTextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SlopeRows(vm: MixerViewModel, id: Int, hpf: Int, lpf: Int) {
    val labels = listOf("12", "24", "36", "48")
    Text("HPF dB/oct", fontSize = 9.sp, color = Console.label)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEachIndexed { k, t ->
            MiniBtn(t, hpf.coerceIn(0, 3) == k, { vm.editChannel(id) { it.copy(hpfSlope = k) } }, "HPF $t dB/oct", Modifier.weight(1f), h = LocalUi.current.btnH)
        }
    }
    Box(Modifier.height(6.dp))
    Text("LPF dB/oct", fontSize = 9.sp, color = Console.label)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEachIndexed { k, t ->
            MiniBtn(t, lpf.coerceIn(0, 3) == k, { vm.editChannel(id) { it.copy(lpfSlope = k) } }, "LPF $t dB/oct", Modifier.weight(1f), h = LocalUi.current.btnH)
        }
    }
}

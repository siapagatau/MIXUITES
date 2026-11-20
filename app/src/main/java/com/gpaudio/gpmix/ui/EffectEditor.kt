@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gpaudio.gpmix.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gpaudio.gpmix.MixerViewModel
import com.gpaudio.gpmix.R
import com.gpaudio.gpmix.model.ChKind
import com.gpaudio.gpmix.model.EffectInfo
import com.gpaudio.gpmix.model.EqMath
import com.gpaudio.gpmix.model.ParamInfo
import com.gpaudio.gpmix.model.ParamScale
import com.gpaudio.gpmix.model.withInsert
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Editor plugin layar penuh, dengan gaya yang sama dengan mixer utama (panel gelap, knob, tombol kecil).
 * Equalizer punya grafik respons yang bisa digeser; efek lain memakai knob berkelompok yang dibuat
 * otomatis dari deskriptor parameter di engine C++.
 */
@Composable
fun EffectEditor(
    vm: MixerViewModel,
    channelId: Int,
    slot: Int,
    onSavePreset: (String) -> Unit,
    onLoadPreset: () -> Unit,
) {
    val ch = vm.project.scene.channel(channelId)
    val ins = ch?.inserts?.getOrNull(slot)
    val info = ins?.let { vm.catalog.info(it.type) }
    if (ch == null || ins == null || info == null) return
    val close = { vm.editingInsert = null }
    var presetMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Console.bg) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(info.name, fontSize = LocalUi.current.title, fontWeight = FontWeight.Bold, color = Console.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(ch.name + " · " + info.category + " · INSERT ${slot + 1}", fontSize = LocalUi.current.caption, color = Console.label, maxLines = 1)
                    }
                    MiniBtn("✕ " + stringResource(R.string.close), false, close, stringResource(R.string.close), toggle = false, h = LocalUi.current.btnH)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    MiniBtn("BYPASS", ins.bypass, { vm.editChannel(channelId) { c -> c.withInsert(slot) { it.copy(bypass = !it.bypass) } } },
                        stringResource(R.string.bypass), onColor = Console.amber, h = LocalUi.current.btnH)
                    MiniBtn("RESET", false, { vm.editChannel(channelId) { c -> c.withInsert(slot) { it.copy(params = info.defaults()) } } },
                        stringResource(R.string.reset), toggle = false, h = LocalUi.current.btnH)
                    Box {
                        MiniBtn("PRESET ▾", false, { presetMenu = true }, "Preset", toggle = false, h = LocalUi.current.btnH)
                        DropdownMenu(expanded = presetMenu, onDismissRequest = { presetMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.preset_save)) }, onClick = { presetMenu = false; vm.savePreset()?.let(onSavePreset) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.preset_load)) }, onClick = { presetMenu = false; onLoadPreset() })
                        }
                    }
                    Box {
                        MiniBtn("⋯", false, { moreMenu = true }, stringResource(R.string.insert_add), toggle = false, h = LocalUi.current.btnH)
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(text = { Text("↑") }, enabled = slot > 0, onClick = { moreMenu = false; vm.moveInsert(channelId, slot, -1) })
                            DropdownMenuItem(text = { Text("↓") }, enabled = slot < ch.inserts.size - 1, onClick = { moreMenu = false; vm.moveInsert(channelId, slot, 1) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { moreMenu = false; vm.removeInsert(channelId, slot) })
                        }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val landscape = maxWidth > maxHeight * 1.2f
                    if (info.name == "Equaliser" && ins.params.size >= EqMath.OUT_GAIN + 1) {
                        EqBody(vm, channelId, slot, info, ins.params, landscape)
                    } else {
                        GenericBody(vm, channelId, slot, info, ins.params, ins.keySource)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Efek umum
// ---------------------------------------------------------------------------------------------

@Composable
private fun GenericBody(vm: MixerViewModel, id: Int, slot: Int, info: EffectInfo, params: List<Float>, key: Int) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (info.params.any { it.name.contains("Threshold") } && info.category == "Dynamics") GainReductionBar(vm.fxGainReduction)
        if (info.usesKey) Panel("SIDECHAIN") { KeySourceField(vm, id, slot, key) }
        // Kelompokkan menurut nama grup; efek tanpa grup menjadi satu panel.
        val order = ArrayList<String>()
        info.params.forEach { if (it.group !in order) order.add(it.group) }
        for (g in order) {
            val idx = info.params.indices.filter { info.params[it].group == g }
            Panel(g.ifEmpty { "PARAMETERS" }.uppercase()) {
                ParamCluster(vm, id, slot, info, params, idx, 52.dp)
            }
        }
    }
}

/** Pilihan, tombol, lalu knob untuk parameter dengan indeks [idx]. */
@Composable
private fun ParamCluster(vm: MixerViewModel, id: Int, slot: Int, info: EffectInfo, params: List<Float>, idx: List<Int>, knob: Dp) {
    val choices = idx.filter { info.params[it].isChoice }
    val toggles = idx.filter { info.params[it].isToggle }
    val floats = idx.filter { !info.params[it].isChoice && !info.params[it].isToggle }
    choices.forEach { i -> ChoiceRow(vm, id, slot, i, info.params[i], params.getOrElse(i) { info.params[i].def }) }
    if (toggles.isNotEmpty()) {
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            toggles.forEach { i ->
                val p = info.params[i]
                val on = params.getOrElse(i) { p.def } > 0.5f
                MiniBtn(p.name, on, { setDiscrete(vm, id, slot, i, if (on) 0f else 1f) }, p.name, h = LocalUi.current.btnH)
            }
        }
    }
    if (floats.isNotEmpty()) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            floats.forEach { i -> KnobCell(vm, id, slot, i, info.params[i], params.getOrElse(i) { info.params[i].def }, knob) }
        }
    }
}

@Composable
private fun KnobCell(vm: MixerViewModel, id: Int, slot: Int, index: Int, p: ParamInfo, v: Float, size: Dp) {
    val text = ParamScale.format(p, v)
    Column(Modifier.width(74.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(p.name, fontSize = 10.sp, color = Console.label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Knob(
            ParamScale.toPos(p, v), { pos -> vm.setFxParam(id, slot, index, ParamScale.fromPos(p, pos)) }, { vm.beginGesture() },
            p.name, text, size = size, onDoubleTap = { vm.setFxParam(id, slot, index, p.def) },
            bipolar = p.min < 0f && p.max > 0f && kotlin.math.abs(p.def) < 1e-6f,
        )
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Console.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChoiceRow(vm: MixerViewModel, id: Int, slot: Int, index: Int, p: ParamInfo, v: Float) {
    val cur = v.roundToInt().coerceIn(0, (p.choices.size - 1).coerceAtLeast(0))
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(p.name, fontSize = 10.sp, color = Console.label)
        if (p.choices.size <= 4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                p.choices.forEachIndexed { k, name ->
                    MiniBtn(name, k == cur, { setDiscrete(vm, id, slot, index, k.toFloat()) }, p.name + " " + name, Modifier.weight(1f), h = LocalUi.current.btnH)
                }
            }
        } else {
            var open by remember { mutableStateOf(false) }
            Box {
                Field(p.choices.getOrElse(cur) { "" }, { open = true }, p.name, Modifier.widthIn160(), h = LocalUi.current.btnH)
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    p.choices.forEachIndexed { k, name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { open = false; setDiscrete(vm, id, slot, index, k.toFloat()) })
                    }
                }
            }
        }
    }
}

private fun Modifier.widthIn160(): Modifier = this.width(160.dp)

private fun setDiscrete(vm: MixerViewModel, id: Int, slot: Int, index: Int, value: Float) {
    vm.editChannel(id) { c -> c.withInsert(slot) { it.copy(params = it.params.toMutableList().also { l -> if (index in l.indices) l[index] = value }) } }
}

@Composable
private fun GainReductionBar(gr: Float) {
    val frac = (abs(gr) / 24f).coerceIn(0f, 1f)
    Panel("GAIN REDUCTION") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(3.dp)).background(Console.track)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Console.amber))
            }
            Text(String.format(java.util.Locale.US, "%.1f dB", abs(gr)), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Console.text)
        }
    }
}

@Composable
private fun KeySourceField(vm: MixerViewModel, id: Int, slot: Int, key: Int) {
    var open by remember { mutableStateOf(false) }
    val scene = vm.project.scene
    val cur = scene.channel(key)?.name ?: stringResource(R.string.key_self)
    Box {
        Field(stringResource(R.string.key_source) + ": " + cur, { open = true }, stringResource(R.string.key_source), Modifier.fillMaxWidth(), h = LocalUi.current.btnH)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.key_self)) }, onClick = {
                open = false; vm.editChannel(id) { c -> c.withInsert(slot) { it.copy(keySource = -1) } }
            })
            scene.channels.filter { it.id != id && it.kind != ChKind.MASTER }.forEach { o ->
                DropdownMenuItem(text = { Text(o.name) }, onClick = {
                    open = false; vm.editChannel(id) { c -> c.withInsert(slot) { it.copy(keySource = o.id) } }
                })
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Equalizer: grafik respons + node yang bisa digeser
// ---------------------------------------------------------------------------------------------

@Composable
private fun EqBody(vm: MixerViewModel, id: Int, slot: Int, info: EffectInfo, params: List<Float>, landscape: Boolean) {
    var sel by rememberSaveable { mutableStateOf(0) }
    val graph: @Composable (Modifier) -> Unit = { m -> EqGraph(vm, id, slot, params, sel, { sel = it }, m) }
    val controls: @Composable () -> Unit = {
        EqBandChips(params, sel) { sel = it }
        Panel("BAND ${sel + 1}", titleColor = ChannelColors[sel]) { EqBandControls(vm, id, slot, info, params, sel) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Panel("HIGH-PASS", Modifier.weight(1f)) { EqPassControls(vm, id, slot, info, params, 0) }
            Panel("LOW-PASS", Modifier.weight(1f)) { EqPassControls(vm, id, slot, info, params, 3) }
        }
        Panel("OUTPUT") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                KnobCell(vm, id, slot, EqMath.OUT_GAIN, info.params[EqMath.OUT_GAIN], params[EqMath.OUT_GAIN], 44.dp)
            }
        }
    }
    if (landscape) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            graph(Modifier.weight(1.25f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) { controls() }
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            graph(Modifier.fillMaxWidth().height(210.dp))
            controls()
        }
    }
}

@Composable
private fun EqBandChips(params: List<Float>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (b in 0 until EqMath.BANDS) {
            val on = params.getOrElse(EqMath.bandIndex(b, 0)) { 1f } > 0.5f
            MiniBtn(
                (b + 1).toString() + if (on) "" else " ○", sel == b, { onSel(b) }, "Band ${b + 1}",
                Modifier.weight(1f), onColor = ChannelColors[b], h = LocalUi.current.btnH,
            )
        }
    }
}

@Composable
private fun EqBandControls(vm: MixerViewModel, id: Int, slot: Int, info: EffectInfo, params: List<Float>, b: Int) {
    val iOn = EqMath.bandIndex(b, 0)
    val iShape = EqMath.bandIndex(b, 4)
    val on = params.getOrElse(iOn) { 1f } > 0.5f
    val shape = params.getOrElse(iShape) { 0f }.roundToInt().coerceIn(0, 2)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        MiniBtn("ON", on, { setDiscrete(vm, id, slot, iOn, if (on) 0f else 1f) }, "Band ${b + 1} on", Modifier.width(48.dp), onColor = Console.green, h = LocalUi.current.btnH)
        listOf("Bell", "Low shelf", "High shelf").forEachIndexed { k, n ->
            MiniBtn(n, shape == k, { setDiscrete(vm, id, slot, iShape, k.toFloat()) }, n, Modifier.weight(1f), h = LocalUi.current.btnH)
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        for (f in 1..3) {
            val i = EqMath.bandIndex(b, f)
            KnobCell(vm, id, slot, i, info.params[i], params[i], 44.dp)
        }
    }
}

@Composable
private fun EqPassControls(vm: MixerViewModel, id: Int, slot: Int, info: EffectInfo, params: List<Float>, base: Int) {
    val on = params.getOrElse(base) { 0f } > 0.5f
    val slope = params.getOrElse(base + 2) { 0f }.roundToInt().coerceIn(0, 3)
    MiniBtn("ON", on, { setDiscrete(vm, id, slot, base, if (on) 0f else 1f) }, info.params[base].group, Modifier.fillMaxWidth(), onColor = Console.green, h = LocalUi.current.btnH)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf("12", "24", "36", "48").forEachIndexed { k, n ->
            MiniBtn(n, slope == k, { setDiscrete(vm, id, slot, base + 2, k.toFloat()) }, "$n dB/oct", Modifier.weight(1f), h = LocalUi.current.btnH)
        }
    }
    Spacer4()
    KnobCell(vm, id, slot, base + 1, info.params[base + 1], params[base + 1], 44.dp)
}

@Composable
private fun Spacer4() { Box(Modifier.height(4.dp)) }

@Composable
private fun EqGraph(
    vm: MixerViewModel, id: Int, slot: Int, params: List<Float>, sel: Int,
    onSel: (Int) -> Unit, modifier: Modifier,
) {
    val cur by rememberUpdatedState(params)
    val curSel by rememberUpdatedState(sel)
    val range = EqMath.DB_RANGE
    val freqMarks = listOf(20f, 50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f, 20000f)
    val freqLabels = listOf("20", "50", "100", "200", "500", "1k", "2k", "5k", "10k", "20k")
    val dbMarks = listOf(18, 12, 6, 0, -6, -12, -18)
    BoxWithConstraints(modifier.clip(RoundedCornerShape(8.dp)).background(Console.panel).padding(6.dp)) {
        val labelH = 14.dp
        val gW = maxWidth - 26.dp
        val gH = maxHeight - labelH
        Box(Modifier.offset(x = 26.dp).width(gW).height(gH)) {
            Canvas(
                Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Console.track)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { o ->
                            val p = cur
                            var best = curSel
                            var bd = Float.MAX_VALUE
                            for (b in 0 until EqMath.BANDS) {
                                val nx = EqMath.freqToX(p.getOrElse(EqMath.bandIndex(b, 1)) { 1000f }) * size.width
                                val ny = (0.5f - p.getOrElse(EqMath.bandIndex(b, 2)) { 0f } / (2f * range)) * size.height
                                val d = (nx - o.x) * (nx - o.x) + (ny - o.y) * (ny - o.y)
                                if (d < bd) { bd = d; best = b }
                            }
                            onSel(best)
                        })
                    }
                    .pointerInput(Unit) {
                        var drag = -1
                        detectDragGestures(
                            onDragStart = { o ->
                                val p = cur
                                var best = -1
                                var bd = (36.dp.toPx()) * (36.dp.toPx())
                                for (b in 0 until EqMath.BANDS) {
                                    val nx = EqMath.freqToX(p.getOrElse(EqMath.bandIndex(b, 1)) { 1000f }) * size.width
                                    val ny = (0.5f - p.getOrElse(EqMath.bandIndex(b, 2)) { 0f } / (2f * range)) * size.height
                                    val d = (nx - o.x) * (nx - o.x) + (ny - o.y) * (ny - o.y)
                                    if (d < bd) { bd = d; best = b }
                                }
                                drag = best
                                if (best >= 0) {
                                    onSel(best)
                                    vm.beginGesture()
                                    if (p.getOrElse(EqMath.bandIndex(best, 0)) { 1f } < 0.5f) setDiscrete(vm, id, slot, EqMath.bandIndex(best, 0), 1f)
                                }
                            },
                            onDragEnd = { drag = -1 },
                            onDragCancel = { drag = -1 },
                        ) { change, _ ->
                            if (drag >= 0) {
                                change.consume()
                                val f = EqMath.xToFreq(change.position.x / size.width)
                                val g = ((0.5f - change.position.y / size.height) * 2f * range).coerceIn(-range, range)
                                vm.setFxParam(id, slot, EqMath.bandIndex(drag, 1), f)
                                vm.setFxParam(id, slot, EqMath.bandIndex(drag, 2), (g * 2f).roundToInt() / 2f)
                            }
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                for (f in freqMarks) {
                    val x = EqMath.freqToX(f) * w
                    drawLine(Color(0x22FFFFFF), Offset(x, 0f), Offset(x, h), 1f)
                }
                for (d in dbMarks) {
                    val y = (0.5f - d / (2f * range)) * h
                    drawLine(if (d == 0) Color(0x55FFFFFF) else Color(0x22FFFFFF), Offset(0f, y), Offset(w, y), if (d == 0) 1.5f else 1f)
                }
                val pts = 160
                val stroke = Path()
                val fill = Path()
                val mid = h / 2f
                fill.moveTo(0f, mid)
                for (i in 0 until pts) {
                    val x = i / (pts - 1f)
                    val db = EqMath.totalDb(params, EqMath.xToFreq(x).toDouble()).toFloat().coerceIn(-range * 1.2f, range * 1.2f)
                    val y = (mid - db / (2f * range) * h).coerceIn(0f, h)
                    if (i == 0) stroke.moveTo(x * w, y) else stroke.lineTo(x * w, y)
                    fill.lineTo(x * w, y)
                }
                fill.lineTo(w, mid)
                fill.close()
                drawPath(fill, Console.blue.copy(alpha = 0.18f))
                drawPath(stroke, Console.blue, style = Stroke(2.5f))
                for (b in 0 until EqMath.BANDS) {
                    val on = params.getOrElse(EqMath.bandIndex(b, 0)) { 1f } > 0.5f
                    val nx = EqMath.freqToX(params.getOrElse(EqMath.bandIndex(b, 1)) { 1000f }) * w
                    val ny = (0.5f - params.getOrElse(EqMath.bandIndex(b, 2)) { 0f } / (2f * range)) * h
                    val col = ChannelColors[b]
                    val r = 11.dp.toPx()
                    drawCircle(if (on) col else col.copy(alpha = 0.25f), r, Offset(nx, ny))
                    if (b == sel) drawCircle(Color.White, r + 2.dp.toPx(), Offset(nx, ny), style = Stroke(2.dp.toPx()))
                }
            }
            // nomor band di atas node
            for (b in 0 until EqMath.BANDS) {
                val nx = EqMath.freqToX(params.getOrElse(EqMath.bandIndex(b, 1)) { 1000f }) * gW.value
                val ny = (0.5f - params.getOrElse(EqMath.bandIndex(b, 2)) { 0f } / (2f * range)) * gH.value
                Text(
                    (b + 1).toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                    modifier = Modifier.offset(x = (nx - 3.5f).dp, y = (ny - 7f).dp),
                )
            }
        }
        // label dB di kiri
        for (d in dbMarks) {
            if (d == 18 || d == -18 || d == 6 || d == -6) continue
            val y = (0.5f - d / (2f * range)) * gH.value
            Text((if (d > 0) "+$d" else "$d"), fontSize = 9.sp, color = Console.label, modifier = Modifier.offset(x = 0.dp, y = (y - 6f).dp))
        }
        // label frekuensi di bawah
        for (k in freqMarks.indices) {
            val x = EqMath.freqToX(freqMarks[k]) * gW.value + 26f
            Text(freqLabels[k], fontSize = 9.sp, color = Console.label, modifier = Modifier.offset(x = (x - 9f).dp, y = gH + 1.dp))
        }
    }
}

package com.gpaudio.gpmix.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpaudio.gpmix.R
import com.gpaudio.gpmix.audio.NativeEngine as N
import com.gpaudio.gpmix.MixerViewModel
import com.gpaudio.gpmix.model.ChKind
import com.gpaudio.gpmix.model.Channel
import com.gpaudio.gpmix.model.FaderMap
import com.gpaudio.gpmix.model.Ops
import com.gpaudio.gpmix.model.Out
import com.gpaudio.gpmix.model.Presets
import com.gpaudio.gpmix.model.withInsert
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Warna panel konsol, mengikuti render strip di manual GP Mix. Isinya dibaca dari [palette] (state), jadi
 * berganti sendiri saat tema diganti (lihat GpTheme). "blue" = warna aksen tema.
 */
object Console {
    var palette: ConsolePalette by mutableStateOf(DefaultPalette)
    val bg: Color get() = palette.bg
    val bar: Color get() = palette.bar
    val body: Color get() = palette.body
    val panel: Color get() = palette.panel
    val panelHi: Color get() = palette.panelHi
    val field: Color get() = palette.field
    val fieldHi: Color get() = palette.fieldHi
    val fieldBorder: Color get() = palette.fieldBorder
    val line: Color get() = palette.line
    val label: Color get() = palette.label
    val text: Color get() = palette.text
    val blue: Color get() = palette.blue
    val track: Color get() = palette.track
    val knob: Color get() = palette.knob
    val knobHi: Color get() = palette.knobHi
    val nameBar: Color get() = palette.nameBar
    val red: Color get() = palette.red
    val green: Color get() = palette.green
    val amber: Color get() = palette.amber
}

val StripW = 108.dp

/**
 * Tinggi tiap bagian strip (dp) pada ukuran rancangan. Strip lengkap seperti di manual selalu digambar
 * utuh pada ukuran ini, lalu seluruh mixer diperkecil/diperbesar agar pas di layar (lihat MixerScreen).
 * FADER mengisi sisa tinggi.
 */
object StripH {
    val PRESET = 68.dp
    val INPUT = 68.dp
    val FILTER = 70.dp
    val INSERT = 92.dp
    val SENDS = 116.dp
    val OUTPUT = 42.dp
    val PAN = 64.dp
    val LATENCY = 32.dp
    val MSS = 34.dp
    val NAME = 28.dp
    val FADER_MIN = 110.dp
    val GAP = 2.dp
}

fun sectionHeight(key: String): Dp = when (key) {
    "preset" -> StripH.PRESET
    "input" -> StripH.INPUT
    "filter" -> StripH.FILTER
    "insert" -> StripH.INSERT
    "sends" -> StripH.SENDS
    "output" -> StripH.OUTPUT
    "pan" -> StripH.PAN
    else -> StripH.LATENCY
}

private val OptionalSections = listOf("preset", "input", "filter", "insert", "sends", "output", "pan", "latency")

/** Tinggi rancangan strip minimum (FADER pada tinggi minimum) untuk bagian yang tidak disembunyikan. */
fun designHeight(hidden: Set<String>): Dp {
    var h = StripH.FADER_MIN + StripH.MSS + StripH.NAME
    var items = 3
    for (k in OptionalSections) {
        if (k in hidden) continue
        h += sectionHeight(k)
        items++
    }
    return h + StripH.GAP * (items - 1) + 7.dp
}

@Composable
fun Panel(
    title: String?,
    modifier: Modifier = Modifier,
    titleColor: Color = Console.label,
    pad: Dp = 3.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(7.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Console.panelHi, Console.panel)))
            .border(1.dp, Console.line, shape).padding(pad),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (title != null) {
            Text(
                title, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = titleColor,
                letterSpacing = 1.2.sp, modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        content()
    }
}

/** Kotak bergaya dropdown (teks + panah), tampil "masuk" (inset) supaya beda dari tombol yang timbul. */
@Composable
fun Field(
    text: String,
    onClick: () -> Unit,
    desc: String,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
    arrow: Boolean = true,
    h: Dp = 22.dp,
) {
    val shape = RoundedCornerShape(6.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val fsv = (h.value * 0.46f).coerceIn(10f, 13f)
    Row(
        modifier.height(h).clip(shape)
            .background(Brush.verticalGradient(listOf(Console.fieldBorder, Console.field)))
            .border(1.dp, if (pressed) Console.blue else Console.line, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick, role = Role.Button)
            .semantics { contentDescription = desc }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text, Modifier.weight(1f), fontSize = fsv.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (dim) Console.label else Console.text,
        )
        if (arrow) Text("▾", fontSize = (fsv - 1f).sp, color = Console.label)
    }
}

/**
 * Tombol kecil. Huruf mengikuti tinggi tombol ([h]) sehingga semua tombol sebanding. Tombol mati/hidup
 * ([on]) memakai warna [onColor] sebagai isi tembus + garis tepi; yang timbul (off) abu dengan kilau atas.
 */
@Composable
fun MiniBtn(
    text: String,
    on: Boolean,
    onClick: () -> Unit,
    desc: String,
    modifier: Modifier = Modifier,
    onColor: Color = Console.blue,
    enabled: Boolean = true,
    toggle: Boolean = true,
    h: Dp = 22.dp,
    hPad: Dp = 3.dp,
) {
    val shape = RoundedCornerShape((h.value * 0.26f).coerceIn(5f, 8f).dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val bg = when {
        on -> Brush.verticalGradient(listOf(onColor.copy(alpha = 0.38f), onColor.copy(alpha = 0.18f)))
        pressed -> Brush.verticalGradient(listOf(Console.fieldBorder, Console.field))
        else -> Brush.verticalGradient(listOf(Console.fieldHi, Console.field))
    }
    val borderColor = if (on) onColor else if (pressed) Console.blue else Console.line
    val fsv = (h.value * 0.44f).coerceIn(9.5f, 13f)
    Box(
        modifier.height(h).alpha(if (enabled) 1f else 0.45f).clip(shape).background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = src, indication = null, enabled = enabled, onClick = onClick,
                role = if (toggle) Role.Switch else Role.Button,
            )
            .semantics { contentDescription = desc; if (toggle) selected = on }
            .padding(horizontal = hPad),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontSize = fsv.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            color = if (on) lerp(onColor, Color.White, 0.35f) else Console.text,
        )
    }
}

@Composable
fun RoundBtn(
    text: String,
    on: Boolean,
    onClick: () -> Unit,
    desc: String,
    onColor: Color,
    modifier: Modifier = Modifier,
    fs: TextUnit = 11.sp,
    dia: Dp = 26.dp,
) {
    val bg = if (on) Brush.verticalGradient(listOf(lerp(onColor, Color.White, 0.25f), onColor))
    else Brush.verticalGradient(listOf(Console.fieldHi, Console.field))
    Box(
        modifier.size(dia).clip(CircleShape).background(bg)
            .border(1.dp, if (on) onColor else Console.line, CircleShape)
            .clickable(onClick = onClick, role = Role.Switch)
            .semantics { contentDescription = desc; selected = on },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = fs, fontWeight = FontWeight.Bold, color = if (on) Color(0xFF101214) else Console.text)
    }
}

/**
 * Knob putar sungguhan: putar jari mengelilingi knob (gerakan melingkar) untuk mengubah nilai; satu putaran
 * penuh jari = 270 derajat rentang knob. Busur hijau di sekeliling knob menunjukkan sudah seberapa jauh
 * diputar ([bipolar] = busur dimulai dari tengah, untuk PAN dan trim). Klik dua kali memanggil [onDoubleTap].
 * [pos] 0..1. Aksesibel lewat semantics progress.
 */
@Composable
fun Knob(
    pos: Float,
    onChange: (Float) -> Unit,
    onStart: () -> Unit,
    desc: String,
    valueText: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    onDoubleTap: (() -> Unit)? = null,
    active: Boolean = true,
    bipolar: Boolean = false,
) {
    val cb by rememberUpdatedState(onChange)
    val st by rememberUpdatedState(onStart)
    val dt by rememberUpdatedState(onDoubleTap)
    val cur by rememberUpdatedState(pos)
    val p = pos.coerceIn(0f, 1f)
    Canvas(
        modifier.size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = desc
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f)
                setProgress { v -> st(); cb(v.coerceIn(0f, 1f)); true }
            }
            .pointerInput(Unit) {
                var last = 0f
                val halfW = this.size.width / 2f   // "this.size": PointerInputScope, bukan parameter size (Dp)
                val halfH = this.size.height / 2f
                detectDragGestures(
                    onDragStart = { o -> st(); last = knobAngle(o, halfW, halfH) },
                    onDrag = { change, _ ->
                        change.consume()
                        val a = knobAngle(change.position, halfW, halfH)
                        var d = a - last
                        if (d > 180f) d -= 360f
                        if (d < -180f) d += 360f
                        last = a
                        cb((cur + d / 270f).coerceIn(0f, 1f))
                    },
                )
            }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { dt?.invoke() }) },
    ) {
        val r = minOf(this.size.width, this.size.height) / 2f
        val c = center
        drawCircle(Console.track, r, c)
        // busur nilai: jalur redup untuk seluruh rentang, lalu busur hijau sampai posisi sekarang
        val ar = r * 0.86f
        val aw = maxOf(r * 0.16f, 1.5.dp.toPx())
        val tl = Offset(c.x - ar, c.y - ar)
        val asz = Size(2f * ar, 2f * ar)
        drawArc(Color(0xFF3A3A40), -225f, 270f, false, tl, asz, style = Stroke(aw, cap = StrokeCap.Round))
        if (active) {
            if (bipolar) drawArc(Console.green, -90f, 270f * (p - 0.5f), false, tl, asz, style = Stroke(aw, cap = StrokeCap.Round))
            else if (p > 0.004f) drawArc(Console.green, -225f, 270f * p, false, tl, asz, style = Stroke(aw, cap = StrokeCap.Round))
        }
        val k = r * 0.68f
        drawCircle(
            Brush.radialGradient(listOf(Console.knobHi, Console.knob), center = Offset(c.x - k * 0.25f, c.y - k * 0.3f), radius = k * 1.5f),
            k, c, alpha = if (active) 1f else 0.6f,
        )
        drawCircle(Color(0x33FFFFFF), k, c, style = Stroke(1.dp.toPx()))
        val a = Math.toRadians(-135.0 + 270.0 * p.toDouble())
        val dx = sin(a).toFloat()
        val dy = -cos(a).toFloat()
        drawLine(
            if (active) Console.text else Console.label,
            Offset(c.x + dx * k * 0.2f, c.y + dy * k * 0.2f),
            Offset(c.x + dx * k * 0.9f, c.y + dy * k * 0.9f),
            2.dp.toPx(), cap = StrokeCap.Round,
        )
    }
}

/** Sudut (derajat, searah jarum jam dari atas) titik [o] terhadap pusat knob. */
private fun knobAngle(o: Offset, cx: Float, cy: Float): Float =
    Math.toDegrees(atan2((o.x - cx).toDouble(), (cy - o.y).toDouble())).toFloat()

private val ScaleTicks = listOf(6f to "6", 3f to "3", 0f to "0", -3f to "-3", -6f to "-6", -10f to "-10",
    -15f to "-15", -20f to "-20", -30f to "-30", -40f to "-40", -60f to "inf")
private val FaderMargin = 14.dp

/** Skala dB di samping fader; posisi tandanya memakai pemetaan yang sama dengan fader dan meter. */
@Composable
fun FaderScale(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.width(24.dp).fillMaxHeight()) {
        val h = maxHeight
        for ((db, label) in ScaleTicks) {
            val pos = FaderMap.dbToPos(db)
            val y = FaderMargin.value + (h.value - 2f * FaderMargin.value) * (1f - pos)
            Text(
                label, fontSize = 8.sp, color = Console.label, textAlign = TextAlign.End,
                modifier = Modifier.align(Alignment.TopEnd).offset(y = (y - 5f).dp).padding(end = 2.dp),
            )
        }
    }
}

@Composable
fun ConsoleFader(
    db: Float,
    label: String,
    onGestureStart: () -> Unit,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cb by rememberUpdatedState(onChange)
    val st by rememberUpdatedState(onGestureStart)
    val pos = FaderMap.dbToPos(db)
    Canvas(
        modifier
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = FaderMap.format(db) + " dB"
                progressBarRangeInfo = ProgressBarRangeInfo(pos, 0f..1f)
                setProgress { v -> st(); cb(FaderMap.posToDb(v.coerceIn(0f, 1f))); true }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { st() },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val m = FaderMargin.toPx()
                        val span = (size.height.toFloat() - 2f * m).coerceAtLeast(1f)
                        val p = 1f - ((change.position.y - m) / span).coerceIn(0f, 1f)
                        cb(FaderMap.posToDb(p))
                    },
                )
            }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { st(); cb(0f) }) },
    ) {
        val m = FaderMargin.toPx()
        val trackW = 6.dp.toPx()
        val cx = size.width / 2f
        drawRoundRect(Console.track, Offset(cx - trackW / 2f, m), Size(trackW, size.height - 2f * m), CornerRadius(trackW / 2f))
        val y = m + (size.height - 2f * m) * (1f - pos)
        val fillH = size.height - m - y
        if (fillH > 0f) {
            drawRoundRect(Console.blue.copy(alpha = 0.55f), Offset(cx - trackW / 2f, y), Size(trackW, fillH), CornerRadius(trackW / 2f))
        }
        val tw = minOf(24.dp.toPx(), size.width * 0.96f)
        val th = 28.dp.toPx()
        drawRoundRect(
            Brush.verticalGradient(listOf(Console.knobHi, Console.knob), startY = y - th / 2f, endY = y + th / 2f),
            Offset(cx - tw / 2f, y - th / 2f), Size(tw, th), CornerRadius(4.dp.toPx()),
        )
        for (k in listOf(-10f, -6f, 6f, 10f)) {
            drawLine(Color(0xFF26262A), Offset(cx - tw / 2f + 2.dp.toPx(), y + k.dp.toPx()), Offset(cx + tw / 2f - 2.dp.toPx(), y + k.dp.toPx()), 1.5.dp.toPx())
        }
        drawLine(Color.White, Offset(cx - tw / 2f + 1.dp.toPx(), y), Offset(cx + tw / 2f - 1.dp.toPx(), y), 2.dp.toPx())
    }
}

private fun meterZone(db: Float): Color = when {
    db > -3f -> Console.red
    db > -12f -> Console.amber
    else -> Console.green
}

/** Posisi meter (0..1) memakai pemetaan fader yang sama, supaya tinggi meter sejajar dengan skala dB. */
private fun meterPosOf(lin: Float): Float = if (lin <= 0.0007f) 0f else FaderMap.dbToPos(FaderMap.linToDb(lin))

/**
 * Meter LED satu kolom (level tertinggi dari kiri/kanan). Tiap blok punya warna tetap menurut zona (hijau,
 * kuning, merah di bagian atas). Blok menyala penuh sampai level saat ini; di atasnya, blok redup yang
 * menyambung menunjukkan ekor yang turun pelan menyusul level. Nilai dibaca di fase gambar.
 */
@Composable
fun ConsoleMeter(level: () -> Float, trail: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(2.dp)).background(Console.track)) {
        val h = size.height
        if (h < 4f) return@Canvas
        val blockH = 2.5.dp.toPx()
        val gap = 1.dp.toPx()
        val step = blockH + gap
        val n = (h / step).toInt().coerceAtLeast(1)
        val gx = 2.dp.toPx()
        val bw = size.width - 2f * gx
        val lv = meterPosOf(level())
        val tr = maxOf(lv, meterPosOf(trail()))
        for (k in 0 until n) {
            val posC = (k + 0.5f) * step / h
            val zone = meterZone(FaderMap.posToDb(posC.coerceIn(0f, 1f)))
            val y = h - (k + 1) * step + gap
            val a = if (lv >= posC) 1f else if (tr >= posC) 0.4f else 0.13f
            drawRect(zone.copy(alpha = a), Offset(gx, y), Size(bw, blockH))
        }
    }
}

@Composable
fun PeakText(vm: MixerViewModel, ch: Int) {
    val lin = vm.meters.getOrElse(3 * N.MAX_CHANNELS + ch) { 0f }
    val clip = ch in vm.clipped
    val db = FaderMap.linToDb(lin)
    val t = if (lin < 0.001f) "-inf" else String.format(Locale.US, "%.1f", db)
    Text(
        t, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        color = if (clip) Console.red else if (lin < 0.001f) Console.label else meterZone(db), maxLines = 1, softWrap = false,
        modifier = Modifier.wrapContentWidth(unbounded = true),
    )
}

@Composable
fun LatencyText(vm: MixerViewModel, ch: Int) {
    val ms = vm.meters.getOrElse(2 * N.MAX_CHANNELS + ch) { 0f }
    Text(String.format(Locale.US, "%.1f ms", ms), fontSize = 9.sp, color = Console.label)
}

@Composable
private fun DbDialog(initial: Float, title: String, onSet: (Float) -> Unit, onClose: () -> Unit) {
    var t by remember { mutableStateOf(String.format(Locale.US, "%.1f", initial)) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                t, { t = it.take(8) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            GpTextButton(onClick = {
                val v = t.trim().replace(',', '.')
                val db = if (v.contains("inf", ignoreCase = true)) -60f else v.toFloatOrNull()
                if (db != null) onSet(db.coerceIn(-60f, 6f))
                onClose()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { GpTextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---------------------------------------------------------------------------------------------
// Strip konsol
// ---------------------------------------------------------------------------------------------

/** Bagian yang bisa disembunyikan lewat View ▾ (kunci disimpan di Prefs). */
val StripSectionKeys = listOf("preset", "input", "filter", "insert", "sends", "output", "pan", "latency")

/**
 * Satu strip channel, lengkap seperti di manual: PRESET, INPUT, FILTER, INSERT, SENDS, OUTPUT, PAN, FADER,
 * M/S/SAFE, LATENCY dan nama. Digambar pada ukuran rancangan [wd] x [hd]; MixerScreen memperkecil seluruh
 * area strip agar pas di layar. Bagian yang disembunyikan lewat View dihilangkan.
 */
@Composable
fun ConsoleStrip(vm: MixerViewModel, c: Channel, wd: Dp, hd: Dp, modifier: Modifier = Modifier) {
    val hidden = vm.hiddenSections
    val master = c.kind == ChKind.MASTER
    val audio = c.kind == ChKind.AUDIO
    val selected = vm.selectedChannel == c.id
    val accent = ChannelColors[c.color.coerceIn(0, ChannelColors.size - 1)]
    val iw = wd - 13.dp
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier.width(wd).height(hd).clip(shape).background(Console.body)
            .border(1.5.dp, if (selected) Console.blue else Console.line, shape).padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(StripH.GAP),
    ) {
        if ("preset" !in hidden) {
            if (audio) Panel("PRESET", Modifier.height(StripH.PRESET)) { PresetBody(vm, c, iw >= 70.dp) }
            else Spacer(Modifier.height(StripH.PRESET))
        }
        if ("input" !in hidden) {
            if (master) Spacer(Modifier.height(StripH.INPUT))
            else Panel("INPUT", Modifier.height(StripH.INPUT)) { InputBody(vm, c) }
        }
        if ("filter" !in hidden) {
            if (master) Spacer(Modifier.height(StripH.FILTER))
            else Panel("FILTER", Modifier.height(StripH.FILTER)) { FilterBody(vm, c) }
        }
        if ("insert" !in hidden) Panel("INSERT", Modifier.height(StripH.INSERT)) { InsertBody(vm, c) }
        if ("sends" !in hidden) {
            if (master) Spacer(Modifier.height(StripH.SENDS))
            else Panel("SENDS", Modifier.height(StripH.SENDS), Console.blue) { SendsBody(vm, c) }
        }
        if ("output" !in hidden) {
            if (master) Spacer(Modifier.height(StripH.OUTPUT))
            else Panel("OUTPUT", Modifier.height(StripH.OUTPUT)) { OutputBody(vm, c) }
        }
        if ("pan" !in hidden) {
            if (master) Spacer(Modifier.height(StripH.PAN))
            else Panel("PAN", Modifier.height(StripH.PAN)) { PanBody(vm, c, iw) }
        }
        Panel("FADER", Modifier.weight(1f)) { FaderBody(vm, c, iw) }
        Panel(null, Modifier.height(StripH.MSS)) { MssBody(vm, c, audio, iw) }
        if ("latency" !in hidden) Panel("LATENCY", Modifier.height(StripH.LATENCY)) { LatencyText(vm, c.id) }
        val openDesc = stringResource(R.string.cd_open_channel, c.name, kindLabel(c.kind))
        Box(
            Modifier.fillMaxWidth().height(StripH.NAME).clip(RoundedCornerShape(5.dp))
                .background(Brush.verticalGradient(listOf(lerp(Console.nameBar, accent, 0.55f), lerp(Console.nameBar, accent, 0.25f))))
                .clickable { vm.selectedChannel = c.id }
                .semantics { contentDescription = openDesc },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                c.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
    }
}

@Composable
fun kindLabel(k: ChKind): String = when (k) {
    ChKind.AUDIO -> stringResource(R.string.kind_input)
    ChKind.AUX -> stringResource(R.string.kind_aux)
    ChKind.GROUP -> stringResource(R.string.kind_group)
    ChKind.MASTER -> stringResource(R.string.kind_master)
}

@Composable
fun PresetBody(vm: MixerViewModel, c: Channel, wide: Boolean) {
    val name = vm.presetNames[c.id] ?: "Default"
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (wide) MiniBtn("<", false, { vm.stepPreset(c.id, -1) }, stringResource(R.string.preset_prev), Modifier.width(18.dp), toggle = false)
        Box(Modifier.weight(1f)) {
            Field(name, { open = true }, "PRESET $name", Modifier.fillMaxWidth())
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                Presets.channelPresetNames.forEach { n ->
                    DropdownMenuItem(text = { Text(n) }, onClick = { open = false; vm.applyPresetNamed(c.id, n) })
                }
            }
        }
        if (wide) MiniBtn(">", false, { vm.stepPreset(c.id, 1) }, stringResource(R.string.preset_next), Modifier.width(18.dp), toggle = false)
    }
    Spacer(Modifier.height(2.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        MiniBtn("C", false, { vm.copyChannel(c.id) }, stringResource(R.string.preset_copy), Modifier.weight(1f), toggle = false)
        MiniBtn("P", false, { vm.pasteChannel(c.id) }, stringResource(R.string.preset_paste), Modifier.weight(1f), enabled = vm.hasClipboard, toggle = false)
        MiniBtn("R", false, { vm.resetChannel(c.id) }, stringResource(R.string.preset_reset), Modifier.weight(1f), toggle = false)
    }
}

@Composable
fun InputBody(vm: MixerViewModel, c: Channel) {
    if (c.kind != ChKind.AUDIO) {
        val t = if (c.kind == ChKind.AUX) "Bus ${c.inIndex + 1}" else if (c.kind == ChKind.GROUP) "Group ${c.groupIndex + 1}" else ""
        Text(t, fontSize = 11.sp, color = Console.text, modifier = Modifier.padding(top = 10.dp))
        return
    }
    var open by remember { mutableStateOf(false) }
    val label = when (c.inKind) {
        N.IN_HW -> if (c.stereo) "In ${c.inIndex + 1}–${c.inIndex + 2}" else "In ${c.inIndex + 1}"
        N.IN_PADS -> stringResource(R.string.src_pads)
        N.IN_PLAYER -> stringResource(R.string.src_player)
        N.IN_APP -> stringResource(R.string.src_app)
        else -> stringResource(R.string.src_none)
    }
    Box(Modifier.fillMaxWidth()) {
        Field(label, { open = true }, stringResource(R.string.input_source) + ": " + label, Modifier.fillMaxWidth())
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val inCh = (if (vm.info.inputChannels > 0) vm.info.inputChannels else vm.project.settings.inputChannels).coerceIn(1, N.MAX_HW_IN)
            for (k in 0 until N.MAX_HW_IN) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.src_hw, k + 1) + if (k >= inCh) " ·" else "") },
                    onClick = { open = false; vm.editChannel(c.id) { it.copy(inKind = N.IN_HW, inIndex = k) } },
                )
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.src_pads)) }, onClick = { open = false; vm.editChannel(c.id) { it.copy(inKind = N.IN_PADS) } })
            DropdownMenuItem(text = { Text(stringResource(R.string.src_player)) }, onClick = { open = false; vm.editChannel(c.id) { it.copy(inKind = N.IN_PLAYER) } })
            DropdownMenuItem(text = { Text(stringResource(R.string.src_app)) }, onClick = { open = false; vm.editChannel(c.id) { it.copy(inKind = N.IN_APP) } })
            DropdownMenuItem(text = { Text(stringResource(R.string.src_none)) }, onClick = { open = false; vm.editChannel(c.id) { it.copy(inKind = N.IN_NONE) } })
        }
    }
    Spacer(Modifier.height(2.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        MiniBtn("INV", c.polarity, { vm.editChannel(c.id) { it.copy(polarity = !it.polarity) } }, stringResource(R.string.polarity), Modifier.weight(1f))
        MiniBtn(if (c.stereo) "S" else "M", c.stereo, { vm.editChannel(c.id) { it.copy(stereo = !it.stereo) } }, stringResource(R.string.stereo), Modifier.weight(0.8f))
        Knob(
            (c.trimDb + 24f) / 48f, { p -> vm.changeChannel(c.id) { it.copy(trimDb = p * 48f - 24f) } }, { vm.beginGesture() },
            stringResource(R.string.trim), FaderMap.format(c.trimDb) + " dB", size = 20.dp,
            onDoubleTap = { vm.editChannel(c.id) { it.copy(trimDb = 0f) } }, bipolar = true,
        )
    }
}

@Composable
private fun FilterKnob(
    label: String, on: Boolean, hz: Float, min: Float, max: Float, desc: String,
    onSet: (Float) -> Unit, onToggle: () -> Unit, onStart: () -> Unit,
) {
    val span = ln(max / min)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Console.text)
        Knob(
            (ln(hz.coerceIn(min, max) / min) / span).coerceIn(0f, 1f), { p -> onSet(min * exp(p * span)) }, onStart,
            desc, if (on) hz.roundToInt().toString() + " Hz" else "off", size = 28.dp, onDoubleTap = onToggle, active = on,
        )
        Text(if (on) hz.roundToInt().toString() else "off", fontSize = 8.sp, color = Console.label)
    }
}

@Composable
fun FilterBody(vm: MixerViewModel, c: Channel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        FilterKnob("HPF", c.hpfOn, c.hpfHz, 20f, 1000f, "HPF " + stringResource(R.string.hpf),
            { v -> vm.changeChannel(c.id) { it.copy(hpfOn = true, hpfHz = v) } },
            { vm.editChannel(c.id) { it.copy(hpfOn = !it.hpfOn) } }, { vm.beginGesture() })
        FilterKnob("LPF", c.lpfOn, c.lpfHz, 1000f, 20000f, "LPF " + stringResource(R.string.lpf),
            { v -> vm.changeChannel(c.id) { it.copy(lpfOn = true, lpfHz = v) } },
            { vm.editChannel(c.id) { it.copy(lpfOn = !it.lpfOn) } }, { vm.beginGesture() })
    }
}

@Composable
fun ColumnScope.InsertBody(vm: MixerViewModel, c: Channel) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        c.inserts.forEachIndexed { i, ins ->
            val nm = vm.catalog.info(ins.type)?.name ?: "?"
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Field(nm, { vm.editingInsert = c.id to i }, "INSERT ${i + 1}: $nm", Modifier.weight(1f), dim = ins.bypass, arrow = false)
                MiniBtn("B", ins.bypass, { vm.editChannel(c.id) { ch -> ch.withInsert(i) { it.copy(bypass = !it.bypass) } } },
                    stringResource(R.string.bypass) + " $nm", Modifier.width(20.dp), Console.amber)
            }
        }
        if (c.inserts.size < vm.limits.inserts) {
            var open by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth()) {
                Field(stringResource(R.string.empty_slot), { open = true }, stringResource(R.string.insert_add), Modifier.fillMaxWidth(), dim = true)
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    for (cat in vm.catalog.categories) {
                        for (e in vm.catalog.effects.filter { it.category == cat }) {
                            DropdownMenuItem(text = { Text(e.name + " · " + cat) }, onClick = { open = false; vm.addInsert(c.id, e.type) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.SendsBody(vm: MixerViewModel, c: Channel) {
    val rows = minOf(vm.limits.sends, maxOf(4, c.sends.size + 1))
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0 until rows) {
            val s = c.sends.getOrNull(i)
            if (s != null) SendRow(vm, c, i) else EmptySendRow(vm, c)
        }
    }
}

@Composable
private fun SendRow(vm: MixerViewModel, c: Channel, i: Int) {
    val s = c.sends[i]
    val scene = vm.project.scene
    val aux = scene.channels.firstOrNull { it.kind == ChKind.AUX && it.inIndex == s.dest }
    val name = (aux?.name ?: "Bus ${s.dest + 1}") + if (s.pre) " ·pre" else ""
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.weight(1f)) {
            Field(name, { open = true }, stringResource(R.string.send_level, aux?.name ?: ""), Modifier.fillMaxWidth())
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (s.pre) R.string.post_fader else R.string.pre_fader)) },
                    onClick = { open = false; vm.editChannel(c.id) { ch -> ch.copy(sends = ch.sends.toMutableList().also { l -> if (i in l.indices) l[i] = l[i].copy(pre = !l[i].pre) }) } },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove_send)) },
                    onClick = { open = false; vm.editChannel(c.id) { ch -> ch.copy(sends = ch.sends.filterIndexed { k, _ -> k != i }) } },
                )
            }
        }
        MiniBtn("M", s.mute, { vm.editChannel(c.id) { ch -> ch.copy(sends = ch.sends.toMutableList().also { l -> if (i in l.indices) l[i] = l[i].copy(mute = !l[i].mute) }) } },
            stringResource(R.string.cd_mute, aux?.name ?: ""), Modifier.width(20.dp), Console.red)
        Knob(
            ((s.levelDb + 60f) / 72f).coerceIn(0f, 1f),
            { p -> vm.changeChannel(c.id) { ch -> ch.copy(sends = ch.sends.toMutableList().also { l -> if (i in l.indices) l[i] = l[i].copy(levelDb = p * 72f - 60f) }) } },
            { vm.beginGesture() }, stringResource(R.string.send_level, aux?.name ?: ""), FaderMap.format(s.levelDb) + " dB", size = 20.dp,
            onDoubleTap = { vm.editChannel(c.id) { ch -> ch.copy(sends = ch.sends.toMutableList().also { l -> if (i in l.indices) l[i] = l[i].copy(levelDb = 0f) }) } },
        )
    }
}

@Composable
private fun EmptySendRow(vm: MixerViewModel, c: Channel) {
    val scene = vm.project.scene
    val free = scene.channels.filter { it.kind == ChKind.AUX && !Ops.busLoop(scene, c, it.inIndex) && c.sends.none { s -> s.dest == it.inIndex } }
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Field(stringResource(R.string.none_send), { if (free.isNotEmpty()) open = true }, stringResource(R.string.send_add), Modifier.fillMaxWidth(), dim = true)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            free.forEach { a ->
                DropdownMenuItem(text = { Text(a.name) }, onClick = {
                    open = false
                    val upd = Ops.addSend(c, scene, a.inIndex, vm.limits)
                    if (upd == null) vm.say(R.string.msg_limit_reached, vm.limits.name) else vm.editChannel(c.id) { upd }
                })
            }
        }
    }
}

@Composable
fun OutputBody(vm: MixerViewModel, c: Channel) {
    val scene = vm.project.scene
    val masterName = stringResource(R.string.kind_master)
    val groups = scene.channels.filter { it.kind == ChKind.GROUP && it.id != c.id }
    val auxes = if (c.kind != ChKind.AUX) scene.channels.filter { it.kind == ChKind.AUX } else emptyList()
    val sel = ArrayList<String>()
    if (c.outMask and Out.MASTER != 0) sel.add(masterName)
    groups.forEach { if (c.outMask and Out.group(it.groupIndex) != 0) sel.add(it.name) }
    auxes.forEach { if (c.outMask and Out.bus(it.inIndex) != 0) sel.add(it.name) }
    val label = if (sel.isEmpty()) "–" else sel.joinToString("+")
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Field(label, { open = true }, "OUTPUT: $label", Modifier.fillMaxWidth())
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text((if (c.outMask and Out.MASTER != 0) "✓ " else "    ") + masterName) },
                onClick = { open = false; vm.editChannel(c.id) { Ops.toggleOut(it, Out.MASTER) } },
            )
            groups.forEach { g ->
                val bit = Out.group(g.groupIndex)
                DropdownMenuItem(
                    text = { Text((if (c.outMask and bit != 0) "✓ " else "    ") + g.name) },
                    onClick = { open = false; vm.editChannel(c.id) { Ops.toggleOut(it, bit) } },
                )
            }
            auxes.forEach { a ->
                val bit = Out.bus(a.inIndex)
                DropdownMenuItem(
                    text = { Text((if (c.outMask and bit != 0) "✓ " else "    ") + a.name) },
                    onClick = { open = false; vm.editChannel(c.id) { Ops.toggleOut(it, bit) } },
                )
            }
        }
    }
}

@Composable
fun PanBody(vm: MixerViewModel, c: Channel, iw: Dp) {
    val pct = (c.pan * 100f).roundToInt()
    val txt = if (pct == 0) "0" else if (pct < 0) "L${-pct}" else "R$pct"
    Knob(
        (c.pan + 1f) / 2f, { p -> vm.changeChannel(c.id) { it.copy(pan = p * 2f - 1f) } }, { vm.beginGesture() },
        stringResource(R.string.cd_pan, c.name), txt, size = minOf(30.dp, iw),
        onDoubleTap = { vm.editChannel(c.id) { it.copy(pan = 0f) } }, bipolar = true,
    )
    Text(txt, fontSize = 9.sp, color = Console.label, modifier = Modifier.padding(top = 1.dp))
}

@Composable
private fun ColumnScope.FaderBody(vm: MixerViewModel, c: Channel, iw: Dp) {
    var dlg by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().weight(1f)) {
        if (iw >= 64.dp) FaderScale()
        ConsoleFader(
            c.faderDb, stringResource(R.string.cd_fader, c.name), { vm.beginGesture() },
            { db -> vm.changeChannel(c.id) { it.copy(faderDb = db) } },
            Modifier.weight(1.15f).fillMaxHeight(),
        )
        Spacer(Modifier.width(2.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(FaderMargin), contentAlignment = Alignment.Center) { PeakText(vm, c.id) }
            ConsoleMeter(
                { maxOf(vm.meters.getOrElse(2 * c.id) { 0f }, vm.meters.getOrElse(2 * c.id + 1) { 0f }) },
                { vm.meters.getOrElse(4 * N.MAX_CHANNELS + c.id) { 0f } },
                Modifier.fillMaxWidth().weight(1f).clickable { vm.clearClip() },
            )
            Spacer(Modifier.height(FaderMargin))
        }
    }
    val valDesc = stringResource(R.string.fader_value) + ": " + FaderMap.format(c.faderDb)
    Box(
        Modifier.padding(top = 3.dp).widthIn(max = 50.dp).fillMaxWidth().height(18.dp).clip(RoundedCornerShape(4.dp)).background(Console.track)
            .border(1.dp, Console.line, RoundedCornerShape(4.dp))
            .clickable(role = Role.Button) { dlg = true }
            .semantics { contentDescription = valDesc },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (c.faderDb <= -60f) "-inf" else String.format(Locale.US, "%.1f", c.faderDb),
            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Console.text, maxLines = 1,
        )
    }
    if (dlg) DbDialog(c.faderDb, stringResource(R.string.fader_value), { db -> vm.editChannel(c.id) { it.copy(faderDb = db) } }, { dlg = false })
}

@Composable
private fun MssBody(vm: MixerViewModel, c: Channel, audio: Boolean, iw: Dp) {
    val dia = 24.dp
    Row(Modifier.fillMaxWidth().fillMaxHeight(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        RoundBtn("M", c.mute, { vm.editChannel(c.id) { it.copy(mute = !it.mute) } }, stringResource(R.string.cd_mute, c.name), Console.red, dia = dia)
        if (audio && iw >= 44.dp) {
            RoundBtn("S", c.solo, { vm.editChannel(c.id) { it.copy(solo = !it.solo) } }, stringResource(R.string.cd_solo, c.name), Console.amber, dia = dia)
        }
        if (audio && iw >= 66.dp) {
            RoundBtn("SAFE", c.safe, { vm.editChannel(c.id) { it.copy(safe = !it.safe) } }, stringResource(R.string.safe_desc), Console.green, fs = 7.sp, dia = dia)
        }
    }
}

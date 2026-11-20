package com.gpaudio.gpmix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Gaya tombol halaman: Tonal (bawaan), Primary (aksi utama), Danger (hapus/reset), Text (tanpa latar). */
enum class BtnStyle { Tonal, Primary, Danger, Text }

/**
 * Tombol standar untuk halaman Settings / Pads / dialog. Tinggi dan huruf diambil dari [LocalUi], jadi sama
 * besarnya dengan tombol toolbar. Isi ([content]) cukup `Text(...)`: warna dan huruf diatur di sini.
 */
@Composable
fun GpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: BtnStyle = BtnStyle.Tonal,
    content: @Composable RowScope.() -> Unit,
) {
    val ui = LocalUi.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val shape = RoundedCornerShape(8.dp)
    val accent = if (style == BtnStyle.Danger) Console.red else Console.blue
    val bg: Brush? = when (style) {
        BtnStyle.Primary -> Brush.verticalGradient(listOf(lerp(accent, Color.White, if (pressed) 0f else 0.18f), accent))
        BtnStyle.Danger -> Brush.verticalGradient(listOf(accent.copy(alpha = if (pressed) 0.38f else 0.26f), accent.copy(alpha = 0.14f)))
        BtnStyle.Tonal -> Brush.verticalGradient(
            if (pressed) listOf(Console.fieldBorder, Console.field) else listOf(Console.fieldHi, Console.field),
        )
        BtnStyle.Text -> if (pressed) SolidColor(Console.blue.copy(alpha = 0.14f)) else null
    }
    val borderColor = when (style) {
        BtnStyle.Primary -> accent
        BtnStyle.Danger -> accent.copy(alpha = 0.6f)
        BtnStyle.Tonal -> if (pressed) Console.blue else Console.line
        BtnStyle.Text -> Color.Transparent
    }
    val fg = when (style) {
        BtnStyle.Primary -> Color(0xFF04121F)
        BtnStyle.Danger -> Console.red
        BtnStyle.Tonal -> Console.text
        BtnStyle.Text -> Console.blue
    }
    Box(
        modifier.defaultMinSize(minWidth = 44.dp).height(ui.btnH).alpha(if (enabled) 1f else 0.4f).clip(shape)
            .then(if (bg != null) Modifier.background(bg) else Modifier)
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (style == BtnStyle.Text) 10.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            ProvideTextStyle(TextStyle(fontSize = ui.btnFont, fontWeight = FontWeight.SemiBold)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

/** Tombol teks (tombol dialog, tombol kecil di daftar). */
@Composable
fun GpTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    GpButton(onClick = onClick, modifier = modifier, enabled = enabled, style = BtnStyle.Text, content = content)
}

/** Tombol bar (toolbar / header / status bar): tinggi dan huruf dari [LocalUi], sama untuk semua tombol di bar. */
@Composable
fun BarBtn(
    text: String,
    on: Boolean,
    onClick: () -> Unit,
    desc: String,
    modifier: Modifier = Modifier,
    onColor: Color = Console.blue,
    enabled: Boolean = true,
    toggle: Boolean = true,
    small: Boolean = false,
) {
    val ui = LocalUi.current
    val h = if (small) ui.smallBtnH else ui.btnH
    MiniBtn(
        text = text, on = on, onClick = onClick, desc = desc,
        modifier = modifier.defaultMinSize(minWidth = h),
        onColor = onColor, enabled = enabled, toggle = toggle, h = h, hPad = 9.dp,
    )
}

@Composable
fun BarTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = LocalUi.current.sub, fontWeight = FontWeight.Bold, color = Console.text, maxLines = 1)
}

/** Pemisah tipis antar kelompok tombol di bar. */
@Composable
fun BarSep() {
    Box(Modifier.width(1.dp).height(LocalUi.current.btnH * 0.6f).background(Console.line))
}

@Composable
fun HLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Console.line))
}

/** Header layar kedua (Pads, Settings): tombol kembali + judul, ukuran sama dengan toolbar mixer. */
@Composable
fun SubHeader(title: String, backText: String, onBack: () -> Unit) {
    val ui = LocalUi.current
    Column(Modifier.fillMaxWidth().background(Console.bar)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ui.barPadH, vertical = ui.barPadV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BarBtn("‹ $backText", false, onBack, backText, toggle = false)
            BarTitle(title)
        }
        HLine()
    }
}

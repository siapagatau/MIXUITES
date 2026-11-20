package com.gpaudio.gpmix.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ukuran UI bersama untuk semua layar di luar strip (toolbar, header, status bar, tombol halaman, dialog).
 * Satu sumber supaya tombol, judul dan teks selalu sebanding: tidak ada yang mendominasi, termasuk di landscape
 * (tinggi layar pendek -> [Compact]).
 */
@Immutable
data class UiMetrics(
    val compact: Boolean,
    /** Tinggi tombol standar (toolbar, header, halaman, dialog). */
    val btnH: Dp,
    /** Tinggi tombol kecil (status bar). */
    val smallBtnH: Dp,
    val btnFont: TextUnit,
    /** Judul layar / dialog. */
    val title: TextUnit,
    /** Judul bar (toolbar). */
    val sub: TextUnit,
    val body: TextUnit,
    val caption: TextUnit,
    val barPadV: Dp,
    val barPadH: Dp,
    val pagePad: Dp,
) {
    companion object {
        val Regular = UiMetrics(false, 36.dp, 28.dp, 13.sp, 18.sp, 15.sp, 14.sp, 11.sp, 4.dp, 8.dp, 14.dp)
        val Compact = UiMetrics(true, 28.dp, 22.dp, 11.sp, 14.sp, 12.sp, 12.sp, 10.sp, 2.dp, 6.dp, 8.dp)
    }
}

val LocalUi = compositionLocalOf { UiMetrics.Regular }

/** Warna panel konsol. Dua palet: bawaan (abu gelap + biru) dan Studio Teal. "blue" = warna aksen tema. */
@Immutable
class ConsolePalette(
    val bg: Color,
    val bar: Color,
    val body: Color,
    val panel: Color,
    val panelHi: Color,
    val field: Color,
    val fieldHi: Color,
    val fieldBorder: Color,
    val line: Color,
    val label: Color,
    val text: Color,
    val blue: Color,
    val track: Color,
    val knob: Color,
    val knobHi: Color,
    val nameBar: Color,
    val red: Color,
    val green: Color,
    val amber: Color,
)

val DefaultPalette = ConsolePalette(
    bg = Color(0xFF17181B), bar = Color(0xFF202226), body = Color(0xFF25272B),
    panel = Color(0xFF2B2E33), panelHi = Color(0xFF32363C),
    field = Color(0xFF383C42), fieldHi = Color(0xFF474C54), fieldBorder = Color(0xFF1C1E21),
    line = Color(0xFF454A52), label = Color(0xFF9AA0AA), text = Color(0xFFECEEF2),
    blue = Color(0xFF4DA3FF), track = Color(0xFF131518),
    knob = Color(0xFF50565F), knobHi = Color(0xFF7A818C), nameBar = Color(0xFF3F5F8A),
    red = Color(0xFFFF6B6B), green = Color(0xFF5CD68A), amber = Color(0xFFFFB020),
)

val StudioPalette = ConsolePalette(
    bg = Color(0xFF0E1819), bar = Color(0xFF142224), body = Color(0xFF172729),
    panel = Color(0xFF1D3234), panelHi = Color(0xFF24393C),
    field = Color(0xFF2A4548), fieldHi = Color(0xFF38595C), fieldBorder = Color(0xFF0C1516),
    line = Color(0xFF356064), label = Color(0xFF8FB3AF), text = Color(0xFFE0EEEC),
    blue = Color(0xFF26C6B0), track = Color(0xFF0A1213),
    knob = Color(0xFF47696D), knobHi = Color(0xFF7BA3A7), nameBar = Color(0xFF2B6A66),
    red = Color(0xFFFF6B6B), green = Color(0xFF5CD68A), amber = Color(0xFFFFB74D),
)

private val DefaultScheme = darkColorScheme(
    primary = Color(0xFF4DA3FF), onPrimary = Color(0xFF04121F),
    primaryContainer = Color(0xFF1F4A7A), onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFF8AB4F8), onSecondary = Color(0xFF00131F),
    background = Color(0xFF17181B), onBackground = Color(0xFFECEEF2),
    surface = Color(0xFF25272B), onSurface = Color(0xFFECEEF2), surfaceTint = Color.Transparent,
    surfaceVariant = Color(0xFF383C42), onSurfaceVariant = Color(0xFFB4BAC4),
    error = Color(0xFFFF6B6B), outline = Color(0xFF6E747E), outlineVariant = Color(0xFF454A52),
)

private val StudioScheme = darkColorScheme(
    primary = Color(0xFF26C6B0), onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF0F5A52), onPrimaryContainer = Color(0xFFBFF3EA),
    secondary = Color(0xFFFFB74D), onSecondary = Color(0xFF241400),
    background = Color(0xFF0E1819), onBackground = Color(0xFFE0EEEC),
    surface = Color(0xFF172729), onSurface = Color(0xFFE0EEEC), surfaceTint = Color.Transparent,
    surfaceVariant = Color(0xFF2A4548), onSurfaceVariant = Color(0xFFADC7C4),
    error = Color(0xFFFF6B6B), outline = Color(0xFF5E807C), outlineVariant = Color(0xFF356064),
)

private val GpShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

private fun gpTypography(ui: UiMetrics) = Typography(
    headlineSmall = TextStyle(fontSize = ui.title, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = ui.title, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = ui.sub, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = ui.btnFont, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = ui.body),
    bodyMedium = TextStyle(fontSize = ui.body),
    bodySmall = TextStyle(fontSize = ui.caption),
    labelLarge = TextStyle(fontSize = ui.btnFont, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = ui.caption, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = ui.caption, fontWeight = FontWeight.Medium),
)

/** Warna channel 0..7 (juga ditampilkan lewat teks/posisi, bukan hanya warna). */
val ChannelColors = listOf(
    Color(0xFFFF7043), Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFF26C6DA),
    Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFEC407A), Color(0xFFBDBDBD),
)

val MeterGreen = Color(0xFF4CAF50)
val MeterYellow = Color(0xFFFFEB3B)
val MeterRed = Color(0xFFF44336)

/**
 * Tema: "default" (abu gelap + biru) dan "studio" (Studio Teal). Selalu gelap: dipakai di panggung/studio redup.
 * Palet konsol ([Console]) ikut berganti sesuai tema, dan ukuran UI ([LocalUi]) menyesuaikan orientasi:
 * landscape (tinggi pendek) memakai ukuran ringkas supaya header dan tombol tidak lebih besar dari isi.
 */
@Composable
fun GpTheme(theme: String, content: @Composable () -> Unit) {
    val studio = theme == "studio"
    val pal = if (studio) StudioPalette else DefaultPalette
    if (Console.palette !== pal) Console.palette = pal
    val h = LocalConfiguration.current.screenHeightDp
    val ui = if (h < 480) UiMetrics.Compact else UiMetrics.Regular
    val typo = remember(ui) { gpTypography(ui) }
    CompositionLocalProvider(LocalUi provides ui) {
        MaterialTheme(
            colorScheme = if (studio) StudioScheme else DefaultScheme,
            typography = typo,
            shapes = GpShapes,
            content = content,
        )
    }
}

# Gaya UI (konsisten portrait & landscape)

- `UiMetrics` (Theme.kt): satu sumber ukuran untuk toolbar, header, status bar, tombol halaman, dialog.
  `Regular` (tinggi layar >= 480dp) dan `Compact` (landscape HP). Ambil lewat `LocalUi.current`.
- Tombol bar: `BarBtn` (tinggi/huruf dari `LocalUi`). Tombol halaman/dialog: `GpButton` / `GpTextButton`
  (`BtnStyle.Tonal|Primary|Danger|Text`). Jangan pakai `OutlinedButton`/`TextButton` Material langsung.
- Tombol kecil di strip: `MiniBtn`; huruf otomatis ikut tinggi tombol (`h * 0.44`), jadi selalu sebanding.
- Warna konsol: `Console.*` dibaca dari `ConsolePalette` (Default / Studio Teal) dan berganti saat tema diganti.
  `Console.blue` = warna aksen tema.
- Strip: area "Fit" tidak lagi mengecil di bawah `MinLegibleScale` (0.6); di bawah itu area strip bisa digeser.

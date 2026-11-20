package com.gpaudio.gpmix.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Perkiraan kurva respons EQ untuk grafik di editor (biquad RBJ). Hanya untuk tampilan; suara tetap
 * dihitung engine C++. Indeks parameter mengikuti deskriptor EQ di engine:
 * 0 HP on, 1 HP freq, 2 HP slope, 3 LP on, 4 LP freq, 5 LP slope, lalu 5 parameter per band
 * (on, freq, gain, Q, shape) untuk 6 band mulai indeks 6, dan terakhir Out Gain.
 */
object EqMath {
    const val BANDS = 6
    const val BAND0 = 6
    const val PER_BAND = 5
    const val OUT_GAIN = BAND0 + BANDS * PER_BAND   // 36
    const val SR = 48000.0
    const val F_MIN = 20f
    const val F_MAX = 20000f
    const val DB_RANGE = 18f

    fun bandIndex(band: Int, field: Int) = BAND0 + band * PER_BAND + field   // field: 0 on,1 freq,2 gain,3 Q,4 shape

    /** Besar |H| (dB) satu biquad. shape: 0 bell, 1 low shelf, 2 high shelf. */
    fun biquadDb(shape: Int, f0: Double, gainDb: Double, q: Double, f: Double): Double {
        if (gainDb == 0.0) return 0.0
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * f0.coerceIn(10.0, SR / 2.0 - 100.0) / SR
        val cw = cos(w0)
        val sw = sin(w0)
        val b0: Double; val b1: Double; val b2: Double; val a0: Double; val a1: Double; val a2: Double
        when (shape) {
            1 -> {
                val al = sw / 2.0 * sqrt(2.0)
                val t = 2.0 * sqrt(a) * al
                b0 = a * ((a + 1) - (a - 1) * cw + t); b1 = 2 * a * ((a - 1) - (a + 1) * cw); b2 = a * ((a + 1) - (a - 1) * cw - t)
                a0 = (a + 1) + (a - 1) * cw + t; a1 = -2 * ((a - 1) + (a + 1) * cw); a2 = (a + 1) + (a - 1) * cw - t
            }
            2 -> {
                val al = sw / 2.0 * sqrt(2.0)
                val t = 2.0 * sqrt(a) * al
                b0 = a * ((a + 1) + (a - 1) * cw + t); b1 = -2 * a * ((a - 1) + (a + 1) * cw); b2 = a * ((a + 1) + (a - 1) * cw - t)
                a0 = (a + 1) - (a - 1) * cw + t; a1 = 2 * ((a - 1) - (a + 1) * cw); a2 = (a + 1) - (a - 1) * cw - t
            }
            else -> {
                val al = sw / (2.0 * q.coerceAtLeast(0.05))
                b0 = 1 + al * a; b1 = -2 * cw; b2 = 1 - al * a
                a0 = 1 + al / a; a1 = -2 * cw; a2 = 1 - al / a
            }
        }
        val w = 2.0 * PI * f / SR
        val c1 = cos(w); val s1 = sin(w); val c2 = cos(2 * w); val s2 = sin(2 * w)
        val nr = b0 + b1 * c1 + b2 * c2
        val ni = -(b1 * s1 + b2 * s2)
        val dr = a0 + a1 * c1 + a2 * c2
        val di = -(a1 * s1 + a2 * s2)
        val num = nr * nr + ni * ni
        val den = dr * dr + di * di
        return 10.0 * log10((num / den).coerceAtLeast(1e-12))
    }

    /** Respons high-pass / low-pass Butterworth orde n = (slopeIndex + 1) * 2. */
    fun passDb(high: Boolean, fc: Double, slopeIdx: Int, f: Double): Double {
        val n = (slopeIdx.coerceIn(0, 3) + 1) * 2
        val r = if (high) (fc / f) else (f / fc)
        return -10.0 * log10(1.0 + r.pow(2.0 * n))
    }

    /** Respons total (dB) pada frekuensi [f], dari daftar parameter efek EQ. */
    fun totalDb(p: List<Float>, f: Double): Double {
        fun v(i: Int, d: Float = 0f) = p.getOrElse(i) { d }
        var db = 0.0
        if (v(0) > 0.5f) db += passDb(true, v(1, 30f).toDouble(), v(2).toInt(), f)
        if (v(3) > 0.5f) db += passDb(false, v(4, 16000f).toDouble(), v(5).toInt(), f)
        for (b in 0 until BANDS) {
            if (v(bandIndex(b, 0)) > 0.5f)
                db += biquadDb(v(bandIndex(b, 4)).toInt(), v(bandIndex(b, 1), 1000f).toDouble(), v(bandIndex(b, 2)).toDouble(), v(bandIndex(b, 3), 1f).toDouble(), f)
        }
        return db + v(OUT_GAIN)
    }

    /** Posisi x (0..1) untuk frekuensi pada sumbu logaritmik 20 Hz .. 20 kHz, dan sebaliknya. */
    fun freqToX(f: Float): Float = (log10(f.coerceIn(F_MIN, F_MAX) / F_MIN) / log10(F_MAX / F_MIN)).toFloat()
    fun xToFreq(x: Float): Float = (F_MIN * 10.0.pow(x.coerceIn(0f, 1f) * log10((F_MAX / F_MIN).toDouble()))).toFloat()
}

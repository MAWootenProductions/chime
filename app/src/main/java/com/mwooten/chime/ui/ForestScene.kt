package com.mwooten.chime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.runtime.withFrameNanos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun rememberClock(): State<Float> = produceState(0f) {
    val start = withFrameNanos { it }
    while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
}

private class Trunk(val x: Float, val w: Float, val lean: Float, val canopyY: Float)
private class Spore(val x: Float, val y: Float, val speed: Float, val phase: Float, val size: Float)

/**
 * The Forest Temple: light shafts falling through a canopy onto a mossy stone archway,
 * fog drifting between the trunks, spores of light rising from the ground.
 * [dim] darkens it behind the inner screens; [showMote] lets Mote wander the title.
 */
@Composable
fun ForestScene(modifier: Modifier = Modifier, dim: Float = 0f, showMote: Boolean = false) {
    val clock = rememberClock()
    val rnd = remember { Random(11) }
    val far = remember { List(10) { Trunk(rnd.nextFloat(), 0.025f + rnd.nextFloat() * 0.03f, (rnd.nextFloat() - .5f) * .02f, 0.08f + rnd.nextFloat() * 0.1f) } }
    val mid = remember { List(6) { Trunk(rnd.nextFloat(), 0.05f + rnd.nextFloat() * 0.04f, (rnd.nextFloat() - .5f) * .03f, 0f) } }
    val spores = remember { List(46) { Spore(rnd.nextFloat(), rnd.nextFloat(), 0.012f + rnd.nextFloat() * 0.025f, rnd.nextFloat() * 6.28f, 1.2f + rnd.nextFloat() * 2.2f) } }

    Canvas(modifier) {
        val t = clock.value
        val w = size.width; val h = size.height

        // canopy-filtered sky and the light breaking through
        drawRect(Brush.verticalGradient(0f to Color(0xFF0A1C17), 0.45f to Color(0xFF14342A), 0.72f to Color(0xFF1E4434), 1f to Color(0xFF06100C)))
        drawRect(Brush.radialGradient(listOf(Color(0x8C7FB98A), Color(0x00000000)), center = Offset(w * 0.42f, h * 0.12f), radius = h * 0.75f))

        // far trunks, pale with distance
        for (tr in far) trunk(tr, w, h, Color(0xFF1C3D30), h * 0.78f, t * 0.4f)

        // god rays
        for (i in 0 until 5) {
            val x0 = w * (0.18f + i * 0.13f) + sin(t * 0.07f + i) * w * 0.01f
            val a = 0.05f + 0.045f * (0.5f + 0.5f * sin(t * 0.35f + i * 1.7f))
            val p = Path().apply {
                moveTo(x0, -10f); lineTo(x0 + w * (0.05f + i % 2 * 0.03f), -10f)
                lineTo(x0 + w * 0.42f, h * 0.9f); lineTo(x0 + w * 0.22f, h * 0.9f); close()
            }
            drawPath(p, Brush.verticalGradient(listOf(Color(0xFFD9F2C0).copy(alpha = a), Color(0x00D9F2C0)), 0f, h * 0.9f))
        }

        // the temple archway
        arch(w * 0.36f, h * 0.76f, min(w * 0.42f, h * 0.3f))

        fog(w, h, h * 0.62f, t, 0.10f, 0)

        // mid trunks
        for (tr in mid) trunk(tr, w, h, Color(0xFF0E2219), h * 0.86f, t * 0.6f)

        // ground
        val ground = Path().apply {
            moveTo(0f, h * 0.84f)
            cubicTo(w * 0.3f, h * 0.80f, w * 0.6f, h * 0.88f, w, h * 0.82f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(ground, Brush.verticalGradient(listOf(Color(0xFF0E2119), Color(0xFF040A07)), h * 0.8f, h))
        // ferns along the ground line
        for (i in 0 until 26) {
            val fx = w * (i / 25f) + sin(i * 3.3f) * 14f
            val fy = h * 0.835f + sin(i * 1.7f) * h * 0.012f
            val sway = sin(t * 0.8f + i) * 4f
            fern(fx, fy, 14f + abs(sin(i * 2.1f)) * 22f, sway)
        }

        fog(w, h, h * 0.8f, t, 0.08f, 2)

        // near framing trunks
        val nearC = Color(0xFF030806)
        drawPath(Path().apply { moveTo(-w * 0.04f, 0f); lineTo(w * 0.09f, 0f); cubicTo(w * 0.07f, h * 0.5f, w * 0.06f, h * 0.8f, w * 0.14f, h); lineTo(-w * 0.04f, h); close() }, nearC)
        drawPath(Path().apply { moveTo(w * 0.93f, 0f); lineTo(w * 1.04f, 0f); lineTo(w * 1.04f, h); lineTo(w * 0.84f, h); cubicTo(w * 0.92f, h * 0.8f, w * 0.95f, h * 0.4f, w * 0.93f, 0f); close() }, nearC)

        // rising spores of light
        for (s in spores) {
            val life = ((s.y - t * s.speed) % 1f + 1f) % 1f
            val y = h * (0.35f + life * 0.6f)
            val x = w * s.x + sin(t * 0.6f + s.phase) * 18f
            val a = (0.25f + 0.45f * (0.5f + 0.5f * sin(t * 1.8f + s.phase))) * min(1f, life * 4f)
            drawCircle(Brush.radialGradient(listOf(C.glow.copy(alpha = a), Color(0x00C8F7A8)), Offset(x, y), s.size * 4f), s.size * 4f, Offset(x, y))
        }

        if (showMote) {
            val mx = { k: Float -> w * 0.5f + sin(k * 0.55f) * w * 0.3f + sin(k * 1.3f) * w * 0.04f }
            val my = { k: Float -> h * 0.52f + sin(k * 0.9f) * h * 0.06f }
            for (k in 1..8) {
                val tt = t - k * 0.05f
                drawCircle(C.glow.copy(alpha = 0.45f - k * 0.05f), 3.4f - k * 0.35f, Offset(mx(tt), my(tt)))
            }
            moteSprite(Offset(mx(t), my(t)), 1f, t)
        }

        // vignette + dim for inner screens
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xB3000000)), Offset(w / 2, h * 0.5f), max(w, h) * 0.8f))
        if (dim > 0f) drawRect(Color(0xFF050C09).copy(alpha = 0.78f * dim))
    }
}

private fun DrawScope.trunk(tr: Trunk, w: Float, h: Float, c: Color, baseY: Float, t: Float) {
    val x = w * tr.x + sin(t * 0.1f + tr.x * 10) * 2f
    val tw = w * tr.w
    val p = Path().apply {
        moveTo(x - tw * 0.4f, -10f); lineTo(x + tw * 0.4f, -10f)
        lineTo(x + tw * 0.55f + w * tr.lean, baseY - tw)
        cubicTo(x + tw * 0.6f, baseY - tw * 0.3f, x + tw * 1.2f, baseY, x + tw * 1.6f, baseY + 4f)
        lineTo(x - tw * 1.6f, baseY + 4f)
        cubicTo(x - tw * 1.2f, baseY, x - tw * 0.6f, baseY - tw * 0.3f, x - tw * 0.55f + w * tr.lean, baseY - tw)
        close()
    }
    drawPath(p, c)
}

private fun DrawScope.arch(cx: Float, base: Float, s: Float) {
    val stone = Color(0xFF243A31); val shade = Color(0xFF172A22); val mossC = Color(0xFF3E6B48)
    val pw = s * 0.16f; val ph = s * 0.95f; val gap = s * 0.62f
    // left pillar, right pillar (broken), lintel arch
    drawRect(stone, Offset(cx - gap / 2 - pw, base - ph), Size(pw, ph))
    drawRect(stone, Offset(cx + gap / 2, base - ph * 0.72f), Size(pw, ph * 0.72f))
    drawRect(shade, Offset(cx - gap / 2 - pw * 0.25f, base - ph), Size(pw * 0.25f, ph))
    val a = Path().apply {
        moveTo(cx - gap / 2 - pw, base - ph)
        cubicTo(cx - gap / 2, base - ph - s * 0.42f, cx + gap / 2, base - ph - s * 0.42f, cx + gap / 2 + pw * 0.3f, base - ph * 0.92f)
        lineTo(cx + gap / 2 + pw * 0.1f, base - ph * 0.8f)
        cubicTo(cx + gap / 2 - pw * 0.4f, base - ph - s * 0.24f, cx - gap / 2 + pw * 0.2f, base - ph - s * 0.26f, cx - gap / 2, base - ph * 0.92f)
        close()
    }
    drawPath(a, stone)
    // a carved triangle sigil glowing faintly in the keystone (an original three-leaf mark)
    val kx = cx - pw * 0.2f; val ky = base - ph - s * 0.22f
    for (i in 0 until 3) {
        val ang = -PI / 2 + i * 2 * PI / 3
        drawCircle(C.glow.copy(alpha = 0.35f), s * 0.025f, Offset(kx + cos(ang).toFloat() * s * 0.04f, ky + sin(ang).toFloat() * s * 0.04f))
    }
    // moss drips
    for (i in 0 until 7) {
        val mx = cx - gap / 2 - pw + i * (gap + pw * 2) / 7f
        drawRect(mossC.copy(alpha = 0.7f), Offset(mx, base - ph - s * 0.06f + abs(sin(i * 1.9f)) * s * 0.05f), Size(s * 0.03f, s * 0.04f + abs(sin(i * 2.7f)) * s * 0.08f))
    }
    // fallen block
    drawRect(stone, Offset(cx + gap / 2 + pw * 1.3f, base - s * 0.1f), Size(s * 0.2f, s * 0.1f))
}

private fun DrawScope.fern(x: Float, y: Float, hgt: Float, sway: Float) {
    val c = Color(0xFF07130E)
    for (k in -2..2) {
        val p = Path().apply {
            moveTo(x, y)
            cubicTo(x + k * hgt * 0.2f, y - hgt * 0.6f, x + k * hgt * 0.45f + sway, y - hgt * 0.9f, x + k * hgt * 0.6f + sway, y - hgt * (1f - abs(k) * 0.15f))
            lineTo(x + k * hgt * 0.6f + sway + 2f, y - hgt * (1f - abs(k) * 0.15f) + 3f)
            cubicTo(x + k * hgt * 0.35f + sway, y - hgt * 0.7f, x + k * hgt * 0.15f + 3f, y - hgt * 0.4f, x + 3f, y)
            close()
        }
        drawPath(p, c)
    }
}

private fun DrawScope.fog(w: Float, h: Float, y: Float, t: Float, a: Float, seed: Int) {
    val drift = (t * 8f * (1 + seed * 0.3f)) % w
    for (k in -1..1) {
        val cx = drift + k * w
        drawOval(
            Brush.radialGradient(listOf(Color(0xFFC9DCCF).copy(alpha = a), Color(0x00C9DCCF)), Offset(cx + w * 0.3f, y), w * 0.45f),
            Offset(cx - w * 0.15f, y - h * 0.06f), Size(w * 0.9f, h * 0.12f)
        )
    }
}

/** Mote: a bright core with four leaf-shaped wings that flutter. */
fun DrawScope.moteSprite(c: Offset, scale: Float, t: Float, intensity: Float = 1f) {
    val r = 34f * scale
    drawCircle(Brush.radialGradient(listOf(C.glow.copy(alpha = 0.55f * intensity), Color(0x00C8F7A8)), c, r * 2.6f), r * 2.6f, c)
    val flap = 0.55f + 0.45f * abs(sin(t * 14f))
    val wing = Color(0xCCE9F7D2)
    for (side in listOf(-1f, 1f)) {
        val up = Path().apply {
            moveTo(c.x + side * r * 0.12f, c.y - r * 0.1f)
            cubicTo(c.x + side * r * 0.5f, c.y - r * 0.9f * flap, c.x + side * r * 1.15f, c.y - r * 0.85f * flap, c.x + side * r * 1.1f, c.y - r * 0.35f * flap)
            cubicTo(c.x + side * r * 0.9f, c.y - r * 0.05f, c.x + side * r * 0.4f, c.y, c.x + side * r * 0.12f, c.y)
            close()
        }
        drawPath(up, wing)
        val down = Path().apply {
            moveTo(c.x + side * r * 0.1f, c.y + r * 0.1f)
            cubicTo(c.x + side * r * 0.4f, c.y + r * 0.6f * flap, c.x + side * r * 0.75f, c.y + r * 0.7f * flap, c.x + side * r * 0.7f, c.y + r * 0.35f)
            cubicTo(c.x + side * r * 0.5f, c.y + r * 0.15f, c.x + side * r * 0.3f, c.y + r * 0.1f, c.x + side * r * 0.1f, c.y + r * 0.1f)
            close()
        }
        drawPath(down, wing.copy(alpha = 0.6f))
    }
    drawCircle(Brush.radialGradient(listOf(C.glowCore, C.glow, Color(0x008FD47A)), c, r * 0.55f), r * 0.55f, c)
    drawCircle(Color.White, r * 0.16f, c)
}

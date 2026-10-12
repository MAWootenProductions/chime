package com.mwooten.chime.worlds

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.mwooten.chime.R
import com.mwooten.chime.data.Companion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * The bell: anything that saves or finishes a task "strikes" the drop. The drop ripples top to
 * bottom and a chime plays, pitched to the companion.
 */
object Bell {
    val strikes = MutableStateFlow(0L)
    private var pool: SoundPool? = null
    private var soundId = 0

    fun strike(ctx: Context, who: Companion) {
        strikes.value = System.nanoTime()
        try {
            val p = pool ?: SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            ).build().also { pool = it; soundId = it.load(ctx.applicationContext, R.raw.mote_chime, 1) }
            val rate = when (who) { Companion.WREN -> 1.0f; Companion.FIZZ -> 0.89f; Companion.PIP -> 1.26f; Companion.DIM -> 0.75f; Companion.BLAZE -> 1.12f }
            if (p.play(soundId, 0.8f, 0.8f, 1, 0, rate) == 0) {
                // first strike: the sound is still loading, play it when ready
                p.setOnLoadCompleteListener { sp, id, _ -> sp.play(id, 0.8f, 0.8f, 1, 0, rate) }
            }
        } catch (e: Exception) { }
    }
}

/**
 * Your companion as a character: one of six [Look]s cast in one of ten [Finish]es, with a face
 * that matches the personality ([who]). It swells and leans in while you talk ([listening],
 * [level] 0..1), and loops and rings when something is saved. Tap to talk; drag it (when
 * [draggable]) and it follows your finger, then floats back.
 */
@Composable
fun Character(
    look: Look,
    finish: Finish,
    who: Companion,
    level: Float,
    listening: Boolean,
    size: Dp,
    draggable: Boolean = true,
    onTap: () -> Unit,
) {
    var time by remember { mutableFloatStateOf(0f) }
    var chime by remember { mutableFloatStateOf(0f) }
    var lvl by remember { mutableFloatStateOf(0f) }
    var hear by remember { mutableFloatStateOf(0f) }
    var px by remember { mutableFloatStateOf(0f) }
    var py by remember { mutableFloatStateOf(0f) }
    var vx by remember { mutableFloatStateOf(0f) }
    var vy by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableFloatStateOf(0f) }
    val strikes by Bell.strikes.collectAsState()
    // a strike that happened before this character appeared shouldn't make it ring
    val seen = remember { Bell.strikes.value }
    LaunchedEffect(strikes) { if (strikes != 0L && strikes != seen) chime = 1f }

    val curLevel by rememberUpdatedState(level)
    val curListening by rememberUpdatedState(listening)
    val tap by rememberUpdatedState(onTap)
    val curWho by rememberUpdatedState(who)
    var mt by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f); last = now
                time += dt
                mt += dt * moodOf(curWho).speed
                chime = max(0f, chime - dt / 1.3f)
                lvl += ((if (curListening) max(curLevel, 0.1f) else 0f) - lvl) * 0.3f
                hear += ((if (curListening) 1f else 0f) - hear) * 0.2f
                if (dragging == 0f) { vx += -px * .08f; vy += -py * .08f; vx *= .82f; vy *= .82f; px += vx; py += vy }
            }
        }
    }

    val tone = remember(finish) { Tone(finish) }
    val drag = if (draggable) Modifier.pointerInput(Unit) {
        detectDragGestures(onDragStart = { dragging = 1f }, onDragEnd = { dragging = 0f }, onDragCancel = { dragging = 0f }) { change, d ->
            change.consume(); px += d.x * .8f; py += d.y * .8f
        }
    } else Modifier

    val mood = moodOf(who)
    Canvas(Modifier.size(size)
        .graphicsLayer { alpha = if (mood.flicker && sin(time * 7f) > .93f) .55f else 1f }
        .pointerInput(Unit) { detectTapGestures(onTap = { tap() }) }.then(drag)) {
        val w = this.size.width; val h = this.size.height
        // personality motion on top of the look's own float: Pip hops, Blaze can't sit still, Dim shrinks
        val hop = if (who == Companion.PIP) -abs(sin(time * 3f)) * h * .05f else 0f
        val jit = if (mood.jitter > 0f) mood.jitter * w / 300f else 0f
        val jx = if (jit > 0f) (Random.nextFloat() - .5f) * jit else 0f
        val jy = if (jit > 0f) (Random.nextFloat() - .5f) * jit else 0f
        translate(jx, hop + jy) { scale(mood.size, mood.size, pivot = Offset(w / 2f, h * .5f)) {
        val p = Pose(
            t = mt, k = chime, who = who, listening = curListening, lvl = lvl,
            grow = 1f + hear * (.08f + lvl * .12f),
            dx = px, dy = py + hear * h * .02f,
            flapBoost = 1f + hear * .5f,
        )
        when (look) {
            Look.STAR -> drawStar(p, tone)
            Look.SPRITE -> drawSprite(p, tone)
            Look.MOTH -> drawMoth(p, tone)
            Look.EMBER -> drawEmber(p, tone)
            Look.BUBBLE -> drawBubble(p, tone)
            Look.PUFF -> drawPuff(p, tone)
        }
        } }
    }
}

/** How each personality carries itself: time speed, jitter (px at 300 wide), size, flicker. */
private class Mood(val speed: Float, val jitter: Float, val size: Float, val flicker: Boolean)

private val MoodWren = Mood(1f, 0f, 1f, false)
private val MoodFizz = Mood(.75f, 0f, 1f, false)
private val MoodPip = Mood(1.6f, 0f, 1f, false)
private val MoodDim = Mood(.7f, 0f, .86f, true)
private val MoodBlaze = Mood(1.3f, 2.2f, 1.05f, false)

private fun moodOf(c: Companion): Mood = when (c) {
    Companion.WREN -> MoodWren
    Companion.FIZZ -> MoodFizz
    Companion.PIP -> MoodPip
    Companion.DIM -> MoodDim
    Companion.BLAZE -> MoodBlaze
}

/* ---------------- drawing (ported from the colors preview) ---------------- */

private const val TAU = 6.2831855f
private const val PIF = 3.1415927f
private const val DEG = 57.29578f
private val FaceInk = Color(0xFF2A2233)
private val Clear = Color.White.copy(alpha = 0f)

/** One frame's state: time, chime strike (1 → 0), mood, listening, size boost, drag offset. */
private class Pose(
    val t: Float, val k: Float, val who: Companion, val listening: Boolean, val lvl: Float,
    val grow: Float, val dx: Float, val dy: Float, val flapBoost: Float,
)

private class Tone(f: Finish) {
    val c = Color(f.c)
    val light = Color(f.light)
    val dark = Color(f.dark)
    val sheen: Color? = f.sheen?.let { Color(it) }
    val kind = f.sheenKind
}

private fun ovalRect(x: Float, y: Float, rx: Float, ry: Float) = Rect(x - rx, y - ry, x + rx, y + ry)

/** The moving gleam, clipped to [path] (the shape just filled). */
private fun DrawScope.sheen(path: Path, f: Tone, x: Float, y: Float, r: Float, t: Float) {
    clipPath(path) {
        val off = sin(t * .7f) * r * 1.8f
        fun band(col: Color, w: Float, o: Float) {
            drawRect(
                Brush.linearGradient(listOf(Clear, col, Clear), Offset(x + off + o - r * w, y - r), Offset(x + off + o + r * w, y + r)),
                Offset(x - r * 3, y - r * 3), Size(r * 6, r * 6),
            )
        }
        when (f.kind) {
            "rainbow" -> listOf(
                Color(255, 120, 160).copy(alpha = .45f), Color(255, 220, 120).copy(alpha = .45f),
                Color(120, 230, 170).copy(alpha = .45f), Color(120, 170, 255).copy(alpha = .5f),
            ).forEachIndexed { i, col -> band(col, .35f, (i - 1.5f) * r * .35f) }
            "bismuth" -> {
                val h = (t * 40f) % 360f
                listOf(h, h + 60f, h + 140f).forEachIndexed { i, hh ->
                    band(Color.hsl(hh % 360f, .9f, .65f, .45f), .45f, (i - 1) * r * .5f)
                }
            }
            else -> band((f.sheen ?: f.light).copy(alpha = .7f), .5f, 0f)
        }
    }
}

private fun bodyGrad(f: Tone, x: Float, y: Float, r: Float): Brush =
    Brush.radialGradient(0f to Color.White, .06f to Color.White, .45f to f.light, 1f to f.c, center = Offset(x - r * .3f, y - r * .35f), radius = r * 1.4f)

private fun DrawScope.glow(x: Float, y: Float, r: Float, col: Color, a: Float) {
    val o = Offset(x, y)
    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a.coerceIn(0f, 1f)), col.copy(alpha = 0f)), o, r), r, o)
}

private fun DrawScope.shadow(x: Float, fy: Float, r: Float, lift: Float, col: Color) {
    val a = max(0f, .22f - lift * .12f)
    if (a <= 0f) return
    val e = ovalRect(x, fy, r * max(.4f, 1.5f - lift), r * max(.05f, .28f - lift * .12f))
    drawOval(col.copy(alpha = a), e.topLeft, e.size)
}

private fun DrawScope.burst(x: Float, y: Float, r: Float, t: Float, k: Float) {
    if (k <= 0f) return
    for (i in 0 until 8) {
        val a = i / 8f * TAU + t; val d = r * (1.4f + (1 - k) * 1.6f)
        drawCircle(Color.White.copy(alpha = k.coerceIn(0f, 1f)), r * .06f, Offset(x + cos(a) * d, y + sin(a) * d))
    }
}

private fun DrawScope.face(x0: Float, y0: Float, r: Float, p: Pose, f: Tone) {
    val t = p.t; val m = p.who; val happy = p.k > .05f
    val lookX = if (p.listening) 0f else sin(t * .7f) * r * .06f
    val lookY = (if (m == Companion.DIM) r * .08f else 0f) + (if (p.listening) r * .07f else 0f)
    val blink = if ((t % 3.6f) < .12f) .12f else 1f
    val big = if (m == Companion.PIP) 1.25f else 1f
    val x = x0 + lookX; val y = y0 + lookY
    for (sd in listOf(-1f, 1f)) {
        val cx = x + sd * r * .34f; val cy = y - r * .05f
        if (happy) {
            val rr = r * .12f; val ay = cy + r * .05f
            drawArc(FaceInk, 198f, 144f, false, Offset(cx - rr, ay - rr), Size(rr * 2, rr * 2), style = Stroke(r * .09f, cap = StrokeCap.Round))
            continue
        }
        val e = ovalRect(cx, cy, r * .11f * big, r * .16f * big * blink)
        drawOval(FaceInk, e.topLeft, e.size)
        if (blink > .5f) drawCircle(Color.White, r * .04f * big, Offset(cx - r * .035f, cy - r * .06f))
        when (m) {
            Companion.FIZZ -> drawRect(f.light, Offset(cx - r * .16f, cy - r * .21f), Size(r * .32f, r * .18f))
            Companion.BLAZE -> drawLine(FaceInk, Offset(cx - sd * r * .15f, cy - r * .3f), Offset(cx + sd * r * .12f, cy - r * .2f), r * .08f, StrokeCap.Round)
            Companion.DIM -> drawLine(FaceInk, Offset(cx - sd * r * .12f, cy - r * .24f), Offset(cx + sd * r * .1f, cy - r * .3f), r * .06f, StrokeCap.Round)
            else -> {}
        }
    }
    val blush = if (m == Companion.PIP) Color(255, 120, 140).copy(alpha = .55f) else Color(255, 130, 150).copy(alpha = .35f)
    for (sd in listOf(-1f, 1f)) { val b = ovalRect(x + sd * r * .56f, y + r * .2f, r * .13f, r * .08f); drawOval(blush, b.topLeft, b.size) }

    val mx = x; val my = y + r * .28f
    val st = Stroke(r * .07f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when {
        happy -> { val rr = r * .16f; drawArc(FaceInk, 27f, 126f, false, Offset(mx - rr, my - r * .04f - rr), Size(rr * 2, rr * 2)) }
        p.listening -> { val o = ovalRect(mx, my, r * (.06f + p.lvl * .03f), r * (.07f + p.lvl * .06f)); drawOval(FaceInk, o.topLeft, o.size) }
        m == Companion.PIP -> { val rr = r * .16f; drawArc(FaceInk, 27f, 126f, false, Offset(mx - rr, my - r * .04f - rr), Size(rr * 2, rr * 2)) }
        m == Companion.FIZZ -> drawPath(Path().apply { moveTo(mx - r * .12f, my); quadraticBezierTo(mx + r * .02f, my + r * .06f, mx + r * .14f, my - r * .06f) }, FaceInk, style = st)
        m == Companion.BLAZE -> drawLine(FaceInk, Offset(mx - r * .13f, my + r * .02f), Offset(mx + r * .13f, my + r * .02f), r * .07f, StrokeCap.Round)
        m == Companion.DIM -> { val rr = r * .08f; drawArc(FaceInk, 207f, 126f, false, Offset(mx - rr, my + r * .1f - rr), Size(rr * 2, rr * 2), style = st) }
        else -> { val rr = r * .12f; drawArc(FaceInk, 36f, 108f, false, Offset(mx - rr, my - r * .06f - rr), Size(rr * 2, rr * 2), style = st) }
    }
}

private fun circlePath(x: Float, y: Float, r: Float) = Path().apply { addOval(Rect(Offset(x, y), r)) }

/* ---------------- the six looks ---------------- */

private fun DrawScope.drawStar(p: Pose, f: Tone) {
    val w = size.width; val h = size.height; val t = p.t; val k = p.k
    val r = w * .17f * p.grow
    val x = w / 2 + sin(t * .8f) * w * .05f + p.dx
    val y = h * .46f + sin(t * 1.6f) * h * .03f - k * h * .06f + p.dy
    val rot = sin(t * .9f) * .12f + k * TAU * (1 - k)
    shadow(x, h * .86f, r * .8f, (h * .86f - y) / h, f.c)
    glow(x, y, r * 2.6f, f.c, .45f + k * .4f)
    for (i in 0 until 12) {
        val a = t * .5f + i * 1.3f; val d = r * (1.6f + (i % 3) * .3f)
        drawCircle(f.light.copy(alpha = (.25f + .25f * sin(t * 3 + i)).coerceIn(0f, 1f)), r * .035f, Offset(x - d * .3f + cos(a) * r * .2f, y + d * .5f))
    }
    translate(x, y) {
        rotate(rot * DEG, pivot = Offset.Zero) {
            val star = Path().apply {
                for (i in 0 until 10) {
                    val a = -PIF / 2 + i * PIF / 5; val rr = if (i % 2 == 1) r * .55f else r
                    val sx = cos(a) * rr; val sy = sin(a) * rr
                    if (i == 0) moveTo(sx, sy) else lineTo(sx, sy)
                }
                close()
            }
            drawPath(star, f.light, style = Stroke(r * .32f, join = StrokeJoin.Round))
            drawPath(star, bodyGrad(f, 0f, 0f, r))
            sheen(star, f, 0f, 0f, r, t)
        }
    }
    face(x, y + r * .05f, r * .62f, p, f)
    burst(x, y, r * .9f, t, k)
}

private fun DrawScope.drawSprite(p: Pose, f: Tone) {
    val w = size.width; val h = size.height; val t = p.t; val k = p.k
    val r = w * .13f * p.grow
    val x = w / 2 + sin(t * .8f) * w * .05f + p.dx
    val y = h * .46f + sin(t * 1.6f) * h * .025f - k * h * .06f + p.dy
    shadow(x, h * .86f, r, (h * .86f - y) / h, f.c)
    glow(x, y, r * 3.2f, f.c, .45f + k * .4f)
    val flap = .55f + .45f * abs(sin(t * 16f * p.flapBoost))
    val wings = listOf(floatArrayOf(.75f, 1.35f, .62f, -.35f), floatArrayOf(-.1f, .9f, .4f, .2f))
    for (wing in wings) {
        val ang = wing[0]; val len = wing[1]; val wid = wing[2]; val yo = wing[3]
        for (sd in listOf(-1f, 1f)) {
            translate(x + sd * r * .55f, y + yo * r) {
                rotate(sd * ang * DEG, pivot = Offset.Zero) {
                    scale(1f, flap, pivot = Offset.Zero) {
                        val cx = sd * len * r * .55f; val cy = -len * r * .32f
                        rotate(sd * -.5f * DEG, pivot = Offset(cx, cy)) {
                            val e = ovalRect(cx, cy, len * r * .62f, wid * r)
                            drawOval(Color.White.copy(alpha = .5f), e.topLeft, e.size)
                            drawOval(Color.White.copy(alpha = .8f), e.topLeft, e.size, style = Stroke(r * .03f))
                        }
                    }
                }
            }
        }
    }
    val body = circlePath(x, y, r)
    drawPath(body, bodyGrad(f, x, y, r))
    sheen(body, f, x, y, r, t)
    face(x, y, r, p, f)
    burst(x, y, r, t, k)
}

private fun DrawScope.drawMoth(p: Pose, f: Tone) {
    val w = size.width; val h = size.height; val t = p.t; val k = p.k
    val r = w * .1f * p.grow
    val x = w / 2 + sin(t * .9f) * w * .06f + p.dx
    val y = h * .47f + sin(t * 1.8f) * h * .03f - k * h * .06f + p.dy
    shadow(x, h * .86f, r, (h * .86f - y) / h, f.c)
    glow(x, y + r * .4f, r * 3.4f, f.c, .5f + k * .4f)
    val flap = .5f + .5f * abs(sin(t * 9f * p.flapBoost))
    for (sd in listOf(-1f, 1f)) {
        translate(x, y) {
            scale(sd * (.55f + .45f * flap), 1f, pivot = Offset.Zero) {
                val upper = Path().apply {
                    moveTo(r * .3f, -r * .2f)
                    cubicTo(r * 1.6f, -r * 2.2f, r * 3.1f, -r * 1.0f, r * 2.4f, r * .2f)
                    cubicTo(r * 1.8f, r * .6f, r * .8f, r * .3f, r * .3f, r * .2f)
                    close()
                }
                drawPath(upper, f.light.copy(alpha = .92f))
                sheen(upper, f, r * 1.6f, -r * .6f, r * 1.2f, t)
                val lower = Path().apply {
                    moveTo(r * .3f, r * .3f)
                    cubicTo(r * 1.4f, r * .6f, r * 1.9f, r * 1.6f, r * 1.1f, r * 1.9f)
                    cubicTo(r * .6f, r * 1.7f, r * .4f, r * 1.0f, r * .3f, r * .4f)
                    close()
                }
                drawPath(lower, f.c.copy(alpha = .8f))
                drawCircle(f.dark, r * .28f, Offset(r * 1.8f, -r * .6f))
                drawCircle(Color.White, r * .1f, Offset(r * 1.75f, -r * .65f))
                val vein = Stroke(r * .06f, cap = StrokeCap.Round)
                val veinCol = f.dark.copy(alpha = .8f)
                drawPath(Path().apply { moveTo(r * .25f, -r * .85f); quadraticBezierTo(r * .8f, -r * 1.9f, r * 1.2f, -r * 1.7f) }, veinCol, style = vein)
                for (i in 1 until 5) {
                    drawLine(veinCol, Offset(r * (.25f + i * .2f), -r * (.85f + i * .22f)), Offset(r * (.35f + i * .2f), -r * (.7f + i * .2f)), r * .06f, StrokeCap.Round)
                }
            }
        }
    }
    val by = y + r * .55f
    val body = Path().apply { addOval(ovalRect(x, by, r * .6f, r * 1.05f)) }
    drawPath(body, bodyGrad(f, x, by, r * 1.05f))
    sheen(body, f, x, by, r, t)
    val hy = y - r * .35f
    drawCircle(f.light, r * .78f, Offset(x, hy))
    for (i in 0 until 14) {
        val a = i / 14f * TAU
        drawCircle(Color.White.copy(alpha = .5f), r * .1f, Offset(x + cos(a) * r * .74f, hy + sin(a) * r * .74f))
    }
    face(x, hy, r * .78f, p, f)
    burst(x, y, r * 1.3f, t, k)
}

private fun DrawScope.drawEmber(p: Pose, f: Tone) {
    val w = size.width; val h = size.height; val t = p.t; val k = p.k
    val r = w * .13f * p.grow
    val x = w / 2 + sin(t * .7f) * w * .05f + p.dx
    val y = h * .52f + sin(t * 1.4f) * h * .02f - k * h * .05f + p.dy
    shadow(x, h * .86f, r, (h * .86f - y) / h, f.c)
    glow(x, y - r * .3f, r * 3.4f, f.c, .5f + k * .4f)
    val lean = sin(t * 1.9f) * .25f
    val fl = 1f + sin(t * 11f) * .04f + sin(t * 17f) * .03f + k * .25f
    fun tip(s: Float): Path = Path().apply {
        moveTo(x - r * s, y)
        cubicTo(x - r * s * 1.05f, y - r * s * 1.1f, x + r * lean * s, y - r * s * 1.6f * fl, x + r * lean * s * 1.4f, y - r * s * 2.4f * fl)
        cubicTo(x + r * s * .55f, y - r * s * 1.5f * fl, x + r * s * 1.05f, y - r * s * 1.0f, x + r * s, y)
        arcTo(Rect(Offset(x, y), r * s), 0f, 180f, false)
        close()
    }
    val outer = tip(1.05f)
    drawPath(outer, f.c)
    sheen(outer, f, x, y - r * .8f, r * 1.3f, t)
    drawPath(tip(.85f), f.light)
    drawPath(tip(.55f), Color.White)
    face(x, y - r * .15f, r * .8f, p, f)
    burst(x, y - r * .4f, r, t, k)
}

private fun DrawScope.drawBubble(p: Pose, f: Tone) {
    val w = size.width; val h = size.height; val t = p.t; val k = p.k
    val r = w * .24f * p.grow
    val x = w / 2 + sin(t * .6f) * w * .04f + p.dx
    val y = h * .45f + sin(t * 1.2f) * h * .03f - k * h * .05f + p.dy
    shadow(x, h * .86f, r * .6f, (h * .86f - y) / h, f.c)
    val wob = 1f + sin(t * 3f) * .02f + k * sin(t * 20f) * .05f
    translate(x, y) {
        scale(wob, 2f - wob, pivot = Offset.Zero) {
            val ball = circlePath(0f, 0f, r)
            drawPath(ball, Brush.linearGradient(listOf(f.light.copy(alpha = .3f), f.c.copy(alpha = .1f), f.c.copy(alpha = .28f)), Offset(-r, -r), Offset(r, r)))
            sheen(ball, f, 0f, 0f, r, t)
            drawCircle(Color.White.copy(alpha = .55f), r, Offset.Zero, style = Stroke(r * .025f))
            rotate(-.7f * DEG, pivot = Offset(-r * .45f, -r * .5f)) {
                val e = ovalRect(-r * .45f, -r * .5f, r * .18f, r * .08f)
                drawOval(Color.White.copy(alpha = .7f), e.topLeft, e.size)
            }
        }
    }
    val ir = r * .38f
    val iy = y + r * .18f + sin(t * 2f) * r * .04f
    glow(x, iy, ir * 2.2f, f.c, .45f + k * .3f)
    val inner = circlePath(x, iy, ir)
    drawPath(inner, bodyGrad(f, x, iy, ir))
    sheen(inner, f, x, iy, ir, t)
    face(x, iy, ir, p, f)
    burst(x, y, r * .8f, t, k)
}

private fun DrawScope.drawPuff(p: Pose, f: Tone) {
    val w = size.width; val h = size.height; val t = p.t; val k = p.k
    val r = w * .12f * p.grow
    val x = w / 2 + sin(t * .5f) * w * .06f + p.dx
    val y = h * .42f + sin(t) * h * .02f + p.dy
    if (k > 0f) {
        listOf(Color(0xFFFF8A8A), Color(0xFFFFD27A), Color(0xFF9BE07A), Color(0xFF7AC0FF)).forEachIndexed { i, col ->
            val rr = r * (2.6f - i * .16f); val cy = y + r * 1.4f
            drawArc(col.copy(alpha = (k * .8f).coerceIn(0f, 1f)), 189f, 162f, false, Offset(x - rr, cy - rr), Size(rr * 2, rr * 2), style = Stroke(r * .12f))
        }
    }
    glow(x, y, r * 3.2f, f.c, .35f + k * .3f)
    if (p.who == Companion.DIM || p.who == Companion.BLAZE) for (i in 0 until 5) {
        val dy = ((t * 60f + i * 37f) % 80f) / 80f
        drawRect(f.c.copy(alpha = (.7f * (1 - dy)).coerceIn(0f, 1f)), Offset(x - r * .9f + i * r * .45f, y + r * .7f + dy * h * .25f), Size(r * .06f, r * .18f))
    }
    val puffs = listOf(floatArrayOf(-1f, .1f, .7f), floatArrayOf(-.45f, -.35f, .85f), floatArrayOf(.25f, -.45f, .9f), floatArrayOf(.85f, -.05f, .72f), floatArrayOf(0f, .25f, .95f))
    val cloud = Path().apply {
        for (pf in puffs) addOval(Rect(Offset(x + pf[0] * r + sin(t * 2 + pf[0]) * r * .03f, y + pf[1] * r), pf[2] * r))
    }
    drawPath(cloud, Brush.radialGradient(0f to Color.White, .45f to f.light, 1f to f.c, center = Offset(x - r * .6f, y - r * .8f), radius = r * 2.4f))
    sheen(cloud, f, x, y, r * 1.6f, t)
    val base = ovalRect(x, y + r * .75f, r * 1.6f, r * .2f)
    drawOval(f.dark.copy(alpha = .25f), base.topLeft, base.size)
    face(x, y + r * .05f, r * .85f, p, f)
    burst(x, y, r * 1.2f, t, k)
}

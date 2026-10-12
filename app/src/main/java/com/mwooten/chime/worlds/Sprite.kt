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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.mwooten.chime.R
import com.mwooten.chime.data.Companion
import kotlinx.coroutines.flow.MutableStateFlow

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

private class Mood(val sway: Float, val bob: Float, val speed: Float, val jitter: Float, val size: Float, val flicker: Boolean)

private fun mood(c: Companion) = when (c) {
    Companion.WREN -> Mood(.05f, .025f, .8f, 0f, 1f, false)
    Companion.FIZZ -> Mood(.035f, .02f, .6f, 0f, 1f, false)
    Companion.PIP -> Mood(.06f, .06f, 1.5f, 0f, 1f, false)
    Companion.DIM -> Mood(.03f, .02f, .55f, 0f, .86f, true)
    Companion.BLAZE -> Mood(.04f, .02f, 1.2f, 2.2f, 1.05f, false)
}

/**
 * Your companion as a small glowing sprite: a warm light body, two pairs of petal wings,
 * eyes that blink and look around, cheeks, and a face that matches the personality.
 * It leans in while you talk, and squints, loops and rings when something is saved.
 * Tap to talk; drag it and it follows your finger, then floats back.
 */
@Composable
fun Sprite(who: Companion, level: Float, listening: Boolean, size: Dp, onTap: () -> Unit) {
    var time by remember { mutableFloatStateOf(0f) }
    var chime by remember { mutableFloatStateOf(0f) }
    var lvl by remember { mutableFloatStateOf(0f) }
    var px by remember { mutableFloatStateOf(0f) }
    var py by remember { mutableFloatStateOf(0f) }
    var vx by remember { mutableFloatStateOf(0f) }
    var vy by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableFloatStateOf(0f) }
    val trail = remember { ArrayDeque<Offset>() }
    val strikes by Bell.strikes.collectAsState()

    LaunchedEffect(strikes) { if (strikes != 0L) chime = 1f }
    val curLevel by rememberUpdatedState(level)
    val curListening by rememberUpdatedState(listening)
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f); last = now
                time += dt
                chime = maxOf(0f, chime - dt / 1.3f)
                lvl += ((if (curListening) maxOf(curLevel, 0.1f) else 0f) - lvl) * 0.3f
                if (dragging == 0f) { vx += -px * .08f; vy += -py * .08f; vx *= .82f; vy *= .82f; px += vx; py += vy }
            }
        }
    }

    Canvas(
        Modifier.size(size)
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { dragging = 1f }, onDragEnd = { dragging = 0f }, onDragCancel = { dragging = 0f }) { change, d ->
                    change.consume(); px += d.x * .8f; py += d.y * .8f
                }
            }
    ) {
        val t = time
        val m = mood(who)
        val c = Color(who.color); val light = Color(who.light)
        val w = this.size.width; val h = this.size.height
        val r = w * .13f * m.size * (1f + if (listening) .08f + lvl * .12f else 0f)
        var x = w / 2 + sin(t * m.speed) * w * m.sway + px
        var y = h * .46f + sin(t * m.speed * 2) * h * m.bob + (if (who == Companion.PIP) -abs(sin(t * 3)) * h * .05f else 0f) + py
        if (m.jitter > 0f) { x += (Random.nextFloat() - .5f) * m.jitter * w / 300f; y += (Random.nextFloat() - .5f) * m.jitter * w / 300f }
        if (chime > 0f) { val a = (1 - chime) * PI.toFloat() * 2; x += sin(a) * w * .12f * chime; y += (1 - cos(a)) * -h * .08f * chime }
        if (listening) y += h * .02f

        // floor shadow: smaller the higher it floats
        val fy = h * .86f; val lift = (fy - y) / h
        drawOval(c.copy(alpha = (.22f - lift * .12f).coerceIn(0f, 1f)), Offset(x - r * (1.5f - lift), fy - r * (.28f - lift * .12f)), Size(r * (1.5f - lift) * 2, r * (.28f - lift * .12f).coerceAtLeast(.02f) * 2))

        // sparkle trail
        trail.addLast(Offset(x, y)); if (trail.size > 26) trail.removeFirst()
        trail.forEachIndexed { i, p ->
            val a = (i.toFloat() / trail.size) * .55f
            drawCircle(light.copy(alpha = a), r * .06f * (i.toFloat() / trail.size + .3f), Offset(p.x + sin(i * 1.7f + t * 3) * r * .25f, p.y + r * .4f + i * .4f))
        }

        val flick = if (m.flicker && sin(t * 7) > .93f) .55f else 1f
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = ((.5f + lvl * .3f + chime * .4f) * flick).coerceIn(0f, 1f)), Color.Transparent), Offset(x, y), r * 3.2f), r * 3.2f, Offset(x, y))

        // wings
        val flap = .55f + .45f * abs(sin(t * (if (listening) 24f else 16f)))
        spriteWing(x, y, r, .75f, 1.35f, .62f, -.35f, flap, light)
        spriteWing(x, y, r, -.1f, .9f, .4f, .2f, flap, light)

        // body
        drawCircle(Brush.radialGradient(listOf(Color.White, light, c), Offset(x - r * .3f, y - r * .35f), r * 1.1f), r, Offset(x, y), alpha = flick)

        // face
        val ink = Color(0xFF2A2233)
        val happy = chime > .05f
        val lookX = if (listening) 0f else sin(t * .7f) * r * .06f
        val lookY = if (listening) r * .06f else if (who == Companion.DIM) r * .08f else 0f
        val blink = if ((t % 3.6f) < .12f) .12f else 1f
        val big = if (who == Companion.PIP) 1.25f else if (listening) 1.15f else 1f
        for (s in listOf(-1f, 1f)) {
            val ex = x + s * r * .34f + lookX; val ey = y - r * .05f + lookY
            if (happy) {
                drawArc(ink, 198f, 144f, false, Offset(ex - r * .12f, ey + r * .05f - r * .12f), Size(r * .24f, r * .24f), style = Stroke(r * .09f, cap = StrokeCap.Round))
                continue
            }
            drawOval(ink, Offset(ex - r * .11f * big, ey - r * .16f * big * blink), Size(r * .22f * big, r * .32f * big * blink))
            if (blink > .5f) drawCircle(Color.White, r * .04f * big, Offset(ex - r * .035f, ey - r * .06f))
            when (who) {
                Companion.FIZZ -> drawRect(light, Offset(ex - r * .16f, ey - r * .2f), Size(r * .32f, r * .17f))
                Companion.BLAZE -> drawLine(ink, Offset(ex - s * r * .15f, ey - r * .3f), Offset(ex + s * r * .12f, ey - r * .2f), r * .08f, StrokeCap.Round)
                Companion.DIM -> drawLine(ink, Offset(ex - s * r * .12f, ey - r * .24f), Offset(ex + s * r * .1f, ey - r * .3f), r * .06f, StrokeCap.Round)
                else -> {}
            }
        }
        val blush = if (who == Companion.PIP) Color(0x8CFF788C) else Color(0x59FF8296)
        for (s in listOf(-1f, 1f)) drawOval(blush, Offset(x + s * r * .56f + lookX - r * .13f, y + r * .2f + lookY - r * .08f), Size(r * .26f, r * .16f))

        // mouth
        val mx = x + lookX; val my = y + r * .28f + lookY
        val st = Stroke(r * .07f, cap = StrokeCap.Round)
        when {
            happy || who == Companion.PIP -> drawArc(ink, 27f, 126f, true, Offset(mx - r * .16f, my - r * .04f - r * .16f), Size(r * .32f, r * .32f))
            listening -> drawOval(ink, Offset(mx - r * .06f, my - (r * .07f + lvl * r * .05f)), Size(r * .12f, (r * .07f + lvl * r * .05f) * 2))
            who == Companion.FIZZ -> drawPath(Path().apply { moveTo(mx - r * .12f, my); quadraticBezierTo(mx + r * .02f, my + r * .06f, mx + r * .14f, my - r * .06f) }, ink, style = st)
            who == Companion.BLAZE -> drawLine(ink, Offset(mx - r * .13f, my + r * .02f), Offset(mx + r * .13f, my + r * .02f), r * .07f, StrokeCap.Round)
            who == Companion.DIM -> drawArc(ink, 207f, 126f, false, Offset(mx - r * .08f, my + r * .1f - r * .08f), Size(r * .16f, r * .16f), style = st)
            else -> drawArc(ink, 36f, 108f, false, Offset(mx - r * .12f, my - r * .06f - r * .12f), Size(r * .24f, r * .24f), style = st)
        }

        // sparkle burst on a chime
        if (chime > 0f) for (i in 0 until 8) {
            val a = i / 8f * PI.toFloat() * 2 + t; val d = r * (1.4f + (1 - chime) * 1.6f)
            drawCircle(Color.White.copy(alpha = chime), r * .06f, Offset(x + cos(a) * d, y + sin(a) * d))
        }
    }
}

/** One pair of petal wings, mirrored, flapping by squashing vertically. */
private fun DrawScope.spriteWing(x: Float, y: Float, r: Float, ang: Float, len: Float, wid: Float, yo: Float, flap: Float, light: Color) {
    for (side in listOf(-1f, 1f)) {
        translate(x + side * r * .55f, y + yo * r) {
            rotate(side * ang * 57.2958f, pivot = Offset.Zero) {
                scale(1f, flap, pivot = Offset.Zero) {
                    val cx = side * len * r * .55f; val cy = -len * r * .32f
                    val rw = len * r * .62f; val rh = wid * r
                    rotate(side * -.5f * 57.2958f, pivot = Offset(cx, cy)) {
                        val rect = Rect(cx - rw, cy - rh, cx + rw, cy + rh)
                        drawOval(Brush.linearGradient(listOf(Color.White.copy(alpha = .75f), light.copy(alpha = .35f)), Offset(0f, 0f), Offset(side * len * r, -len * r * .4f)), rect.topLeft, rect.size)
                        drawOval(Color.White.copy(alpha = .7f), rect.topLeft, rect.size, style = Stroke(r * .03f))
                    }
                }
            }
        }
    }
}

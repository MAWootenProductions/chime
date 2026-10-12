package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val RainforestMist = Color(0xFFF2F6EA)
private val RainforestVineGreen = Color(0xFF4A7A3A)
private val RainforestTag = Color(0xFFE8DCB8)
private val RainforestGold = Color(0xFFFFD27A)

/** Where each band starts, as a fraction of the scene below the status bar: emergent, canopy, understory, floor. */
private val RainforestBands = floatArrayOf(0.06f, 0.24f, 0.55f, 0.84f)
private val RainforestBandNames = listOf("EMERGENT" to "someday", "CANOPY" to "this week", "UNDERSTORY" to "today", "FOREST FLOOR" to "now")

/** 0 emergent (someday/later), 1 canopy (this week), 2 understory (today), 3 forest floor (overdue or within the hour). */
private fun rainforestBand(q: Quest, now: Long): Int {
    val shelf = q.shelf(now)
    val m = q.minutesLeft(now)
    return when {
        shelf == Shelf.OVERDUE -> 3
        !q.allDay && m != null && m <= 60 -> 3
        shelf == Shelf.TODAY -> 2
        shelf == Shelf.TOMORROW || shelf == Shelf.WEEK -> 1
        else -> 0
    }
}

/** Rainforest: quests hang on vines through the forest's layers and drop lower as time runs out. */
@Composable
fun RainforestWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val name = s.companion.title
    val clock = rememberClock()
    var planting by rememberSaveable { mutableStateOf(false) }
    val work = TextStyle(fontFamily = Fonts.workSans, color = RainforestMist)
    val hung: List<Quest> = (listOfNotNull(q) + s.open.filter { it.id != q?.id }).take(5)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color(0xFFD8ECC8), 0.14f to Color(0xFF6AA06A), 0.38f to Color(0xFF2E6A44),
                0.64f to Color(0xFF1A4A30), 0.86f to Color(0xFF0E2A1C), 1f to Color(0xFF2A1E12),
            )
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val w = maxWidth
                val sceneH = maxHeight - topInset
                fun bandTop(i: Int): Dp = topInset + sceneH * RainforestBands[i]
                fun bandBottom(i: Int): Dp = if (i == 3) maxHeight else bandTop(i + 1)
                val vineTop = topInset + 34.dp

                // strata lines and the canopy leaves along the top
                Canvas(Modifier.fillMaxSize()) {
                    for (i in 1..3) {
                        val y = bandTop(i).toPx()
                        drawLine(
                            RainforestMist.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f),
                        )
                    }
                    val k = size.width / 390f
                    val dy = topInset.toPx() * 0.5f
                    val canopy = Path().apply {
                        moveTo(-10f * k, 0f); lineTo(400f * k, 0f); lineTo(400f * k, 40f * k + dy)
                        cubicTo(360f * k, 70f * k + dy, 320f * k, 30f * k + dy, 280f * k, 60f * k + dy)
                        cubicTo(240f * k, 90f * k + dy, 200f * k, 40f * k + dy, 160f * k, 70f * k + dy)
                        cubicTo(120f * k, 96f * k + dy, 80f * k, 50f * k + dy, 40f * k, 76f * k + dy)
                        cubicTo(20f * k, 88f * k + dy, 0f, 70f * k + dy, -10f * k, 80f * k + dy)
                        close()
                    }
                    drawPath(canopy, Color(0xFF1E5A36))
                    val leafA = Path().apply {
                        moveTo(30f * k, 0f); cubicTo(60f * k, 30f * k + dy, 90f * k, 40f * k + dy, 120f * k, 30f * k + dy)
                        cubicTo(100f * k, 60f * k + dy, 70f * k, 66f * k + dy, 40f * k, 58f * k + dy); close()
                    }
                    val leafB = Path().apply {
                        moveTo(250f * k, 0f); cubicTo(280f * k, 26f * k + dy, 320f * k, 30f * k + dy, 350f * k, 20f * k + dy)
                        cubicTo(330f * k, 52f * k + dy, 290f * k, 58f * k + dy, 262f * k, 48f * k + dy); close()
                    }
                    drawPath(leafA, Color(0xFF2E7A4A))
                    drawPath(leafB, Color(0xFF2E7A4A))
                }

                // band labels: tap any layer to open the full log
                for (i in 0..3) {
                    val (title, sub) = RainforestBandNames[i]
                    Column(
                        Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = bandTop(i) + 8.dp).tap(a::openLog),
                        horizontalAlignment = Alignment.End,
                    ) {
                        val tone = if (i == 0) Color(0xB31E3C28) else RainforestMist.copy(alpha = 0.6f)
                        Text(title, style = work.copy(fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 2.sp, color = tone))
                        Text(sub, style = work.copy(fontSize = 10.sp, fontWeight = FontWeight.W500, color = tone, textAlign = TextAlign.End))
                    }
                }

                // vines with quest tags; the lower the tag, the sooner it's due
                val n = hung.size
                val perBandSeen = IntArray(4)
                for ((i, hq) in hung.withIndex()) {
                    val band = rainforestBand(hq, s.now)
                    val nth = perBandSeen[band]++
                    val big = band == 3
                    val tagW = if (big) 108.dp else 72.dp
                    val tagH = if (big) 58.dp else 44.dp
                    val room = (bandBottom(band) - bandTop(band) - tagH).coerceAtLeast(0.dp)
                    val frac = if (big) 0.12f else if (nth % 2 == 0) 0.18f else 0.62f
                    val tagTop = bandTop(band) + room * frac
                    val len = (tagTop - vineTop).coerceAtLeast(10.dp)
                    val cx = if (n <= 1) w * 0.3f else w * (0.16f + 0.56f * i / (n - 1))
                    val left = (cx - tagW / 2).coerceAtLeast(8.dp)
                    RainforestVine(
                        quest = hq, focus = hq.id == q?.id, big = big, tagW = tagW, len = len, clock = clock,
                        period = 5f + i * 0.7f, phase = i * 1.3f,
                        bird = s.companion.c, birdWing = s.companion.cDark, work = work,
                        modifier = Modifier.offset(x = left, y = vineTop),
                        onClick = { a.openQuest(hq) },
                    )
                }
                if (q == null) {
                    // nothing hanging: the bird rests on the canopy
                    Canvas(Modifier.offset(x = w * 0.3f, y = topInset + 46.dp).size(40.dp, 30.dp)) {
                        rainforestBird(s.companion.c, s.companion.cDark)
                    }
                }

                // a blue morpho and rain dripping from the leaf tips
                Canvas(Modifier.fillMaxSize()) {
                    val t = clock.value
                    val drips = listOf(Triple(120f, 60f, 2.4f), Triple(290f, 50f, 3.1f), Triple(210f, 70f, 2.8f))
                    for ((j, d) in drips.withIndex()) {
                        val p = ((t + j * 1f) % d.third) / d.third
                        val alpha = if (p < 0.1f) 0.8f * p / 0.1f else 0.8f
                        val x = d.first / 390f * size.width
                        val y = topInset.toPx() + d.second.dp.toPx() - 20.dp.toPx() + p * (size.height + 20.dp.toPx())
                        drawRect(Color(0xFFDCF0FF).copy(alpha = alpha * 0.9f), topLeft = Offset(x, y), size = Size(2.dp.toPx(), 10.dp.toPx()))
                    }
                    val (mx, my) = rainforestMorpho(t)
                    val bx = size.width * 0.51f + mx.dp.toPx()
                    val by = topInset.toPx() + (size.height - topInset.toPx()) * 0.7f + my.dp.toPx()
                    val flap = 0.6f + 0.4f * cos(2f * PI.toFloat() * t / 0.35f)
                    val ww = 16.dp.toPx() * flap
                    val wh = 20.dp.toPx()
                    val blue = Brush.radialGradient(
                        listOf(Color(0xFF6AE0FF), Color(0xFF1A6AD8)),
                        center = Offset(bx, by + wh * 0.4f), radius = 16.dp.toPx(),
                    )
                    drawOval(blue, topLeft = Offset(bx - ww, by), size = Size(ww, wh))
                    drawOval(blue, topLeft = Offset(bx, by), size = Size(ww, wh))
                    drawLine(Color(0xFF10203A), Offset(bx, by + 3.dp.toPx()), Offset(bx, by + wh - 3.dp.toPx()), 2.dp.toPx())
                }

                // field log and options, tucked under the leaves
                Row(
                    Modifier.offset(x = 12.dp, y = topInset + 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RainforestChip("Field log", work, a::openLog)
                    RainforestChip("Options", work, a::openOptions)
                }
            }

            // the companion's line, and planting a seed = spill
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp)
            ) {
                Text(
                    buildAnnotatedString {
                        append("“${s.line}” ")
                        withStyle(SpanStyle(fontFamily = Fonts.workSans, fontSize = 11.sp, fontWeight = FontWeight.W700, letterSpacing = 2.sp, color = s.companion.cLight)) {
                            append(name.uppercase())
                        }
                    },
                    style = TextStyle(fontFamily = Fonts.youngSerif, fontSize = 17.sp, lineHeight = 23.sp, color = RainforestMist),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (q != null) Modifier.tap { a.openQuest(q) } else Modifier,
                )
                if (q != null) {
                    Text(
                        "Tie it higher for an hour",
                        style = work.copy(fontSize = 12.sp, fontWeight = FontWeight.W600, color = Color(0xFFB8D8A0)),
                        modifier = Modifier.padding(top = 6.dp).tap { a.snooze(q, 60) },
                    )
                }
                if (planting || q == null) {
                    Spacer(Modifier.height(12.dp))
                    SpillInput(
                        onSpill = { a.spill(it); planting = false },
                        textStyle = work.copy(fontSize = 16.sp, fontWeight = FontWeight.W500),
                        placeholder = "pay rent by friday",
                        placeholderColor = RainforestMist.copy(alpha = 0.45f),
                        cursor = RainforestGold,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                            .background(RainforestMist.copy(alpha = 0.1f))
                            .padding(start = 16.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                    ) { submit ->
                        Text(
                            "Plant",
                            style = work.copy(fontSize = 14.sp, fontWeight = FontWeight.W700, color = Color(0xFF2A1A06)),
                            modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(18.dp)).background(RainforestGold)
                                .tap(submit).padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (q != null) {
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(RainforestGold)
                                .tap { a.done(q) }.padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Cut it down (done)", style = work.copy(fontSize = 14.sp, fontWeight = FontWeight.W700, color = Color(0xFF2A1A06)), maxLines = 1)
                        }
                    }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(RainforestMist.copy(alpha = 0.14f))
                            .border(1.dp, RainforestMist.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                            .tap { planting = !planting }.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Plant a seed", style = work.copy(fontSize = 14.sp, fontWeight = FontWeight.W700), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RainforestChip(label: String, work: TextStyle, onClick: () -> Unit) {
    Text(
        label,
        style = work.copy(fontSize = 11.sp, fontWeight = FontWeight.W700, letterSpacing = 1.sp),
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x990E2A1C))
            .tap(onClick).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun RainforestVine(
    quest: Quest,
    focus: Boolean,
    big: Boolean,
    tagW: Dp,
    len: Dp,
    clock: State<Float>,
    period: Float,
    phase: Float,
    bird: Color,
    birdWing: Color,
    work: TextStyle,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.width(tagW).graphicsLayer {
            rotationZ = 1.5f * sin(2f * PI.toFloat() * clock.value / period + phase)
            transformOrigin = TransformOrigin(0.5f, 0f)
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(3.dp).height(len).background(RainforestVineGreen))
            val tagColor = if (focus) RainforestGold else RainforestTag
            val shape = RoundedCornerShape(2.dp, 2.dp, if (big) 16.dp else 12.dp, if (big) 16.dp else 12.dp)
            Column(
                Modifier.width(tagW)
                    .drawBehind {
                        if (focus) {
                            val r = size.maxDimension * 0.9f
                            drawCircle(
                                Brush.radialGradient(listOf(RainforestGold.copy(alpha = 0.5f), Color.Transparent), center = center, radius = r),
                                radius = r, center = center,
                            )
                        }
                    }
                    .clip(shape).background(tagColor).tap(onClick)
                    .padding(horizontal = if (big) 6.dp else 4.dp, vertical = if (big) 10.dp else 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val ink = if (focus) Color(0xFF2A1A06) else Color(0xFF2A3A20)
                Text(
                    quest.text,
                    style = work.copy(fontSize = if (big) 15.sp else 12.sp, fontWeight = if (big) FontWeight.W700 else FontWeight.W600, color = ink, textAlign = TextAlign.Center, lineHeight = if (big) 18.sp else 14.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    quest.shortDue(),
                    style = work.copy(fontSize = if (big) 13.sp else 11.sp, fontWeight = if (big) FontWeight.W600 else FontWeight.W500, color = ink, textAlign = TextAlign.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (focus) {
            // the companion, a small bird perched on the urgent vine
            Canvas(
                Modifier.offset(x = tagW / 2 - 4.dp, y = (len * 0.6f - 26.dp).coerceAtLeast(0.dp)).size(40.dp, 30.dp)
            ) { rainforestBird(bird, birdWing) }
        }
    }
}

/** The bird from the 60x48 design, in the companion's colors. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.rainforestBird(body: Color, wing: Color) {
    val kx = size.width / 60f
    val ky = size.height / 48f
    val torso = Path().apply {
        moveTo(8f * kx, 30f * ky)
        cubicTo(8f * kx, 18f * ky, 22f * kx, 12f * ky, 34f * kx, 16f * ky)
        cubicTo(42f * kx, 18f * ky, 46f * kx, 26f * ky, 44f * kx, 34f * ky)
        cubicTo(36f * kx, 40f * ky, 18f * kx, 40f * ky, 8f * kx, 30f * ky)
        close()
    }
    drawPath(torso, body)
    val tail = Path().apply { moveTo(40f * kx, 22f * ky); lineTo(56f * kx, 4f * ky); lineTo(50f * kx, 22f * ky); close() }
    drawPath(tail, wing)
    val w = Path().apply {
        moveTo(18f * kx, 24f * ky)
        cubicTo(26f * kx, 18f * ky, 36f * kx, 20f * ky, 40f * kx, 28f * ky)
        cubicTo(32f * kx, 32f * ky, 24f * kx, 32f * ky, 18f * kx, 24f * ky)
        close()
    }
    drawPath(w, wing)
    drawCircle(body, radius = 7f * kx, center = Offset(12f * kx, 20f * ky))
    val beak = Path().apply { moveTo(6f * kx, 18f * ky); lineTo(0f, 20f * ky); lineTo(6f * kx, 21f * ky); close() }
    drawPath(beak, Color(0xFF3A2A1A))
    drawCircle(Color(0xFF111111), radius = 1.4f * kx, center = Offset(10f * kx, 19f * ky))
}

/** The morpho's 9s loop: (0,0) → (40,-20) → (90,10) → (50,30) → back, eased between keyframes. */
private fun rainforestMorpho(t: Float): Pair<Float, Float> {
    val keys = listOf(0f to 0f, 40f to -20f, 90f to 10f, 50f to 30f, 0f to 0f)
    val p = (t % 9f) / 9f * 4f
    val i = p.toInt().coerceIn(0, 3)
    val f = p - i
    val e = 0.5f - 0.5f * cos(PI.toFloat() * f)
    val (x0, y0) = keys[i]
    val (x1, y1) = keys[i + 1]
    return (x0 + (x1 - x0) * e) to (y0 + (y1 - y0) * e)
}

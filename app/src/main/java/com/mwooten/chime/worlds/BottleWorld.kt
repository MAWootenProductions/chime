package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.sin

private val BottleNavy = Color(0xFF1E3A48)
private val BottleSea = Color(0xFF2F7A9A)
private val BottleSand = Color(0xFFE8D4A8)
private val BottleParchment = Color(0xFFF4E8C8)

/** Message in a Bottle: a beach at low tide; the note has to be dealt with before the tide line. */
@Composable
fun BottleWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val name = s.companion.title
    val clock = rememberClock()
    var tossing by rememberSaveable { mutableStateOf(false) }
    val josefin = TextStyle(fontFamily = Fonts.josefinSans, fontWeight = FontWeight.W700, color = BottleNavy)
    val others = (s.open.size - if (q != null) 1 else 0).coerceAtLeast(0)
    val glowColor = s.companion.cLight

    val due: Long? = q?.due
    val tideLabel = when {
        q == null -> "CALM SEAS · NOTHING ON THE TIDE"
        due == null -> "NO TIDE FOR THIS ONE · SOMEDAY"
        q.allDay -> "TIDE REACHES HERE ${Time.dayName(Time.date(due)).uppercase()}"
        due < s.now -> "TIDE CAME IN AT ${Time.time(due)}"
        else -> "TIDE REACHES HERE AT ${Time.time(due)}"
    }
    val days = if (q != null) ChronoUnit.DAYS.between(Time.date(q.created), Time.date(s.now)).coerceAtLeast(0) else 0L
    val atSea = when (days) {
        0L -> "FOUND ON SHORE · WASHED UP TODAY"
        1L -> "FOUND ON SHORE · 1 DAY AT SEA"
        else -> "FOUND ON SHORE · $days DAYS AT SEA"
    }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BottleSand, Color(0xFFDCC394))))) {
        // sky and sea
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color(0xFFF6C99A),
                        0.40f to Color(0xFFF2A98A),
                        0.67f to Color(0xFF5AA0C0),
                        1f to BottleSea,
                    )
                )
                // low sun, half under the horizon line
                val r = 46.dp.toPx()
                val baseY = size.height * 0.56f
                drawArc(
                    Color(0xFFFFE6B0), startAngle = 180f, sweepAngle = 180f, useCenter = true, alpha = 0.9f,
                    topLeft = Offset(size.width / 2f - r, baseY - r), size = Size(r * 2f, r * 2f),
                )
            }
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Low tide · ${Time.time(s.now)}",
                        style = josefin.copy(fontSize = 13.sp, color = Color(0xFF6A3A2A)),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Text(
                        if (others == 1) "1 more bottle washing in" else "$others more bottles washing in",
                        style = josefin.copy(fontSize = 13.sp, color = Color(0xFF6A3A2A), textAlign = TextAlign.End),
                        modifier = Modifier.tap(a::openLog),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    BottleChip("Tide chart", a::openLog)
                    BottleChip("Harbor", a::openOptions)
                }
            }
        }

        // the surf line, washing in and out over the sand
        Canvas(Modifier.fillMaxWidth().height(56.dp)) {
            val k = size.width / 390f
            val wash = 14.dp.toPx() * (0.5f - 0.5f * kotlin.math.cos(2f * PI.toFloat() * (clock.value % 4f) / 4f))
            drawRect(BottleSea, size = Size(size.width, wash + 1f))
            val top = Path().apply { bottleSurf(k, wash) }
            val fill = Path().apply {
                bottleSurf(k, wash)
                lineTo(size.width, wash)
                lineTo(0f, wash)
                close()
            }
            drawPath(fill, BottleSea)
            drawPath(top, Color.White, alpha = 0.85f, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
            val wet = Path().apply {
                moveTo(0f, 38f * k + wash)
                cubicTo(50f * k, 30f * k + wash, 90f * k, 48f * k + wash, 140f * k, 40f * k + wash)
                cubicTo(190f * k, 32f * k + wash, 240f * k, 50f * k + wash, 290f * k, 42f * k + wash)
                cubicTo(330f * k, 36f * k + wash, 360f * k, 44f * k + wash, 390f * k, 40f * k + wash)
            }
            drawPath(wet, Color(0xFFC8B488), alpha = 0.6f, style = Stroke(width = 2.dp.toPx()))
        }

        // the beach
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 18.dp)) {
            // the tide mark, scratched into the sand
            Canvas(Modifier.fillMaxWidth().padding(horizontal = 2.dp).height(2.dp)) {
                drawLine(
                    BottleNavy.copy(alpha = 0.45f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
                )
            }
            Text(
                tideLabel,
                style = josefin.copy(fontSize = 12.sp, letterSpacing = 1.sp, color = BottleNavy.copy(alpha = 0.7f), textAlign = TextAlign.End),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // the bottle, rocking, with the companion's light glowing inside
            Box(Modifier.fillMaxWidth().height(78.dp)) {
                Canvas(
                    Modifier.padding(start = 42.dp, top = 4.dp).size(180.dp, 70.dp)
                        .graphicsLayer {
                            rotationZ = -22f + 2f * sin(2f * PI.toFloat() * clock.value / 3f)
                            transformOrigin = TransformOrigin(0.5f, 0.8f)
                        }
                ) { drawBottle(glowColor, bottleGlow(clock.value)) }
            }

            // the note, unrolled
            Column(
                Modifier.fillMaxWidth().padding(top = 6.dp)
                    .rotate(1f)
                    .shadow(8.dp)
                    .background(Brush.verticalGradient(listOf(BottleParchment, Color(0xFFE6D4A6))))
                    .then(if (q != null) Modifier.tap { a.openQuest(q) } else Modifier)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text(
                    if (q != null) atSea else "THE SHORE IS CLEAR",
                    style = josefin.copy(fontSize = 11.sp, letterSpacing = 2.sp, color = Color(0xFF8A6A3A)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${s.line} — $name (the light in the bottle)",
                    style = TextStyle(fontFamily = Fonts.gaegu, fontSize = 19.sp, lineHeight = 24.sp, color = Color(0xFF3A2A1A)),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
                if (q != null) {
                    Text(
                        "Throw it back for an hour",
                        style = josefin.copy(fontSize = 12.sp, color = Color(0xFF8A6A3A)),
                        modifier = Modifier.align(Alignment.End).padding(top = 6.dp).tap { a.snooze(q, 60) },
                    )
                }
            }

            if (tossing || q == null) {
                Spacer(Modifier.height(14.dp))
                SpillInput(
                    onSpill = { a.spill(it); tossing = false },
                    textStyle = TextStyle(fontFamily = Fonts.gaegu, fontSize = 21.sp, color = Color(0xFF3A2A1A)),
                    placeholder = "pay rent by friday",
                    placeholderColor = Color(0xFF9A8A6A),
                    cursor = BottleNavy,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        .background(BottleParchment.copy(alpha = 0.85f)).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                ) { submit ->
                    Text(
                        "Cork it",
                        style = josefin.copy(fontSize = 14.sp, color = BottleParchment),
                        modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFB0824A))
                            .tap(submit).padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (q != null) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(BottleNavy)
                            .tap { a.done(q) }.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Done, beach it", style = josefin.copy(fontSize = 15.sp, color = BottleParchment), maxLines = 1) }
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.5f))
                        .tap { tossing = !tossing }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Toss a new bottle", style = josefin.copy(fontSize = 15.sp), maxLines = 1) }
            }
        }
    }
}

private fun Path.bottleSurf(k: Float, wash: Float) {
    moveTo(0f, 20f * k + wash)
    cubicTo(40f * k, 10f * k + wash, 80f * k, 34f * k + wash, 120f * k, 22f * k + wash)
    cubicTo(170f * k, 8f * k + wash, 210f * k, 34f * k + wash, 260f * k, 24f * k + wash)
    cubicTo(310f * k, 14f * k + wash, 350f * k, 30f * k + wash, 390f * k, 20f * k + wash)
}

@Composable
private fun BottleChip(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = TextStyle(fontFamily = Fonts.josefinSans, fontWeight = FontWeight.W700, fontSize = 12.sp, color = Color(0xFF6A3A2A)),
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.35f))
            .tap(onClick).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** The glow keyframes: .35 → .85 (40%) → .2 (44%) → .8 (48%) → .35, over 3.4s. A light that flickers. */
private fun bottleGlow(t: Float): Float {
    val p = (t % 3.4f) / 3.4f
    fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
    return when {
        p < 0.40f -> lerp(0.35f, 0.85f, p / 0.40f)
        p < 0.44f -> lerp(0.85f, 0.2f, (p - 0.40f) / 0.04f)
        p < 0.48f -> lerp(0.2f, 0.8f, (p - 0.44f) / 0.04f)
        else -> lerp(0.8f, 0.35f, (p - 0.48f) / 0.52f)
    }
}

private fun DrawScope.drawBottle(glow: Color, glowAlpha: Float) {
    val k = size.width / 180f
    drawOval(Color.Black.copy(alpha = 0.18f), topLeft = Offset(10f * k, 61f * k), size = Size(160f * k, 10f * k))
    val glass = Path().apply {
        moveTo(10f * k, 18f * k)
        cubicTo(10f * k, 6f * k, 22f * k, 2f * k, 40f * k, 2f * k)
        lineTo(120f * k, 2f * k)
        cubicTo(132f * k, 2f * k, 136f * k, 12f * k, 150f * k, 14f * k)
        lineTo(164f * k, 16f * k)
        lineTo(164f * k, 46f * k)
        lineTo(150f * k, 48f * k)
        cubicTo(136f * k, 50f * k, 132f * k, 60f * k, 120f * k, 60f * k)
        lineTo(40f * k, 60f * k)
        cubicTo(22f * k, 60f * k, 10f * k, 56f * k, 10f * k, 44f * k)
        close()
    }
    drawPath(glass, Color(0x8C50966E))
    drawPath(glass, Color(0xB3285A3C), style = Stroke(width = 2f * k))
    // cork
    drawRoundRect(Color(0xFFB0824A), topLeft = Offset(164f * k, 18f * k), size = Size(14f * k, 26f * k), cornerRadius = CornerRadius(3f * k))
    // the rolled note inside
    drawRoundRect(Color(0xE6EFE2C2), topLeft = Offset(40f * k, 16f * k), size = Size(80f * k, 30f * k), cornerRadius = CornerRadius(15f * k))
    val ink = Color(0xFF8A7A5A)
    drawLine(ink, Offset(48f * k, 26f * k), Offset(108f * k, 26f * k), 1.2f * k)
    drawLine(ink, Offset(48f * k, 32f * k), Offset(98f * k, 32f * k), 1.2f * k)
    drawLine(ink, Offset(48f * k, 38f * k), Offset(104f * k, 38f * k), 1.2f * k)
    // glass highlight
    val hi = Path().apply {
        moveTo(20f * k, 10f * k)
        cubicTo(30f * k, 6f * k, 60f * k, 6f * k, 120f * k, 6f * k)
    }
    drawPath(hi, Color.White.copy(alpha = 0.6f), style = Stroke(width = 3f * k, cap = StrokeCap.Round))
    // the companion, a small light in the bottle
    val c = Offset(35f * k, 35f * k)
    val r = 13f * k
    drawCircle(
        Brush.radialGradient(0f to glow.copy(alpha = 0.95f), 0.65f to glow.copy(alpha = 0f), center = c, radius = r),
        radius = r, center = c, alpha = glowAlpha,
    )
}

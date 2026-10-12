package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.tan

private val AirplaneInk = Color(0xFF2A2A3A)
private val AirplaneBlue = Color(0xFF2A4AA8)
private val AirplaneGray = Color(0xFF8A8A9A)

/** The design's flight path, as three cubic segments in 390-wide coordinates. */
private val AirplaneFlight = listOf(
    floatArrayOf(400f, 30f, 300f, 20f, 260f, 120f, 200f, 100f),
    floatArrayOf(200f, 100f, 140f, 80f, 170f, 20f, 230f, 40f),
    floatArrayOf(230f, 40f, 280f, 60f, 220f, 170f, 120f, 190f),
)

private val AirplaneFolded = listOf(
    Color(0xFFFFF8D8) to Color(0xFFF0E8C0),
    Color(0xFFE0F0FF) to Color(0xFFCFE2F8),
    Color(0xFFFFE4E4) to Color(0xFFF4CFCF),
    Color(0xFFE6F6DC) to Color(0xFFD2E8C4),
)

/** Paper Airplanes: notes passed in class on ruled paper. The landed one is unfolded; check one. */
@Composable
fun AirplaneWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val name = s.companion.title
    val clock = rememberClock()
    val hand = TextStyle(fontFamily = Fonts.patrickHand, color = AirplaneInk)
    val marker = TextStyle(fontFamily = Fonts.permanentMarker)
    val others = s.open.filter { it.id != q?.id }
    val date = Time.ldt(s.now).format(DateTimeFormatter.ofPattern("EEE M/d", Locale.getDefault()))
    val doodle = s.companion.c

    Box(Modifier.fillMaxSize().background(Color(0xFFF9F7EF))) {
        // ruled notebook paper: blue lines, red margin, binder holes
        Canvas(Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            var y = 31.dp.toPx()
            while (y < size.height) {
                drawLine(Color(0xFFBCD0EA), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += step
            }
            drawLine(Color(0xFFE8A0A0), Offset(53.dp.toPx(), 0f), Offset(53.dp.toPx(), size.height), 2.dp.toPx())
            for (f in listOf(0.18f, 0.5f, 0.82f)) {
                val c = Offset(27.dp.toPx(), size.height * f)
                drawCircle(Color(0xFF3A4A5A), radius = 9.dp.toPx(), center = c)
                drawCircle(Color.Black.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = Offset(c.x, c.y + 1.dp.toPx()))
            }
        }

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // header
            Row(
                Modifier.fillMaxWidth().padding(start = 66.dp, end = 20.dp, top = 14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    date,
                    style = marker.copy(fontSize = 26.sp, color = AirplaneBlue),
                    modifier = Modifier.weight(1f).tap(a::openLog),
                    maxLines = 1,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (s.open.size == 1) "1 note passed" else "${s.open.size} notes passed",
                        style = hand.copy(fontSize = 18.sp, color = AirplaneGray),
                        modifier = Modifier.tap(a::openLog),
                    )
                    Text(
                        "options",
                        style = hand.copy(fontSize = 15.sp, color = AirplaneBlue),
                        modifier = Modifier.tap(a::openOptions).drawBehind {
                            drawLine(AirplaneBlue, Offset(0f, size.height - 2.dp.toPx()), Offset(size.width, size.height - 3.dp.toPx()), 1.5.dp.toPx())
                        },
                    )
                }
            }

            // a plane in flight, looping in from the window
            Canvas(Modifier.fillMaxWidth().weight(1f).heightIn(min = 40.dp)) {
                val k = size.width / 390f
                val sy = minOf(k, size.height / 200f)
                val path = Path()
                for ((i, sg) in AirplaneFlight.withIndex()) {
                    if (i == 0) path.moveTo(sg[0] * k, sg[1] * sy)
                    path.cubicTo(sg[2] * k, sg[3] * sy, sg[4] * k, sg[5] * sy, sg[6] * k, sg[7] * sy)
                }
                drawPath(
                    path, AirplaneGray,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()), 0f)),
                )
                val raw = (clock.value % 4f) / 4f
                val eased = 0.5f - 0.5f * cos(PI.toFloat() * raw)
                val (pos, dir) = airplaneAt(eased, k, sy)
                val deg = (atan2(dir.y, dir.x) * 180f / PI.toFloat())
                translate(pos.x, pos.y) {
                    rotate(deg, pivot = Offset.Zero) {
                        drawAirplane(1.dp.toPx(), Color.White, Color(0xFFE8E8EE))
                    }
                }
            }

            // the one that landed, unfolded: crease lines and all
            Box(
                Modifier.fillMaxWidth().padding(start = 64.dp, end = 18.dp)
                    .rotate(-2f)
                    .shadow(6.dp)
                    .background(Color.White)
                    .drawBehind {
                        val crease = Color.Black.copy(alpha = 0.07f)
                        val cx = size.width / 2f
                        drawLine(Color.Black.copy(alpha = 0.09f), Offset(cx, 0f), Offset(cx, size.height), 1f)
                        val dx = size.height / 2f / tan(PI.toFloat() / 3f)
                        drawLine(crease, Offset(cx - dx * 2f, size.height), Offset(cx + dx * 2f, 0f), 1f)
                        drawLine(crease, Offset(cx - dx * 2f, 0f), Offset(cx + dx * 2f, size.height), 1f)
                    }
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())
                        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp)
                ) {
                    Text(
                        "from: ${name.lowercase()}",
                        style = hand.copy(fontSize = 16.sp, color = AirplaneGray),
                    )
                    Text(
                        s.line,
                        style = hand.copy(fontSize = 23.sp, lineHeight = 28.sp),
                        modifier = if (q != null) Modifier.tap { a.openQuest(q) } else Modifier,
                    )
                    if (q != null) {
                        Text("check one:", style = hand.copy(fontSize = 22.sp), modifier = Modifier.padding(top = 10.dp))
                        Column(Modifier.padding(top = 4.dp, end = 64.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AirplaneCheck("yes (done)", hand) { a.done(q) }
                            AirplaneCheck("not yet", hand) { a.snooze(q) }
                            AirplaneCheck("later, i promise", hand) { a.snooze(q, 120) }
                        }
                    } else {
                        Spacer(Modifier.height(48.dp))
                    }
                }
                // doodle: the companion as a little smiley, in its own color
                Canvas(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp).size(70.dp, 50.dp)) {
                    val k = size.width / 70f
                    val st = Stroke(width = 2f * k, cap = StrokeCap.Round)
                    drawCircle(doodle, radius = 10f * k, center = Offset(56f * k, 16f * k), style = st)
                    drawCircle(doodle, radius = 1.4f * k, center = Offset(52f * k, 14f * k))
                    drawCircle(doodle, radius = 1.4f * k, center = Offset(60f * k, 14f * k))
                    val smile = Path().apply { moveTo(51f * k, 19f * k); quadraticBezierTo(56f * k, 23f * k, 61f * k, 19f * k) }
                    drawPath(smile, doodle, style = Stroke(width = 1.6f * k, cap = StrokeCap.Round))
                    // a little star doodle
                    val star = Path().apply {
                        moveTo(22f * k, 22f * k); lineTo(26f * k, 32f * k); lineTo(16f * k, 26f * k)
                        lineTo(28f * k, 26f * k); lineTo(18f * k, 32f * k); close()
                    }
                    drawPath(star, AirplaneBlue, style = Stroke(width = 1.6f * k, cap = StrokeCap.Round))
                    drawLine(AirplaneBlue, Offset(30f * k, 38f * k), Offset(44f * k, 42f * k), 1.6f * k, cap = StrokeCap.Round)
                }
            }

            // still-folded notes waiting: the other open quests
            if (others.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp).horizontalScroll(rememberScrollState())
                        .padding(start = 70.dp, end = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    for ((i, o) in others.take(6).withIndex()) {
                        val (top, under) = AirplaneFolded[i % AirplaneFolded.size]
                        Column(
                            Modifier.rotate(if (i % 2 == 0) 8f else -6f).tap { a.openQuest(o) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Canvas(Modifier.size(54.dp, 34.dp)) {
                                translate(27.dp.toPx(), 17.dp.toPx()) { drawAirplane(54.dp.toPx() / 44f, top, under, mirrored = false) }
                            }
                            Text(
                                o.text.lowercase() + (if (o.due != null) " " + o.shortDue().lowercase() else ""),
                                style = hand.copy(fontSize = 17.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 110.dp),
                            )
                        }
                    }
                    if (others.size > 6) {
                        Text(
                            "+${others.size - 6} more",
                            style = hand.copy(fontSize = 17.sp, color = AirplaneBlue),
                            modifier = Modifier.padding(top = 10.dp).tap(a::openLog),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // write a note, fold it, throw it = spill
            Column(Modifier.fillMaxWidth().padding(start = 64.dp, end = 18.dp, bottom = 24.dp)) {
                Text("write a note, fold it, throw it:", style = hand.copy(fontSize = 18.sp, color = AirplaneGray))
                Spacer(Modifier.height(6.dp))
                SpillInput(
                    onSpill = a::spill,
                    textStyle = hand.copy(fontSize = 24.sp, color = AirplaneBlue),
                    placeholder = "pay rent by friday",
                    placeholderColor = Color(0xFFA6B0D0),
                    cursor = AirplaneBlue,
                    modifier = Modifier.fillMaxWidth().drawBehind {
                        drawLine(AirplaneBlue, Offset(0f, size.height), Offset(size.width - 110.dp.toPx(), size.height), 2.dp.toPx())
                    },
                ) { submit ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Throw it",
                        style = marker.copy(fontSize = 18.sp, color = Color.White),
                        modifier = Modifier.rotate(-2f).background(AirplaneBlue).tap(submit).padding(horizontal = 14.dp, vertical = 8.dp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun AirplaneCheck(label: String, hand: TextStyle, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().tap(onClick).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp).border(2.dp, AirplaneInk))
        Spacer(Modifier.width(10.dp))
        Text(label, style = hand.copy(fontSize = 22.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Position and direction along the flight path at progress [t] (0..1), in canvas px. */
private fun airplaneAt(t: Float, kx: Float, ky: Float): Pair<Offset, Offset> {
    val n = AirplaneFlight.size
    val f = (t.coerceIn(0f, 0.9999f)) * n
    val i = f.toInt().coerceIn(0, n - 1)
    val u = f - i
    val p = AirplaneFlight[i]
    val mu = 1f - u
    val x = mu * mu * mu * p[0] + 3f * mu * mu * u * p[2] + 3f * mu * u * u * p[4] + u * u * u * p[6]
    val y = mu * mu * mu * p[1] + 3f * mu * mu * u * p[3] + 3f * mu * u * u * p[5] + u * u * u * p[7]
    val dx = 3f * mu * mu * (p[2] - p[0]) + 6f * mu * u * (p[4] - p[2]) + 3f * u * u * (p[6] - p[4])
    val dy = 3f * mu * mu * (p[3] - p[1]) + 6f * mu * u * (p[5] - p[3]) + 3f * u * u * (p[7] - p[5])
    return Offset(x * kx, y * ky) to Offset(dx * kx, dy * ky)
}

/**
 * A paper plane centered on the origin, nose pointing along +x (the design's mirrored 44x30 plane).
 * [mirrored] = false draws it nose-left, like the folded ones waiting on the desk.
 */
private fun DrawScope.drawAirplane(unit: Float, top: Color, under: Color, mirrored: Boolean = true) {
    fun px(x: Float) = (if (mirrored) 44f - x else x) * unit - 22f * unit
    fun py(y: Float) = y * unit - 15f * unit
    val wing = Path().apply { moveTo(px(0f), py(15f)); lineTo(px(44f), py(2f)); lineTo(px(20f), py(18f)); close() }
    val keel = Path().apply { moveTo(px(20f), py(18f)); lineTo(px(44f), py(2f)); lineTo(px(24f), py(28f)); close() }
    val edge = Color(0xFF6A6A7A)
    drawPath(wing, top)
    drawPath(wing, edge, style = Stroke(width = unit))
    drawPath(keel, under)
    drawPath(keel, edge, style = Stroke(width = unit))
}

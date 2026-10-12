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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlin.math.abs
import kotlin.math.cos

private val SsDim = Color(0xFF6A6A6A)
private val SsGrey = Color(0xFF9A9A9A)
private val SsMono = TextStyle(fontFamily = Fonts.spaceMono, fontSize = 13.sp, color = Color.White)

private class SsEnvelope(
    val dur: Float, val lead: Float, val topDp: Float, val unitDp: Float,
    val wing: Color, val body: Color, val edge: Color?, val flap: Float, val alpha: Float,
)

private val SsFlock = listOf(
    SsEnvelope(7f, 0f, 0f, 1f, Color(0xFFE8E8E8), Color(0xFFF4F0E2), Color(0xFF999999), 0.3f, 1f),
    SsEnvelope(9f, 3f, 200f, 0.75f, Color(0xFFC8C8C8), Color(0xFFE4DCC8), Color(0xFF888888), 0.35f, 1f),
    SsEnvelope(11f, 6f, 420f, 0.5625f, Color(0xFFAAAAAA), Color(0xFFCFC8B4), null, 0.4f, 0.7f),
)

/** Rotate a color's hue, like CSS hue-rotate. */
private fun ssHue(c: Color, deg: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    val h = ((hsv[0] + deg) % 360f + 360f) % 360f
    return Color.hsv(h, hsv[1].coerceIn(0f, 1f), hsv[2].coerceIn(0f, 1f))
}

/** "NOON", "3:00 PM", "FRIDAY", "LATE", "SOMEDAY". */
private fun ssDueWord(q: Quest, now: Long): String {
    val due = q.due ?: return "SOMEDAY"
    if (q.shelf(now) == Shelf.OVERDUE) return "LATE"
    if (q.allDay) return Time.dayName(Time.date(due)).uppercase()
    return q.shortDue().uppercase().replace("12:00 PM", "NOON")
}

private fun DrawScope.ssDrawEnvelope(e: SsEnvelope, t: Float) {
    val u = e.unitDp.dp.toPx()
    val p = (((t + e.lead) / e.dur) % 1f)
    val x = size.width + 30.dp.toPx() + (-160.dp.toPx() - size.width - 30.dp.toPx()) * p
    val y = e.topDp.dp.toPx() + (-80f + 640f * p).dp.toPx()
    val f = (t % e.flap) / e.flap
    val flapY = 1f - 1.6f * (1f - abs(2f * f - 1f))
    withTransform({
        translate(x, y)
        scale(u, u, Offset.Zero)
    }) {
        val left = Path().apply { moveTo(18f, 14f); cubicTo(4f, 0f, -2f, 10f, 6f, 18f); close() }
        val right = Path().apply { moveTo(46f, 14f); cubicTo(60f, 0f, 66f, 10f, 58f, 18f); close() }
        scale(1f, flapY, Offset(18f, 16f)) { drawPath(left, e.wing, alpha = e.alpha) }
        scale(1f, flapY, Offset(46f, 16f)) { drawPath(right, e.wing, alpha = e.alpha) }
        drawRect(e.body, Offset(14f, 14f), Size(36f, 24f), alpha = e.alpha)
        if (e.edge != null) {
            drawRect(e.edge, Offset(14f, 14f), Size(36f, 24f), style = Stroke(1f), alpha = e.alpha)
            val v = Path().apply { moveTo(14f, 14f); lineTo(32f, 28f); lineTo(50f, 14f) }
            drawPath(v, e.edge, style = Stroke(1f), alpha = e.alpha)
        }
    }
}

@Composable
fun ScreensaverWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val c = s.companion
    val clock = rememberClock()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var spillOpen by rememberSaveable { mutableStateOf(false) }
    val lastActivity = s.quests.maxOfOrNull { maxOf(it.created, it.doneAt ?: 0L) }?.coerceAtMost(s.now) ?: s.now
    val waiting = s.open.size

    Box(Modifier.fillMaxSize().background(Color.Black).tap { a.openLog() }) {
        // flying envelopes with wings
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            SsFlock.forEach { ssDrawEnvelope(it, t) }
        }

        // the companion's badge, bouncing off the walls and changing color
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            val badgeW = 128.dp
            val badgeH = 58.dp
            val maxX = (maxWidth - badgeW).value.coerceAtLeast(0f)
            val minY = (maxHeight.value * 0.12f)
            val maxY = (maxHeight.value * 0.78f - badgeH.value).coerceAtLeast(minY)
            Row(
                Modifier
                    .offset {
                        val t = clock.value
                        val fx = (t / 3.7f) % 2f
                        val fy = (t / 5.3f) % 2f
                        val px = if (fx < 1f) fx else 2f - fx
                        val py = if (fy < 1f) fy else 2f - fy
                        IntOffset(
                            (px * maxX).dp.roundToPx(),
                            (minY + py * (maxY - minY)).dp.roundToPx(),
                        )
                    }
                    .size(badgeW, badgeH)
                    .drawBehind {
                        val step = ((clock.value / 4f % 1f) * 6f).toInt()
                        val tint = ssHue(c.c, step * 60f)
                        drawRoundRect(tint, cornerRadius = CornerRadius(size.height / 2f))
                        val r = 13.dp.toPx()
                        val center = Offset(size.width / 2f - 26.dp.toPx(), size.height / 2f)
                        drawCircle(Color.Black, radius = r + 2.dp.toPx(), center = center)
                        drawCircle(
                            Brush.radialGradient(
                                listOf(ssHue(c.cLight, step * 60f), tint, ssHue(c.cDark, step * 60f)),
                                center = Offset(center.x - r * 0.2f, center.y - r * 0.3f),
                                radius = r * 1.2f,
                            ),
                            radius = r, center = center,
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.width(34.dp))
                Text(
                    c.title,
                    style = SsMono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                    maxLines = 1, overflow = TextOverflow.Clip,
                )
            }
        }

        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.c, fontWeight = FontWeight.Bold)) { append("${c.title}: ") }
                        append(s.line)
                    },
                    style = SsMono.copy(color = SsGrey, lineHeight = 20.sp),
                    maxLines = 5, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                // small corner menu
                Canvas(
                    Modifier.size(28.dp).border(1.dp, if (menuOpen) SsGrey else Color(0xFF333333))
                        .tap { menuOpen = !menuOpen }
                ) {
                    val w = 12.dp.toPx()
                    val x0 = (size.width - w) / 2f
                    for (i in -1..1) {
                        val y = size.height / 2f + i * 4.dp.toPx()
                        drawLine(SsDim, Offset(x0, y), Offset(x0 + w, y), strokeWidth = 1.5f.dp.toPx())
                    }
                }
            }
            if (menuOpen) {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.align(Alignment.End).background(Color.Black).border(1.dp, SsDim)
                        .tap { }.padding(vertical = 4.dp)
                ) {
                    SsMenuItem("options") { menuOpen = false; a.openOptions() }
                    SsMenuItem("spill a quest") { menuOpen = false; spillOpen = true }
                    SsMenuItem("quest log") { menuOpen = false; a.openLog() }
                    if (q != null) {
                        SsMenuItem("mark done") { menuOpen = false; a.done(q) }
                        SsMenuItem("snooze 30 min") { menuOpen = false; a.snooze(q, 30) }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 3D marquee of the next quest
            val words = q?.text?.uppercase() ?: "ALL CLEAR"
            val fs = when {
                words.length <= 14 -> 40.sp
                words.length <= 28 -> 32.sp
                else -> 26.sp
            }
            Column(
                Modifier.fillMaxWidth().graphicsLayer {
                    rotationY = -30f * cos(clock.value / 6f * 2f * Math.PI.toFloat())
                    cameraDistance = 12f * density
                }.tap { if (q != null) a.openQuest(q) else a.openLog() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    words,
                    style = TextStyle(fontFamily = Fonts.bungeeShade, fontSize = fs, lineHeight = fs * 1.05f, color = Color(0xFF6FE0FF), textAlign = TextAlign.Center),
                    maxLines = 4, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (q != null) ssDueWord(q, s.now) else "NO QUESTS",
                    style = TextStyle(fontFamily = Fonts.bungeeShade, fontSize = fs, lineHeight = fs * 1.05f, color = Color(0xFFFFD23A), textAlign = TextAlign.Center),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.weight(1f))

            if (spillOpen || q == null) {
                Box(
                    Modifier.fillMaxWidth().background(Color.Black).border(1.dp, SsDim).tap { }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    SpillInput(
                        onSpill = { a.spill(it); spillOpen = false },
                        textStyle = SsMono.copy(fontSize = 14.sp),
                        placeholder = "> type a quest, press enter",
                        placeholderColor = SsDim,
                        cursor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        trailing = { submit ->
                            Text(
                                "ENTER",
                                style = SsMono.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SsGrey),
                                modifier = Modifier.padding(start = 8.dp).tap { submit() },
                            )
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                "idle since ${Time.time(lastActivity)} · $waiting quest${if (waiting == 1) "" else "s"} waiting",
                style = SsMono.copy(fontSize = 12.sp, color = SsDim),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "TAP TO WAKE",
                style = SsMono.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
                modifier = Modifier.graphicsLayer {
                    val f = (clock.value / 2f) % 1f
                    alpha = 0.25f + 0.65f * (1f - abs(2f * f - 1f))
                },
            )
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SsMenuItem(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = SsMono.copy(color = Color(0xFFC0C0C0)),
        modifier = Modifier.tap(onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

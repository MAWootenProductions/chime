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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PigeonInk = Color(0xFF2A1D10)
private val PigeonBrass = Color(0xFFE8C88A)
private val PigeonCream = Color(0xFFF2E8CC)

/** Carrier Pigeon: pigeonholes on a wooden desk, a note just delivered, a blank slip to send. */
@Composable
fun PigeonWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val name = s.companion.title
    val clock = rememberClock()
    val sheen = s.companion.c
    val sheenLight = s.companion.cLight

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF7A4F2E), Color(0xFF5E3A20))))
    ) {
        // wood grain
        Canvas(Modifier.fillMaxSize()) {
            val h = size.height
            var x = -h * 0.06f
            val step = 9.dp.toPx()
            while (x < size.width + h * 0.06f) {
                drawLine(Color.Black.copy(alpha = 0.05f), Offset(x, 0f), Offset(x + h * 0.052f, h), 2.dp.toPx())
                x += step
            }
            x = -h * 0.06f
            val step2 = 23.dp.toPx()
            while (x < size.width + h * 0.06f) {
                drawLine(Color.White.copy(alpha = 0.04f), Offset(x, 0f), Offset(x - h * 0.035f, h), 1.dp.toPx())
                x += step2
            }
        }

        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 18.dp)
        ) {
            // desk top bar: drawer = options
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "The Pigeonhole Desk",
                    style = TextStyle(fontFamily = Fonts.imFellEnglishSc, fontSize = 15.sp, color = PigeonBrass.copy(alpha = 0.8f)),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.background(Color(0xFF3E2614)).tap(a::openOptions).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(12.dp)) {
                        drawCircle(Color(0xFFB8925A), radius = size.minDimension / 2f)
                        drawCircle(Color(0xFF6A4A22), radius = size.minDimension / 5f)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Drawer", style = TextStyle(fontFamily = Fonts.imFellEnglishSc, fontSize = 13.sp, color = PigeonBrass))
                }
            }

            // the pigeonholes: this is the quest log
            Row(
                Modifier.fillMaxWidth()
                    .background(Color(0xFF2A180C)).padding(bottom = 3.dp)
                    .background(Color(0xFF3E2614)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PigeonHole("Today", s.today.size, PigeonRollsToday, Modifier.weight(1f), a::openLog)
                PigeonHole("Week", s.week.size, PigeonRollsWeek, Modifier.weight(1f), a::openLog)
                PigeonHole("Someday", s.someday.size, PigeonRollsSomeday, Modifier.weight(1f), a::openLog)
            }

            Spacer(Modifier.height(10.dp))

            // the note just delivered, and the pigeon who carried it
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val noteMax = (maxHeight - 28.dp).coerceAtLeast(60.dp)
                Box(
                    Modifier.align(Alignment.TopStart)
                        .padding(start = 4.dp, top = 18.dp)
                        .fillMaxWidth(0.68f)
                        .heightIn(max = noteMax)
                        .rotate(-3f)
                ) {
                    Column(
                        Modifier.shadow(10.dp)
                            .background(Brush.verticalGradient(listOf(Color(0xFFF6EDD4), Color(0xFFECDFBD))))
                            .drawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        listOf(Color(0x40785A28), Color.Transparent),
                                        startY = 0f, endY = 10.dp.toPx(),
                                    ),
                                    size = Size(size.width, 10.dp.toPx()),
                                )
                            }
                            .verticalScroll(rememberScrollState())
                            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (q != null) {
                            Column(Modifier.tap { a.openQuest(q) }) {
                                Text(
                                    q.text,
                                    style = TextStyle(fontFamily = Fonts.kalam, fontWeight = FontWeight.W700, fontSize = 21.sp, lineHeight = 26.sp, color = PigeonInk),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    q.dueText(),
                                    style = TextStyle(fontFamily = Fonts.kalam, fontSize = 15.sp, color = Color(0xFF7A6A4A)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            s.line,
                            style = TextStyle(fontFamily = Fonts.kalam, fontSize = if (q == null) 19.sp else 17.sp, lineHeight = 23.sp, color = PigeonInk),
                        )
                        Text(
                            "— $name",
                            style = TextStyle(fontFamily = Fonts.kalam, fontSize = 18.sp, color = PigeonInk),
                            modifier = Modifier.align(Alignment.End).padding(end = 22.dp),
                        )
                    }
                    // postmark
                    val stamp = Time.ldt(s.now).format(DateTimeFormatter.ofPattern("EEE h:mm", Locale.getDefault()))
                    Box(
                        Modifier.align(Alignment.BottomEnd)
                            .offset(x = 14.dp, y = 16.dp)
                            .size(70.dp)
                            .rotate(14f)
                            .border(2.dp, Color(0xB3962828), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Delivered\n$stamp",
                            style = TextStyle(fontFamily = Fonts.imFellEnglishSc, fontSize = 10.sp, lineHeight = 12.sp, color = Color(0xCC962828), textAlign = TextAlign.Center),
                        )
                    }
                }

                // the pigeon, pecking at the desk
                Canvas(
                    Modifier.align(Alignment.TopEnd)
                        .offset(x = 4.dp)
                        .size(130.dp)
                        .graphicsLayer {
                            rotationZ = pigeonPeck(clock.value)
                            transformOrigin = TransformOrigin(0.4f, 0.9f)
                        }
                ) { drawPigeon(sheen, sheenLight) }
            }

            Spacer(Modifier.height(12.dp))

            if (q != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f)
                            .background(Color(0xFF141C14)).padding(bottom = 3.dp)
                            .background(Color(0xFF2A3A2A))
                            .tap { a.done(q) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Sent it", style = TextStyle(fontFamily = Fonts.kalam, fontSize = 17.sp, color = PigeonCream))
                    }
                    Box(
                        Modifier.weight(1f)
                            .background(PigeonCream.copy(alpha = 0.14f))
                            .border(1.dp, PigeonCream.copy(alpha = 0.35f))
                            .tap { a.snooze(q, 60) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Fly back in an hour",
                            style = TextStyle(fontFamily = Fonts.kalam, fontSize = 17.sp, color = PigeonCream, textAlign = TextAlign.Center),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // blank slip + pencil = spill
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .rotate(1.5f)
                    .shadow(8.dp)
                    .background(Color(0xFFFAF5E6))
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Write a note for $name to carry",
                    style = TextStyle(fontFamily = Fonts.kalam, fontSize = 15.sp, color = Color(0xFF7A6A4A)),
                )
                SpillInput(
                    onSpill = a::spill,
                    textStyle = TextStyle(fontFamily = Fonts.kalam, fontSize = 22.sp, color = PigeonInk),
                    placeholder = "pay rent by friday",
                    placeholderColor = Color(0xFFB8A67A),
                    cursor = PigeonInk,
                    modifier = Modifier.fillMaxWidth(),
                ) { submit ->
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Roll it up",
                        style = TextStyle(fontFamily = Fonts.imFellEnglishSc, fontSize = 15.sp, color = Color(0xFF8A2A2A)),
                        modifier = Modifier.border(1.5.dp, Color(0xFF8A2A2A)).tap(submit).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.5.dp).background(Color(0xFFB8A67A)))
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

private class PigeonRoll(val left: Float, val top: Float, val deg: Float, val color: Color)

private val PigeonRollsToday = listOf(
    PigeonRoll(14f, 26f, -6f, Color(0xFFEFE3C4)),
    PigeonRoll(22f, 34f, 4f, Color(0xFFE6D6B0)),
    PigeonRoll(16f, 42f, 0f, Color(0xFFF2E8CC)),
)
private val PigeonRollsWeek = listOf(
    PigeonRoll(14f, 28f, 6f, Color(0xFFD8C8A0)),
    PigeonRoll(18f, 36f, -3f, Color(0xFFE6D6B0)),
    PigeonRoll(22f, 44f, 0f, Color(0xFFEFE3C4)),
)
private val PigeonRollsSomeday = listOf(
    PigeonRoll(14f, 30f, 8f, Color(0xFFD8C8A0)),
    PigeonRoll(20f, 40f, -5f, Color(0xFFE6D6B0)),
    PigeonRoll(18f, 46f, 0f, Color(0xFFEFE3C4)),
)

@Composable
private fun PigeonHole(label: String, count: Int, rolls: List<PigeonRoll>, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(74.dp).background(Color(0xFF24160B)).tap(onClick),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
            // inset shadow at the top of the cubby
            drawRect(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent), startY = 0f, endY = 14.dp.toPx()),
                size = Size(size.width, 14.dp.toPx()),
            )
            val shown = rolls.takeLast(count.coerceIn(0, rolls.size))
            val shift = (size.width - 108.dp.toPx()) / 2f
            val w = 44.dp.toPx()
            val h = 12.dp.toPx()
            for (r in shown) {
                val tl = Offset(shift + r.left.dp.toPx(), r.top.dp.toPx())
                rotate(r.deg, pivot = Offset(tl.x + w / 2f, tl.y + h / 2f)) {
                    drawRoundRect(r.color, topLeft = tl, size = Size(w, h), cornerRadius = CornerRadius(h / 2f))
                    drawLine(Color(0x33785A28), Offset(tl.x + w * 0.2f, tl.y + h / 2f), Offset(tl.x + w * 0.8f, tl.y + h / 2f), 1f)
                }
            }
        }
        Text(
            "$label · $count",
            style = TextStyle(fontFamily = Fonts.imFellEnglishSc, fontSize = 13.sp, color = PigeonBrass),
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp, end = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 4s loop: still for most of it, then a quick peck. */
private fun pigeonPeck(t: Float): Float {
    val p = (t % 4f) / 4f
    return when {
        p < 0.8f -> 0f
        p < 0.88f -> 14f * ((p - 0.8f) / 0.08f)
        else -> 14f * (1f - (p - 0.88f) / 0.12f)
    }
}

private class PigeonPen(private val k: Float) {
    val path = Path()
    fun m(x: Float, y: Float) { path.moveTo(x * k, y * k) }
    fun l(x: Float, y: Float) { path.lineTo(x * k, y * k) }
    fun c(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) {
        path.cubicTo(x1 * k, y1 * k, x2 * k, y2 * k, x * k, y * k)
    }
    fun z() { path.close() }
}

private fun pigeonPath(k: Float, block: PigeonPen.() -> Unit): Path = PigeonPen(k).apply(block).path

/** The pigeon from the 130x130 design, scaled to the canvas. Its neck sheen is the companion's color. */
private fun DrawScope.drawPigeon(sheen: Color, sheenLight: Color) {
    val k = size.width / 130f
    drawOval(Color.Black.copy(alpha = 0.25f), topLeft = Offset(26f * k, 114f * k), size = Size(80f * k, 12f * k))
    // body
    drawPath(pigeonPath(k) {
        m(30f, 86f); c(30f, 60f, 56f, 50f, 80f, 58f); c(100f, 64f, 112f, 80f, 120f, 98f)
        c(96f, 104f, 60f, 106f, 36f, 100f); z()
    }, Color(0xFF8E95A3))
    // wing
    drawPath(pigeonPath(k) {
        m(48f, 72f); c(64f, 66f, 88f, 70f, 104f, 90f); c(84f, 92f, 62f, 90f, 48f, 84f); z()
    }, Color(0xFF6F7686))
    // tail
    drawPath(pigeonPath(k) { m(104f, 90f); l(124f, 96f); l(106f, 100f); z() }, Color(0xFF4A505C))
    // head and neck
    drawPath(pigeonPath(k) {
        m(34f, 72f); c(30f, 52f, 38f, 36f, 52f, 34f); c(64f, 34f, 70f, 46f, 66f, 62f); c(60f, 70f, 44f, 76f, 34f, 72f); z()
    }, Color(0xFF9AA2B2))
    // iridescent neck sheen: companion colors
    drawPath(pigeonPath(k) {
        m(40f, 66f); c(44f, 60f, 56f, 58f, 66f, 62f); c(62f, 70f, 50f, 76f, 40f, 72f); z()
    }, sheen, alpha = 0.85f)
    drawPath(
        pigeonPath(k) { m(40f, 66f); c(46f, 62f, 54f, 62f, 60f, 64f) },
        sheenLight, alpha = 0.8f, style = Stroke(width = 2f * k),
    )
    // beak
    drawPath(pigeonPath(k) { m(38f, 44f); l(26f, 48f); l(38f, 51f); z() }, Color(0xFFC9A07A))
    // eye and a slightly sheepish brow
    drawCircle(Color(0xFFF3A03A), radius = 4f * k, center = Offset(46f * k, 44f * k))
    drawCircle(Color(0xFF1A1A1A), radius = 2f * k, center = Offset(46.5f * k, 44.5f * k))
    drawLine(Color(0xFF5A606C), Offset(41f * k, 39f * k), Offset(50f * k, 40f * k), 2f * k, cap = StrokeCap.Round)
    // legs and feet
    val leg = Color(0xFFC96A6A)
    drawLine(leg, Offset(58f * k, 104f * k), Offset(58f * k, 116f * k), 3f * k, cap = StrokeCap.Round)
    drawLine(leg, Offset(70f * k, 104f * k), Offset(70f * k, 116f * k), 3f * k, cap = StrokeCap.Round)
    drawLine(leg, Offset(52f * k, 116f * k), Offset(64f * k, 116f * k), 3f * k, cap = StrokeCap.Round)
    drawLine(leg, Offset(64f * k, 116f * k), Offset(76f * k, 116f * k), 3f * k, cap = StrokeCap.Round)
    // the message capsule on its leg
    drawRoundRect(Color(0xFFEFE3C4), topLeft = Offset(66f * k, 106f * k), size = Size(9f * k, 6f * k), cornerRadius = CornerRadius(1f * k))
    drawRoundRect(
        Color(0xFFA88A50), topLeft = Offset(66f * k, 106f * k), size = Size(9f * k, 6f * k),
        cornerRadius = CornerRadius(1f * k), style = Stroke(width = 1f * k),
    )
}

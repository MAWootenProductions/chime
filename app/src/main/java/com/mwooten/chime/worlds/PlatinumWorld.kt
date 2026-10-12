package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap

private val PlatWindow = Color(0xFFDEDEDE)
private val PlatEdge = Color(0xFF222222)
private val PlatText = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = Color.Black)

/** Hard offset drop shadow, drawn outside the bounds. */
private fun Modifier.platShadow(offsetDp: Float, alpha: Float): Modifier = this.drawBehind {
    val o = offsetDp.dp.toPx()
    drawRect(Color.Black.copy(alpha = alpha), Offset(o, o), size)
}

/** The diagonal blue desktop pattern. */
private fun Modifier.platDesktop(): Modifier = this.drawBehind {
    drawRect(Color(0xFF5A6AA0))
    val step = 4.dp.toPx() * 1.4142f
    val w = 2.dp.toPx()
    val stripe = Color(0xFF6676AC)
    var x = -size.height
    while (x < size.width + size.height) {
        drawLine(stripe, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = w)
        x += step
    }
}

/** Pinstriped title bar of a window, with optional close box. */
@Composable
private fun PlatTitleBar(title: String, onClose: (() -> Unit)?) {
    Box(
        Modifier.fillMaxWidth().height(20.dp).drawBehind {
            drawRect(Color(0xFFB8B8B8))
            val p = 1.dp.toPx()
            var y = 0f
            while (y < size.height) { drawRect(Color(0xFFE8E8E8), Offset(0f, y), Size(size.width, p)); y += 2 * p }
            drawRect(Color(0xFF555555), Offset(0f, size.height - p), Size(size.width, p))
        },
        contentAlignment = Alignment.Center,
    ) {
        if (onClose != null) {
            Box(
                Modifier.align(Alignment.CenterStart).padding(start = 6.dp).size(11.dp)
                    .background(Color(0xFFE8E8E8)).border(1.dp, Color(0xFF333333)).tap(onClose)
            )
        }
        Text(
            title,
            style = PlatText.copy(fontWeight = FontWeight.Bold),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp).background(PlatWindow).padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun PlatButton(label: String, default: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(5.dp)
    val inner = @Composable {
        Box(
            Modifier.clip(shape)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD0D0D0))))
                .border(1.dp, Color(0xFF333333), shape)
                .tap(onClick)
                .padding(horizontal = 18.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = PlatText.copy(fontWeight = if (default) FontWeight.Bold else FontWeight.Normal), maxLines = 1)
        }
    }
    if (default) {
        Box(Modifier.border(2.dp, PlatEdge, RoundedCornerShape(9.dp)).padding(4.dp)) { inner() }
    } else {
        Box(Modifier.padding(4.dp)) { inner() }
    }
}

@Composable
private fun PlatWindowBox(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.platShadow(2f, 0.4f).background(PlatWindow).border(1.dp, PlatEdge),
        content = content,
    )
}

@Composable
fun PlatinumWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val c = s.companion
    val clock = rememberClock()
    val addHolder = remember { arrayOfNulls<() -> Unit>(1) }
    val padItems: List<Quest> = s.today.ifEmpty { s.open }.take(5)
    val padTitle = if (s.today.isNotEmpty()) "Today" else "Note Pad"
    val lastDone = s.done.firstOrNull()

    Box(Modifier.fillMaxSize().platDesktop()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // menu bar
            Row(
                Modifier.fillMaxWidth().height(22.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFF2F2F2), Color(0xFFD8D8D8))))
                    .drawBehind {
                        val p = 1.dp.toPx()
                        drawRect(Color(0xFF555555), Offset(0f, size.height - p), Size(size.width, p))
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Canvas(Modifier.size(14.dp).tap(a::openOptions)) {
                    val r = 5.dp.toPx()
                    drawCircle(c.c, radius = r)
                    drawCircle(c.cDark, radius = r, style = Stroke(1.dp.toPx()))
                }
                val bold = PlatText.copy(fontWeight = FontWeight.Bold)
                Text("File", style = bold, modifier = Modifier.tap(a::openLog))
                Text("Edit", style = bold, modifier = Modifier.tap { if (q != null) a.openQuest(q) else a.openLog() })
                Text("View", style = bold, modifier = Modifier.tap(a::openLog))
                Text("Special", style = bold, modifier = Modifier.tap(a::openOptions))
                Spacer(Modifier.weight(1f))
                Text(com.mwooten.chime.data.Time.time(s.now), style = PlatText, maxLines = 1)
                Text("Chime", style = bold, maxLines = 1, modifier = Modifier.tap(a::openOptions))
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    // a note pad window = today's list
                    PlatWindowBox(Modifier.weight(1f)) {
                        PlatTitleBar(padTitle, onClose = a::openLog)
                        Column(
                            Modifier.fillMaxWidth().padding(4.dp)
                                .background(Color(0xFFFFFBD0)).border(1.dp, Color(0xFF888888))
                                .padding(vertical = 6.dp)
                        ) {
                            val rows = padItems.size + (if (lastDone != null) 1 else 0)
                            padItems.forEachIndexed { i, item ->
                                PlatPadRow(Modifier.tap { a.openQuest(item) }) {
                                    Text(
                                        buildAnnotatedString {
                                            append("☐ ")
                                            if (i == 0) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(item.text) }
                                            else append(item.text)
                                            append(" — ")
                                            append(item.shortDue())
                                        },
                                        style = PlatText.copy(fontSize = 14.sp, color = if (i < 2) Color.Black else Color(0xFF777777)),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (lastDone != null) {
                                PlatPadRow(Modifier.tap { a.openQuest(lastDone) }) {
                                    Text(
                                        "☑ ${lastDone.text}",
                                        style = PlatText.copy(fontSize = 14.sp, color = Color(0xFF777777), textDecoration = TextDecoration.LineThrough),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (rows == 0) {
                                PlatPadRow(Modifier) {
                                    Text("Nothing written here yet.", style = PlatText.copy(fontSize = 14.sp, color = Color(0xFF777777)), maxLines = 1)
                                }
                            }
                            repeat((3 - maxOf(rows, 1)).coerceAtLeast(0)) { PlatPadRow(Modifier) {} }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    // desktop icons, right side
                    Column(
                        Modifier.width(70.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        PlatIcon("Chime HD", onClick = a::openOptions) {
                            Canvas(Modifier.size(34.dp, 24.dp)) {
                                val u = size.width / 34f
                                drawRoundRect(Color(0xFFD6D6D6), Offset(1 * u, 4 * u), Size(32 * u, 18 * u), androidx.compose.ui.geometry.CornerRadius(2 * u))
                                drawRoundRect(Color(0xFF333333), Offset(1 * u, 4 * u), Size(32 * u, 18 * u), androidx.compose.ui.geometry.CornerRadius(2 * u), style = Stroke(u))
                                drawRect(c.c, Offset(4 * u, 15 * u), Size(10 * u, 3 * u))
                            }
                        }
                        PlatIcon("This Week (${s.week.size})", onClick = a::openLog) {
                            Canvas(Modifier.size(30.dp, 26.dp)) {
                                val u = size.width / 30f
                                val p = Path().apply {
                                    moveTo(1 * u, 4 * u); lineTo(12 * u, 4 * u); lineTo(14 * u, 7 * u); lineTo(29 * u, 7 * u)
                                    lineTo(29 * u, 25 * u); lineTo(1 * u, 25 * u); close()
                                }
                                drawPath(p, Color(0xFFB8C8F0))
                                drawPath(p, Color(0xFF333333), style = Stroke(u))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // the alert
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                        .platShadow(3f, 0.45f)
                        .background(PlatWindow)
                        .drawBehind {
                            val p = 1.dp.toPx()
                            drawRect(Color.White, Offset(p, p), Size(size.width - 2 * p, p))
                            drawRect(Color.White, Offset(p, p), Size(p, size.height - 2 * p))
                            drawRect(Color(0xFF999999), Offset(p, size.height - 2 * p), Size(size.width - 2 * p, p))
                            drawRect(Color(0xFF999999), Offset(size.width - 2 * p, p), Size(p, size.height - 2 * p))
                        }
                        .border(1.dp, PlatEdge)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Canvas(Modifier.size(44.dp)) {
                            val f = (clock.value % 3f) / 3f
                            val hop = when {
                                f < 0.9f -> 0f
                                f < 0.94f -> -4f * (f - 0.9f) / 0.04f
                                else -> -4f * (1f - (f - 0.94f) / 0.06f)
                            }
                            translate(top = hop.dp.toPx()) { platBird(c.c, c.cDark) }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val body = PlatText.copy(fontSize = 13.sp, lineHeight = 18.sp)
                            if (q != null) {
                                Text(
                                    "“${q.text}” — ${q.dueText()}.",
                                    style = body.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 4, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.tap { a.openQuest(q) },
                                )
                            } else {
                                Text("There are no alerts.", style = body.copy(fontWeight = FontWeight.Bold))
                            }
                            Text("${c.title} says: ${s.line}", style = body)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                        if (q != null) {
                            PlatButton("Later", default = false) { a.snooze(q, 30) }
                            PlatButton("Done", default = true) { a.done(q) }
                        } else {
                            PlatButton("Open Log", default = true, onClick = a::openLog)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // find-file style box = spill
                PlatWindowBox(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                    PlatTitleBar("New Item", onClose = null)
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Name the thing on your mind:", style = PlatText)
                        Box(
                            Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF333333))
                                .drawBehind {
                                    val p = 1.dp.toPx()
                                    drawRect(Color(0xFF999999), Offset(p, p), Size(size.width - 2 * p, p))
                                    drawRect(Color(0xFF999999), Offset(p, p), Size(p, size.height - 2 * p))
                                }
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            SpillInput(
                                onSpill = a::spill,
                                textStyle = PlatText.copy(fontSize = 13.sp),
                                placeholder = "pay rent friday",
                                placeholderColor = Color(0xFF999999),
                                cursor = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                trailing = { submit -> addHolder[0] = submit },
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            PlatButton("Add", default = true) { addHolder[0]?.invoke() }
                        }
                    }
                }
            }

            // trash, bottom right
            Row(Modifier.fillMaxWidth().padding(end = 18.dp, bottom = 10.dp, top = 4.dp), horizontalArrangement = Arrangement.End) {
                PlatIcon("Done (${s.done.size})", onClick = a::openLog) {
                    Canvas(Modifier.size(28.dp, 32.dp)) {
                        val u = size.width / 28f
                        val can = Path().apply {
                            moveTo(4 * u, 6 * u); lineTo(24 * u, 6 * u); lineTo(22 * u, 30 * u); lineTo(6 * u, 30 * u); close()
                        }
                        drawPath(can, Color(0xFFD6D6D6))
                        drawPath(can, Color(0xFF333333), style = Stroke(u))
                        drawRect(Color(0xFFBBBBBB), Offset(2 * u, 3 * u), Size(24 * u, 3 * u))
                        drawRect(Color(0xFF333333), Offset(2 * u, 3 * u), Size(24 * u, 3 * u), style = Stroke(u))
                        for (x in listOf(10f, 14f, 18f)) drawLine(Color(0xFF888888), Offset(x * u, 10 * u), Offset(x * u, 26 * u), strokeWidth = u)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatPadRow(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(25.dp).drawBehind {
            val p = 1.dp.toPx()
            drawRect(Color(0xFFE8DCA0), Offset(0f, size.height - p), Size(size.width, p))
        }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

@Composable
private fun PlatIcon(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        Modifier.width(70.dp).tap(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        icon()
        Text(
            label, style = PlatText, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.background(Color.White).padding(horizontal = 3.dp),
        )
    }
}

/** The companion as a little songbird alert icon (44x44 viewBox). */
private fun DrawScope.platBird(body: Color, wing: Color) {
    val u = size.width / 44f
    val ink = Color(0xFF222222)
    val stroke = Stroke(u)
    val b = Path().apply {
        moveTo(6 * u, 28 * u)
        cubicTo(6 * u, 18 * u, 16 * u, 14 * u, 26 * u, 16 * u)
        cubicTo(32 * u, 18 * u, 36 * u, 24 * u, 34 * u, 30 * u)
        cubicTo(28 * u, 34 * u, 14 * u, 34 * u, 6 * u, 28 * u)
        close()
    }
    drawPath(b, body); drawPath(b, ink, style = stroke)
    val tail = Path().apply { moveTo(30 * u, 22 * u); lineTo(42 * u, 8 * u); lineTo(38 * u, 22 * u); close() }
    drawPath(tail, wing); drawPath(tail, ink, style = stroke)
    drawCircle(body, radius = 6 * u, center = Offset(10 * u, 19 * u))
    drawCircle(ink, radius = 6 * u, center = Offset(10 * u, 19 * u), style = stroke)
    val beak = Path().apply { moveTo(5 * u, 18 * u); lineTo(0f, 19.5f * u); lineTo(5 * u, 20.5f * u); close() }
    drawPath(beak, ink)
    drawCircle(Color(0xFF111111), radius = 1.2f * u, center = Offset(8 * u, 18 * u))
    drawLine(ink, Offset(14 * u, 34 * u), Offset(14 * u, 40 * u), strokeWidth = u)
    drawLine(ink, Offset(20 * u, 34 * u), Offset(20 * u, 40 * u), strokeWidth = u)
}

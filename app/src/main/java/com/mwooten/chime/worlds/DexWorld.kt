package com.mwooten.chime.worlds

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap

private val DexCase = Color(0xFF3B4A3A)
private val DexInk = Color(0xFF1E261D)
private val DexLcd = Color(0xFFD8E4C4)
private val DexBone = Color(0xFFE8ECD8)
private val DexYellow = Color(0xFFF2C84A)
private val DexPaper = Color(0xFFF4F0E2)

/** A hard, offset "box-shadow" drawn behind the element. */
private fun Modifier.dexShadow(color: Color, dy: Dp, radius: Dp): Modifier = drawBehind {
    drawRoundRect(color, topLeft = Offset(0f, dy.toPx()), size = size, cornerRadius = CornerRadius(radius.toPx()))
}

private fun dexNo(s: WorldState, q: Quest): String =
    (s.quests.indexOfFirst { it.id == q.id } + 1).toString().padStart(3, '0')

private fun dexShortName(q: Quest): String =
    q.text.trim().split(Regex("\\s+")).take(2).joinToString(" ").uppercase().take(12)

private fun dexSpecies(q: Quest, now: Long): String = when {
    q.due == null -> "The Drifting Creature"
    q.shelf(now) == Shelf.OVERDUE -> "The Escaped Creature"
    q.allDay -> "The All-Day Creature"
    q.kind == "by" -> "The Deadline Creature"
    else -> "The Appointment Creature"
}

private fun dexFlees(q: Quest, now: Long): String = when {
    q.due == null -> "never (someday)"
    q.shelf(now) == Shelf.OVERDUE -> "already fled, ${q.shortDue()}"
    else -> q.shortDue()
}

@Composable
fun DexWorld(s: WorldState, a: WorldActions) {
    var selId by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    val q: Quest? = s.open.firstOrNull { it.id == selId } ?: s.focus
    val idx = if (q == null) -1 else s.open.indexOfFirst { it.id == q.id }
    val prev = if (idx > 0) s.open[idx - 1] else null
    val next = if (idx >= 0) s.open.getOrNull(idx + 1) else null
    val clock = rememberClock()
    val inf = rememberInfiniteTransition(label = "dex")
    val lensAlpha by inf.animateFloat(0.7f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "lens")

    Box(
        Modifier
            .fillMaxSize()
            .background(DexCase)
            .drawBehind {
                // rugged casing: a soft highlight and fine horizontal ridges
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.1f),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.2f, size.height * 0.1f),
                )
                val step = 5.dp.toPx()
                val h = 2.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawRect(Color.Black.copy(alpha = 0.06f), Offset(0f, y), Size(size.width, h))
                    y += step
                }
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            // lens, indicator lights and the counter
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(62.dp)
                        .dexShadow(DexInk, 3.dp, 31.dp)
                        .background(DexBone, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .alpha(lensAlpha)
                            .drawBehind {
                                val c = Offset(size.width * 0.35f, size.height * 0.3f)
                                drawCircle(
                                    Brush.radialGradient(
                                        0f to Color(0xFFC8F4FF), 0.5f to Color(0xFF2AA0D8), 1f to Color(0xFF0A4A7A),
                                        center = c, radius = size.width * 0.85f,
                                    ),
                                )
                            }
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(14.dp)
                        .drawBehind { drawCircle(Color(0xFFE2533A).copy(alpha = 0.35f), radius = size.minDimension) }
                        .background(Color(0xFFE2533A), CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(14.dp).background(Color(0xFFF2C84A), CircleShape))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(14.dp).background(Color(0xFF6AC46A), CircleShape))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("FIELD GUIDE", color = Color(0xFFB8C4A0), fontSize = 12.sp, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W500)
                    Text(
                        "SEEN ${s.quests.size} · CAUGHT ${s.done.size}",
                        color = DexYellow, fontSize = 17.sp, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W700,
                        textAlign = TextAlign.End, lineHeight = 20.sp, maxLines = 2,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // the screen
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DexInk, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 40.dp))
                    .padding(14.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DexLcd)
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(DexInk).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(if (q != null) "No. ${dexNo(s, q)}" else "No. ---", color = DexLcd, fontFamily = Fonts.silkscreen, fontSize = 12.sp)
                        Text(if (q != null) "NOT CAUGHT" else "NO SIGNAL", color = DexLcd, fontFamily = Fonts.silkscreen, fontSize = 12.sp)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 10.dp)) {
                            // the creature, under a scanning beam
                            Box(
                                Modifier
                                    .size(116.dp)
                                    .clip(RectangleShape)
                                    .background(Color(0xFFC4D4AC))
                                    .border(2.dp, DexInk)
                                    .tap { q?.let { a.openQuest(it) } }
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val line = 1.dp.toPx()
                                    var y = 0f
                                    while (y < size.height) {
                                        drawRect(DexInk.copy(alpha = 0.08f), Offset(0f, y), Size(size.width, line))
                                        y += 6.dp.toPx()
                                    }
                                    dexCreature(q != null, s.companion.c)
                                    val t = clock.value
                                    val sy = ((t % 1.8f) / 1.8f) * size.height
                                    drawRect(Color(0xB32AA0D8), Offset(0f, sy), Size(size.width, 2.dp.toPx()))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    q?.text?.uppercase() ?: "NOTHING NEARBY",
                                    color = DexInk, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W700,
                                    fontSize = 20.sp, lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (q != null) dexSpecies(q, s.now) else "The Empty Meadow",
                                    color = DexInk, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W500, fontSize = 12.sp,
                                )
                                if (q != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val sh = q.shelf(s.now)
                                        DexTag(sh.label.uppercase(), if (sh == Shelf.OVERDUE) Color(0xFFA83A2A) else Color(0xFF7A6A4A))
                                        if (q.kind == "by") DexTag("DEADLINE", Color(0xFFA83A2A)) else DexTag("EVENT", Color(0xFF3A6A8A))
                                    }
                                    Text(
                                        "HABITAT: your list, since ${Time.dayName(Time.date(q.created)).lowercase()}\n" +
                                            "WT: ${String.format("%.1f", 0.4f * q.text.trim().split(Regex("\\s+")).size)} oz\n" +
                                            "FLEES AT: ${dexFlees(q, s.now)}",
                                        color = DexInk, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W500,
                                        fontSize = 12.sp, lineHeight = 17.sp,
                                    )
                                } else {
                                    Text(
                                        "HABITAT: everywhere\nWT: 0 oz\nFLEES AT: n/a",
                                        color = DexInk, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W500,
                                        fontSize = 12.sp, lineHeight = 17.sp,
                                    )
                                }
                            }
                        }
                        // the field entry, written by the companion (or the scanner prompt)
                        Column(
                            Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                                .border(2.dp, DexInk)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (scanning || q == null) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.W700, color = s.companion.cDark)) {
                                            append(if (scanning) "Scanning… " else "Entry by ${s.companion.title}: ")
                                        }
                                        append(if (scanning) "describe the new creature." else s.line)
                                    },
                                    color = DexInk, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W500,
                                    fontSize = 14.sp, lineHeight = 20.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                SpillInput(
                                    onSpill = { a.spill(it); scanning = false },
                                    textStyle = TextStyle(color = DexInk, fontFamily = Fonts.silkscreen, fontSize = 13.sp),
                                    placeholder = "> new creature…",
                                    placeholderColor = DexInk.copy(alpha = 0.45f),
                                    cursor = DexInk,
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFC4D4AC)).padding(horizontal = 8.dp, vertical = 8.dp),
                                    trailing = { submit ->
                                        Text(
                                            "LOG IT", color = DexLcd, fontFamily = Fonts.silkscreen, fontSize = 11.sp,
                                            modifier = Modifier.background(DexInk).tap(submit).padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    },
                                )
                            } else {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.W700)) { append("Entry by ") }
                                        withStyle(SpanStyle(fontWeight = FontWeight.W700, color = s.companion.cDark)) { append(s.companion.title) }
                                        withStyle(SpanStyle(fontWeight = FontWeight.W700)) { append(": ") }
                                        append(s.line.takeIf { q.id == s.focus?.id } ?: s.companion.line(q, s.now))
                                    },
                                    color = DexInk, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W500,
                                    fontSize = 14.sp, lineHeight = 20.sp,
                                    modifier = Modifier.tap { a.openQuest(q) },
                                )
                            }
                        }
                    }
                    // browse neighbors
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (prev != null) "◀ ${dexNo(s, prev)} ${dexShortName(prev)}" else "◀ ---",
                            color = DexInk.copy(alpha = if (prev != null) 1f else 0.4f), fontFamily = Fonts.silkscreen, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).tap { prev?.let { selId = it.id } },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (next != null) "${dexNo(s, next)} ${dexShortName(next)} ▶" else "--- ▶",
                            color = DexInk.copy(alpha = if (next != null) 1f else 0.4f), fontFamily = Fonts.silkscreen, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f, fill = false).tap { next?.let { selId = it.id } },
                        )
                    }
                }
            }

            // rubber select/start style pills
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            ) {
                if (q != null) DexPill("LATER") { a.snooze(q, 60) }
                DexPill("MENU") { a.openOptions() }
            }

            // controls
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(96.dp)) {
                    Box(Modifier.offset(x = 32.dp).size(32.dp, 96.dp).background(DexInk, RoundedCornerShape(4.dp)))
                    Box(Modifier.offset(y = 32.dp).size(96.dp, 32.dp).background(DexInk, RoundedCornerShape(4.dp)))
                    Canvas(Modifier.fillMaxSize()) {
                        val u = size.width / 96f
                        val l = Path().apply { moveTo(10 * u, 48 * u); lineTo(20 * u, 41 * u); lineTo(20 * u, 55 * u); close() }
                        val r = Path().apply { moveTo(86 * u, 48 * u); lineTo(76 * u, 41 * u); lineTo(76 * u, 55 * u); close() }
                        drawPath(l, DexCase.copy(alpha = if (prev != null) 1f else 0.5f))
                        drawPath(r, DexCase.copy(alpha = if (next != null) 1f else 0.5f))
                    }
                    Box(Modifier.offset(y = 32.dp).size(32.dp).tap { prev?.let { selId = it.id } })
                    Box(Modifier.offset(x = 64.dp, y = 32.dp).size(32.dp).tap { next?.let { selId = it.id } })
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (q != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .dexShadow(Color(0xFFA88A20), 4.dp, 10.dp)
                                .background(DexYellow, RoundedCornerShape(10.dp))
                                .tap { a.done(q); selId = null }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("CATCH (done)", color = DexInk, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W700, fontSize = 16.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DexButton(if (scanning) "CANCEL" else "SCAN NEW", Modifier.weight(1f)) { scanning = !scanning }
                        DexButton("TRACK", Modifier.weight(1f)) { a.openLog() }
                    }
                }
            }
        }
    }
}

@Composable
private fun DexTag(text: String, color: Color) {
    Text(
        text, color = DexPaper, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W700, fontSize = 11.sp, maxLines = 1,
        modifier = Modifier.background(color, RoundedCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 1.dp),
    )
}

@Composable
private fun DexButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.background(DexInk, RoundedCornerShape(10.dp)).tap(onClick).padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = DexBone, fontFamily = Fonts.chakraPetch, fontWeight = FontWeight.W700, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun DexPill(label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.tap(onClick)) {
        Box(Modifier.size(46.dp, 12.dp).background(DexInk, RoundedCornerShape(6.dp)))
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color(0xFFB8C4A0), fontFamily = Fonts.silkscreen, fontSize = 10.sp)
    }
}

/** The grumpy envelope with legs, drawn from a 116×116 viewBox. The stamp wears the companion's color. */
private fun DrawScope.dexCreature(present: Boolean, stamp: Color) {
    val k = size.width / 116f
    fun o(x: Float, y: Float) = Offset(x * k, y * k)
    val ink = if (present) DexInk else DexInk.copy(alpha = 0.3f)
    val w25 = Stroke(width = 2.5f * k)
    drawRect(if (present) DexPaper else DexInk.copy(alpha = 0.18f), o(20f, 32f), Size(76 * k, 50 * k))
    drawRect(ink, o(20f, 32f), Size(76 * k, 50 * k), style = w25)
    drawPath(Path().apply { moveTo(20 * k, 32 * k); lineTo(58 * k, 62 * k); lineTo(96 * k, 32 * k) }, ink, style = w25)
    drawLine(ink, o(40f, 82f), o(40f, 96f), 3 * k, StrokeCap.Round)
    drawLine(ink, o(40f, 96f), o(34f, 96f), 3 * k, StrokeCap.Round)
    drawLine(ink, o(76f, 82f), o(76f, 96f), 3 * k, StrokeCap.Round)
    drawLine(ink, o(76f, 96f), o(82f, 96f), 3 * k, StrokeCap.Round)
    if (!present) return
    drawLine(ink, o(40f, 46f), o(50f, 50f), 3 * k, StrokeCap.Round)
    drawLine(ink, o(76f, 46f), o(66f, 50f), 3 * k, StrokeCap.Round)
    drawCircle(ink, 3 * k, o(46f, 54f))
    drawCircle(ink, 3 * k, o(70f, 54f))
    drawPath(Path().apply { moveTo(50 * k, 72 * k); quadraticBezierTo(58 * k, 67 * k, 66 * k, 72 * k) }, ink, style = w25)
    drawRect(stamp, o(74f, 36f), Size(14 * k, 10 * k))
    drawRect(ink, o(74f, 36f), Size(14 * k, 10 * k), style = Stroke(1.5f * k))
}

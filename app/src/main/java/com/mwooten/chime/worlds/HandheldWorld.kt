package com.mwooten.chime.worlds

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap

// the four shades, and nothing else on the LCD
private val Hh0 = Color(0xFF0F380F) // darkest
private val Hh1 = Color(0xFF306230)
private val Hh2 = Color(0xFF8BAC0F)
private val Hh3 = Color(0xFF9BBC0F) // lightest
private val HhNavy = Color(0xFF2A2A7A)

/** Maps any color to its brightness on the LCD's green ramp (darkest 0F380F to lightest 9BBC0F). */
private val hhLcdFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.165f, 0.324f, 0.060f, 0f, 15f,
            0.155f, 0.305f, 0.057f, 0f, 56f,
            0f, 0f, 0f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

private class HhPx(val x: Int, val y: Int, val w: Int, val h: Int, val c: Color)

/** The enemy: an angry envelope, 16x12. */
private val hhEnemy = listOf(
    HhPx(0, 1, 16, 11, Hh0), HhPx(1, 2, 14, 9, Hh2),
    HhPx(1, 2, 2, 1, Hh0), HhPx(3, 3, 2, 1, Hh0), HhPx(5, 4, 2, 1, Hh0), HhPx(7, 5, 2, 1, Hh0),
    HhPx(9, 4, 2, 1, Hh0), HhPx(11, 3, 2, 1, Hh0), HhPx(13, 2, 2, 1, Hh0),
    HhPx(4, 7, 2, 1, Hh0), HhPx(10, 7, 2, 1, Hh0),
    HhPx(6, 9, 4, 1, Hh0), HhPx(6, 8, 1, 1, Hh0), HhPx(9, 8, 1, 1, Hh0),
)

@Composable
private fun HhSprite(px: List<HhPx>, gw: Int, gh: Int, modifier: Modifier) {
    Canvas(modifier) {
        val cw = size.width / gw
        val ch = size.height / gh
        for (p in px) drawRect(p.c, topLeft = Offset(p.x * cw, p.y * ch), size = Size(p.w * cw + 0.5f, p.h * ch + 0.5f))
    }
}

/** Days in a row (ending today or yesterday) with at least one quest finished. */
private fun hhStreak(s: WorldState): Int {
    val days = s.done.mapNotNull { it.doneAt }.map { Time.date(it) }.toSet()
    var d = Time.date(s.now)
    if (d !in days) d = d.minusDays(1)
    var n = 0
    while (d in days) { n++; d = d.minusDays(1) }
    return n
}

@Composable
fun HandheldWorld(s: WorldState, a: WorldActions) {
    val q: Quest? = s.focus
    var sel by rememberSaveable { mutableStateOf(0) } // 0 DONE, 1 LATER, 2 LIST, 3 NEW
    var spilling by rememberSaveable { mutableStateOf(false) }
    val selected = if (q == null && sel < 2) 3 else sel
    val clock = rememberClock()
    val name = s.companion.title.uppercase()
    val voice = rememberVoice { heard -> a.spill(heard) }

    fun hhRun(item: Int) {
        when (item) {
            0 -> { if (q != null) a.done(q) }
            1 -> { if (q != null) a.snooze(q, 60) }
            2 -> { a.openLog() }
            else -> { spilling = !spilling }
        }
    }
    fun move(dx: Int, dy: Int) {
        var col = selected % 2
        var row = selected / 2
        col = (col + dx + 2) % 2
        row = (row + dy + 2) % 2
        if (q == null) row = 1
        sel = row * 2 + col
    }

    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFCFCBC2), Color(0xFFBAB5AA)))),
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(12.dp))

            // ---- screen bezel
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 44.dp, bottomStart = 10.dp))
                    .background(Color(0xFF5A5A6A))
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 26.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(2.dp).background(Color(0xFF8A2A5A)))
                    Text(
                        "CHIME POCKET · 4 SHADES",
                        style = TextStyle(fontFamily = Fonts.archivoNarrow, fontStyle = FontStyle.Italic, fontWeight = FontWeight.W700, fontSize = 10.sp, letterSpacing = 1.sp, color = Color(0xFFB8B8C8)),
                        maxLines = 1,
                    )
                    Box(Modifier.weight(1f).height(2.dp).background(Color(0xFF2A2A8A)))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    // power LED
                    Box(Modifier.width(18.dp).padding(top = 120.dp)) {
                        Box(
                            Modifier.size(8.dp).drawBehind {
                                drawCircle(Color(0x66FF3A2A), radius = size.minDimension)
                            }.clip(CircleShape).background(Color(0xFFFF3A2A)),
                        )
                    }
                    HhLcd(s, a, q, name, selected, spilling, clock, voice, onRun = { hhRun(it) }, onSelect = { sel = it }, onSpilled = { spilling = false }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(18.dp))
                }
            }

            // ---- wordmark on the body
            Text(
                buildAnnotatedString {
                    append("Chime ")
                    withStyle(SpanStyle(fontFamily = Fonts.silkscreen, fontStyle = FontStyle.Normal, fontSize = 14.sp)) { append("POCKET") }
                },
                style = TextStyle(fontFamily = Fonts.archivoNarrow, fontStyle = FontStyle.Italic, fontWeight = FontWeight.W700, fontSize = 22.sp, color = HhNavy),
                modifier = Modifier.padding(start = 8.dp, top = 10.dp),
            )

            // ---- d-pad and A/B
            Box(Modifier.fillMaxWidth().height(150.dp)) {
                HhDpad(
                    Modifier.align(Alignment.CenterStart).padding(start = 18.dp),
                    onUp = { move(0, -1) }, onDown = { move(0, 1) }, onLeft = { move(-1, 0) }, onRight = { move(1, 0) },
                )
                Box(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).width(150.dp).height(100.dp).rotate(-24f)) {
                    HhRoundButton("B", Modifier.align(Alignment.BottomStart)) { if (q != null) a.snooze(q, 60) }
                    HhRoundButton("A", Modifier.align(Alignment.TopEnd)) { hhRun(selected) }
                }
            }

            // ---- select / start, plus the speaker grille
            Box(Modifier.fillMaxWidth().height(86.dp)) {
                Row(
                    Modifier.align(Alignment.TopCenter).padding(top = 4.dp).rotate(-24f),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    HhPill("SETTINGS", a::openOptions)
                    HhPill("LIST", a::openLog)
                    HhPill("NEW") { spilling = !spilling }
                }
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 10.dp).rotate(-30f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(5) {
                        Box(
                            Modifier.width(6.dp).height(46.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF9A958A))
                                .drawBehind { drawRect(Color(0x33000000), size = Size(size.width, 3.dp.toPx())) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HhLcd(
    s: WorldState,
    a: WorldActions,
    q: Quest?,
    name: String,
    selected: Int,
    spilling: Boolean,
    clock: State<Float>,
    voice: Voice,
    onRun: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    onSpilled: () -> Unit,
    modifier: Modifier,
) {
    val txt = TextStyle(fontFamily = Fonts.silkscreen, fontSize = 10.sp, lineHeight = 15.sp, color = Hh0)
    Box(
        modifier.fillMaxSize().background(Hh3).border(2.dp, Hh2),
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp)) {
            // ---- battle field
            Box(Modifier.weight(1.3f).fillMaxWidth()) {
                if (q != null) {
                    val enemy = q.text.uppercase()
                    Column(
                        Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).tap { a.openQuest(q) },
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            enemy,
                            style = TextStyle(fontFamily = Fonts.silkscreen, fontWeight = FontWeight.W700, fontSize = 9.sp, color = Hh0),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 170.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("TIME", style = TextStyle(fontFamily = Fonts.silkscreen, fontSize = 8.sp, color = Hh0))
                            Spacer(Modifier.width(4.dp))
                            val left = q.due?.let { d ->
                                val span = (d - q.created).toFloat()
                                if (span <= 0f) 0f else ((d - s.now) / span).coerceIn(0f, 1f)
                            } ?: 1f
                            Box(Modifier.width(70.dp).height(6.dp).border(1.dp, Hh0).padding(1.dp)) {
                                Box(Modifier.fillMaxWidth(left).height(4.dp).background(Hh1))
                            }
                        }
                    }
                    HhSprite(
                        hhEnemy, 16, 12,
                        Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 22.dp).width(64.dp).height(48.dp)
                            .graphicsLayer {
                                val step = ((clock.value / 0.25f).toInt() % 2)
                                translationX = (if (step == 0) -2f else 2f) * density
                            }
                            .tap { a.openQuest(q) },
                    )
                }
                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // your character as a chunky sprite, pushed through the LCD's green so it sits in the four shades
                    val lcdPaint = remember { Paint().apply { colorFilter = hhLcdFilter } }
                    Box(
                        Modifier.size(64.dp)
                            .drawWithContent {
                                drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), lcdPaint) }
                                drawContent()
                                drawIntoCanvas { it.restore() }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        CharacterArt(
                            64.dp, ArtStyle.PIXEL, who = s.companion, level = voice.level, listening = voice.listening,
                            glow = false, pixels = 24, onTap = { voice.toggle() },
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$name  LV ${s.done.size + 1}", style = TextStyle(fontFamily = Fonts.silkscreen, fontWeight = FontWeight.W700, fontSize = 9.sp, color = Hh0))
                        Text("STREAK ${hhStreak(s)} DAYS", style = TextStyle(fontFamily = Fonts.silkscreen, fontSize = 8.sp, color = Hh0))
                        Text(
                            when {
                                voice.listening -> "LISTENING..."
                                voice.note.isNotBlank() -> voice.note.uppercase()
                                else -> "TAP ME TO TALK"
                            },
                            style = TextStyle(fontFamily = Fonts.silkscreen, fontSize = 8.sp, lineHeight = 10.sp, color = Hh1),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            // ---- battle text box (or the new-quest prompt)
            Box(
                Modifier.weight(1f).fillMaxWidth().border(2.dp, Hh0).background(Hh3).padding(8.dp),
            ) {
                if (spilling) {
                    Column {
                        Text("NAME THE NEW QUEST:", style = txt)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("> ", style = txt)
                            SpillInput(
                                onSpill = { a.spill(it); onSpilled() },
                                textStyle = txt,
                                placeholder = "TYPE HERE",
                                placeholderColor = Hh1,
                                cursor = Hh0,
                                modifier = Modifier.weight(1f),
                                trailing = { submit ->
                                    Text(
                                        "OK",
                                        style = txt.copy(fontWeight = FontWeight.W700),
                                        modifier = Modifier.padding(start = 6.dp).border(2.dp, Hh0).background(Hh2)
                                            .tap { submit() }.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                },
                            )
                        }
                    }
                } else if (voice.listening) {
                    Text(
                        "$name IS LISTENING...\n> " + voice.heard.uppercase().ifBlank { "_" },
                        style = txt,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                } else {
                    val story = if (q == null) "The path is clear. $name: ${s.line}"
                    else {
                        val e = q.text.uppercase()
                        val opener = if (q.shelf(s.now) == Shelf.OVERDUE) "$e is still on the field! It was due ${q.shortDue()}."
                        else if (q.due == null) "A wild $e appeared! It has no due date."
                        else "$e blocks the path! It's due ${q.shortDue()}."
                        "$opener $name: ${s.line}"
                    }
                    Text(
                        story, style = txt,
                        modifier = Modifier.verticalScroll(rememberScrollState()).tap { if (q != null) a.openQuest(q) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // ---- command menu
            Column(
                Modifier.fillMaxWidth().border(2.dp, Hh0).background(Hh2).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val labels = listOf("DONE", "LATER", "LIST", "NEW")
                for (r in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0..1) {
                            val idx = r * 2 + c
                            val enabled = q != null || idx >= 2
                            Row(
                                Modifier.weight(1f).tap { if (enabled) { onSelect(idx); onRun(idx) } },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "▶",
                                    style = TextStyle(fontSize = 10.sp, color = Hh0),
                                    modifier = Modifier.width(14.dp).graphicsLayer {
                                        alpha = if (idx == selected && (clock.value % 1f) < 0.5f) 1f else 0f
                                    },
                                )
                                Text(
                                    if (enabled) labels[idx] else "---",
                                    style = TextStyle(fontFamily = Fonts.silkscreen, fontWeight = FontWeight.W700, fontSize = 11.sp, color = if (enabled) Hh0 else Hh1),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        // the LCD's pixel grid, over everything
        Canvas(Modifier.matchParentSize()) {
            val step = 3.dp.toPx()
            val line = Color(0x0F0F380F)
            var y = 0f
            while (y < size.height) { drawRect(line, topLeft = Offset(0f, y), size = Size(size.width, 1f)); y += step }
            var x = 0f
            while (x < size.width) { drawRect(line, topLeft = Offset(x, 0f), size = Size(1f, size.height)); x += step }
        }
    }
}

@Composable
private fun HhDpad(modifier: Modifier, onUp: () -> Unit, onDown: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit) {
    Box(modifier.size(110.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val u = size.width / 110f
            val arm = 38f * u
            val r = androidx.compose.ui.geometry.CornerRadius(4f * u)
            // shadow, then the cross
            drawRoundRect(Color(0xFF0E0E12), topLeft = Offset(36f * u, 3f * u), size = Size(arm, 110f * u), cornerRadius = r)
            drawRoundRect(Color(0xFF0E0E12), topLeft = Offset(0f, 39f * u), size = Size(110f * u, arm), cornerRadius = r)
            drawRoundRect(Color(0xFF222228), topLeft = Offset(36f * u, 0f), size = Size(arm, 110f * u), cornerRadius = r)
            drawRoundRect(Color(0xFF222228), topLeft = Offset(0f, 36f * u), size = Size(110f * u, arm), cornerRadius = r)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF34343A), Color(0xFF222228)), center = Offset(55f * u, 55f * u), radius = 10f * u),
                radius = 10f * u, center = Offset(55f * u, 55f * u),
            )
        }
        Box(Modifier.align(Alignment.TopCenter).width(38.dp).height(36.dp).tap(onUp))
        Box(Modifier.align(Alignment.BottomCenter).width(38.dp).height(36.dp).tap(onDown))
        Box(Modifier.align(Alignment.CenterStart).width(36.dp).height(38.dp).tap(onLeft))
        Box(Modifier.align(Alignment.CenterEnd).width(36.dp).height(38.dp).tap(onRight))
    }
}

@Composable
private fun HhRoundButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(56.dp)
                .drawBehind { drawCircle(Color(0xFF4A0A28), center = Offset(size.width / 2, size.height / 2 + 4.dp.toPx())) }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFB03A6A), Color(0xFF7A1A44)),
                    ),
                )
                .tap(onClick),
        )
        Text(label, style = TextStyle(fontFamily = Fonts.silkscreen, fontWeight = FontWeight.W700, fontSize = 12.sp, color = HhNavy))
    }
}

@Composable
private fun HhPill(label: String, onClick: () -> Unit) {
    Column(
        Modifier.tap(onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.width(50.dp).height(12.dp)
                .drawBehind {
                    drawRoundRect(
                        Color(0xFF4A4A52), topLeft = Offset(0f, 2.dp.toPx()), size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    )
                }
                .clip(RoundedCornerShape(6.dp)).background(Color(0xFF7A7A84)),
        )
        Text(label, style = TextStyle(fontFamily = Fonts.silkscreen, fontWeight = FontWeight.W700, fontSize = 10.sp, color = HhNavy))
    }
}

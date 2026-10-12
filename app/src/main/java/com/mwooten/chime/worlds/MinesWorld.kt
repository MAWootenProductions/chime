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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap

private val MinesFace = Color(0xFFC0C0C0)
private val MinesShade = Color(0xFF808080)
private val MinesLight = Color(0xFFDFDFDF)
private val MinesDark = Color(0xFF0A0A0A)

/** 9×9 field. o = open, digit = quests nearby, F = flag, M = the armed mine (the focus quest), ? = covered. */
private val MinesMap = listOf(
    "ooo1F1ooo",
    "ooo111ooo",
    "oooooo111",
    "11ooooo1?",
    "F1oooo12?",
    "11ooo1M2?",
    "ooooo111?",
    "?????????",
    "?????????",
)

/** One bevel ring: [tl] on the top/left edges, [br] on the bottom/right, [inset] px in from the edge. */
private fun DrawScope.minesEdge(tl: Color, br: Color, inset: Float, w: Float) {
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    drawRect(tl, Offset(l, t), Size(r - l, w))
    drawRect(tl, Offset(l, t), Size(w, b - t))
    drawRect(br, Offset(l, b - w), Size(r - l, w))
    drawRect(br, Offset(r - w, t), Size(w, b - t))
}

/** The classic raised window/button: four-tone bevel. */
private fun Modifier.minesRaised(): Modifier = drawBehind {
    drawRect(MinesFace)
    val p = 1.dp.toPx()
    minesEdge(Color.White, MinesDark, 0f, p)
    minesEdge(MinesLight, MinesShade, p, p)
}

private fun Modifier.minesSunken(w: Int = 2): Modifier = drawBehind {
    minesEdge(MinesShade, Color.White, 0f, w.dp.toPx())
}

@Composable
fun MinesWorld(s: WorldState, a: WorldActions) {
    val f = s.focus
    var spilling by remember { mutableStateOf(false) }
    // the face button is your character: tap it to talk, and what you say becomes a new mine
    val voice = rememberVoice { heard -> a.spill(heard) }

    val due = f?.due
    val allDay = f?.allDay == true
    val deadline: Long? = if (due == null) null else if (allDay) Time.ms(Time.date(due).plusDays(1)) else due
    val mins = if (deadline == null) 0L else ((deadline - s.now) / 60_000).coerceIn(0L, 999L)
    val fuseLabel = if (due == null) "no fuse lit" else if (allDay) "min to midnight" else "min to ${Time.time(due)}"
    val digits = TextStyle(fontFamily = Fonts.silkscreen, fontSize = 28.sp, lineHeight = 28.sp, color = Color(0xFFFF1A1A), letterSpacing = 2.sp)
    val ui = TextStyle(fontFamily = FontFamily.SansSerif, color = Color.Black)

    Box(Modifier.fillMaxSize().background(Color(0xFF008080))) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 14.dp)) {
            Column(Modifier.weight(1f)) {
                Spacer(Modifier.height(6.dp))
                // the window
                Column(Modifier.weight(1f, fill = false).fillMaxWidth().minesRaised().padding(3.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .background(Brush.horizontalGradient(listOf(Color(0xFF000080), Color(0xFF1084D0))))
                            .padding(start = 6.dp, end = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Today Sweeper", style = ui, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        // title-bar close box opens the full log
                        Box(Modifier.size(16.dp, 14.dp).minesRaised().tap { a.openLog() }, contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(8.dp)) {
                                val w = 1.5.dp.toPx()
                                drawLine(Color.Black, Offset.Zero, Offset(size.width, size.height), w)
                                drawLine(Color.Black, Offset(size.width, 0f), Offset(0f, size.height), w)
                            }
                        }
                    }
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MinesMenu("G", "ame", ui) { spilling = !spilling }
                        MinesMenu("V", "iew", ui) { a.openLog() }
                        MinesMenu("S", "ettings", ui) { a.openOptions() }
                    }
                    Box(Modifier.weight(1f, fill = false).padding(6.dp).minesSunken().padding(8.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // counters + the companion's face button
                            Row(
                                Modifier.fillMaxWidth().minesSunken().padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(s.today.size.coerceIn(0, 999).toString().padStart(3, '0'), style = digits, modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 3.dp))
                                Box(
                                    Modifier.size(44.dp).drawBehind {
                                        drawRect(if (voice.listening) MinesLight else MinesFace)
                                        if (voice.listening) minesEdge(MinesShade, Color.White, 0f, 2.dp.toPx())
                                        else minesEdge(Color.White, MinesShade, 0f, 2.dp.toPx())
                                    },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CharacterArt(
                                        size = 34.dp,
                                        style = ArtStyle.PIXEL,
                                        level = voice.level,
                                        listening = voice.listening,
                                        glow = false,
                                        pixels = 16,
                                        onTap = { voice.toggle() },
                                    )
                                }
                                Text(mins.toString().padStart(3, '0'), style = digits, modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 3.dp))
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("quests left", style = ui, color = Color(0xFF404040), fontSize = 11.sp)
                                Text(fuseLabel, style = ui, color = Color(0xFF404040), fontSize = 11.sp, maxLines = 1)
                            }
                            // the field: a fixed decorative minefield with the focus quest armed
                            Box(
                                Modifier
                                    .weight(1f, fill = false)
                                    .aspectRatio(1f)
                                    .drawBehind { minesEdge(MinesShade, Color.White, 0f, 3.dp.toPx()) }
                                    .padding(3.dp)
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    MinesMap.forEach { row ->
                                        Row(Modifier.fillMaxWidth().weight(1f)) {
                                            row.forEach { ch ->
                                                MinesCell(
                                                    ch = if (ch == 'M' && f == null) 'o' else ch,
                                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                                    onArmed = { f?.let { a.openQuest(it) } },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // the companion as the status/tooltip bar
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${s.companion.title}: ") }
                        when {
                            voice.listening -> {
                                withStyle(SpanStyle(color = Color(0xFF000080))) { append(voice.heard.ifBlank { "Listening... say a new quest." }) }
                            }
                            voice.note.isNotBlank() -> append(voice.note)
                            else -> append(s.line)
                        }
                    },
                    style = ui, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 5, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFFE1))
                        .border(1.dp, Color.Black)
                        .tap { f?.let { a.openQuest(it) } }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )

                if (spilling || f == null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("New mine:", style = ui, fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(end = 6.dp))
                        SpillInput(
                            onSpill = { a.spill(it); spilling = false },
                            textStyle = ui.copy(fontSize = 14.sp),
                            placeholder = "call dentist by 3pm",
                            placeholderColor = Color(0xFF808080),
                            cursor = Color.Black,
                            modifier = Modifier.weight(1f),
                            trailing = { submit ->
                                Box(
                                    Modifier.padding(start = 6.dp).minesRaised().tap(submit).padding(horizontal = 12.dp, vertical = 6.dp),
                                ) { Text("OK", style = ui, fontSize = 13.sp) }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (f != null) {
                    MinesButton("Defuse (done)", true, Modifier.weight(1f)) { a.done(f) }
                    MinesButton("Move flag", false, Modifier.weight(1f)) { a.snooze(f, 30) }
                } else {
                    MinesButton("New game", true, Modifier.weight(1f)) { spilling = true }
                }
                MinesButton("Week view", false, Modifier.weight(1f)) { a.openLog() }
            }
        }
    }
}

@Composable
private fun MinesMenu(key: String, rest: String, ui: TextStyle, onClick: () -> Unit) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(key) }
            append(rest)
        },
        style = ui, fontSize = 12.sp, modifier = Modifier.tap(onClick),
    )
}

@Composable
private fun MinesCell(ch: Char, modifier: Modifier, onArmed: () -> Unit) {
    val glyph = TextStyle(fontFamily = Fonts.silkscreen, fontWeight = FontWeight.W700, fontSize = 16.sp)
    when (ch) {
        'o', '1', '2', '3' -> Box(
            modifier.background(MinesFace).drawBehind {
                val p = 1.dp.toPx()
                drawRect(MinesShade, Offset(size.width - p, 0f), Size(p, size.height))
                drawRect(MinesShade, Offset(0f, size.height - p), Size(size.width, p))
            },
            contentAlignment = Alignment.Center,
        ) {
            if (ch != 'o') {
                val c = when (ch) { '1' -> Color(0xFF0000FF); '2' -> Color(0xFF008000); else -> Color(0xFFFF0000) }
                Text(ch.toString(), style = glyph, color = c)
            }
        }
        'M' -> {
            val inf = rememberInfiniteTransition(label = "fuse")
            val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "pulse")
            Box(
                modifier.drawBehind {
                    drawRect(Color(0xFFFFD23A))
                    minesEdge(Color.White, MinesShade, 0f, 2.dp.toPx())
                }.tap(onArmed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "✉", fontFamily = FontFamily.SansSerif, fontSize = 15.sp, color = Color.Black,
                    modifier = Modifier.graphicsLayer {
                        val sc = 1f + 0.3f * pulse
                        scaleX = sc; scaleY = sc; alpha = 1f - 0.4f * pulse
                    },
                )
            }
        }
        else -> Box(
            modifier.drawBehind {
                drawRect(MinesFace)
                minesEdge(Color.White, MinesShade, 0f, 2.dp.toPx())
            },
            contentAlignment = Alignment.Center,
        ) {
            if (ch == 'F') Text("⚑", fontFamily = FontFamily.SansSerif, fontSize = 15.sp, color = Color(0xFFFF0000))
        }
    }
}

@Composable
private fun MinesButton(label: String, focused: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .minesRaised()
            .drawBehind {
                if (focused) {
                    val i = 6.dp.toPx()
                    val p = 1.dp.toPx()
                    drawRect(
                        Color.Black, Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i),
                        style = Stroke(width = p, pathEffect = PathEffect.dashPathEffect(floatArrayOf(p, p))),
                    )
                }
            }
            .tap(onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = Color.Black,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

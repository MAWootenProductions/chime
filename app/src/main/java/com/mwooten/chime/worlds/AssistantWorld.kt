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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlin.math.PI
import kotlin.math.sin

private val AsInk = Color(0xFFEEF2EC)
private val AsMuted = Color(0xFF9FB3A4)
private val AsPrev = Color(0xFF6F8577)
private val AsBg = Color(0xFF121815)
private val AsFace = Color(0xFFE9E1CF)

private val asNumbers = listOf("Nothing", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve")

private fun asCount(n: Int): String = asNumbers.getOrElse(n) { n.toString() }

@Composable
fun AssistantWorld(s: WorldState, a: WorldActions) {
    val items = s.open
    var index by rememberSaveable { mutableStateOf(0) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val voice = rememberVoice { heard -> a.spill(heard) }
    val i = if (items.isEmpty()) 0 else index.mod(items.size)
    val q: Quest? = items.getOrNull(i)
    val line = q?.let { s.companion.line(it, s.now) } ?: s.line
    val clock = rememberClock()
    val callSecs by remember(clock) { derivedStateOf { clock.value.toInt() } }

    val hour = Time.ldt(s.now).hour
    val part = when (hour) { in 4..11 -> "morning"; in 12..16 -> "afternoon"; else -> "evening" }
    val withDeadline = s.today.size
    val intro = if (items.isEmpty()) "Good $part. Your list is clear."
    else "Good $part. ${asCount(items.size)} ${if (items.size == 1) "thing" else "things"} on your list, " +
        (if (withDeadline == 0) "none due today." else "${asCount(withDeadline).lowercase()} due today.")

    Box(
        Modifier.fillMaxSize().background(AsBg).drawBehind {
            drawRect(
                Brush.radialGradient(
                    0f to Color(0xFF2A3A30), 1f to AsBg,
                    center = Offset(size.width / 2f, 0f),
                    radius = size.height * 0.55f,
                ),
            )
        },
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // top bar: call status on the left, Settings where a call screen keeps its menu
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF7BD88F)))
                Spacer(Modifier.width(6.dp))
                Text(
                    "On a call",
                    style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W500, fontSize = 13.sp, color = AsMuted),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Settings",
                    style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W700, fontSize = 14.sp, color = AsInk),
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x1AEEF2EC))
                        .border(1.dp, Color(0x29EEF2EC), RoundedCornerShape(50))
                        .tap(a::openOptions).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }

            // who's on the line and the call timer
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "${part.uppercase()} BRIEFING",
                    style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W500, fontSize = 13.sp, letterSpacing = 1.sp, color = AsMuted),
                )
                Text(s.companion.title, style = TextStyle(fontFamily = Fonts.libreCaslonText, fontSize = 30.sp, color = AsInk))
                val mm = (callSecs / 60).toString().padStart(2, '0')
                val ss = (callSecs % 60).toString().padStart(2, '0')
                Text("$mm:$ss", style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W500, fontSize = 14.sp, color = AsMuted))
            }

            Spacer(Modifier.height(18.dp))
            AsAvatar(s, voice)
            Text(
                if (voice.listening) "Listening… tap again when you're done" else "Tap ${s.companion.title} to talk",
                style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, color = AsMuted),
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(18.dp))

            // live captions
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.width(13.dp).height(18.dp)) {
                        val t = clock.value
                        val bw = 3.dp.toPx()
                        for (k in 0..2) {
                            val wave = (sin((t / 0.5f) * 2 * PI.toFloat() - k * 0.3f * 2 * PI.toFloat()) + 1f) / 2f
                            val ph = if (voice.listening) (0.2f + voice.level) * wave else wave
                            val h = (6f + 12f * ph.coerceIn(0f, 1f)).dp.toPx()
                            drawRect(Color(0xFFB9D8A8), topLeft = Offset(k * (bw + 2.dp.toPx()), (size.height - h) / 2), size = Size(bw, h))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (voice.listening) "Live captions · you're talking"
                        else if (q == null) "Live captions · nothing on your list"
                        else "Live captions · item ${i + 1} of ${items.size}",
                        style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, color = AsMuted),
                    )
                }
                if (voice.listening || voice.heard.isNotBlank() || voice.note.isNotBlank()) {
                    // your side of the call
                    val mine = when {
                        voice.listening -> "You: " + voice.heard.ifBlank { "…" }
                        voice.note.isNotBlank() -> voice.note
                        else -> "You: “${voice.heard}” (added to your list)"
                    }
                    Text(
                        mine,
                        style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W500, fontSize = 17.sp, lineHeight = 24.sp, color = Color(0xFFB9D8A8)),
                    )
                }
                if (!voice.listening) {
                    Text(
                        if (i == 0 || q == null) intro else "Next up, item ${i + 1}.",
                        style = TextStyle(fontFamily = Fonts.libreCaslonText, fontSize = 22.sp, lineHeight = 30.sp, color = AsPrev),
                    )
                    Text(
                        line,
                        style = TextStyle(fontFamily = Fonts.libreCaslonText, fontSize = 25.sp, lineHeight = 34.sp, color = AsInk),
                        modifier = Modifier.tap { if (q != null) a.openQuest(q) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // plain answers you can tap
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (q != null) {
                    AsReply("Done ✓", primary = true, modifier = Modifier.fillMaxWidth()) { a.done(q) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsReply("Later (1 hour)", primary = false, modifier = Modifier.weight(1f)) { a.snooze(q, 60) }
                        AsReply(
                            if (items.size > 1) "Next item" else "Full list",
                            primary = false, modifier = Modifier.weight(1f),
                        ) { if (items.size > 1) { index = i + 1 } else { a.openLog() } }
                    }
                } else if (!adding) {
                    AsReply("Add a to-do", primary = true, modifier = Modifier.fillMaxWidth()) { adding = true }
                }
                if (adding) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Color(0x14EEF2EC)).border(1.dp, Color(0x29EEF2EC), RoundedCornerShape(14.dp))
                            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpillInput(
                            onSpill = { a.spill(it); adding = false },
                            textStyle = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W700, fontSize = 16.sp, color = AsInk),
                            placeholder = "Type a to-do, like “call mom at 5”",
                            placeholderColor = AsPrev,
                            cursor = Color(0xFFB9D8A8),
                            modifier = Modifier.weight(1f),
                            trailing = { submit ->
                                Text(
                                    "Add",
                                    style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W700, fontSize = 14.sp, color = AsBg),
                                    modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(10.dp)).background(AsInk)
                                        .tap { submit() }.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            },
                        )
                    }
                }
            }

            // call controls, each with a plain label underneath
            Row(
                Modifier.padding(top = 18.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                AsControl("Type") {
                    AsRoundIcon(Modifier, size = 58, bg = if (adding) Color(0x33EEF2EC) else Color(0x1AEEF2EC), onClick = { adding = !adding }) { col ->
                        val w = 2.dp.toPx(); val c = size.width / 2; val h = 6.dp.toPx()
                        drawLine(col, Offset(c, c - h), Offset(c, c + h), strokeWidth = w, cap = StrokeCap.Round)
                        drawLine(col, Offset(c - h, c), Offset(c + h, c), strokeWidth = w, cap = StrokeCap.Round)
                    }
                }
                AsControl(if (voice.listening) "Stop" else "Talk") {
                    AsRoundIcon(Modifier, size = 58, bg = if (voice.listening) AsInk else Color(0x1AEEF2EC), onClick = { voice.toggle() }) { _ ->
                        val col = if (voice.listening) AsBg else AsInk
                        val ox = size.width / 2 - 10.dp.toPx(); val oy = size.height / 2 - 10.dp.toPx()
                        val sw = 1.8.dp.toPx()
                        drawRoundRect(
                            col, topLeft = Offset(ox + 7.dp.toPx(), oy + 2.dp.toPx()), size = Size(6.dp.toPx(), 10.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()), style = Stroke(sw),
                        )
                        val p = Path().apply {
                            moveTo(ox + 4.dp.toPx(), oy + 10.dp.toPx())
                            cubicTo(ox + 4.dp.toPx(), oy + 18.dp.toPx(), ox + 16.dp.toPx(), oy + 18.dp.toPx(), ox + 16.dp.toPx(), oy + 10.dp.toPx())
                            moveTo(ox + 10.dp.toPx(), oy + 16.dp.toPx())
                            lineTo(ox + 10.dp.toPx(), oy + 19.dp.toPx())
                        }
                        drawPath(p, col, style = Stroke(sw))
                    }
                }
                // end the briefing: hang up and look at the whole list
                AsControl("Full list") {
                    AsRoundIcon(Modifier, size = 58, bg = Color(0xFFD4453A), onClick = a::openLog) { _ ->
                        val sc = 24.dp.toPx() / 24f
                        val ox = size.width / 2 - 12.dp.toPx(); val oy = size.height / 2 - 6.dp.toPx()
                        fun X(x: Float) = ox + x * sc
                        fun Y(y: Float) = oy + y * sc
                        val p = Path().apply {
                            moveTo(X(2f), Y(8f))
                            cubicTo(X(8f), Y(2f), X(16f), Y(2f), X(22f), Y(8f))
                            lineTo(X(19f), Y(11f)); lineTo(X(15f), Y(9f)); lineTo(X(15f), Y(6f))
                            cubicTo(X(12f), Y(5f), X(12f), Y(5f), X(9f), Y(6f))
                            lineTo(X(9f), Y(9f)); lineTo(X(5f), Y(11f)); close()
                        }
                        drawPath(p, Color.White)
                    }
                }
            }
        }
    }
}

/** A round call button with a short plain-words label under it. */
@Composable
private fun AsControl(label: String, button: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        button()
        Text(label, style = TextStyle(fontFamily = Fonts.manrope, fontWeight = FontWeight.W500, fontSize = 12.sp, color = AsMuted))
    }
}

/** The caller: the user's own character, glossy, inside soft "on call" rings. Tap to talk. */
@Composable
private fun AsAvatar(s: WorldState, voice: Voice) {
    Box(Modifier.size(152.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(152.dp)) {
            val r = 62.dp.toPx()
            val swell = if (voice.listening) voice.level * 8.dp.toPx() else 0f
            drawCircle(AsFace.copy(alpha = 0.05f), r + 14.dp.toPx() + swell)
            drawCircle(AsFace.copy(alpha = 0.12f), r + 6.dp.toPx() + swell * 0.5f)
        }
        CharacterArt(
            124.dp, ArtStyle.SHINY, who = s.companion, level = voice.level, listening = voice.listening,
            glow = false, onTap = { voice.toggle() },
        )
    }
}

@Composable
private fun AsReply(text: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.clip(shape)
            .background(if (primary) AsInk else Color(0x14EEF2EC))
            .then(if (primary) Modifier else Modifier.border(1.dp, Color(0x29EEF2EC), shape))
            .tap(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text,
            style = TextStyle(
                fontFamily = Fonts.manrope, fontWeight = FontWeight.W700,
                fontSize = if (primary) 16.sp else 15.sp,
                color = if (primary) AsBg else AsInk,
                textAlign = TextAlign.Start,
            ),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AsRoundIcon(
    modifier: Modifier,
    size: Int,
    bg: Color = Color(0x1AEEF2EC),
    onClick: () -> Unit,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit,
) {
    Box(
        modifier.size(size.dp).clip(CircleShape).background(bg).tap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size.dp)) { draw(AsInk) }
    }
}

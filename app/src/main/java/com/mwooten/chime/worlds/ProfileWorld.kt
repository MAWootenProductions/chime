package com.mwooten.chime.worlds

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap
import java.time.format.DateTimeFormatter

private val ProfPink = Color(0xFFFF6AD5)
private val ProfCyan = Color(0xFF6AE0FF)
private val ProfGreen = Color(0xFF6AFF8A)
private val ProfBlue = Color(0xFF2A4AB8)
private val ProfText = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = Color(0xFFF0E0FF))

private val ProfTiles = listOf(
    ProfPink to Color(0xFF3A1060),
    ProfCyan to Color(0xFF10304A),
    ProfGreen to Color(0xFF0F3A1A),
    Color(0xFFFFD23A) to Color(0xFF3A2A08),
)

private val ProfStamp = DateTimeFormatter.ofPattern("M/d h:mm a")

private fun profSlug(text: String): String =
    text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').take(32).ifEmpty { "untitled" }

private fun profMood(s: WorldState): String = when {
    s.overdueCount > 0 -> "behind :/"
    s.today.isNotEmpty() -> "procrastinating :/"
    s.open.isNotEmpty() -> "chillin"
    else -> "accomplished :)"
}

@Composable
fun ProfileWorld(s: WorldState, a: WorldActions) {
    val top = s.open.take(8)
    val first = top.firstOrNull()
    val c = s.companion
    val other = remember(c) {
        val all = enumValues<com.mwooten.chime.data.Companion>()
        all[(c.ordinal + 2) % all.size]
    }
    var adding by rememberSaveable { mutableStateOf(false) }
    val anim = rememberInfiniteTransition(label = "profile")
    val scroll by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "song")
    val sparkle by anim.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "sparkle")
    val postHolder = remember { arrayOfNulls<() -> Unit>(1) }
    val voice = rememberVoice { heard -> a.spill(heard) }

    Column(
        Modifier.fillMaxSize()
            .drawBehind {
                drawRect(Color(0xFF0A0010))
                val tile = 60.dp.toPx()
                var ty = 0f
                while (ty < size.height) {
                    var tx = 0f
                    while (tx < size.width) {
                        drawCircle(ProfPink, radius = 1.dp.toPx(), center = Offset(tx + 10.dp.toPx(), ty + 12.dp.toPx()))
                        drawCircle(Color.White, radius = 0.7f.dp.toPx(), center = Offset(tx + 30.dp.toPx(), ty + 40.dp.toPx()))
                        drawCircle(ProfCyan, radius = 1.dp.toPx(), center = Offset(tx + 50.dp.toPx(), ty + 22.dp.toPx()))
                        tx += tile
                    }
                    ty += tile
                }
            }
            .statusBarsPadding()
            .imePadding()
    ) {
        // site strip
        Row(
            Modifier.fillMaxWidth().height(28.dp).background(ProfBlue).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("chimespace", style = ProfText.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White), maxLines = 1)
            val link = ProfText.copy(fontSize = 10.sp, color = Color.White)
            Text("Home", style = link, maxLines = 1)
            Text("|", style = link)
            Text("Browse", style = link, maxLines = 1, modifier = Modifier.tap(a::openLog))
            Text("|", style = link)
            Text("Mail (${s.overdueCount})", style = link, maxLines = 1, modifier = Modifier.tap(a::openLog))
            Text("|", style = link)
            Text(
                "Settings",
                style = link.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE14A), textDecoration = TextDecoration.Underline),
                maxLines = 1, modifier = Modifier.tap(a::openOptions),
            )
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // profile head
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // profile pic: your character, drawn flat like a scanned sticker. Tap it to talk.
                Box(
                    Modifier.size(110.dp).border(3.dp, if (voice.listening) ProfGreen else ProfPink)
                        .background(Brush.linearGradient(listOf(Color(0xFF3A1060), Color(0xFF10102A))))
                        .padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CharacterArt(
                        96.dp, ArtStyle.FLAT, who = c, level = voice.level, listening = voice.listening,
                        glow = false, onTap = { voice.toggle() },
                    )
                    Text(
                        "✦",
                        style = ProfText.copy(fontSize = 14.sp, color = Color.White),
                        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).graphicsLayer { alpha = sparkle },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        "~*${c.title}*~",
                        style = TextStyle(
                            fontFamily = Fonts.pacifico, fontSize = 24.sp, lineHeight = 30.sp, color = ProfPink,
                            shadow = Shadow(ProfPink, Offset.Zero, 16f),
                        ),
                        maxLines = 1,
                    )
                    Text(
                        when {
                            voice.listening -> "\"" + voice.heard.ifBlank { "..." } + "\""
                            voice.note.isNotBlank() -> voice.note
                            else -> "\"do it later\" -- you, every time"
                        },
                        style = ProfText, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    ProfField("Mood:", profMood(s))
                    ProfField("To-dos:", "${s.open.size} open, ${s.done.size} done")
                    ProfField("Last login:", Time.time(s.now))
                    Text(
                        if (voice.listening) "● Listening… (tap pic to stop)" else "● Online Now! (tap pic to talk)",
                        style = ProfText.copy(color = ProfGreen, fontWeight = FontWeight.Bold),
                    )
                }
            }

            // profile song player
            Row(
                Modifier.fillMaxWidth().height(26.dp).border(1.dp, ProfCyan).background(Color(0xFF10102A)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("▶", style = ProfText.copy(color = ProfCyan))
                Box(Modifier.weight(1f).clipToBounds()) {
                    val song = if (first != null) "now playing: ${profSlug(first.text)}_due_${profSlug(first.shortDue())}_(acoustic).mp3"
                    else "now playing: silence_(nothing_due).mp3"
                    Text(
                        song, style = ProfText, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible,
                        modifier = Modifier.graphicsLayer { translationX = (1f - 2f * scroll) * size.width },
                    )
                }
            }

            // Top 8 = your top 8 open quests
            Column(Modifier.fillMaxWidth().border(1.dp, ProfPink).background(Color(0xD9140028))) {
                Text(
                    "My Top 8 (to-dos)",
                    style = ProfText.copy(color = Color(0xFF10001A), fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().background(ProfPink).padding(horizontal = 6.dp, vertical = 3.dp),
                )
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (col in 0 until 4) {
                                val i = row * 4 + col
                                ProfTile(top.getOrNull(i), i, s.now, Modifier.weight(1f), a)
                            }
                        }
                    }
                }
            }

            // comments, from the companion and one friend
            Column(Modifier.fillMaxWidth().border(1.dp, ProfCyan).background(Color(0xE60A0A28))) {
                Text(
                    "Friends Comments · Displaying 2 of 2",
                    style = ProfText.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().background(ProfBlue).padding(horizontal = 6.dp, vertical = 3.dp),
                )
                ProfComment(c, s.now, s.line, divider = true, mine = true)
                val otherLine = when {
                    s.open.size > 1 -> other.line(s.open[1], s.now)
                    first != null -> other.line(first, s.now)
                    else -> other.emptyLine
                }
                ProfComment(other, s.now - 4 * 60_000L, otherLine, divider = false, mine = false)
            }
        }

        if (adding || first == null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                    .border(1.dp, ProfCyan).background(Color(0xFF10102A)).padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    SpillInput(
                        onSpill = { a.spill(it); adding = false },
                        textStyle = ProfText.copy(fontSize = 13.sp, color = Color.White),
                        placeholder = "what's on ur mind?",
                        placeholderColor = Color(0xFF8A8AB0),
                        cursor = ProfCyan,
                        modifier = Modifier.fillMaxWidth(),
                        trailing = { submit -> postHolder[0] = submit },
                    )
                }
                Text(
                    "Post",
                    style = ProfText.copy(color = Color(0xFF10001A), fontWeight = FontWeight.Bold),
                    modifier = Modifier.background(ProfCyan).tap { postHolder[0]?.invoke() }.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (first != null) ProfButton("#1 Done ✓", ProfGreen, Modifier.weight(1f)) { a.done(first) }
            ProfButton(if (adding) "Cancel" else "Add a to-do", ProfCyan, Modifier.weight(1f)) { adding = !adding }
            ProfButton("Full list", ProfPink, Modifier.weight(1f), onClick = a::openLog)
        }
    }
}

@Composable
private fun ProfField(label: String, value: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = ProfCyan, fontWeight = FontWeight.Bold)) { append(label) }
            append(" ")
            append(value)
        },
        style = ProfText, maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ProfTile(q: Quest?, i: Int, now: Long, modifier: Modifier, a: WorldActions) {
    val (edge, fill) = ProfTiles[i % ProfTiles.size]
    Column(
        modifier.then(if (q != null) Modifier.tap { a.openQuest(q) } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (q != null) {
            val late = q.shelf(now) == Shelf.OVERDUE
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).border(2.dp, if (late) Color(0xFFFF5A5A) else edge).background(fill),
                contentAlignment = Alignment.Center,
            ) {
                Text("${i + 1}", style = ProfText.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
            Text(
                q.text,
                style = ProfText.copy(fontSize = 10.sp, lineHeight = 12.sp, color = ProfCyan, textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, Color(0xFF3A3A6A)).background(Color(0xFF10102A)),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", style = ProfText.copy(fontSize = 16.sp, color = Color(0xFF3A3A6A)))
            }
            Text(" ", style = ProfText.copy(fontSize = 10.sp, lineHeight = 12.sp))
        }
    }
}

@Composable
private fun ProfComment(who: com.mwooten.chime.data.Companion, at: Long, text: String, divider: Boolean, mine: Boolean) {
    Row(
        Modifier.fillMaxWidth().drawBehind {
            if (divider) {
                val p = 1.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color(0xFF3A3A6A), Offset(x, size.height - p / 2), Offset(x + 3 * p, size.height - p / 2), strokeWidth = p)
                    x += 6 * p
                }
            }
        }.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (mine) {
            // your character's own comment wears the same flat sticker as the profile pic
            Box(Modifier.size(40.dp).background(Color(0xFF10102A)).border(2.dp, who.c), contentAlignment = Alignment.Center) {
                CharacterArt(34.dp, ArtStyle.FLAT, who = who, glow = false)
            }
        } else {
            Box(
                Modifier.size(40.dp)
                    .background(Brush.radialGradient(listOf(who.cLight, who.c, who.cDark)))
                    .border(2.dp, who.c)
            )
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = who.c, fontWeight = FontWeight.Bold)) { append(who.title) }
                append(" ")
                withStyle(SpanStyle(color = Color(0xFF8A8AB0))) { append(ProfStamp.format(Time.ldt(at))) }
                append("\n")
                append(text)
            },
            style = ProfText.copy(fontSize = 12.sp, lineHeight = 17.sp),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ProfButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.border(1.dp, color).tap(onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = ProfText.copy(color = color, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

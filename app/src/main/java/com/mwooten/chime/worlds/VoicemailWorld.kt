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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.abs
import kotlin.math.sin

private val VmBg = Color(0xFF111315)
private val VmCard = Color(0xFF1B1E21)
private val VmCardLine = Color(0xFF2A2E33)
private val VmText = Color(0xFFE9EAEC)
private val VmMuted = Color(0xFF8A8F96)
private val VmFaint = Color(0xFF6A6F76)
private val VmKeyBg = Color(0xFF1F2226)

private val vmOnes = listOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
)
private val vmTens = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
private val vmOrdinals = listOf("First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth")

/** 0..99 spelled out the way the voicemail lady says it. */
private fun vmWords(n: Int): String = when {
    n < 0 -> n.toString()
    n < 20 -> vmOnes[n]
    n < 100 -> vmTens[n / 10] + if (n % 10 != 0) "-" + vmOnes[n % 10] else ""
    else -> n.toString()
}

/** "nine forty-four A.M." */
private fun vmTimeWords(ms: Long): String {
    val t = Time.ldt(ms)
    val h12 = if (t.hour % 12 == 0) 12 else t.hour % 12
    val m = t.minute
    val mins = when {
        m == 0 -> ""
        m < 10 -> " oh " + vmOnes[m]
        else -> " " + vmWords(m)
    }
    return vmWords(h12) + mins + if (t.hour < 12) " A.M." else " P.M."
}

/** Fake but stable message length in seconds, from how much the companion has to say. */
private fun vmSeconds(line: String): Int = (6 + line.length / 7).coerceIn(6, 59)

private fun vmClock(sec: Int): String = "0:" + sec.toString().padStart(2, '0')

@Composable
fun VoicemailWorld(s: WorldState, a: WorldActions) {
    val msgs: List<Quest> = if (s.today.isNotEmpty()) s.today else s.open
    var index by rememberSaveable { mutableStateOf(0) }
    var spilling by rememberSaveable { mutableStateOf(false) }
    val i = if (msgs.isEmpty()) 0 else index.mod(msgs.size)
    val q: Quest? = msgs.getOrNull(i)
    val line = q?.let { s.companion.line(it, s.now) } ?: s.line
    val clock = rememberClock()
    val voice = rememberVoice { heard -> a.spill(heard) }

    Box(Modifier.fillMaxSize().background(VmBg)) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 14.dp),
        ) {
            // header
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 2.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Voicemail",
                    style = TextStyle(fontFamily = Fonts.barlowCondensed, fontWeight = FontWeight.W700, fontSize = 30.sp, letterSpacing = 0.5.sp, color = VmText),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${msgs.size} new",
                    style = TextStyle(fontFamily = Fonts.barlow, fontSize = 13.sp, color = VmMuted),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Settings",
                    style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W600, fontSize = 15.sp, color = Color(0xFF8AB0F0)),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).tap { a.openOptions() }.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }

            // the robotic greeting + the message card scroll if space runs out
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(14.dp))
                val greet = buildAnnotatedString {
                    val hi = SpanStyle(color = VmText)
                    append("“You have ")
                    if (q == null) {
                        withStyle(hi) { append("no") }
                        append(" new messages.”")
                    } else {
                        withStyle(hi) { append(vmWords(msgs.size)) }
                        append(if (msgs.size == 1) " new message. " else " new messages. ")
                        append(vmOrdinals.getOrElse(i) { "Next" })
                        append(" new message, received ")
                        val day = Time.dayName(Time.date(q.created))
                        append(if (day == "Today" || day == "Yesterday") day.lowercase() else "on $day")
                        append(", at ")
                        withStyle(hi) { append(vmTimeWords(q.created)) }
                        append("”")
                    }
                }
                Text(
                    greet,
                    style = TextStyle(fontFamily = Fonts.barlow, fontSize = 14.sp, lineHeight = 20.sp, color = VmMuted),
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                Spacer(Modifier.height(18.dp))
                VmMessageCard(s, q, line, clock, voice, onOpen = { q?.let(a::openQuest) })
                Spacer(Modifier.height(16.dp))
            }

            // prompt, live recording, or the star-key spill line
            if (voice.listening) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFFE5534B))) { append("● REC  ") }
                        append(if (voice.heard.isNotBlank()) voice.heard + "…" else "Speak after the tone. Tap the caller to stop.")
                    },
                    style = TextStyle(fontFamily = Fonts.barlow, fontSize = 15.sp, lineHeight = 20.sp, color = VmText, textAlign = TextAlign.Center),
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                )
            } else if (spilling) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(14.dp)).background(Color(0xFF20283A))
                        .border(1.dp, Color(0xFF34405A), RoundedCornerShape(14.dp))
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpillInput(
                        onSpill = { a.spill(it); spilling = false },
                        textStyle = TextStyle(fontFamily = Fonts.barlow, fontSize = 16.sp, color = VmText),
                        placeholder = "After the tone, say what's on your mind",
                        placeholderColor = VmFaint,
                        cursor = Color(0xFF8AB0F0),
                        modifier = Modifier.weight(1f),
                        trailing = { submit ->
                            Text(
                                "SEND",
                                style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W700, fontSize = 12.sp, letterSpacing = 1.sp, color = Color(0xFF8AB0F0)),
                                modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(10.dp)).tap { submit() }.padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        },
                    )
                    Text(
                        "✕",
                        style = TextStyle(fontSize = 14.sp, color = VmMuted),
                        modifier = Modifier.tap { spilling = false }.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            } else if (voice.note.isNotBlank()) {
                Text(
                    voice.note,
                    style = TextStyle(fontFamily = Fonts.barlow, fontSize = 13.sp, lineHeight = 18.sp, color = VmMuted, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                )
            } else {
                Text(
                    if (q != null) "“To mark done, press 7. To save for later, press 9. To leave yourself a new one, press star. For all messages, press 1.”"
                    else "“To leave yourself a new one, press star. For all messages, press 1.”",
                    style = TextStyle(fontFamily = Fonts.barlow, fontSize = 13.sp, lineHeight = 18.sp, color = VmMuted, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            // keypad, mapped to quest actions
            val hasMsg = q != null
            Column(
                Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VmKey("1", "ALL", VmKeyBg, VmMuted) { a.openLog() }
                    VmKey("2") {}
                    VmKey("3") {}
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (hasMsg) VmKey("7", "DONE", Color(0xFF2F9E5B), VmText) { q?.let(a::done) }
                    else VmKey("7") {}
                    VmKey("8") {}
                    if (hasMsg) VmKey("9", "SAVE", Color(0xFF3A3020), Color(0xFFE0B050)) { q?.let { a.snooze(it, 60) } }
                    else VmKey("9") {}
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VmKey("*", "SPILL", Color(0xFF20283A), Color(0xFF8AB0F0)) { spilling = !spilling }
                    VmKey("0", "SETTINGS", VmKeyBg, VmMuted) { a.openOptions() }
                    VmKey("#", "NEXT", VmKeyBg, VmMuted, enabled = msgs.size > 1) { index = i + 1 }
                }
            }
        }
    }
}

@Composable
private fun VmMessageCard(s: WorldState, q: Quest?, line: String, clock: State<Float>, voice: Voice, onOpen: () -> Unit) {
    val comp = s.companion
    val total = vmSeconds(line)
    val urgent = q != null && q.shelf(s.now).let { it == Shelf.OVERDUE || it == Shelf.TODAY }
    val played = remember(total, clock) { derivedStateOf { (clock.value % total).toInt() } }
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VmCard)
            .border(1.dp, VmCardLine, RoundedCornerShape(14.dp))
            .tap(onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // caller art: your character, in its own clipped tile inside the card so it can never
            // drift over the keypad or the controls. Tap it to record a new message.
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(VmKeyBg),
                contentAlignment = Alignment.Center,
            ) {
                CharacterArt(
                    size = 58.dp, style = ArtStyle.SHINY, level = voice.level, listening = voice.listening,
                    glow = false, onTap = { voice.toggle() },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(comp.title, style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W700, fontSize = 16.sp, color = VmText))
                Text(
                    if (voice.listening) "recording… tap to stop" else "tap to leave a message",
                    style = TextStyle(fontFamily = Fonts.barlow, fontSize = 11.sp, color = if (voice.listening) Color(0xFFE5534B) else VmFaint),
                )
                Text(
                    if (q != null) "${vmClock(total)} · re: ${q.text}" else "${vmClock(total)} · greeting",
                    style = TextStyle(fontFamily = Fonts.barlow, fontSize = 12.sp, color = VmMuted),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (urgent) {
                Spacer(Modifier.width(8.dp))
                Text("URGENT", style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W700, fontSize = 12.sp, color = comp.c))
            } else if (q != null) {
                Spacer(Modifier.width(8.dp))
                Text(q.shortDue(), style = TextStyle(fontFamily = Fonts.barlow, fontSize = 12.sp, color = VmMuted), maxLines = 1)
            }
        }

        // waveform: bar heights come from the message itself, the played part lights up
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            val c = comp.c
            val dim = Color(
                red = c.red * 0.3f + VmCard.red * 0.7f,
                green = c.green * 0.3f + VmCard.green * 0.7f,
                blue = c.blue * 0.3f + VmCard.blue * 0.7f,
            )
            val seed = line.hashCode()
            val loud = urgent
            Canvas(Modifier.weight(1f).height(44.dp)) {
                val t = clock.value
                val frac = (t % total) / total
                val n = 30
                val bw = 3.dp.toPx()
                val gap = 2.dp.toPx()
                for (k in 0 until n) {
                    val r = abs((seed * 31 + k * 7919) % 100) / 100f
                    var h = if (loud) 0.35f + r * 0.9f else 0.2f + r * 0.7f
                    h = h.coerceAtMost(1f)
                    val wob = 0.75f + 0.25f * sin(t * 6f + k)
                    val bh = size.height * h * if (k.toFloat() / n < frac) wob else 1f
                    val x = k * (bw + gap)
                    if (x + bw > size.width) break
                    drawRect(
                        if (k.toFloat() / n < frac) c else dim,
                        topLeft = Offset(x, (size.height - bh) / 2f),
                        size = Size(bw, bh),
                    )
                }
            }
            Text(
                "${vmClock(played.value)} / ${vmClock(total)}",
                style = TextStyle(fontFamily = Fonts.barlow, fontSize = 12.sp, color = VmMuted),
            )
        }

        // auto transcript, a few low-confidence words greyed out
        val words = line.split(" ")
        val transcript = buildAnnotatedString {
            append("“")
            words.forEachIndexed { k, w ->
                if (k > 0) append(" ")
                val unsure = k > 2 && (abs(w.hashCode()) % 9 == 0) && !w.contains('“') && !w.contains('”')
                if (unsure) withStyle(SpanStyle(color = VmFaint)) { append(w) } else append(w)
            }
            append("”")
        }
        Text(
            transcript,
            style = TextStyle(fontFamily = Fonts.barlow, fontSize = 16.sp, lineHeight = 23.sp, color = VmText),
            maxLines = 8, overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Transcription is automatic and may be inaccurate.",
            style = TextStyle(fontFamily = Fonts.barlow, fontSize = 11.sp, color = VmFaint),
        )
    }
}

@Composable
private fun VmKey(
    digit: String,
    label: String? = null,
    bg: Color = VmKeyBg,
    labelColor: Color = VmText,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(78.dp).height(58.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(29.dp))
            .background(bg)
            .tap { if (enabled) onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(digit, style = TextStyle(fontFamily = Fonts.barlow, fontSize = 24.sp, lineHeight = 24.sp, color = VmText))
        if (label != null) {
            Text(
                label,
                style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W700, fontSize = 10.sp, lineHeight = 11.sp, letterSpacing = 1.sp, color = labelColor),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

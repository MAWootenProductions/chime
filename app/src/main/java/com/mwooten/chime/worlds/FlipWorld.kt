package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap

private val FlipNavy = Color(0xFF1C3A6A)
private val FlipInk = Color(0xFF10141A)
private val FlipKeyDark = Color(0xFF2C3442)
private val FlipKeyText = Color(0xFFC6CFDC)
private val FlipSub = Color(0xFF4A5466)

/** Turn the companion's sentence into a period-correct text message. */
private fun flipTxt(line: String): String {
    var t = " " + line + " "
    val subs = listOf(
        " you " to " u ", " You " to " U ", " your " to " ur ", " Your " to " Ur ",
        " you're " to " ur ", " You're " to " Ur ", " are " to " r ", " to " to " 2 ", " too " to " 2 ",
        " for " to " 4 ", " before " to " b4 ", " today" to " 2day", " Today" to " 2day",
        " tomorrow" to " 2moro", " tonight" to " 2nite", " please" to " pls", " because " to " cuz ",
        " minutes" to " mins", " minute" to " min", " hours" to " hrs", " hour" to " hr",
        " and " to " & ", " with " to " w/ ", " later" to " l8r", " great" to " gr8",
        " see " to " c ", " okay" to " ok", " That's " to " thats ", " that's " to " thats ",
        " It's " to " its ", " it's " to " its ", " I'm " to " im ", " don't " to " dont ",
    )
    for ((from, to) in subs) {
        // repeat so back-to-back matches ("you to you") all convert
        var prev: String
        do { prev = t; t = t.replace(from, to) } while (t != prev)
    }
    t = t.trim()
    return if (t.length > 160) t.take(157) + "..." else t
}

/** "9:44a", the way the little screen squeezes it. */
private fun flipClock(ms: Long): String =
    Time.time(ms).replace(" AM", "a").replace(" PM", "p").replace(" am", "a").replace(" pm", "p")

@Composable
fun FlipWorld(s: WorldState, a: WorldActions) {
    val msgs: List<Quest> = s.open
    var index by rememberSaveable { mutableStateOf(0) }
    val i = if (msgs.isEmpty()) 0 else index.mod(msgs.size)
    val q: Quest? = msgs.getOrNull(i)
    val sms = flipTxt(q?.let { s.companion.line(it, s.now) } ?: s.line)
    val voice = rememberVoice { heard -> a.spill(heard) }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2C3442), Color(0xFF1B2029)))),
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 30.dp, vertical = 14.dp),
        ) {
            // ---- top half: brushed metal lid with the screen
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(
                        Brush.horizontalGradient(
                            0f to Color(0xFF8D99AD), 0.3f to Color(0xFFC6CFDC), 0.55f to Color(0xFF9AA6B9),
                            0.8f to Color(0xFFC0C9D6), 1f to Color(0xFF8693A8),
                        ),
                    )
                    .drawBehind {
                        drawLine(Color(0x80FFFFFF), Offset(40.dp.toPx(), 1f), Offset(size.width - 40.dp.toPx(), 1f), strokeWidth = 2.dp.toPx())
                    }
                    .padding(start = 26.dp, end = 26.dp, top = 22.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // earpiece slot
                Box(Modifier.width(56.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(FlipSub))
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)).background(Color(0xFF0D1016)).padding(12.dp),
                ) {
                    FlipScreen(s, a, q, i, msgs.size, sms, voice)
                }
            }

            // ---- hinge
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.verticalGradient(0f to Color(0xFF5A6578), 0.6f to Color(0xFF2E3644), 1f to Color(0xFF4A5466))),
            )

            // ---- bottom half: keypad
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(
                        Brush.horizontalGradient(
                            0f to Color(0xFF7F8BA0), 0.35f to Color(0xFFB4BFCF), 0.6f to Color(0xFF8D99AD), 1f to Color(0xFFA8B3C4),
                        ),
                    )
                    .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // soft keys + d-pad
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FlipSoftKey("DONE", left = true, visible = q != null) { q?.let(a::done) }
                    Spacer(Modifier.weight(1f))
                    FlipDpad(
                        enabled = msgs.size > 1,
                        onPrev = { index = i - 1 },
                        onNext = { index = i + 1 },
                        onOk = { if (q != null) a.openQuest(q) else a.openLog() },
                    )
                    Spacer(Modifier.weight(1f))
                    FlipSoftKey("LATER", left = false, visible = q != null) { q?.let { a.snooze(it, 60) } }
                }

                // T9 typing in progress = spill
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(FlipInk)
                        .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("New: ", style = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 14.sp, color = Color(0xFF9FD0FF)))
                    SpillInput(
                        onSpill = a::spill,
                        textStyle = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 14.sp, color = Color(0xFF9FD0FF)),
                        placeholder = "type a quest_",
                        placeholderColor = Color(0xFF3F6A96),
                        cursor = Color(0xFF9FD0FF),
                        modifier = Modifier.weight(1f),
                        trailing = { submit ->
                            Text(
                                "T9 Abc",
                                style = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 11.sp, color = Color(0xFF5A8AC0)),
                                modifier = Modifier.padding(start = 6.dp).tap { submit() }.padding(horizontal = 6.dp, vertical = 6.dp),
                            )
                        },
                    )
                }

                // the number keys
                val keys = listOf(
                    "1" to "LOG", "2" to "TALK", "3" to "DEF",
                    "4" to "GHI", "5" to "JKL", "6" to "MNO",
                    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
                )
                for (r in 0 until 3) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0 until 3) {
                            val (d, letters) = keys[r * 3 + c]
                            FlipNumKey(d, letters, Modifier.weight(1f)) {
                                when (d) {
                                    "1" -> { a.openLog() }
                                    "2" -> { voice.toggle() }
                                    "4" -> { if (msgs.size > 1) index = i - 1 }
                                    "6" -> { if (msgs.size > 1) index = i + 1 }
                                    "5" -> { if (q != null) a.openQuest(q) }
                                    else -> {}
                                }
                                Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipScreen(s: WorldState, a: WorldActions, q: Quest?, i: Int, count: Int, sms: String, voice: Voice) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFCFE6FF), Color(0xFFA8C8F0)))),
    ) {
        // status bar
        Row(
            Modifier.fillMaxWidth().background(FlipNavy).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.width(9.dp).height(10.dp)) {
                val bw = 2.dp.toPx(); val gap = 1.dp.toPx()
                val hs = listOf(3f, 5f, 7f, 10f)
                hs.forEachIndexed { k, h ->
                    val hp = h.dp.toPx()
                    drawRect(
                        if (k < 3) Color.White else Color(0x59FFFFFF),
                        topLeft = Offset(k * (bw + gap), size.height - hp), size = Size(bw, hp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(flipClock(s.now), style = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 12.sp, color = Color.White))
            Spacer(Modifier.weight(1f))
            Canvas(Modifier.width(18.dp).height(8.dp)) {
                val sw = 1.dp.toPx()
                drawRect(Color.White, topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw), style = Stroke(sw))
                drawRect(Color.White, topLeft = Offset(sw * 2, sw * 2), size = Size(8.dp.toPx(), size.height - sw * 4))
            }
        }
        // header
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // contact picture: your character as a tiny phone-screen sprite. Tap to talk.
            Box(
                Modifier.size(34.dp).border(1.dp, FlipNavy).background(Color(0xFFDDEBFA)),
                contentAlignment = Alignment.Center,
            ) {
                CharacterArt(
                    30.dp, ArtStyle.PIXEL, who = s.companion, level = voice.level, listening = voice.listening,
                    glow = false, pixels = 22, onTap = { voice.toggle() },
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                if (voice.listening) "Talk now..." else "From: ${s.companion.title}",
                style = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 13.sp, color = FlipInk),
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (count == 0) "0/0" else "${i + 1}/$count",
                style = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 13.sp, color = FlipInk),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(FlipNavy))
        // the message itself
        Box(
            Modifier.weight(1f).fillMaxWidth().background(Color.White)
                .tap { if (q != null) a.openQuest(q) }
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            val body = when {
                voice.listening -> "New msg: " + voice.heard.ifBlank { "..." } + "_"
                else -> sms
            }
            Text(body, style = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 15.sp, lineHeight = 20.sp, color = FlipInk))
        }
        Text(
            when {
                voice.listening -> "tap pic to send"
                voice.note.isNotBlank() -> voice.note
                else -> "${sms.length}/160"
            },
            style = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 11.sp, color = Color(0xFF5A6A80), textAlign = TextAlign.End),
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp),
        )
        // on-screen soft key labels
        Row(
            Modifier.fillMaxWidth().background(FlipNavy).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val st = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 13.sp, color = Color.White)
            Box(Modifier.weight(1f)) {
                if (q != null) Text("Done", style = st, modifier = Modifier.tap { a.done(q) })
            }
            Text("Settings", style = st, modifier = Modifier.tap(a::openOptions))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (q != null) Text("Later", style = st, modifier = Modifier.tap { a.snooze(q, 60) })
                else Text("Log", style = st, modifier = Modifier.tap(a::openLog))
            }
        }
    }
}

@Composable
private fun FlipSoftKey(label: String, left: Boolean, visible: Boolean, onClick: () -> Unit) {
    val shape = if (left) RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp, topEnd = 4.dp, bottomEnd = 4.dp)
    else RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 13.dp, bottomEnd = 13.dp)
    Box(
        Modifier.width(64.dp).height(26.dp).alpha(if (visible) 1f else 0f)
            .clip(shape).background(FlipKeyDark)
            .tap { if (visible) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W600, fontSize = 11.sp, color = FlipKeyText))
    }
}

@Composable
private fun FlipDpad(enabled: Boolean, onPrev: () -> Unit, onNext: () -> Unit, onOk: () -> Unit) {
    Box(
        Modifier.size(74.dp).clip(CircleShape).background(Color(0xFF3E4858))
            .border(1.dp, Color(0x26FFFFFF), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // arrows on the ring
        Canvas(Modifier.fillMaxSize()) {
            val col = if (enabled) FlipKeyText else Color(0x66C6CFDC)
            val cy = size.height / 2
            val a = 4.dp.toPx()
            val inset = 7.dp.toPx()
            val l = Path().apply {
                moveTo(inset, cy); lineTo(inset + a * 1.4f, cy - a); lineTo(inset + a * 1.4f, cy + a); close()
            }
            val r = Path().apply {
                moveTo(size.width - inset, cy); lineTo(size.width - inset - a * 1.4f, cy - a); lineTo(size.width - inset - a * 1.4f, cy + a); close()
            }
            drawPath(l, col); drawPath(r, col)
        }
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight().tap { if (enabled) onPrev() })
            Box(Modifier.width(50.dp).fillMaxHeight())
            Box(Modifier.weight(1f).fillMaxHeight().tap { if (enabled) onNext() })
        }
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(FlipKeyDark).tap(onOk),
            contentAlignment = Alignment.Center,
        ) {
            Text("OK", style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W600, fontSize = 11.sp, color = FlipKeyText))
        }
    }
}

@Composable
private fun FlipNumKey(digit: String, letters: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFE6EBF2), Color(0xFFB9C3D2))))
            .tap(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(digit, style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W600, fontSize = 17.sp, lineHeight = 17.sp, color = FlipInk))
        Text(letters, style = TextStyle(fontFamily = Fonts.barlow, fontWeight = FontWeight.W600, fontSize = 8.sp, lineHeight = 9.sp, color = FlipSub))
    }
}

package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlinx.coroutines.delay
import kotlin.random.Random

private val GroupBg = Color(0xFF17181C)
private val GroupBar = Color(0xFF1E1F24)
private val GroupBorder = Color(0xFF2C2D33)
private val GroupPinBg = Color(0xFF22232A)
private val GroupText = Color(0xFFECECF1)
private val GroupSoft = Color(0xFFC9CAD3)
private val GroupMuted = Color(0xFF8B8D97)
private val GroupChip = Color(0xFF2A2B31)
private val GroupBlue = Color(0xFF3A5BD9)

/** One message in the thread. [who] null means you. [poll] attaches the done/snooze poll. */
private class GroupLine(
    val key: String,
    val who: Companion?,
    val text: String,
    val reactors: List<String> = emptyList(),
    val poll: Quest? = null,
)

@Composable
fun GroupWorld(s: WorldState, a: WorldActions) {
    val everyone = Companion.entries
    val poller = s.companion
    val talkers = everyone.filter { it != poller }
    val focus = s.focus
    val scroll = rememberScrollState()
    val voice = rememberVoice { heard -> a.spill(heard) }
    val heard = voice.heard

    // everything the group wants to say right now, in order
    val items: List<GroupLine> = run {
        val out = ArrayList<GroupLine>()
        val recent = s.quests.filter { s.now - it.created < 36 * 3600_000L }.sortedBy { it.created }.takeLast(2)
        recent.forEach { q ->
            out += GroupLine("you:${q.id}", null, q.text)
            val r = everyone[Math.floorMod(q.id.hashCode(), everyone.size)]
            out += GroupLine("re:${q.id}:${q.done}", r, r.line(q, s.now))
        }
        if (focus != null) {
            // the gentlest voice gets the +1s
            val plusOneOn = talkers.firstOrNull { it == Companion.WREN } ?: talkers.first()
            talkers.forEach { c ->
                val reactors = if (c == plusOneOn) talkers.filter { it != c }.takeLast(2).map { it.title } else emptyList()
                out += GroupLine("f:${focus.id}:${c.name}", c, c.line(focus, s.now), reactors)
            }
            out += GroupLine("p:${focus.id}", poller, s.line, poll = focus)
        } else {
            talkers.forEach { c -> out += GroupLine("e:${c.name}", c, c.emptyLine) }
            out += GroupLine("e:${poller.name}", poller, s.line)
        }
        out
    }
    val byKey = items.associateBy { it.key }
    val latestItems by rememberUpdatedState(items)

    // what has already been shown stays shown; new lines arrive one at a time, in the order they were said
    val revealed = remember { mutableStateListOf<String>() }
    var typing by remember { mutableStateOf<Companion?>(null) }
    val keySig = items.joinToString("|") { it.key }
    LaunchedEffect(keySig) {
        for (item in latestItems) {
            if (item.key in revealed) continue
            if (item.who == null) {
                delay(150)
            } else {
                typing = item.who
                delay(Random.nextLong(800, 2500))
                typing = null
            }
            if (item.key !in revealed) revealed.add(item.key)
            delay(Random.nextLong(120, 380))
        }
        typing = null
    }
    LaunchedEffect(revealed.size, typing, voice.listening, heard, scroll.maxValue) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().background(GroupBg).imePadding()) {
        // header with the five faces stacked
        Row(
            Modifier
                .fillMaxWidth()
                .background(GroupBar)
                .statusBarsPadding()
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = (28 + 18 * (everyone.size - 1)).dp, height = 28.dp)) {
                everyone.forEachIndexed { i, c ->
                    Box(
                        Modifier.offset(x = (18 * i).dp).size(28.dp).clip(CircleShape).background(Color(0xFF2E3038)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CharacterArt(size = 26.dp, style = ArtStyle.FLAT, who = c, glow = false)
                    }
                }
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    if (focus != null) "${groupShort(focus.text)} task force" else "the quest task force",
                    fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 16.sp, color = GroupText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    typing?.let { "${it.title} is typing…" } ?: ("you, " + everyone.joinToString(", ") { it.title }),
                    fontFamily = Fonts.figtree, fontSize = 12.sp, color = if (typing != null) GroupSoft else GroupMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Box(Modifier.size(40.dp).tap { a.openLog() }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(18.dp)) {
                    val k = size.width / 16f
                    for (row in 0..2) {
                        val y = (4 + row * 4) * k
                        drawLine(GroupSoft, Offset(3 * k, y), Offset((if (row == 2) 9 else 13) * k, y), 1.8f * k, cap = StrokeCap.Round)
                    }
                }
            }
            Text(
                "Settings", fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 14.sp, color = GroupText,
                modifier = Modifier.tap { a.openOptions() }.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(GroupBorder))

        // pinned, with quick replies so done/snooze never wait on the animation
        if (focus != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(GroupPinBg)
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(width = 12.dp, height = 14.dp)) {
                    val k = size.width / 12f
                    val p = Path().apply {
                        moveTo(3 * k, 1 * k); lineTo(9 * k, 1 * k); lineTo(8 * k, 6 * k); lineTo(11 * k, 9 * k)
                        lineTo(1 * k, 9 * k); lineTo(4 * k, 6 * k); close()
                        moveTo(6 * k, 9 * k); lineTo(6 * k, 14 * k)
                    }
                    drawPath(p, GroupSoft, style = Stroke(1.4f * k))
                }
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.W800)) { append("Pinned: ") }
                        append(focus.text)
                        append(" · ")
                        append(if (focus.due == null) "someday" else "due " + focus.shortDue())
                    },
                    fontFamily = Fonts.figtree, fontSize = 13.sp, color = GroupSoft,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).tap { a.openQuest(focus) }.padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
                )
                Text(
                    "Done", fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 13.sp, color = GroupText,
                    modifier = Modifier.tap { a.done(focus) }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Text(
                    "Later", fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 13.sp, color = GroupMuted,
                    modifier = Modifier.tap { a.snooze(focus, 30) }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        // the thread
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            revealed.forEach { key ->
                val m = byKey[key] ?: return@forEach
                val who = m.who
                if (who == null) {
                    GroupMine(m.text)
                } else {
                    GroupMessage(who, onAvatar = { voice.toggle() }) {
                        Text(m.text, fontFamily = Fonts.figtree, fontSize = 15.sp, lineHeight = 20.sp, color = GroupText)
                        if (m.reactors.isNotEmpty()) Text(
                            "+1 " + m.reactors.joinToString(", "),
                            fontFamily = Fonts.figtree, fontSize = 12.sp, color = GroupSoft,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(GroupChip, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                        val poll = m.poll
                        if (poll != null) GroupPoll(poll, who.c, a)
                    }
                }
            }
            val t = typing
            if (t != null) GroupTyping(t)
            if (voice.listening) GroupMine(if (heard.isNotBlank()) "$heard…" else "…", draft = true)
            else if (voice.note.isNotBlank()) Text(voice.note, fontFamily = Fonts.figtree, fontSize = 12.sp, color = GroupMuted)
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(GroupBorder))
        Row(
            Modifier
                .fillMaxWidth()
                .background(GroupBar)
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // you, in the group: tap to talk instead of type
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (voice.listening) GroupBlue else GroupChip),
                contentAlignment = Alignment.Center,
            ) {
                CharacterArt(
                    size = 34.dp, style = ArtStyle.FLAT, level = voice.level, listening = voice.listening,
                    glow = false, onTap = { voice.toggle() },
                )
            }
            SpillInput(
                onSpill = a::spill,
                textStyle = TextStyle(fontFamily = Fonts.figtree, fontSize = 15.sp, color = GroupText),
                placeholder = if (voice.listening) "Listening… tap yourself to stop" else "Tell the group what you need to do",
                placeholderColor = GroupMuted,
                cursor = GroupText,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .background(GroupChip, RoundedCornerShape(8.dp))
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                trailing = { submit ->
                    Text(
                        "Send", fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 14.sp, color = GroupBg,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(GroupText, RoundedCornerShape(6.dp))
                            .tap { submit() }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                },
            )
        }
    }
}

private fun groupShort(text: String): String {
    val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return words.take(3).joinToString(" ").ifEmpty { "quest" }
}

@Composable
private fun GroupAvatar(who: Companion, onTap: () -> Unit = {}) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF2E3038)),
        contentAlignment = Alignment.Center,
    ) {
        CharacterArt(size = 30.dp, style = ArtStyle.FLAT, who = who, glow = false, onTap = onTap)
    }
}

@Composable
private fun GroupMessage(who: Companion, onAvatar: () -> Unit, body: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        GroupAvatar(who, onAvatar)
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(who.title, fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 12.sp, color = who.c)
            body()
        }
    }
}

@Composable
private fun GroupMine(text: String, draft: Boolean = false) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text, fontFamily = Fonts.figtree, fontSize = 15.sp, lineHeight = 20.sp,
            fontStyle = if (draft) FontStyle.Italic else FontStyle.Normal,
            color = if (draft) GroupSoft else Color.White,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (draft) Modifier.border(1.dp, GroupBlue, RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                    else Modifier.background(GroupBlue, RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun GroupTyping(who: Companion) {
    val clock = rememberClock()
    Row(verticalAlignment = Alignment.CenterVertically) {
        GroupAvatar(who)
        Row(
            Modifier.padding(start = 8.dp).background(GroupChip, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (i in 0..2) {
                Box(
                    Modifier
                        .graphicsLayer {
                            val p = (((clock.value - i * 0.15f) % 1.2f) + 1.2f) % 1.2f / 1.2f
                            val up = when { p < 0.3f -> p / 0.3f; p < 0.6f -> 1f - (p - 0.3f) / 0.3f; else -> 0f }
                            translationY = -3.dp.toPx() * up
                            alpha = 0.4f + 0.6f * up
                        }
                        .size(6.dp)
                        .background(GroupSoft, CircleShape)
                )
            }
        }
        Text(
            "${who.title} is typing…", fontFamily = Fonts.figtree, fontSize = 12.sp, color = GroupMuted,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun GroupPoll(q: Quest, tint: Color, a: WorldActions) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(Color(0xFF24252C), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF33343C), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "when is “${q.text}” getting done",
            fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 15.sp, color = GroupText,
            maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
        GroupOption("right now", 3, 0.75f, tint.copy(alpha = 0.28f)) { a.done(q) }
        GroupOption("after coffee", 1, 0.25f, tint.copy(alpha = 0.18f)) { a.snooze(q, 30) }
        GroupOption("it can come to me later", 0, 0f, tint.copy(alpha = 0.18f)) { a.snooze(q, 180) }
        Text("You haven't voted", fontFamily = Fonts.figtree, fontSize = 12.sp, color = GroupMuted)
    }
}

@Composable
private fun GroupOption(label: String, votes: Int, frac: Float, fill: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C2D34))
            .drawBehind { if (frac > 0f) drawRect(fill, Offset.Zero, Size(size.width * frac, size.height)) }
            .tap(onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label, fontFamily = Fonts.figtree, fontSize = 15.sp, color = GroupText,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Text(votes.toString(), fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 15.sp, color = GroupText)
    }
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap
import kotlin.math.cos
import kotlin.math.sin

private val GroupBg = Color(0xFF17181C)
private val GroupBar = Color(0xFF1E1F24)
private val GroupBorder = Color(0xFF2C2D33)
private val GroupPinBg = Color(0xFF22232A)
private val GroupText = Color(0xFFECECF1)
private val GroupSoft = Color(0xFFC9CAD3)
private val GroupMuted = Color(0xFF8B8D97)
private val GroupChip = Color(0xFF2A2B31)

@Composable
fun GroupWorld(s: WorldState, a: WorldActions) {
    val everyone = Companion.entries
    val poller = s.companion
    val talkers = everyone.filter { it != poller }
    val focus = s.focus
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue, focus?.id) { scroll.scrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().background(GroupBg).imePadding()) {
        // header with the five avatars stacked
        Row(
            Modifier
                .fillMaxWidth()
                .background(GroupBar)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = (26 + 17 * (everyone.size - 1)).dp, height = 26.dp)) {
                everyone.forEachIndexed { i, c ->
                    Box(
                        Modifier
                            .offset(x = (17 * i).dp)
                            .size(26.dp)
                            .background(GroupBar, CircleShape)
                            .padding(2.dp)
                            .background(c.c, CircleShape)
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    if (focus != null) "${groupShort(focus.text)} task force" else "the quest task force",
                    fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 16.sp, color = GroupText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "you, " + everyone.joinToString(", ") { it.title },
                    fontFamily = Fonts.figtree, fontSize = 12.sp, color = GroupMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            GroupIcon(onClick = { a.openLog() }) { col ->
                val k = size.width / 16f
                for (row in 0..2) {
                    val y = (4 + row * 4) * k
                    drawLine(col, Offset(3 * k, y), Offset((if (row == 2) 9 else 13) * k, y), 1.8f * k, cap = StrokeCap.Round)
                }
            }
            GroupIcon(onClick = { a.openOptions() }) { col ->
                val k = size.width / 16f
                val c = Offset(8 * k, 8 * k)
                for (i in 0 until 8) {
                    val ang = i * Math.PI / 4
                    val dx = cos(ang).toFloat()
                    val dy = sin(ang).toFloat()
                    drawLine(col, Offset(c.x + dx * 4.5f * k, c.y + dy * 4.5f * k), Offset(c.x + dx * 7f * k, c.y + dy * 7f * k), 2f * k, cap = StrokeCap.Round)
                }
                drawCircle(col, 4.5f * k, c, style = Stroke(1.8f * k))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(GroupBorder))

        // pinned
        if (focus != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(GroupPinBg)
                    .tap { a.openQuest(focus) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    modifier = Modifier.padding(start = 8.dp),
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
            // the gentlest voice gets the +1s
            val plusOneOn = talkers.firstOrNull { it.name == "WREN" } ?: talkers.first()
            talkers.forEach { c ->
                val said = if (focus != null) c.line(focus, s.now) else c.emptyLine
                val reactors = if (c == plusOneOn) talkers.filter { it != c }.takeLast(2).map { it.title } else emptyList()
                GroupMessage(c.title, c.c) {
                    Text(said, fontFamily = Fonts.figtree, fontSize = 15.sp, lineHeight = 20.sp, color = GroupText)
                    if (reactors.isNotEmpty()) Text(
                        "+1 " + reactors.joinToString(", "),
                        fontFamily = Fonts.figtree, fontSize = 12.sp, color = GroupSoft,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(GroupChip, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            // your companion runs the poll
            GroupMessage(poller.title, poller.c) {
                if (focus != null) {
                    Text(
                        if (poller == s.companion) s.line else poller.line(focus, s.now),
                        fontFamily = Fonts.figtree, fontSize = 15.sp, lineHeight = 20.sp, color = GroupText,
                    )
                    GroupPoll(focus, poller.c, a)
                } else {
                    Text(s.line, fontFamily = Fonts.figtree, fontSize = 15.sp, lineHeight = 20.sp, color = GroupText)
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(GroupBorder))
        SpillInput(
            onSpill = a::spill,
            textStyle = TextStyle(fontFamily = Fonts.figtree, fontSize = 15.sp, color = GroupText),
            placeholder = "Tell the group what you need to do",
            placeholderColor = GroupMuted,
            cursor = GroupText,
            modifier = Modifier
                .fillMaxWidth()
                .background(GroupBar)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 14.dp)
                .background(GroupChip, RoundedCornerShape(22.dp))
                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            trailing = { submit ->
                Text(
                    "Send", fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 14.sp, color = GroupBg,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(GroupText, RoundedCornerShape(20.dp))
                        .tap { submit() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            },
        )
    }
}

private fun groupShort(text: String): String {
    val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return words.take(3).joinToString(" ").ifEmpty { "quest" }
}

@Composable
private fun GroupIcon(onClick: () -> Unit, draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit) {
    Box(Modifier.size(36.dp).tap(onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(18.dp)) { draw(GroupSoft) }
    }
}

@Composable
private fun GroupMessage(name: String, color: Color, body: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.size(28.dp).background(color, CircleShape))
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(name, fontFamily = Fonts.figtree, fontWeight = FontWeight.W800, fontSize = 12.sp, color = color)
            body()
        }
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

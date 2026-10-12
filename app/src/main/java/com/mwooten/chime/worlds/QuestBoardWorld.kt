package com.mwooten.chime.worlds

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap

private val QbInk = Color(0xFF2A1A0C)
private val QbBlood = Color(0xFF7A1A0C)
private val QbGold = Color(0xFFF2D48A)
private val QbFrame = Color(0xFF2A180A)

/** The torn top edge of the WANTED poster. */
private val QbTorn = GenericShape { size, _ ->
    val pts = listOf(
        0f to 0.02f, 0.08f to 0f, 0.22f to 0.01f, 0.40f to 0f, 0.62f to 0.02f, 0.80f to 0f, 1f to 0.01f,
        0.99f to 0.30f, 1f to 0.62f, 0.98f to 1f, 0f to 1f, 0.01f to 0.60f,
    )
    moveTo(pts[0].first * size.width, pts[0].second * size.height)
    pts.drop(1).forEach { lineTo(it.first * size.width, it.second * size.height) }
    close()
}

/** Guild rank from urgency: overdue or within 2h = S, today = A, this week = B, else C. */
private fun qbRank(q: Quest, now: Long): Char {
    val sh = q.shelf(now)
    val m = q.minutesLeft(now)
    return when {
        sh == Shelf.OVERDUE -> 'S'
        m != null && !q.allDay && m <= 120 -> 'S'
        sh == Shelf.TODAY -> 'A'
        sh == Shelf.TOMORROW || sh == Shelf.WEEK -> 'B'
        else -> 'C'
    }
}

private fun qbXp(r: Char) = when (r) { 'S' -> 120; 'A' -> 80; 'B' -> 40; else -> 15 }

private fun qbDeadline(q: Quest, now: Long): String {
    val d = q.due ?: return "Whenever"
    if (q.shelf(now) == Shelf.OVERDUE) return "Overdue!"
    if (q.allDay) return if (q.shelf(now) == Shelf.TODAY) "By nightfall" else q.shortDue()
    val t = Time.ldt(d)
    if (t.hour == 12 && t.minute == 0 && q.shelf(now) == Shelf.TODAY) return "High noon"
    return (if (q.kind == "by") "Before " else "At ") + q.shortDue()
}

private fun Modifier.qbHardShadow(color: Color, dy: Dp): Modifier = drawBehind {
    drawRect(color, Offset(0f, dy.toPx()), size)
}

@Composable
fun QuestBoardWorld(s: WorldState, a: WorldActions) {
    val f = s.focus
    var posting by remember { mutableStateOf(false) }
    val others = s.open.filter { it.id != f?.id }.take(2)
    val claimed = s.done.take(2)
    val rank = if (f != null) qbRank(f, s.now) else 'S'

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF5A3A22), Color(0xFF3E2614))))
            .drawBehind {
                // wall planks
                val gap = 64.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawRect(Color.Black.copy(alpha = 0.18f), Offset(x, 0f), Size(2.dp.toPx(), size.height))
                    x += gap
                }
            }
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(4.dp))
            // guild sign flanked by two little plaques
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QbPlaque("Log") { a.openLog() }
                Box(
                    Modifier
                        .weight(1f)
                        .qbHardShadow(Color(0xFF1E1006), 6.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFF8A5A2E), Color(0xFF6A4220))))
                        .border(3.dp, QbFrame)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Quests & Bounties", fontFamily = Fonts.pirataOne, fontSize = 28.sp, color = QbGold, maxLines = 1, softWrap = false)
                }
                QbPlaque("Guild") { a.openOptions() }
            }
            Spacer(Modifier.height(18.dp))

            // the cork board
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF4A2C14))
                    .padding(10.dp)
                    .background(Color(0xFFB8875A))
                    .drawBehind {
                        val a1 = 14.dp.toPx()
                        val r = 2.dp.toPx()
                        var y = 0f
                        while (y < size.height) {
                            var x = 0f
                            while (x < size.width) {
                                drawCircle(Color.Black.copy(alpha = 0.08f), r, Offset(x + a1 * 0.2f, y + a1 * 0.3f))
                                x += a1
                            }
                            y += a1
                        }
                        val a2 = 18.dp.toPx()
                        y = 0f
                        while (y < size.height) {
                            var x = 0f
                            while (x < size.width) {
                                drawCircle(Color.White.copy(alpha = 0.06f), r, Offset(x + a2 * 0.7f, y + a2 * 0.6f))
                                x += a2
                            }
                            y += a2
                        }
                        // inner shadow at the frame
                        val e = 18.dp.toPx()
                        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent), 0f, e), Offset.Zero, Size(size.width, e))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)), size.height - e, size.height), Offset(0f, size.height - e), Size(size.width, e))
                        drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent), 0f, e), Offset.Zero, Size(e, size.height))
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)), size.width - e, size.width), Offset(size.width - e, 0f), Size(e, size.height))
                    }
                    .clipToBounds()
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        // the main bounty
                        Box(Modifier.weight(0.63f)) {
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .rotate(-2.5f)
                                    .shadow(8.dp, QbTorn)
                                    .background(Brush.verticalGradient(listOf(Color(0xFFF3E6C4), Color(0xFFE4D0A2))), QbTorn)
                                    .tap { f?.let { a.openQuest(it) } }
                            ) {
                                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 30.dp)) {
                                    Text(
                                        if (f != null) "WANTED" else "NO BOUNTIES", fontFamily = Fonts.pirataOne,
                                        fontSize = if (f != null) 38.sp else 28.sp, lineHeight = 38.sp,
                                        color = QbBlood, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1,
                                    )
                                    Text(
                                        f?.text ?: "The board is bare",
                                        fontFamily = Fonts.alegreya, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 23.sp,
                                        color = QbInk, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    )
                                    Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(2.dp).background(QbInk))
                                    if (f != null) {
                                        val since = Time.dayName(Time.date(f.created)).lowercase()
                                        Text(
                                            "Posted $since. Last seen on your list. Must be ${if (f.kind == "by") "answered and done" else "met in person"}.",
                                            fontFamily = Fonts.alegreya, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 19.sp,
                                            color = QbInk, maxLines = 4, overflow = TextOverflow.Ellipsis,
                                        )
                                        QbPosterRow("DEADLINE", qbDeadline(f, s.now), QbBlood)
                                        QbPosterRow("REWARD", "${qbXp(rank)} XP · peace", QbInk)
                                    } else {
                                        Text(
                                            "Pin a new bounty and the guild will hunt it down.",
                                            fontFamily = Fonts.alegreya, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 19.sp, color = QbInk,
                                        )
                                    }
                                }
                                if (f != null) {
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                            .rotate(-14f)
                                            .size(46.dp)
                                            .border(3.dp, Color(0xFFB0281A), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(rank.toString(), fontFamily = Fonts.pirataOne, fontSize = 22.sp, color = Color(0xFFB0281A))
                                    }
                                }
                            }
                            QbPin(Color(0xFFFF8A7A), Color(0xFFB0281A), 14.dp, Modifier.align(Alignment.TopCenter))
                        }
                        Spacer(Modifier.width(10.dp))
                        // smaller bounties
                        Column(Modifier.weight(0.37f).padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                            others.forEachIndexed { i, q ->
                                val r = qbRank(q, s.now)
                                Box {
                                    Column(
                                        Modifier
                                            .padding(top = 4.dp)
                                            .rotate(if (i == 0) 4f else -3f)
                                            .shadow(6.dp)
                                            .background(if (i == 0) Color(0xFFEFE0BC) else Color(0xFFE9DBB8))
                                            .tap { a.openQuest(q) }
                                            .padding(horizontal = 10.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            q.text, fontFamily = Fonts.alegreya, fontWeight = FontWeight.W700, fontSize = 15.sp, lineHeight = 18.sp,
                                            color = QbInk, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(qbDeadline(q, s.now), fontFamily = Fonts.alegreya, fontWeight = FontWeight.W500, fontSize = 12.sp, color = QbInk, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$r-rank · ${qbXp(r)} XP", fontFamily = Fonts.alegreya, fontWeight = FontWeight.W700, fontSize = 12.sp, color = QbInk, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                                    }
                                    if (i == 0) QbPin(Color(0xFFFFF2A0), Color(0xFFC8A020), 12.dp, Modifier.align(Alignment.TopCenter))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        // bounties already claimed
                        Column(Modifier.weight(0.44f).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            claimed.forEachIndexed { i, q ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .rotate(if (i == 0) 3f else -2f)
                                        .alpha(0.85f)
                                        .shadow(6.dp)
                                        .background(Color(0xFFE2D2AC))
                                        .tap { a.openQuest(q) }
                                        .padding(horizontal = 10.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        q.text, fontFamily = Fonts.alegreya, fontWeight = FontWeight.W700, fontSize = 14.sp, lineHeight = 17.sp,
                                        color = QbInk, textDecoration = TextDecoration.LineThrough, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text("CLAIMED", fontFamily = Fonts.pirataOne, fontSize = 28.sp, color = Color(0xFF2A6A2A), modifier = Modifier.rotate(-16f), maxLines = 1, softWrap = false)
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        // the guildmaster's chalk slate: the companion speaks here
                        QbSlate(s, Modifier.weight(0.56f)) { a.openQuest(it) }
                    }
                }
            }

            // post a bounty: a parchment strip with a quill line
            if (posting || f == null) {
                Spacer(Modifier.height(12.dp))
                SpillInput(
                    onSpill = { a.spill(it); posting = false },
                    textStyle = TextStyle(fontFamily = Fonts.alegreya, fontWeight = FontWeight.W500, fontSize = 17.sp, color = QbInk),
                    placeholder = "Describe the bounty…",
                    placeholderColor = QbInk.copy(alpha = 0.45f),
                    cursor = QbBlood,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3E6C4))
                        .border(3.dp, QbFrame)
                        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    trailing = { submit ->
                        Text(
                            "Pin it", fontFamily = Fonts.pirataOne, fontSize = 20.sp, color = QbGold,
                            modifier = Modifier.padding(start = 8.dp).background(QbBlood).tap(submit).padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    },
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (f != null) {
                    QbButton("Take the $rank-rank", QbBlood, QbGold, Modifier.weight(1.3f)) { a.done(f) }
                }
                QbButton(if (posting && f != null) "Never mind" else "Post a bounty", Color(0xFFE4D0A2), QbInk, Modifier.weight(1f)) {
                    posting = !posting
                }
            }
        }
    }
}

@Composable
private fun QbSlate(s: WorldState, modifier: Modifier, onOpen: (Quest) -> Unit) {
    val inf = rememberInfiniteTransition(label = "flame")
    val sx by inf.animateFloat(1f, 0.92f, infiniteRepeatable(tween(200), RepeatMode.Reverse), label = "fx")
    val sy by inf.animateFloat(1f, 1.1f, infiniteRepeatable(tween(200), RepeatMode.Reverse), label = "fy")
    Column(
        modifier
            .rotate(-1.5f)
            .background(Color(0xFF8A5A2E))
            .padding(6.dp)
            .background(Color(0xFF2A2E2A))
            .tap { s.focus?.let { onOpen(it) } }
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Box(
                Modifier
                    .size(20.dp, 24.dp)
                    .graphicsLayer { scaleX = sx; scaleY = sy }
                    .background(
                        Brush.verticalGradient(listOf(s.companion.cLight, s.companion.c, s.companion.cDark)),
                        RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 45, bottomStartPercent = 45),
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "GUILDMASTER ${s.companion.title.uppercase()}", fontFamily = Fonts.kalam, fontWeight = FontWeight.W700,
                fontSize = 13.sp, color = lerp(s.companion.c, Color.White, 0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            s.line, fontFamily = Fonts.kalam, fontWeight = FontWeight.W700, fontSize = 16.sp, lineHeight = 20.sp,
            color = Color(0xFFF0EFE6), maxLines = 8, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QbPosterRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = Fonts.alegreya, fontWeight = FontWeight.W700, fontSize = 14.sp, color = QbInk, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(
            value, fontFamily = Fonts.alegreya, fontWeight = FontWeight.W700, fontSize = 14.sp, color = valueColor,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun QbPin(hi: Color, lo: Color, size: Dp, modifier: Modifier) {
    Box(
        modifier
            .size(size)
            .drawBehind {
                drawCircle(Color.Black.copy(alpha = 0.4f), this.size.minDimension / 2, center.copy(x = center.x + 1.dp.toPx(), y = center.y + 3.dp.toPx()))
                drawCircle(Brush.radialGradient(listOf(hi, lo), center = Offset(this.size.width * 0.35f, this.size.height * 0.3f), radius = this.size.width * 0.7f))
            }
    )
}

@Composable
private fun QbPlaque(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .qbHardShadow(Color(0xFF1E1006), 4.dp)
            .background(Color(0xFF6A4220))
            .border(2.dp, QbFrame)
            .tap(onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = Fonts.pirataOne, fontSize = 16.sp, color = QbGold, maxLines = 1)
    }
}

@Composable
private fun QbButton(label: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .qbHardShadow(QbFrame, 5.dp)
            .background(bg)
            .border(3.dp, QbFrame)
            .tap(onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = Fonts.pirataOne, fontSize = 22.sp, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

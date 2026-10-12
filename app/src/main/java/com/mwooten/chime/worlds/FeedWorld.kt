package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap

private val FeedBg = Color(0xFFEEF0F3)
private val FeedRed = Color(0xFFE2533A)
private val FeedRedTrack = Color(0xFFF2C9BF)
private val FeedInk = Color(0xFF15171C)
private val FeedMuted = Color(0xFF6A7180)
private val FeedDash = Color(0xFF9AA1AD)
private val FeedLine = Color(0xFFDFE2E7)
private val FeedAction = Color(0xFF4A5160)
private val FeedMemoryBg = Color(0xFFF6EFE2)
private val FeedMemoryInk = Color(0xFF8A5A1A)

private val FeedStoryColors = listOf(
    Color(0xFFF6EFE2) to Color(0xFF8A5A1A),
    Color(0xFFE3EEF7) to Color(0xFF1F5A8A),
    Color(0xFFE6F2E3) to Color(0xFF2A6A3A),
)

@Composable
fun FeedWorld(s: WorldState, a: WorldActions) {
    var composing by rememberSaveable { mutableStateOf(false) }
    val focus = s.focus
    val dueToday = s.today.size

    Column(Modifier.fillMaxSize().background(FeedBg).imePadding()) {
        // header
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "chime", fontFamily = Fonts.onest, fontWeight = FontWeight.W800, fontSize = 26.sp,
                letterSpacing = (-1).sp, color = FeedRed, modifier = Modifier.weight(1f),
            )
            Text(
                if (dueToday == 0) "nothing due today" else "$dueToday due today",
                fontFamily = Fonts.onest, fontWeight = FontWeight.W700, fontSize = 12.sp,
                color = if (dueToday == 0) FeedMuted else Color.White,
                modifier = Modifier
                    .background(if (dueToday == 0) FeedBg else FeedRed, RoundedCornerShape(12.dp))
                    .tap { a.openLog() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            // stories row = today's quests, ringed until done
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FeedStory("Spill", FeedMuted, onClick = { composing = !composing }) {
                    Canvas(Modifier.size(58.dp)) {
                        val w = 2.dp.toPx()
                        drawCircle(
                            FeedDash, size.width / 2f - w / 2f, center,
                            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                        )
                        val k = 18.dp.toPx() / 18f
                        val o = Offset(center.x - 9 * k, center.y - 9 * k)
                        drawLine(FeedMuted, Offset(o.x + 9 * k, o.y + 3 * k), Offset(o.x + 9 * k, o.y + 15 * k), 2 * k, cap = StrokeCap.Round)
                        drawLine(FeedMuted, Offset(o.x + 3 * k, o.y + 9 * k), Offset(o.x + 15 * k, o.y + 9 * k), 2 * k, cap = StrokeCap.Round)
                    }
                }
                val stories = (s.today + s.week).take(12)
                stories.forEachIndexed { i, q ->
                    val isFocus = q.id == focus?.id
                    val timed = q.due != null && !q.allDay
                    val (fill, ink) = if (timed) FeedStoryColors[i % FeedStoryColors.size] else (FeedBg to FeedMuted)
                    val progress = if (isFocus && timed) feedProgress(q, s.now) else 1f
                    val ring = if (timed) FeedRed else Color(0xFFC9CED6)
                    FeedStory(feedShortName(q.text), if (timed) FeedInk else FeedMuted, onClick = { a.openQuest(q) }) {
                        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(58.dp)) {
                                val w = 2.dp.toPx()
                                val r = size.width / 2f - w / 2f
                                drawCircle(if (progress < 1f) FeedRedTrack else ring, r, center, style = Stroke(w))
                                if (progress < 1f) drawArc(
                                    ring, -90f, 360f * progress, false,
                                    topLeft = Offset(w / 2f, w / 2f), size = Size(size.width - w, size.height - w),
                                    style = Stroke(w),
                                )
                                drawCircle(fill, size.width / 2f - 4.dp.toPx(), center)
                            }
                            Text(
                                feedStoryTime(q, s.now), fontFamily = Fonts.onest, fontWeight = FontWeight.W800,
                                fontSize = 12.sp, color = ink, maxLines = 1,
                            )
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(FeedLine))

            if (composing || focus == null) {
                FeedCard(Modifier.padding(top = 10.dp)) {
                    SpillInput(
                        onSpill = { t -> a.spill(t); composing = false },
                        textStyle = TextStyle(fontFamily = Fonts.onest, fontSize = 16.sp, color = FeedInk),
                        placeholder = "What's on your mind?",
                        placeholderColor = FeedMuted,
                        cursor = FeedRed,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        trailing = { submit ->
                            Text(
                                "Post", fontFamily = Fonts.onest, fontWeight = FontWeight.W700, fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(FeedRed, RoundedCornerShape(14.dp))
                                    .tap { submit() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                        },
                    )
                }
            }

            // the deadline as a "birthday" card
            if (focus != null) {
                FeedCard(Modifier.padding(top = 10.dp).tap { a.openQuest(focus) }) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(44.dp).background(Color(0xFFFBE6E1), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { FeedGift() }
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.W700)) { append(focus.text) }
                                append(feedBirthday(focus, s.now))
                            },
                            fontFamily = Fonts.onest, fontSize = 14.sp, lineHeight = 19.sp, color = FeedInk,
                            maxLines = 4, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                        )
                    }
                }
            }

            // the companion's post about you
            val others = Companion.entries.filter { it != s.companion }
            FeedCard(Modifier.padding(top = 10.dp)) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FeedAvatar(s.companion.cLight, s.companion.c, s.companion.cDark, 40)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.W700)) { append(s.companion.title) }
                                    withStyle(SpanStyle(color = FeedMuted)) { append(" is feeling " + s.companion.trait.lowercase()) }
                                },
                                fontFamily = Fonts.onest, fontSize = 15.sp, color = FeedInk,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                (focus?.let { feedAgo(s.now - it.created) } ?: "Just now") + " · Only you",
                                fontFamily = Fonts.onest, fontSize = 12.sp, color = FeedMuted,
                            )
                        }
                    }
                    Text(
                        s.line, fontFamily = Fonts.onest, fontSize = 16.sp, lineHeight = 22.sp, color = FeedInk,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    val memory = s.done.lastOrNull()
                    if (memory != null) {
                        val doneAt = memory.doneAt ?: memory.created
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .background(FeedMemoryBg, RoundedCornerShape(8.dp))
                                .tap { a.openLog() }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "A MEMORY · " + feedAgoLong(s.now - doneAt).uppercase() + " AGO",
                                fontFamily = Fonts.onest, fontWeight = FontWeight.W700, fontSize = 11.sp,
                                letterSpacing = 1.sp, color = FeedMemoryInk,
                            )
                            Text(
                                feedMemory(memory, doneAt), fontFamily = Fonts.onest, fontSize = 14.sp,
                                lineHeight = 19.sp, color = FeedInk,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(
                            "${others[0].title} and ${others[1].title} cheered", fontFamily = Fonts.onest,
                            fontSize = 12.sp, color = FeedMuted, modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${s.done.size} done", fontFamily = Fonts.onest, fontSize = 12.sp, color = FeedMuted,
                            modifier = Modifier.tap { a.openLog() },
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE6E8EC)))
                Row(Modifier.fillMaxWidth()) {
                    if (focus != null) {
                        FeedButton("Done it", FeedRed) { a.done(focus) }
                        FeedButton("Snooze", FeedAction) { a.snooze(focus, 30) }
                        FeedButton("Comment", FeedAction) { a.openQuest(focus) }
                    } else {
                        FeedButton("Spill something", FeedRed) { composing = true }
                    }
                }
            }

            // more posts: the others weigh in on what's next
            val rest = s.open.filter { it.id != focus?.id }.take(4)
            rest.forEachIndexed { i, q ->
                val who = others[i % others.size]
                FeedCard(Modifier.padding(top = 10.dp).tap { a.openQuest(q) }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        FeedAvatar(who.cLight, who.c, who.cDark, 32)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.W700)) { append(who.title) }
                                    withStyle(SpanStyle(color = FeedMuted)) { append(" · " + q.shortDue()) }
                                },
                                fontFamily = Fonts.onest, fontSize = 13.sp, color = FeedInk,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                who.line(q, s.now), fontFamily = Fonts.onest, fontSize = 14.sp, lineHeight = 19.sp,
                                color = FeedInk, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
            Box(Modifier.height(12.dp))
        }

        // bottom tabs
        Box(Modifier.fillMaxWidth().height(1.dp).background(FeedLine))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(vertical = 4.dp),
        ) {
            FeedTab("Feed", true) { }
            FeedTab("Calendar", false) { a.openLog() }
            FeedTab("Memories", false) { a.openLog() }
            FeedTab("You", false) { a.openOptions() }
        }
    }
}

@Composable
private fun FeedCard(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .then(modifier)
            .background(Color.White, RoundedCornerShape(12.dp))
    ) { content() }
}

@Composable
private fun FeedStory(label: String, labelColor: Color, onClick: () -> Unit, ring: @Composable () -> Unit) {
    Column(
        Modifier.width(62.dp).tap(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ring()
        Text(
            label, fontFamily = Fonts.onest, fontSize = 11.sp, color = labelColor,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeedAvatar(light: Color, mid: Color, dark: Color, sizeDp: Int) {
    Canvas(Modifier.size(sizeDp.dp)) {
        drawCircle(
            Brush.radialGradient(
                0f to light, 0.6f to mid, 1f to dark,
                center = Offset(size.width * 0.4f, size.height * 0.35f),
                radius = size.width * 0.75f,
            ),
            radius = size.width / 2f,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.FeedButton(label: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.weight(1f).tap(onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(label, fontFamily = Fonts.onest, fontWeight = FontWeight.W700, fontSize = 14.sp, color = color)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.FeedTab(label: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.weight(1f).tap(onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            label, fontFamily = Fonts.onest, fontWeight = FontWeight.W700, fontSize = 12.sp,
            color = if (active) FeedRed else FeedMuted,
        )
    }
}

@Composable
private fun FeedGift() {
    Canvas(Modifier.size(width = 24.dp, height = 22.dp)) {
        val k = size.width / 24f
        drawRoundRect(FeedRed, Offset(2 * k, 9 * k), Size(20 * k, 12 * k), CornerRadius(1 * k, 1 * k))
        drawRect(Color(0xFFF07A62), Offset(1 * k, 6 * k), Size(22 * k, 4 * k))
        drawLine(Color.White, Offset(12 * k, 6 * k), Offset(12 * k, 21 * k), 2 * k)
        val bow = Path().apply {
            moveTo(12 * k, 6 * k)
            cubicTo(8 * k, 0f, 4 * k, 4 * k, 12 * k, 6 * k)
            cubicTo(20 * k, 4 * k, 16 * k, 0f, 12 * k, 6 * k)
        }
        drawPath(bow, FeedRed, style = Stroke(1.8f * k))
    }
}

private fun feedShortName(text: String): String {
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val two = words.take(2).joinToString(" ")
    return two.replaceFirstChar { it.uppercaseChar() }.ifEmpty { "Quest" }
}

private fun feedStoryTime(q: Quest, now: Long): String {
    val d = q.due ?: return "—"
    if (q.shelf(now) == Shelf.OVERDUE) return "late"
    if (q.allDay) return if (Time.date(d) == Time.date(now)) "day" else Time.dayName(Time.date(d)).take(3)
    if (Time.date(d) != Time.date(now)) return Time.dayName(Time.date(d)).take(3)
    return Time.time(d).substringBefore(" ")
}

/** How much of the time between spilling and the deadline has been used (the ring fills as it runs out). */
private fun feedProgress(q: Quest, now: Long): Float {
    val d = q.due ?: return 1f
    val span = (d - q.created).coerceAtLeast(1L)
    return ((now - q.created).toFloat() / span).coerceIn(0.02f, 1f)
}

private fun feedBirthday(q: Quest, now: Long): String {
    val d = q.due ?: return " has been waiting since ${Time.dayName(Time.date(q.created)).lowercase()}. Maybe today's its day."
    val sameDay = Time.date(d) == Time.date(now)
    if (q.shelf(now) == Shelf.OVERDUE) return "'s deadline has passed. Say something now."
    if (q.allDay) return if (sameDay) "'s deadline is today. Say something before the day is out."
        else "'s deadline is ${Time.dayName(Time.date(d))}."
    return if (sameDay) "'s deadline is today. Say something before ${Time.time(d)}."
        else "'s deadline is ${Time.dayName(Time.date(d))} at ${Time.time(d)}."
}

private fun feedMemory(q: Quest, doneAt: Long): String {
    val d = q.due
    if (d != null && !q.allDay && doneAt < d) {
        val early = d - doneAt
        val hours = early / 3_600_000L
        val by = if (hours >= 24) "${hours / 24} day" + (if (hours / 24 == 1L) "" else "s") else if (hours >= 1) "$hours hour" + (if (hours == 1L) "" else "s") else "a few minutes"
        return "You spilled “${q.text}” and finished it $by early. Who was that person?"
    }
    return "You spilled “${q.text}” and actually finished it. Who was that person?"
}

private fun feedAgo(ms: Long): String {
    val m = (ms / 60_000L).coerceAtLeast(0L)
    return when {
        m < 1 -> "Just now"
        m < 60 -> "$m min"
        m < 60 * 24 -> "${m / 60} h"
        else -> "${m / (60 * 24)} d"
    }
}

private fun feedAgoLong(ms: Long): String {
    val m = (ms / 60_000L).coerceAtLeast(0L)
    return when {
        m < 60 -> "$m min"
        m < 60 * 24 -> "${m / 60} hour" + if (m / 60 == 1L) "" else "s"
        m < 60 * 24 * 365 -> "${m / (60 * 24)} day" + if (m / (60 * 24) == 1L) "" else "s"
        else -> "${m / (60 * 24 * 365)} year" + if (m / (60 * 24 * 365) == 1L) "" else "s"
    }
}

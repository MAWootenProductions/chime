package com.mwooten.chime.worlds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import com.mwooten.chime.ui.tap
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PlainPaper = Color(0xFFF4F2ED)
private val PlainInk = Color(0xFF22201B)
private val PlainMuted = Color(0xFF8A8478)
private val PlainHour = Color(0xFFA39D90)
private val PlainRule = Color(0xFFDCD7CC)
private val PlainCardLine = Color(0xFFCFC9BC)
private val PlainRed = Color(0xFFC2452D)
private val PlainQuote = Color(0xFF4A463E)

private class PlainSlot(val label: String, val quests: List<Quest>, val nowFrac: Float?)

@Composable
fun PlainWorld(s: WorldState, a: WorldActions) {
    val today = Time.date(s.now)
    val nowLdt = Time.ldt(s.now)
    val dayName = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()).format(today)
    val slots = plainSlots(s)
    val voice = rememberVoice { heard -> a.spill(heard) }

    Column(
        Modifier
            .fillMaxSize()
            .background(PlainPaper)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        // the date at the top of the planner page
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                buildAnnotatedString {
                    append(dayName)
                    append(" ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = PlainMuted)) {
                        append("the " + plainOrdinal(nowLdt.dayOfMonth))
                    }
                },
                fontFamily = Fonts.newsreader, fontSize = 30.sp, color = PlainInk,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${s.today.size} TO DO",
                fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600, fontSize = 12.sp,
                letterSpacing = 1.sp, color = PlainMuted,
                modifier = Modifier.padding(bottom = 6.dp).tap { a.openLog() },
            )
        }

        // ruled hours down the left, the way a paper diary does it
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            slots.forEachIndexed { i, slot ->
                PlainRow(slot, s, a, last = i == slots.lastIndex)
            }
        }

        // the companion, perched on the margin
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val heard = voice.heard
                val line = when {
                    voice.listening && heard.isNotBlank() -> heard + "…"
                    voice.listening -> "Listening…"
                    voice.note.isNotBlank() -> voice.note
                    else -> s.line
                }
                val said = if (line.startsWith("“")) line else "“$line”"
                Text(
                    said,
                    fontFamily = Fonts.newsreader, fontStyle = FontStyle.Italic, fontSize = 18.sp,
                    lineHeight = 25.sp, color = PlainQuote, maxLines = 5, overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val small = Modifier.padding(vertical = 4.dp)
                    Text(
                        "— ${s.companion.title.uppercase()}",
                        fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600, fontSize = 12.sp,
                        letterSpacing = 1.sp, color = PlainMuted, modifier = small,
                    )
                    val f = s.focus
                    if (f != null) {
                        Text(
                            "   DONE", fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600,
                            fontSize = 12.sp, letterSpacing = 1.sp, color = PlainInk,
                            modifier = small.tap { a.done(f) },
                        )
                        Text(
                            "   LATER", fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600,
                            fontSize = 12.sp, letterSpacing = 1.sp, color = PlainMuted,
                            modifier = small.tap { a.snooze(f, 30) },
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            // your character, drawn into the margin like a doodle. Tap it to talk.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CharacterArt(
                    size = 60.dp, style = ArtStyle.FLAT, level = voice.level, listening = voice.listening,
                    glow = false, onTap = { voice.toggle() },
                )
                Text(
                    if (voice.listening) "listening" else "tap to talk",
                    fontFamily = Fonts.newsreader, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = PlainHour,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // spill: just a line to write on
        SpillInput(
            onSpill = a::spill,
            textStyle = TextStyle(fontFamily = Fonts.newsreader, fontSize = 22.sp, color = PlainInk),
            placeholder = "Something on your mind…",
            placeholderColor = PlainHour,
            cursor = PlainInk,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .drawBehind {
                    val y = size.height - 0.75.dp.toPx()
                    drawLine(PlainInk, Offset(0f, y), Offset(size.width - 52.dp.toPx(), y), 1.5.dp.toPx())
                }
                .padding(vertical = 6.dp),
            trailing = { submit ->
                Box(Modifier.width(52.dp).tap { submit() }, contentAlignment = Alignment.CenterEnd) {
                    Text(
                        "Add", fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600,
                        fontSize = 14.sp, color = PlainInk, modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            },
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            PlainNav("Week", PlainInk) { a.openLog() }
            PlainNav("Someday", PlainMuted) { a.openLog() }
            Spacer(Modifier.weight(1f))
            PlainNav("Settings", PlainMuted) { a.openOptions() }
        }
    }
}

@Composable
private fun PlainNav(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label, fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600, fontSize = 14.sp,
        color = color, modifier = Modifier.tap(onClick).padding(vertical = 4.dp),
    )
}

@Composable
private fun PlainRow(slot: PlainSlot, s: WorldState, a: WorldActions, last: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .drawBehind {
                val line = 1.dp.toPx()
                drawRect(PlainRule, Offset.Zero, Size(size.width, line))
                if (last) drawRect(PlainRule, Offset(0f, size.height - line), Size(size.width, line))
                val frac = slot.nowFrac
                if (frac != null) {
                    val y = size.height * frac
                    val x0 = 46.dp.toPx()
                    drawRect(PlainRed, Offset(x0, y - 1.dp.toPx()), Size(size.width - x0, 2.dp.toPx()))
                    drawCircle(PlainRed, 5.dp.toPx(), Offset(45.dp.toPx(), y))
                }
            }
            .padding(vertical = if (slot.quests.isEmpty()) 0.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            slot.label, fontFamily = Fonts.instrumentSans, fontSize = if (slot.label.length > 3) 11.sp else 12.sp,
            color = PlainHour, maxLines = 1, modifier = Modifier.width(46.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slot.quests.forEach { q -> PlainCard(q, q.id == s.focus?.id, s, a) }
        }
    }
}

@Composable
private fun PlainCard(q: Quest, isFocus: Boolean, s: WorldState, a: WorldActions) {
    val late = q.shelf(s.now) == Shelf.OVERDUE
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (isFocus) Modifier.background(Color.White) else Modifier)
            .border(1.dp, if (isFocus) PlainInk else PlainCardLine)
            .tap { a.openQuest(q) }
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).tap { a.done(q) }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(14.dp).border(1.5.dp, if (isFocus) PlainInk else PlainMuted))
        }
        Text(
            q.text, fontFamily = Fonts.instrumentSans,
            fontWeight = if (isFocus) FontWeight.W600 else FontWeight.W500,
            fontSize = 15.sp, color = PlainInk, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp),
        )
        val due = plainDue(q, late)
        if (due.isNotEmpty()) Text(
            due, fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600, fontSize = 12.sp,
            color = if (isFocus || late) PlainRed else PlainMuted, maxLines = 1,
        )
    }
}

private fun plainDue(q: Quest, late: Boolean): String {
    val d = q.due ?: return ""
    if (late && Time.date(d) != Time.date(System.currentTimeMillis())) return "late"
    if (q.allDay) return if (late) "late" else ""
    val t = Time.time(d)
    return if (q.kind == "by") "by $t" else t
}

private fun plainOrdinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$n$suffix"
}

private fun plainHourLabel(h: Int): String = if (h % 12 == 0) "12" else (h % 12).toString()

/** Builds the planner rows: a late row (if needed), one row per hour around now, then the all-day row. */
private fun plainSlots(s: WorldState): List<PlainSlot> {
    val today = Time.date(s.now)
    val nowLdt = Time.ldt(s.now)
    val nowHour = nowLdt.hour
    val timed = s.today.filter { q -> q.due != null && !q.allDay && Time.date(q.due) == today }
    val allDay = s.today.filter { q -> q.due != null && q.allDay && Time.date(q.due) == today }
    val late = s.today.filter { q -> !timed.contains(q) && !allDay.contains(q) }
    val hours = timed.map { q -> Time.ldt(q.due!!).hour }
    var start = minOf(nowHour, hours.minOrNull() ?: nowHour).coerceAtMost(17)
    if (start == nowHour && start > 0 && hours.none { it < nowHour }) start = (nowHour - 1).coerceAtLeast(0).coerceAtMost(17)
    val end = maxOf(start + 6, hours.maxOrNull() ?: 0).coerceAtMost(23)
    val out = ArrayList<PlainSlot>()
    if (late.isNotEmpty()) out += PlainSlot("late", late, null)
    for (h in start..end) {
        val qs = timed.filter { q -> Time.ldt(q.due!!).hour == h }
        val frac = if (h == nowHour) nowLdt.minute / 60f else null
        out += PlainSlot(plainHourLabel(h), qs, frac)
    }
    out += PlainSlot("all day", allDay, null)
    return out
}

package com.mwooten.chime.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Settings
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.label
import com.mwooten.chime.data.questOrder
import com.mwooten.chime.data.shelf
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

/**
 * "Your list", on the same stage as the opening screen: the companion up top, then your tasks
 * grouped Today / Coming up / Someday / Done. Ticking one fills the circle with your color,
 * draws the check, strikes the text through, and the companion chimes.
 */
@Composable
fun LogScreen(
    quests: List<Quest>,
    view: String,
    onView: (String) -> Unit,
    onBack: () -> Unit,
    onSpill: () -> Unit,
    onToggle: (Quest) -> Unit,
    onOpen: (Quest) -> Unit,
) {
    val ctx = LocalContext.current
    var shelf by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var pickStr by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val month = YearMonth.parse(monthStr); val pick = LocalDate.parse(pickStr)
    val accent = Color(Settings.finish(ctx).c)
    val who = Settings.companion(ctx)
    val openN = quests.count { !it.done }; val doneN = quests.count { it.done }
    // ticking a task goes through onToggle, which strikes the bell; the companion up top hears it and chimes
    val toggle: (Quest) -> Unit = { onToggle(it) }

    StagePage(
        "Your list", onBack,
        subtitle = "$openN open · $doneN done",
        characterSize = 120.dp,
        actions = {
            val searchTint by animateColorAsState(if (searching) Stage.ink else Stage.mute, tween(200), label = "searchTint")
            Box(Modifier.size(44.dp).clip(CircleShape).background(if (searching) Stage.card else Stage.bg)
                .clickable { searching = !searching; if (!searching) query = "" }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(18.dp)) {
                    val s = size.width
                    drawCircle(searchTint, s * .32f, Offset(s * .42f, s * .42f), style = Stroke(1.8.dp.toPx()))
                    drawLine(searchTint, Offset(s * .66f, s * .66f), Offset(s * .95f, s * .95f), 1.8.dp.toPx(), StrokeCap.Round)
                }
            }
            Text("+ Add", style = Stage.ui.copy(fontWeight = FontWeight.W600, color = Stage.ink),
                modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onSpill).padding(horizontal = 12.dp, vertical = 10.dp))
        },
        belowTitle = { ViewSwitch(view) { onView(it); shelf = null } },
    ) {
        var n = 1
        if (searching) item(key = "search") { Rise(1) { StageField(query, { query = it }, "Search your list", cursor = accent) } }

        if (query.isNotBlank()) {
            val hits = quests.filter { it.text.contains(query, true) }.sortedWith(questOrder)
            group("Matches for “$query”", hits, "Nothing on your list matches that.", n, accent, toggle, onOpen)
        } else when (view) {
            "calendar" -> {
                item(key = "calendar") {
                    Rise(1) { Calendar(month, pick, quests, accent, onMonth = { monthStr = it.toString() }, onPick = { pickStr = it.toString() }) }
                }
                n = group(Time.dayName(pick), quests.filter { it.due != null && Time.date(it.due) == pick }.sortedWith(questOrder),
                    "Nothing planned this day.", 2, accent, toggle, onOpen)
                val someday = quests.filter { it.due == null && !it.done }
                if (someday.isNotEmpty()) group("Someday (no date)", someday.sortedWith(questOrder), null, n, accent, toggle, onOpen)
            }
            "shelves" -> {
                val sh = shelf
                if (sh != null) {
                    val s = Shelf.valueOf(sh)
                    item(key = "allShelves") {
                        Rise(1) {
                            Text("‹  All shelves", style = Stage.ui.copy(color = Stage.mute),
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { shelf = null }.padding(horizontal = 6.dp, vertical = 8.dp))
                        }
                    }
                    val list = quests.filter { it.shelf() == s }.let { l -> if (s == Shelf.DONE) l.sortedByDescending { it.doneAt ?: 0 } else l.sortedWith(questOrder) }
                    group(s.label, list, "Nothing on this shelf.", 2, accent, toggle, onOpen)
                } else {
                    Shelf.entries.chunked(2).forEachIndexed { r, pair ->
                        item(key = "shelves_$r") {
                            Rise(1 + r) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { s ->
                                        val count = quests.count { it.shelf() == s }
                                        ShelfTile(s, count, Modifier.weight(1f)) { shelf = s.name }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val open = quests.filter { !it.done }
                if (quests.isEmpty()) {
                    item(key = "empty") { Rise(1) { EmptyCard(who.emptyLine) } }
                }
                val today = open.filter { it.shelf() == Shelf.OVERDUE || it.shelf() == Shelf.TODAY }.sortedWith(questOrder)
                val coming = open.filter { it.shelf() == Shelf.TOMORROW || it.shelf() == Shelf.WEEK || it.shelf() == Shelf.LATER }.sortedWith(questOrder)
                val someday = open.filter { it.shelf() == Shelf.SOMEDAY }.sortedWith(questOrder)
                val done = quests.filter { it.done }.sortedByDescending { it.doneAt ?: 0 }
                if (open.isEmpty() && done.isNotEmpty()) item(key = "allDone") { Rise(1) { EmptyCard("All clear. ${who.emptyLine}") } }
                if (today.isNotEmpty()) n = group("Today", today, null, n, accent, toggle, onOpen)
                if (coming.isNotEmpty()) n = group("Coming up", coming, null, n, accent, toggle, onOpen)
                if (someday.isNotEmpty()) n = group("Someday", someday, null, n, accent, toggle, onOpen)
                if (done.isNotEmpty()) group("Done", done, null, n, accent, toggle, onOpen)
            }
        }
    }
}

/** A titled group of task rows. Returns the next stagger index. */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.group(
    title: String, list: List<Quest>, empty: String?, start: Int, accent: Color,
    onToggle: (Quest) -> Unit, onOpen: (Quest) -> Unit,
): Int {
    var i = start
    val hi = i++
    item(key = "h_$title") {
        Rise(hi, Modifier.animateItem()) { StageLabel(title, trailing = if (list.isNotEmpty()) "${list.size}" else null) }
    }
    if (list.isEmpty() && empty != null) {
        val ei = i++
        item(key = "e_$title") { Rise(ei, Modifier.animateItem()) { EmptyCard(empty) } }
    }
    list.forEach { q ->
        val qi = i++
        item(key = q.id) {
            Rise(qi, Modifier.animateItem()) { TaskRow(q, accent, { onToggle(q) }, { onOpen(q) }) }
        }
    }
    return i
}

@Composable
private fun TaskRow(q: Quest, accent: Color, onToggle: () -> Unit, onOpen: () -> Unit) {
    val fill by animateFloatAsState(if (q.done) 1f else 0f, tween(260, easing = FastOutSlowInEasing), label = "tickFill")
    val check by animateFloatAsState(if (q.done) 1f else 0f, tween(if (q.done) 300 else 120, delayMillis = if (q.done) 140 else 0, easing = FastOutSlowInEasing), label = "tickCheck")
    val strike by animateFloatAsState(if (q.done) 1f else 0f, tween(if (q.done) 420 else 160, delayMillis = if (q.done) 120 else 0, easing = FastOutSlowInEasing), label = "strike")
    val textColor by animateColorAsState(if (q.done) Stage.mute else Stage.ink, tween(420), label = "taskText")
    val overdue = !q.done && q.shelf() == Shelf.OVERDUE
    val meta = when {
        q.done -> q.doneAt?.let { "Done " + Time.dayName(Time.date(it)).lowercase() } ?: "Done"
        else -> q.label()
    }
    StageCard(padding = PaddingValues(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(26.dp)) {
                    val r = size.minDimension / 2f
                    val sw = 1.5.dp.toPx()
                    drawCircle(lerp(Stage.mute, accent, fill), r - sw / 2f, center, style = Stroke(sw))
                    if (fill > 0f) drawCircle(accent, r * fill, center)
                    if (check > 0f) {
                        val w = size.width
                        val a = Offset(w * .28f, w * .52f); val b = Offset(w * .44f, w * .68f); val c = Offset(w * .74f, w * .36f)
                        val l1 = (b - a).getDistance(); val l2 = (c - b).getDistance()
                        val d = check * (l1 + l2)
                        val stroke = 2.2.dp.toPx()
                        if (d <= l1) drawLine(Stage.bg, a, a + (b - a) * (d / l1), stroke, StrokeCap.Round)
                        else {
                            drawLine(Stage.bg, a, b, stroke, StrokeCap.Round)
                            drawLine(Stage.bg, b, b + (c - b) * ((d - l1) / l2), stroke, StrokeCap.Round)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StrikeText(q.text, strike, textColor)
                Text(meta, style = Stage.body.copy(fontSize = 13.sp, lineHeight = 17.sp, color = if (overdue) Stage.bad else Stage.mute))
            }
        }
    }
}

/** Text with a strike line drawn across it, line by line, as [progress] goes 0..1. */
@Composable
private fun StrikeText(text: String, progress: Float, color: Color) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text,
        style = Stage.ui.copy(fontSize = 16.sp, lineHeight = 21.sp, color = color),
        onTextLayout = { layout = it },
        modifier = Modifier.drawWithContent {
            drawContent()
            val l = layout
            if (l != null && progress > 0f) {
                val lines = l.lineCount
                val widths = (0 until lines).map { l.getLineRight(it) - l.getLineLeft(it) }
                var remain = widths.sum() * progress
                for (i in 0 until lines) {
                    if (remain <= 0f) break
                    val w = minOf(widths[i], remain); remain -= w
                    val y = (l.getLineTop(i) + l.getLineBottom(i)) / 2f
                    val x0 = l.getLineLeft(i)
                    drawLine(color, Offset(x0, y), Offset(x0 + w, y), 1.5.dp.toPx(), StrokeCap.Round)
                }
            }
        },
    )
}

/** List / Calendar / Shelves, with an ink marker that glides to the one you pick. */
@Composable
private fun ViewSwitch(view: String, onView: (String) -> Unit) {
    val opts = listOf("ahead" to "List", "calendar" to "Calendar", "shelves" to "Shelves")
    val idx = opts.indexOfFirst { it.first == view }.coerceAtLeast(0)
    BoxWithConstraints(Modifier.padding(top = 16.dp).widthIn(max = 320.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Stage.card).padding(4.dp)) {
        val seg = maxWidth / 3
        val x by animateDpAsState(seg * idx, tween(320, easing = FastOutSlowInEasing), label = "segX")
        Box(Modifier.offset(x = x).size(width = seg, height = 38.dp).clip(RoundedCornerShape(11.dp)).background(Stage.ink))
        Row(Modifier.fillMaxWidth()) {
            opts.forEachIndexed { i, (k, l) ->
                val c by animateColorAsState(if (i == idx) Stage.bg else Stage.mute, tween(240), label = "segText")
                Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp)).clickable { onView(k) }, contentAlignment = Alignment.Center) {
                    Text(l, style = Stage.ui.copy(fontWeight = FontWeight.W600, fontSize = 14.sp, color = c))
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    StageCard(padding = PaddingValues(22.dp)) {
        Text(text, style = Stage.head.copy(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.W400, color = Stage.ink.copy(alpha = .8f)),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ShelfTile(s: Shelf, n: Int, modifier: Modifier, onClick: () -> Unit) {
    val hot = s == Shelf.OVERDUE && n > 0
    StageCard(modifier.heightIn(min = 118.dp), padding = PaddingValues(16.dp), onClick = onClick) {
        Text("$n", style = Stage.head.copy(fontSize = 38.sp, lineHeight = 42.sp, color = if (hot) Stage.bad else if (n == 0) Stage.mute else Stage.ink))
        Spacer(Modifier.height(6.dp))
        Text(s.label, style = Stage.ui.copy(fontWeight = FontWeight.W600, fontSize = 16.sp, color = if (n == 0) Stage.mute else Stage.ink))
        Text(s.blurb, style = Stage.body.copy(fontSize = 13.sp, lineHeight = 17.sp))
    }
}

@Composable
private fun Calendar(month: YearMonth, pick: LocalDate, quests: List<Quest>, accent: Color, onMonth: (YearMonth) -> Unit, onPick: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    StageCard(padding = PaddingValues(12.dp), spacing = 0.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { onMonth(month.minusMonths(1)) }, contentAlignment = Alignment.Center) { Arrow(true, Stage.ink, 14.dp) }
            Text(month.month.getDisplayName(JTextStyle.FULL, Locale.getDefault()) + " " + month.year,
                style = Stage.head.copy(fontSize = 20.sp, lineHeight = 24.sp), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { onMonth(month.plusMonths(1)) }, contentAlignment = Alignment.Center) { Arrow(false, Stage.ink, 14.dp) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, style = Stage.eyebrow, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val first = month.atDay(1)
        val start = first.minusDays((first.dayOfWeek.value % 7).toLong())
        val weeks = if (start.plusDays(35).month == month.month) 6 else 5
        for (wk in 0 until weeks) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (d in 0 until 7) {
                    val day = start.plusDays((wk * 7 + d).toLong())
                    val inMonth = day.month == month.month
                    val picked = day == pick
                    val dayQuests = quests.filter { it.due != null && Time.date(it.due) == day }
                    val bg by animateColorAsState(if (picked) accent else accent.copy(alpha = 0f), tween(220), label = "dayBg")
                    Column(
                        Modifier.weight(1f).aspectRatio(1f).padding(vertical = 2.dp).clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .then(if (day == today && !picked) Modifier.border(1.dp, Stage.mute, RoundedCornerShape(10.dp)) else Modifier)
                            .clickable { onPick(day) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text("${day.dayOfMonth}", style = Stage.ui.copy(fontSize = 15.sp,
                            color = if (picked) Stage.bg else if (inMonth) Stage.ink else Stage.mute.copy(alpha = .5f)))
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                            dayQuests.take(3).forEach { q ->
                                val c = when {
                                    picked -> Stage.bg
                                    q.done -> Stage.mute
                                    q.shelf() == Shelf.OVERDUE -> Stage.bad
                                    else -> accent
                                }
                                Box(Modifier.size(5.dp).clip(CircleShape).background(c))
                            }
                        }
                    }
                }
            }
        }
    }
}

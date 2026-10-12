package com.mwooten.chime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.questOrder
import com.mwooten.chime.data.shelf
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

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
    var shelf by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var pickStr by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val month = YearMonth.parse(monthStr); val pick = LocalDate.parse(pickStr)

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Crumb("Back", true, onBack); Crumb("Spill", false, onSpill)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val title = when {
                    query.isNotBlank() -> "Search"
                    view == "shelves" && shelf != null -> Shelf.valueOf(shelf!!).label
                    view == "calendar" -> "Calendar"
                    view == "shelves" -> "Shelves"
                    else -> "Quest Log"
                }
                Text(title, style = T.title, modifier = Modifier.weight(1f))
                Box(Modifier.size(42.dp).clip(CircleShape).clickable { searching = !searching; if (!searching) query = "" }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(18.dp)) {
                        val s = size.width
                        drawCircle(C.mist, s * .32f, Offset(s * .42f, s * .42f), style = Stroke(1.8.dp.toPx()))
                        drawLine(C.mist, Offset(s * .66f, s * .66f), Offset(s * .95f, s * .95f), 1.8.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            // three views, one pill
            Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0x8C08130F)).border(1.dp, C.line, RoundedCornerShape(50)).padding(4.dp)) {
                listOf("ahead" to "Ahead", "calendar" to "Calendar", "shelves" to "Shelves").forEach { (k, l) ->
                    val on = view == k
                    Text(l.uppercase(), style = T.eyebrow.copy(color = if (on) C.ink else C.mist, fontSize = 12.sp, letterSpacing = 1.5.sp),
                        modifier = Modifier.clip(RoundedCornerShape(50)).then(if (on) Modifier.background(C.brassBrush) else Modifier)
                            .clickable { onView(k); shelf = null }.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
            if (searching) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xCC14261E)).border(1.dp, C.line, RoundedCornerShape(12.dp)).padding(14.dp)) {
                    if (query.isEmpty()) Text("Search your quests", style = T.body.copy(color = C.faint))
                    BasicTextField(query, { query = it }, textStyle = T.body, cursorBrush = SolidColor(C.glow), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(4.dp))

            if (query.isNotBlank()) {
                val hits = quests.filter { it.text.contains(query, true) }.sortedWith(questOrder)
                Group("Matches for “$query”", hits, "Nothing in your log matches that.", onToggle, onOpen)
            } else when (view) {
                "calendar" -> {
                    Calendar(month, pick, quests,
                        onMonth = { monthStr = it.toString() }, onPick = { pickStr = it.toString() })
                    Spacer(Modifier.height(8.dp))
                    Group(Time.dayName(pick), quests.filter { it.due != null && Time.date(it.due) == pick }.sortedWith(questOrder),
                        "Nothing planned this day.", onToggle, onOpen)
                    val someday = quests.filter { it.due == null && !it.done }
                    if (someday.isNotEmpty()) Group("Someday (no date)", someday.sortedWith(questOrder), null, onToggle, onOpen)
                }
                "shelves" -> {
                    val sh = shelf
                    if (sh != null) {
                        val s = Shelf.valueOf(sh)
                        Crumb("All shelves", true) { shelf = null }
                        val list = quests.filter { it.shelf() == s }.let { l -> if (s == Shelf.DONE) l.sortedByDescending { it.doneAt ?: 0 } else l.sortedWith(questOrder) }
                        Group(s.blurb, list, "Nothing on this shelf.", onToggle, onOpen)
                    } else {
                        Shelf.entries.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { s ->
                                    val n = quests.count { it.shelf() == s }
                                    ShelfTile(s, n, Modifier.weight(1f)) { shelf = s.name }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                else -> {
                    val open = quests.filter { !it.done }
                    if (open.isEmpty()) EmptyNote("Your satchel is empty. Open the Spill board and say whatever's on your mind.")
                    Shelf.entries.filter { it != Shelf.DONE }.forEach { s ->
                        val l = open.filter { it.shelf() == s }.sortedWith(questOrder)
                        if (l.isNotEmpty()) { Group(s.label, l, null, onToggle, onOpen); Spacer(Modifier.height(10.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(title: String, list: List<Quest>, empty: String?, onToggle: (Quest) -> Unit, onOpen: (Quest) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupHeader(title, list.size)
        if (list.isEmpty() && empty != null) EmptyNote(empty)
        list.forEach { q -> QuestRow(q, { onToggle(q) }, { onOpen(q) }) }
    }
}

@Composable
private fun ShelfTile(s: Shelf, n: Int, modifier: Modifier, onClick: () -> Unit) {
    val hot = s == Shelf.OVERDUE && n > 0
    Column(
        modifier.heightIn(min = 124.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xE61F3D30), Color(0xF20F1D17))))
            .border(1.dp, if (hot) C.ember.copy(alpha = .45f) else Color(0x24D9C27A), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$n", style = T.title.copy(fontSize = 40.sp, color = if (hot) C.ember else if (n == 0) C.faint else C.brass))
        Column {
            Text(s.label, style = T.body.copy(fontSize = 17.sp, color = if (n == 0) C.mist else C.parchment))
            Text(s.blurb, style = T.small.copy(fontSize = 13.sp))
        }
    }
}

@Composable
private fun Calendar(month: YearMonth, pick: LocalDate, quests: List<Quest>, onMonth: (YearMonth) -> Unit, onPick: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xB3122219)).border(1.dp, C.line, RoundedCornerShape(18.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { onMonth(month.minusMonths(1)) }, contentAlignment = Alignment.Center) { Arrow(true, C.parchment, 14.dp) }
            Text(month.month.getDisplayName(JTextStyle.FULL, Locale.getDefault()) + " " + month.year,
                style = T.body.copy(fontSize = 19.sp), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { onMonth(month.plusMonths(1)) }, contentAlignment = Alignment.Center) { Arrow(false, C.parchment, 14.dp) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, style = T.eyebrow.copy(color = C.faint), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
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
                    Column(
                        Modifier.weight(1f).aspectRatio(1f).padding(vertical = 2.dp).clip(RoundedCornerShape(10.dp))
                            .then(if (picked) Modifier.background(C.brassBrush) else Modifier)
                            .then(if (day == today && !picked) Modifier.border(1.dp, C.brassDim, RoundedCornerShape(10.dp)) else Modifier)
                            .clickable { onPick(day) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text("${day.dayOfMonth}", style = T.body.copy(fontSize = 15.sp,
                            color = if (picked) C.ink else if (inMonth) C.parchment else C.faint.copy(alpha = .6f)))
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                            dayQuests.take(3).forEach { q ->
                                val c = when {
                                    picked -> C.ink
                                    q.done -> C.done
                                    q.shelf() == Shelf.OVERDUE -> C.ember
                                    else -> C.glow
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

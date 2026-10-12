package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BuddyDesk = Color(0xFF3A6EA5)
private val BuddyNavy = Color(0xFF0A246A)
private val BuddyChrome = Color(0xFFECE9D8)
private val BuddyFieldLine = Color(0xFF7F9DB9)
private val BuddySelect = Color(0xFF316AC5)
private val BuddyGray = Color(0xFF808080)
private val BuddyDim = Color(0xFF666666)
private val BuddyFont = FontFamily.SansSerif
private const val BuddyMe = "spilled_milk_98"
private const val BuddyAway = "out doing the thing i said i'd do. brb (probably)"

@Composable
fun BuddyWorld(s: WorldState, a: WorldActions) {
    var imOpen by rememberSaveable { mutableStateOf(true) }
    var spilling by rememberSaveable { mutableStateOf(false) }
    val screenW = LocalConfiguration.current.screenWidthDp

    Box(Modifier.fillMaxSize().background(BuddyDesk)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().imePadding()) {
                // buddy list window
                BuddyWindow(
                    title = "$BuddyMe's Buddy List",
                    modifier = Modifier
                        .padding(start = 12.dp, top = 14.dp, bottom = 14.dp)
                        .width((screenW * 0.62f).coerceIn(220f, 300f).dp)
                        .fillMaxHeight(),
                ) {
                    BuddyList(s, a, spilling, onSpill = { spilling = !spilling })
                }

                // the companion's IM window, popped up on top and knocking
                if (imOpen) {
                    val clock = rememberClock()
                    BuddyWindow(
                        title = "${buddyScreenName(s)} — Instant Message",
                        onClose = { imOpen = false },
                        elevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(start = 74.dp, end = 10.dp, bottom = 36.dp)
                            .graphicsLayer { translationY = -3.dp.toPx() * buddyKnock(clock.value) },
                    ) {
                        BuddyIm(s, a)
                    }
                }
            }

            // system tray-ish strip
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF3168D5), Color(0xFF1941A5))))
                    .navigationBarsPadding()
                    .height(40.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${s.today.size} buddies online · " + if (imOpen) "chatting" else "1 new IM",
                    color = Color.White, fontSize = 12.sp, fontFamily = BuddyFont,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).tap { imOpen = true },
                )
                Text(Time.time(s.now), color = Color.White, fontSize = 12.sp, fontFamily = BuddyFont)
            }
        }
    }
}

private fun buddyKnock(t: Float): Float {
    val p = (t % 2.5f) / 2.5f
    return when {
        p < 0.1f -> p / 0.1f
        p < 0.2f -> 1f - (p - 0.1f) / 0.1f
        p < 0.3f -> (p - 0.2f) / 0.1f
        p < 0.4f -> 1f - (p - 0.3f) / 0.1f
        else -> 0f
    }
}

private fun buddyScreenName(s: WorldState): String = when (s.companion.name) {
    "WREN" -> "WrenSong"
    "FIZZ" -> "FizzyLogic"
    "PIP" -> "PipTheSpark"
    "DIM" -> "dimlight_x"
    "BLAZE" -> "BLAZE4EVER"
    else -> s.companion.title
}

/** "call the dentist about the crown" -> "CallTheDentistAbout" */
private fun buddyHandle(text: String): String {
    val joined = text.split(Regex("\\s+"))
        .map { w -> w.filter { it.isLetterOrDigit() } }
        .filter { it.isNotEmpty() }
        .joinToString("") { w -> w.replaceFirstChar { it.uppercaseChar() } }
    return joined.ifEmpty { "Buddy" }.take(22)
}

@Composable
private fun BuddyWindow(
    title: String,
    modifier: Modifier,
    onClose: (() -> Unit)? = null,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .shadow(elevation, RectangleShape)
            .background(BuddyChrome)
            .border(1.dp, BuddyNavy)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Brush.horizontalGradient(listOf(BuddyNavy, BuddyDesk)))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = BuddyFont,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (onClose != null) Text(
                "×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                modifier = Modifier.tap(onClose).padding(start = 8.dp),
            )
        }
        content()
    }
}

@Composable
private fun BuddyButton(label: String, bold: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD6D3C2))), RoundedCornerShape(3.dp))
            .border(1.dp, Color(0xFF7F7D6A), RoundedCornerShape(3.dp))
            .tap(onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontSize = 13.sp, fontFamily = BuddyFont, color = Color.Black,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, maxLines = 1,
        )
    }
}

@Composable
private fun ColumnScope.BuddyList(
    s: WorldState, a: WorldActions, spilling: Boolean, onSpill: () -> Unit,
) {
    // away message
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp).background(Color(0xFFE8B020), CircleShape)
                    .border(1.dp, Color(0xFF8A6010), CircleShape)
            )
            Text(
                "$BuddyMe (Away)", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = BuddyFont,
                color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Text(
            BuddyAway, fontFamily = Fonts.comicNeue, fontWeight = FontWeight.W700, fontSize = 13.sp,
            lineHeight = 17.sp, color = Color(0xFF7A2A9A),
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFACA899)))

    // groups = time windows; buddies = quests
    var showToday by rememberSaveable { mutableStateOf(true) }
    var showWeek by rememberSaveable { mutableStateOf(true) }
    var showLater by rememberSaveable { mutableStateOf(false) }
    var showSomeday by rememberSaveable { mutableStateOf(false) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(6.dp)
            .background(Color.White)
            .border(1.dp, BuddyFieldLine)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BuddyGroup("Today", s.today, showToday, { showToday = !showToday }, s, a, first = true)
        BuddyGroup("This Week", s.week, showWeek, { showWeek = !showWeek }, s, a)
        if (s.later.isNotEmpty()) BuddyGroup("Later", s.later, showLater, { showLater = !showLater }, s, a)
        BuddyGroup("Someday", s.someday, showSomeday, { showSomeday = !showSomeday }, s, a)
        Text(
            (if (showDone) "▼" else "▶") + " Offline · Done (${s.done.size})",
            fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = BuddyFont, color = BuddyGray,
            modifier = Modifier.padding(top = 6.dp).tap { showDone = !showDone },
        )
        if (showDone) Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            s.done.take(20).forEach { q ->
                Text(
                    buddyHandle(q.text), fontSize = 13.sp, fontFamily = BuddyFont, color = BuddyGray,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().tap { a.openQuest(q) }.padding(horizontal = 3.dp, vertical = 1.dp),
                )
            }
        }
    }

    if (spilling) {
        SpillInput(
            onSpill = a::spill,
            textStyle = TextStyle(fontSize = 13.sp, fontFamily = BuddyFont, color = Color.Black),
            placeholder = "Add a Buddy (new quest)…",
            placeholderColor = BuddyGray,
            cursor = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                .background(Color.White)
                .border(1.dp, BuddyFieldLine)
                .padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            trailing = { submit ->
                BuddyButton("Add", true, Modifier.padding(start = 4.dp)) { submit() }
            },
        )
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BuddyButton("Spill", true, Modifier.weight(1f)) { onSpill() }
        BuddyButton("Log", false, Modifier.weight(1f)) { a.openLog() }
        BuddyButton("Setup", false, Modifier.weight(1f)) { a.openOptions() }
    }
}

@Composable
private fun BuddyGroup(
    title: String, quests: List<Quest>, open: Boolean, toggle: () -> Unit,
    s: WorldState, a: WorldActions, first: Boolean = false,
) {
    Text(
        (if (open) "▼ " else "▶ ") + "$title (${quests.size}/${quests.size})",
        fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = BuddyFont, color = Color.Black,
        modifier = Modifier.padding(top = if (first) 0.dp else 6.dp).tap(toggle),
    )
    if (!open) return
    Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        quests.forEach { q ->
            val sel = q.id == s.focus?.id
            val idleDays = ((s.now - q.created) / 86_400_000L).toInt()
            val idle = q.due == null && idleDays >= 1
            val late = q.shelf(s.now) == Shelf.OVERDUE
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (sel) Modifier.background(BuddySelect) else Modifier)
                    .tap { a.openQuest(q) }
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    buddyHandle(q.text) + if (idle) " (idle ${idleDays}d)" else "",
                    fontSize = 13.sp, fontFamily = BuddyFont,
                    fontStyle = if (idle) FontStyle.Italic else FontStyle.Normal,
                    color = when { sel -> Color.White; idle -> BuddyGray; late -> Color(0xFFB0101B); else -> Color.Black },
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(
                    buddyWhen(q, s.now), fontSize = 11.sp, fontFamily = BuddyFont,
                    color = if (sel) Color.White else BuddyDim, maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

private val buddyClock = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
private val buddyStamp = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.getDefault())

private fun buddyWhen(q: Quest, now: Long): String {
    val d = q.due ?: return ""
    val sameDay = Time.date(d) == Time.date(now)
    if (q.allDay) return if (sameDay) "today" else Time.dayName(Time.date(d)).take(3)
    if (sameDay) return buddyClock.format(Time.ldt(d))
    return Time.dayName(Time.date(d)).take(3)
}

@Composable
private fun BuddyIm(s: WorldState, a: WorldActions) {
    val name = buddyScreenName(s)
    val nameColor = s.companion.cDark
    val focus = s.focus
    fun stamp(back: Int) = buddyStamp.format(Time.ldt(s.now - back * 1000L))

    Column(
        Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .background(Color.White)
            .border(1.dp, BuddyFieldLine)
            .heightIn(max = 220.dp)
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BuddyMsg(name, nameColor, stamp(9), s.companion.callout.lowercase())
        BuddyMsg(name, nameColor, stamp(6), s.line)
        Text(
            "Auto response from $BuddyMe: $BuddyAway",
            color = BuddyGray, fontStyle = FontStyle.Italic, fontSize = 12.sp, fontFamily = BuddyFont, lineHeight = 16.sp,
        )
        if (focus != null) {
            val left = focus.minutesLeft(s.now)
            val nudge = when {
                left == null -> "type done or brb 30. i'll wait"
                left < 0 -> "it's late. type done or brb 30"
                left < 60 -> "${left} min. type done or brb 30"
                else -> "${(left + 30) / 60} hrs. type done or brb 30"
            }
            BuddyMsg(name, nameColor, stamp(2), nudge)
        }
    }
    SpillInput(
        onSpill = { text ->
            val t = text.trim().lowercase()
            when {
                focus != null && (t == "done" || t == "done!" || t == "did it") -> a.done(focus)
                focus != null && t.startsWith("brb") -> {
                    val mins = Regex("\\d+").find(t)?.value?.toIntOrNull() ?: 30
                    a.snooze(focus, mins.coerceIn(1, 60 * 24 * 7))
                }
                else -> a.spill(text)
            }
        },
        textStyle = TextStyle(fontSize = 13.sp, fontFamily = BuddyFont, color = Color.Black),
        placeholder = if (focus != null) "done / brb 30 / new quest" else "type a new quest",
        placeholderColor = BuddyGray,
        cursor = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
            .background(Color.White)
            .border(1.dp, BuddyFieldLine)
            .padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        trailing = { submit -> BuddyButton("Send", true, Modifier.padding(start = 6.dp)) { submit() } },
    )
}

@Composable
private fun BuddyMsg(name: String, color: Color, stamp: String, text: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(name) }
            withStyle(SpanStyle(color = BuddyGray, fontSize = 11.sp)) { append(" ($stamp)") }
            append(": ")
            append(text)
        },
        fontSize = 13.sp, lineHeight = 18.sp, fontFamily = BuddyFont, color = Color.Black,
    )
}

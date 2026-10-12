package com.mwooten.chime.worlds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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

private val BuddyDesk = Color(0xFF3A6EA5)
private val BuddyNavy = Color(0xFF0A246A)
private val BuddyTitle2 = Color(0xFF2A5BB8)
private val BuddyChrome = Color(0xFFECE9D8)
private val BuddyChromeDark = Color(0xFFACA899)
private val BuddyFieldLine = Color(0xFF7F9DB9)
private val BuddySelect = Color(0xFF316AC5)
private val BuddyGray = Color(0xFF808080)
private val BuddyDim = Color(0xFF5E5E5E)
private val BuddyAwayInk = Color(0xFF7A2A9A)
private val BuddyFont = FontFamily.SansSerif
private const val BuddyMe = "spilled_milk_98"
private const val BuddyAway = "out doing the thing i said i'd do. brb (probably)"

@Composable
fun BuddyWorld(s: WorldState, a: WorldActions) {
    var imOpen by rememberSaveable { mutableStateOf(true) }
    var spilling by rememberSaveable { mutableStateOf(false) }
    val voice = rememberVoice { heard -> a.spill(heard) }

    Column(Modifier.fillMaxSize().background(BuddyDesk)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().statusBarsPadding().imePadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // buddy list window
            BuddyWindow(
                title = "$BuddyMe — Buddy List",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                // menu bar, where these windows always kept their settings
                Row(
                    Modifier.fillMaxWidth().background(BuddyChrome).padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BuddyMenu("My List") { a.openLog() }
                    BuddyMenu("Add Buddy") { spilling = !spilling }
                    BuddyMenu("Settings", strong = true) { a.openOptions() }
                    Spacer(Modifier.weight(1f))
                    BuddyMenu(if (imOpen) "Hide IM" else "Show IM") { imOpen = !imOpen }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(BuddyChromeDark))
                BuddyList(s, a, voice, spilling, onToggleSpill = { spilling = !spilling })
            }

            // the companion's IM window, docked under the list so it never covers it
            if (imOpen) {
                BuddyWindow(
                    title = "${buddyScreenName(s)} — Instant Message",
                    onClose = { imOpen = false },
                    modifier = Modifier.weight(0.82f).fillMaxWidth(),
                ) {
                    BuddyIm(s, a, voice)
                }
            }
        }

        // taskbar-ish strip: one button per window and a clock
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF245DDA))
                .navigationBarsPadding()
                .height(38.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BuddyTask("Buddy List", active = !imOpen, modifier = Modifier.weight(1f)) { imOpen = false }
            BuddyTask(
                if (imOpen) buddyScreenName(s) else "${buddyScreenName(s)} (1 new)",
                active = imOpen, modifier = Modifier.weight(1f),
            ) { imOpen = true }
            Text(
                Time.time(s.now), color = Color.White, fontSize = 12.sp, fontFamily = BuddyFont,
                modifier = Modifier.background(Color(0xFF0F8FE8)).padding(horizontal = 8.dp, vertical = 9.dp),
            )
        }
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
    return joined.ifEmpty { "Buddy" }.take(24)
}

@Composable
private fun BuddyWindow(
    title: String,
    modifier: Modifier,
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.background(BuddyChrome).border(1.dp, BuddyNavy)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(Brush.verticalGradient(listOf(BuddyTitle2, BuddyNavy)))
                .padding(start = 8.dp, end = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = BuddyFont,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (onClose != null) Box(
                Modifier
                    .size(22.dp)
                    .background(Color(0xFFD8553A), RoundedCornerShape(3.dp))
                    .border(1.dp, Color.White, RoundedCornerShape(3.dp))
                    .tap(onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = BuddyFont)
            }
        }
        content()
    }
}

/** A menu-bar word with its first letter underlined, the way menus used to show their shortcut. */
@Composable
private fun BuddyMenu(label: String, strong: Boolean = false, onClick: () -> Unit) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(label.take(1)) }
            append(label.drop(1))
        },
        fontSize = 13.sp, fontFamily = BuddyFont, color = Color.Black,
        fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal, maxLines = 1,
        modifier = Modifier.tap(onClick).padding(horizontal = 7.dp, vertical = 7.dp),
    )
}

@Composable
private fun BuddyButton(label: String, bold: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(Color(0xFFF4F3EE), RoundedCornerShape(3.dp))
            .border(1.dp, Color(0xFF003C74), RoundedCornerShape(3.dp))
            .tap(onClick)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontSize = 13.sp, fontFamily = BuddyFont, color = Color.Black,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, maxLines = 1,
        )
    }
}

@Composable
private fun BuddyTask(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        label, color = Color.White, fontSize = 12.sp, fontFamily = BuddyFont,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(if (active) Color(0xFF1E4AB8) else Color(0xFF3C81F3), RoundedCornerShape(3.dp))
            .tap(onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
    )
}

/** A white sunken field, the inset box every list and text area sat in. */
private fun Modifier.buddyField(): Modifier = this
    .background(Color.White)
    .drawBehind {
        val w = 1.dp.toPx()
        drawLine(Color(0xFF7F9DB9), Offset(0f, 0f), Offset(size.width, 0f), w)
        drawLine(Color(0xFF7F9DB9), Offset(0f, 0f), Offset(0f, size.height), w)
        drawLine(Color(0xFFD6D3C6), Offset(0f, size.height - w), Offset(size.width, size.height - w), w)
        drawLine(Color(0xFFD6D3C6), Offset(size.width - w, 0f), Offset(size.width - w, size.height), w)
    }

@Composable
private fun ColumnScope.BuddyList(
    s: WorldState, a: WorldActions, voice: Voice, spilling: Boolean, onToggleSpill: () -> Unit,
) {
    // me: buddy icon (your character), screen name, away message
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(52.dp).clipToBounds().background(Color.White).border(1.dp, BuddyFieldLine),
            contentAlignment = Alignment.Center,
        ) {
            CharacterArt(
                size = 46.dp, style = ArtStyle.FLAT, level = voice.level, listening = voice.listening,
                glow = false, onTap = { voice.toggle() },
            )
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
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
                lineHeight = 16.sp, color = BuddyAwayInk, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                if (voice.listening) "listening… tap your icon to stop" else "tap your buddy icon to talk",
                fontSize = 11.sp, fontFamily = BuddyFont, color = if (voice.listening) BuddySelect else BuddyDim,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }

    // groups = time windows; buddies = quests
    var showToday by rememberSaveable { mutableStateOf(true) }
    var showWeek by rememberSaveable { mutableStateOf(true) }
    var showLater by rememberSaveable { mutableStateOf(false) }
    var showSomeday by rememberSaveable { mutableStateOf(true) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    val total = s.open.size
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .buddyField()
            .verticalScroll(rememberScrollState())
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
    ) {
        BuddyGroup("Due today", s.today, total, showToday, { showToday = !showToday }, s, a)
        BuddyGroup("This week", s.week, total, showWeek, { showWeek = !showWeek }, s, a)
        if (s.later.isNotEmpty()) BuddyGroup("Later", s.later, total, showLater, { showLater = !showLater }, s, a)
        BuddyGroup("Someday", s.someday, total, showSomeday, { showSomeday = !showSomeday }, s, a)
        BuddyGroupHeader("Offline · Done", "${s.done.size}", showDone, muted = true) { showDone = !showDone }
        if (showDone) s.done.take(20).forEach { q ->
            Text(
                buddyHandle(q.text), fontSize = 13.sp, fontFamily = BuddyFont, color = BuddyGray,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().tap { a.openQuest(q) }.padding(start = 30.dp, top = 3.dp, bottom = 3.dp),
            )
        }
    }

    if (spilling) {
        SpillInput(
            onSpill = { t -> a.spill(t); onToggleSpill() },
            textStyle = TextStyle(fontSize = 13.sp, fontFamily = BuddyFont, color = Color.Black),
            placeholder = "New buddy (a new to-do)…",
            placeholderColor = BuddyGray,
            cursor = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp)
                .buddyField()
                .padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            trailing = { submit -> BuddyButton("Add", true, Modifier.padding(start = 4.dp)) { submit() } },
        )
    }
    // the classic button row along the bottom of the list
    Row(
        Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BuddyButton(if (spilling) "Cancel" else "Add", true, Modifier.weight(1f)) { onToggleSpill() }
        BuddyButton("List", false, Modifier.weight(1f)) { a.openLog() }
        BuddyButton("Settings", false, Modifier.weight(1.3f)) { a.openOptions() }
    }
}

@Composable
private fun BuddyGroupHeader(title: String, count: String, open: Boolean, muted: Boolean = false, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tap(toggle).padding(top = 6.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (open) "▾" else "▸", fontSize = 13.sp, fontFamily = BuddyFont,
            color = if (muted) BuddyGray else Color.Black, modifier = Modifier.width(14.dp),
        )
        Text(
            "$title ($count)", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = BuddyFont,
            color = if (muted) BuddyGray else Color.Black, maxLines = 1,
        )
    }
}

@Composable
private fun BuddyGroup(
    title: String, quests: List<Quest>, total: Int, open: Boolean, toggle: () -> Unit,
    s: WorldState, a: WorldActions,
) {
    BuddyGroupHeader(title, "${quests.size}/$total", open, toggle = toggle)
    if (!open) return
    quests.forEach { q ->
        val sel = q.id == s.focus?.id
        val idleDays = ((s.now - q.created) / 86_400_000L).toInt()
        val idle = q.due == null && idleDays >= 1
        val late = q.shelf(s.now) == Shelf.OVERDUE
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 26.dp)
                .then(if (sel) Modifier.background(BuddySelect) else Modifier)
                .tap { a.openQuest(q) }
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuddyStatus(late = late, idle = idle)
            Text(
                buddyHandle(q.text) + if (idle) " (idle ${idleDays}d)" else "",
                fontSize = 13.sp, fontFamily = BuddyFont,
                fontStyle = if (idle) FontStyle.Italic else FontStyle.Normal,
                color = when { sel -> Color.White; idle -> BuddyGray; late -> Color(0xFFB0101B); else -> Color.Black },
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
            Text(
                buddyWhen(q, s.now), fontSize = 11.sp, fontFamily = BuddyFont,
                color = if (sel) Color.White else BuddyDim, maxLines = 1,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** Little status glyph left of each buddy: a red "late" door, a gray idle dot, or a yellow running man-ish dot. */
@Composable
private fun RowScope.BuddyStatus(late: Boolean, idle: Boolean) {
    val fill = when { late -> Color(0xFFD23A2A); idle -> Color(0xFFBDBDBD); else -> Color(0xFF3FAE2A) }
    Box(Modifier.size(9.dp).background(fill, RoundedCornerShape(2.dp)).border(1.dp, Color(0x55000000), RoundedCornerShape(2.dp)))
}

private val buddyClock = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val buddyStamp = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.getDefault())

private fun buddyWhen(q: Quest, now: Long): String {
    val d = q.due ?: return ""
    val sameDay = Time.date(d) == Time.date(now)
    if (q.allDay) return if (sameDay) "today" else Time.dayName(Time.date(d)).take(3)
    if (sameDay) return buddyClock.format(Time.ldt(d))
    return Time.dayName(Time.date(d)).take(3)
}

@Composable
private fun ColumnScope.BuddyIm(s: WorldState, a: WorldActions, voice: Voice) {
    val name = buddyScreenName(s)
    val nameColor = s.companion.cDark
    val focus = s.focus
    val heard = voice.heard
    fun stamp(back: Int) = buddyStamp.format(Time.ldt(s.now - back * 1000L))
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue, voice.listening, heard) { scroll.scrollTo(scroll.maxValue) }

    Row(Modifier.weight(1f).fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp)) {
        // their buddy icon: your character, in this personality's face
        Box(
            Modifier.size(52.dp).clipToBounds().background(Color.White).border(1.dp, BuddyFieldLine),
            contentAlignment = Alignment.Center,
        ) {
            CharacterArt(size = 46.dp, style = ArtStyle.FLAT, who = s.companion, glow = false, onTap = { voice.toggle() })
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 6.dp)
                .buddyField()
                .verticalScroll(scroll)
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
                    left < 60 -> "$left min. type done or brb 30"
                    else -> "${(left + 30) / 60} hrs. type done or brb 30"
                }
                BuddyMsg(name, nameColor, stamp(2), nudge)
            }
            if (voice.listening && heard.isNotBlank()) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0000CC))) { append(BuddyMe) }
                        append(": ")
                        withStyle(SpanStyle(color = BuddyGray, fontStyle = FontStyle.Italic)) { append("$heard…") }
                    },
                    fontSize = 13.sp, lineHeight = 18.sp, fontFamily = BuddyFont, color = Color.Black,
                )
            }
        }
    }
    // status line under the conversation, like "is typing"
    val note = voice.note
    Text(
        when {
            voice.listening -> "$BuddyMe is typing…"
            note.isNotBlank() -> note
            else -> "Last message received at ${Time.time(s.now)}."
        },
        fontSize = 11.sp, fontFamily = BuddyFont, color = BuddyDim, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
    )
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
        placeholder = if (focus != null) "done / brb 30 / new to-do" else "type a new to-do",
        placeholderColor = BuddyGray,
        cursor = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 6.dp)
            .buddyField()
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

package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val TermBg = Color(0xFF0D0904)
private val TermAmber = Color(0xFFFFB000)
private val TermBright = Color(0xFFFFCF6A)
private val TermUser = Color(0xFFFFD27A)
private val TermPath = Color(0xFFC89A3A)
private val TermDim = Color(0xFF8A6A20)
private val TermRule = Color(0xFF5A4210)
private val TermAlarm = Color(0xFFFF5A3C)
private val TermText = TextStyle(
    fontFamily = Fonts.ibmPlexMono, fontSize = 13.sp, lineHeight = 20.sp, color = TermAmber,
    shadow = Shadow(Color(0x73FFB000), Offset.Zero, 12f),
)

private val TermHm = DateTimeFormatter.ofPattern("HH:mm")
private val TermDay = DateTimeFormatter.ofPattern("EEE")
private val TermMd = DateTimeFormatter.ofPattern("MM/dd")
private val TermLogin = DateTimeFormatter.ofPattern("EEE MMM d HH:mm")

/** "Jury duty letter!" -> "jury-duty-letter" */
private fun termSlug(text: String): String =
    text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').take(18).trimEnd('-').ifEmpty { "quest" }

/** Compact day/time: "12:00", "today", "fri", "10/31", "--". */
private fun termWhen(ms: Long, allDay: Boolean, now: Long): String {
    val days = ChronoUnit.DAYS.between(Time.date(now), Time.date(ms))
    return when {
        days == 0L -> if (allDay) "today" else TermHm.format(Time.ldt(ms))
        days in -6L..6L -> TermDay.format(Time.ldt(ms)).lowercase().take(3)
        else -> TermMd.format(Time.ldt(ms))
    }
}

/** "fri 19:12" style stamp. */
private fun termStamp(ms: Long, now: Long): String {
    val days = ChronoUnit.DAYS.between(Time.date(ms), Time.date(now))
    val day = if (days in 0L..6L) TermDay.format(Time.ldt(ms)).lowercase().take(3) else TermMd.format(Time.ldt(ms))
    return "$day ${TermHm.format(Time.ldt(ms))}"
}

/** "(T-02:16:04)" countdown, or "(T+00:12:30 LATE)". */
private fun termCountdown(due: Long, now: Long): String {
    val diff = due - now
    val sec = kotlin.math.abs(diff) / 1000
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    val body = if (h >= 100) "${h / 24}d ${"%02d".format(h % 24)}h" else "%02d:%02d:%02d".format(h, m, s)
    return if (diff >= 0) "(T-$body)" else "(T+$body LATE)"
}

@Composable
fun TerminalWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val c = s.companion
    val name = c.title.lowercase()
    val clock = rememberClock()
    var live by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); live = System.currentTimeMillis() }
    }
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val rows = s.open.take(8)

    Box(Modifier.fillMaxSize().background(TermBg)) {
        Column(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = if (((clock.value * 16f).toInt() % 2) == 0) 1f else 0.96f }
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(top = 14.dp)) {
                Text("Last login: ${TermLogin.format(Time.ldt(s.now))} on ttys001", style = TermText.copy(color = TermDim), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TermPrompt("chime ls today")
                Text("DUE    QUEST              STATUS", style = TermText.copy(color = TermBright), maxLines = 1, softWrap = false)
                if (rows.isEmpty()) {
                    Text("total 0", style = TermText.copy(color = TermDim))
                }
                rows.forEach { item ->
                    TermRow(item, item == q, s.now, Modifier.tap { a.openQuest(item) })
                }
                if (s.open.size > rows.size) {
                    Text("... ${s.open.size - rows.size} more (chime ls --all)", style = TermText.copy(color = TermDim), modifier = Modifier.tap(a::openLog))
                }
                Spacer(Modifier.height(8.dp))

                if (q != null) {
                    val slug = termSlug(q.text)
                    TermPrompt("cat $slug", Modifier.tap { a.openQuest(q) })
                    TermField("spilled:", termStamp(q.created, s.now))
                    TermField("raw:", "\"${q.text}\"")
                    val due = q.due
                    if (due == null) {
                        TermField("due:", "someday  (no deadline)")
                    } else if (q.allDay) {
                        TermField("due:", "${termWhen(due, true, s.now)}  (all day)")
                    } else {
                        TermField("due:", "${termStamp(due, s.now)}  ${termCountdown(due, live)}")
                        TermField("type:", if (q.kind == "by") "deadline" else "appointment")
                    }
                    TermField("status:", if (q.shelf(s.now) == Shelf.OVERDUE) "OVERDUE" else "open")
                    Spacer(Modifier.height(8.dp))
                }

                // the companion, logging to stderr
                val voice = TermText.copy(color = c.c, fontWeight = FontWeight.Bold)
                TermLog(name, s.line, voice)
                if (s.overdueCount > 0) {
                    TermLog(name, "ERR ${s.overdueCount} quest${if (s.overdueCount == 1) "" else "s"} past due", voice)
                }
                if (q != null) {
                    TermLog(name, "FIX: chime done ${termSlug(q.text)}", voice)
                } else {
                    TermLog(name, "HINT: chime add \"something\"", voice)
                }
                Spacer(Modifier.height(8.dp))

                // the prompt line is the spill input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = TermUser)) { append("you@chime") }
                            append(":")
                            withStyle(SpanStyle(color = TermPath)) { append("~") }
                            append("$ chime add ")
                        },
                        style = TermText, maxLines = 1, softWrap = false,
                    )
                    SpillInput(
                        onSpill = a::spill,
                        textStyle = TermText,
                        placeholder = "\"pay rent fri\"",
                        placeholderColor = TermRule,
                        cursor = TermAmber,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("  ↳ press enter to spill; dates get parsed", style = TermText.copy(color = TermDim))
                Spacer(Modifier.height(12.dp))
            }

            // command shortcuts, as a man-page style footer
            Column(
                Modifier.fillMaxWidth().drawBehind {
                    drawRect(TermRule, Offset.Zero, Size(size.width, 1.dp.toPx()))
                }.padding(top = 10.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("TAB to complete · ↑ for history", style = TermText.copy(color = TermDim), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("[man chime]", style = TermText.copy(color = TermBright), maxLines = 1, modifier = Modifier.tap(a::openOptions).padding(start = 8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (q != null) {
                        TermButton("done", filled = true, Modifier.weight(1f)) { a.done(q) }
                        TermButton("snooze 30", filled = false, Modifier.weight(1f)) { a.snooze(q, 30) }
                    } else {
                        TermButton("options", filled = false, Modifier.weight(1f), onClick = a::openOptions)
                    }
                    TermButton("ls week", filled = false, Modifier.weight(1f), onClick = a::openLog)
                }
            }
        }

        // scanlines and CRT vignette, on top of everything (they don't take touches)
        Canvas(Modifier.fillMaxSize()) {
            val p = 1.dp.toPx()
            val step = 3.dp.toPx()
            var y = 0f
            val line = Color.Black.copy(alpha = 0.28f)
            while (y < size.height) { drawRect(line, Offset(0f, y), Size(size.width, p)); y += step }
            drawRect(
                Brush.radialGradient(
                    0f to Color.Transparent,
                    0.65f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.85f),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = maxOf(size.width, size.height) * 0.62f,
                )
            )
        }
    }
}

@Composable
private fun TermPrompt(cmd: String, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = TermUser)) { append("you@chime") }
            append(":")
            withStyle(SpanStyle(color = TermPath)) { append("~") }
            append("$ ")
            append(cmd)
        },
        style = TermText, maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun TermRow(q: Quest, focus: Boolean, now: Long, modifier: Modifier) {
    val due = q.due
    val whenCol = if (due == null) "--" else termWhen(due, q.allDay, now)
    val late = q.shelf(now) == Shelf.OVERDUE
    val status = when {
        late -> "LATE  !!"
        focus -> "OPEN  !!"
        due == null -> "idle ${ChronoUnit.DAYS.between(Time.date(q.created), Time.date(now)).coerceAtLeast(0)}d"
        else -> "OPEN"
    }
    val line = whenCol.padEnd(6).take(6) + " " + termSlug(q.text).padEnd(18) + " " + status
    val style = when {
        late || focus -> TermText.copy(color = TermAlarm, fontWeight = FontWeight.Bold)
        due == null -> TermText.copy(color = TermDim)
        else -> TermText
    }
    Text(line, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = modifier.fillMaxWidth())
}

@Composable
private fun TermField(key: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(key.padEnd(10), style = TermText, maxLines = 1, softWrap = false)
        Text(value, style = TermText, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TermLog(name: String, msg: String, style: TextStyle) {
    Row(Modifier.fillMaxWidth()) {
        Text("[$name] ", style = style, maxLines = 1, softWrap = false)
        Text(msg, style = style, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TermButton(label: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .then(if (filled) Modifier.background(TermAmber) else Modifier)
            .border(1.dp, if (filled) TermAmber else TermDim)
            .tap(onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (filled) TermText.copy(color = TermBg, fontWeight = FontWeight.Bold, shadow = null) else TermText,
            maxLines = 1,
        )
    }
}

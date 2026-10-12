package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap

private val D98Face = Color(0xFFC3C3C3)
private val D98Desk = Color(0xFF2F7D7A)
private val D98Shadow = Color(0xFF808080)
private val D98Dark = Color(0xFF0A0A0A)
private val D98Light = Color(0xFFDFDFDF)
private val D98Text = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = Color.Black)

/** The classic two-pixel raised bevel. */
private fun Modifier.d98Raised(fill: Color = D98Face): Modifier = this.drawBehind {
    val p = 1.dp.toPx()
    drawRect(fill)
    d98Edges(p, 0f, Color.White, D98Dark)
    d98Edges(p, p, D98Light, D98Shadow)
}

/** Sunken field (text boxes, taskbar wells). */
private fun Modifier.d98Sunken(fill: Color, deep: Boolean): Modifier = this.drawBehind {
    val p = 1.dp.toPx()
    drawRect(fill)
    d98Edges(p, 0f, D98Shadow, Color.White)
    if (deep) d98Edges(p, p, D98Dark, D98Light)
}

private fun DrawScope.d98Edges(p: Float, inset: Float, tl: Color, br: Color) {
    val w = size.width - 2 * inset
    val h = size.height - 2 * inset
    if (w <= 0f || h <= 0f) return
    drawRect(tl, Offset(inset, inset), Size(w, p))
    drawRect(tl, Offset(inset, inset), Size(p, h))
    drawRect(br, Offset(inset, inset + h - p), Size(w, p))
    drawRect(br, Offset(inset + w - p, inset), Size(p, h))
}

private fun Modifier.d98Dotted(color: Color, insetDp: Dp): Modifier = this.drawBehind {
    val p = 1.dp.toPx()
    val i = insetDp.toPx()
    drawRect(
        color, Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i),
        style = Stroke(width = p, pathEffect = PathEffect.dashPathEffect(floatArrayOf(p, p))),
    )
}

private fun d98Poly(sx: Float, sy: Float, vararg pts: Float): Path = Path().apply {
    moveTo(pts[0] * sx, pts[1] * sy)
    var i = 2
    while (i + 1 < pts.size) { lineTo(pts[i] * sx, pts[i + 1] * sy); i += 2 }
    close()
}

/** "jury duty letter" -> "Jury_duty_letter.exe" */
private fun d98Exe(text: String): String {
    val base = text.trim().replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').take(24).trimEnd('_')
    val name = if (base.isEmpty()) "Quest" else base.lowercase().replaceFirstChar { it.uppercaseChar() }
    return "$name.exe"
}

/** The day it was spilled: "Thursday", "yesterday", or a clock time for today. */
private fun d98Since(q: Quest): String {
    val d = Time.dayName(Time.date(q.created))
    return when (d) {
        "Today" -> Time.time(q.created)
        "Yesterday" -> "yesterday"
        else -> d
    }
}

@Composable
fun Desktop98World(s: WorldState, a: WorldActions) {
    val q = s.focus
    val clock = rememberClock()
    val c = s.companion
    val okHolder = remember { arrayOfNulls<() -> Unit>(1) }
    val voice = rememberVoice { heard -> a.spill(heard) }
    var startOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(D98Desk)) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding()
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            // desktop icons: one folder per time window
            Row(
                Modifier.padding(start = 10.dp, top = 18.dp, end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                D98Icon("Today (${s.today.size})", selected = true, trash = false, onClick = a::openLog)
                D98Icon("This Week (${s.week.size})", selected = false, trash = false, onClick = a::openLog)
                D98Icon("Someday (${s.someday.size})", selected = false, trash = false, onClick = a::openLog)
                D98Icon("Done (${s.done.size})", selected = false, trash = true, onClick = a::openLog)
            }
            Spacer(Modifier.height(28.dp))

            Column(Modifier.padding(horizontal = 22.dp)) {
                // the error dialog that IS the reminder
                if (q != null) {
                    D98Window(d98Exe(q.text), active = true, onClose = { a.snooze(q, 30) }) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Canvas(Modifier.size(34.dp)) { d98Warning() }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "This quest is not responding. It has been not responding since ${d98Since(q)}.",
                                    style = D98Text.copy(fontSize = 13.sp, lineHeight = 17.sp),
                                )
                                Text(
                                    "“${q.text}”",
                                    style = D98Text.copy(fontSize = 13.sp, lineHeight = 17.sp),
                                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                                )
                                Text(d98DueLine(q, s.now), style = D98Text.copy(fontSize = 13.sp, lineHeight = 17.sp))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            D98Button("Done", 64.dp, focused = true) { a.done(q) }
                            D98Button("Later", 64.dp) { a.snooze(q, 30) }
                            D98Button("Open...", 72.dp) { a.openQuest(q) }
                        }
                    }
                } else {
                    D98Window("Chime.exe", active = true, onClose = null) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Canvas(Modifier.size(34.dp)) { d98Info() }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No programs are not responding.", style = D98Text.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                Text("${c.title}: ${s.line}", style = D98Text.copy(fontSize = 13.sp, lineHeight = 17.sp))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            D98Button("OK", 84.dp, focused = true, onClick = a::openLog)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Run box = add a to-do
                D98Window("New To-Do", active = false, onClose = null) {
                    Column(
                        Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(voice.note.ifBlank { "Type a to-do and Chime will file it, or click ${c.title} to say it." }, style = D98Text)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("To-do:", style = D98Text)
                            Box(Modifier.weight(1f).d98Sunken(Color.White, deep = true).padding(horizontal = 6.dp, vertical = 5.dp)) {
                                SpillInput(
                                    onSpill = a::spill,
                                    textStyle = D98Text.copy(fontSize = 13.sp),
                                    placeholder = "call dentist by 3pm",
                                    placeholderColor = D98Shadow,
                                    cursor = Color.Black,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailing = { submit -> okHolder[0] = submit },
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                            D98Button("OK", 64.dp) { okHolder[0]?.invoke() }
                            D98Button("Browse...", 76.dp, onClick = a::openLog)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // the assistant's tooltip balloon
        val balloon: String? = when {
            voice.listening -> "I'm listening... " + voice.heard
            q != null -> s.line
            else -> null
        }
        if (balloon != null) {
            Box(Modifier.fillMaxWidth().padding(start = 120.dp, end = 14.dp)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${c.title}: ") }
                        append(balloon)
                    },
                    style = D98Text.copy(lineHeight = 16.sp),
                    maxLines = 5, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = 230.dp)
                        .background(Color(0xFFFFFFE1))
                        .drawBehind { drawRect(Color.Black, style = Stroke(1.dp.toPx())) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        // your character as a little pixel desktop pet pacing above the taskbar. Click it to talk.
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            Box(
                Modifier
                    .offset {
                        val t = clock.value
                        val p = (t % 9f) / 9f
                        val travel = 150.dp.toPx()
                        val dx = if (voice.listening) 0f else when {
                            p < 0.48f -> travel * (p / 0.48f)
                            p < 0.5f -> travel
                            p < 0.98f -> travel * (1f - (p - 0.5f) / 0.48f)
                            else -> 0f
                        }
                        IntOffset((110.dp.toPx() + dx).toInt(), 0)
                    }
                    .graphicsLayer {
                        val p = (clock.value % 9f) / 9f
                        scaleX = if (!voice.listening && p >= 0.5f && p < 0.98f) -1f else 1f
                    },
            ) {
                CharacterArt(
                    40.dp, ArtStyle.PIXEL, who = c, level = voice.level, listening = voice.listening,
                    glow = false, pixels = 24, onTap = { voice.toggle() },
                )
            }
        }

        // taskbar
        Column(
            Modifier.fillMaxWidth().background(D98Face).drawBehind {
                val p = 1.dp.toPx()
                drawRect(D98Light, Offset.Zero, Size(size.width, p))
                drawRect(Color.White, Offset(0f, p), Size(size.width, p))
            }.navigationBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    Modifier.height(26.dp)
                        .then(if (startOpen) Modifier.d98Sunken(D98Face, deep = true) else Modifier.d98Raised())
                        .tap { startOpen = !startOpen }.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Canvas(Modifier.size(16.dp)) {
                        val r = size.width / 16f
                        drawCircle(c.c, radius = 6 * r)
                        drawCircle(c.cDark, radius = 6 * r, style = Stroke(r))
                        drawCircle(Color.White, radius = 2 * r)
                    }
                    Text("Chime", style = D98Text.copy(fontWeight = FontWeight.Bold))
                }
                Box(
                    Modifier.weight(1f).height(26.dp).d98Sunken(D98Face, deep = false)
                        .tap { if (q != null) a.openQuest(q) else a.openLog() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        if (q != null) d98Exe(q.text) else "To-Do List",
                        style = D98Text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier.height(26.dp).d98Sunken(D98Face, deep = false).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(Time.time(s.now), style = D98Text, maxLines = 1)
                }
            }
        }
    }

    // the Chime menu, rising out of the taskbar button
    if (startOpen) {
        Box(Modifier.fillMaxSize().tap { startOpen = false })
        Row(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 4.dp, bottom = 33.dp)
                .width(220.dp).height(IntrinsicSize.Min).d98Raised().padding(3.dp),
        ) {
            // the tall banner down the left side
            Box(
                Modifier.width(24.dp).fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1084D0), Color(0xFF00087A)))),
                contentAlignment = Alignment.Center,
            ) {
                // reads bottom-to-top, like the old start-menu banners
                Text(
                    "Chime 98",
                    style = D98Text.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.requiredWidth(90.dp).graphicsLayer { rotationZ = -90f },
                )
            }
            Column(Modifier.weight(1f)) {
                D98MenuItem("Talk to ${c.title}") { startOpen = false; voice.toggle() }
                D98MenuItem("To-Do List") { startOpen = false; a.openLog() }
                if (q != null) D98MenuItem("Open ${d98Exe(q.text)}") { startOpen = false; a.openQuest(q) }
                Box(Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = 2.dp).height(2.dp).d98Sunken(D98Face, deep = false))
                D98MenuItem("Settings", bold = true) { startOpen = false; a.openOptions() }
            }
        }
    }
    }
}

@Composable
private fun D98MenuItem(label: String, bold: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(32.dp).tap(onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            label,
            style = D98Text.copy(fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun d98DueLine(q: Quest, now: Long) = buildAnnotatedString {
    val due = q.due
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    when {
        due == null -> append("It has no deadline, so it may never respond.")
        q.shelf(now) == Shelf.OVERDUE -> {
            append("It was due ")
            withStyle(bold) { append(if (q.allDay) Time.dayName(Time.date(due)).lowercase() else q.shortDue()) }
            append(" and still has not responded.")
        }
        q.allDay -> {
            append("It must be answered by end of ")
            withStyle(bold) { append(Time.dayName(Time.date(due)).lowercase()) }
            append(".")
        }
        q.kind == "by" -> {
            append("It must be answered by ")
            withStyle(bold) { append(q.shortDue()) }
            append(".")
        }
        else -> {
            append("It is scheduled for ")
            withStyle(bold) { append(q.shortDue()) }
            append(".")
        }
    }
}

@Composable
private fun D98Window(
    title: String,
    active: Boolean,
    onClose: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().d98Raised().padding(3.dp)) {
        val bar: Brush = if (active) Brush.horizontalGradient(listOf(Color(0xFF00087A), Color(0xFF1084D0)))
        else SolidColor(D98Shadow)
        Row(
            Modifier.fillMaxWidth().height(22.dp).background(bar).padding(start = 6.dp, end = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = D98Text.copy(color = if (active) Color.White else D98Face, fontWeight = FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onClose != null) {
                Canvas(Modifier.size(16.dp, 14.dp).d98Raised().tap(onClose)) {
                    val p = 1.dp.toPx()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = 3.dp.toPx()
                    drawLine(Color.Black, Offset(cx - r, cy - r), Offset(cx + r, cy + r), strokeWidth = 1.6f * p)
                    drawLine(Color.Black, Offset(cx + r, cy - r), Offset(cx - r, cy + r), strokeWidth = 1.6f * p)
                }
            }
        }
        content()
    }
}

@Composable
private fun D98Button(label: String, minWidth: Dp, focused: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .widthIn(min = minWidth)
            .d98Raised()
            .then(if (focused) Modifier.d98Dotted(Color.Black, 4.dp) else Modifier)
            .tap(onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = D98Text, maxLines = 1)
    }
}

@Composable
private fun D98Icon(label: String, selected: Boolean, trash: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(76.dp).tap(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (trash) Canvas(Modifier.size(30.dp, 32.dp)) { d98Trash() }
        else Canvas(Modifier.size(34.dp, 30.dp)) { d98Folder() }
        val labelMod = if (selected) Modifier.background(Color(0xFF0A2A8A)).d98Dotted(Color(0xFFFFFF9A), 0.dp).padding(horizontal = 3.dp)
        else Modifier.padding(horizontal = 3.dp)
        Text(
            label,
            style = D98Text.copy(color = Color.White, textAlign = TextAlign.Center),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = labelMod,
        )
    }
}

private fun DrawScope.d98Folder() {
    val sx = size.width / 34f
    val sy = size.height / 30f
    val edge = Color(0xFF7A5A10)
    val back = d98Poly(sx, sy, 1f, 5f, 13f, 5f, 16f, 8f, 33f, 8f, 33f, 29f, 1f, 29f)
    drawPath(back, Color(0xFFF5D36A))
    drawPath(back, edge, style = Stroke(sx))
    val front = d98Poly(sx, sy, 1f, 10f, 33f, 10f, 33f, 29f, 1f, 29f)
    drawPath(front, Color(0xFFFFE48A))
    drawPath(front, edge, style = Stroke(sx))
}

private fun DrawScope.d98Trash() {
    val sx = size.width / 30f
    val sy = size.height / 32f
    val edge = Color(0xFF404040)
    val can = d98Poly(sx, sy, 5f, 6f, 25f, 6f, 23f, 31f, 7f, 31f)
    drawPath(can, Color(0xFFC8C8C8))
    drawPath(can, edge, style = Stroke(sx))
    val lid = d98Poly(sx, sy, 3f, 4f, 27f, 4f, 27f, 7f, 3f, 7f)
    drawPath(lid, Color(0xFFA0A0A0))
    drawPath(lid, edge, style = Stroke(sx))
    for (x in listOf(10f, 15f, 20f)) {
        drawLine(Color(0xFF707070), Offset(x * sx, 11f * sy), Offset(x * sx, 27f * sy), strokeWidth = sx)
    }
}

private fun DrawScope.d98Warning() {
    val s = size.width / 34f
    val tri = d98Poly(s, s, 17f, 2f, 33f, 31f, 1f, 31f)
    drawPath(tri, Color(0xFFFFD400))
    drawPath(tri, Color.Black, style = Stroke(s))
    drawRect(Color.Black, Offset(15.5f * s, 11f * s), Size(3f * s, 11f * s))
    drawRect(Color.Black, Offset(15.5f * s, 24.5f * s), Size(3f * s, 3f * s))
}

private fun DrawScope.d98Info() {
    val s = size.width / 34f
    drawCircle(Color.White, radius = 15f * s)
    drawCircle(Color(0xFF0A2A8A), radius = 15f * s, style = Stroke(1.5f * s))
    drawRect(Color(0xFF0A2A8A), Offset(15.5f * s, 8f * s), Size(3f * s, 3f * s))
    drawRect(Color(0xFF0A2A8A), Offset(15.5f * s, 13f * s), Size(3f * s, 12f * s))
}

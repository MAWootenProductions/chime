package com.mwooten.chime.worlds

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val EmailInk = Color(0xFF1D2229)
private val EmailSub = Color(0xFF4B5260)
private val EmailMuted = Color(0xFF6B7280)
private val EmailRule = Color(0xFFEEF0F3)
private val EmailHeadRule = Color(0xFFE7E9EE)
private val EmailChipLine = Color(0xFFD6D9DF)
private val EmailBlue = Color(0xFF1A5FD0)
private val EmailUnread = Color(0xFFF7F9FC)
private val EmailGreen = Color(0xFF2F9E5B)
private val EmailAmber = Color(0xFFE09A2A)
private val EmailRed = Color(0xFFC2382A)
private val EmailFont = Fonts.ibmPlexSans

private val EmailInitials = listOf(
    Color(0xFFDFE7F2) to Color(0xFF3A4A62),
    Color(0xFFEFE6D8) to Color(0xFF6A4A1A),
    Color(0xFFE3F0E6) to Color(0xFF2A6A3A),
)

private val EmailTabs = listOf("Today", "This week", "Someday", "Archive")

@Composable
fun EmailWorld(s: WorldState, a: WorldActions) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var touched by rememberSaveable { mutableStateOf(false) }
    val focus = s.focus
    val weekAgo = s.now - 7L * 24 * 60 * 60 * 1000
    val doneWeek = s.done.count { (it.doneAt ?: 0L) >= weekAgo }
    val list: List<Quest> = when (tab) {
        0 -> s.today
        1 -> s.week + s.later
        2 -> s.someday
        else -> s.done
    }
    val showFocusMail = (focus != null && list.any { it.id == focus.id }) || (focus == null && tab == 0)
    val rows = list.filter { it.id != focus?.id }.take(40)

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            // header
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Inbox", fontFamily = EmailFont, fontWeight = FontWeight.W700, fontSize = 28.sp, color = EmailInk)
                    Text(
                        "${s.open.size} to do · $doneWeek done this week",
                        fontFamily = EmailFont, fontSize = 13.sp, color = EmailMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 12.dp).tap { a.openLog() },
                    )
                    EmailIcon({ a.openLog() }) {
                        val k = size.width / 18f
                        for (r in 0..2) {
                            val y = (5 + r * 4) * k
                            drawLine(EmailInk, Offset(3 * k, y), Offset(15 * k, y), 1.6f * k, cap = StrokeCap.Round)
                        }
                    }
                    EmailIcon({ a.openOptions() }) {
                        val k = size.width / 18f
                        val c = Offset(9 * k, 9 * k)
                        for (i in 0 until 8) {
                            val ang = i * Math.PI / 4
                            val dx = cos(ang).toFloat()
                            val dy = sin(ang).toFloat()
                            drawLine(EmailInk, Offset(c.x + dx * 5f * k, c.y + dy * 5f * k), Offset(c.x + dx * 8f * k, c.y + dy * 8f * k), 2f * k, cap = StrokeCap.Round)
                        }
                        drawCircle(EmailInk, 5f * k, c, style = Stroke(1.6f * k))
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EmailTabs.forEachIndexed { i, label ->
                        val on = tab == i
                        Text(
                            label, fontFamily = EmailFont, fontWeight = FontWeight.W600, fontSize = 13.sp,
                            color = if (on) Color.White else EmailInk,
                            modifier = Modifier
                                .then(
                                    if (on) Modifier.background(EmailInk, RoundedCornerShape(14.dp))
                                    else Modifier.border(1.dp, EmailChipLine, RoundedCornerShape(14.dp))
                                )
                                .tap { tab = i }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(EmailHeadRule))

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                // the companion's email, unread
                if (showFocusMail) {
                    if (focus != null) {
                        key(focus.id) {
                            EmailSwipe(focus, a, hint = false, onTouched = { touched = true }) {
                                EmailCompanionMail(s, focus)
                            }
                        }
                    } else {
                        EmailCompanionMail(s, null)
                    }
                }
                rows.forEachIndexed { i, q ->
                    key(q.id) {
                        if (q.done) {
                            EmailRowBody(q, i, s, Modifier.tap { a.openQuest(q) })
                        } else {
                            EmailSwipe(q, a, hint = i == 0 && !touched, onTouched = { touched = true }) {
                                EmailRowBody(q, i, s, Modifier)
                            }
                        }
                    }
                }
                if (rows.isEmpty() && !showFocusMail) {
                    Text(
                        if (tab == 3) "Nothing archived yet." else "Inbox zero. Enjoy it.",
                        fontFamily = EmailFont, fontSize = 14.sp, color = EmailMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 28.dp),
                    )
                }
                Box(Modifier.height(if (composing) 16.dp else 110.dp))
            }

            // compose = spill
            if (composing) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "New message to ${s.companion.title}", fontFamily = EmailFont, fontWeight = FontWeight.W700,
                            fontSize = 15.sp, color = EmailInk, modifier = Modifier.weight(1f),
                        )
                        Text(
                            "Discard", fontFamily = EmailFont, fontWeight = FontWeight.W600, fontSize = 13.sp,
                            color = EmailMuted, modifier = Modifier.tap { composing = false }.padding(6.dp),
                        )
                    }
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(EmailRule))
                    SpillInput(
                        onSpill = { t -> a.spill(t); composing = false },
                        textStyle = TextStyle(fontFamily = EmailFont, fontSize = 16.sp, color = EmailInk),
                        placeholder = "Subject: what you need to do",
                        placeholderColor = EmailMuted,
                        cursor = EmailBlue,
                        modifier = Modifier.fillMaxWidth(),
                        trailing = { submit ->
                            Text(
                                "Send", fontFamily = EmailFont, fontWeight = FontWeight.W700, fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(EmailInk, RoundedCornerShape(12.dp))
                                    .tap { submit() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        },
                    )
                }
            }
        }

        if (!composing) {
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 24.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .background(EmailInk, RoundedCornerShape(16.dp))
                    .tap { composing = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(18.dp)) {
                    val k = size.width / 18f
                    val p = Path().apply {
                        moveTo(3 * k, 15 * k); lineTo(4 * k, 11 * k); lineTo(12 * k, 3 * k); lineTo(15 * k, 6 * k)
                        lineTo(7 * k, 14 * k); close()
                        moveTo(11 * k, 4 * k); lineTo(14 * k, 7 * k)
                    }
                    drawPath(p, Color.White, style = Stroke(1.6f * k, join = StrokeJoin.Round))
                }
                Text(
                    "Spill", fontFamily = EmailFont, fontWeight = FontWeight.W700, fontSize = 15.sp, color = Color.White,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun EmailIcon(onClick: () -> Unit, draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    Box(Modifier.size(36.dp).tap(onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(18.dp)) { draw() }
    }
}

/** A row you can drag left to reveal Done and Snooze, like a mail app. */
@Composable
private fun EmailSwipe(
    q: Quest,
    a: WorldActions,
    hint: Boolean,
    onTouched: () -> Unit,
    content: @Composable () -> Unit,
) {
    val maxPx = with(LocalDensity.current) { 128.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    val hintAnim = remember { Animatable(0f) }
    LaunchedEffect(hint) {
        if (hint) {
            repeat(2) {
                delay(1500)
                hintAnim.animateTo(-maxPx, tween(1000, easing = FastOutSlowInEasing))
                delay(1500)
                hintAnim.animateTo(0f, tween(1000, easing = FastOutSlowInEasing))
            }
        } else {
            hintAnim.snapTo(0f)
        }
    }
    Box(Modifier.fillMaxWidth()) {
        // actions behind the row
        Row(
            Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(0.5f to EmailGreen, 0.5f to EmailAmber))
        ) {
            Box(Modifier.weight(1f))
            EmailAction("Done", EmailGreen) { drag = 0f; a.done(q) }
            EmailAction("Snooze", EmailAmber) { drag = 0f; a.snooze(q, 30) }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset((drag + if (hint) hintAnim.value else 0f).roundToInt(), 0) }
                .background(Color.White)
                .pointerInput(q.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { onTouched() },
                        onDragEnd = { drag = if (drag < -maxPx / 2f) -maxPx else 0f },
                        onDragCancel = { drag = if (drag < -maxPx / 2f) -maxPx else 0f },
                    ) { change, amount ->
                        change.consume()
                        drag = (drag + amount).coerceIn(-maxPx, 0f)
                    }
                }
                .tap {
                    val wasOpen = drag != 0f
                    drag = 0f
                    if (!wasOpen) a.openQuest(q)
                }
        ) { Column(Modifier.fillMaxWidth()) { content() } }
    }
}

@Composable
private fun EmailAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.width(64.dp).fillMaxHeight().background(color).tap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = EmailFont, fontWeight = FontWeight.W700, fontSize = 13.sp, color = Color.White)
    }
}

@Composable
private fun EmailCompanionMail(s: WorldState, q: Quest?) {
    val c = s.companion
    Row(
        Modifier
            .fillMaxWidth()
            .background(EmailUnread)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Canvas(Modifier.size(38.dp)) {
            drawCircle(
                Brush.radialGradient(
                    0f to c.cLight, 0.6f to c.c, 1f to c.cDark,
                    center = Offset(size.width * 0.4f, size.height * 0.35f),
                    radius = size.width * 0.75f,
                ),
                radius = size.width / 2f,
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.title, fontFamily = EmailFont, fontWeight = FontWeight.W700, fontSize = 15.sp, color = EmailInk,
                    modifier = Modifier.weight(1f),
                )
                Text(Time.time(s.now), fontFamily = EmailFont, fontWeight = FontWeight.W700, fontSize = 12.sp, color = EmailBlue)
            }
            Text(
                if (q != null) "Re: ${q.text} (${q.shortDue()})" else "Nothing in your inbox",
                fontFamily = EmailFont, fontWeight = FontWeight.W700, fontSize = 15.sp, color = EmailInk,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                s.line + " — " + c.title.take(1),
                fontFamily = EmailFont, fontSize = 14.sp, lineHeight = 20.sp, color = EmailSub,
                maxLines = 5, overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(EmailRule))
}

@Composable
private fun EmailRowBody(q: Quest, i: Int, s: WorldState, modifier: Modifier) {
    val (bg, ink) = EmailInitials[i % EmailInitials.size]
    val late = q.shelf(s.now) == Shelf.OVERDUE
    val ageDays = ((s.now - q.created) / 86_400_000L).toInt()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Box(Modifier.size(38.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                Text(emailInitials(q.text), fontFamily = EmailFont, fontWeight = FontWeight.W700, fontSize = 14.sp, color = ink)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        q.text, fontFamily = EmailFont,
                        fontWeight = if (q.done) FontWeight.W500 else if (i == 0) FontWeight.W700 else FontWeight.W600,
                        fontSize = 15.sp, color = if (q.done) EmailMuted else if (i == 0) EmailInk else EmailSub,
                        textDecoration = if (q.done) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Text(
                        emailWhen(q, s.now, late), fontFamily = EmailFont, fontSize = 12.sp,
                        fontWeight = if (late) FontWeight.W600 else FontWeight.W400,
                        color = if (late) EmailRed else EmailMuted, maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    emailSnippet(q, s.now), fontFamily = EmailFont, fontSize = 14.sp, color = EmailMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (!q.done && q.due == null && ageDays >= 2) Text(
                    "Spilled $ageDays days ago. Follow up?",
                    fontFamily = EmailFont, fontWeight = FontWeight.W600, fontSize = 12.sp, color = Color(0xFF8A5A00),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(Color(0xFFFFF3D6), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(EmailRule))
    }
}

private fun emailInitials(text: String): String {
    val letters = text.filter { it.isLetterOrDigit() }
    if (letters.isEmpty()) return "?"
    return letters.take(1).uppercase() + letters.drop(1).take(1).lowercase()
}

private fun emailWhen(q: Quest, now: Long, late: Boolean): String {
    if (q.done) return q.doneAt?.let { "done " + Time.dayName(Time.date(it)).take(3) } ?: "done"
    val d = q.due ?: return "someday"
    if (late) return "late"
    val sameDay = Time.date(d) == Time.date(now)
    if (q.allDay) return if (sameDay) "today" else Time.dayName(Time.date(d)).take(3)
    if (sameDay) return (if (q.kind == "by") "due " else "at ") + Time.time(d)
    return Time.dayName(Time.date(d)).take(3) + " " + Time.time(d)
}

private fun emailSnippet(q: Quest, now: Long): String {
    val day = Time.dayName(Time.date(q.created))
    val since = if (Time.date(q.created) == Time.date(now)) "today" else day.take(3)
    return "Spilled $since: \"${q.text.lowercase()}\""
}

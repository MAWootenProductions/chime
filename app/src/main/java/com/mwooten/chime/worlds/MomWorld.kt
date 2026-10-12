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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap

private val MomBg = Color(0xFFFBFAF7)
private val MomLine = Color(0xFFE6E3DC)
private val MomTeal = Color(0xFF1F6F6A)
private val MomGrayBubble = Color(0xFFECEBE6)
private val MomInk = Color(0xFF1B1B1B)
private val MomSub = Color(0xFF7A766D)
private val MomStampColor = Color(0xFF9A958B)
private val MomIcon = Color(0xFF5A564E)

@Composable
fun MomWorld(s: WorldState, a: WorldActions) {
    val screenW = LocalConfiguration.current.screenWidthDp
    val mine = (screenW * 0.74f).dp
    val hers = (screenW * 0.78f).dp
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue, s.quests.size, s.focus?.id) { scroll.scrollTo(scroll.maxValue) }

    // your past spills, oldest first
    val spills = s.quests.sortedBy { it.created }.takeLast(8)
    val focus = s.focus

    Box(Modifier.fillMaxSize().background(MomBg)) {
    Column(Modifier.fillMaxSize().imePadding()) {
        // header
        Row(
            Modifier
                .fillMaxWidth()
                .background(MomBg.copy(alpha = 0.96f))
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(28.dp).tap { a.openLog() }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(width = 12.dp, height = 20.dp)) {
                    val k = size.width / 12f
                    val p = Path().apply { moveTo(10 * k, 2 * k); lineTo(2 * k, 10 * k); lineTo(10 * k, 18 * k) }
                    drawPath(p, MomTeal, style = Stroke(2.4f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            MomRoses(s, Modifier.size(40.dp).tap { a.openOptions() })
            Column(Modifier.weight(1f).tap { a.openOptions() }) {
                Text("Mom", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MomInk, fontFamily = FontFamily.SansSerif)
                Text(
                    "${s.companion.title} is texting as Mom", fontSize = 12.sp, color = MomSub,
                    fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            // contact info = options
            Box(Modifier.size(32.dp).tap { a.openOptions() }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    val r = size.width / 2f
                    drawCircle(MomTeal, r - 1.dp.toPx(), center, style = Stroke(1.8.dp.toPx()))
                    drawCircle(MomTeal, 1.4.dp.toPx(), Offset(r, r - 4.5.dp.toPx()))
                    drawLine(MomTeal, Offset(r, r - 1.5.dp.toPx()), Offset(r, r + 5.dp.toPx()), 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MomLine))

        // thread
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            var lastDay: java.time.LocalDate? = null
            spills.forEach { q ->
                val d = Time.date(q.created)
                MomStamp(
                    if (d != lastDay) Time.dayName(d).let { if (it.length > 9) it.take(3) else it } + " " + Time.time(q.created)
                    else null
                )
                lastDay = d
                MomBubble(q.text, mine = true, maxW = mine, faded = q.done) { a.openQuest(q) }
                MomBubble(momAck(q), mine = false, maxW = hers) { a.openQuest(q) }
            }

            // what Mom is saying right now
            MomStampText(Time.dayName(Time.date(s.now)) + " " + Time.time(s.now), top = 10.dp)
            if (focus != null) {
                MomBubble(momGreeting(s.now), mine = false, maxW = hers)
                MomBubble(s.line, mine = false, maxW = hers) { a.openQuest(focus) }
                val left = focus.minutesLeft(s.now)
                if (left != null && left > 0) MomBubble(momLeft(left), mine = false, maxW = hers)
                MomBubble("Text me \"done\" when it's done and I'll stop asking. Love, Mom", mine = false, maxW = hers)

                // reply chips: the snooze/done of this world
                Row(
                    Modifier.align(Alignment.End).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MomChip("done", MomTeal, MomTeal) { a.done(focus) }
                    MomChip("in a bit mom", Color(0xFFC9C5BB), MomIcon) { a.snooze(focus, 30) }
                    MomChip("tonight", Color(0xFFC9C5BB), MomIcon) { a.snooze(focus, 240) }
                }
            } else {
                MomBubble(s.line, mine = false, maxW = hers)
                MomBubble("Love, Mom", mine = false, maxW = hers)
            }
            MomTyping(Modifier.padding(top = 4.dp))
        }

        // composer = spill
        Box(Modifier.fillMaxWidth().height(1.dp).background(MomLine))
        SpillInput(
            onSpill = a::spill,
            textStyle = TextStyle(fontSize = 16.sp, color = MomInk, fontFamily = FontFamily.SansSerif),
            placeholder = "Tell Mom what you need to do",
            placeholderColor = MomStampColor,
            cursor = MomTeal,
            modifier = Modifier
                .fillMaxWidth()
                .background(MomBg)
                .navigationBarsPadding()
                .padding(start = 56.dp, end = 12.dp, top = 10.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFD9D5CC), RoundedCornerShape(20.dp))
                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            trailing = { submit ->
                Box(
                    Modifier.padding(start = 8.dp).size(32.dp).background(MomTeal, CircleShape).tap { submit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(16.dp)) {
                        val k = size.width / 16f
                        val p = Path().apply {
                            moveTo(8 * k, 13 * k); lineTo(8 * k, 3 * k)
                            moveTo(3.5f * k, 7.5f * k); lineTo(8 * k, 3 * k); lineTo(12.5f * k, 7.5f * k)
                        }
                        drawPath(p, Color.White, style = Stroke(2 * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            },
        )
    }
    // quest log button, laid over the left of the composer row
    Box(Modifier.fillMaxSize().imePadding().navigationBarsPadding(), contentAlignment = Alignment.BottomStart) {
        Box(
            Modifier
                .padding(start = 12.dp, bottom = 16.dp)
                .size(36.dp)
                .background(MomGrayBubble, CircleShape)
                .tap { a.openLog() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(16.dp)) {
                val k = size.width / 16f
                val w = 1.8f * k
                drawLine(MomIcon, Offset(3 * k, 4 * k), Offset(13 * k, 4 * k), w, cap = StrokeCap.Round)
                drawLine(MomIcon, Offset(3 * k, 8 * k), Offset(13 * k, 8 * k), w, cap = StrokeCap.Round)
                drawLine(MomIcon, Offset(3 * k, 12 * k), Offset(9 * k, 12 * k), w, cap = StrokeCap.Round)
            }
        }
    }
    }
}

@Composable
private fun MomStamp(text: String?) {
    if (text != null) MomStampText(text, top = 4.dp)
}

@Composable
private fun MomStampText(text: String, top: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxWidth().padding(top = top, bottom = 2.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 11.sp, color = MomStampColor, fontFamily = FontFamily.SansSerif)
    }
}

@Composable
private fun MomBubble(
    text: String,
    mine: Boolean,
    maxW: androidx.compose.ui.unit.Dp,
    faded: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = if (mine) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            text,
            fontSize = 16.sp, lineHeight = 21.sp, fontFamily = FontFamily.SansSerif,
            color = if (mine) Color.White else MomInk,
            modifier = Modifier
                .widthIn(max = maxW)
                .graphicsLayer { alpha = if (faded) 0.6f else 1f }
                .background(if (mine) MomTeal else MomGrayBubble, shape)
                .then(if (onClick != null) Modifier.tap(onClick) else Modifier)
                .padding(horizontal = 13.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun MomChip(label: String, border: Color, color: Color, onClick: () -> Unit) {
    Text(
        label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = color, fontFamily = FontFamily.SansSerif,
        modifier = Modifier
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .tap(onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun MomTyping(modifier: Modifier) {
    val clock = rememberClock()
    Row(
        modifier.background(MomGrayBubble, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in 0..2) {
            Box(
                Modifier
                    .graphicsLayer {
                        val p = momDot(clock.value - i * 0.15f)
                        translationY = -3.dp.toPx() * p
                        alpha = 0.4f + 0.6f * p
                    }
                    .size(7.dp)
                    .background(Color(0xFF8A867C), CircleShape)
            )
        }
    }
}

/** dot keyframes: rest, up at 30%, back down by 60% of a 1.2s loop. */
private fun momDot(t: Float): Float {
    val p = (((t % 1.2f) + 1.2f) % 1.2f) / 1.2f
    return when {
        p < 0.3f -> p / 0.3f
        p < 0.6f -> 1f - (p - 0.3f) / 0.3f
        else -> 0f
    }
}

@Composable
private fun MomRoses(s: WorldState, modifier: Modifier) {
    // Mom's contact photo: her garden flowers (in the companion's colors), slightly blurry
    Canvas(modifier.clip(CircleShape)) {
        val w = size.width
        drawRect(Color(0xFF4F7A3E))
        drawCircle(s.companion.c, w * 0.18f, Offset(w * 0.35f, w * 0.40f))
        drawCircle(s.companion.cLight, w * 0.07f, Offset(w * 0.35f, w * 0.40f))
        drawCircle(s.companion.cLight, w * 0.16f, Offset(w * 0.65f, w * 0.60f))
        drawCircle(s.companion.c, w * 0.06f, Offset(w * 0.65f, w * 0.60f))
        drawCircle(s.companion.cDark, w * 0.12f, Offset(w * 0.30f, w * 0.75f))
    }
}

private fun momAck(q: Quest): String {
    val d = q.due
    if (q.done) return "So proud of you. Love, Mom"
    if (d == null) return "Ok got it. Whenever you can. Love, Mom"
    val day = Time.dayName(Time.date(d))
    val whenText = if (q.allDay) day else day + " " + (if (q.kind == "by") "by " else "at ") + Time.time(d)
    return "Ok got it. $whenText. Love, Mom"
}

private fun momGreeting(now: Long): String {
    val h = Time.ldt(now).hour
    return when {
        h < 12 -> "Good morning sweetie"
        h < 17 -> "Hi honey, just checking in"
        else -> "Evening sweetie. Hope your day was ok"
    }
}

private fun momLeft(min: Long): String = when {
    min < 60 -> "$min minutes left honey"
    min < 60 * 24 -> "${(min + 30) / 60} hours left honey"
    else -> "${(min + 720) / (60 * 24)} days left. No rush but don't forget"
}

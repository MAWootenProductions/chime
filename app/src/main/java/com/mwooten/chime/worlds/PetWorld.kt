package com.mwooten.chime.worlds

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
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

private val PetNavy = Color(0xFF1A2A7A)
private val PetInk = Color(0xFF1A1A4A)
private val PetYellow = Color(0xFFFFD23A)
private val PetRed = Color(0xFFD0021B)
private val PetPale = Color(0xFFEEF3FF)
private val PetSans = FontFamily.SansSerif

private fun Modifier.petCard(): Modifier = drawBehind {
    val o = 4.dp.toPx()
    drawRect(PetNavy, Offset(o, o), size)
}.background(Color.White).border(3.dp, PetNavy)

private fun petSymbol(q: Quest): String = when {
    q.due == null -> "☆"
    q.allDay -> "♨"
    q.kind == "by" -> "✉"
    else -> "☎"
}

@Composable
fun PetWorld(s: WorldState, a: WorldActions) {
    val f = s.focus
    var spilling by remember { mutableStateOf(false) }
    val name = s.companion.title
    val fed = s.done.size
    val total = s.quests.size
    val glow = (if (total == 0) 100 else fed * 100 / total) - s.overdueCount * 10
    val glowPct = glow.coerceIn(0, 100)
    val (hunger, hungerColor) = when {
        s.overdueCount > 0 -> "Very Hungry" to PetRed
        s.today.isNotEmpty() -> "Peckish" to Color(0xFFC87A00)
        s.open.isNotEmpty() -> "Satisfied" to Color(0xFF2A7A2A)
        else -> "Stuffed" to Color(0xFF2A7A2A)
    }
    val mood = when {
        s.overdueCount > 0 -> "Droopy"
        s.today.isNotEmpty() -> "Hopeful"
        else -> "Glowing"
    }
    val since = (s.quests.minOfOrNull { it.created } ?: s.now).let {
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()).format(Time.date(it))
    }
    val bowlIsToday = s.today.isNotEmpty()
    val bowl = if (bowlIsToday) s.today.take(4) else s.open.take(3)
    val ticker = if (f != null) {
        "★ LIMITED EVENT: ${f.text} ends ${f.dueText().replace(" · ", " ")}! ★ Finish it to earn 500 CP and a Rare Envelope! ★"
    } else {
        "★ ${name.uppercase()} IS WAITING FOR A NEW QUEST! ★ Spill one to start a new event! ★"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFFE98A))
            .drawBehind {
                val step = 16.dp.toPx()
                val r = 2.dp.toPx()
                var y = step / 2
                while (y < size.height) {
                    var x = step / 2
                    while (x < size.width) { drawCircle(PetYellow, r, Offset(x, y)); x += step }
                    y += step
                }
            }
            .imePadding()
    ) {
        // header
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF3A6AE0), Color(0xFF2A4AB0))))) {
            Row(
                Modifier.statusBarsPadding().fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                PetOutlined("Chime Pets!", 34.sp, Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.tap { a.openOptions() }) {
                    Text("spilled_milk_98", fontFamily = PetSans, fontSize = 11.sp, color = Color.White, lineHeight = 15.sp)
                    Text(String.format(Locale.US, "%,d CP", fed * 50), fontFamily = PetSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PetYellow, lineHeight = 15.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(4.dp).background(PetNavy))
        }
        // news ticker
        PetTicker(ticker)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 10.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)
        ) {
            // pet card
            Column(Modifier.fillMaxWidth().petCard()) {
                Text(
                    "$name the Lumimoth · Level ${1 + fed / 3}",
                    fontFamily = PetSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PetInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(lerp(s.companion.c, Color.White, 0.35f))
                        .drawBehind { drawRect(PetNavy, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx())) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .size(130.dp)
                            .background(
                                Brush.radialGradient(listOf(s.companion.cLight, PetPale))
                            )
                            .border(2.dp, lerp(s.companion.c, Color.White, 0.35f))
                            .tap { f?.let { a.openQuest(it) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        PetMoth(s, hungry = s.overdueCount > 0, modifier = Modifier.size(110.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PetStat("Hunger:", hunger, hungerColor, bold = true)
                        PetStat("Mood:", mood, PetInk)
                        PetStat("Glow:", "$glowPct%", PetInk)
                        Box(Modifier.fillMaxWidth().height(10.dp).border(2.dp, PetNavy).background(PetPale).padding(2.dp)) {
                            Box(Modifier.fillMaxWidth(glowPct / 100f).fillMaxHeight().background(Color(0xFF6AB0FF)))
                        }
                        PetStat("Fed by:", "finishing quests", PetInk)
                        PetStat("Owner since:", since, PetInk)
                    }
                }
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$name says: ") }
                        append("“${s.line}”")
                    },
                    fontFamily = PetSans, fontSize = 13.sp, lineHeight = 19.sp, color = PetInk,
                    modifier = Modifier
                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFFFF7CC))
                        .drawBehind {
                            val p = 2.dp.toPx()
                            drawRect(
                                Color(0xFFE0B020), Offset(p / 2, p / 2), Size(size.width - p, size.height - p),
                                style = Stroke(width = p, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                            )
                        }
                        .tap { f?.let { a.openQuest(it) } }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // feed bowl = today's quests
            Column(Modifier.fillMaxWidth().petCard()) {
                Text(
                    "Feed $name (${if (bowlIsToday) "today's quests" else "up next"})",
                    fontFamily = PetSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PetInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PetYellow)
                        .drawBehind { drawRect(PetNavy, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx())) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (bowl.isEmpty()) {
                        Text("The bowl is empty. Spill a quest to cook something up.", fontFamily = PetSans, fontSize = 12.sp, color = PetInk)
                    }
                    bowl.forEachIndexed { i, q ->
                        val first = i == 0
                        val late = q.shelf(s.now) == Shelf.OVERDUE
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                buildAnnotatedString {
                                    append(petSymbol(q) + " ")
                                    if (first) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(q.text) } else append(q.text)
                                    withStyle(SpanStyle(color = if (first || late) PetRed else PetInk)) { append(" · " + q.shortDue()) }
                                },
                                fontFamily = PetSans, fontSize = 12.sp, lineHeight = 16.sp, color = PetInk,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).tap { a.openQuest(q) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (first) "Feed!" else "Feed",
                                fontFamily = PetSans, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                color = if (first) Color.White else PetInk,
                                modifier = Modifier
                                    .background(if (first) Color(0xFF3A6AE0) else PetPale)
                                    .border(2.dp, PetNavy)
                                    .tap { a.done(q) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (spilling || s.open.isEmpty()) {
                        SpillInput(
                            onSpill = { a.spill(it); spilling = false },
                            textStyle = TextStyle(fontFamily = PetSans, fontSize = 13.sp, color = PetInk),
                            placeholder = "new quest, e.g. water plants tonight",
                            placeholderColor = PetInk.copy(alpha = 0.4f),
                            cursor = PetInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .border(2.dp, PetNavy)
                                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            trailing = { submit ->
                                Text(
                                    "Add!", fontFamily = PetSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White,
                                    modifier = Modifier.padding(start = 6.dp).background(Color(0xFF3A6AE0)).border(2.dp, PetNavy).tap(submit).padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            },
                        )
                    }
                }
            }
        }

        // side links, like the old nav
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PetLink("Spill a quest") { spilling = !spilling }
            PetLink("Quest Shop") { a.openLog() }
            PetLink("Calendar") { a.openLog() }
            PetLink("My Account") { a.openOptions() }
        }
    }
}

@Composable
private fun PetLink(label: String, onClick: () -> Unit) {
    Text(
        label, fontFamily = PetSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PetNavy,
        textDecoration = TextDecoration.Underline, maxLines = 1, modifier = Modifier.tap(onClick).padding(vertical = 4.dp),
    )
}

@Composable
private fun PetStat(label: String, value: String, color: Color, bold: Boolean = false) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$label ") }
            withStyle(SpanStyle(color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)) { append(value) }
        },
        fontFamily = PetSans, fontSize = 12.sp, color = PetInk, maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
}

/** Chewy title with a dark outline and a hard drop shadow, like a 2000s site logo. */
@Composable
private fun PetOutlined(text: String, size: TextUnit, modifier: Modifier) {
    val style = TextStyle(fontFamily = Fonts.chewy, fontSize = size, lineHeight = size)
    Box(modifier) {
        Text(text, style = style, color = PetInk, maxLines = 1, modifier = Modifier.offset(3.dp, 3.dp))
        val d = 1.5.dp
        for ((dx, dy) in listOf(-d to 0.dp, d to 0.dp, 0.dp to -d, 0.dp to d, -d to -d, d to d, -d to d, d to -d)) {
            Text(text, style = style, color = PetInk, maxLines = 1, modifier = Modifier.offset(dx, dy))
        }
        Text(text, style = style, color = PetYellow, maxLines = 1)
    }
}

@Composable
private fun PetTicker(text: String) {
    val inf = rememberInfiniteTransition(label = "ticker")
    val p by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "marquee")
    var textW by remember { mutableIntStateOf(0) }
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(22.dp).background(PetInk).clipToBounds(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val boxW = with(LocalDensity.current) { maxWidth.toPx() }
        Text(
            text, fontFamily = PetSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PetYellow,
            maxLines = 1, softWrap = false,
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .onSizeChanged { textW = it.width }
                .graphicsLayer { translationX = boxW - p * (boxW + textW) },
        )
    }
}

@Composable
private fun PetMoth(s: WorldState, hungry: Boolean, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "moth")
    val droop by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "droop")
    val clock = rememberClock()
    val c = s.companion.c
    val wing = lerp(s.companion.cLight, c, 0.45f)
    val body = lerp(c, s.companion.cLight, 0.15f)
    Canvas(modifier) {
        val k = size.width / 110f
        translate(top = 3f * k * droop) {
            rotate(-3f * droop, pivot = Offset(55f * k, 66f * k)) {
                petMothBody(k, wing, c, body, hungry, clock.value)
            }
        }
    }
}

private fun DrawScope.petMothBody(k: Float, wing: Color, spot: Color, body: Color, hungry: Boolean, t: Float) {
    fun o(x: Float, y: Float) = Offset(x * k, y * k)
    val line = Stroke(width = 2.5f * k)
    val thin = Stroke(width = 2f * k)
    // wings
    rotate(-20f, pivot = o(32f, 50f)) {
        drawOval(wing, o(6f, 30f), Size(52f * k, 40f * k))
        drawOval(PetNavy, o(6f, 30f), Size(52f * k, 40f * k), style = line)
    }
    rotate(20f, pivot = o(78f, 50f)) {
        drawOval(wing, o(52f, 30f), Size(52f * k, 40f * k))
        drawOval(PetNavy, o(52f, 30f), Size(52f * k, 40f * k), style = line)
    }
    drawCircle(spot, 6f * k, o(26f, 48f))
    drawCircle(spot, 6f * k, o(84f, 48f))
    // body
    drawOval(body, o(35f, 40f), Size(40f * k, 52f * k))
    drawOval(PetNavy, o(35f, 40f), Size(40f * k, 52f * k), style = line)
    // antennae with flickering glow tips
    val ant = Path().apply {
        moveTo(46f * k, 40f * k); cubicTo(40f * k, 26f * k, 34f * k, 22f * k, 30f * k, 22f * k)
        moveTo(64f * k, 40f * k); cubicTo(70f * k, 26f * k, 76f * k, 22f * k, 80f * k, 22f * k)
    }
    drawPath(ant, PetNavy, style = Stroke(width = 2.5f * k, cap = StrokeCap.Round))
    fun flick(phase: Float): Float {
        val x = ((t - phase) % 2.4f + 2.4f) % 2.4f / 2.4f
        return if (x in 0.45f..0.52f) 0.3f else 1f
    }
    drawCircle(Color(0xFFFFE98A), 4f * k, o(30f, 22f), alpha = flick(0f))
    drawCircle(PetNavy, 4f * k, o(30f, 22f), style = thin)
    drawCircle(Color(0xFFFFE98A), 4f * k, o(80f, 22f), alpha = flick(0.7f))
    drawCircle(PetNavy, 4f * k, o(80f, 22f), style = thin)
    // eyes
    drawOval(Color.White, o(43f, 53f), Size(10f * k, 14f * k))
    drawOval(PetNavy, o(43f, 53f), Size(10f * k, 14f * k), style = thin)
    drawOval(Color.White, o(57f, 53f), Size(10f * k, 14f * k))
    drawOval(PetNavy, o(57f, 53f), Size(10f * k, 14f * k), style = thin)
    drawCircle(PetInk, 2.5f * k, o(48f, 63f))
    drawCircle(PetInk, 2.5f * k, o(62f, 63f))
    // worried brows and a small mouth
    drawLine(PetNavy, o(43f, 53f), o(52f, 56f), 2f * k, StrokeCap.Round)
    drawLine(PetNavy, o(67f, 53f), o(58f, 56f), 2f * k, StrokeCap.Round)
    val mouth = Path().apply {
        if (hungry) { moveTo(50f * k, 76f * k); quadraticBezierTo(55f * k, 73f * k, 60f * k, 76f * k) }
        else { moveTo(50f * k, 75f * k); quadraticBezierTo(55f * k, 79f * k, 60f * k, 75f * k) }
    }
    drawPath(mouth, PetNavy, style = thin)
    if (hungry) {
        val tear = Path().apply { moveTo(66f * k, 70f * k); quadraticBezierTo(68f * k, 76f * k, 66f * k, 78f * k) }
        drawPath(tear, Color(0xFF6AB0FF), style = Stroke(width = 2.5f * k))
    }
}


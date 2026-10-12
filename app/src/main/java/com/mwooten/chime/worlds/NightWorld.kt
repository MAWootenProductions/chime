package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NightText = Color(0xFFC8D0F0)
private val NightDim = Color(0xFF8A92B8)
private val NightStar = Color(0xFFCFE0FF)

/** Background stars in the design's 390x560 chart coordinates: x, y, radius. */
private val NightField = listOf(
    floatArrayOf(30f, 40f, 1f), floatArrayOf(120f, 80f, 1.2f), floatArrayOf(210f, 30f, 0.8f), floatArrayOf(300f, 70f, 1f),
    floatArrayOf(370f, 140f, 1.3f), floatArrayOf(40f, 200f, 0.9f), floatArrayOf(130f, 380f, 1f), floatArrayOf(230f, 420f, 1.2f),
    floatArrayOf(340f, 400f, 0.8f), floatArrayOf(70f, 480f, 1f), floatArrayOf(300f, 500f, 1.1f), floatArrayOf(190f, 140f, 0.9f),
)

/** Where quest stars sit (chart coordinates), joined in this order into the day's constellation. */
private val NightSlots = listOf(
    170f to 230f, 250f to 260f, 320f to 180f, 230f to 140f, 300f to 340f, 140f to 150f,
)
private const val NightGiantX = 90f
private const val NightGiantY = 300f

/** Magnitude by urgency: lower is brighter. The focus is always magnitude 0. */
private fun nightMag(q: Quest, now: Long): Int {
    val m = q.minutesLeft(now)
    return when (q.shelf(now)) {
        Shelf.OVERDUE -> 1
        Shelf.TODAY -> if (!q.allDay && m != null && m <= 60) 1 else if (!q.allDay && m != null && m <= 180) 2 else 3
        Shelf.TOMORROW -> 4
        else -> 5
    }
}

/** Night Sky: today's and this week's quests as a star chart. The focus is a red giant about to go nova. */
@Composable
fun NightWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val name = s.companion.title
    val clock = rememberClock()
    var naming by rememberSaveable { mutableStateOf(false) }
    val mono = TextStyle(fontFamily = Fonts.dmMono, fontSize = 11.sp, color = NightText)
    val stars = (s.today + s.week).filter { it.id != q?.id }.take(NightSlots.size)
    val meteor = s.companion.c
    // a plain shooting star crosses the sky now; your character lives in the transmission box
    val streak = Color(0xFFFFE9D0)
    val voice = rememberVoice { heard -> a.spill(heard) }
    val clock24 = Time.ldt(s.now).format(DateTimeFormatter.ofPattern("EEE · HH:mm", Locale.getDefault()))
    val hour0 = Time.ldt(s.now).hour

    Box(Modifier.fillMaxSize().background(Color(0xFF070A1A))) {
        // sky, a shooting star, and the planisphere dial
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    0f to Color(0xFF121A3A), 0.7f to Color(0xFF070A1A), 1f to Color(0xFF070A1A),
                    center = Offset(size.width / 2f, size.height * 0.3f), radius = size.maxDimension * 0.75f,
                )
            )
            val t = clock.value
            val k = size.width / 390f
            val p = (t % 3.2f) / 3.2f
            val head = Offset((420f - 460f * p) * k, (90f + 300f * p) * k)
            val dir = Offset(cos(33f * PI.toFloat() / 180f), -sin(33f * PI.toFloat() / 180f))
            val tail = Offset(head.x + dir.x * 120f * k, head.y + dir.y * 120f * k)
            drawLine(
                Brush.linearGradient(listOf(streak, streak.copy(alpha = 0f)), start = head, end = tail),
                head, tail, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
            )
            val hr = 5.dp.toPx()
            drawCircle(
                Brush.radialGradient(0f to Color.White, 0.6f to streak, 1f to streak.copy(alpha = 0f), center = head, radius = hr),
                radius = hr, center = head,
            )

            // planisphere: drag the sky to the due hour
            val ph = (t % 12f) / 12f
            val tri = if (ph < 0.5f) ph * 2f else 2f - ph * 2f
            val turn = -34f * (0.5f - 0.5f * cos(PI.toFloat() * tri))
            val dc = Offset(size.width / 2f, size.height)
            val dr = 150.dp.toPx()
            drawCircle(Color(0x59C8D2FF), radius = dr, center = dc, style = Stroke(width = 1.dp.toPx()))
            for (i in 0 until 24) {
                val ang = (i * 15f + turn) * PI.toFloat() / 180f
                drawLine(
                    Color(0x26C8D2FF), dc,
                    Offset(dc.x + cos(ang) * dr, dc.y + sin(ang) * dr), 1.dp.toPx(),
                )
            }
            drawLine(
                Color(0xFFF05A3C), Offset(size.width / 2f, size.height - 156.dp.toPx()),
                Offset(size.width / 2f, size.height - 140.dp.toPx()), 2.dp.toPx(),
            )
        }

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    "Caelum Hodiernum",
                    style = TextStyle(fontFamily = Fonts.cormorantGaramond, fontStyle = FontStyle.Italic, fontWeight = FontWeight.W600, fontSize = 26.sp, color = Color(0xFFE8ECFF)),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("$clock24 local", style = mono, maxLines = 1)
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        stars.isEmpty() && q == null -> "a clear sky: nothing due"
                        s.week.isNotEmpty() -> "the constellation of things due this week"
                        else -> "the constellation of things due today"
                    },
                    style = mono.copy(color = NightDim),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                NightChip("catalog", mono, a::openLog)
                Spacer(Modifier.width(6.dp))
                NightChip("Settings", mono, a::openOptions)
            }

            // the star chart
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val w = maxWidth
                val h = maxHeight
                Canvas(Modifier.fillMaxSize()) {
                    val kx = size.width / 390f
                    val ky = size.height / 560f
                    fun pt(x: Float, y: Float) = Offset(x * kx, y * ky)
                    val grid = Color(0x248CA0E6)
                    for (y in listOf(120f, 240f, 360f, 480f)) {
                        val g = Path().apply { moveTo(-20f * kx, y * ky); quadraticBezierTo(195f * kx, (y - 60f) * ky, 410f * kx, y * ky) }
                        drawPath(g, grid, style = Stroke(width = 1f))
                    }
                    for ((x, bend) in listOf(60f to 40f, 160f to 150f, 260f to 270f, 350f to 370f)) {
                        val g = Path().apply { moveTo(x * kx, 0f); quadraticBezierTo(bend * kx, 280f * ky, x * kx, 560f * ky) }
                        drawPath(g, grid, style = Stroke(width = 1f))
                    }
                    // background stars, twinkling
                    for ((i, f) in NightField.withIndex()) {
                        val tw = 0.65f + 0.35f * sin(clock.value * 1.3f + i * 2.1f)
                        drawCircle(Color.White, radius = f[2] * kx, center = pt(f[0], f[1]), alpha = tw)
                    }
                    // constellation lines from the giant through the day's stars
                    if (stars.isNotEmpty()) {
                        val line = Path()
                        if (q != null) line.moveTo(NightGiantX * kx, NightGiantY * ky)
                        else line.moveTo(NightSlots[0].first * kx, NightSlots[0].second * ky)
                        for (i in stars.indices) line.lineTo(NightSlots[i].first * kx, NightSlots[i].second * ky)
                        drawPath(
                            line, Color(0x59C8D2FF),
                            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()), 0f)),
                        )
                    }
                    // quest stars, sized by magnitude
                    for ((i, sq) in stars.withIndex()) {
                        val mag = nightMag(sq, s.now)
                        val (x, y) = NightSlots[i]
                        drawCircle(NightStar, radius = (3.2f - 0.25f * mag) * kx, center = pt(x, y), alpha = (1f - 0.12f * (mag - 1)).coerceIn(0.4f, 1f))
                    }
                    // the focus: a red giant about to go nova
                    if (q != null) {
                        val pulse = 0.5f - 0.5f * cos(2f * PI.toFloat() * clock.value / 1.4f)
                        val r = 20.dp.toPx() * (1f + 0.35f * pulse)
                        val c = pt(NightGiantX, NightGiantY)
                        drawCircle(
                            Brush.radialGradient(
                                0f to Color(0xFFFFF2D8), 0.18f to Color(0xFFFFF2D8), 0.40f to Color(0xFFFF8A5A),
                                0.70f to Color(0x00F05A3C), 1f to Color(0x00F05A3C),
                                center = c, radius = r,
                            ),
                            radius = r, center = c, alpha = 0.9f + 0.1f * pulse,
                        )
                    }
                }

                // chart labels: declination and the next few hours of right ascension
                val degs = listOf("+40°" to 116f, "+20°" to 236f, "0°" to 356f)
                for ((label, y) in degs) {
                    Text(
                        label, style = mono.copy(fontSize = 9.sp, color = Color(0x668CA0E6)),
                        modifier = Modifier.offset(x = 6.dp, y = h * (y / 560f) - 10.dp),
                    )
                }
                for ((i, x) in listOf(64f, 164f, 264f, 354f).withIndex()) {
                    Text(
                        "${(hour0 + i) % 24}h", style = mono.copy(fontSize = 9.sp, color = Color(0x668CA0E6)),
                        modifier = Modifier.offset(x = w * (x / 390f) - 4.dp, y = h - 16.dp),
                    )
                }
                // quest star names
                for ((i, sq) in stars.withIndex()) {
                    val (x, y) = NightSlots[i]
                    val sx = w * (x / 390f)
                    val sy = h * (y / 560f)
                    val labelW = 128.dp
                    val leftSide = sx + 8.dp + labelW > w
                    Text(
                        "${sq.text} · mag ${nightMag(sq, s.now)}",
                        style = mono.copy(fontSize = 10.sp, textAlign = if (leftSide) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .offset(x = if (leftSide) sx - 8.dp - labelW else sx + 8.dp, y = sy - 16.dp)
                            .width(labelW)
                            .tap { a.openQuest(sq) },
                    )
                }
                if (q != null) {
                    val gx = w * (NightGiantX / 390f)
                    val gy = h * (NightGiantY / 560f)
                    val due = q.due
                    val nova = when {
                        due == null -> "no nova date charted"
                        q.allDay -> "goes nova ${Time.dayName(Time.date(due)).lowercase()}"
                        due < s.now -> "went nova at ${Time.time(due)}"
                        else -> "goes nova at ${Time.time(due)}"
                    }
                    Column(
                        Modifier.offset(x = gx + 26.dp, y = gy - 12.dp)
                            .widthIn(max = (w - gx - 42.dp).coerceAtLeast(80.dp))
                            .tap { a.openQuest(q) }
                    ) {
                        Text(
                            "${q.text.uppercase()} · mag 0",
                            style = mono.copy(fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFFFFB59A)),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(nova, style = mono.copy(fontSize = 12.sp, lineHeight = 17.sp, color = NightDim), maxLines = 1)
                    }
                }
            }

            // your character's transmission: tap the character to talk back
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    .border(1.dp, meteor.copy(alpha = 0.5f))
                    .background(s.companion.cDark.copy(alpha = 0.3f))
                    .padding(start = 8.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CharacterArt(
                    size = 60.dp,
                    style = ArtStyle.SHINY,
                    level = voice.level,
                    listening = voice.listening,
                    onTap = { voice.toggle() },
                )
                Spacer(Modifier.width(10.dp))
                Column(
                    Modifier.weight(1f)
                        .then(if (q != null) Modifier.tap { a.openQuest(q) } else Modifier)
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = meteor, fontWeight = FontWeight.W500)) { append("${name.uppercase()} › ") }
                            when {
                                voice.listening -> withStyle(SpanStyle(color = Color(0xFFFFE9D0))) {
                                    append(voice.heard.ifBlank { "listening… name a new star" })
                                }
                                voice.note.isNotBlank() -> append(voice.note)
                                else -> append(s.line)
                            }
                        },
                        style = mono.copy(fontSize = 13.sp, lineHeight = 19.sp, color = s.companion.cLight),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (q != null) {
                        Text(
                            "› let it drift an hour",
                            style = mono.copy(color = NightDim),
                            modifier = Modifier.padding(top = 6.dp).tap { a.snooze(q, 60) },
                        )
                    }
                }
            }

            if (naming || q == null) {
                Spacer(Modifier.height(10.dp))
                SpillInput(
                    onSpill = { a.spill(it); naming = false },
                    textStyle = mono.copy(fontSize = 14.sp, color = Color(0xFFE8ECFF)),
                    placeholder = "pay rent by friday",
                    placeholderColor = NightDim.copy(alpha = 0.7f),
                    cursor = meteor,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                        .background(Color(0xD90A0E28)).border(1.dp, Color(0x66C8D2FF))
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                ) { submit ->
                    Text(
                        "CHART IT",
                        style = mono.copy(fontSize = 12.sp, fontWeight = FontWeight.W500, color = Color(0xFF10060A)),
                        modifier = Modifier.padding(start = 8.dp).background(meteor).tap(submit).padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (q != null) {
                    Box(
                        Modifier.weight(1f).background(meteor).tap { a.done(q) }.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("CATCH IT (done)", style = mono.copy(fontSize = 13.sp, fontWeight = FontWeight.W500, color = Color(0xFF10060A)), maxLines = 1)
                    }
                }
                Box(
                    Modifier.weight(1f).background(Color(0xD90A0E28)).border(1.dp, Color(0x66C8D2FF))
                        .tap { naming = !naming }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("NAME A NEW STAR", style = mono.copy(fontSize = 13.sp), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun NightChip(label: String, mono: TextStyle, onClick: () -> Unit) {
    Text(
        label,
        style = mono.copy(fontSize = 10.sp, color = NightText),
        modifier = Modifier.border(1.dp, Color(0x59C8D2FF)).tap(onClick).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

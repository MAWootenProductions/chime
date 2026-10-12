package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin

private val WheelGrey = Color(0xFF9A9A96)
private val WheelBlue = Color(0xFF2A6AD8)

/** "-2:13:00" left, "+0:12:00" late, "--:--" when undated. */
private fun wheelRemaining(q: Quest, now: Long): String {
    val d = q.due ?: return "--:--"
    val diff = d - now
    val secs = abs(diff) / 1000
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val sec = secs % 60
    val body = "$h:" + m.toString().padStart(2, '0') + ":" + sec.toString().padStart(2, '0')
    return (if (diff >= 0) "-" else "+") + body
}

/** Elapsed share of the time between spilling and due. */
private fun wheelProgress(q: Quest, now: Long): Float {
    val d = q.due ?: return 0f
    val span = (d - q.created).toFloat()
    if (span <= 0f) return 1f
    return ((now - q.created) / span).coerceIn(0f, 1f)
}

private fun wheelDay(ms: Long): String {
    val n = Time.dayName(Time.date(ms))
    return if (n.length > 9) n.take(3) else if (n == "Today" || n == "Yesterday") n else n.take(3)
}

@Composable
fun WheelWorld(s: WorldState, a: WorldActions) {
    val tracks = s.open
    var index by rememberSaveable { mutableStateOf(0) }
    var spilling by rememberSaveable { mutableStateOf(false) }
    val i = if (tracks.isEmpty()) 0 else index.mod(tracks.size)
    val q: Quest? = tracks.getOrNull(i)
    val line = q?.let { s.companion.line(it, s.now) } ?: s.line
    val tick by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }
    val clock = rememberClock()
    val voice = rememberVoice { heard -> a.spill(heard) }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF6F6F3), Color(0xFFE4E4DF))))
            .drawBehind {
                // chrome back peeking at the edges
                val w6 = 6.dp.toPx()
                drawRect(Color(0xFFD8D8D4), style = Stroke(w6 * 2))
                drawRect(Color(0xFFB8B8B2), topLeft = Offset(w6, w6), size = Size(size.width - w6 * 2, size.height - w6 * 2), style = Stroke(1.dp.toPx()))
            },
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            // ---- the screen
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(300.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A)).padding(8.dp),
            ) {
                Column(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFBFBFB), Color(0xFFE9ECEF)))),
                ) {
                    // title bar
                    Row(
                        Modifier.fillMaxWidth().height(22.dp)
                            .background(Brush.verticalGradient(listOf(Color(0xFFF2F2F2), Color(0xFFCFD3D8))))
                            .drawBehind { drawLine(Color(0xFF9AA0A8), Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Canvas(Modifier.width(12.dp).height(10.dp)) {
                            val t = clock.value
                            val bw = 2.dp.toPx()
                            for (k in 0..2) {
                                val ph = if (q == null) 0f else (sin(t / 0.6f * 2f * PI.toFloat() - k * 2.1f) + 1f) / 2f
                                val h = (3f + 6f * ph).dp.toPx()
                                drawRect(WheelBlue, topLeft = Offset(k * 4.dp.toPx(), size.height - h), size = Size(bw, h))
                            }
                        }
                        Text(
                            "Now Due",
                            style = TextStyle(fontFamily = Fonts.archivo, fontWeight = FontWeight.W700, fontSize = 11.sp, color = Color(0xFF111111), textAlign = TextAlign.Center),
                            modifier = Modifier.weight(1f),
                        )
                        Canvas(Modifier.width(22.dp).height(10.dp)) {
                            val sw = 1.dp.toPx()
                            drawRoundRect(Color(0xFF333333), topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = Stroke(sw))
                            drawRect(Color(0xFF4CAF50), topLeft = Offset(2 * sw, 2 * sw), size = Size(size.width - 8.dp.toPx(), size.height - 4 * sw))
                        }
                    }
                    // art + track info
                    Row(
                        Modifier.weight(1f).fillMaxWidth().tap { if (q != null) a.openQuest(q) }
                            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WheelArt(s, voice)
                        Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                q?.text ?: "Nothing Due",
                                style = TextStyle(fontFamily = Fonts.archivo, fontWeight = FontWeight.W700, fontSize = 15.sp, lineHeight = 18.sp, color = Color(0xFF111111)),
                                maxLines = 3, overflow = TextOverflow.Ellipsis,
                            )
                            Text(s.companion.title, style = TextStyle(fontFamily = Fonts.archivo, fontSize = 13.sp, color = Color(0xFF444444)))
                            Text(
                                q?.dueText() ?: "Press the center to add one",
                                style = TextStyle(fontFamily = Fonts.archivo, fontSize = 13.sp, color = Color(0xFF777777)),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (q == null) "0 of 0" else "${i + 1} of ${tracks.size}",
                                style = TextStyle(fontFamily = Fonts.archivo, fontSize = 11.sp, color = Color(0xFF777777)),
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    // progress: elapsed share between spilled and due
                    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val frac = if (q == null) 0f else wheelProgress(q, tick)
                        Box(
                            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFC8CDD3)),
                        ) {
                            Box(
                                Modifier.fillMaxWidth(frac).fillMaxHeight()
                                    .background(Brush.verticalGradient(listOf(Color(0xFF7FB2FF), WheelBlue))),
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            val st = TextStyle(fontFamily = Fonts.archivo, fontWeight = FontWeight.W600, fontSize = 11.sp, color = Color(0xFF111111))
                            Text(if (q == null) "Added --" else "Added ${wheelDay(q.created)}", style = st, modifier = Modifier.weight(1f))
                            Text(if (q == null) "--:--" else wheelRemaining(q, tick), style = st)
                        }
                    }
                    // the companion's liner note
                    Text(
                        when {
                            voice.listening -> "Recording… " + voice.heard.ifBlank { "say what's due" }
                            else -> "${s.companion.title}: “$line”"
                        },
                        style = TextStyle(fontFamily = Fonts.archivo, fontSize = 12.sp, lineHeight = 15.sp, color = Color(0xFF3A3410)),
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF8D8))
                            .drawBehind { drawLine(Color(0xFFE0D6A0), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // ---- the wheel
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val d: Dp = minOf(maxWidth, maxHeight - 16.dp, 280.dp).coerceAtLeast(150.dp)
                WheelPad(
                    d = d,
                    hasQuest = q != null,
                    canSkip = tracks.size > 1,
                    onLog = a::openLog,
                    onPrev = { index = i - 1 },
                    onNext = { index = i + 1 },
                    onPlay = { q?.let(a::done) },
                    onPause = { q?.let { a.snooze(it, 60) } },
                    onCenter = { spilling = !spilling },
                )
            }

            // ---- spill line or the hint
            if (spilling) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White)
                        .border(1.dp, Color(0xFFC8C8C2), RoundedCornerShape(10.dp))
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpillInput(
                        onSpill = { a.spill(it); spilling = false },
                        textStyle = TextStyle(fontFamily = Fonts.archivo, fontSize = 15.sp, color = Color(0xFF111111)),
                        placeholder = "New to-do: what's due?",
                        placeholderColor = Color(0xFF9A9A96),
                        cursor = WheelBlue,
                        modifier = Modifier.weight(1f),
                        trailing = { submit ->
                            Text(
                                "Add",
                                style = TextStyle(fontFamily = Fonts.archivo, fontWeight = FontWeight.W700, fontSize = 13.sp, color = Color.White),
                                modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).background(WheelBlue)
                                    .tap { submit() }.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        },
                    )
                }
            } else {
                Text(
                    voice.note.ifBlank { "Spin to scroll your to-dos. Press the center to type one, or tap the cover to talk." },
                    style = TextStyle(fontFamily = Fonts.archivo, fontSize = 12.sp, letterSpacing = 0.5.sp, color = Color(0xFF8A8A86), textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                "Settings",
                style = TextStyle(fontFamily = Fonts.archivo, fontWeight = FontWeight.W700, fontSize = 13.sp, letterSpacing = 0.5.sp, color = Color(0xFF3A3A38)),
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp).clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE1E1DC))))
                    .border(1.dp, Color(0xFFC8C8C2), RoundedCornerShape(50))
                    .tap(a::openOptions).padding(horizontal = 18.dp, vertical = 7.dp),
            )
        }
    }
}

/** Album art: the user's character as a flat drawn cover, on a sleeve in its own colors. Tap to talk. */
@Composable
private fun WheelArt(s: WorldState, voice: Voice) {
    val c = s.companion.c
    Box(
        Modifier.size(96.dp)
            .drawBehind {
                drawRect(Color(0x1F000000), topLeft = Offset(0f, 4.dp.toPx()), size = size)
            }
            .background(Brush.linearGradient(listOf(s.companion.cLight, lerp(c, s.companion.cLight, 0.25f))))
            .drawBehind {
                // sleeve detail: a thin rule and a sticker dot, like a single's cover
                drawLine(Color(0x33000000), Offset(0f, size.height - 14.dp.toPx()), Offset(size.width, size.height - 14.dp.toPx()), 1.dp.toPx())
                drawCircle(if (voice.listening) Color(0xFFE53935) else Color(0x55FFFFFF), 4.dp.toPx(), Offset(size.width - 9.dp.toPx(), 9.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        CharacterArt(
            78.dp, ArtStyle.FLAT, who = s.companion, level = voice.level, listening = voice.listening,
            glow = false, onTap = { voice.toggle() },
        )
    }
}

@Composable
private fun WheelPad(
    d: Dp,
    hasQuest: Boolean,
    canSkip: Boolean,
    onLog: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onCenter: () -> Unit,
) {
    val prev by rememberUpdatedState(onPrev)
    val next by rememberUpdatedState(onNext)
    val skip by rememberUpdatedState(canSkip)
    val acc = remember { floatArrayOf(0f, 0f) } // [accumulated angle, last angle]
    val k = d / 280.dp
    Box(
        Modifier.size(d)
            .clip(CircleShape)
            .background(Brush.radialGradient(0f to Color.White, 0.7f to Color(0xFFE6E6E1), 1f to Color(0xFFD6D6D0)))
            .border(1.dp, Color(0xFFC8C8C2), CircleShape)
            .pointerInput(Unit) {
                // spin to scrub: every ~30 degrees of travel is one track
                detectDragGestures(
                    onDragStart = { p ->
                        acc[0] = 0f
                        acc[1] = atan2(p.y - size.height / 2f, p.x - size.width / 2f)
                    },
                    onDrag = { change, _ ->
                        val p = change.position
                        val ang = atan2(p.y - size.height / 2f, p.x - size.width / 2f)
                        var delta = ang - acc[1]
                        if (delta > PI) delta -= (2 * PI).toFloat()
                        if (delta < -PI) delta += (2 * PI).toFloat()
                        acc[1] = ang
                        acc[0] += delta
                        val step = (PI / 6).toFloat()
                        if (skip) {
                            while (acc[0] > step) { acc[0] -= step; next() }
                            while (acc[0] < -step) { acc[0] += step; prev() }
                        }
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // MENU-position label: the log
        Text(
            "LOG",
            style = TextStyle(fontFamily = Fonts.archivo, fontWeight = FontWeight.W700, fontSize = 13.sp, letterSpacing = 1.sp, color = WheelGrey),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp * k).tap(onLog).padding(horizontal = 14.dp, vertical = 6.dp),
        )
        // previous
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 12.dp * k).alpha(if (canSkip) 1f else 0.4f)
                .tap { if (canSkip) onPrev() }.padding(8.dp),
        ) {
            Canvas(Modifier.width(26.dp).height(14.dp)) {
                val u = size.width / 26f
                drawPath(Path().apply { moveTo(12 * u, 0f); lineTo(12 * u, 14 * u); lineTo(2 * u, 7 * u); close() }, WheelGrey)
                drawPath(Path().apply { moveTo(24 * u, 0f); lineTo(24 * u, 14 * u); lineTo(14 * u, 7 * u); close() }, WheelGrey)
                drawRect(WheelGrey, topLeft = Offset(0f, 0f), size = Size(2 * u, 14 * u))
            }
        }
        // next
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp * k).alpha(if (canSkip) 1f else 0.4f)
                .tap { if (canSkip) onNext() }.padding(8.dp),
        ) {
            Canvas(Modifier.width(26.dp).height(14.dp)) {
                val u = size.width / 26f
                drawPath(Path().apply { moveTo(2 * u, 0f); lineTo(2 * u, 14 * u); lineTo(12 * u, 7 * u); close() }, WheelGrey)
                drawPath(Path().apply { moveTo(14 * u, 0f); lineTo(14 * u, 14 * u); lineTo(24 * u, 7 * u); close() }, WheelGrey)
                drawRect(WheelGrey, topLeft = Offset(24 * u, 0f), size = Size(2 * u, 14 * u))
            }
        }
        // play / pause = done / later
        if (hasQuest) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp * k),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.tap(onPlay).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Canvas(Modifier.width(10.dp).height(14.dp)) {
                        drawPath(Path().apply { moveTo(0f, 0f); lineTo(0f, size.height); lineTo(size.width, size.height / 2); close() }, WheelGrey)
                    }
                    Text("done", style = TextStyle(fontFamily = Fonts.archivo, fontSize = 9.sp, color = WheelGrey))
                }
                Column(
                    Modifier.tap(onPause).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Canvas(Modifier.width(11.dp).height(14.dp)) {
                        val bw = 4.dp.toPx()
                        drawRect(WheelGrey, topLeft = Offset(0f, 0f), size = Size(bw, size.height))
                        drawRect(WheelGrey, topLeft = Offset(size.width - bw, 0f), size = Size(bw, size.height))
                    }
                    Text("later", style = TextStyle(fontFamily = Fonts.archivo, fontSize = 9.sp, color = WheelGrey))
                }
            }
        }
        // center button = spill
        Box(
            Modifier.size(104.dp * k).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFF8F8F6), Color(0xFFE1E1DC))))
                .border(1.dp, Color(0xFFC8C8C2), CircleShape)
                .tap(onCenter),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(20.dp)) {
                val w = 2.4.dp.toPx()
                drawLine(Color(0xFFB0B0AA), Offset(size.width / 2, 3.dp.toPx()), Offset(size.width / 2, size.height - 3.dp.toPx()), w, cap = StrokeCap.Round)
                drawLine(Color(0xFFB0B0AA), Offset(3.dp.toPx(), size.height / 2), Offset(size.width - 3.dp.toPx(), size.height / 2), w, cap = StrokeCap.Round)
            }
        }
    }
}

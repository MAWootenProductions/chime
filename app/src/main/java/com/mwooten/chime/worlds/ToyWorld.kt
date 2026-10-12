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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlin.math.PI
import kotlin.math.sin

private val ToyInk = Color(0xFF3A1F3A)
private val ToyPink = Color(0xFFC2367A)
private val ToyPlum = Color(0xFF8A4A7A)

/** The toy's own language: each word squashed into two baby syllables. "jury duty" -> "Joo-ree! Doo-tee!" */
private fun toyGibberish(text: String): String {
    val vowel = mapOf('a' to "ah", 'e' to "ee", 'i' to "ee", 'o' to "oo", 'u' to "oo", 'y' to "ee")
    val words = text.lowercase().split(Regex("[^a-z]+")).filter { it.length > 2 }.take(3)
    val out = words.mapNotNull { w ->
        val syl = Regex("[^aeiouy]*[aeiouy]+").findAll(w).map { it.value }.take(2).toList()
        if (syl.isEmpty()) null
        else syl.joinToString("-") { sy ->
            val cons = sy.firstOrNull { it !in vowel.keys } ?: 'w'
            val v = vowel[sy.first { it in vowel.keys }] ?: "ah"
            "$cons$v"
        }.replaceFirstChar { it.uppercase() } + "!"
    }
    return (out + "Kah-boo!").joinToString(" ")
}

@Composable
fun ToyWorld(s: WorldState, a: WorldActions) {
    val q: Quest? = s.focus
    var feeding by rememberSaveable { mutableStateOf(false) }
    val total = s.done.size + s.open.size
    val learned = if (total == 0) 0 else (s.done.size * 100 / total).coerceIn(0, 100)
    val firstDay = s.quests.minOfOrNull { it.created } ?: s.now
    val day = ((s.now - firstDay) / 86_400_000L + 1).coerceAtLeast(1)
    val clock = rememberClock()
    val voice = rememberVoice { heard -> a.spill(heard) }

    Box(
        Modifier.fillMaxSize().background(Color(0xFFF7E6F2)).drawBehind {
            // blister-card stripes
            val stripe = 18.dp.toPx()
            rotate(45f) {
                val diag = size.width + size.height
                var x = -diag
                while (x < diag * 2) {
                    drawRect(Color(0xFFF2DCEC), topLeft = Offset(x + stripe, -diag), size = Size(stripe, diag * 3))
                    x += stripe * 2
                }
            }
        },
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 22.dp),
        ) {
            // ---- header: name on the box, battery note, the starburst
            Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Column(Modifier.weight(1f).padding(top = 10.dp)) {
                    Text(
                        s.companion.title,
                        style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W800, fontSize = 34.sp, lineHeight = 36.sp, color = ToyPink),
                    )
                    Text(
                        "Batteries included · 3 AA",
                        style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W700, fontSize = 14.sp, color = ToyPlum),
                    )
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToyPill("Dictionary", onClick = a::openLog)
                        ToyPill("Settings", onClick = a::openOptions)
                    }
                }
                Box(Modifier.size(100.dp).rotate(12f), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val pts = listOf(
                            50f to 0f, 61f to 18f, 82f to 10f, 80f to 32f, 100f to 38f, 86f to 54f, 98f to 72f, 76f to 74f, 72f to 96f,
                            54f to 84f, 36f to 100f, 30f to 78f, 8f to 80f, 16f to 60f, 0f to 46f, 18f to 34f, 12f to 12f, 34f to 16f,
                        )
                        val p = Path()
                        pts.forEachIndexed { k, (x, y) ->
                            val px = x / 100f * size.width; val py = y / 100f * size.height
                            if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
                        }
                        p.close()
                        drawPath(p, Color(0xFFFFE14A))
                    }
                    Text(
                        "LEARNS\nYOUR\nTO-DOS!",
                        style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W800, fontSize = 13.sp, lineHeight = 13.sp, color = ToyInk, textAlign = TextAlign.Center),
                    )
                }
            }

            // ---- the toy itself
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val h = minOf(maxHeight - 22.dp, 260.dp).coerceAtLeast(80.dp)
                val w = h * (240f / 260f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ToyCreature(
                        s, clock, Modifier.width(w).height(h),
                        onBelly = { if (q != null) a.done(q) },
                        onTap = { voice.toggle() },
                    )
                    Text(
                        if (voice.listening) "It's listening! Tap again when you're done."
                        else if (voice.note.isNotBlank()) voice.note
                        else "Tap ${s.companion.title} to talk to it!",
                        style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 14.sp, color = ToyPlum, textAlign = TextAlign.Center),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // ---- what it says, then the translation from the little paper dictionary
            val translation = when {
                voice.listening -> "Listening: " + voice.heard.ifBlank { "say a to-do…" }
                else -> s.line
            }
            val babble = if (voice.listening) "Ooh! Ooh! Tell me!" else toyGibberish(q?.text ?: s.line)
            Box(Modifier.fillMaxWidth().padding(end = 5.dp, bottom = 5.dp)) {
                Box(
                    Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp)
                        .clip(RoundedCornerShape(22.dp)).background(ToyInk),
                )
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White)
                        .border(3.dp, ToyInk, RoundedCornerShape(22.dp))
                        .tap { if (q != null) a.openQuest(q) }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "“$babble”",
                        style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W800, fontSize = 24.sp, lineHeight = 27.sp, color = ToyPink),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        translation,
                        style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W500, fontSize = 16.sp, lineHeight = 21.sp, color = ToyInk),
                        maxLines = 4, overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ---- how much of your words it has picked up
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                val st = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W700, fontSize = 13.sp, color = ToyInk)
                Text("${s.companion.title} has learned $learned% of your words", style = st, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Day $day", style = st)
            }
            Spacer(Modifier.height(6.dp))
            val c = s.companion.c
            val stripe = lerp(c, s.companion.cLight, 0.4f)
            Box(
                Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White)
                    .border(2.dp, ToyInk, RoundedCornerShape(6.dp)),
            ) {
                Canvas(Modifier.fillMaxWidth(learned / 100f).fillMaxHeight().padding(2.dp)) {
                    clipRect {
                        drawRect(c)
                        val st = 8.dp.toPx()
                        var x = -size.height
                        while (x < size.width + size.height) {
                            val p = Path().apply {
                                moveTo(x, size.height); lineTo(x + st, size.height); lineTo(x + st + size.height, 0f); lineTo(x + size.height, 0f); close()
                            }
                            drawPath(p, stripe)
                            x += st * 2
                        }
                    }
                }
            }

            // ---- feeding it a new word = spill
            if (feeding) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .border(2.dp, ToyInk, RoundedCornerShape(16.dp))
                        .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpillInput(
                        onSpill = { a.spill(it); feeding = false },
                        textStyle = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W700, fontSize = 16.sp, color = ToyInk),
                        placeholder = "Teach it a new to-do",
                        placeholderColor = ToyPlum.copy(alpha = 0.6f),
                        cursor = ToyPink,
                        modifier = Modifier.weight(1f),
                        trailing = { submit ->
                            Text(
                                "Feed",
                                style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W800, fontSize = 15.sp, color = Color.White),
                                modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(12.dp)).background(ToyInk)
                                    .tap { submit() }.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        },
                    )
                }
            }

            // ---- how you play with it
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (q != null) {
                    ToyButton("Tickle belly", "done", dark = true, modifier = Modifier.weight(1f)) { a.done(q) }
                    ToyButton("Pet head", "later", dark = false, modifier = Modifier.weight(1f)) { a.snooze(q, 60) }
                }
                ToyButton(if (feeding) "Not now" else "Feed it", if (feeding) "close" else "add a to-do", dark = q == null, modifier = Modifier.weight(1f)) {
                    feeding = !feeding
                }
            }
        }
    }
}

@Composable
private fun ToyPill(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W700, fontSize = 12.sp, color = ToyInk),
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White)
            .border(2.dp, ToyInk, RoundedCornerShape(50)).tap(onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

@Composable
private fun ToyButton(title: String, sub: String, dark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape)
            .background(if (dark) ToyInk else Color.White)
            .then(if (dark) Modifier else Modifier.border(2.dp, ToyInk, shape))
            .tap(onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W800, fontSize = 15.sp, lineHeight = 17.sp, color = if (dark) Color.White else ToyInk, textAlign = TextAlign.Center),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            sub,
            style = TextStyle(fontFamily = Fonts.baloo2, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 14.sp, color = if (dark) Color(0xFFFFB86A) else ToyPlum),
        )
    }
}

/** Fill [path] with fur: base color plus diagonal hatching, like the design's SVG pattern. */
private fun DrawScope.toyFur(path: Path, base: Color, hatch: Color, u: Float, width: Float) {
    drawPath(path, base)
    clipPath(path) {
        val b = path.getBounds()
        val step = 4f * u
        var x = b.left - b.height
        while (x < b.right + step) {
            // short tufts: "M0 8 L4 0" repeated on an 8px grid
            var y = b.bottom
            while (y > b.top - 8f * u) {
                drawLine(hatch, Offset(x, y), Offset(x + 4f * u, y - 8f * u), strokeWidth = width)
                y -= 8f * u
            }
            x += step
        }
    }
}

@Composable
private fun ToyCreature(s: WorldState, clock: androidx.compose.runtime.State<Float>, modifier: Modifier, onBelly: () -> Unit, onTap: () -> Unit) {
    val fur = s.companion.c
    val furHatch = lerp(s.companion.c, s.companion.cDark, 0.35f)
    val belly = s.companion.cLight
    val bellyHatch = lerp(s.companion.cLight, s.companion.c, 0.3f)
    Box(modifier.tap(onTap)) {
        // layer 1, animated: ears wiggle behind the head
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width / 240f
            val t = clock.value
            fun P(x: Float, y: Float) = Offset(x * u, y * u)
            val earL = -8f * (1f - kotlin.math.cos(t / 2f * 2f * PI.toFloat())) / 2f
            val earR = -8f * (1f - kotlin.math.cos((t - 0.2f) / 2f * 2f * PI.toFloat())) / 2f
            withTransform({ rotate(earL, pivot = P(70f, 40f)) }) {
                toyFur(Path().apply { moveTo(70 * u, 60 * u); lineTo(40 * u, 0f); lineTo(96 * u, 40 * u); close() }, fur, furHatch, u, 1.4f * u)
                drawPath(Path().apply { moveTo(70 * u, 52 * u); lineTo(52 * u, 14 * u); lineTo(86 * u, 42 * u); close() }, Color(0xFFFF6AA8))
            }
            withTransform({ rotate(-earR, pivot = P(170f, 40f)) }) {
                toyFur(Path().apply { moveTo(170 * u, 60 * u); lineTo(200 * u, 0f); lineTo(144 * u, 40 * u); close() }, fur, furHatch, u, 1.4f * u)
                drawPath(Path().apply { moveTo(170 * u, 52 * u); lineTo(188 * u, 14 * u); lineTo(154 * u, 42 * u); close() }, Color(0xFFFF6AA8))
            }
        }
        // layer 2, static: furry body, belly, eyes, belly sensor (no clock read, so it is not redrawn every frame)
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width / 240f
            fun P(x: Float, y: Float) = Offset(x * u, y * u)
            toyFur(Path().apply { addOval(Rect(16 * u, 44 * u, 224 * u, 256 * u)) }, fur, furHatch, u, 1.4f * u)
            toyFur(Path().apply { addOval(Rect(58 * u, 132 * u, 182 * u, 248 * u)) }, belly, bellyHatch, u, 1.2f * u)
            for ((cx, px, pp) in listOf(Triple(84f, 90f, 92f), Triple(156f, 150f, 148f))) {
                drawCircle(Color.White, 30 * u, P(cx, 110f))
                drawCircle(ToyInk, 30 * u, P(cx, 110f), style = Stroke(3 * u))
                drawCircle(Color(0xFF3A6AD0), 16 * u, P(px, 114f))
                drawCircle(Color(0xFF10102A), 8 * u, P(pp, 116f))
                drawCircle(Color.White, 4 * u, P(px + 6f, 108f))
            }
            drawCircle(Color(0xFFFFD0A0), 10 * u, P(120f, 214f))
            drawCircle(Color(0xFFE0A060), 10 * u, P(120f, 214f), style = Stroke(2 * u))
        }
        // layer 3, animated: plastic eyelids blink, beak talks
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width / 240f
            val t = clock.value
            fun P(x: Float, y: Float) = Offset(x * u, y * u)
            val phase = (t % 4f) / 4f
            val lid = when {
                phase < 0.92f -> 0f
                phase < 0.95f -> (phase - 0.92f) / 0.03f
                else -> (1f - (phase - 0.95f) / 0.05f).coerceAtLeast(0f)
            }
            if (lid > 0f) {
                for (cx in listOf(84f, 156f)) {
                    clipPath(Path().apply { addOval(Rect((cx - 31.5f) * u, 78.5f * u, (cx + 31.5f) * u, 141.5f * u)) }) {
                        drawRect(fur, topLeft = P(cx - 32f, 78f), size = Size(64 * u, 64 * u * lid))
                    }
                }
            }
            val upper = Path().apply { moveTo(104 * u, 142 * u); lineTo(136 * u, 142 * u); lineTo(120 * u, 160 * u); close() }
            drawPath(upper, Color(0xFFFFCF3A))
            drawPath(upper, Color(0xFFC28A10), style = Stroke(2 * u))
            val open = 0.7f + 0.3f * sin(t / 0.3f * 2f * PI.toFloat())
            val lower = Path().apply {
                moveTo(106 * u, 158 * u); lineTo(134 * u, 158 * u); lineTo(120 * u, (158f + 14f * open) * u); close()
            }
            drawPath(lower, Color(0xFFF0B020))
            drawPath(lower, Color(0xFFC28A10), style = Stroke(2 * u))
        }
        // the belly sensor is a real button: tickle it to finish the quest
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val u = maxWidth / 240f
            Box(
                Modifier.offset(x = u * 100f, y = u * 194f).size(u * 40f).clip(RoundedCornerShape(50)).tap(onBelly),
            )
        }
    }
}

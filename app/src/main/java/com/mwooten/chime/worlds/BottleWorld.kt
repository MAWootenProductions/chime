package com.mwooten.chime.worlds

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

// dusk palette, picked by hand
private val BtSkyTop = Color(0xFF5E5A92)
private val BtLavender = Color(0xFF9C86B8)
private val BtRose = Color(0xFFE9A99A)
private val BtPeach = Color(0xFFF7C79C)
private val BtGlow = Color(0xFFFBDDB0)
private val BtDeep = Color(0xFF1D4F5E)
private val BtTeal = Color(0xFF23626B)
private val BtShallow = Color(0xFF2E7A7A)
private val BtSand = Color(0xFFE8D3B0)
private val BtSandLow = Color(0xFFD9BC90)
private val BtWet = Color(0xFFB99B78)
private val BtInk = Color(0xFF3A2A1A)
private val BtInkSoft = Color(0xFF8A6A3A)
private val BtParchment = Color(0xFFF3E3C1)
private val BtNight = Color(0xFF2E2A4A)

/** Fractions of the screen height where things sit. */
private const val BtHorizon = 0.36f
private const val BtShoreLow = 0.585f
private const val BtShoreRise = 0.115f
private const val BtTideMark = 0.685f

private fun btRand(i: Int, salt: Int): Float {
    val x = sin(i * 12.9898f + salt * 78.233f) * 43758.547f
    return x - floor(x)
}

private class BtStar(val x: Float, val y: Float, val r: Float, val phase: Float)
private val BtStars = List(26) { BtStar(btRand(it, 1), btRand(it, 2) * 0.2f, 0.6f + btRand(it, 3) * 0.9f, btRand(it, 4) * 6f) }
private val BtSpeckles = List(170) { floatArrayOf(btRand(it, 5), btRand(it, 6), btRand(it, 7)) }
private val BtRipples = List(16) { floatArrayOf(btRand(it, 8), 0.12f + btRand(it, 9) * 0.8f, 0.04f + btRand(it, 10) * 0.05f) }
private val BtWrack = List(70) { floatArrayOf(btRand(it, 11), btRand(it, 12), btRand(it, 13)) }

/** Clouds: x, y (fraction of the sky), width (fraction of screen), thickness dp, drift speed, color. */
private class BtCloud(val x: Float, val y: Float, val w: Float, val th: Float, val speed: Float, val c: Color)
private val BtClouds = listOf(
    BtCloud(0.08f, 0.30f, 0.34f, 7f, 0.0030f, Color(0x99B98AB0)),
    BtCloud(0.55f, 0.42f, 0.42f, 9f, 0.0022f, Color(0xB3F0A28E)),
    BtCloud(0.30f, 0.58f, 0.30f, 5f, 0.0040f, Color(0x99FFD2A0)),
    BtCloud(0.70f, 0.74f, 0.50f, 6f, 0.0018f, Color(0xB3E79C8C)),
    BtCloud(0.12f, 0.86f, 0.26f, 4f, 0.0035f, Color(0x80FFE2BC)),
)

/** Sea glass on the sand: x, y (fractions of the screen), size dp, color index, turn. */
private val BtGlass = listOf(
    floatArrayOf(0.04f, 0.712f, 9f, 0f, 0.2f), floatArrayOf(0.62f, 0.742f, 7f, 1f, 1.1f), floatArrayOf(0.55f, 0.862f, 10f, 2f, 2.3f),
    floatArrayOf(0.93f, 0.858f, 8f, 3f, 0.7f), floatArrayOf(0.70f, 0.884f, 6f, 4f, 1.9f), floatArrayOf(0.40f, 0.705f, 7f, 5f, 0.4f),
    floatArrayOf(0.26f, 0.886f, 8f, 0f, 2.8f), floatArrayOf(0.83f, 0.892f, 6f, 1f, 1.4f), floatArrayOf(0.10f, 0.895f, 7f, 2f, 0.9f),
    floatArrayOf(0.60f, 0.896f, 5f, 3f, 2.1f), floatArrayOf(0.97f, 0.742f, 7f, 4f, 0.1f), floatArrayOf(0.33f, 0.716f, 5f, 5f, 1.7f),
    floatArrayOf(0.015f, 0.860f, 6f, 1f, 2.6f), floatArrayOf(0.47f, 0.892f, 6f, 0f, 1.2f),
)
private val BtGlassColors = listOf(
    Color(0xFF9AD9C0), Color(0xFFE3A456), Color(0xFF4F7FD0), Color(0xFFE9F3EF), Color(0xFFB9A3D9), Color(0xFF5DB878),
)

/** How far up the beach the tide has come: 0 = far out, 1 = the deadline has passed. */
private fun btTide(q: Quest?, now: Long): Float {
    if (q == null) return 0.08f
    val d = q.due ?: return 0.08f
    val end = if (q.allDay) Time.ms(Time.date(d).plusDays(1)) else d
    val mins = (end - now) / 60_000f
    if (mins <= 0f) return 1f
    return (1f - mins / (12f * 60f)).coerceIn(0.08f, 0.95f)
}

/** Lays its child out at full size but only shows the top [p] of it: the scroll unrolling. */
private fun Modifier.unroll(p: () -> Float): Modifier = this.clipToBounds().layout { m, c ->
    val pl = m.measure(c)
    val h = (pl.height * p().coerceIn(0f, 1f)).roundToInt()
    layout(pl.width, h) { pl.placeRelative(0, 0) }
}

/**
 * Message in a Bottle: a dusk beach seen from down on the sand. Your character keeps a lantern
 * glow on the driftwood. Tap the bottle to uncork it and unroll what's due. The tide climbs the
 * beach toward the wrack line as the next deadline gets close. Throwing a bottle adds a quest.
 */
@Composable
fun BottleWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val name = s.companion.title
    val clock = rememberClock()
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    var tossing by rememberSaveable { mutableStateOf(false) }
    val throwP = remember { Animatable(0f) }
    fun throwBottle() {
        scope.launch {
            throwP.snapTo(0f)
            throwP.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing))
            throwP.snapTo(0f)
        }
    }
    val voice = rememberVoice { heard -> a.spill(heard); throwBottle() }

    val tide by animateFloatAsState(btTide(q, s.now), tween(2400, easing = FastOutSlowInEasing), label = "tide")
    val corkP by animateFloatAsState(if (open) 1f else 0f, tween(520, easing = FastOutSlowInEasing), label = "cork")
    val scrollP by animateFloatAsState(
        if (open) 1f else 0f,
        tween(if (open) 950 else 520, delayMillis = if (open) 280 else 0, easing = FastOutSlowInEasing),
        label = "scroll",
    )
    val glassCount = (5 + s.done.size).coerceAtMost(BtGlass.size)

    val josefin = TextStyle(fontFamily = Fonts.josefinSans, fontWeight = FontWeight.W700, color = BtInk)
    val due: Long? = q?.due
    val tideLabel = when {
        q == null -> "CALM SEAS · NOTHING DUE"
        due == null -> "SOMEDAY · NO TIDE FOR THIS ONE"
        q.shelf(s.now) == Shelf.OVERDUE -> "THE TIDE CAME IN · ${q.text.uppercase()}"
        else -> "HIGH TIDE ${q.shortDue().uppercase()} · ${q.text.uppercase()}"
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(BtSand)) {
        val w = maxWidth
        val h = maxHeight

        // ---- the scene
        Canvas(Modifier.fillMaxSize()) {
            btScene(clock.value, tide, glassCount, throwP.value)
        }

        // ---- the tide (deadline) label on the wrack line
        Text(
            tideLabel,
            style = josefin.copy(fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF5A4636)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .offset(x = 16.dp, y = h * BtTideMark - 19.dp)
                .width(w - 32.dp)
                .tap { open = true },
        )

        // ---- the bottle, half in the sand
        val bottleX = w * 0.05f
        val bottleY = h * 0.725f
        Canvas(
            Modifier
                .offset(x = bottleX, y = bottleY)
                .size(210.dp, 120.dp)
                .tap { open = !open }
        ) { btBottle(clock.value, corkP, open) }
        if (!open) {
            Text(
                if (s.open.isEmpty()) "tap the bottle" else "tap to uncork · ${s.open.size} note${if (s.open.size == 1) "" else "s"} inside",
                style = josefin.copy(fontSize = 11.sp, letterSpacing = 0.5.sp, color = Color(0xFF7A5E44)),
                modifier = Modifier.offset(x = bottleX + 24.dp, y = bottleY - 2.dp),
                maxLines = 1,
            )
        }

        // ---- your character, sitting on the driftwood with a lantern glow
        val charSize = 84.dp
        val charX = w * 0.79f - charSize / 2f
        val charY = h * 0.792f - charSize
        CharacterArt(
            size = charSize,
            style = ArtStyle.SHINY,
            modifier = Modifier.offset(x = charX, y = charY),
            level = voice.level,
            listening = voice.listening,
            onTap = { voice.toggle() },
        )
        if (voice.listening || voice.note.isNotBlank()) {
            Column(
                Modifier
                    .offset(x = w - 222.dp, y = charY - 74.dp)
                    .width(206.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BtParchment.copy(alpha = 0.96f))
                    .border(1.dp, Color(0x55704A20), RoundedCornerShape(12.dp))
                    .tap { voice.toggle() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    if (voice.listening) "listening…" else name,
                    style = josefin.copy(fontSize = 10.sp, letterSpacing = 1.sp, color = BtInkSoft),
                )
                Text(
                    if (voice.listening) voice.heard.ifBlank { "Say it and I'll throw the bottle." } else voice.note,
                    style = TextStyle(fontFamily = Fonts.gaegu, fontSize = 18.sp, lineHeight = 21.sp, color = BtInk),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ---- header
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 14.dp, top = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Dusk tide",
                    style = TextStyle(
                        fontFamily = Fonts.youngSerif, fontSize = 28.sp, color = Color(0xFFFFF1E2),
                        shadow = Shadow(Color(0x663A2A5A), Offset(0f, 3f), 8f),
                    ),
                )
                Text(
                    "${Time.time(s.now)} · " + if (s.open.isEmpty()) "no notes adrift" else "${s.open.size} note${if (s.open.size == 1) "" else "s"} adrift",
                    style = josefin.copy(fontSize = 12.sp, letterSpacing = 0.5.sp, color = Color(0xFFF7DCCB)),
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BottleChip("Tide chart") { a.openLog() }
                BottleChip("Settings") { a.openOptions() }
            }
        }

        // ---- the unrolled scroll
        if (scrollP > 0.001f) {
            Box(
                Modifier.fillMaxSize()
                    .background(BtNight.copy(alpha = 0.28f * scrollP))
                    .tap { open = false }
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 74.dp)
            ) {
                BottleRod(paper = false, thin = 0f)
                Box(Modifier.fillMaxWidth().unroll { scrollP }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .drawBehind { btParchment() }
                            .heightIn(max = h * 0.56f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
                    ) {
                        Text(
                            "WASHED ASHORE AT ${Time.time(s.now)}",
                            style = josefin.copy(fontSize = 10.sp, letterSpacing = 2.sp, color = BtInkSoft),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            s.line,
                            style = TextStyle(fontFamily = Fonts.gaegu, fontSize = 20.sp, lineHeight = 24.sp, color = BtInk),
                        )
                        Text(
                            "— $name",
                            style = TextStyle(fontFamily = Fonts.gaegu, fontStyle = FontStyle.Italic, fontSize = 17.sp, color = BtInkSoft, textAlign = TextAlign.End),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                            val p = Path()
                            val y = size.height / 2f
                            p.moveTo(0f, y)
                            var x = 0f
                            val step = 12.dp.toPx()
                            while (x < size.width) {
                                p.quadraticBezierTo(x + step / 2f, y + (if ((x / step).toInt() % 2 == 0) -3f else 3f) * density, x + step, y)
                                x += step
                            }
                            drawPath(p, Color(0x668A6A3A), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
                        }
                        if (s.open.isEmpty()) {
                            Text(
                                "Nothing is due. The sea is calm tonight. Throw a bottle if something's on your mind.",
                                style = TextStyle(fontFamily = Fonts.gaegu, fontSize = 18.sp, lineHeight = 22.sp, color = BtInkSoft),
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        for (item in s.open.take(8)) {
                            BottleRow(
                                item, s.now,
                                onDone = { a.done(item) },
                                onLater = { a.snooze(item, 60) },
                                onOpen = { a.openQuest(item) },
                            )
                        }
                        if (s.open.size > 8) {
                            Text(
                                "+ ${s.open.size - 8} more in the tide chart",
                                style = josefin.copy(fontSize = 12.sp, color = BtInkSoft),
                                modifier = Modifier.padding(top = 6.dp).tap { a.openLog() },
                            )
                        }
                        Text(
                            "Roll it up",
                            style = josefin.copy(fontSize = 13.sp, color = BtInkSoft, textAlign = TextAlign.End),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).tap { open = false },
                        )
                    }
                }
                BottleRod(paper = true, thin = scrollP)
            }
        }

        // ---- throw a new bottle
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
        ) {
            if (tossing) {
                SpillInput(
                    onSpill = { a.spill(it); tossing = false; throwBottle() },
                    textStyle = TextStyle(fontFamily = Fonts.gaegu, fontSize = 21.sp, color = BtInk),
                    placeholder = "pay rent by friday",
                    placeholderColor = Color(0xFF9A8A6A),
                    cursor = BtDeep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(BtParchment)
                        .border(1.dp, Color(0x55704A20), RoundedCornerShape(6.dp))
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                ) { submit ->
                    Text(
                        "Cork & throw",
                        style = josefin.copy(fontSize = 13.sp, color = BtParchment),
                        modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF9A6A3A))
                            .tap(submit).padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(BtDeep)
                        .tap { tossing = !tossing }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (tossing) "Keep it ashore" else "Throw a bottle", style = josefin.copy(fontSize = 15.sp, color = BtParchment), maxLines = 1)
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(BtParchment.copy(alpha = 0.92f))
                        .border(1.dp, Color(0x55704A20), RoundedCornerShape(24.dp))
                        .tap { open = !open }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (open) "Roll it up" else "Read the note", style = josefin.copy(fontSize = 15.sp), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun BottleRow(q: Quest, now: Long, onDone: () -> Unit, onLater: () -> Unit, onOpen: () -> Unit) {
    val late = q.shelf(now) == Shelf.OVERDUE
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = 4.dp).size(20.dp).clip(CircleShape)
                .border(1.5.dp, BtInk.copy(alpha = 0.7f), CircleShape)
                .tap(onDone)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).tap(onOpen)) {
            Text(
                q.text,
                style = TextStyle(fontFamily = Fonts.gaegu, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 22.sp, color = BtInk),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                q.dueText() + if (late) " · the tide's in" else "",
                style = TextStyle(fontFamily = Fonts.josefinSans, fontWeight = FontWeight.W700, fontSize = 11.sp, color = if (late) Color(0xFFA8432C) else BtInkSoft),
                maxLines = 1,
            )
        }
        Text(
            "later",
            style = TextStyle(fontFamily = Fonts.josefinSans, fontWeight = FontWeight.W700, fontSize = 12.sp, color = BtInkSoft),
            modifier = Modifier.tap(onLater).padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

/** A scroll end: a wooden dowel on top, the rolled-up paper (thinning as it unrolls) below. */
@Composable
private fun BottleRod(paper: Boolean, thin: Float) {
    Canvas(Modifier.fillMaxWidth().height(20.dp)) {
        val cy = size.height / 2f
        if (paper) {
            val r = (11f - 4f * thin).dp.toPx()
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFFBF0D6), Color(0xFFE6CFA0), Color(0xFFB8975E)), startY = cy - r, endY = cy + r),
                topLeft = Offset(4.dp.toPx(), cy - r), size = Size(size.width - 8.dp.toPx(), r * 2f), cornerRadius = CornerRadius(r),
            )
            // the spiral at each end of the roll
            for (x in listOf(4.dp.toPx() + r, size.width - 4.dp.toPx() - r)) {
                drawCircle(Color(0xFFD9BE8A), radius = r * 0.92f, center = Offset(x, cy))
                drawArc(Color(0x998A6A3A), 0f, 300f, false, Offset(x - r * 0.6f, cy - r * 0.6f), Size(r * 1.2f, r * 1.2f), style = Stroke(1.dp.toPx()))
                drawArc(Color(0x998A6A3A), 90f, 260f, false, Offset(x - r * 0.3f, cy - r * 0.3f), Size(r * 0.6f, r * 0.6f), style = Stroke(1.dp.toPx()))
            }
        } else {
            val r = 7.dp.toPx()
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFD9A06A), Color(0xFF8A5A32), Color(0xFF5E3A1E)), startY = cy - r, endY = cy + r),
                topLeft = Offset(10.dp.toPx(), cy - r), size = Size(size.width - 20.dp.toPx(), r * 2f), cornerRadius = CornerRadius(r),
            )
            for (x in listOf(8.dp.toPx(), size.width - 8.dp.toPx())) {
                drawCircle(Color(0xFF5E3A1E), radius = 9.dp.toPx(), center = Offset(x, cy))
                drawCircle(Color(0xFFB07A48), radius = 4.dp.toPx(), center = Offset(x - 2.dp.toPx(), cy - 2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun BottleChip(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = TextStyle(fontFamily = Fonts.josefinSans, fontWeight = FontWeight.W700, fontSize = 12.sp, color = Color(0xFFFFF1E2)),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x40FFFFFF))
            .border(1.dp, Color(0x66FFF1E2), RoundedCornerShape(14.dp))
            .tap(onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Paper: warm base, darker edges, a couple of old water stains, shade under the rod and at the curl. */
private fun DrawScope.btParchment() {
    drawRect(BtParchment)
    val edge = Color(0x2EA0703A)
    drawRect(Brush.horizontalGradient(listOf(edge, Color.Transparent), startX = 0f, endX = size.width * 0.1f), size = Size(size.width * 0.1f, size.height))
    drawRect(
        Brush.horizontalGradient(listOf(Color.Transparent, edge), startX = size.width * 0.9f, endX = size.width),
        topLeft = Offset(size.width * 0.9f, 0f), size = Size(size.width * 0.1f, size.height),
    )
    drawCircle(Brush.radialGradient(listOf(Color(0x1EA0703A), Color.Transparent), center = Offset(size.width * 0.82f, 60.dp.toPx()), radius = 46.dp.toPx()), radius = 46.dp.toPx(), center = Offset(size.width * 0.82f, 60.dp.toPx()))
    drawCircle(Brush.radialGradient(listOf(Color(0x16A0703A), Color.Transparent), center = Offset(size.width * 0.14f, size.height * 0.7f), radius = 34.dp.toPx()), radius = 34.dp.toPx(), center = Offset(size.width * 0.14f, size.height * 0.7f))
    drawRect(Brush.verticalGradient(listOf(Color(0x40000000), Color.Transparent), startY = 0f, endY = 10.dp.toPx()), size = Size(size.width, 10.dp.toPx()))
    val curl = 16.dp.toPx()
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, Color(0x33704A20)), startY = size.height - curl, endY = size.height),
        topLeft = Offset(0f, size.height - curl), size = Size(size.width, curl),
    )
}

/** The whole beach: sky, sun, clouds, headland, sea and waves, the swash, sand, wrack line, glass, driftwood. */
private fun DrawScope.btScene(t: Float, tide: Float, glassCount: Int, throwP: Float) {
    val w = size.width
    val h = size.height
    val hz = h * BtHorizon
    val shore = h * (BtShoreLow + BtShoreRise * tide)
    val dp = density

    // sky
    drawRect(
        Brush.verticalGradient(0f to BtSkyTop, 0.38f to BtLavender, 0.7f to BtRose, 0.9f to BtPeach, 1f to BtGlow, startY = 0f, endY = hz),
        size = Size(w, hz + 1f),
    )
    for (st in BtStars) {
        val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(t * 1.7f + st.phase))
        drawCircle(Color(0xFFFFF4E8), radius = st.r * dp, center = Offset(st.x * w, st.y * h), alpha = tw * (1f - st.y * 3.5f).coerceIn(0.15f, 1f))
    }
    // sun sitting on the horizon
    val sunX = w * 0.64f
    val sunR = w * 0.075f
    val sun = Offset(sunX, hz + sunR * 0.18f)
    drawCircle(Brush.radialGradient(listOf(Color(0x77FFD9A8), Color(0x00FFD9A8)), center = sun, radius = sunR * 3.4f), radius = sunR * 3.4f, center = sun)
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFF0C8), Color(0xFFFFB98A)), startY = sun.y - sunR, endY = sun.y + sunR), radius = sunR, center = sun)
    // long dusk cloud streaks drifting
    for (c in BtClouds) {
        val span = 1.4f
        val x = (((c.x + t * c.speed) % span) - 0.2f) * w
        val th = c.th * dp
        drawRoundRect(c.c, topLeft = Offset(x, c.y * hz), size = Size(c.w * w, th), cornerRadius = CornerRadius(th / 2f))
        drawRoundRect(c.c.copy(alpha = c.c.alpha * 0.5f), topLeft = Offset(x + c.w * w * 0.2f, c.y * hz + th * 0.9f), size = Size(c.w * w * 0.55f, th * 0.6f), cornerRadius = CornerRadius(th / 2f))
    }
    // headland and its lighthouse
    val head = Path().apply {
        moveTo(0f, hz); lineTo(0f, hz - 0.030f * h); lineTo(0.05f * w, hz - 0.042f * h); lineTo(0.10f * w, hz - 0.050f * h)
        lineTo(0.15f * w, hz - 0.046f * h); lineTo(0.20f * w, hz - 0.030f * h); lineTo(0.26f * w, hz - 0.012f * h); lineTo(0.30f * w, hz); close()
    }
    drawPath(head, Color(0xFF5F527F))
    val lhX = 0.105f * w
    val lhTop = hz - 0.050f * h - 0.032f * h
    drawRect(Color(0xFFEDE3D6), Offset(lhX - 3f * dp, lhTop), Size(6f * dp, 0.032f * h))
    drawRect(Color(0xFFB0485A), Offset(lhX - 3f * dp, lhTop + 0.012f * h), Size(6f * dp, 3f * dp))
    val beam = 0.5f + 0.5f * sin(t * 2.2f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE9A8), Color(0x00FFE9A8)), center = Offset(lhX, lhTop), radius = 14f * dp), radius = 14f * dp, center = Offset(lhX, lhTop), alpha = 0.3f + 0.7f * beam)

    // sea body
    drawRect(
        Brush.verticalGradient(listOf(BtDeep, BtTeal, BtShallow), startY = hz, endY = shore + 0.02f * h),
        topLeft = Offset(0f, hz), size = Size(w, shore - hz + 0.03f * h),
    )
    drawLine(Color(0x88F5C8A8), Offset(0f, hz), Offset(w, hz), 1f * dp)
    // the sun's glitter path, widening toward us
    for (j in 0 until 22) {
        val y = hz + (j + 0.5f) / 22f * (shore - hz) * 0.85f
        val spread = 0.02f * w + j * 0.006f * w
        for (k in 0 until 3) {
            val x = sunX + spread * (btRand(j * 3 + k, 21) * 2f - 1f)
            val len = (6f + j * 0.6f) * dp
            val al = 0.2f + 0.6f * (0.5f + 0.5f * sin(t * 2.6f + j * 1.3f + k * 2.1f))
            drawLine(Color(0xFFFFD9AE), Offset(x - len / 2f, y), Offset(x + len / 2f, y), (1.2f + j * 0.06f) * dp, cap = StrokeCap.Round, alpha = al)
        }
    }
    // layered rolling waves, far to near, each with a broken foam line
    val waveColors = listOf(Color(0xFF22606A), Color(0xFF2A6F76), Color(0xFF347F80), Color(0xFF3F8E89))
    for (i in 0 until 4) {
        val y0 = hz + 0.03f * h + (shore - 0.03f * h - (hz + 0.03f * h)) * (i / 3f)
        val amp = (1.5f + i * 1.8f) * dp
        val lambda = w * (0.22f + i * 0.1f)
        val speed = 0.6f + i * 0.25f
        val crest = Path()
        val body = Path()
        val stepX = 8f * dp
        val n = (w / stepX).toInt() + 2
        for (k in 0..n) {
            val x = k * stepX
            val y = y0 + amp * sin(2f * PI.toFloat() * x / lambda + t * speed + i) + 0.35f * amp * sin(2f * PI.toFloat() * x / (0.47f * lambda) - t * speed * 1.4f)
            if (k == 0) { crest.moveTo(x, y); body.moveTo(x, y) } else { crest.lineTo(x, y); body.lineTo(x, y) }
        }
        body.lineTo(n * stepX, shore + 0.02f * h)
        body.lineTo(0f, shore + 0.02f * h)
        body.close()
        drawPath(body, waveColors[i])
        drawPath(
            crest, Color.White.copy(alpha = 0.16f + i * 0.13f),
            style = Stroke(
                width = (1f + i * 0.45f) * dp, cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf((10f + i * 9f) * dp, (16f - i * 2f) * dp), -t * 14f * dp * (i + 1)),
            ),
        )
    }

    // sand, top edge where the water sits
    val sandTop = Path().apply {
        moveTo(0f, shore + 0.004f * h)
        cubicTo(w * 0.3f, shore - 0.004f * h, w * 0.6f, shore + 0.006f * h, w, shore - 0.002f * h)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(sandTop, Brush.verticalGradient(0f to BtSand, 0.6f to Color(0xFFE4CBA3), 1f to BtSandLow, startY = shore, endY = h))
    // wet band, with the sky and sun mirrored in it
    val wetH = 0.06f * h
    drawRect(Brush.verticalGradient(listOf(BtWet.copy(alpha = 0.8f), BtWet.copy(alpha = 0f)), startY = shore, endY = shore + wetH), topLeft = Offset(0f, shore), size = Size(w, wetH))
    drawRect(Brush.verticalGradient(listOf(Color(0x449C86B8), Color(0x009C86B8)), startY = shore, endY = shore + wetH), topLeft = Offset(0f, shore), size = Size(w, wetH))
    drawOval(
        Brush.verticalGradient(listOf(Color(0x99FFD2A6), Color(0x00FFD2A6)), startY = shore, endY = shore + wetH),
        topLeft = Offset(sunX - w * 0.06f, shore), size = Size(w * 0.12f, wetH),
    )

    // sand texture: speckles and wind ripples
    val sandH = h - shore
    for ((i, sp) in BtSpeckles.withIndex()) {
        val y = shore + wetH * 0.6f + sp[1] * (sandH - wetH * 0.6f)
        val c = if (i % 3 == 0) Color.White else Color(0xFF9C7E58)
        drawCircle(c, radius = (0.6f + sp[2] * 0.8f) * dp, center = Offset(sp[0] * w, y), alpha = 0.22f + sp[2] * 0.2f)
    }
    for (r in BtRipples) {
        val y = shore + wetH + r[1] * (sandH - wetH)
        val x = r[0] * w
        val len = r[2] * w
        val p = Path().apply { moveTo(x, y); quadraticBezierTo(x + len / 2f, y - 3f * dp, x + len, y) }
        drawPath(p, Color(0x40A08060), style = Stroke(width = 1.2f * dp, cap = StrokeCap.Round))
    }

    // the wrack line: where the tide reaches at the deadline
    val mark = h * BtTideMark
    for (wr in BtWrack) {
        val x = wr[0] * w
        val y = mark + (wr[1] - 0.5f) * 6f * dp
        if (wr[2] < 0.12f) {
            drawOval(Color(0xFFF6EDE0), Offset(x, y - 2f * dp), Size(5f * dp, 3.5f * dp))
        } else {
            drawLine(Color(0xFF5E5B34), Offset(x, y), Offset(x + (3f + wr[2] * 7f) * dp, y + (wr[1] - 0.5f) * 3f * dp), (1f + wr[2]) * dp, cap = StrokeCap.Round, alpha = 0.55f)
        }
    }

    // the swash: a thin sheet of water running up the sand and back, with a lace of foam
    val surge = 0.5f - 0.5f * cos(2f * PI.toFloat() * t / 6.5f)
    val reach = shore + (0.012f + 0.03f * surge) * h
    val edgePts = List(61) { k ->
        val x = w * k / 60f
        Offset(x, reach + 0.006f * h * sin(x / w * 2f * PI.toFloat() * 3f + t * 0.5f) + 0.003f * h * sin(x / w * 2f * PI.toFloat() * 7.3f - t))
    }
    val edge = Path()
    for ((k, pt) in edgePts.withIndex()) { if (k == 0) edge.moveTo(pt.x, pt.y) else edge.lineTo(pt.x, pt.y) }
    val sheet = Path()
    sheet.moveTo(0f, shore - 0.004f * h)
    sheet.lineTo(w, shore - 0.004f * h)
    for (pt in edgePts.asReversed()) sheet.lineTo(pt.x, pt.y)
    sheet.close()
    drawPath(sheet, Brush.verticalGradient(listOf(Color(0xB36FB3A6), Color(0x596FB3A6)), startY = shore, endY = reach + 0.01f * h))
    drawPath(edge, Color.White.copy(alpha = 0.55f + 0.3f * surge), style = Stroke(width = 2.2f * dp, cap = StrokeCap.Round))
    drawPath(
        edge, Color.White.copy(alpha = 0.35f),
        style = Stroke(width = 1f * dp, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * dp, 7f * dp), t * 9f * dp)),
    )
    for (k in 0 until 18) {
        val bx = btRand(k, 31) * w
        val by = reach - (2f + btRand(k, 32) * 6f) * dp
        drawCircle(Color.White, radius = (0.8f + btRand(k, 33)) * dp, center = Offset(bx, by), alpha = 0.5f * (0.4f + 0.6f * surge))
    }

    // the lantern glow your character casts on the sand
    val glowC = Offset(w * 0.79f, h * 0.80f)
    val glowR = w * (0.2f + 0.012f * sin(t * 1.4f))
    drawOval(
        Brush.radialGradient(listOf(Color(0x66FFD9A0), Color(0x00FFD9A0)), center = glowC, radius = glowR),
        topLeft = Offset(glowC.x - glowR, glowC.y - glowR * 0.45f), size = Size(glowR * 2f, glowR * 0.9f),
    )
    // driftwood
    val logL = w * 0.60f
    val logR = w * 0.97f
    val logY = h * 0.792f
    val logT = 0.022f * h
    drawOval(Color(0x33402A10), Offset(logL, logY + logT * 0.6f), Size(logR - logL, logT * 0.9f))
    val log = Path().apply {
        moveTo(logL, logY + logT * 0.2f)
        cubicTo(logL + (logR - logL) * 0.3f, logY - logT * 0.15f, logL + (logR - logL) * 0.7f, logY - logT * 0.05f, logR, logY - logT * 0.3f)
        lineTo(logR, logY + logT * 0.65f)
        cubicTo(logL + (logR - logL) * 0.7f, logY + logT * 0.95f, logL + (logR - logL) * 0.3f, logY + logT * 1.0f, logL, logY + logT * 1.05f)
        close()
    }
    drawPath(log, Brush.verticalGradient(listOf(Color(0xFFB59C84), Color(0xFF8C7462), Color(0xFF5E4A3A)), startY = logY - logT * 0.3f, endY = logY + logT))
    drawOval(Color(0xFFD9C3A2), Offset(logR - logT * 0.45f, logY - logT * 0.3f), Size(logT * 0.9f, logT * 0.95f))
    drawOval(Color(0xFFB59C84), Offset(logR - logT * 0.25f, logY - logT * 0.05f), Size(logT * 0.45f, logT * 0.45f), style = Stroke(1f * dp))
    for (k in 0 until 4) {
        val gx = logL + (logR - logL) * (0.12f + k * 0.2f)
        drawLine(Color(0x665E4A3A), Offset(gx, logY + logT * 0.1f), Offset(gx + logT * 1.6f, logY + logT * 0.2f), 1f * dp)
    }
    drawLine(Color(0xFF8C7462), Offset(logL + (logR - logL) * 0.25f, logY), Offset(logL + (logR - logL) * 0.2f, logY - logT * 1.1f), 2.4f * dp, cap = StrokeCap.Round)

    // sea glass: one more piece for every quest you've finished
    for (g in BtGlass.take(glassCount)) {
        val c = Offset(g[0] * w, g[1] * h)
        val r = g[2] * dp
        val col = BtGlassColors[g[3].toInt() % BtGlassColors.size]
        drawOval(Color(0x26402A10), Offset(c.x - r, c.y + r * 0.35f), Size(r * 2.1f, r * 0.8f))
        val piece = Path()
        for (k in 0 until 6) {
            val ang = g[4] + k * PI.toFloat() / 3f
            val rr = r * (0.75f + 0.25f * btRand(k + (g[0] * 100).toInt(), 41))
            val px = c.x + cos(ang) * rr
            val py = c.y + sin(ang) * rr * 0.62f
            if (k == 0) piece.moveTo(px, py) else piece.lineTo(px, py)
        }
        piece.close()
        drawPath(piece, col, alpha = 0.82f)
        drawPath(piece, Color.White, alpha = 0.35f, style = Stroke(width = 0.8f * dp))
        drawCircle(Color.White, radius = r * 0.18f, center = Offset(c.x - r * 0.3f, c.y - r * 0.2f), alpha = 0.75f)
    }

    // a bottle you just threw, arcing out to sea
    if (throwP > 0f && throwP < 1f) {
        val start = Offset(w * 0.5f, h * 0.93f)
        val end = Offset(w * 0.56f, hz + 0.07f * h)
        val p = throwP.coerceAtMost(0.86f) / 0.86f
        val pos = Offset(start.x + (end.x - start.x) * p, start.y + (end.y - start.y) * p - sin(p * PI.toFloat()) * 0.22f * h)
        if (throwP < 0.86f) {
            val sz = (1f - 0.65f * p)
            withTransform({ rotate(p * 720f, pos) }) {
                drawRoundRect(Color(0xCC3E8E6A), Offset(pos.x - 12f * dp * sz, pos.y - 4f * dp * sz), Size(24f * dp * sz, 8f * dp * sz), CornerRadius(4f * dp * sz))
                drawRect(Color(0xFFB0824A), Offset(pos.x + 10f * dp * sz, pos.y - 2.5f * dp * sz), Size(4f * dp * sz, 5f * dp * sz))
            }
        } else {
            val sp = (throwP - 0.86f) / 0.14f
            for (k in 0 until 2) {
                val rr = (6f + sp * 22f + k * 8f) * dp
                drawOval(Color.White.copy(alpha = (1f - sp) * 0.8f), Offset(end.x - rr, end.y - rr * 0.3f), Size(rr * 2f, rr * 0.6f), style = Stroke(1.5f * dp))
            }
        }
    }
}

/** The bottle lying half-buried, glass catching the dusk. [cork] 0..1 pops the cork out onto the sand. */
private fun DrawScope.btBottle(t: Float, cork: Float, open: Boolean) {
    val k = size.width / 210f
    // invitation glow while it's still corked
    if (!open) {
        val pulse = 0.5f + 0.5f * sin(t * 2f)
        val c = Offset(100f * k, 66f * k)
        drawOval(
            Brush.radialGradient(listOf(Color(0x55FFD9AE), Color(0x00FFD9AE)), center = c, radius = 110f * k),
            topLeft = Offset(c.x - 110f * k, c.y - 50f * k), size = Size(220f * k, 100f * k), alpha = 0.5f + 0.5f * pulse,
        )
    }
    drawOval(Color(0x38402A10), Offset(18f * k, 92f * k), Size(172f * k, 16f * k))
    withTransform({
        translate(14f * k, 34f * k)
        rotate(-10f, Offset(90f * k, 32f * k))
    }) {
        val glass = Path().apply {
            moveTo(10f * k, 18f * k)
            cubicTo(10f * k, 6f * k, 22f * k, 2f * k, 40f * k, 2f * k)
            lineTo(112f * k, 2f * k)
            cubicTo(126f * k, 2f * k, 132f * k, 12f * k, 148f * k, 15f * k)
            lineTo(162f * k, 17f * k)
            lineTo(162f * k, 45f * k)
            lineTo(148f * k, 47f * k)
            cubicTo(132f * k, 50f * k, 126f * k, 60f * k, 112f * k, 60f * k)
            lineTo(40f * k, 60f * k)
            cubicTo(22f * k, 60f * k, 10f * k, 56f * k, 10f * k, 44f * k)
            close()
        }
        drawPath(glass, Brush.verticalGradient(0f to Color(0xD966AE8E), 0.45f to Color(0xCC2F7A5C), 1f to Color(0xE61C4F3C), startY = 2f * k, endY = 60f * k))
        // dusk sky caught along the top of the glass
        val sky = Path().apply {
            moveTo(18f * k, 12f * k)
            cubicTo(26f * k, 6f * k, 40f * k, 6f * k, 60f * k, 6f * k)
            lineTo(112f * k, 6f * k)
            cubicTo(124f * k, 6f * k, 130f * k, 14f * k, 146f * k, 19f * k)
            lineTo(146f * k, 23f * k)
            cubicTo(128f * k, 19f * k, 120f * k, 13f * k, 104f * k, 14f * k)
            lineTo(30f * k, 16f * k)
            close()
        }
        drawPath(sky, Brush.horizontalGradient(listOf(Color(0x66C9A6D0), Color(0x88FFC9A8)), startX = 18f * k, endX = 146f * k))
        // the rolled note inside, until it's taken out
        if (cork < 1f) {
            val al = 1f - cork
            drawRoundRect(Color(0xFFEFE2C2).copy(alpha = 0.92f * al), Offset(42f * k, 18f * k), Size(76f * k, 28f * k), CornerRadius(14f * k))
            drawLine(Color(0xFFB08A50).copy(alpha = al), Offset(78f * k, 18f * k), Offset(78f * k, 46f * k), 2.5f * k)
            val ink = Color(0xFF8A7A5A).copy(alpha = al)
            drawLine(ink, Offset(50f * k, 27f * k), Offset(72f * k, 27f * k), 1.2f * k)
            drawLine(ink, Offset(50f * k, 33f * k), Offset(70f * k, 33f * k), 1.2f * k)
            drawLine(ink, Offset(84f * k, 30f * k), Offset(110f * k, 30f * k), 1.2f * k)
        }
        drawPath(glass, Color(0xB3204A38), style = Stroke(width = 1.6f * k))
        // thick base and lip
        drawLine(Color(0x8812362A), Offset(11f * k, 20f * k), Offset(11f * k, 42f * k), 3f * k, cap = StrokeCap.Round)
        drawRect(Color(0xCC2A5E48), Offset(158f * k, 15f * k), Size(5f * k, 32f * k))
        // highlights
        val hi = Path().apply { moveTo(22f * k, 9f * k); cubicTo(34f * k, 5f * k, 60f * k, 5f * k, 108f * k, 5f * k) }
        drawPath(hi, Color.White.copy(alpha = 0.75f), style = Stroke(width = 2.6f * k, cap = StrokeCap.Round))
        drawLine(Color.White.copy(alpha = 0.35f), Offset(30f * k, 52f * k), Offset(96f * k, 53f * k), 1.6f * k, cap = StrokeCap.Round)
        // a twinkle on the glass
        val tw = (0.5f + 0.5f * sin(t * 3.1f)).let { it * it }
        val sc = Offset(40f * k, 9f * k)
        drawLine(Color.White.copy(alpha = tw), Offset(sc.x - 7f * k * tw, sc.y), Offset(sc.x + 7f * k * tw, sc.y), 1.4f * k, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = tw), Offset(sc.x, sc.y - 7f * k * tw), Offset(sc.x, sc.y + 7f * k * tw), 1.4f * k, cap = StrokeCap.Round)
        // the cork: in the neck, or popped out onto the sand
        val home = Offset(170f * k, 31f * k)
        val rest = Offset(186f * k, 64f * k)
        val cc = Offset(home.x + (rest.x - home.x) * cork, home.y + (rest.y - home.y) * cork - sin(cork * PI.toFloat()) * 38f * k)
        withTransform({ rotate(cork * 105f, cc) }) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color(0xFFC89A62), Color(0xFF9A6E3E)), startX = cc.x - 7f * k, endX = cc.x + 7f * k),
                Offset(cc.x - 7f * k, cc.y - 12f * k), Size(14f * k, 24f * k), CornerRadius(3f * k),
            )
            for (d in 0 until 5) {
                drawCircle(Color(0x667A4E24), radius = 0.9f * k, center = Offset(cc.x + ((d * 37) % 9 - 4) * k, cc.y + ((d * 53) % 19 - 9) * k))
            }
        }
    }
    // sand heaped against the bottom of the bottle
    val mound = Path().apply {
        moveTo(6f * k, 104f * k)
        cubicTo(26f * k, 84f * k, 66f * k, 82f * k, 104f * k, 92f * k)
        cubicTo(122f * k, 97f * k, 136f * k, 104f * k, 148f * k, 108f * k)
        lineTo(6f * k, 108f * k)
        close()
    }
    drawPath(mound, Brush.verticalGradient(listOf(Color(0xFFEFDCBC), BtSand, Color(0xFFE0C8A0)), startY = 84f * k, endY = 108f * k))
    drawPath(
        Path().apply { moveTo(20f * k, 95f * k); cubicTo(44f * k, 86f * k, 76f * k, 85f * k, 100f * k, 92f * k) },
        Color.White.copy(alpha = 0.4f), style = Stroke(width = 1.4f * k, cap = StrokeCap.Round),
    )
}

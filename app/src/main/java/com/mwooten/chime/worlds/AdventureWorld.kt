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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap
import kotlin.math.floor
import kotlin.math.min

private val AdvNight = Color(0xFF0E1A10)
private val AdvNavy = Color(0xFF10183A)
private val AdvGold = Color(0xFFFFD23A)

/** One pixel-art rectangle on the 130-wide map grid. */
private class AdvPx(val x: Float, val y: Float, val w: Float, val h: Float, val c: Long)

private fun advPx(x: Number, y: Number, w: Number, h: Number, c: Long) = AdvPx(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), c)

/** The overworld, minus grass base and path (those stretch with the screen). */
private val AdvMap: List<AdvPx> = buildList {
    // grass tufts
    listOf(8 to 60, 22 to 90, 96 to 70, 110 to 120, 18 to 150, 70 to 170, 40 to 128, 102 to 160).forEach { (x, y) -> add(advPx(x, y, 2, 1, 0xFF4A8A3C)) }
    listOf(9 to 59, 23 to 89, 97 to 69, 111 to 119).forEach { (x, y) -> add(advPx(x, y, 1, 1, 0xFF7AC060)) }
    // east branch of the dirt path
    add(advPx(68, 96, 62, 14, 0xFFC8A466))
    listOf(54 to 60, 62 to 80, 56 to 130, 80 to 100, 100 to 104).forEach { (x, y) -> add(advPx(x, y, 2, 1, 0xFFB08A50)) }
    // your house
    add(advPx(34, 14, 52, 8, 0xFFA8402A)); add(advPx(30, 22, 60, 6, 0xFF8A3020))
    add(advPx(36, 28, 48, 16, 0xFFE8D8B0)); add(advPx(56, 34, 8, 10, 0xFF6A4020))
    add(advPx(40, 31, 7, 6, 0xFF7AC0E0)); add(advPx(73, 31, 7, 6, 0xFF7AC0E0))
    // trees
    for ((tx, ty) in listOf(4 to 20, 104 to 20)) {
        add(advPx(tx, ty, 18, 16, 0xFF2A6A2A)); add(advPx(tx + 2, ty - 2, 14, 2, 0xFF2A6A2A))
        add(advPx(tx + 2, ty + 2, 4, 4, 0xFF3A8A3A)); add(advPx(tx + 7, ty + 16, 4, 5, 0xFF6A4020))
    }
    for ((tx, ty) in listOf(6 to 140, 96 to 150)) {
        add(advPx(tx, ty, 18, 16, 0xFF2A6A2A)); add(advPx(tx + 2, ty + 2, 4, 4, 0xFF3A8A3A)); add(advPx(tx + 7, ty + 16, 4, 5, 0xFF6A4020))
    }
    // pond
    add(advPx(14, 88, 26, 16, 0xFF3A7AC8)); add(advPx(16, 86, 22, 2, 0xFF3A7AC8)); add(advPx(16, 104, 22, 2, 0xFF3A7AC8))
    add(advPx(20, 92, 6, 1, 0xFFA8D8FF))
    // the mailbox: the quest objective
    add(advPx(112, 88, 2, 12, 0xFF6A4020)); add(advPx(108, 82, 10, 7, 0xFF3A5AB0))
    add(advPx(117, 78, 1, 5, 0xFFE2533A)); add(advPx(118, 78, 3, 2, 0xFFE2533A))
}

private val AdvArrow: List<AdvPx> = listOf(
    advPx(111, 70, 4, 4, 0xFFFFE24A), advPx(110, 74, 6, 1, 0xFFFFE24A), advPx(111, 75, 4, 1, 0xFFFFE24A), advPx(112, 76, 2, 1, 0xFFFFE24A),
)

/** Draws a grid rect snapped to whole pixels so neighbors never show seams. */
private fun DrawScope.advRect(x: Float, y: Float, w: Float, h: Float, c: Color, k: Float, ox: Float, oy: Float) {
    val l = floor(ox + x * k); val t = floor(oy + y * k)
    val r = floor(ox + (x + w) * k); val b = floor(oy + (y + h) * k)
    drawRect(c, Offset(l, t), Size(r - l, b - t))
}

@Composable
fun AdventureWorld(s: WorldState, a: WorldActions) {
    val f = s.focus
    var writing by remember { mutableStateOf(false) }
    val clock = rememberClock()
    val shadowPx = with(LocalDensity.current) { 2.dp.toPx() }
    val hud = TextStyle(fontFamily = Fonts.pixelifySans, color = Color.White, shadow = Shadow(Color.Black, Offset(shadowPx, shadowPx), 0f))
    // tap the hero to talk: whatever you say becomes a new quest
    val voice = rememberVoice { heard -> a.spill(heard) }

    // daylight = hours until the focus quest is due
    val due: Long? = f?.due
    val allDay = f?.allDay == true
    val deadline: Long? = if (due == null) null else if (allDay) Time.ms(Time.date(due).plusDays(1)) else due
    val minsLeft: Long? = if (deadline == null) null else (deadline - s.now) / 60_000
    val pips = if (minsLeft == null) 6 else if (minsLeft <= 0) 0 else ((minsLeft + 59) / 60).toInt().coerceIn(0, 6)
    val target = if (due == null) "" else if (allDay) "sundown" else Time.time(due)
    val daylightText = when {
        f == null -> "the day is yours"
        minsLeft == null -> "no sunset set"
        minsLeft < 0 -> "past $target"
        minsLeft < 60 -> "$minsLeft min till $target"
        minsLeft < 120 -> "1 hr till $target"
        else -> "${minsLeft / 60} hrs till $target"
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(AdvNight)) {
        // map grid -> dp, matching the Canvas math below
        val kDp = maxWidth / 130f
        val shift = (maxHeight - 250.dp) - kDp * 140f
        val oyDp = if (shift < 0.dp) shift else 0.dp
        // the overworld, scaled to the screen width with hard pixel edges
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / 130f
            // on short screens slide the map up so the hero stays above the dialog
            val oy = min(0f, (size.height - 250.dp.toPx()) - 140f * k)
            val gridH = (size.height - oy) / k
            val bottom = 200f
            advRect(0f, 0f, 130f, bottom, Color(0xFF5AA04A), k, 0f, oy)
            advRect(52f, 40f, 16f, bottom - 40f, Color(0xFFC8A466), k, 0f, oy)
            if (gridH > bottom) advRect(0f, bottom, 130f, gridH - bottom, AdvNight, k, 0f, oy)
            AdvMap.forEach { advRect(it.x, it.y, it.w, it.h, Color(it.c), k, 0f, oy) }

            val t = clock.value
            // the hero's shadow on the grass (the hero itself is the user's character, drawn on top)
            advRect(55f, 132f, 10f, 2f, Color(0x55000000), k, 0f, oy)
            // quest arrow over the mailbox
            if (f != null) {
                val drop = if (floor(t / 0.4f).toInt() % 2 == 1) 1f else 0f
                AdvArrow.forEach { advRect(it.x, it.y + drop, it.w, it.h, Color(it.c), k, 0f, oy) }
            }
        }

        // the hero: the user's own character as a chunky sprite, standing on the path
        val heroSize = kDp * 22f
        val hop = if (floor(clock.value / 0.25f).toInt() % 2 == 1) -(kDp / 3f) else 0.dp
        CharacterArt(
            size = heroSize,
            style = ArtStyle.PIXEL,
            modifier = Modifier.offset(x = kDp * 60f - heroSize / 2f, y = oyDp + kDp * 133.5f - heroSize + hop),
            level = voice.level,
            listening = voice.listening,
            glow = false,
            pixels = 24,
            onTap = { voice.toggle() },
        )

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // HUD
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DAYLIGHT", style = hud, fontSize = 12.sp, fontWeight = FontWeight.W700)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(6) { i ->
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .drawBehind {
                                        val o = 2.dp.toPx()
                                        drawRect(Color.Black, Offset(o, o), size)
                                    }
                                    .background(if (i < pips) AdvGold else Color(0xFF3A3A2A))
                            )
                        }
                    }
                    Text(daylightText, style = hud, fontSize = 11.sp, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdvSlot("PEN", { writing = !writing }) { advPen(it) }
                    AdvSlot("LOG", { a.openLog() }) { advLetter(it) }
                    AdvSlot("Settings", { a.openOptions() }) { advMenu(it) }
                }
            }

            Spacer(Modifier.weight(1f))

            // dialog box
            Box(
                Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .background(AdvNavy)
                    .padding(3.dp)
                    .border(3.dp, Color.White)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (writing) "JOURNAL" else if (voice.listening) "LISTENING..." else s.companion.title.uppercase(),
                        style = hud.copy(shadow = null), color = s.companion.c, fontSize = 13.sp, fontWeight = FontWeight.W700,
                    )
                    if (voice.listening || (voice.note.isNotBlank() && !writing)) {
                        // what the hero hears, typed out like dialog
                        Text(
                            if (voice.listening) voice.heard.ifBlank { "Speak up, adventurer. I'm all ears..." } else voice.note,
                            style = hud.copy(shadow = null), color = if (voice.listening) AdvGold else Color.White,
                            fontSize = 17.sp, lineHeight = 23.sp, maxLines = 4, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.tap { voice.toggle() },
                        )
                    }
                    if (voice.listening) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { AdvBlinkArrow(clock) }
                    } else if (writing || f == null) {
                        if (!writing) {
                            Text(s.line, style = hud.copy(shadow = null), fontSize = 17.sp, lineHeight = 23.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        }
                        SpillInput(
                            onSpill = { a.spill(it); writing = false },
                            textStyle = TextStyle(fontFamily = Fonts.pixelifySans, color = Color.White, fontSize = 17.sp),
                            placeholder = "write a new quest…",
                            placeholderColor = Color.White.copy(alpha = 0.45f),
                            cursor = AdvGold,
                            modifier = Modifier.fillMaxWidth().border(2.dp, Color.White.copy(alpha = 0.5f)).padding(horizontal = 8.dp, vertical = 8.dp),
                            trailing = { submit ->
                                Text(
                                    "OK", style = hud.copy(shadow = null), color = AdvGold, fontSize = 15.sp, fontWeight = FontWeight.W700,
                                    modifier = Modifier.padding(start = 8.dp).tap(submit).padding(4.dp),
                                )
                            },
                        )
                    } else {
                        Text(
                            s.line, style = hud.copy(shadow = null), fontSize = 17.sp, lineHeight = 23.sp,
                            maxLines = 5, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.tap { a.openQuest(f) },
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            AdvBlinkArrow(clock)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // action menu
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (f != null) {
                    AdvButton("Deliver", Color(0xFF2A6A2A), Modifier.weight(1f)) { a.done(f) }
                    AdvButton("Rest", AdvNavy, Modifier.weight(1f)) { a.snooze(f, 60) }
                } else {
                    AdvButton(if (writing) "Close" else "Write", Color(0xFF2A6A2A), Modifier.weight(1f)) { writing = !writing }
                }
                AdvButton("Map", AdvNavy, Modifier.weight(1f)) { a.openLog() }
            }
        }
    }
}

@Composable
private fun AdvBlinkArrow(clock: androidx.compose.runtime.State<Float>) {
    Canvas(Modifier.size(14.dp, 14.dp)) {
        val down = if (floor(clock.value / 0.3f).toInt() % 2 == 1) 3.dp.toPx() else 0f
        val u = size.width / 7f
        // a stepped pixel triangle
        drawRect(Color.White, Offset(0f, down + u), Size(7 * u, u))
        drawRect(Color.White, Offset(u, down + 2 * u), Size(5 * u, u))
        drawRect(Color.White, Offset(2 * u, down + 3 * u), Size(3 * u, u))
        drawRect(Color.White, Offset(3 * u, down + 4 * u), Size(u, u))
    }
}

@Composable
private fun AdvButton(label: String, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.background(bg).border(3.dp, Color.White).tap(onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = Fonts.pixelifySans, fontWeight = FontWeight.W700, fontSize = 15.sp, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun AdvSlot(label: String, onClick: () -> Unit, icon: DrawScope.(Float) -> Unit) {
    Column(
        Modifier
            .height(46.dp)
            .widthIn(min = 46.dp)
            .border(3.dp, Color.White)
            .background(Color.Black.copy(alpha = 0.55f))
            .tap(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(22.dp, 18.dp)) { icon(this, size.height / 10f) }
        Spacer(Modifier.width(2.dp).height(2.dp))
        Text(label, fontFamily = Fonts.pixelifySans, fontSize = 9.sp, color = Color.White, lineHeight = 10.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 5.dp))
    }
}

private fun DrawScope.advPen(u: Float) {
    val ox = (size.width - 10 * u) / 2
    fun r(x: Int, y: Int, w: Int, h: Int, c: Long) = drawRect(Color(c), Offset(ox + x * u, y * u), Size(w * u, h * u))
    r(7, 0, 2, 2, 0xFFE8E8E8); r(5, 2, 2, 2, 0xFF3A6AA8); r(3, 4, 2, 2, 0xFF3A6AA8); r(1, 6, 2, 2, 0xFF3A6AA8); r(0, 8, 1, 1, 0xFF1A1A1A)
}

private fun DrawScope.advLetter(u: Float) {
    val ox = (size.width - 11 * u) / 2
    val oy = u
    fun r(x: Int, y: Int, w: Int, h: Int, c: Long) = drawRect(Color(c), Offset(ox + x * u, oy + y * u), Size(w * u, h * u))
    r(0, 0, 11, 8, 0xFFF4F0E2); r(0, 0, 2, 1, 0xFF8A7A5A); r(2, 1, 2, 1, 0xFF8A7A5A); r(4, 2, 3, 1, 0xFF8A7A5A); r(7, 1, 2, 1, 0xFF8A7A5A); r(9, 0, 2, 1, 0xFF8A7A5A)
}

private fun DrawScope.advMenu(u: Float) {
    val ox = (size.width - 10 * u) / 2
    fun r(x: Int, y: Int, w: Int, h: Int, c: Long) = drawRect(Color(c), Offset(ox + x * u, y * u), Size(w * u, h * u))
    r(0, 1, 10, 2, 0xFFE8E8E8); r(0, 4, 10, 2, 0xFFFFD23A); r(0, 7, 10, 2, 0xFFE8E8E8)
}

package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Canvas as OneBitGfxCanvas

private val OneBitInk = Color.Black
private val OneBitPaper = Color.White
private val OneBitMenu = TextStyle(fontFamily = Fonts.silkscreen, fontSize = 11.sp, color = OneBitInk)

/** Builds a tiling 1-bit pattern brush; [black] decides each pattern pixel. */
private fun oneBitBrush(w: Int, h: Int, unit: Int, black: (Int, Int) -> Boolean): Brush {
    val bmp = ImageBitmap(w * unit, h * unit)
    val canvas = OneBitGfxCanvas(bmp)
    val ink = Paint().apply { color = OneBitInk }
    val paper = Paint().apply { color = OneBitPaper }
    for (y in 0 until h) for (x in 0 until w) {
        canvas.drawRect(
            (x * unit).toFloat(), (y * unit).toFloat(), ((x + 1) * unit).toFloat(), ((y + 1) * unit).toFloat(),
            if (black(x, y)) ink else paper,
        )
    }
    return ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
}

private class OneBitPatterns(unit: Int) {
    val checker = oneBitBrush(4, 4, unit) { x, y -> (x < 2) == (y < 2) }
    val diag = oneBitBrush(4, 4, unit) { x, y -> (x + y) % 4 == 0 }
    val vert = oneBitBrush(4, 4, unit) { x, _ -> x == 0 }
    val horiz = oneBitBrush(3, 3, unit) { _, y -> y == 0 }
    val dots = oneBitBrush(4, 4, unit) { x, y -> x in 1..2 && y in 1..2 }
    val back = oneBitBrush(5, 5, unit) { x, y -> ((x - y) % 5 + 5) % 5 < 2 }
}

private fun oneBitShout(q: Quest, now: Long): String {
    val due = q.due ?: return "someday..."
    if (q.shelf(now) == Shelf.OVERDUE) return "LATE!!"
    if (q.allDay) return Time.dayName(Time.date(due)).uppercase() + "!!"
    val t = q.shortDue().uppercase().replace("12:00 PM", "NOON")
    return (if (q.kind == "by") "by " else "at ") + t + "!!"
}

@Composable
fun OneBitWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val clock = rememberClock()
    val unit = with(LocalDensity.current) { 1.dp.roundToPx().coerceAtLeast(1) }
    val pat = remember(unit) { OneBitPatterns(unit) }
    var showNew by rememberSaveable { mutableStateOf(false) }
    val inputOpen = showNew || q == null
    val okHolder = remember { arrayOfNulls<() -> Unit>(1) }
    val weekday = DateTimeFormatter.ofPattern("EEEE").format(Time.date(s.now))

    Box(Modifier.fillMaxSize().background(pat.checker)) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // menu bar
            Row(
                Modifier.fillMaxWidth().height(24.dp).background(OneBitPaper).drawBehind {
                    drawRect(OneBitInk, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
                }.padding(start = 10.dp, end = 10.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(12.dp).border(2.dp, OneBitInk, CircleShape).tap(a::openOptions))
                val bold = OneBitMenu.copy(fontWeight = FontWeight.Bold)
                Text("File", style = bold, modifier = Modifier.tap(a::openLog))
                Text("Edit", style = bold, modifier = Modifier.tap { if (q != null) a.openQuest(q) else { showNew = true } })
                Text("Goodies", style = bold, modifier = Modifier.tap(a::openOptions))
                Text("Font", style = bold, modifier = Modifier.tap(a::openOptions))
            }

            Spacer(Modifier.height(14.dp))

            // paint window
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp)
                    .drawBehind { val o = 4.dp.toPx(); drawRect(OneBitInk, Offset(o, o), size) }
                    .background(OneBitPaper).border(2.dp, OneBitInk)
                    .padding(2.dp)
            ) {
                // title bar
                Box(
                    Modifier.fillMaxWidth().height(20.dp).drawBehind {
                        val p = 1.dp.toPx()
                        var y = 0f
                        while (y < size.height - 2 * p) { drawRect(OneBitInk, Offset(0f, y), Size(size.width, p)); y += 3 * p }
                        drawRect(OneBitInk, Offset(0f, size.height - 2 * p), Size(size.width, 2 * p))
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(12.dp)
                            .background(OneBitPaper).border(2.dp, OneBitInk).tap(a::openLog)
                    )
                    Text(
                        "Chime Paint: $weekday",
                        style = OneBitMenu.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 28.dp).background(OneBitPaper).padding(horizontal = 8.dp),
                    )
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    // tool palette
                    Column(
                        Modifier.width(54.dp).fillMaxHeight().drawBehind {
                            drawRect(OneBitInk, Offset(size.width - 2.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))
                        }.padding(start = 2.dp, top = 2.dp, end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            OneBitTool(false, {}) { oneBitIconSelect(it) }
                            OneBitTool(true, {}) { oneBitIconPencil(it) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            OneBitTool(showNew, { showNew = !showNew }) { oneBitIconText(it) }
                            OneBitTool(false, {}) { oneBitIconBucket(it) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            OneBitTool(false, {}) { oneBitIconRect(it) }
                            OneBitTool(false, {}) { oneBitIconOval(it) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(Modifier.size(24.dp).background(pat.checker).border(1.dp, OneBitInk))
                            Box(Modifier.size(24.dp).background(pat.diag).border(1.dp, OneBitInk))
                        }
                    }

                    // the canvas
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState())
                        ) {
                            Box(Modifier.fillMaxWidth().heightIn(min = 200.dp)) {
                                // a circle drawn around it, not quite closed
                                Canvas(Modifier.matchParentSize()) { oneBitScribble() }
                                Column(Modifier.padding(start = 20.dp, top = 24.dp, end = 26.dp, bottom = 44.dp)) {
                                    val big = q?.text?.uppercase() ?: "NOTHING\nTO DRAW"
                                    val fs = when {
                                        big.length <= 16 -> 36.sp
                                        big.length <= 32 -> 30.sp
                                        else -> 24.sp
                                    }
                                    Text(
                                        big,
                                        style = TextStyle(fontFamily = Fonts.pixelifySans, fontWeight = FontWeight.W700, fontSize = fs, lineHeight = fs, color = OneBitInk),
                                        maxLines = 4, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.rotate(-2f).tap { if (q != null) a.openQuest(q) else { showNew = true } },
                                    )
                                    Text(
                                        if (q != null) oneBitShout(q, s.now) else "yet",
                                        style = TextStyle(fontFamily = Fonts.pixelifySans, fontWeight = FontWeight.W700, fontSize = 26.sp, color = OneBitInk),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 10.dp, top = 8.dp).rotate(1.5f),
                                    )
                                }
                            }
                            // the companion, a little bitmap moth with a pencil
                            Row(Modifier.fillMaxWidth().padding(end = 14.dp), horizontalArrangement = Arrangement.End) {
                                Canvas(Modifier.size(72.dp)) {
                                    val step = ((clock.value % 1f) >= 0.5f)
                                    val dx = if (step) 4.dp.toPx() else 0f
                                    val dy = if (step) 2.dp.toPx() else 0f
                                    oneBitMoth(dx, dy, (size.width - 4.dp.toPx()) / 12f)
                                }
                            }
                            // marching-ants selection around the companion's note
                            Box(
                                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp)
                                    .drawBehind {
                                        val w = 2.dp.toPx()
                                        val dash = 4.dp.toPx()
                                        val phase = (clock.value / 0.6f % 1f) * 2 * dash
                                        drawRect(OneBitPaper, style = Stroke(w))
                                        drawRect(
                                            OneBitInk,
                                            topLeft = Offset(w / 2, w / 2),
                                            size = Size(size.width - w, size.height - w),
                                            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash), phase)),
                                        )
                                    }
                                    .padding(10.dp)
                            ) {
                                Text(
                                    "${s.line} — ${s.companion.title.lowercase()}",
                                    style = TextStyle(fontFamily = Fonts.pixelifySans, fontWeight = FontWeight.W400, fontSize = 15.sp, lineHeight = 20.sp, color = OneBitInk),
                                )
                            }
                        }

                        if (inputOpen) {
                            Row(
                                Modifier.fillMaxWidth().drawBehind {
                                    drawRect(OneBitInk, Offset.Zero, Size(size.width, 2.dp.toPx()))
                                }.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(Modifier.weight(1f).border(2.dp, OneBitInk).padding(horizontal = 6.dp, vertical = 5.dp)) {
                                    SpillInput(
                                        onSpill = { a.spill(it); showNew = false },
                                        textStyle = TextStyle(fontFamily = Fonts.pixelifySans, fontSize = 15.sp, color = OneBitInk),
                                        placeholder = "type a new quest…",
                                        placeholderColor = OneBitInk,
                                        cursor = OneBitInk,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailing = { submit -> okHolder[0] = submit },
                                    )
                                }
                                OneBitButton("OK", ring = false) { okHolder[0]?.invoke() }
                            }
                        }

                        // bottom bar
                        Row(
                            Modifier.fillMaxWidth().height(36.dp).drawBehind {
                                drawRect(OneBitInk, Offset.Zero, Size(size.width, 2.dp.toPx()))
                            }.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            if (q != null) {
                                OneBitButton("Done", ring = true) { a.done(q) }
                                OneBitButton("Later", ring = false) { a.snooze(q, 30) }
                            }
                            OneBitButton("New", ring = q == null) { showNew = !showNew }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // pattern palette strip, like the paint app had
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(36.dp)
                    .background(OneBitPaper).border(2.dp, OneBitInk).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val cell = Modifier.weight(1f).fillMaxHeight()
                Box(cell.background(OneBitInk))
                Box(cell.background(pat.checker))
                Box(cell.background(pat.diag))
                Box(cell.background(pat.vert))
                Box(cell.background(pat.horiz))
                Box(cell.background(pat.dots))
                Box(cell.background(pat.back))
                Box(cell.background(OneBitPaper).border(1.dp, OneBitInk))
            }

            Spacer(Modifier.height(14.dp))

            // desktop icons
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                OneBitIcon("Week (${s.week.size})", trash = false, onClick = a::openLog)
                OneBitIcon("Someday (${s.someday.size})", trash = false, onClick = a::openLog)
                Spacer(Modifier.weight(1f))
                OneBitIcon("Done (${s.done.size})", trash = true, onClick = a::openLog)
            }
        }
    }
}

@Composable
private fun OneBitTool(selected: Boolean, onClick: () -> Unit, icon: DrawScope.(Color) -> Unit) {
    Box(
        Modifier.size(24.dp).background(if (selected) OneBitInk else OneBitPaper).border(1.dp, OneBitInk).tap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) { icon(if (selected) OneBitPaper else OneBitInk) }
    }
}

@Composable
private fun OneBitButton(label: String, ring: Boolean, onClick: () -> Unit) {
    val btn = @Composable {
        Box(
            Modifier.border(2.dp, OneBitInk, RoundedCornerShape(8.dp)).background(OneBitPaper, RoundedCornerShape(8.dp))
                .tap(onClick).padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = OneBitMenu.copy(fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
    if (ring) Box(Modifier.border(2.dp, OneBitInk, RoundedCornerShape(11.dp)).padding(3.dp)) { btn() }
    else Box(Modifier.padding(3.dp)) { btn() }
}

@Composable
private fun OneBitIcon(label: String, trash: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.tap(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (trash) {
            Canvas(Modifier.size(26.dp, 30.dp)) {
                val u = size.width / 13f
                oneBitBox(1f, 3f, 11f, 12f, u)
                oneBitBox(0f, 1f, 13f, 2f, u)
                drawRect(OneBitInk, Offset(4 * u, 5 * u), Size(u, 8 * u))
                drawRect(OneBitInk, Offset(8 * u, 5 * u), Size(u, 8 * u))
            }
        } else {
            Canvas(Modifier.size(32.dp, 26.dp)) {
                val u = size.width / 16f
                oneBitBox(0f, 2f, 16f, 11f, u)
                oneBitBox(0f, 0f, 6f, 3f, u)
                drawRect(OneBitPaper, Offset(u, 2 * u), Size(4 * u, 1.5f * u))
            }
        }
        Text(
            label, style = OneBitMenu, maxLines = 1, textAlign = TextAlign.Center,
            modifier = Modifier.background(OneBitPaper).padding(horizontal = 3.dp),
        )
    }
}

/** White box with a one-pixel black outline, inside its cell. */
private fun DrawScope.oneBitBox(x: Float, y: Float, w: Float, h: Float, u: Float) {
    drawRect(OneBitPaper, Offset(x * u, y * u), Size(w * u, h * u))
    drawRect(OneBitInk, Offset(x * u + u / 2, y * u + u / 2), Size(w * u - u, h * u - u), style = Stroke(u))
}

private fun DrawScope.oneBitIconSelect(ink: Color) {
    val u = size.width / 7f
    drawRect(ink, Offset(u / 2, u / 2), Size(6 * u, 6 * u), style = Stroke(u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u, u))))
}

private fun DrawScope.oneBitIconPencil(ink: Color) {
    val u = size.width / 7f
    drawRect(ink, Offset(5 * u, 0f), Size(2 * u, 2 * u))
    drawRect(ink, Offset(3 * u, 2 * u), Size(2 * u, 2 * u))
    drawRect(ink, Offset(1 * u, 4 * u), Size(2 * u, 2 * u))
    drawRect(ink, Offset(0f, 6 * u), Size(u, u))
}

private fun DrawScope.oneBitIconText(ink: Color) {
    val u = size.width / 7f
    val p = Path().apply { moveTo(0.5f * u, 7 * u); lineTo(3.5f * u, 0f); lineTo(6.5f * u, 7 * u) }
    drawPath(p, ink, style = Stroke(1.4f * u, cap = StrokeCap.Square))
    drawRect(ink, Offset(2 * u, 4 * u), Size(3 * u, u))
}

private fun DrawScope.oneBitIconBucket(ink: Color) {
    val u = size.width / 7f
    drawRect(ink, Offset(u, 2 * u), Size(5 * u, 4 * u))
    drawRect(ink, Offset(2.5f * u, 0.5f * u), Size(2 * u, 1.5f * u), style = Stroke(u))
}

private fun DrawScope.oneBitIconRect(ink: Color) {
    val u = size.width / 7f
    drawRect(ink, Offset(1.5f * u, 1.5f * u), Size(4 * u, 4 * u), style = Stroke(u))
}

private fun DrawScope.oneBitIconOval(ink: Color) {
    val u = size.width / 7f
    drawCircle(ink, radius = 2.5f * u, style = Stroke(u))
}

/** The not-quite-closed circle and arrow, from a 270x200 sketch. */
private fun DrawScope.oneBitScribble() {
    val sx = size.width / 270f
    val sy = size.height / 200f
    val w = 3.dp.toPx()
    val ring = Path().apply {
        moveTo(40 * sx, 30 * sy)
        cubicTo(120 * sx, -6 * sy, 250 * sx, 10 * sy, 240 * sx, 90 * sy)
        cubicTo(232 * sx, 160 * sy, 110 * sx, 176 * sy, 40 * sx, 150 * sy)
        cubicTo(-4 * sx, 132 * sy, 0f, 60 * sy, 52 * sx, 26 * sy)
    }
    drawPath(ring, OneBitInk, style = Stroke(w))
    val arrow = Path().apply {
        moveTo(200 * sx, 160 * sy); lineTo(230 * sx, 180 * sy)
        moveTo(222 * sx, 170 * sy); lineTo(232 * sx, 182 * sy); lineTo(218 * sx, 182 * sy)
    }
    drawPath(arrow, OneBitInk, style = Stroke(w))
}

/** A 12x12 bitmap moth holding a pencil. Pure black and white. */
private fun DrawScope.oneBitMoth(dx: Float, dy: Float, u: Float) {
    fun px(x: Int, y: Int, w: Int, h: Int) = drawRect(OneBitInk, Offset(dx + x * u, dy + y * u), Size(w * u, h * u))
    fun wing(x: Int, y: Int) {
        drawRect(OneBitPaper, Offset(dx + x * u, dy + y * u), Size(4 * u, 4 * u))
        drawRect(OneBitInk, Offset(dx + x * u, dy + y * u), Size(4 * u, 4 * u), style = Stroke(u / 2))
    }
    wing(1, 2); wing(7, 2)
    px(2, 3, 1, 1); px(9, 3, 1, 1)
    px(5, 3, 2, 6)
    px(4, 0, 1, 2); px(7, 0, 1, 2)
    px(7, 8, 1, 1); px(8, 9, 1, 1); px(9, 10, 1, 1)
}

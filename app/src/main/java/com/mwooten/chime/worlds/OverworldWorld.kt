package com.mwooten.chime.worlds

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val OwGold = Color(0xFFFFD23A)
private val OwBlue = Color(0xFF2A6AD8)
private val OwGreen = Color(0xFF2AA84A)
private val OwRed = Color(0xFFD83A2A)
private val OwInk = Color(0xE60C0A26)
private val OwCream = Color(0xFFFFF4DC)

// ---------------------------------------------------------------- the world, in dp units

private const val OW_W = 1700f
private const val OW_H = 1620f
private val OwVillage = Offset(760f, 760f)
private val OwHome = Offset(760f, 812f)
private val OwIsle = Offset(1545f, 1462f)
private val OwLake = Offset(430f, 450f)
private const val OwLakeRx = 175f
private const val OwLakeRy = 105f

/** The mainland coastline, hand-placed. */
private val OwCoast = floatArrayOf(
    90f, 140f, 260f, 70f, 520f, 95f, 760f, 55f, 1010f, 85f, 1260f, 60f, 1400f, 170f, 1445f, 420f,
    1400f, 700f, 1450f, 960f, 1395f, 1220f, 1250f, 1360f, 980f, 1405f, 700f, 1380f, 420f, 1415f,
    180f, 1335f, 70f, 1100f, 110f, 830f, 50f, 560f, 80f, 330f,
)

/** Someday Isle, off the south-east coast. */
private val OwIsleShape = floatArrayOf(
    1430f, 1400f, 1500f, 1345f, 1600f, 1350f, 1665f, 1420f, 1660f, 1520f, 1580f, 1580f, 1470f, 1570f, 1415f, 1490f,
)

/** Mountains along the north: centre x, base y, half width, height. */
private val OwPeaks = listOf(
    floatArrayOf(250f, 260f, 130f, 170f), floatArrayOf(440f, 225f, 115f, 140f), floatArrayOf(640f, 240f, 140f, 185f),
    floatArrayOf(900f, 222f, 130f, 160f), floatArrayOf(1120f, 245f, 140f, 178f), floatArrayOf(1310f, 300f, 105f, 128f),
)

/** Rolling low-poly hills: centre x, base y, half width, height. */
private val OwHills = listOf(
    floatArrayOf(300f, 1050f, 120f, 80f), floatArrayOf(1180f, 560f, 110f, 70f), floatArrayOf(1150f, 1110f, 130f, 85f),
    floatArrayOf(560f, 1200f, 100f, 60f), floatArrayOf(230f, 720f, 90f, 60f), floatArrayOf(1000f, 1300f, 90f, 50f),
)

private val OwTrees = listOf(
    180f to 420f, 220f to 470f, 150f to 520f, 610f to 360f, 660f to 420f, 880f to 380f, 950f to 430f, 1010f to 360f,
    1220f to 380f, 1290f to 440f, 1330f to 620f, 1250f to 880f, 1320f to 960f, 1180f to 980f, 980f to 940f, 1040f to 1020f,
    900f to 1150f, 860f to 1240f, 620f to 1060f, 560f to 980f, 480f to 1120f, 380f to 1240f, 250f to 1180f, 170f to 960f,
    230f to 880f, 330f to 820f, 420f to 700f, 1080f to 640f, 980f to 580f, 1120f to 820f, 640f to 880f, 320f to 580f,
    1380f to 1120f, 1060f to 1200f, 140f to 1220f,
)

private val OwRoads = listOf(
    floatArrayOf(760f, 760f, 770f, 560f, 745f, 380f, 760f, 262f),
    floatArrayOf(760f, 760f, 980f, 772f, 1200f, 742f, 1405f, 760f),
    floatArrayOf(760f, 760f, 740f, 1000f, 790f, 1200f, 760f, 1372f),
    floatArrayOf(760f, 760f, 620f, 660f, 545f, 565f),
    floatArrayOf(1120f, 760f, 1250f, 1000f, 1330f, 1250f),
)

/** Village houses, relative to the plaza. */
private val OwHouses = listOf(-118f to -64f, 104f to -74f, -122f to 92f, 114f to 86f)

/** Distance rings around the village: each further band is a further day. */
private val OwBands = listOf(
    Shelf.OVERDUE to (105f to 145f), Shelf.TODAY to (150f to 205f), Shelf.TOMORROW to (245f to 305f),
    Shelf.WEEK to (345f to 425f), Shelf.LATER to (465f to 545f),
)
private val OwRingLabels = listOf(178f to "TODAY", 275f to "TOMORROW", 385f to "THIS WEEK", 505f to "LATER")

private fun owPath(pts: FloatArray, u: Float): Path = Path().apply {
    moveTo(pts[0] * u, pts[1] * u)
    var i = 2
    while (i < pts.size) { lineTo(pts[i] * u, pts[i + 1] * u); i += 2 }
    close()
}

private fun DrawScope.owPoly(u: Float, c: Color, vararg pts: Float) = drawPath(owPath(pts, u), c)

private fun owHash(s: String): Int = s.hashCode() and 0x7fffffff

/**
 * Where every open quest stands on the map. Due today: right by the village. Each later day sits
 * one ring further out. No date at all: out on Someday Isle. Always the same spot for the same quest.
 */
private fun owSpots(open: List<Quest>, now: Long): Map<String, Offset> {
    val out = LinkedHashMap<String, Offset>()
    val placed = ArrayList<Offset>()
    val base = mapOf(Shelf.OVERDUE to 200f, Shelf.TODAY to 20f, Shelf.TOMORROW to 100f, Shelf.WEEK to 250f, Shelf.LATER to 320f)
    for ((shelf, band) in OwBands) {
        val group = open.filter { it.shelf(now) == shelf }
        for ((i, q) in group.withIndex()) {
            val due = q.due ?: now
            val frac = when (shelf) {
                Shelf.OVERDUE -> 0.5f
                Shelf.TODAY -> ((due - now) / 86_400_000f).coerceIn(0f, 1f)
                Shelf.TOMORROW -> (Time.ldt(due).hour / 24f).coerceIn(0f, 1f)
                Shelf.WEEK -> ((due - now) / (7f * 86_400_000f)).coerceIn(0f, 1f)
                else -> ((due - now) / (60f * 86_400_000f)).coerceIn(0f, 1f)
            }
            var r = band.first + (band.second - band.first) * frac
            var deg = (base[shelf] ?: 0f) + i * 137.5f + (owHash(q.id) % 24)
            var p = Offset.Zero
            for (attempt in 0 until 16) {
                val rad = deg * PI.toFloat() / 180f
                p = Offset(OwVillage.x + cos(rad) * r, OwVillage.y + sin(rad) * r * 0.92f)
                // keep out of the lake
                var guard = 0
                while (guard < 40) {
                    val ex = (p.x - OwLake.x) / (OwLakeRx * 1.2f)
                    val ey = (p.y - OwLake.y) / (OwLakeRy * 1.25f)
                    if (ex * ex + ey * ey >= 1f) break
                    val d = (p - OwLake).getDistance().coerceAtLeast(1f)
                    p += (p - OwLake) / d * 12f
                    guard++
                }
                p = Offset(p.x.coerceIn(150f, 1330f), p.y.coerceIn(210f, 1320f))
                if (placed.none { abs(it.x - p.x) < 112f && abs(it.y - p.y) < 64f }) break
                deg += 29f
                r += 5f
            }
            placed += p
            out[q.id] = p
        }
    }
    val someday = open.filter { it.shelf(now) == Shelf.SOMEDAY }
    for ((i, q) in someday.withIndex()) {
        val rad = (30f + i * 137.5f) * PI.toFloat() / 180f
        val r = if (i == 0) 0f else 32f + (i % 3) * 26f
        out[q.id] = Offset(OwIsle.x + cos(rad) * r, OwIsle.y + 10f + sin(rad) * r * 0.75f)
    }
    return out
}

/** The whole map: sea, coast, faceted grass, peaks, lake, roads, village, trees and the isle. */
private fun DrawScope.owTerrain(spots: Collection<Offset>) {
    val u = size.width / OW_W
    // sea, in big flat facets
    drawRect(Color(0xFF2C6CB4))
    val sc = 140f
    var gy = 0
    while (gy * sc < OW_H) {
        var gx = 0
        while (gx * sc < OW_W) {
            val x0 = gx * sc
            val y0 = gy * sc
            if ((gx + gy) % 2 == 0) owPoly(u, Color(0xFF3074BE), x0, y0, x0 + sc, y0, x0, y0 + sc)
            else owPoly(u, Color(0xFF2966AC), x0 + sc, y0, x0 + sc, y0 + sc, x0, y0 + sc)
            gx++
        }
        gy++
    }
    // shallows and beach, then the mainland
    val coast = owPath(OwCoast, u)
    val isle = owPath(OwIsleShape, u)
    for (p in listOf(coast, isle)) {
        drawPath(p, Color(0xFF4E96D2), style = Stroke(width = 90f * u, join = StrokeJoin.Round))
        drawPath(p, Color(0xFFE6D29C), style = Stroke(width = 44f * u, join = StrokeJoin.Round))
    }
    val greens = listOf(Color(0xFF5DA03C), Color(0xFF68AC44), Color(0xFF56973A), Color(0xFF62A640), Color(0xFF6CB04A))
    val cell = 100f
    for (p in listOf(coast, isle)) {
        clipPath(p) {
            drawRect(greens[0])
            var j = 0
            while (j * cell < OW_H) {
                var i = 0
                while (i * cell < OW_W) {
                    val x0 = i * cell
                    val y0 = j * cell
                    val h1 = ((i * 73856093) xor (j * 19349663)) and 0x7fffffff
                    val h2 = ((i * 83492791) xor (j * 2971215)) and 0x7fffffff
                    owPoly(u, greens[h1 % greens.size], x0, y0, x0 + cell, y0, x0, y0 + cell)
                    owPoly(u, greens[h2 % greens.size], x0 + cell, y0, x0 + cell, y0 + cell, x0, y0 + cell)
                    i++
                }
                j++
            }
        }
    }
    // distance rings around home, faint chalk on the grass
    for (r in listOf(148f, 225f, 325f, 445f)) {
        drawOval(
            Color.White.copy(alpha = 0.22f),
            topLeft = Offset((OwVillage.x - r) * u, (OwVillage.y - r * 0.92f) * u),
            size = Size(2f * r * u, 2f * r * 0.92f * u),
            style = Stroke(width = 3f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f * u, 12f * u), 0f)),
        )
    }
    // peaks
    for (m in OwPeaks) {
        val cx = m[0]; val bY = m[1]; val hw = m[2]; val h = m[3]
        val ax = cx; val ay = bY - h
        val mx = cx + hw * 0.15f
        owPoly(u, Color(0xFF9A9A84), ax, ay, cx - hw, bY, mx, bY)
        owPoly(u, Color(0xFF6E6F5E), ax, ay, mx, bY, cx + hw, bY)
        val k = 0.32f
        owPoly(u, Color(0xFFF2F5F8), ax, ay, ax + (cx - hw - ax) * k, ay + h * k, ax + (mx - ax) * k, ay + h * k)
        owPoly(u, Color(0xFFC9D3DE), ax, ay, ax + (mx - ax) * k, ay + h * k, ax + (cx + hw - ax) * k, ay + h * k)
    }
    // hills
    val hillTones = listOf(Color(0xFF7DBA52), Color(0xFF6EAE48), Color(0xFF5E9C3E), Color(0xFF4F8834))
    for (hl in OwHills) {
        val cx = hl[0]; val bY = hl[1]; val hw = hl[2]; val h = hl[3]
        val pts = (0..4).map { k ->
            val a = PI.toFloat() * (1f - k / 4f)
            Offset(cx + hw * cos(a), bY - h * sin(a))
        }
        val hub = Offset(cx - hw * 0.12f, bY - h * 0.3f)
        drawOval(Color.Black.copy(alpha = 0.12f), Offset((cx - hw * 0.9f) * u, (bY - 8f) * u), Size(hw * 2.1f * u, 18f * u))
        for (k in 0 until 4) {
            owPoly(u, hillTones[k], hub.x, hub.y, pts[k].x, pts[k].y, pts[k + 1].x, pts[k + 1].y)
        }
        owPoly(u, Color(0xFF4A7E30), hub.x, hub.y, pts[4].x, pts[4].y, pts[0].x, pts[0].y)
    }
    // river from the peaks into the lake
    val river = Path().apply {
        moveTo(640f * u, 250f * u); lineTo(580f * u, 300f * u); lineTo(530f * u, 340f * u); lineTo(490f * u, 372f * u)
    }
    drawPath(river, Color(0xFF3C86C8), style = Stroke(width = 20f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // lake: sandy rim, water, a lighter facet
    fun lakePts(scale: Float, dx: Float = 0f, dy: Float = 0f): FloatArray {
        val n = 10
        val f = FloatArray(n * 2)
        for (k in 0 until n) {
            val a = 2f * PI.toFloat() * k / n + 0.2f
            val wob = if (k % 2 == 0) 1f else 0.9f
            f[k * 2] = OwLake.x + dx + cos(a) * OwLakeRx * scale * wob
            f[k * 2 + 1] = OwLake.y + dy + sin(a) * OwLakeRy * scale * wob
        }
        return f
    }
    drawPath(owPath(lakePts(1.14f), u), Color(0xFFD9C48F))
    drawPath(owPath(lakePts(1f), u), Color(0xFF3C86C8))
    drawPath(owPath(lakePts(0.55f, -30f, -18f), u), Color(0xFF5DA2DC))
    for (k in 0 until 5) {
        val sx = OwLake.x - 80f + k * 38f
        val sy = OwLake.y + 30f - (k % 2) * 40f
        drawLine(Color.White.copy(alpha = 0.6f), Offset(sx * u, sy * u), Offset((sx + 16f) * u, sy * u), 3f * u, cap = StrokeCap.Round)
    }
    // dock
    drawRect(Color(0xFF8A5A30), Offset(590f * u, 470f * u), Size(48f * u, 12f * u))
    drawRect(Color(0xFF6A4020), Offset(590f * u, 482f * u), Size(48f * u, 4f * u))
    // roads: a darker kerb under a sandy path
    for (rd in OwRoads) {
        val p = Path().apply {
            moveTo(rd[0] * u, rd[1] * u)
            var i = 2
            while (i < rd.size) { lineTo(rd[i] * u, rd[i + 1] * u); i += 2 }
        }
        drawPath(p, Color(0xFFA88A52), style = Stroke(width = 34f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, Color(0xFFD2B47A), style = Stroke(width = 26f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    // footpaths out to every signpost
    for (sp in spots) {
        if (sp.x > 1420f || sp.y > 1390f) continue
        drawLine(
            Color(0xFFC9AE78).copy(alpha = 0.8f), Offset(OwVillage.x * u, OwVillage.y * u), Offset(sp.x * u, (sp.y + 6f) * u),
            strokeWidth = 9f * u, cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * u, 10f * u), 0f),
        )
    }
    // pier and a little boat out to Someday Isle
    drawRect(Color(0xFF8A5A30), Offset(1300f * u, 1300f * u), Size(110f * u, 14f * u))
    for (k in 0 until 6) drawRect(Color(0xFF6A4020), Offset((1304f + k * 18f) * u, 1300f * u), Size(3f * u, 14f * u))
    owPoly(u, Color(0xFFB0683A), 1428f, 1318f, 1478f, 1318f, 1468f, 1334f, 1438f, 1334f)
    owPoly(u, Color(0xFFF4ECDC), 1452f, 1280f, 1452f, 1316f, 1474f, 1316f)
    // plaza and fountain
    val plaza = FloatArray(16)
    for (k in 0 until 8) {
        val a = 2f * PI.toFloat() * k / 8f + PI.toFloat() / 8f
        plaza[k * 2] = OwVillage.x + cos(a) * 82f
        plaza[k * 2 + 1] = OwVillage.y + sin(a) * 66f
    }
    drawPath(owPath(plaza, u), Color(0xFFB8A57E))
    for (k in 0 until 8) { plaza[k * 2] = OwVillage.x + (plaza[k * 2] - OwVillage.x) * 0.92f; plaza[k * 2 + 1] = OwVillage.y + (plaza[k * 2 + 1] - OwVillage.y) * 0.9f }
    drawPath(owPath(plaza, u), Color(0xFFDCCDA8))
    drawOval(Color(0xFF9C9584), Offset((OwVillage.x - 26f) * u, (OwVillage.y - 20f) * u), Size(52f * u, 36f * u))
    drawOval(Color(0xFF6BB0E0), Offset((OwVillage.x - 18f) * u, (OwVillage.y - 15f) * u), Size(36f * u, 24f * u))
    drawRect(Color(0xFF9C9584), Offset((OwVillage.x - 4f) * u, (OwVillage.y - 34f) * u), Size(8f * u, 22f * u))
    // houses, each a little box with a two-tone roof
    for ((dx, dy) in OwHouses) owHouse(u, OwVillage.x + dx, OwVillage.y + dy)
    // trees (and a palm on the isle)
    for ((tx, ty) in OwTrees.sortedBy { it.second }) owTree(u, tx, ty)
    owPalm(u, 1600f, 1440f)
    owPalm(u, 1480f, 1520f)
}

private fun DrawScope.owHouse(u: Float, x: Float, y: Float) {
    val w = 60f; val wallH = 32f; val d = 24f; val roofH = 24f
    owPoly(u, Color.Black.copy(alpha = 0.18f), x - w / 2, y, x + w / 2 + d, y, x + w / 2 + d + 14f, y + 10f, x - w / 2 + 14f, y + 10f)
    // front wall and side wall
    drawRect(Color(0xFFEFE2C4), Offset((x - w / 2) * u, (y - wallH) * u), Size(w * u, wallH * u))
    owPoly(u, Color(0xFFC8B48C), x + w / 2, y - wallH, x + w / 2 + d * 0.6f, y - wallH - d * 0.5f, x + w / 2 + d * 0.6f, y - d * 0.5f, x + w / 2, y)
    // gable and roof slope
    owPoly(u, Color(0xFFE2D1A8), x - w / 2, y - wallH, x, y - wallH - roofH, x + w / 2, y - wallH)
    owPoly(u, Color(0xFF9A3E28), x, y - wallH - roofH, x + d * 0.6f, y - wallH - roofH - d * 0.5f, x + w / 2 + 4f + d * 0.6f, y - wallH - d * 0.5f + 2f, x + w / 2 + 4f, y - wallH + 2f)
    owPoly(u, Color(0xFFD0603F), x - w / 2 - 4f, y - wallH + 2f, x, y - wallH - roofH, x + 3f, y - wallH - roofH + 5f, x - w / 2, y - wallH + 6f)
    // door and window
    drawRect(Color(0xFF6A4020), Offset((x - 6f) * u, (y - 18f) * u), Size(12f * u, 18f * u))
    drawRect(Color(0xFFFFE08A), Offset((x - 22f) * u, (y - 24f) * u), Size(10f * u, 9f * u))
    drawRect(Color(0xFFFFE08A), Offset((x + 12f) * u, (y - 24f) * u), Size(10f * u, 9f * u))
}

private fun DrawScope.owTree(u: Float, x: Float, y: Float) {
    drawOval(Color.Black.copy(alpha = 0.18f), Offset((x - 10f) * u, (y - 5f) * u), Size(34f * u, 10f * u))
    drawRect(Color(0xFF6A4020), Offset((x - 4f) * u, (y - 16f) * u), Size(8f * u, 16f * u))
    owPoly(u, Color(0xFF2E6A2A), x, y - 58f, x - 22f, y - 14f, x, y - 14f)
    owPoly(u, Color(0xFF235A22), x, y - 58f, x, y - 14f, x + 22f, y - 14f)
    owPoly(u, Color(0xFF3A7A32), x, y - 76f, x - 16f, y - 40f, x, y - 40f)
    owPoly(u, Color(0xFF2E6A2A), x, y - 76f, x, y - 40f, x + 16f, y - 40f)
}

private fun DrawScope.owPalm(u: Float, x: Float, y: Float) {
    drawOval(Color.Black.copy(alpha = 0.18f), Offset((x - 10f) * u, (y - 4f) * u), Size(30f * u, 8f * u))
    owPoly(u, Color(0xFF8A6034), x - 3f, y, x + 3f, y, x + 10f, y - 50f, x + 5f, y - 50f)
    owPoly(u, Color(0xFF3E8A36), x + 7f, y - 52f, x - 22f, y - 46f, x - 6f, y - 58f)
    owPoly(u, Color(0xFF2F7029), x + 7f, y - 52f, x + 36f, y - 44f, x + 20f, y - 60f)
    owPoly(u, Color(0xFF4A9A40), x + 7f, y - 52f, x - 4f, y - 72f, x + 16f, y - 70f)
}

private class OwCam { var job: Job? = null }

/**
 * Overworld 64: a whole little low-poly world you drag around (pinch to zoom). Every open quest is a
 * signpost: today's stand by the village, later ones further out, someday ones on the far isle.
 * Tap a sign and your character walks over and reads it. Tap your character to talk.
 */
@Composable
fun OverworldWorld(s: WorldState, a: WorldActions) {
    val scope = rememberCoroutineScope()
    val voice = rememberVoice { heard -> a.spill(heard) }
    val inf = rememberInfiniteTransition(label = "ow")
    val press by inf.animateFloat(1f, 1.12f, infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "press")
    val step by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(340, easing = LinearEasing)), label = "step")
    val breathe by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val shadowPx = with(LocalDensity.current) { 2.dp.toPx() }
    val hud = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, color = Color.White, shadow = Shadow(Color.Black, Offset(shadowPx, shadowPx), 0f))

    val spots = owSpots(s.open, s.now)
    var selId by remember { mutableStateOf<String?>(null) }
    val selected = s.open.firstOrNull { it.id == selId }
    val walk = remember { Animatable(OwHome, Offset.VectorConverter) }
    val cam = remember { OwCam() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF2C6CB4)).clipToBounds()) {
        val density = LocalDensity.current.density
        val sw = constraints.maxWidth.toFloat()
        val sh = constraints.maxHeight.toFloat()
        val minZoom = max(0.6f, max(sw / (OW_W * density), sh / (OW_H * density)))

        fun clampPan(p: Offset, z: Float): Offset {
            val ww = OW_W * density * z
            val wh = OW_H * density * z
            val x = if (ww <= sw) (sw - ww) / 2f else p.x.coerceIn(sw - ww, 0f)
            val y = if (wh <= sh) (sh - wh) / 2f else p.y.coerceIn(sh - wh, 0f)
            return Offset(x, y)
        }
        fun panFor(world: Offset, z: Float): Offset =
            clampPan(Offset(sw * 0.5f - world.x * density * z, sh * 0.40f - world.y * density * z), z)

        var zoom by remember { mutableFloatStateOf(1f) }
        var pan by remember { mutableStateOf(panFor(OwHome, 1f)) }

        fun glideTo(world: Offset) {
            cam.job?.cancel()
            val from = pan
            val to = panFor(world, zoom)
            cam.job = scope.launch {
                animate(0f, 1f, animationSpec = tween(700, easing = FastOutSlowInEasing)) { v, _ ->
                    pan = Offset(from.x + (to.x - from.x) * v, from.y + (to.y - from.y) * v)
                }
            }
        }
        fun walkTo(target: Offset) {
            val d = (target - walk.value).getDistance()
            scope.launch { walk.animateTo(target, tween((d * 2.4f).toInt().coerceIn(250, 2400), easing = LinearEasing)) }
        }
        fun visit(q: Quest) {
            val at = spots[q.id] ?: return
            selId = q.id
            walkTo(Offset(at.x + 34f, at.y + 6f))
            glideTo(at)
        }

        // ---- the world layer: pans and zooms as one piece
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(sw, sh) {
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        cam.job?.cancel()
                        val z0 = zoom
                        val z1 = (z0 * zoomChange).coerceIn(minZoom, 1.8f)
                        val worldPt = (centroid - pan) / z0
                        pan = clampPan(centroid - worldPt * z1 + panChange, z1)
                        zoom = z1
                    }
                }
        ) {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .size(OW_W.dp, OW_H.dp)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = pan.x
                        translationY = pan.y
                        scaleX = zoom
                        scaleY = zoom
                    }
            ) {
                val spotList = spots.values.toList()
                Canvas(Modifier.fillMaxSize()) { owTerrain(spotList) }

                // place names
                OwPlace("Home Village", OwVillage.x, OwVillage.y + 112f)
                OwPlace("Mirror Lake", OwLake.x, OwLake.y - 10f)
                OwPlace("The Far Peaks", 760f, 96f)
                OwPlace("Someday Isle", OwIsle.x, OwIsle.y - 128f)
                for ((r, label) in OwRingLabels) {
                    Text(
                        label,
                        style = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 10.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.7f)),
                        modifier = Modifier.offset(x = (OwVillage.x - 40f).dp, y = (OwVillage.y + r * 0.92f - 18f).dp).width(80.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }

                // a signpost for every open quest, back to front
                for (q in s.open.sortedBy { spots[it.id]?.y ?: 0f }) {
                    val at = spots[q.id] ?: continue
                    OwSign(q, at, s.now, selected = q.id == selId, press = press) { visit(q) }
                }

                // you
                val walking = walk.isRunning
                Box(
                    Modifier
                        .offset { IntOffset((walk.value.x - 36f).dp.roundToPx(), (walk.value.y - 74f).dp.roundToPx()) }
                        .size(72.dp, 78.dp)
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = 34.dp.toPx()
                        drawOval(Color.Black.copy(alpha = 0.28f), Offset((size.width - w) / 2f, size.height - 9.dp.toPx()), Size(w, 9.dp.toPx()))
                    }
                    CharacterArt(
                        size = 60.dp,
                        style = ArtStyle.SHINY,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp)
                            .graphicsLayer {
                                translationY = if (walking) -abs(sin(step * PI.toFloat())) * 9.dp.toPx() else -breathe * 2.dp.toPx()
                            },
                        level = voice.level,
                        listening = voice.listening,
                        glow = false,
                        onTap = { voice.toggle() },
                    )
                }
                if (voice.listening || voice.note.isNotBlank()) {
                    Box(
                        Modifier
                            .offset { IntOffset((walk.value.x - 95f).dp.roundToPx(), (walk.value.y - 138f).dp.roundToPx()) }
                            .width(190.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFF1A1A3A), RoundedCornerShape(12.dp))
                            .tap { voice.toggle() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            if (voice.listening) voice.heard.ifBlank { "Listening… what should the next sign say?" } else voice.note,
                            fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 13.sp, lineHeight = 17.sp,
                            color = Color(0xFF1A1A3A), maxLines = 4, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // ---- distance haze over the top of the screen, like a 64-era draw distance
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(listOf(Color(0xAAD8E8F6), Color(0x00D8E8F6)), 0f, size.height * 0.24f),
                size = Size(size.width, size.height * 0.24f),
            )
        }

        // ---- HUD and dialog
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                Column(
                    Modifier.weight(1f, fill = false)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .tap { a.openLog() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    val date = Time.ldt(s.now).format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
                    Text("$date · ${Time.time(s.now)}", style = hud, fontSize = 14.sp, maxLines = 1)
                    Text(
                        "${s.open.size} quest${if (s.open.size == 1) "" else "s"}" + if (s.overdueCount > 0) " · ${s.overdueCount} late" else "",
                        style = hud, fontSize = 12.sp, color = OwGold, maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OwPill("START", OwRed, "Settings") { a.openOptions() }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OwPill("R", OwGreen, "Me") { selId = null; glideTo(walk.value) }
                        OwPill("C", OwGold, "Log") { a.openLog() }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            val q = selected
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(OwInk, RoundedCornerShape(14.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (q != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("The sign reads:", fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
                        Text(
                            "Close", fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.tap { selId = null }.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        "“${q.text}”",
                        fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 19.sp, lineHeight = 24.sp, color = OwGold,
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.tap { a.openQuest(q) },
                    )
                    Text(
                        q.dueText() + if (q.shelf(s.now) == Shelf.OVERDUE) " · overdue" else "",
                        fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 14.sp,
                        color = if (q.shelf(s.now) == Shelf.OVERDUE) Color(0xFFFF9A8A) else Color.White.copy(alpha = 0.85f),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        OwFaceButton("A", OwBlue, "Done", press) { a.done(q); selId = null }
                        Spacer(Modifier.width(14.dp))
                        OwFaceButton("B", OwGreen, "Later", 1f) { a.snooze(q, 30); selId = null }
                        Spacer(Modifier.width(14.dp))
                        OwFaceButton("C", OwGold, "Open", 1f) { a.openQuest(q) }
                    }
                } else {
                    val f = s.focus
                    Text(
                        "${s.companion.title}: ${s.line}",
                        fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 15.sp, lineHeight = 20.sp,
                        color = Color.White, maxLines = 4, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.tap { if (f != null) visit(f) },
                    )
                    Text(
                        if (s.open.isEmpty()) "No signs up yet. Tap yourself to talk, or put one up below."
                        else "Drag to explore · tap a sign to walk there · tap yourself to talk",
                        fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f),
                    )
                    SpillInput(
                        onSpill = a::spill,
                        textStyle = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 15.sp, color = Color.White),
                        placeholder = "Put up a new sign…",
                        placeholderColor = Color.White.copy(alpha = 0.5f),
                        cursor = OwGold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        trailing = { submit ->
                            Box(
                                Modifier.size(30.dp).background(OwBlue, CircleShape).tap(submit),
                                contentAlignment = Alignment.Center,
                            ) { Text("A", fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center) }
                        },
                    )
                }
            }
        }
    }
}

/** A region name floating over the map. */
@Composable
private fun OwPlace(name: String, x: Float, y: Float) {
    Text(
        name,
        style = TextStyle(
            fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 15.sp, color = OwCream,
            shadow = Shadow(Color(0xCC1A1A3A), Offset(2f, 3f), 2f), textAlign = TextAlign.Center,
        ),
        modifier = Modifier.offset(x = (x - 90f).dp, y = (y - 11f).dp).width(180.dp),
        maxLines = 1,
    )
}

/** One quest as a wooden signpost planted at [at] (its foot). */
@Composable
private fun OwSign(q: Quest, at: Offset, now: Long, selected: Boolean, press: Float, onTap: () -> Unit) {
    val late = q.shelf(now) == Shelf.OVERDUE
    Column(
        Modifier.offset(x = (at.x - 62f).dp, y = (at.y - 76f).dp).width(124.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .widthIn(max = 124.dp)
                .graphicsLayer { if (selected) { scaleX = press; scaleY = press } }
                .background(if (late) Color(0xFFA8432C) else Color(0xFF9A6A3A), RoundedCornerShape(3.dp))
                .border(if (selected) 2.dp else 1.dp, if (selected) OwGold else Color(0xFF5A3A1C), RoundedCornerShape(3.dp))
                .tap(onTap)
                .padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                q.text, fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 11.sp, lineHeight = 13.sp,
                color = OwCream, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            Text(q.shortDue(), fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 9.sp, color = Color(0xFFFFD98A), maxLines = 1)
        }
        Canvas(Modifier.size(28.dp, 24.dp).tap(onTap)) {
            val cx = size.width / 2f
            drawOval(Color.Black.copy(alpha = 0.25f), Offset(cx - 11.dp.toPx(), size.height - 6.dp.toPx()), Size(22.dp.toPx(), 6.dp.toPx()))
            drawRect(Color(0xFF7A5028), Offset(cx - 3.dp.toPx(), 0f), Size(4.dp.toPx(), size.height - 3.dp.toPx()))
            drawRect(Color(0xFF5A3A1C), Offset(cx + 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height - 3.dp.toPx()))
            if (late) {
                // a little red pennant on overdue signs
                val p = Path().apply {
                    moveTo(cx + 3.dp.toPx(), 2.dp.toPx()); lineTo(cx + 13.dp.toPx(), 6.dp.toPx()); lineTo(cx + 3.dp.toPx(), 10.dp.toPx()); close()
                }
                drawPath(p, OwRed)
            }
        }
    }
}

@Composable
private fun OwFaceButton(letter: String, color: Color, label: String, scale: Float, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.tap(onClick).padding(vertical = 4.dp)) {
        Box(
            Modifier.size(30.dp).graphicsLayer { scaleX = scale; scaleY = scale }.background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 15.sp, color = if (color == OwGold) Color(0xFF3A2A00) else Color.White)
        }
        Spacer(Modifier.width(6.dp))
        Text(label, fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 15.sp, color = Color.White)
    }
}

@Composable
private fun OwPill(key: String, keyColor: Color, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .tap(onClick)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.height(20.dp).background(keyColor, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(key, fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 10.sp, color = if (keyColor == OwGold) Color(0xFF3A2A00) else Color.White)
        }
        Spacer(Modifier.width(6.dp))
        Text(label, fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 13.sp, color = Color.White)
    }
}

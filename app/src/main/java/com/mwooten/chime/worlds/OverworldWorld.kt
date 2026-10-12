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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap
import kotlin.math.PI
import kotlin.math.sin

private val OwGold = Color(0xFFFFD23A)
private val OwBlue = Color(0xFF2A6AD8)
private val OwGreen = Color(0xFF2AA84A)

/** Builds a polygon in the 390×844 scene, stretched to the canvas. */
private fun DrawScope.owPoly(vararg p: Float): Path {
    val sx = size.width / 390f
    val sy = size.height / 844f
    return Path().apply {
        moveTo(p[0] * sx, p[1] * sy)
        var i = 2
        while (i < p.size) { lineTo(p[i] * sx, p[i + 1] * sy); i += 2 }
        close()
    }
}

private fun DrawScope.owScene() {
    val sx = size.width / 390f
    val sy = size.height / 844f
    // sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF4A78C8), Color(0xFFB8D8F0))))
    // clouds as flat billboards
    drawPath(owPoly(40f, 140f, 70f, 124f, 110f, 130f, 130f, 146f, 90f, 154f, 50f, 152f), Color.White, alpha = 0.9f)
    drawPath(owPoly(250f, 100f, 290f, 86f, 330f, 92f, 350f, 108f, 300f, 116f, 260f, 112f), Color.White, alpha = 0.85f)
    // ground plane, tilted like a 3D camera, with a tiled grass texture
    val ground = owPoly(-60f, 844f, 450f, 844f, 300f, 420f, 90f, 420f)
    clipPath(ground) {
        drawRect(Color(0xFF5A9A3A))
        val t = 16f
        var y = 416f
        while (y < 844f) {
            var x = 0f
            while (x < 390f) {
                fun r(dx: Float, dy: Float, w: Float, h: Float, c: Long) =
                    drawRect(Color(c), Offset((x + dx) * sx, (y + dy) * sy), Size(w * sx, h * sy))
                r(2f, 3f, 3f, 2f, 0xFF6AAA44); r(9f, 9f, 4f, 2f, 0xFF4A8A30); r(11f, 2f, 2f, 2f, 0xFF72B24A); r(4f, 11f, 2f, 3f, 0xFF4E8E34)
                x += t
            }
            y += t
        }
    }
    drawPath(owPoly(90f, 420f, 300f, 420f, 450f, 844f, 390f, 844f, 280f, 440f, 110f, 440f), Color(0xFF4A8A30), alpha = 0.35f)
    // distant hills (pop-in range)
    drawPath(owPoly(0f, 440f, 60f, 380f, 120f, 420f, 180f, 370f, 250f, 410f, 320f, 360f, 390f, 420f, 390f, 450f, 0f, 450f), Color(0xFF5A8A4A))
    // the fog wall: draw distance
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFC8DCEC), Color(0x00C8DCEC)), 340f * sy, 450f * sy),
        Offset(0f, 340f * sy), Size(size.width, 110f * sy),
    )
    // path
    drawPath(owPoly(160f, 844f, 250f, 844f, 214f, 430f, 200f, 430f), Color(0xFFC8A466))
    // a low-poly tree
    drawPath(owPoly(60f, 540f, 20f, 640f, 100f, 640f), Color(0xFF2E6A2A))
    drawPath(owPoly(60f, 540f, 60f, 640f, 100f, 640f), Color(0xFF246024))
    drawPath(owPoly(60f, 500f, 30f, 580f, 90f, 580f), Color(0xFF3A7A32))
    drawPath(owPoly(60f, 500f, 60f, 580f, 90f, 580f), Color(0xFF2E6A2A))
    drawRect(Color(0xFF6A4020), Offset(54f * sx, 640f * sy), Size(12f * sx, 28f * sy))
    // the signpost, in 3D
    drawOval(Color.Black.copy(alpha = 0.25f), Offset(280f * sx, 657f * sy), Size(52f * sx, 14f * sy))
    drawPath(owPoly(298f, 566f, 308f, 565f, 310f, 660f, 300f, 662f), Color(0xFF7A5028))
    drawPath(owPoly(308f, 565f, 314f, 568f, 316f, 658f, 310f, 660f), Color(0xFF5A3A1C))
    val board = owPoly(262f, 510f, 344f, 498f, 346f, 560f, 264f, 570f)
    clipPath(board) {
        drawRect(Color(0xFF9A6A3A))
        var y = 496f
        while (y < 572f) {
            drawRect(Color(0xFF7A5028), Offset(260f * sx, (y + 3f) * sy), Size(90f * sx, 1f * sy))
            drawRect(Color(0xFF8A5C30), Offset(260f * sx, (y + 7f) * sy), Size(90f * sx, 1f * sy))
            y += 10f
        }
    }
    drawPath(owPoly(344f, 498f, 354f, 504f, 356f, 564f, 346f, 560f), Color(0xFF6A4420))
    drawPath(owPoly(262f, 510f, 344f, 498f, 354f, 504f, 272f, 516f), Color(0xFFB8844A))
}

@Composable
fun OverworldWorld(s: WorldState, a: WorldActions) {
    val f = s.focus
    val inf = rememberInfiniteTransition(label = "ow")
    val spin by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "spin")
    val press by inf.animateFloat(1f, 1.12f, infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "press")
    val nudge by inf.animateFloat(0f, 4f, infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "nudge")
    val shadowPx = with(LocalDensity.current) { 2.dp.toPx() }
    val hud = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, color = Color.White, shadow = Shadow(Color.Black, Offset(shadowPx, shadowPx), 0f))
    val hearts = (4 - s.overdueCount).coerceIn(0, 4)

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF8AB8E8))) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize()) { owScene() }

        // the companion as a spinning hint gem
        Box(
            Modifier
                .offset(x = w * (288f / 390f), y = h * (420f / 844f))
                .size(34.dp, 46.dp)
                .graphicsLayer {
                    rotationY = spin * 360f
                    translationY = -10.dp.toPx() * sin(spin * PI.toFloat())
                    cameraDistance = 8f * density
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val k = size.width / 34f
                fun p(vararg v: Float) = Path().apply {
                    moveTo(v[0] * k, v[1] * k); var i = 2
                    while (i < v.size) { lineTo(v[i] * k, v[i + 1] * k); i += 2 }
                    close()
                }
                drawPath(p(17f, 0f, 34f, 18f, 17f, 46f), s.companion.c)
                drawPath(p(17f, 0f, 0f, 18f, 17f, 46f), lerp(s.companion.c, s.companion.cLight, 0.4f))
                drawPath(p(0f, 18f, 34f, 18f, 17f, 24f), s.companion.cLight, alpha = 0.6f)
            }
        }

        // "press A to read" over the sign
        if (f != null) {
            Row(
                Modifier
                    .offset(x = w * (256f / 390f), y = h * (470f / 844f))
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .tap { a.openQuest(f) }
                    .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(24.dp).graphicsLayer { scaleX = press; scaleY = press }.background(OwBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("A", fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 13.sp, color = Color.White) }
                Spacer(Modifier.width(6.dp))
                Text("Read", fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 13.sp, color = Color.White)
            }
        }

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 14.dp)) {
            // HUD
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.padding(start = 4.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .rotate(45f)
                                    .drawBehind { if (i < hearts) drawRect(Color.Black.copy(alpha = 0.5f), Offset(2.dp.toPx(), 2.dp.toPx()), size) }
                                    .background(if (i < hearts) OwGold else Color.Black.copy(alpha = 0.35f))
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OwPill("START", Color(0xFFD83A2A), "Menu") { a.openOptions() }
                        OwPill("C", OwGold, "Quest log") { a.openLog() }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Time.time(s.now), style = hud, fontSize = 15.sp)
                    Text(
                        "${s.open.size} quest${if (s.open.size == 1) "" else "s"}", style = hud, fontSize = 12.sp,
                        modifier = Modifier.tap { a.openLog() },
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // the text box, open
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color(0xB80A081E), RoundedCornerShape(14.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        if (f != null) {
                            append("The sign reads: ")
                            withStyle(SpanStyle(color = OwGold)) { append("“${f.text}. ${f.dueText()}.”") }
                        } else {
                            append("The sign is blank. ")
                            withStyle(SpanStyle(color = OwGold)) { append("“Nothing this way.”") }
                        }
                    },
                    fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 17.sp, lineHeight = 23.sp, color = Color.White,
                    maxLines = 4, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.tap { f?.let { a.openQuest(it) } },
                )
                Text(
                    "${s.companion.title}: ${s.line}",
                    fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 15.sp, lineHeight = 20.sp,
                    color = lerp(s.companion.c, Color.White, 0.55f), maxLines = 5, overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (f != null) {
                        OwFaceButton("A", OwBlue, "Done") { a.done(f) }
                        Spacer(Modifier.width(14.dp))
                        OwFaceButton("B", OwGreen, "Later") { a.snooze(f, 30) }
                    }
                    Spacer(Modifier.weight(1f))
                    Canvas(Modifier.size(16.dp, 14.dp).graphicsLayer { translationY = nudge * density }) {
                        drawPath(
                            Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height * 0.72f); close() },
                            OwGold,
                        )
                    }
                }
                SpillInput(
                    onSpill = a::spill,
                    textStyle = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 15.sp, color = Color.White),
                    placeholder = "Put up a new sign…",
                    placeholderColor = Color.White.copy(alpha = 0.5f),
                    cursor = OwGold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
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

@Composable
private fun OwFaceButton(letter: String, color: Color, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.tap(onClick).padding(vertical = 4.dp)) {
        Box(Modifier.size(30.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            Text(letter, fontFamily = Fonts.nunito, fontWeight = FontWeight.W900, fontSize = 15.sp, color = Color.White)
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

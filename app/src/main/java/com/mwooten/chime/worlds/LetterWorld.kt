package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LetterInk = Color(0xFF1E2A5A)
private val LetterPaper = Color(0xFFFBF6EA)

/** The letter's deckled, hand-torn edge (top and sides), from the design's clip-path. */
private val LetterDeckle = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, h * 0.01f)
    val top = listOf(0.03f to 0f, 0.07f to 0.006f, 0.12f to 0f, 0.20f to 0.005f, 0.30f to 0f, 0.45f to 0.007f, 0.60f to 0f, 0.75f to 0.006f, 0.88f to 0f, 1f to 0.005f)
    for ((x, y) in top) lineTo(w * x, h * y)
    lineTo(w * 0.994f, h * 0.20f)
    lineTo(w, h * 0.50f)
    lineTo(w * 0.995f, h * 0.80f)
    lineTo(w, h)
    lineTo(0f, h)
    lineTo(w * 0.005f, h * 0.70f)
    lineTo(0f, h * 0.40f)
    close()
}

/** Handwritten Letter: today, written out longhand by the companion, with a P.S. */
@Composable
fun LetterWorld(s: WorldState, a: WorldActions) {
    val q = s.focus
    val name = s.companion.title
    var writing by rememberSaveable { mutableStateOf(false) }
    val hand = TextStyle(fontFamily = Fonts.nanumPenScript, color = LetterInk)
    val serifSc = TextStyle(fontFamily = Fonts.cormorantSc, fontWeight = FontWeight.W600)
    val dateLine = Time.ldt(s.now).format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    val rest = s.today.filter { it.id != q?.id }

    Box(Modifier.fillMaxSize().background(Color(0xFF3A4A5A))) {
        // the desk's fine grain
        Canvas(Modifier.fillMaxSize()) {
            var x = 0f
            val step = 5.dp.toPx()
            while (x < size.width) {
                drawLine(Color.White.copy(alpha = 0.03f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                x += step
            }
        }

        // the envelope it came in, tucked behind the letter
        Box(
            Modifier.align(Alignment.TopEnd).statusBarsPadding()
                .offset(x = 20.dp, y = 16.dp)
                .size(250.dp, 150.dp)
                .rotate(8f)
                .shadow(10.dp)
                .background(Color(0xFFE9DCC0))
        ) {
            // postmark
            Canvas(Modifier.align(Alignment.TopEnd).offset(x = (-50).dp, y = 30.dp).size(60.dp, 30.dp)) {
                val c = LetterInk.copy(alpha = 0.4f)
                drawLine(c, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 2.dp.toPx())
                drawLine(c, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 2.dp.toPx())
            }
            // stamp, with the companion's own post
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-16).dp, y = 14.dp).size(46.dp, 54.dp)
                    .background(s.companion.cLight),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        Color(0xFFF4ECD8),
                        topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                        size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 3.dp.toPx()), 0f),
                        ),
                    )
                }
                Text(
                    "$name\nPost\n58¢",
                    style = serifSc.copy(fontSize = 11.sp, lineHeight = 11.sp, color = s.companion.cDark, textAlign = TextAlign.Center),
                )
            }
        }

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // the desk's two little drawers
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                Row(
                    Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Old letters",
                        style = serifSc.copy(fontSize = 14.sp, color = LetterPaper, letterSpacing = 1.sp),
                        modifier = Modifier.background(Color(0xE61E2A5A)).tap(a::openLog).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    Text(
                        "Stationery",
                        style = serifSc.copy(fontSize = 14.sp, color = LetterPaper, letterSpacing = 1.sp),
                        modifier = Modifier.background(Color(0xE61E2A5A)).tap(a::openOptions).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // the letter
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .padding(start = 16.dp, end = 22.dp)
                    .graphicsLayer { rotationZ = -1.2f }
                    .shadow(12.dp, LetterDeckle)
                    .clip(LetterDeckle)
                    .background(Brush.verticalGradient(listOf(LetterPaper, Color(0xFFF3EAD4))))
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 20.dp)
                ) {
                    Text(
                        "FROM THE DESK OF ${name.uppercase()}",
                        style = serifSc.copy(fontSize = 13.sp, letterSpacing = 3.sp, color = Color(0xFF8A7A5A), textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD8CCB0)))
                    Text(
                        dateLine,
                        style = hand.copy(fontSize = 22.sp, textAlign = TextAlign.End),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                    Text("Dear friend,", style = hand.copy(fontSize = 28.sp), modifier = Modifier.padding(top = 6.dp))

                    if (q != null) {
                        Text(
                            buildAnnotatedString {
                                append("I hope this finds you well, and with a cup of something warm. Just one thing today that can't wait: ")
                                withStyle(SpanStyle(background = Color(0x99FFD65A))) { append(s.line) }
                            },
                            style = hand.copy(fontSize = 25.sp, lineHeight = 29.sp),
                            modifier = Modifier.padding(top = 6.dp).tap { a.openQuest(q) },
                        )
                        if (rest.isNotEmpty()) {
                            Text("The rest can keep a little while:", style = hand.copy(fontSize = 25.sp, lineHeight = 29.sp), modifier = Modifier.padding(top = 10.dp))
                            Column(Modifier.padding(start = 10.dp, top = 4.dp)) {
                                for (r in rest.take(5)) {
                                    Text(
                                        "— ${r.text}, ${r.shortDue()}",
                                        style = hand.copy(fontSize = 25.sp, lineHeight = 31.sp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.tap { a.openQuest(r) },
                                    )
                                }
                                if (rest.size > 5) {
                                    Text(
                                        "— and ${rest.size - 5} more, in the other letters",
                                        style = hand.copy(fontSize = 25.sp, lineHeight = 31.sp),
                                        modifier = Modifier.tap(a::openLog),
                                    )
                                }
                            }
                        } else {
                            Text(
                                "That's the only thing on today's page.",
                                style = hand.copy(fontSize = 25.sp, lineHeight = 29.sp),
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    } else {
                        Text(s.line, style = hand.copy(fontSize = 25.sp, lineHeight = 29.sp), modifier = Modifier.padding(top = 6.dp))
                    }

                    Text("Warmly,", style = hand.copy(fontSize = 26.sp), modifier = Modifier.padding(top = 14.dp))
                    Text(
                        name,
                        style = hand.copy(fontSize = 38.sp),
                        modifier = Modifier.padding(top = 0.dp).graphicsLayer {
                            rotationZ = -4f
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        },
                    )
                    Text(
                        if (q != null) "P.S. Write back and tell me when it's done." else "P.S. Write me a new letter whenever something comes up.",
                        style = hand.copy(fontSize = 22.sp, lineHeight = 25.sp, color = Color(0xFF3A4A7A)),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                // ink blot
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = (-40).dp, y = 300.dp).size(14.dp, 12.dp)
                        .clip(RoundedCornerShape(50, 40, 55, 45))
                        .background(LetterInk.copy(alpha = 0.75f))
                )
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = (-36).dp, y = 314.dp).size(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LetterInk.copy(alpha = 0.6f))
                )
            }

            Spacer(Modifier.height(16.dp))

            if (writing || q == null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .background(LetterPaper).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dear $name, ", style = hand.copy(fontSize = 24.sp))
                    SpillInput(
                        onSpill = { a.spill(it); writing = false },
                        textStyle = hand.copy(fontSize = 26.sp),
                        placeholder = "pay rent by friday",
                        placeholderColor = Color(0xFFA8A090),
                        cursor = LetterInk,
                        modifier = Modifier.weight(1f),
                    ) { submit ->
                        Text(
                            "Seal",
                            style = serifSc.copy(fontSize = 15.sp, color = LetterPaper, letterSpacing = 1.sp),
                            modifier = Modifier.padding(start = 8.dp).background(Color(0xFF8A2A2A)).tap(submit)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (q != null) {
                    Box(
                        Modifier.weight(1f).background(LetterInk).tap { a.done(q) }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Write back: done", style = serifSc.copy(fontSize = 17.sp, color = LetterPaper, letterSpacing = 1.sp), maxLines = 1)
                    }
                }
                Box(
                    Modifier.weight(1f).background(LetterPaper).tap { writing = !writing }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (writing && q != null) "Put the pen down" else "Start a new letter",
                        style = serifSc.copy(fontSize = 17.sp, color = LetterInk, letterSpacing = 1.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

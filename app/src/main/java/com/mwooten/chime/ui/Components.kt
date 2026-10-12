package com.mwooten.chime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.label
import com.mwooten.chime.data.shelf

fun Modifier.tap(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)

@Composable
fun Chip(text: String, tone: Color = C.parchment, border: Color = C.line) {
    Text(text, style = T.small.copy(color = tone, fontSize = 13.sp),
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x8008130F))
            .border(1.dp, border, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp))
}

@Composable
fun QuestChip(q: Quest) {
    val s = q.shelf()
    when {
        q.done -> Chip("Finished", C.done, C.done.copy(alpha = .4f))
        s == Shelf.OVERDUE -> Chip(q.label(), Color(0xFFF2B396), C.ember.copy(alpha = .5f))
        q.due != null && !q.allDay && q.due - System.currentTimeMillis() < 2 * 3600_000 -> Chip(q.label(), C.brass, C.brass.copy(alpha = .55f))
        else -> Chip(q.label())
    }
}

@Composable
fun QuestRow(q: Quest, onToggle: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xCC16291F), Color(0xB3101E17))))
            .border(1.dp, Color(0x149DB3A6), RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(if (q.done) C.done else Color.Transparent)
                .border(1.5.dp, if (q.done) C.done else C.brassDim, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (q.done) Canvas(Modifier.size(14.dp)) {
                val s = size.width
                drawLine(C.night, Offset(s * .15f, s * .55f), Offset(s * .4f, s * .8f), 2.6.dp.toPx(), StrokeCap.Round)
                drawLine(C.night, Offset(s * .4f, s * .8f), Offset(s * .88f, s * .2f), 2.6.dp.toPx(), StrokeCap.Round)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(q.text, style = T.body.copy(
                color = if (q.done) C.faint else C.parchment,
                textDecoration = if (q.done) TextDecoration.LineThrough else null))
            QuestChip(q)
        }
    }
}

@Composable
fun GroupHeader(title: String, count: Int?) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title.uppercase(), style = T.eyebrow)
        if (count != null && count > 0) Text("$count", style = T.small.copy(color = C.faint))
    }
}

@Composable
fun EmptyNote(text: String) {
    Text(text, style = T.voice.copy(color = C.mist, fontSize = 19.sp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.line, RoundedCornerShape(14.dp)).padding(22.dp))
}

@Composable
fun Crumb(label: String, back: Boolean, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (back) Arrow(true)
        Text(label.uppercase(), style = T.eyebrow.copy(color = C.mist, fontSize = 13.sp))
        if (!back) Arrow(false)
    }
}

@Composable
fun Arrow(left: Boolean, color: Color = C.mist, size: Dp = 12.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.width; val sw = 1.6.dp.toPx()
        if (left) {
            drawLine(color, Offset(s * .7f, s * .1f), Offset(s * .25f, s * .5f), sw, StrokeCap.Round)
            drawLine(color, Offset(s * .25f, s * .5f), Offset(s * .7f, s * .9f), sw, StrokeCap.Round)
        } else {
            drawLine(color, Offset(s * .3f, s * .1f), Offset(s * .75f, s * .5f), sw, StrokeCap.Round)
            drawLine(color, Offset(s * .75f, s * .5f), Offset(s * .3f, s * .9f), sw, StrokeCap.Round)
        }
    }
}

@Composable
fun BrassButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(C.brassBrush).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center) {
        Text(label, style = T.body.copy(color = C.ink, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun StoneButton(label: String, modifier: Modifier = Modifier, tone: Color = C.parchment, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x9908130F)).border(1.dp, Color(0x269DB3A6), RoundedCornerShape(12.dp))
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
        Text(label, style = T.body.copy(color = tone))
    }
}

/* ---------------- Mote in the corner ---------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxScope.MoteCompanion(
    calling: Boolean,
    letter: String?,
    letterActions: List<Pair<String, () -> Unit>>,
    onTapMote: () -> Unit,
    onCloseLetter: () -> Unit,
) {
    val clock = rememberClock()
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(0f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pv")

    // the letter: what Mote has to say
    AnimatedVisibility(letter != null, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
        enter = fadeIn() + slideInVertically { it / 4 }, exit = fadeOut() + slideOutVertically { it / 4 }) {
        Column(
            Modifier.widthIn(max = 440.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFF4EDD8), Color(0xFFE5DABB))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("MOTE SAYS", style = T.eyebrow.copy(color = Color(0xFF7C6A35)))
            Text(letter ?: "", style = T.voice)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                letterActions.forEachIndexed { i, (label, fn) ->
                    Text(label, style = T.body.copy(color = if (i == 0) Color(0xFFF4EDD8) else C.ink, fontSize = 15.sp, fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (i == 0) C.ink else Color.Transparent)
                            .border(1.dp, Color(0x332A1E0E), RoundedCornerShape(10.dp))
                            .clickable { fn() }.padding(horizontal = 14.dp, vertical = 9.dp))
                }
                Text("Thanks", style = T.body.copy(color = C.ink, fontSize = 15.sp),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Color(0x332A1E0E), RoundedCornerShape(10.dp))
                        .clickable { onCloseLetter() }.padding(horizontal = 14.dp, vertical = 9.dp))
            }
        }
    }

    // the callout bubble
    AnimatedVisibility(calling && letter == null, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 96.dp, bottom = 40.dp),
        enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)) + fadeIn(), exit = fadeOut()) {
        Column(Modifier.clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)).background(Color(0xFFF4EDD8)).tap(onTapMote).padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("MOTE", style = T.eyebrow.copy(color = Color(0xFF7C6A35), fontSize = 10.sp))
            Text("Psst! Over here!", style = T.voice.copy(fontSize = 21.sp))
        }
    }

    // Mote herself
    Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 18.dp).size(84.dp).tap(onTapMote)) {
        Canvas(Modifier.size(84.dp)) {
            val t = clock.value
            val bob = sin(t * (if (calling) 5f else 2f)) * 6f
            moteSprite(Offset(size.width / 2, size.height / 2 + bob), if (calling) 0.62f + pulse * 0.08f else 0.55f, t, if (calling) 1f + pulse * 0.6f else 1f)
        }
    }
}

private fun sin(x: Float) = kotlin.math.sin(x)

@Composable
fun Toast(text: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(text != null, modifier, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
        Text(text ?: "", style = T.body.copy(fontSize = 15.sp),
            modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(C.stone)
                .border(1.dp, C.line, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 11.dp))
    }
}

@Composable
fun Spacer16() = Spacer(Modifier.size(16.dp))

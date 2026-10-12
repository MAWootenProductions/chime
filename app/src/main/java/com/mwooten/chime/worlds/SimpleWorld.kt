package com.mwooten.chime.worlds

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mwooten.chime.data.Settings
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.rememberClock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

private val SBg = Color(0xFF0F0F12)
private val SInk = Color(0xFFF1EEE7)
private val SMute = Color(0xFF8C8A93)
private val SCard = Color(0xFF1A1A1F)

private val sUi = TextStyle(fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W500, fontSize = 15.sp, color = SInk)
private val sVoice = TextStyle(fontFamily = Fonts.newsreader, fontStyle = FontStyle.Italic, fontWeight = FontWeight.W400, fontSize = 26.sp, lineHeight = 32.sp, color = SInk)
private val sEyebrow = TextStyle(fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600, fontSize = 11.sp, letterSpacing = 2.sp, color = SMute)

private enum class Mic { IDLE, LISTENING, HEARD, ERROR }

/**
 * The default mode: one sphere you tap and talk to. While you speak, the sphere swells with your
 * voice and the words appear under it as you say them. Everything else is one tap away.
 */
@Composable
fun SimpleWorld(s: WorldState, a: WorldActions) {
    val ctx = LocalContext.current
    val who = s.companion
    var mic by remember { mutableStateOf(Mic.IDLE) }
    var heard by remember { mutableStateOf("") }
    var level by remember { mutableFloatStateOf(0f) }
    var typing by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var howTo by remember { mutableStateOf(!Settings.raw(ctx).getBoolean("howto_seen", false)) }

    val recognizer = remember { if (SpeechRecognizer.isRecognitionAvailable(ctx)) SpeechRecognizer.createSpeechRecognizer(ctx) else null }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    fun listen() {
        val r = recognizer ?: run { typing = true; return }
        heard = ""; level = 0f; mic = Mic.LISTENING
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { level = 0f }
            override fun onError(error: Int) {
                level = 0f
                mic = if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) Mic.ERROR else Mic.ERROR
            }
            override fun onResults(results: Bundle?) {
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                level = 0f
                if (t.isNotBlank()) { heard = t; mic = Mic.HEARD; a.spill(t) } else mic = Mic.ERROR
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { if (it.isNotBlank()) heard = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) listen() else typing = true }
    fun tapSphere() {
        if (mic == Mic.LISTENING) { recognizer?.stopListening(); return }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val swell by animateFloatAsState(if (mic == Mic.LISTENING) 1f + level * 0.14f else 1f, tween(120), label = "swell")

    Box(Modifier.fillMaxSize().background(SBg)) {
        // a faint wash of the companion's color behind everything
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(who.c.copy(alpha = .16f), Color.Transparent), Offset(size.width / 2, size.height * .42f), size.width * .9f))
        }

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // top bar: the date on the left, one menu on the right
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = sUi.copy(fontWeight = FontWeight.W600, fontSize = 17.sp))
                    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), style = sUi.copy(color = SMute, fontSize = 13.sp))
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(if (menu) SInk else SCard).clickable { menu = !menu }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(18.dp)) {
                        val c = if (menu) SBg else SInk
                        for (i in 0..2) drawCircle(c, 2.2.dp.toPx(), Offset(size.width / 2, size.height * (0.15f + i * 0.35f)))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // the sphere
            // your companion: tap to talk, drag it around
            Sprite(who, level, mic == Mic.LISTENING, 300.dp) { tapSphere() }
            Spacer(Modifier.height(18.dp))
            Text(who.title.uppercase(), style = sEyebrow.copy(color = who.cLight.copy(alpha = .8f)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                val line = when (mic) {
                    Mic.LISTENING -> heard.ifBlank { "Listening…" }
                    Mic.HEARD -> "“$heard”"
                    Mic.ERROR -> "I didn't catch that. Tap and try again."
                    Mic.IDLE -> "Tap and tell me what you need to do."
                }
                Text(line, style = sVoice.copy(color = if (mic == Mic.IDLE || mic == Mic.ERROR) SInk.copy(alpha = .78f) else SInk),
                    textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 340.dp))
            }

            Spacer(Modifier.weight(1f))

            // next up: one line, not a list
            val f = s.focus
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SCard)
                .clickable { if (f != null) a.openQuest(f) else a.openLog() }.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (f != null && s.overdueCount > 0) Color(0xFFEF6A55) else who.c))
                Column(Modifier.weight(1f)) {
                    Text(if (f == null) "NOTHING YET" else "NEXT", style = sEyebrow)
                    Text(f?.text ?: "Your list is empty.", style = sUi.copy(fontWeight = FontWeight.W600, fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (f != null) Text(f.shortDue(), style = sUi.copy(color = SMute, fontSize = 14.sp))
            }
            val more = s.open.size - (if (s.focus != null) 1 else 0)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (more > 0) "+ $more more" else "See your list", style = sUi.copy(color = SMute, fontSize = 14.sp),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { a.openLog() }.padding(vertical = 8.dp, horizontal = 4.dp))
                Spacer(Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(20.dp)).clickable { typing = !typing }
                    .padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.size(width = 16.dp, height = 12.dp)) {
                        drawRoundRect(SMute, style = Stroke(1.4.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
                        drawLine(SMute, Offset(size.width * .25f, size.height * .7f), Offset(size.width * .75f, size.height * .7f), 1.4.dp.toPx(), StrokeCap.Round)
                    }
                    Text(if (typing) "Close" else "Type instead", style = sUi.copy(fontSize = 14.sp))
                }
            }
            AnimatedVisibility(typing, enter = fadeIn(), exit = fadeOut()) {
                SpillInput(
                    onSpill = { a.spill(it); typing = false },
                    textStyle = sUi.copy(fontSize = 17.sp),
                    placeholder = "grocery store at 2pm",
                    placeholderColor = SMute,
                    cursor = who.c,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(16.dp)).background(SCard).padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    trailing = { submit ->
                        Box(Modifier.size(44.dp).clip(CircleShape).background(who.c).clickable { submit() }, contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(16.dp)) {
                                val w = size.width
                                drawLine(SBg, Offset(w / 2, w * .9f), Offset(w / 2, w * .1f), 2.2.dp.toPx(), StrokeCap.Round)
                                drawLine(SBg, Offset(w * .15f, w * .45f), Offset(w / 2, w * .1f), 2.2.dp.toPx(), StrokeCap.Round)
                                drawLine(SBg, Offset(w * .85f, w * .45f), Offset(w / 2, w * .1f), 2.2.dp.toPx(), StrokeCap.Round)
                            }
                        }
                    },
                )
            }
        }

        // the small menu: only the things you'd actually want from here
        AnimatedVisibility(menu, Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(top = 62.dp, end = 22.dp), enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.width(240.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1F1F25)).border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(18.dp)).padding(6.dp)) {
                SimpleMenuItem("Personality", who.title) { menu = false; a.openPersonality() }
                SimpleMenuItem("Mode", "Simple") { menu = false; a.openMode() }
                SimpleMenuItem("Your list", "${s.open.size} open") { menu = false; a.openLog() }
                SimpleMenuItem("Settings", "") { menu = false; a.openOptions() }
            }
        }

        // first open: three lines on how this works
        AnimatedVisibility(howTo, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0xCC0B0B0E)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }) {
                Column(Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(18.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SInk).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("How Chime works", style = TextStyle(fontFamily = Fonts.newsreader, fontWeight = FontWeight.W600, fontSize = 26.sp, color = SBg))
                    HowStep("1", "Tap the sphere and say what you need to do.", "“Grocery store at 2pm.”  “Call Mom tomorrow.”", who.c)
                    HowStep("2", "Check what I heard, then tap Confirm.", "Wrong? Just say the fix.", who.c)
                    HowStep("3", "When it's time, ${who.title} calls you.", "Answer, or reply right from the notification.", who.c)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SBg).clickable {
                        Settings.raw(ctx).edit().putBoolean("howto_seen", true).apply(); howTo = false
                    }.padding(15.dp), contentAlignment = Alignment.Center) {
                        Text("Got it", style = sUi.copy(fontWeight = FontWeight.W600, fontSize = 16.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleMenuItem(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = sUi.copy(fontWeight = FontWeight.W600, fontSize = 16.sp), modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, style = sUi.copy(color = SMute, fontSize = 14.sp))
    }
}

@Composable
private fun HowStep(n: String, line: String, hint: String, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Text(n, style = sUi.copy(color = SBg, fontWeight = FontWeight.W600, fontSize = 14.sp))
        }
        Column(Modifier.weight(1f)) {
            Text(line, style = sUi.copy(color = SBg, fontWeight = FontWeight.W600, fontSize = 16.sp, lineHeight = 21.sp))
            Text(hint, style = sUi.copy(color = Color(0xFF5E5C66), fontSize = 14.sp, lineHeight = 19.sp))
        }
    }
}

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when { h < 5 -> "Up late"; h < 12 -> "Good morning"; h < 17 -> "Good afternoon"; else -> "Good evening" }
}

/**
 * A glass sphere with slow currents of the companion's color inside it. Listening speeds the
 * currents up and lets them push toward the surface with your voice.
 */
@Composable
private fun Sphere(c: Color, light: Color, dark: Color, swell: Float, listening: Boolean, level: Float) {
    val clock = rememberClock()
    Canvas(Modifier.size(250.dp)) {
        val t = clock.value * (if (listening) 2.2f else 1f)
        val cx = size.width / 2; val cy = size.height / 2
        val r = size.minDimension * 0.36f * swell
        val center = Offset(cx, cy)
        // halo
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = .38f + level * .3f), Color.Transparent), center, r * 1.75f), r * 1.75f, center)
        val ball = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, r)) }
        clipPath(ball) {
            // body: deep at the rim, brighter toward the middle
            drawCircle(Brush.radialGradient(listOf(c, dark), Offset(cx - r * .2f, cy - r * .25f), r * 1.25f), r, center)
            // three currents drifting on their own orbits
            val currents = listOf(
                Triple(light.copy(alpha = .75f), 0.55f, 0.0f),
                Triple(c.copy(alpha = .9f), 0.62f, 2.1f),
                Triple(Color.White.copy(alpha = .35f), 0.45f, 4.2f),
            )
            currents.forEachIndexed { i, (col, rr, ph) ->
                val sp = 0.35f + i * 0.13f
                val reach = r * (0.38f + level * 0.25f)
                val p = Offset(cx + cos(t * sp + ph) * reach, cy + sin(t * sp * 1.3f + ph) * reach * 0.8f)
                drawCircle(Brush.radialGradient(listOf(col, Color.Transparent), p, r * rr), r * rr, p)
            }
            // a darker band low in the sphere gives it weight
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, dark.copy(alpha = .55f)), cy, cy + r), Offset(cx - r, cy), Size(r * 2, r))
        }
        // glass: a soft highlight and a thin rim
        val hl = Offset(cx - r * .38f, cy - r * .45f)
        drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = .55f), Color.Transparent), hl, r * .42f), Offset(hl.x - r * .42f, hl.y - r * .26f), Size(r * .84f, r * .52f))
        drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = .35f), Color.Transparent, Color.White.copy(alpha = .08f)), cy - r, cy + r), r, center, style = Stroke(1.2.dp.toPx()))
    }
}

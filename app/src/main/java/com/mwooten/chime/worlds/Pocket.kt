package com.mwooten.chime.worlds

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mwooten.chime.data.Settings
import com.mwooten.chime.ui.Fonts
import kotlinx.coroutines.delay

/**
 * Your character, carried into every mode: a small copy of the same look, color and personality
 * that floats in the corner. Tap it to talk (same voice capture as Simple mode, with Google's
 * voice-typing popup as the fallback), or tap the little keyboard to type.
 */
@Composable
fun Pocket(s: WorldState, a: WorldActions) {
    val ctx = LocalContext.current
    val look = Settings.look(ctx)
    val finish = Settings.finish(ctx)
    val who = s.companion
    var listening by remember { mutableStateOf(false) }
    var level by remember { mutableFloatStateOf(0f) }
    var bubble by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    // say hello once per visit so you know who's here
    LaunchedEffect(Unit) { bubble = who.callout; delay(3500); if (!listening) bubble = "" }
    LaunchedEffect(bubble, listening) { if (bubble.isNotBlank() && !listening) { delay(5000); bubble = "" } }

    fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
        .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        .putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you need to do?")

    val popup = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val t = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (t.isNotBlank()) { bubble = "“$t”"; a.spill(t) }
    }
    fun openPopup() { listening = false; level = 0f; try { popup.launch(intent()) } catch (e: Exception) { typing = true } }

    fun listen() {
        recognizer?.destroy(); recognizer = null
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { openPopup(); return }
        val r = try { SpeechRecognizer.createSpeechRecognizer(ctx) } catch (e: Exception) { null } ?: run { openPopup(); return }
        recognizer = r; listening = true; bubble = "Listening…"
        var heard = ""
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { level = 0f }
            override fun onError(error: Int) {
                listening = false; level = 0f
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    if (heard.isNotBlank()) { bubble = "“$heard”"; a.spill(heard) } else bubble = "Didn't catch that."
                } else openPopup()
            }
            override fun onResults(results: Bundle?) {
                listening = false; level = 0f
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().ifBlank { heard }
                if (t.isNotBlank()) { bubble = "“$t”"; a.spill(t) } else bubble = "Didn't catch that."
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { if (it.isNotBlank()) { heard = it; bubble = it } }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try { r.startListening(intent().putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)) } catch (e: Exception) { openPopup() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) listen() else typing = true }
    fun tap() {
        if (listening) { recognizer?.stopListening(); return }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val ink = Color(0xFF1B1A17)
    val paper = Color(0xFFF6F2E8)
    val accent = Color(finish.c)
    Box(Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(end = 6.dp, bottom = 84.dp), contentAlignment = Alignment.BottomEnd) {
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AnimatedVisibility(bubble.isNotBlank() && !typing, enter = fadeIn() + scaleIn(initialScale = .85f), exit = fadeOut() + scaleOut(targetScale = .85f)) {
                Text(bubble, maxLines = 3,
                    style = TextStyle(fontFamily = Fonts.newsreader, fontStyle = FontStyle.Italic, fontSize = 16.sp, lineHeight = 21.sp, color = ink),
                    modifier = Modifier.padding(end = 10.dp).widthIn(max = 230.dp).clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)).background(paper).padding(horizontal = 12.dp, vertical = 8.dp))
            }
            AnimatedVisibility(typing, enter = fadeIn(), exit = fadeOut()) {
                SpillInput(
                    onSpill = { t -> typing = false; bubble = "“$t”"; a.spill(t) },
                    textStyle = TextStyle(fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W500, fontSize = 16.sp, color = ink),
                    placeholder = "Tell ${who.title}…",
                    placeholderColor = Color(0xFF8A857A),
                    cursor = accent,
                    modifier = Modifier.padding(end = 10.dp).widthIn(min = 220.dp, max = 280.dp).clip(RoundedCornerShape(14.dp)).background(paper).padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Box(Modifier.padding(bottom = 18.dp).size(34.dp).clip(CircleShape).background(Color(0xCC1A1A1F)).clickable { typing = !typing },
                    contentAlignment = Alignment.Center) {
                    Text(if (typing) "✕" else "⌨", style = TextStyle(fontSize = 15.sp, color = Color(0xFFF1EEE7)))
                }
                Character(look, finish, who, level, listening, 104.dp, draggable = true) { tap() }
            }
        }
    }
}

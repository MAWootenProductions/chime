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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Live voice capture any mode can use: tap the character → [toggle]. */
@Stable
class Voice internal constructor() {
    var listening by mutableStateOf(false); internal set
    /** 0..1 loudness while listening, to drive the character's mouth and swell. */
    var level by mutableFloatStateOf(0f); internal set
    /** Words heard so far (partial while listening, final after). */
    var heard by mutableStateOf(""); internal set
    /** Plain-words problem to show, or "" if none. */
    var note by mutableStateOf(""); internal set
    internal var toggleImpl: () -> Unit = {}
    /** Start listening, or stop if already listening. */
    fun toggle() = toggleImpl()
}

/**
 * Speech for a mode. A fresh recognizer per tap (reusing one gets stuck on newer Pixels); if it
 * fails for any reason besides "heard nothing", Google's voice-typing popup takes over.
 * [onHeard] gets the final words.
 */
@Composable
fun rememberVoice(onHeard: (String) -> Unit): Voice {
    val ctx = LocalContext.current
    val v = remember { Voice() }
    val heardCb by rememberUpdatedState(onHeard)
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
        .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        .putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you need to do?")

    val popup = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val t = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (t.isNotBlank()) { v.heard = t; heardCb(t) }
    }
    fun openPopup() {
        v.listening = false; v.level = 0f
        try { popup.launch(intent()) } catch (e: Exception) { v.note = "Voice isn't available on this phone. Type instead." }
    }
    fun finish(t: String) { v.listening = false; v.level = 0f; if (t.isNotBlank()) { v.heard = t; heardCb(t) } else v.note = "I didn't catch that." }
    fun listen() {
        recognizer?.destroy(); recognizer = null
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { openPopup(); return }
        val r = try { SpeechRecognizer.createSpeechRecognizer(ctx) } catch (e: Exception) { null } ?: run { openPopup(); return }
        recognizer = r; v.listening = true; v.heard = ""; v.note = ""
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { v.level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { v.level = 0f }
            override fun onError(error: Int) {
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) finish(v.heard)
                else openPopup()
            }
            override fun onResults(results: Bundle?) {
                finish(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().ifBlank { v.heard })
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { if (it.isNotBlank()) v.heard = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try { r.startListening(intent().putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)) } catch (e: Exception) { openPopup() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) listen() else v.note = "Chime needs the microphone. Turn it on in Settings › Apps › Chime › Permissions."
    }
    v.toggleImpl = {
        if (v.listening) recognizer?.stopListening()
        else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    return v
}

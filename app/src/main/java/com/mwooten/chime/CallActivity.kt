package com.mwooten.chime

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.QuestStore
import com.mwooten.chime.ui.BrassButton
import com.mwooten.chime.ui.C
import com.mwooten.chime.ui.ForestScene
import com.mwooten.chime.ui.StoneButton
import com.mwooten.chime.ui.T
import com.mwooten.chime.ui.moteSprite
import com.mwooten.chime.ui.rememberClock

/** Mote's incoming call: rings over the lock screen; answer to hear what she has to say. */
class CallActivity : ComponentActivity() {
    private var answered by mutableStateOf(false)
    private var questId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true); setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        QuestStore.load(this)
        read(intent)
        setContent {
            val q = questId?.let { QuestStore.get(this, it) }
            CallScreen(
                q = q, answered = answered,
                onAnswer = { Notifier.stopRinging(this); answered = true },
                onLater = {
                    Notifier.stopRinging(this)
                    if (q != null && !q.done) QuestStore.snooze(this, q.id, 10)
                    finish()
                },
                onDone = { if (q != null) QuestStore.setDone(this, q.id, true); finish() },
                onSnooze = { if (q != null) QuestStore.snooze(this, q.id, 30); finish() },
                onOpen = {
                    getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
                    startActivity(Intent(this, MainActivity::class.java)
                        .putExtra(Notifier.EXTRA_MESSAGE, q?.let { MoteVoice.forQuest(it) } ?: "That was just a test call. I'll ring like that whenever a quest comes due.")
                        .putExtra(Notifier.EXTRA_QUEST, q?.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                },
                onClose = { finish() },
            )
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); read(intent) }

    private fun read(i: Intent) {
        questId = i.getStringExtra(Notifier.EXTRA_QUEST)
        if (i.getBooleanExtra(Notifier.EXTRA_ANSWERED, false)) { Notifier.stopRinging(this); answered = true }
    }
}

@Composable
private fun CallScreen(
    q: Quest?, answered: Boolean,
    onAnswer: () -> Unit, onLater: () -> Unit,
    onDone: () -> Unit, onSnooze: () -> Unit, onOpen: () -> Unit, onClose: () -> Unit,
) {
    val clock = rememberClock()
    val pulse by rememberInfiniteTransition(label = "ring").animateFloat(0f, 1f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "rp")
    Box(Modifier.fillMaxSize().background(C.night)) {
        ForestScene(Modifier.fillMaxSize(), dim = 0.55f)
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(56.dp))
            Text(if (answered) "ON THE LINE" else "INCOMING", style = T.eyebrow.copy(color = C.mist, fontSize = 13.sp, letterSpacing = 4.sp))
            Spacer(Modifier.height(6.dp))
            Text("Mote", style = T.logo.copy(fontSize = 54.sp))
            Spacer(Modifier.height(18.dp))
            Canvas(Modifier.size(200.dp)) {
                val t = clock.value
                if (!answered) for (k in 0..2) {
                    val ph = ((t * 0.7f + k / 3f) % 1f)
                    drawCircle(C.glow.copy(alpha = (1f - ph) * 0.35f), size.width * (0.2f + ph * 0.3f), center, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                }
                moteSprite(Offset(center.x, center.y + kotlin.math.sin(t * 4f) * 6f), if (answered) 1.5f else 1.4f + pulse * 0.15f, t, 1f + if (answered) 0f else pulse * 0.5f)
            }
            Spacer(Modifier.height(18.dp))
            AnimatedContent(answered, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) }, label = "call") { ans ->
                if (!ans) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(MoteVoice.CALLOUT, style = T.heading.copy(fontSize = 34.sp), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(if (q != null) "About a quest due soon" else "A test call", style = T.small)
                    }
                } else {
                    Column(Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFF4EDD8), Color(0xFFE5DABB)))).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("MOTE SAYS", style = T.eyebrow.copy(color = Color(0xFF7C6A35)))
                        Text(q?.let { MoteVoice.forQuest(it) } ?: "This is how I'll sound when a quest comes due. Answer, and I'll tell you what it is.", style = T.voice.copy(fontSize = 24.sp, lineHeight = 30.sp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (!answered) {
                Row(Modifier.fillMaxWidth().padding(bottom = 48.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RoundAction("Later", Color(0xFF6E3B2C), false, onLater)
                    RoundAction("Answer", Color(0xFF3F7A4C), true, onAnswer, pulse)
                }
            } else {
                Column(Modifier.widthIn(max = 460.dp).fillMaxWidth().padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (q != null && !q.done) {
                        BrassButton("Done it", Modifier.fillMaxWidth(), onDone)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StoneButton("Snooze 30 min", Modifier.weight(1f), onClick = onSnooze)
                            StoneButton("Open Chime", Modifier.weight(1f), onClick = onOpen)
                        }
                    } else {
                        BrassButton("Open Chime", Modifier.fillMaxWidth(), onOpen)
                    }
                    StoneButton("Hang up", Modifier.fillMaxWidth(), C.mist, onClose)
                }
            }
        }
    }
}

@Composable
private fun RoundAction(label: String, color: Color, answer: Boolean, onClick: () -> Unit, pulse: Float = 0f) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(76.dp + (if (answer) (pulse * 6).dp else 0.dp)).clip(CircleShape).background(color).clickable(onClick = onClick),
            contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(28.dp)) {
                val s = size.width; val sw = 3.dp.toPx()
                if (answer) {
                    drawLine(Color.White, Offset(s * .12f, s * .55f), Offset(s * .4f, s * .82f), sw, StrokeCap.Round)
                    drawLine(Color.White, Offset(s * .4f, s * .82f), Offset(s * .9f, s * .2f), sw, StrokeCap.Round)
                } else {
                    drawLine(Color.White, Offset(s * .2f, s * .2f), Offset(s * .8f, s * .8f), sw, StrokeCap.Round)
                    drawLine(Color.White, Offset(s * .8f, s * .2f), Offset(s * .2f, s * .8f), sw, StrokeCap.Round)
                }
            }
        }
        Text(label, style = T.body.copy(color = C.parchment))
    }
}

package com.mwooten.chime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mwooten.chime.data.Brain
import com.mwooten.chime.data.focusQuest
import com.mwooten.chime.projects.ProjectsScreen
import com.mwooten.chime.worlds.SpillInput
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.QuestStore
import com.mwooten.chime.data.Settings
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.label
import com.mwooten.chime.ui.C
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.ForestScene
import com.mwooten.chime.ui.LogScreen
import com.mwooten.chime.ui.OptionsScreen
import com.mwooten.chime.ui.QuestSheet
import com.mwooten.chime.ui.T
import com.mwooten.chime.ui.Toast
import com.mwooten.chime.worlds.WorldActions
import com.mwooten.chime.worlds.WorldHost
import com.mwooten.chime.worlds.WorldId
import com.mwooten.chime.worlds.WorldState
import kotlinx.coroutines.delay

/** Something the companion is holding for you (opened from a notification). */
data class MoteMessage(val text: String, val questId: String?)

class MainActivity : ComponentActivity() {
    private val incoming = mutableStateOf<MoteMessage?>(null)
    private val goTo = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        QuestStore.load(this)
        Scheduler.rescheduleAll(this)
        Scheduler.scheduleGentle(this)
        Scheduler.schedulePoll(this)
        readIntent(intent)
        setContent { ChimeRoot(incoming.value, goTo.value, { goTo.value = null }) { incoming.value = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(i: Intent?) {
        i?.getStringExtra(Notifier.EXTRA_SCREEN)?.let { goTo.value = it; i.removeExtra(Notifier.EXTRA_SCREEN) }
        val msg = i?.getStringExtra(Notifier.EXTRA_MESSAGE) ?: return
        incoming.value = MoteMessage(msg, i.getStringExtra(Notifier.EXTRA_QUEST))
        i.removeExtra(Notifier.EXTRA_MESSAGE)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChimeRoot(incoming: MoteMessage?, goTo: String?, consumeGoTo: () -> Unit, consumeIncoming: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var thinking by remember { mutableStateOf(false) }
    val quests by QuestStore.quests.collectAsState()
    var screen by rememberSaveable { mutableStateOf("world") }
    var view by rememberSaveable { mutableStateOf("ahead") }
    var sheetId by rememberSaveable { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var toastKey by remember { mutableIntStateOf(0) }
    var settingsRev by remember { mutableIntStateOf(0) }
    var letter by remember { mutableStateOf<MoteMessage?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // keep "time left" text fresh
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) { requestNotifications() }

    fun say(s: String) { toast = s; toastKey++ }
    LaunchedEffect(toastKey) { if (toast != null) { delay(2600); toast = null } }

    LaunchedEffect(goTo) { if (goTo != null) { screen = goTo; consumeGoTo() } }

    LaunchedEffect(incoming) {
        if (incoming != null) { letter = incoming; screen = "world"; consumeIncoming() }
    }

    val worldId = remember(settingsRev) { WorldId.byName(Settings.world(ctx)) }
    val companion = remember(settingsRev) { Settings.companion(ctx) }
    val state = remember(quests, companion, now) { WorldState(quests, companion, System.currentTimeMillis()) }

    val actions = remember {
        object : WorldActions {
            override fun done(q: Quest) {
                QuestStore.setDone(ctx, q.id, !q.done)
                if (!q.done) say("Done: ${q.text}")
            }
            override fun snooze(q: Quest, minutes: Int) {
                QuestStore.snooze(ctx, q.id, minutes)
                say(if (minutes >= 60) "Later: back in ${minutes / 60} hour${if (minutes >= 120) "s" else ""}." else "Later: back in $minutes minutes.")
            }
            override fun spill(text: String) {
                if (text.isBlank()) return
                thinking = true
                letter = MoteMessage("…", null)
                scope.launch {
                    val r = Brain.handle(ctx, text, null)
                    thinking = false
                    letter = MoteMessage(r.reply, focusQuest(QuestStore.all(ctx))?.id)
                }
            }
            override fun openLog() { screen = "log" }
            override fun openOptions() { screen = "options" }
            override fun openQuest(q: Quest) { sheetId = q.id }
        }
    }

    BackHandler(enabled = screen == "log") { screen = "world" }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(screen, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "screen") { s ->
            when (s) {
                "log" -> Box(Modifier.fillMaxSize().background(C.night)) {
                    ForestScene(Modifier.fillMaxSize(), dim = 1f)
                    LogScreen(quests, view, { view = it }, { screen = "world" }, { screen = "world" }, { actions.done(it) }) { sheetId = it.id }
                }
                "projects" -> ProjectsScreen { screen = "options" }
                "options" -> OptionsScreen(
                    focus = state.focus,
                    onClose = { screen = "world" },
                    onChanged = { settingsRev++ },
                    toast = ::say,
                    requestNotifications = ::requestNotifications,
                    onOpenProjects = { screen = "projects" },
                )
                else -> WorldHost(worldId, state, actions)
            }
        }

        // a message the companion was holding for you
        val l = letter
        AnimatedVisibility(l != null && screen == "world", Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(16.dp),
            enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
            val lq = l?.questId?.let { id -> quests.firstOrNull { it.id == id } }
            Column(
                Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF6F2E8)).border(2.dp, Color(companion.color), RoundedCornerShape(18.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(companion.color)))
                    Text(companion.title.uppercase(), style = T.small.copy(fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, letterSpacing = 2.sp, color = Color(companion.dark)))
                }
                Text(l?.text ?: "", style = T.body.copy(fontFamily = Fonts.fraunces, fontWeight = FontWeight.W600, fontSize = 20.sp, lineHeight = 27.sp, color = Color(0xFF1B1A17)))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lq != null && !lq.done) {
                        LetterButton("Done", true, companion.color) { actions.done(lq); letter = null }
                        LetterButton("Later", false, companion.color) { actions.snooze(lq, 30); letter = null }
                        LetterButton("Open", false, companion.color) { sheetId = lq.id; letter = null }
                    }
                    LetterButton("Close", false, companion.color) { letter = null }
                }
                if (!thinking) SpillInput(
                    onSpill = { text ->
                        val about = l?.questId
                        thinking = true
                        letter = MoteMessage("…", about)
                        scope.launch {
                            val r = Brain.handle(ctx, text, about)
                            thinking = false
                            letter = MoteMessage(r.reply, about?.let { id -> QuestStore.get(ctx, id)?.takeIf { !it.done }?.id } ?: focusQuest(QuestStore.all(ctx))?.id)
                        }
                    },
                    textStyle = T.body.copy(fontFamily = Fonts.nunito, fontWeight = FontWeight.W600, fontSize = 16.sp, color = Color(0xFF1B1A17)),
                    placeholder = "Reply to ${companion.title}…",
                    placeholderColor = Color(0xFF8A857A),
                    cursor = Color(companion.color),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }

        Toast(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp))

        val sq = sheetId?.let { id -> quests.firstOrNull { it.id == id } }
        if (sq != null) QuestSheet(
            q = sq,
            onDismiss = { sheetId = null },
            onRename = { QuestStore.upsert(ctx, sq.copy(text = it)) },
            onToggle = { actions.done(sq) },
            onSnooze = { actions.snooze(sq, 60) },
            onTomorrow = {
                val d = sq.due
                val tomorrow = java.time.LocalDate.now().plusDays(1)
                val nd = if (d != null && !sq.allDay) Time.ms(tomorrow.atTime(Time.ldt(d).toLocalTime())) else Time.ms(tomorrow)
                QuestStore.upsert(ctx, sq.copy(due = nd, allDay = d == null || sq.allDay, done = false))
                say("Moved to tomorrow.")
            },
            onDelete = { QuestStore.delete(ctx, sq.id); say("Quest deleted.") },
        )
    }
}

@Composable
private fun LetterButton(label: String, primary: Boolean, color: Long, onClick: () -> Unit) {
    Text(label, style = T.body.copy(fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 15.sp, color = if (primary) Color.White else Color(0xFF1B1A17)),
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (primary) Color(color) else Color.Transparent)
            .border(1.dp, Color(0x332A1E0E), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp))
}

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.QuestStore
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.label
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.C
import com.mwooten.chime.ui.ForestScene
import com.mwooten.chime.ui.LogScreen
import com.mwooten.chime.ui.MoteCompanion
import com.mwooten.chime.ui.QuestSheet
import com.mwooten.chime.ui.SpillScreen
import com.mwooten.chime.ui.TitleScreen
import com.mwooten.chime.ui.Toast
import kotlinx.coroutines.delay

/** A message Mote is holding for you (from a notification, or noticed while the app is open). */
data class MoteMessage(val text: String, val questId: String?)

class MainActivity : ComponentActivity() {
    private val incoming = mutableStateOf<MoteMessage?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        QuestStore.load(this)
        Scheduler.rescheduleAll(this)
        Scheduler.scheduleGentle(this)
        readIntent(intent)
        setContent { ChimeRoot(incoming.value) { incoming.value = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(i: Intent?) {
        val msg = i?.getStringExtra(Notifier.EXTRA_MESSAGE) ?: return
        incoming.value = MoteMessage(msg, i.getStringExtra(Notifier.EXTRA_QUEST))
        i.removeExtra(Notifier.EXTRA_MESSAGE)
    }
}

@Composable
fun ChimeRoot(incoming: MoteMessage?, consumeIncoming: () -> Unit) {
    val ctx = LocalContext.current
    val quests by QuestStore.quests.collectAsState()
    var screen by rememberSaveable { mutableStateOf("title") }
    var pressed by rememberSaveable { mutableStateOf(false) }
    var view by rememberSaveable { mutableStateOf("ahead") }
    var sheetId by rememberSaveable { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var toastKey by remember { mutableStateOf(0) }

    // Mote's state
    var pending by remember { mutableStateOf<MoteMessage?>(null) }
    var letter by remember { mutableStateOf<MoteMessage?>(null) }
    val heardInApp = remember { mutableSetOf<String>() }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) { requestNotifications() }

    fun say(s: String) { toast = s; toastKey++ }
    LaunchedEffect(toastKey) { if (toast != null) { delay(2600); toast = null } }

    // a message arrived from a notification: open it right away
    LaunchedEffect(incoming) {
        if (incoming != null) { letter = incoming; pending = null; if (screen == "title") { pressed = true; screen = "log" }; consumeIncoming() }
    }

    // while the app is open, Mote notices anything coming due in the next 15 minutes
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            val now = System.currentTimeMillis()
            if (pending == null && letter == null) {
                val urgent = quests.filter { !it.done && it.due != null && !it.allDay }
                    .firstOrNull { q -> val d = q.due!!; (d - now in 0..15 * 60_000L || q.shelf(now) == Shelf.OVERDUE) && q.id + d !in heardInApp }
                if (urgent != null) {
                    heardInApp += urgent.id + urgent.due
                    pending = MoteMessage(MoteVoice.forQuest(urgent, now), urgent.id)
                }
            }
        }
    }

    fun toggle(q: Quest) {
        QuestStore.setDone(ctx, q.id, !q.done)
        if (!q.done) say("Quest complete: ${q.text}")
    }

    BackHandler(enabled = screen != "title") { screen = "title" }

    val dim by animateFloatAsState(if (screen == "title") 0f else 1f, tween(800), label = "dim")

    Box(Modifier.fillMaxSize().background(C.night)) {
        ForestScene(Modifier.fillMaxSize(), dim = dim, showMote = screen == "title")

        AnimatedContent(screen, transitionSpec = {
            (fadeIn(tween(500, 120)) + slideInVertically(tween(500, 120)) { it / 20 }) togetherWith fadeOut(tween(250))
        }, label = "screen") { s ->
            when (s) {
                "spill" -> SpillScreen(
                    recent = quests.filter { !it.done }.sortedByDescending { it.created }.take(4),
                    onBack = { screen = "title" }, onLog = { screen = "log" },
                    onAdd = { p, raw ->
                        val q = Quest(Quest.newId(), p.title, p.due, p.allDay, p.kind)
                        QuestStore.upsert(ctx, q)
                        say("Added “${q.text}” · ${q.label()}")
                    },
                    onToggle = ::toggle, onOpen = { sheetId = it.id }, toast = ::say,
                )
                "log" -> LogScreen(quests, view, { view = it }, { screen = "title" }, { screen = "spill" }, ::toggle) { sheetId = it.id }
                else -> TitleScreen(
                    pressed = pressed, onPress = { pressed = true },
                    openCount = quests.count { !it.done },
                    overdueCount = quests.count { it.shelf() == Shelf.OVERDUE },
                    onSpill = { screen = "spill" }, onLog = { screen = "log" },
                    toast = ::say, requestNotifications = ::requestNotifications,
                )
            }
        }

        if (screen != "title") {
            val lq = letter?.questId?.let { id -> quests.firstOrNull { it.id == id } }
            val actions = buildList<Pair<String, () -> Unit>> {
                if (lq != null && !lq.done) {
                    add("Done it" to { toggle(lq); letter = null })
                    add("Snooze 30 min" to { QuestStore.snooze(ctx, lq.id, 30); say("Mote will remind you in 30 minutes."); letter = null })
                    add("Open" to { sheetId = lq.id; letter = null })
                } else if (quests.none { !it.done }) {
                    add("Open Spill" to { screen = "spill"; letter = null })
                }
            }
            MoteCompanion(
                calling = pending != null,
                letter = letter?.text,
                letterActions = actions,
                onTapMote = {
                    if (letter != null) letter = null
                    else {
                        letter = pending ?: MoteVoice.gentle(quests).let { (t, q) -> MoteMessage(t, q?.id) }
                        pending = null
                    }
                },
                onCloseLetter = { letter = null },
            )
        }

        Toast(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 110.dp))

        val sq = sheetId?.let { id -> quests.firstOrNull { it.id == id } }
        if (sq != null) QuestSheet(
            q = sq,
            onDismiss = { sheetId = null },
            onRename = { QuestStore.upsert(ctx, sq.copy(text = it)) },
            onToggle = { toggle(sq) },
            onSnooze = { QuestStore.snooze(ctx, sq.id, 60); say("Snoozed for an hour.") },
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

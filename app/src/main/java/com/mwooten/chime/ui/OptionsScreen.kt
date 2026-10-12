package com.mwooten.chime.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as SysSettings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.mwooten.chime.Notifier
import com.mwooten.chime.Scheduler
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.QuestStore
import com.mwooten.chime.data.Settings
import com.mwooten.chime.worlds.Category
import com.mwooten.chime.worlds.Character
import com.mwooten.chime.worlds.Finish
import com.mwooten.chime.worlds.Look
import com.mwooten.chime.worlds.WorldId
import kotlinx.coroutines.delay

private val Ink = Color(0xFF14151B)
private val Paper = Color(0xFFF2EFE8)
private val Dim = Color(0xFF8A8C96)
private val Line = Color(0xFF2A2C35)

private val head = TextStyle(fontFamily = Fonts.fraunces, fontWeight = FontWeight.W800, fontSize = 34.sp, color = Paper)
private val rowT = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 18.sp, color = Paper)
private val small = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W600, fontSize = 14.sp, color = Dim)
private val eyebrow = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 12.sp, letterSpacing = 2.sp, color = Dim)

/**
 * Options, game-menu style: a short top level, and everything else one level down.
 * Top: World · Companion · Reminders · Permissions.
 */
@Composable
fun OptionsScreen(
    focus: Quest?,
    onClose: () -> Unit,
    onChanged: () -> Unit,
    toast: (String) -> Unit,
    requestNotifications: () -> Unit,
    onOpenProjects: () -> Unit,
    startPage: String = "top",
) {
    val ctx = LocalContext.current
    var page by rememberSaveable { mutableStateOf(startPage) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(page) { while (page == "permissions") { delay(1000); tick++ } }
    fun back() { if (page == "top" || page == startPage) onClose() else page = "top" }
    BackHandler { back() }

    Box(Modifier.fillMaxSize().background(Ink)) {
        AnimatedContent(page, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "opt") { p ->
            Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (p == "top" || p == startPage) "‹ Back" else "‹ Settings", style = small.copy(color = Paper),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { back() }.padding(8.dp))
                }
                @Suppress("UNUSED_VARIABLE") val refresh = tick
                when (p) {
                    "world" -> WorldPicker { Settings.setWorld(ctx, it.name); onChanged(); toast("Switched to ${it.title} mode."); onClose() }
                    "companion" -> CompanionPicker(focus) { Settings.setCompanion(ctx, it); onChanged(); toast("${it.title} is with you now."); onClose() }
                    "look" -> LookPicker(onChanged)
                    "reminders" -> Reminders(ctx, toast) { tick++ }
                    "permissions" -> Permissions(ctx, requestNotifications)
                    "brain" -> SmartReplies(ctx, toast) { page = "top" }
                    else -> {
                        Text("Settings", style = head)
                        Spacer(Modifier.height(6.dp))
                        val w = WorldId.byName(Settings.world(ctx)); val c = Settings.companion(ctx)
                        val lk = Settings.look(ctx); val fin = Settings.finish(ctx)
                        TopRow("Look", "${lk.title} · ${fin.title}", Color(fin.c)) { page = "look" }
                        TopRow("Mode", w.title) { page = "world" }
                        TopRow("Personality", "${c.title} · ${c.trait.lowercase()}", Color(c.color)) { page = "companion" }
                        TopRow("Smart replies", if (Settings.apiKey(ctx).isNotBlank()) "On · powered by Claude" else "Basic · add an API key to turn on",
                            if (Settings.apiKey(ctx).isNotBlank()) Color(0xFF9FD6A0) else null) { page = "brain" }
                        TopRow("Projects", if (Settings.githubToken(ctx).isNotBlank()) "Tracking your GitHub repos" else "Connect GitHub to track Claude's work") { onOpenProjects() }
                        TopRow("Reminders", "${Settings.chatter(ctx)} check-ins · ${if (Settings.rings(ctx)) "rings" else "chimes"}") { page = "reminders" }
                        TopRow("Permissions", if (allGranted(ctx)) "All set" else "Needs attention", if (allGranted(ctx)) null else Color(0xFFE58A5F)) { page = "permissions" }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TopRow(title: String, value: String, dot: Color? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1C1E26)).clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = rowT.copy(fontSize = 20.sp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (dot != null) Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
                Text(value, style = small)
            }
        }
        Text("›", style = rowT.copy(color = Dim, fontSize = 26.sp))
    }
}

@Composable
private fun WorldPicker(onPick: (WorldId) -> Unit) {
    val ctx = LocalContext.current
    val current = Settings.world(ctx)
    Text("Choose a mode", style = head.copy(fontSize = 30.sp))
    Text("Modes change how Chime looks and feels. Your list stays the same.", style = small)
    Category.entries.forEach { cat ->
        Spacer(Modifier.height(10.dp))
        Text(cat.title.uppercase(), style = eyebrow)
        WorldId.entries.filter { it.category == cat }.forEach { w ->
            val on = w.name == current
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) Paper else Color(0xFF1C1E26))
                .clickable { onPick(w) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(width = 6.dp, height = 34.dp).clip(RoundedCornerShape(3.dp)).background(worldSwatch(w)))
                Column(Modifier.weight(1f)) {
                    Text(w.title, style = rowT.copy(color = if (on) Ink else Paper))
                    Text(w.blurb, style = small.copy(color = if (on) Color(0xFF55575F) else Dim))
                }
                if (on) Text("✓", style = rowT.copy(color = Ink))
            }
        }
    }
}

/** A color strip per world so the list reads at a glance. */
private fun worldSwatch(w: WorldId): Brush = Brush.verticalGradient(
    when (w) {
        WorldId.SIMPLE -> listOf(Color(0xFF0F0F12), Color(0xFF7CC35A))
        WorldId.PLAIN -> listOf(Color(0xFFF4F2ED), Color(0xFFC2452D))
        WorldId.MOM -> listOf(Color(0xFF1F6F6A), Color(0xFFECEBE6))
        WorldId.BUDDY -> listOf(Color(0xFF0A246A), Color(0xFFECE9D8))
        WorldId.GROUP -> listOf(Color(0xFFB07AF0), Color(0xFF17181C))
        WorldId.FEED -> listOf(Color(0xFFE2533A), Color(0xFFEEF0F3))
        WorldId.EMAIL -> listOf(Color(0xFF1D2229), Color(0xFF2F9E5B))
        WorldId.VOICEMAIL -> listOf(Color(0xFFF05A3C), Color(0xFF111315))
        WorldId.FLIP -> listOf(Color(0xFFC6CFDC), Color(0xFF1C3A6A))
        WorldId.ASSISTANT -> listOf(Color(0xFF2A3A30), Color(0xFFE9E1CF))
        WorldId.WHEEL -> listOf(Color(0xFFF6F6F3), Color(0xFF2A6AD8))
        WorldId.TOY -> listOf(Color(0xFFFF9A3A), Color(0xFFF7E6F2))
        WorldId.HANDHELD -> listOf(Color(0xFF9BBC0F), Color(0xFF0F380F))
        WorldId.DEX -> listOf(Color(0xFF3B4A3A), Color(0xFFF2C84A))
        WorldId.ADVENTURE -> listOf(Color(0xFF5AA04A), Color(0xFF10183A))
        WorldId.QUESTBOARD -> listOf(Color(0xFFB8875A), Color(0xFF7A1A0C))
        WorldId.MINES -> listOf(Color(0xFFC0C0C0), Color(0xFF008080))
        WorldId.OVERWORLD -> listOf(Color(0xFF4A78C8), Color(0xFF5A9A3A))
        WorldId.PET -> listOf(Color(0xFFFFE98A), Color(0xFF3A6AE0))
        WorldId.DESKTOP98 -> listOf(Color(0xFF2F7D7A), Color(0xFFC3C3C3))
        WorldId.PLATINUM -> listOf(Color(0xFF5A6AA0), Color(0xFFDEDEDE))
        WorldId.ONEBIT -> listOf(Color(0xFFFFFFFF), Color(0xFF000000))
        WorldId.SCREENSAVER -> listOf(Color(0xFF6FE0FF), Color(0xFF000000))
        WorldId.PROFILE -> listOf(Color(0xFFFF6AD5), Color(0xFF0A0010))
        WorldId.TERMINAL -> listOf(Color(0xFFFFB000), Color(0xFF0D0904))
        WorldId.PIGEON -> listOf(Color(0xFF7A4F2E), Color(0xFFEFE3C4))
        WorldId.LETTER -> listOf(Color(0xFFFBF6EA), Color(0xFF1E2A5A))
        WorldId.BOTTLE -> listOf(Color(0xFFF6C99A), Color(0xFF2F7A9A))
        WorldId.AIRPLANE -> listOf(Color(0xFFF9F7EF), Color(0xFF2A4AA8))
        WorldId.RAINFOREST -> listOf(Color(0xFF6AA06A), Color(0xFF0E2A1C))
        WorldId.NIGHT -> listOf(Color(0xFF121A3A), Color(0xFFF05A3C))
    }
)

@Composable
private fun CompanionPicker(focus: Quest?, onPick: (Companion) -> Unit) {
    val ctx = LocalContext.current
    var sel by remember { mutableStateOf(Settings.companion(ctx)) }
    val sample = focus ?: Quest("sample", "Respond to jury duty letter", System.currentTimeMillis() + 2 * 3600_000L, false, "by")
    Text("Choose a personality", style = head.copy(fontSize = 30.sp))
    Text("Same reminders, five ways of saying them.", style = small)
    Spacer(Modifier.height(6.dp))
    Companion.entries.forEach { c ->
        val on = c == sel
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) Color(c.color).copy(alpha = .16f) else Color(0xFF1C1E26))
            .border(if (on) 2.dp else 0.dp, if (on) Color(c.color) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable { sel = c }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(c.color)))
                Text(c.title, style = rowT.copy(fontSize = 22.sp, fontFamily = Fonts.fraunces))
                Text(c.trait.uppercase(), style = eyebrow.copy(color = Color(c.light).copy(alpha = .8f)))
            }
            Text(c.line(sample), style = rowT.copy(fontWeight = FontWeight.W600, fontSize = 16.sp, lineHeight = 22.sp, color = Paper.copy(alpha = if (on) 1f else .75f)))
        }
    }
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(sel.color)).clickable { onPick(sel) }.padding(16.dp), contentAlignment = Alignment.Center) {
        Text("Travel with ${sel.title}", style = rowT.copy(color = Ink))
    }
}

/** Pick the companion's look (six bodies) and finish (ten materials). Saves as you tap. */
@Composable
private fun LookPicker(onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val who = Settings.companion(ctx)
    var look by remember { mutableStateOf(Settings.look(ctx)) }
    var finish by remember { mutableStateOf(Settings.finish(ctx)) }
    fun pickLook(l: Look) { Settings.setLook(ctx, l); look = l; finish = l.defaultFinish; onChanged() }
    fun pickFinish(f: Finish) { Settings.setFinish(ctx, f); finish = f; onChanged() }

    Text("Choose a look", style = head.copy(fontSize = 30.sp))
    Text("How ${who.title} shows up in Simple mode. The personality still sets the voice and the face.", style = small)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Character(look, finish, who, 0f, false, 220.dp, draggable = true) { com.mwooten.chime.worlds.Bell.strike(ctx, who) }
    }
    Text("${look.title} · ${finish.title}", style = rowT.copy(fontFamily = Fonts.fraunces, fontSize = 22.sp), modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center)

    Spacer(Modifier.height(6.dp))
    Text("LOOK", style = eyebrow)
    Look.entries.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { l ->
                val on = l == look
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1C1E26))
                    .border(2.dp, if (on) Paper else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable { pickLook(l) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Character(l, if (on) finish else l.defaultFinish, who, 0f, false, 96.dp, draggable = false) { pickLook(l) }
                    Text(l.title, style = small.copy(color = if (on) Paper else Dim), maxLines = 1)
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Text("COLOR", style = eyebrow)
    Finish.entries.chunked(5).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { f ->
                val on = f == finish
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { pickFinish(f) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FinishSwatch(f, on)
                    Text(f.title, style = small.copy(fontSize = 12.sp, lineHeight = 14.sp, color = if (on) Paper else Dim),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
    Text(finish.about, style = small.copy(color = Paper.copy(alpha = .8f), lineHeight = 20.sp))
}

/** A round chip of a finish: highlight, body and shadow in one gradient. */
@Composable
internal fun FinishSwatch(f: Finish, on: Boolean, ring: Color = Paper) {
    Box(Modifier.size(50.dp).border(2.dp, if (on) ring else Color.Transparent, CircleShape).padding(3.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(f.light), Color(f.c), Color(f.dark)))))
    }
}

@Composable
private fun SmartReplies(ctx: Context, toast: (String) -> Unit, done: () -> Unit) {
    var key by remember { mutableStateOf(Settings.apiKey(ctx)) }
    Text("Smart replies", style = head.copy(fontSize = 30.sp))
    Text("Talk back in your own words, in any spill box or straight from a notification: “thanks, don't worry about this today”, “did it”, “push the dentist to Thursday and add milk”. Your companion understands, makes the change, and answers in character.", style = small)
    Spacer(Modifier.height(4.dp))
    Text("Without a key, Chime still understands the basics (done, later, not today, thanks). With a key it understands anything.", style = small)
    Spacer(Modifier.height(4.dp))
    Text("1. Go to console.anthropic.com → API Keys → Create Key.", style = rowT.copy(fontSize = 15.sp, fontWeight = FontWeight.W600))
    Text("2. Paste it below. It stays on this phone. Each reply costs a fraction of a cent.", style = rowT.copy(fontSize = 15.sp, fontWeight = FontWeight.W600))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1E26)).padding(14.dp)) {
        if (key.isEmpty()) Text("sk-ant-…", style = small)
        androidx.compose.foundation.text.BasicTextField(key, { key = it.trim() }, singleLine = true,
            textStyle = small.copy(color = Paper, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Paper), modifier = Modifier.fillMaxWidth())
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Paper).clickable {
            Settings.setApiKey(ctx, key); toast(if (key.isBlank()) "Smart replies off." else "Smart replies on."); done()
        }.padding(14.dp), contentAlignment = Alignment.Center) { Text("Save", style = rowT.copy(color = Ink)) }
        if (Settings.apiKey(ctx).isNotBlank()) Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1E26)).clickable {
            Settings.setApiKey(ctx, ""); key = ""; toast("Smart replies off."); done()
        }.padding(14.dp), contentAlignment = Alignment.Center) { Text("Remove key", style = rowT) }
    }
}

@Composable
private fun Reminders(ctx: Context, toast: (String) -> Unit, refresh: () -> Unit) {
    Text("Reminders", style = head.copy(fontSize = 30.sp))
    Text("Tap a row to change it.", style = small)
    Spacer(Modifier.height(6.dp))
    TopRow("Check-ins", Settings.chatter(ctx) + " · between 9am and 9pm") {
        val o = Settings.chatterOptions.map { it.first }
        Settings.setChatter(ctx, o[(o.indexOf(Settings.chatter(ctx)) + 1) % o.size]); Scheduler.scheduleGentle(ctx); refresh()
    }
    TopRow("When something's due", if (Settings.rings(ctx)) "Ring like a phone call" else "A gentle chime") {
        Settings.setRings(ctx, !Settings.rings(ctx)); refresh()
    }
    TopRow("Ring early", Settings.lead(ctx).let { if (it == 0) "Right on time" else "$it minutes before" }) {
        val o = Settings.leadOptions
        Settings.setLead(ctx, o[(o.indexOf(Settings.lead(ctx)) + 1) % o.size]); Scheduler.rescheduleAll(ctx); refresh()
    }
    TopRow("Test a call", "Lock your phone; it rings in a few seconds") {
        Scheduler.testCall(ctx, 6); toast("Lock your phone. ${Settings.companion(ctx).title} will call in a few seconds.")
    }
    TopRow("Clear finished quests", "${QuestStore.all(ctx).count { it.done }} finished") {
        val n = QuestStore.clearDone(ctx); toast(if (n > 0) "Cleared $n finished quest${if (n > 1) "s" else ""}." else "Nothing finished to clear yet."); refresh()
    }
}

private fun allGranted(ctx: Context): Boolean =
    NotificationManagerCompat.from(ctx).areNotificationsEnabled() && Notifier.canRingFullScreen(ctx) && Scheduler.canExact(ctx) &&
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

@Composable
private fun Permissions(ctx: Context, requestNotifications: () -> Unit) {
    val pkg = Uri.parse("package:" + ctx.packageName)
    fun open(i: Intent) = try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {
        ctx.startActivity(Intent(SysSettings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    val notif = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    val full = Notifier.canRingFullScreen(ctx)
    val exact = Scheduler.canExact(ctx)
    val battery = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    val ok = Color(0xFF9FD6A0); val bad = Color(0xFFE58A5F)
    Text("Permissions", style = head.copy(fontSize = 30.sp))
    Text("Your companion needs all four to reach you reliably. Tap any that are off.", style = small)
    Spacer(Modifier.height(6.dp))
    TopRow("Notifications", if (notif) "Allowed" else "Off", if (notif) ok else bad) {
        if (!notif) { if (Build.VERSION.SDK_INT >= 33) requestNotifications() else open(Intent(SysSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(SysSettings.EXTRA_APP_PACKAGE, ctx.packageName)) }
    }
    TopRow("Full-screen calls", if (full) "Allowed" else "Off", if (full) ok else bad) {
        if (!full && Build.VERSION.SDK_INT >= 34) open(Intent(SysSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
    }
    TopRow("Exact timing", if (exact) "On" else "Off", if (exact) ok else bad) {
        if (!exact && Build.VERSION.SDK_INT >= 31) open(Intent(SysSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
    }
    TopRow("Battery", if (battery) "Unrestricted" else "Optimized", if (battery) ok else bad) {
        if (!battery) open(Intent(SysSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
    }
}

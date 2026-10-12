package com.mwooten.chime.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as SysSettings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.mwooten.chime.Notifier
import com.mwooten.chime.Scheduler
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.QuestStore
import com.mwooten.chime.data.Settings
import com.mwooten.chime.worlds.Bell
import com.mwooten.chime.worlds.Category
import com.mwooten.chime.worlds.Character
import com.mwooten.chime.worlds.Finish
import com.mwooten.chime.worlds.Look
import com.mwooten.chime.worlds.WorldId
import kotlinx.coroutines.delay

/**
 * Options, dressed like the opening screen: your companion at the top of every page, a short
 * top level, and everything else one level down. Pages slide in from the right and back out
 * to the left; rows rise in one after another.
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

    Box(Modifier.fillMaxSize().background(Stage.bg)) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                // deeper pages come in from the right; going back to the top comes in from the left
                val dir = if (targetState == "top") -1 else 1
                (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 4 * dir } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 4 * dir } + fadeOut(tween(180)))
            },
            label = "opt",
        ) { p ->
            val backLabel = if (p == "top" || p == startPage) "Back" else "Settings"
            when (p) {
                "world" -> WorldPicker(backLabel, ::back) { Settings.setWorld(ctx, it.name); onChanged(); toast("Switched to ${it.title} mode."); onClose() }
                "companion" -> CompanionPicker(focus, backLabel, ::back) { Settings.setCompanion(ctx, it); onChanged(); toast("${it.title} is with you now."); onClose() }
                "look" -> LookPicker(backLabel, ::back, onChanged)
                "reminders" -> Reminders(ctx, tick, backLabel, ::back, toast) { tick++ }
                "permissions" -> Permissions(ctx, tick, backLabel, ::back, requestNotifications)
                "brain" -> SmartReplies(ctx, backLabel, ::back, toast) { page = "top" }
                else -> Top(ctx, tick, backLabel, ::back, onOpenProjects) { page = it }
            }
        }
    }
}

@Composable
private fun Top(ctx: Context, tick: Int, backLabel: String, onBack: () -> Unit, onOpenProjects: () -> Unit, go: (String) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val refresh = tick
    val w = WorldId.byName(Settings.world(ctx)); val c = Settings.companion(ctx)
    val lk = Settings.look(ctx); val fin = Settings.finish(ctx)
    val smart = Settings.apiKey(ctx).isNotBlank()
    val gh = Settings.githubToken(ctx).isNotBlank()
    val chatter = Settings.chatter(ctx); val rings = Settings.rings(ctx)
    val granted = allGranted(ctx)

    StagePage("Settings", onBack, subtitle = "${c.title} is with you. Tap anything to change it.", backLabel = backLabel, characterSize = 132.dp) {
        item(key = "look") { Rise(1) { StageRow("Look", "${lk.title} · ${fin.title}", Color(fin.c)) { go("look") } } }
        item(key = "personality") { Rise(2) { StageRow("Personality", "${c.title} · ${c.trait.lowercase()}", Color(c.color)) { go("companion") } } }
        item(key = "mode") { Rise(3) { StageRow("Mode", w.title) { go("world") } } }
        item(key = "brain") { Rise(4) { StageRow("Smart replies", if (smart) "On · powered by Claude" else "Basic · add an API key to turn on", if (smart) Stage.ok else null) { go("brain") } } }
        item(key = "projects") { Rise(5) { StageRow("Projects", if (gh) "Tracking your GitHub repos" else "Connect GitHub to track Claude's work") { onOpenProjects() } } }
        item(key = "reminders") { Rise(6) { StageRow("Reminders", "$chatter check-ins · ${if (rings) "rings" else "chimes"}") { go("reminders") } } }
        item(key = "permissions") { Rise(7) { StageRow("Permissions", if (granted) "All set" else "Needs attention", if (granted) Stage.ok else Stage.bad) { go("permissions") } } }
    }
}

/* ---------------- Mode ---------------- */

@Composable
private fun WorldPicker(backLabel: String, onBack: () -> Unit, onPick: (WorldId) -> Unit) {
    val ctx = LocalContext.current
    val current = Settings.world(ctx)
    val who = Settings.companion(ctx)
    val accent = Color(Settings.finish(ctx).c)
    StagePage("Choose a mode", onBack, subtitle = "Modes change how Chime looks and feels. Your list stays the same.",
        backLabel = backLabel, characterSize = 110.dp) {
        var i = 1
        Category.entries.forEach { cat ->
            val worlds = WorldId.entries.filter { it.category == cat }
            if (worlds.isEmpty()) return@forEach
            val hi = i++
            item(key = "cat_${cat.name}") { Rise(hi) { StageLabel(cat.title) } }
            worlds.forEach { w ->
                val idx = i++
                val on = w.name == current
                item(key = "w_${w.name}") {
                    Rise(idx) {
                        StageRow(w.title, w.blurb, selected = on, chevron = false,
                            leading = { WorldSwatch(w) },
                            trailing = { if (on) Text("✓", style = Stage.ui.copy(color = accent, fontWeight = FontWeight.W600, fontSize = 18.sp)) },
                        ) { Bell.strike(ctx, who); onPick(w) }
                    }
                }
            }
        }
    }
}

/** Two colors per world, split in a little coin, so the list reads at a glance. */
@Composable
private fun WorldSwatch(w: WorldId) {
    val (a, b) = worldColors(w)
    Canvas(Modifier.size(30.dp)) {
        drawArc(a, 180f, 180f, useCenter = true)
        drawArc(b, 0f, 180f, useCenter = true)
    }
}

private fun worldColors(w: WorldId): Pair<Color, Color> = when (w) {
    WorldId.SIMPLE -> Color(0xFF0F0F12) to Color(0xFF7CC35A)
    WorldId.PLAIN -> Color(0xFFF4F2ED) to Color(0xFFC2452D)
    WorldId.MOM -> Color(0xFF1F6F6A) to Color(0xFFECEBE6)
    WorldId.BUDDY -> Color(0xFF0A246A) to Color(0xFFECE9D8)
    WorldId.GROUP -> Color(0xFFB07AF0) to Color(0xFF17181C)
    WorldId.FEED -> Color(0xFFE2533A) to Color(0xFFEEF0F3)
    WorldId.EMAIL -> Color(0xFF1D2229) to Color(0xFF2F9E5B)
    WorldId.VOICEMAIL -> Color(0xFFF05A3C) to Color(0xFF111315)
    WorldId.FLIP -> Color(0xFFC6CFDC) to Color(0xFF1C3A6A)
    WorldId.ASSISTANT -> Color(0xFF2A3A30) to Color(0xFFE9E1CF)
    WorldId.WHEEL -> Color(0xFFF6F6F3) to Color(0xFF2A6AD8)
    WorldId.TOY -> Color(0xFFFF9A3A) to Color(0xFFF7E6F2)
    WorldId.HANDHELD -> Color(0xFF9BBC0F) to Color(0xFF0F380F)
    WorldId.DEX -> Color(0xFF3B4A3A) to Color(0xFFF2C84A)
    WorldId.ADVENTURE -> Color(0xFF5AA04A) to Color(0xFF10183A)
    WorldId.MINES -> Color(0xFFC0C0C0) to Color(0xFF008080)
    WorldId.OVERWORLD -> Color(0xFF4A78C8) to Color(0xFF5A9A3A)
    WorldId.DESKTOP98 -> Color(0xFF2F7D7A) to Color(0xFFC3C3C3)
    WorldId.PLATINUM -> Color(0xFF5A6AA0) to Color(0xFFDEDEDE)
    WorldId.PROFILE -> Color(0xFFFF6AD5) to Color(0xFF0A0010)
    WorldId.BOTTLE -> Color(0xFFF6C99A) to Color(0xFF2F7A9A)
    WorldId.NIGHT -> Color(0xFF121A3A) to Color(0xFFF05A3C)
}

/* ---------------- Personality ---------------- */

@Composable
private fun CompanionPicker(focus: Quest?, backLabel: String, onBack: () -> Unit, onPick: (Companion) -> Unit) {
    val ctx = LocalContext.current
    var sel by remember { mutableStateOf(Settings.companion(ctx)) }
    val sample = focus ?: Quest("sample", "Respond to jury duty letter", System.currentTimeMillis() + 2 * 3600_000L, false, "by")
    val accent = Color(Settings.finish(ctx).c)
    // the companion up top is whoever you're previewing, so its face and motion change as you tap
    StagePage("Choose a personality", onBack, subtitle = "Same reminders, five ways of saying them.",
        backLabel = backLabel, who = sel, characterSize = 150.dp,
        belowTitle = {
            Text("${sel.title.uppercase()} · ${sel.trait.uppercase()}", style = Stage.eyebrow.copy(color = Color(sel.light).copy(alpha = .85f)),
                modifier = Modifier.padding(top = 8.dp))
        }) {
        Companion.entries.forEachIndexed { i, c ->
            item(key = "c_${c.name}") {
                val on = c == sel
                val lineAlpha by animateFloatAsState(if (on) 1f else .62f, tween(260), label = "lineAlpha")
                Rise(i + 1) {
                    StageCard(selected = on, spacing = 8.dp, onClick = { sel = c; Bell.strike(ctx, c) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(c.color)))
                            Text(c.title, style = Stage.head.copy(fontSize = 22.sp, lineHeight = 26.sp))
                            Text(c.trait.uppercase(), style = Stage.eyebrow)
                        }
                        Text(c.line(sample), style = Stage.ui.copy(fontSize = 15.sp, lineHeight = 21.sp, color = Stage.ink.copy(alpha = lineAlpha)))
                    }
                }
            }
        }
        item(key = "go") {
            Rise(Companion.entries.size + 1) {
                StageButton("Travel with ${sel.title}", Modifier.fillMaxWidth().padding(top = 6.dp), fill = accent) { onPick(sel) }
            }
        }
    }
}

/* ---------------- Look ---------------- */

/** Pick the companion's look (six bodies) and finish (ten materials). Saves as you tap. */
@Composable
private fun LookPicker(backLabel: String, onBack: () -> Unit, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val who = Settings.companion(ctx)
    var look by remember { mutableStateOf(Settings.look(ctx)) }
    var finish by remember { mutableStateOf(Settings.finish(ctx)) }
    fun pickLook(l: Look) { Settings.setLook(ctx, l); look = l; finish = l.defaultFinish; onChanged(); Bell.strike(ctx, who) }
    fun pickFinish(f: Finish) { Settings.setFinish(ctx, f); finish = f; onChanged(); Bell.strike(ctx, who) }

    StagePage("Choose a look", onBack, subtitle = "${look.title} in ${finish.title}. How ${who.title} shows up in Simple mode; the personality still sets the voice and the face.",
        backLabel = backLabel, look = look, finish = finish, characterSize = 170.dp) {
        item(key = "lbl_look") { Rise(1) { StageLabel("Look") } }
        Look.entries.chunked(3).forEachIndexed { r, row ->
            item(key = "looks_$r") {
                Rise(2 + r) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { l ->
                            val on = l == look
                            val nameColor by animateColorAsState(if (on) Stage.ink else Stage.mute, tween(240), label = "lookName")
                            StageCard(Modifier.weight(1f), selected = on, padding = PaddingValues(vertical = 8.dp),
                                spacing = 0.dp, horizontalAlignment = Alignment.CenterHorizontally, onClick = { pickLook(l) }) {
                                Character(l, if (on) finish else l.defaultFinish, who, 0f, false, 88.dp, draggable = false) { pickLook(l) }
                                Text(l.title, style = Stage.ui.copy(fontSize = 13.sp, fontWeight = FontWeight.W600, color = nameColor), maxLines = 1)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item(key = "lbl_color") { Rise(4) { StageLabel("Color") } }
        Finish.entries.chunked(5).forEachIndexed { r, row ->
            item(key = "fin_$r") {
                Rise(5 + r) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { f ->
                            val on = f == finish
                            val nameColor by animateColorAsState(if (on) Stage.ink else Stage.mute, tween(240), label = "finName")
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { pickFinish(f) }.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FinishSwatch(f, on, Stage.ink)
                                Text(f.title, style = Stage.ui.copy(fontSize = 12.sp, lineHeight = 14.sp, color = nameColor),
                                    textAlign = TextAlign.Center, maxLines = 2)
                                if (f == look.defaultFinish) Text("SUGGESTED", style = Stage.eyebrow.copy(fontSize = 9.sp, letterSpacing = 1.sp, color = Color(finish.c)))
                            }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item(key = "about") {
            Rise(7) {
                Text(finish.about, style = Stage.body.copy(color = Stage.ink.copy(alpha = .8f)), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 8.dp, end = 8.dp))
            }
        }
    }
}

/** A round chip of a finish: highlight, body and shadow in one gradient. The ring eases in when picked. */
@Composable
internal fun FinishSwatch(f: Finish, on: Boolean, ring: Color = Stage.ink) {
    val ringColor by animateColorAsState(if (on) ring else ring.copy(alpha = 0f), tween(240), label = "ring")
    val inset by animateDpAsState(if (on) 4.dp else 0.dp, tween(260, easing = FastOutSlowInEasing), label = "inset")
    Box(Modifier.size(50.dp).border(2.dp, ringColor, CircleShape).padding(3.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(44.dp).padding(inset).clip(CircleShape).background(Brush.linearGradient(listOf(Color(f.light), Color(f.c), Color(f.dark)))))
    }
}

/* ---------------- Smart replies ---------------- */

@Composable
private fun SmartReplies(ctx: Context, backLabel: String, onBack: () -> Unit, toast: (String) -> Unit, done: () -> Unit) {
    var key by remember { mutableStateOf(Settings.apiKey(ctx)) }
    val who = Settings.companion(ctx)
    val accent = Color(Settings.finish(ctx).c)
    val hasKey = Settings.apiKey(ctx).isNotBlank()
    StagePage("Smart replies", onBack, subtitle = if (hasKey) "On. ${who.title} understands anything you say." else "Talk back in your own words.",
        backLabel = backLabel, characterSize = 110.dp) {
        item(key = "about") {
            Rise(1) {
                StageCard(spacing = 10.dp) {
                    Text("Reply in any spill box or straight from a notification: “thanks, don't worry about this today”, “did it”, “push the dentist to Thursday and add milk”. ${who.title} understands, makes the change, and answers in character.",
                        style = Stage.ui.copy(fontSize = 15.sp, lineHeight = 21.sp))
                    Text("Without a key, Chime still understands the basics (done, later, not today, thanks). With a key it understands anything.", style = Stage.body)
                }
            }
        }
        item(key = "lbl") { Rise(2) { StageLabel("Set up") } }
        item(key = "s1") { Rise(3) { Step("1", "Go to console.anthropic.com → API Keys → Create Key.", accent) } }
        item(key = "s2") { Rise(4) { Step("2", "Paste it below. It stays on this phone. Each reply costs a fraction of a cent.", accent) } }
        item(key = "field") { Rise(5) { StageField(key, { key = it.trim() }, "sk-ant-…", mono = true, cursor = accent) } }
        item(key = "buttons") {
            Rise(6) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StageButton("Save", Modifier.weight(1f), fill = accent) {
                        Settings.setApiKey(ctx, key)
                        if (key.isNotBlank()) Bell.strike(ctx, who)
                        toast(if (key.isBlank()) "Smart replies off." else "Smart replies on."); done()
                    }
                    if (hasKey) StageButton("Remove key", Modifier.weight(1f)) {
                        Settings.setApiKey(ctx, ""); key = ""; toast("Smart replies off."); done()
                    }
                }
            }
        }
    }
}

@Composable
private fun Step(n: String, text: String, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Text(n, style = Stage.ui.copy(color = Stage.bg, fontWeight = FontWeight.W600, fontSize = 13.sp))
        }
        Text(text, style = Stage.ui.copy(fontSize = 15.sp, lineHeight = 21.sp), modifier = Modifier.weight(1f).padding(top = 2.dp))
    }
}

/* ---------------- Reminders ---------------- */

@Composable
private fun Reminders(ctx: Context, tick: Int, backLabel: String, onBack: () -> Unit, toast: (String) -> Unit, refresh: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val r = tick
    val who = Settings.companion(ctx)
    val chatter = Settings.chatter(ctx)
    val rings = Settings.rings(ctx)
    val lead = Settings.lead(ctx)
    val finished = QuestStore.all(ctx).count { it.done }
    StagePage("Reminders", onBack, subtitle = "How and when ${who.title} reaches you. Tap a row to change it.",
        backLabel = backLabel, characterSize = 110.dp) {
        item(key = "chatter") {
            Rise(1) {
                StageRow("Check-ins", "$chatter · between 9am and 9pm", chevron = false, trailing = { CycleHint() }) {
                    val o = Settings.chatterOptions.map { it.first }
                    Settings.setChatter(ctx, o[(o.indexOf(Settings.chatter(ctx)) + 1) % o.size]); Scheduler.scheduleGentle(ctx)
                    Bell.strike(ctx, who); refresh()
                }
            }
        }
        item(key = "rings") {
            Rise(2) {
                StageRow("When something's due", if (rings) "Ring like a phone call" else "A gentle chime", chevron = false,
                    trailing = { Toggle(rings) }) {
                    Settings.setRings(ctx, !Settings.rings(ctx)); Bell.strike(ctx, who); refresh()
                }
            }
        }
        item(key = "lead") {
            Rise(3) {
                StageRow("Ring early", if (lead == 0) "Right on time" else "$lead minutes before", chevron = false, trailing = { CycleHint() }) {
                    val o = Settings.leadOptions
                    Settings.setLead(ctx, o[(o.indexOf(Settings.lead(ctx)) + 1) % o.size]); Scheduler.rescheduleAll(ctx)
                    Bell.strike(ctx, who); refresh()
                }
            }
        }
        item(key = "lbl") { Rise(4) { StageLabel("Try it") } }
        item(key = "test") {
            Rise(5) {
                StageRow("Test a call", "Lock your phone; it rings in a few seconds", chevron = false) {
                    Scheduler.testCall(ctx, 6); toast("Lock your phone. ${Settings.companion(ctx).title} will call in a few seconds.")
                }
            }
        }
        item(key = "clear") {
            Rise(6) {
                StageRow("Clear finished quests", "$finished finished", chevron = false) {
                    val n = QuestStore.clearDone(ctx); toast(if (n > 0) "Cleared $n finished quest${if (n > 1) "s" else ""}." else "Nothing finished to clear yet."); refresh()
                }
            }
        }
    }
}

/** A small on/off switch whose knob glides across. */
@Composable
private fun Toggle(on: Boolean) {
    val ctx = LocalContext.current
    val accent = Color(Settings.finish(ctx).c)
    val track by animateColorAsState(if (on) accent else Stage.line, tween(240), label = "track")
    val x by animateDpAsState(if (on) 18.dp else 0.dp, tween(260, easing = FastOutSlowInEasing), label = "knob")
    Box(Modifier.size(width = 44.dp, height = 26.dp).clip(CircleShape).background(track).padding(3.dp)) {
        Box(Modifier.padding(start = x).size(20.dp).clip(CircleShape).background(if (on) Stage.bg else Stage.ink))
    }
}

/** Rows that cycle through options show a little "tap to change" mark instead of a chevron. */
@Composable
private fun CycleHint() {
    Text("↻", style = Stage.ui.copy(color = Stage.mute, fontSize = 18.sp))
}

/* ---------------- Permissions ---------------- */

private fun allGranted(ctx: Context): Boolean =
    NotificationManagerCompat.from(ctx).areNotificationsEnabled() && Notifier.canRingFullScreen(ctx) && Scheduler.canExact(ctx) &&
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

@Composable
private fun Permissions(ctx: Context, tick: Int, backLabel: String, onBack: () -> Unit, requestNotifications: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val r = tick
    val pkg = Uri.parse("package:" + ctx.packageName)
    fun open(i: Intent) = try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {
        ctx.startActivity(Intent(SysSettings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    val notif = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    val full = Notifier.canRingFullScreen(ctx)
    val exact = Scheduler.canExact(ctx)
    val battery = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    val n = listOf(notif, full, exact, battery).count { it }
    StagePage("Permissions", onBack,
        subtitle = if (n == 4) "All four are on. ${Settings.companion(ctx).title} can reach you." else "Your companion needs all four to reach you reliably. Tap any that are off.",
        backLabel = backLabel, characterSize = 110.dp,
        belowTitle = { PermissionMeter(n) }) {
        item(key = "notif") {
            Rise(1) {
                StageRow("Notifications", if (notif) "Allowed" else "Off", if (notif) Stage.ok else Stage.bad, chevron = !notif) {
                    if (!notif) { if (Build.VERSION.SDK_INT >= 33) requestNotifications() else open(Intent(SysSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(SysSettings.EXTRA_APP_PACKAGE, ctx.packageName)) }
                }
            }
        }
        item(key = "full") {
            Rise(2) {
                StageRow("Full-screen calls", if (full) "Allowed" else "Off", if (full) Stage.ok else Stage.bad, chevron = !full) {
                    if (!full && Build.VERSION.SDK_INT >= 34) open(Intent(SysSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
                }
            }
        }
        item(key = "exact") {
            Rise(3) {
                StageRow("Exact timing", if (exact) "On" else "Off", if (exact) Stage.ok else Stage.bad, chevron = !exact) {
                    if (!exact && Build.VERSION.SDK_INT >= 31) open(Intent(SysSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
                }
            }
        }
        item(key = "battery") {
            Rise(4) {
                StageRow("Battery", if (battery) "Unrestricted" else "Optimized", if (battery) Stage.ok else Stage.bad, chevron = !battery) {
                    if (!battery) open(Intent(SysSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
                }
            }
        }
    }
}

/** Four short bars under the headline, one per permission; they fill as you turn things on. */
@Composable
private fun PermissionMeter(n: Int) {
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until 4) {
            val on = i < n
            val c by animateColorAsState(if (on) Stage.ok else Stage.line, tween(300, delayMillis = i * 60), label = "meter")
            val w by animateDpAsState(if (on) 22.dp else 14.dp, tween(300, delayMillis = i * 60), label = "meterW")
            Box(Modifier.size(width = w, height = 5.dp).clip(CircleShape).background(c))
        }
    }
}

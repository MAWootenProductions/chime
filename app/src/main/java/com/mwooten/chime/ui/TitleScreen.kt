package com.mwooten.chime.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as SysSettings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.mwooten.chime.Notifier
import com.mwooten.chime.Scheduler
import com.mwooten.chime.data.QuestStore
import com.mwooten.chime.data.Settings
import kotlinx.coroutines.delay

private class MenuItem(val label: String, val value: String? = null, val tone: Color? = null, val act: () -> Unit)

@Composable
fun TitleScreen(
    pressed: Boolean,
    onPress: () -> Unit,
    openCount: Int,
    overdueCount: Int,
    onSpill: () -> Unit,
    onLog: () -> Unit,
    toast: (String) -> Unit,
    requestNotifications: () -> Unit,
) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf("main") }
    var sel by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) } // forces re-reading settings and permission states
    var armClear by remember { mutableStateOf(false) }

    LaunchedEffect(menu) { while (menu == "permissions") { delay(1000); tick++ } }
    LaunchedEffect(armClear) { if (armClear) { delay(3000); armClear = false } }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(72.dp))
        Text("Chime", style = T.logo.copy(fontSize = 76.sp, shadow = Shadow(Color(0xCC000000), Offset(0f, 6f), 24f)))
        Ornament()
        Text("Small quests, watched over by Mote", style = T.voice.copy(color = C.parchment.copy(alpha = .85f), fontSize = 20.sp,
            shadow = Shadow(Color.Black, Offset(0f, 2f), 12f)), textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))

        if (!pressed) {
            val a by rememberInfiniteTransition(label = "b").animateFloat(0.3f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "ba")
            Text("PRESS START", style = T.eyebrow.copy(color = C.parchment, fontSize = 15.sp, letterSpacing = 5.sp,
                shadow = Shadow(C.glow.copy(alpha = .6f), Offset.Zero, 18f)),
                modifier = Modifier.alpha(a).tap(onPress).padding(18.dp))
            Spacer(Modifier.height(120.dp))
        } else {
            val refresh = tick
            val items: List<MenuItem> = when (menu) {
                "options" -> listOf(
                    MenuItem("Mote's chatter", Settings.chatter(ctx)) {
                        val o = Settings.chatterOptions.map { it.first }
                        Settings.setChatter(ctx, o[(o.indexOf(Settings.chatter(ctx)) + 1) % o.size])
                        Scheduler.scheduleGentle(ctx); tick++
                    },
                    MenuItem("Due quests", if (Settings.rings(ctx)) "Ring as a call" else "Gentle chime") {
                        Settings.setRings(ctx, !Settings.rings(ctx)); tick++
                    },
                    MenuItem("Ring early", Settings.lead(ctx).let { if (it == 0) "On time" else "$it min before" }) {
                        val o = Settings.leadOptions
                        Settings.setLead(ctx, o[(o.indexOf(Settings.lead(ctx)) + 1) % o.size])
                        Scheduler.rescheduleAll(ctx); tick++
                    },
                    MenuItem("Test a call") {
                        Scheduler.testCall(ctx, 6)
                        toast("Lock your phone. Mote will call in a few seconds.")
                    },
                    MenuItem("Permissions") { menu = "permissions"; sel = 0 },
                    MenuItem("Clear finished", if (armClear) "Tap again" else null) {
                        if (!armClear) armClear = true
                        else { armClear = false; val n = QuestStore.clearDone(ctx); toast(if (n > 0) "Cleared $n finished quest${if (n > 1) "s" else ""}." else "Nothing finished to clear yet.") }
                    },
                    MenuItem("Back") { menu = "main"; sel = 2 },
                )
                "permissions" -> permissionItems(ctx, requestNotifications) + MenuItem("Back") { menu = "options"; sel = 4 }
                else -> listOf(
                    MenuItem("Spill") { onSpill() },
                    MenuItem("Quest Log") { onLog() },
                    MenuItem("Options") { menu = "options"; sel = 0 },
                )
            }
            AnimatedContent(menu, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "menu") { _ ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items.forEachIndexed { i, it ->
                        MenuRow(it, i == sel) { sel = i; it.act() }
                    }
                    Spacer(Modifier.height(14.dp))
                    val foot = when (menu) {
                        "main" -> if (openCount == 0) "Your satchel is empty" else "$openCount quest${if (openCount > 1) "s" else ""} waiting" + if (overdueCount > 0) " · $overdueCount overdue" else ""
                        "permissions" -> "Mote needs all four to ring you reliably"
                        else -> "Saved on this phone"
                    }
                    Text(foot, style = T.small.copy(color = if (menu == "main" && overdueCount > 0) C.ember else C.mist))
                }
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}

private fun permissionItems(ctx: Context, requestNotifications: () -> Unit): List<MenuItem> {
    val pkg = Uri.parse("package:" + ctx.packageName)
    fun open(i: Intent) = try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {
        ctx.startActivity(Intent(SysSettings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    val notif = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    val full = Notifier.canRingFullScreen(ctx)
    val exact = Scheduler.canExact(ctx)
    val battery = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    val ok = Color(0xFF9FD6A0); val bad = C.ember
    return listOf(
        MenuItem("Notifications", if (notif) "Allowed" else "Off", if (notif) ok else bad) {
            if (!notif) {
                if (Build.VERSION.SDK_INT >= 33) requestNotifications()
                else open(Intent(SysSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(SysSettings.EXTRA_APP_PACKAGE, ctx.packageName))
            }
        },
        MenuItem("Full-screen calls", if (full) "Allowed" else "Off", if (full) ok else bad) {
            if (!full && Build.VERSION.SDK_INT >= 34) open(Intent(SysSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
        },
        MenuItem("Exact timing", if (exact) "On" else "Off", if (exact) ok else bad) {
            if (!exact && Build.VERSION.SDK_INT >= 31) open(Intent(SysSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
        },
        MenuItem("Battery", if (battery) "Unrestricted" else "Optimized", if (battery) ok else bad) {
            if (!battery) open(Intent(SysSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
        },
    )
}

@Composable
private fun MenuRow(item: MenuItem, selected: Boolean, onClick: () -> Unit) {
    val clock = rememberClock()
    Row(Modifier.tap(onClick).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (selected) Canvas(Modifier.size(22.dp)) {
                val t = clock.value
                val r = size.width * (0.28f + 0.05f * kotlin.math.sin(t * 6f))
                drawCircle(Brush.radialGradient(listOf(C.glowCore, C.glow, Color(0x008FD47A)), center, r * 1.8f), r * 1.8f, center)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(item.label, style = T.menu.copy(
            color = if (selected) C.parchment else C.mist,
            fontSize = if (item.value != null) 21.sp else 26.sp,
            shadow = Shadow(Color.Black, Offset(0f, 3f), 14f)))
        if (item.value != null) {
            Spacer(Modifier.width(10.dp))
            Text(item.value, style = T.body.copy(color = item.tone ?: C.brass, fontSize = 15.sp))
        }
        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun Ornament() {
    Canvas(Modifier.width(260.dp).height(22.dp)) {
        val w = size.width; val h = size.height; val cy = h / 2
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, C.brassDim, C.brassDim, Color.Transparent)), Offset(0f, cy), Offset(w, cy), 1.dp.toPx())
        // three leaves around a seed of light
        for (dx in listOf(-14f, 14f)) {
            val p = Path().apply {
                moveTo(w / 2, cy); cubicTo(w / 2 + dx * 0.5f, cy - 8f, w / 2 + dx * 1.4f, cy - 6f, w / 2 + dx * 1.8f, cy)
                cubicTo(w / 2 + dx * 1.4f, cy + 6f, w / 2 + dx * 0.5f, cy + 8f, w / 2, cy); close()
            }
            drawPath(p, C.brass)
        }
        drawCircle(Brush.radialGradient(listOf(C.glowCore, C.glow.copy(alpha = .0f)), Offset(w / 2, cy), 10f), 10f, Offset(w / 2, cy))
    }
    Spacer(Modifier.fillMaxWidth().height(4.dp))
}

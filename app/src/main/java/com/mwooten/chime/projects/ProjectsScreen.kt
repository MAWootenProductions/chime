package com.mwooten.chime.projects

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.Scheduler
import com.mwooten.chime.data.Settings
import com.mwooten.chime.data.Time
import com.mwooten.chime.ui.Fonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF14151B)
private val Card = Color(0xFF1C1E26)
private val Paper = Color(0xFFF2EFE8)
private val Mute = Color(0xFF8A8C96)
private val Pass = Color(0xFF6CC48A)
private val Fail = Color(0xFFEF6A55)
private val Run = Color(0xFFE6B84A)

private val h1 = TextStyle(fontFamily = Fonts.fraunces, fontWeight = FontWeight.W800, fontSize = 34.sp, color = Paper)
private val b = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W600, fontSize = 15.sp, lineHeight = 21.sp, color = Paper)
private val t = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W800, fontSize = 18.sp, color = Paper)
private val sm = TextStyle(fontFamily = Fonts.nunito, fontWeight = FontWeight.W600, fontSize = 13.sp, color = Mute)
private val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Mute)

private fun ago(ms: Long): String {
    if (ms <= 0) return "—"
    val m = (System.currentTimeMillis() - ms) / 60_000
    return when {
        m < 1 -> "just now"
        m < 60 -> "${m}m ago"
        m < 60 * 24 -> "${m / 60}h ago"
        else -> "${m / 1440}d ago"
    }
}

/** Every repo a Claude session touched lately, newest activity first. */
@Composable
fun ProjectsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val projects by Tracker.projects.collectAsState()
    val status by Tracker.status.collectAsState()
    var hasToken by remember { mutableStateOf(Settings.githubToken(ctx).isNotBlank()) }
    var checking by remember { mutableStateOf(false) }
    var rev by remember { mutableIntStateOf(0) }
    BackHandler { onClose() }

    LaunchedEffect(hasToken) {
        Tracker.loadCached(ctx)
        while (hasToken) {
            checking = true
            Tracker.poll(ctx, notify = projects.isNotEmpty())
            checking = false
            delay(60_000)
        }
    }

    Column(Modifier.fillMaxSize().background(Ink).systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("‹ Back", style = sm.copy(color = Paper), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClose() }.padding(8.dp))
        Text("Projects", style = h1)
        Text("Your Claude sessions push their work to GitHub. When one adds commits, opens a pull request, or finishes a build, your companion lets you know.", style = sm)

        if (!hasToken) {
            TokenSetup { tok ->
                Settings.setGithubToken(ctx, tok); hasToken = true
                Scheduler.schedulePoll(ctx)
            }
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (checking) "Checking…" else status, style = sm, modifier = Modifier.weight(1f))
            Text("Check now", style = sm.copy(color = Paper, fontWeight = FontWeight.W800),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF3A3C46), RoundedCornerShape(10.dp))
                    .clickable { scope.launch { checking = true; Tracker.poll(ctx); checking = false } }.padding(horizontal = 12.dp, vertical = 8.dp))
        }

        if (projects.isEmpty()) {
            Text(if (checking) "Looking through your repos…" else "No repos with activity in the last 30 days yet.", style = b.copy(color = Mute))
        }
        @Suppress("UNUSED_VARIABLE") val r = rev
        projects.forEach { p ->
            val muted = p.repo in Settings.mutedRepos(ctx)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card)
                .clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.lastUrl ?: p.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dot = when (p.ci) { "passed" -> Pass; "failed" -> Fail; "running" -> Run; else -> Color(0xFF4A4C56) }
                    Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
                    Text(p.name, style = t.copy(color = if (muted) Mute else Paper), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(ago(maxOf(p.lastAt, p.pushedAt)), style = sm)
                }
                Text(p.lastText.ifBlank { "Pushed ${ago(p.pushedAt)}" }, style = b, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (p.ci != null) Text("build ${p.ci}" + (p.ciName?.let { " · $it" } ?: ""), style = mono, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                    Text(if (muted) "Unmute" else "Mute", style = sm.copy(color = Paper),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { Settings.setMuted(ctx, p.repo, !muted); rev++ }.padding(6.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Remove GitHub token", style = sm, modifier = Modifier.clickable { Settings.setGithubToken(ctx, ""); hasToken = false; Scheduler.schedulePoll(ctx) }.padding(6.dp))
    }
}

@Composable
private fun TokenSetup(onSave: (String) -> Unit) {
    var tok by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Connect GitHub (one time)", style = t)
        Text("1. On GitHub: Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token.", style = b)
        Text("2. Repository access: All repositories. Permissions (read-only): Contents, Metadata, Pull requests, Actions.", style = b)
        Text("3. Copy the token and paste it here. It stays on this phone.", style = b)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ink).padding(12.dp)) {
            if (tok.isEmpty()) Text("github_pat_…", style = mono.copy(fontSize = 14.sp))
            BasicTextField(tok, { tok = it.trim() }, singleLine = true, textStyle = mono.copy(fontSize = 14.sp, color = Paper), cursorBrush = SolidColor(Paper), modifier = Modifier.fillMaxWidth())
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (tok.length > 20) Paper else Color(0xFF3A3C46))
            .clickable(enabled = tok.length > 20) { onSave(tok) }.padding(14.dp), contentAlignment = Alignment.Center) {
            Text("Start tracking", style = t.copy(color = Ink))
        }
    }
}

package com.mwooten.chime.projects

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.Scheduler
import com.mwooten.chime.data.Settings
import com.mwooten.chime.ui.Rise
import com.mwooten.chime.ui.Stage
import com.mwooten.chime.ui.StageButton
import com.mwooten.chime.ui.StageCard
import com.mwooten.chime.ui.StageField
import com.mwooten.chime.ui.StageLabel
import com.mwooten.chime.ui.StagePage
import com.mwooten.chime.worlds.Bell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Pass = Color(0xFF6CC48A)
private val Fail = Color(0xFFEF6A55)
private val Run = Color(0xFFE6B84A)
private val mono = Stage.ui.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Stage.mute)

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
@OptIn(ExperimentalFoundationApi::class)
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

    val who = Settings.companion(ctx)
    val accent = Color(Settings.finish(ctx).c)
    @Suppress("UNUSED_VARIABLE") val r = rev
    val mutedSet = Settings.mutedRepos(ctx)

    StagePage(
        "Projects", onClose,
        subtitle = "Your Claude sessions push their work to GitHub. When one adds commits, opens a pull request, or finishes a build, ${who.title} lets you know.",
        characterSize = 110.dp,
    ) {
        if (!hasToken) {
            item(key = "setup") {
                Rise(1) {
                    TokenSetup(accent) { tok ->
                        Settings.setGithubToken(ctx, tok); hasToken = true
                        Scheduler.schedulePoll(ctx)
                        Bell.strike(ctx, who)
                    }
                }
            }
        } else {
            item(key = "status") {
                Rise(1) {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CheckingDot(checking, accent)
                        Text(if (checking) "Checking…" else status, style = Stage.body.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
                        StageButton("Check now", enabled = !checking) {
                            scope.launch { checking = true; Tracker.poll(ctx); checking = false }
                        }
                    }
                }
            }
            if (projects.isEmpty()) {
                item(key = "none") {
                    Rise(2) {
                        StageCard(padding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
                            Text(if (checking) "Looking through your repos…" else "No repos with activity in the last 30 days yet.",
                                style = Stage.head.copy(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.W400, color = Stage.ink.copy(alpha = .8f)))
                        }
                    }
                }
            } else {
                item(key = "lbl") { Rise(2) { StageLabel("Recent", trailing = "${projects.size}") } }
            }
            projects.forEachIndexed { i, p ->
                val muted = p.repo in mutedSet
                item(key = "p_${p.repo}") {
                    Rise(3 + i, Modifier.animateItem()) {
                        ProjectCard(p, muted,
                            onOpen = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.lastUrl ?: p.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                            onMute = { Settings.setMuted(ctx, p.repo, !muted); rev++ })
                    }
                }
            }
            item(key = "remove") {
                Rise(3 + projects.size) {
                    Text("Remove GitHub token", style = Stage.body.copy(fontSize = 13.sp),
                        modifier = Modifier.padding(top = 12.dp).clip(RoundedCornerShape(8.dp))
                            .clickable { Settings.setGithubToken(ctx, ""); hasToken = false; Scheduler.schedulePoll(ctx) }.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(p: Project, muted: Boolean, onOpen: () -> Unit, onMute: () -> Unit) {
    val fade by animateFloatAsState(if (muted) .55f else 1f, tween(280), label = "muted")
    val dot by animateColorAsState(when (p.ci) { "passed" -> Pass; "failed" -> Fail; "running" -> Run; else -> Stage.line }, tween(300), label = "ci")
    StageCard(Modifier.graphicsLayer { alpha = fade }, spacing = 6.dp, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
            Text(p.name, style = Stage.ui.copy(fontWeight = FontWeight.W600, fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(ago(maxOf(p.lastAt, p.pushedAt)), style = Stage.body.copy(fontSize = 13.sp))
        }
        Text(p.lastText.ifBlank { "Pushed ${ago(p.pushedAt)}" }, style = Stage.ui.copy(fontSize = 15.sp, lineHeight = 21.sp, color = Stage.ink.copy(alpha = .88f)),
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (p.ci != null) Text("build ${p.ci}" + (p.ciName?.let { " · $it" } ?: ""), style = mono, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            else Spacer(Modifier.weight(1f))
            Text(if (muted) "Unmute" else "Mute", style = Stage.ui.copy(fontSize = 13.sp, fontWeight = FontWeight.W600, color = Stage.ink),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onMute).padding(horizontal = 8.dp, vertical = 6.dp))
        }
    }
}

/** A small dot that breathes while the tracker is checking. */
@Composable
private fun CheckingDot(checking: Boolean, accent: Color) {
    val breathe by rememberInfiniteTransition(label = "check").animateFloat(.3f, 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "breathe")
    Box(Modifier.size(8.dp).graphicsLayer { alpha = if (checking) breathe else 1f }.clip(CircleShape).background(if (checking) accent else Stage.ok))
}

@Composable
private fun TokenSetup(accent: Color, onSave: (String) -> Unit) {
    var tok by remember { mutableStateOf("") }
    StageCard(spacing = 12.dp) {
        Text("Connect GitHub (one time)", style = Stage.head.copy(fontSize = 21.sp, lineHeight = 26.sp))
        Text("1. On GitHub: Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token.", style = Stage.ui.copy(lineHeight = 21.sp))
        Text("2. Repository access: All repositories. Permissions (read-only): Contents, Metadata, Pull requests, Actions.", style = Stage.ui.copy(lineHeight = 21.sp))
        Text("3. Copy the token and paste it here. It stays on this phone.", style = Stage.ui.copy(lineHeight = 21.sp))
        StageField(tok, { tok = it.trim() }, "github_pat_…", mono = true, cursor = accent, background = Stage.bg)
        StageButton("Start tracking", Modifier.fillMaxWidth(), fill = if (tok.length > 20) accent else Stage.line, enabled = tok.length > 20) { onSave(tok) }
    }
}

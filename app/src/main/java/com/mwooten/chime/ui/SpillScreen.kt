package com.mwooten.chime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Parser
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Time

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpillScreen(
    recent: List<Quest>,
    onBack: () -> Unit,
    onLog: () -> Unit,
    onAdd: (Parser.Parsed, String) -> Unit,
    onToggle: (Quest) -> Unit,
    onOpen: (Quest) -> Unit,
    toast: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(350); try { focus.requestFocus() } catch (e: Exception) { } }
    val parsed = remember(text) { if (text.isBlank()) null else Parser.parse(text) }

    fun submit() {
        val p = parsed ?: return
        if (p.title.isBlank()) { toast("Add a few words about what needs doing."); return }
        onAdd(p, text); text = ""
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Crumb("Title", true, onBack); Crumb("Quest Log", false, onLog)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(48.dp))
            Text("THE SPILL BOARD", style = T.eyebrow)
            Text("What's on your mind?", style = T.heading)
            Row(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xD91B3328), Color(0xE6102019))))
                    .border(1.dp, if (text.isNotEmpty()) C.brass.copy(alpha = .5f) else C.line, RoundedCornerShape(16.dp))
                    .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    if (text.isEmpty()) Text("Respond to jury duty letter before noon", style = T.body.copy(color = C.faint, fontSize = 19.sp))
                    BasicTextField(
                        value = text, onValueChange = { text = it },
                        textStyle = T.body.copy(fontSize = 19.sp),
                        cursorBrush = SolidColor(C.glow),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                }
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(C.brassBrush)
                    .alpha(if (text.isBlank()) 0.4f else 1f).clickable(enabled = text.isNotBlank()) { submit() },
                    contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(20.dp)) {
                        val s = size.width; val sw = 2.4.dp.toPx()
                        drawLine(C.ink, Offset(s / 2, 0f), Offset(s / 2, s), sw, StrokeCap.Round)
                        drawLine(C.ink, Offset(0f, s / 2), Offset(s, s / 2), sw, StrokeCap.Round)
                    }
                }
            }
            if (parsed == null) {
                Text("Say it how you'd say it: today, before noon, by 3pm, tomorrow morning, friday, in 2 hours.", style = T.small.copy(color = C.faint))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(parsed.title.ifBlank { "…" })
                    Chip(Time.dueLabel(parsed.due, parsed.allDay, parsed.kind), if (parsed.due != null) C.brass else C.parchment,
                        if (parsed.due != null) C.brass.copy(alpha = .55f) else C.line)
                }
            }
            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                GroupHeader("Just spilled", null)
                recent.forEach { q -> QuestRow(q, { onToggle(q) }, { onOpen(q) }) }
            }
        }
    }
}

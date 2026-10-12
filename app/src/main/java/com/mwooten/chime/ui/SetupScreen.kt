package com.mwooten.chime.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Settings
import com.mwooten.chime.worlds.Bell
import com.mwooten.chime.worlds.Character
import com.mwooten.chime.worlds.Finish
import com.mwooten.chime.worlds.Look

private val UBg = Color(0xFF0F0F12)
private val UCard = Color(0xFF1A1A1F)
private val UInk = Color(0xFFF1EEE7)
private val UMute = Color(0xFF8C8A93)

private val uUi = TextStyle(fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W500, fontSize = 15.sp, color = UInk)
private val uHead = TextStyle(fontFamily = Fonts.newsreader, fontWeight = FontWeight.W600, fontSize = 30.sp, lineHeight = 36.sp, color = UInk)
private val uEyebrow = TextStyle(fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600, fontSize = 11.sp, letterSpacing = 2.sp, color = UMute)

/**
 * First run: pick who reminds you. Three steps (look, color, personality) under a big live
 * preview that changes as you choose. Start saves everything and opens the world.
 */
@Composable
fun SetupScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var look by rememberSaveable { mutableStateOf(Settings.look(ctx)) }
    var finish by rememberSaveable { mutableStateOf(Settings.finish(ctx)) }
    var who by rememberSaveable { mutableStateOf(Settings.companion(ctx)) }
    val sample = remember { Quest("sample", "Grocery store", System.currentTimeMillis() + 2 * 3600_000L, false, "at") }
    val accent = Color(finish.c)

    BackHandler(enabled = step > 0) { step-- }

    fun start() {
        Settings.setLook(ctx, look)
        Settings.setFinish(ctx, finish)
        Settings.setCompanion(ctx, who)
        Settings.setSetupDone(ctx, true)
        onDone()
    }

    Column(Modifier.fillMaxSize().background(UBg).systemBarsPadding().padding(horizontal = 20.dp)) {
        // step indicator
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..2) Box(Modifier.size(width = if (i == step) 18.dp else 6.dp, height = 6.dp).clip(CircleShape).background(if (i <= step) accent else Color(0xFF2E2E36)))
            Spacer(Modifier.weight(1f))
            Text("${step + 1} of 3", style = uEyebrow)
        }

        // the live preview: tap it to hear it ring
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Character(look, finish, who, 0f, false, 190.dp) { Bell.strike(ctx, who) }
        }
        Text(
            when (step) { 0 -> "Pick a look"; 1 -> "Pick a color"; else -> "Pick a personality" },
            style = uHead, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Text(
            when (step) {
                0 -> "Who's going to remind you? Pick a shape. You can change it any time."
                1 -> "${look.title} in ${finish.title}. ${finish.about}"
                else -> "Same reminders, five ways of saying them."
            },
            style = uUi.copy(color = UMute, fontSize = 14.sp, lineHeight = 19.sp), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
        )

        // the choices
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (step) {
                0 -> Look.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { l ->
                            val on = l == look
                            val pick = { look = l; finish = l.defaultFinish }
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(UCard)
                                .border(2.dp, if (on) UInk else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { pick() }.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Character(l, l.defaultFinish, who, 0f, false, 92.dp, draggable = false) { pick() }
                                Text(l.title, style = uUi.copy(fontSize = 13.sp, fontWeight = FontWeight.W600, color = if (on) UInk else UMute), maxLines = 1)
                            }
                        }
                    }
                }
                1 -> Finish.entries.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { f ->
                            val on = f == finish
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { finish = f }.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FinishSwatch(f, on, UInk)
                                Text(f.title, style = uUi.copy(fontSize = 12.sp, lineHeight = 14.sp, color = if (on) UInk else UMute),
                                    textAlign = TextAlign.Center, maxLines = 2)
                                if (f == look.defaultFinish) Text("SUGGESTED", style = uEyebrow.copy(fontSize = 9.sp, letterSpacing = 1.sp, color = accent))
                            }
                        }
                    }
                }
                else -> Companion.entries.forEach { c ->
                    val on = c == who
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(UCard)
                        .border(2.dp, if (on) accent else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { who = c }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(c.title, style = uHead.copy(fontSize = 22.sp, lineHeight = 26.sp))
                            Text(c.trait.uppercase(), style = uEyebrow)
                        }
                        Text(c.line(sample), style = uUi.copy(fontSize = 15.sp, lineHeight = 21.sp, color = UInk.copy(alpha = if (on) 1f else .72f)))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Back / Next
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(14.dp))
                .clickable { step-- }.padding(15.dp), contentAlignment = Alignment.Center) {
                Text("Back", style = uUi.copy(fontWeight = FontWeight.W600, fontSize = 16.sp))
            } else Spacer(Modifier.weight(1f))
            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(accent)
                .clickable { if (step < 2) step++ else start() }.padding(15.dp), contentAlignment = Alignment.Center) {
                Text(if (step < 2) "Next" else "Start", style = uUi.copy(fontWeight = FontWeight.W600, fontSize = 16.sp, color = UBg))
            }
        }
    }
}

package com.mwooten.chime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Settings
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.label
import com.mwooten.chime.data.shelf
import com.mwooten.chime.worlds.Bell
import com.mwooten.chime.worlds.Character

/** The task editor, on the same dark stage as the menus, with your companion saying its piece. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestSheet(
    q: Quest,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onToggle: () -> Unit,
    onSnooze: () -> Unit,
    onTomorrow: () -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(q.id) { mutableStateOf(q.text) }
    var armDelete by remember { mutableStateOf(false) }
    LaunchedEffect(armDelete) { if (armDelete) { kotlinx.coroutines.delay(3000); armDelete = false } }
    fun close() { if (name.isNotBlank() && name != q.text) onRename(name.trim()); onDismiss() }

    val who = Settings.companion(ctx)
    val look = Settings.look(ctx)
    val finish = Settings.finish(ctx)
    val accent = Color(finish.c)
    val overdue = !q.done && q.shelf() == Shelf.OVERDUE

    ModalBottomSheet(
        onDismissRequest = { close() },
        sheetState = state,
        containerColor = Stage.card,
        contentColor = Stage.ink,
        scrimColor = Color(0xB3060608),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Stage.line) },
    ) {
        StageEntrance {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // your companion, saying what it thinks about this one
                Rise(0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Character(look, finish, who, 0f, false, 64.dp, draggable = false) { Bell.strike(ctx, who) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(who.title.uppercase(), style = Stage.eyebrow.copy(color = Color(finish.light).copy(alpha = .8f)))
                            Text(who.line(q), style = Stage.ui.copy(fontFamily = Fonts.newsreader, fontStyle = FontStyle.Italic, fontWeight = FontWeight.W400,
                                fontSize = 16.sp, lineHeight = 21.sp, color = Stage.ink.copy(alpha = .85f)), maxLines = 3)
                        }
                    }
                }
                Rise(1) {
                    StageField(
                        name, { name = it }, "What needs doing?",
                        singleLine = false,
                        textStyle = Stage.head.copy(fontSize = 22.sp, lineHeight = 28.sp),
                        cursor = accent,
                        background = Stage.bg,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onRename(name.trim()) }),
                    )
                }
                Rise(2) {
                    Text(
                        (if (q.done) "Finished" else q.label()) + "  ·  Added " + Time.dayName(Time.date(q.created)).lowercase() + " " + Time.time(q.created),
                        style = Stage.body.copy(fontSize = 13.sp, color = if (overdue) Stage.bad else Stage.mute),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                Rise(3) {
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StageButton(if (q.done) "Not done yet" else "Mark done", Modifier.weight(1f), fill = accent) { onToggle(); onDismiss() }
                        StageButton("Snooze 1 hour", Modifier.weight(1f)) { onSnooze(); onDismiss() }
                    }
                }
                Rise(4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StageButton("Move to tomorrow", Modifier.weight(1f)) { onTomorrow(); onDismiss() }
                        StageButton(if (armDelete) "Really delete?" else "Delete", Modifier.weight(1f),
                            fill = if (armDelete) Stage.bad else null, tone = Stage.bad) {
                            if (armDelete) { onDelete(); onDismiss() } else armDelete = true
                        }
                    }
                }
            }
        }
    }
}

package com.mwooten.chime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Time

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(q.id) { mutableStateOf(q.text) }
    var armDelete by remember { mutableStateOf(false) }
    LaunchedEffect(armDelete) { if (armDelete) { kotlinx.coroutines.delay(3000); armDelete = false } }
    fun close() { if (name.isNotBlank() && name != q.text) onRename(name.trim()); onDismiss() }

    ModalBottomSheet(
        onDismissRequest = { close() },
        sheetState = state,
        containerColor = Color(0xFF12241C),
        contentColor = C.parchment,
        scrimColor = Color(0x99030806),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x7308130F)).border(1.dp, C.line, RoundedCornerShape(12.dp)).padding(14.dp)) {
                BasicTextField(name, { name = it }, textStyle = T.body.copy(fontSize = 19.sp), cursorBrush = SolidColor(C.glow),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onRename(name.trim()) }),
                    modifier = Modifier.fillMaxWidth())
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestChip(q)
                Chip("Spilled " + Time.dayName(Time.date(q.created)).lowercase() + " " + Time.time(q.created))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrassButton(if (q.done) "Not done yet" else "Mark done", Modifier.weight(1f)) { onToggle(); onDismiss() }
                StoneButton("Snooze 1 hour", Modifier.weight(1f)) { onSnooze(); onDismiss() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StoneButton("Move to tomorrow", Modifier.weight(1f)) { onTomorrow(); onDismiss() }
                if (armDelete) BrassButton("Tap again to delete", Modifier.weight(1f)) { onDelete(); onDismiss() }
                else StoneButton("Delete", Modifier.weight(1f), C.ember) { armDelete = true }
            }
        }
    }
}

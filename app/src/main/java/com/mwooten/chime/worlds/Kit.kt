package com.mwooten.chime.worlds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.label

/** Companion colors as Compose colors. */
val Companion.c: Color get() = Color(color)
val Companion.cLight: Color get() = Color(light)
val Companion.cDark: Color get() = Color(dark)

/** "by 12:00 PM", "3:00 PM", "Today", "Fri", "Someday" — short due text for tight spots. */
fun Quest.shortDue(): String {
    val d = due ?: return "Someday"
    if (allDay) return Time.dayName(Time.date(d)).let { if (it.length > 9) it.take(3) else it }
    val sameDay = Time.date(d) == Time.date(System.currentTimeMillis())
    return if (sameDay) Time.time(d) else Time.dayName(Time.date(d)).take(3) + " " + Time.time(d)
}

/** Full due text, e.g. "Today · by 12:00 PM". */
fun Quest.dueText(): String = label()

/** Minutes until due (negative when late), or null. */
fun Quest.minutesLeft(now: Long = System.currentTimeMillis()): Long? = due?.let { (it - now) / 60_000 }

/**
 * A themed one-line input for spilling a quest. Clears itself after submit.
 * Style it through [textStyle], [placeholder] and [modifier]. If [trailing] is given it is drawn
 * to the right of the field and receives a `submit` callback to wire to your own button.
 */
@Composable
fun SpillInput(
    onSpill: (String) -> Unit,
    textStyle: TextStyle,
    placeholder: String,
    placeholderColor: Color,
    cursor: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable (submit: () -> Unit) -> Unit)? = null,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = { if (text.isNotBlank()) { onSpill(text.trim()); text = "" } }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Text(placeholder, style = textStyle.copy(color = placeholderColor), maxLines = 1)
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(cursor),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailing != null) trailing(submit)
    }
}

package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.rememberClock
import com.mwooten.chime.ui.tap

private val MomBg = Color(0xFFFBFAF7)
private val MomLine = Color(0xFFE6E3DC)
private val MomTeal = Color(0xFF1F6F6A)
private val MomGrayBubble = Color(0xFFECEBE6)
private val MomInk = Color(0xFF1B1B1B)
private val MomSub = Color(0xFF7A766D)
private val MomStampColor = Color(0xFF9A958B)
private val MomIcon = Color(0xFF5A564E)

@Composable
fun MomWorld(s: WorldState, a: WorldActions) {
    val screenW = LocalConfiguration.current.screenWidthDp
    val mine = (screenW * 0.74f).dp
    val hers = (screenW * 0.78f).dp
    val scroll = rememberScrollState()
    val voice = rememberVoice { heard -> a.spill(heard) }
    val heard = voice.heard
    LaunchedEffect(scroll.maxValue, s.quests.size, s.focus?.id, voice.listening, heard) { scroll.scrollTo(scroll.maxValue) }

    // your past spills, oldest first
    val spills = s.quests.sortedBy { it.created }.takeLast(8)
    val focus = s.focus
    val day = Time.date(s.now).toEpochDay()
    val hourBucket = Time.ldt(s.now).hour / 4
    val justDone = s.done.firstOrNull { q -> q.doneAt != null && s.now - q.doneAt < 6 * 3600_000L }

    Column(Modifier.fillMaxSize().background(MomBg).imePadding()) {
        // header: back to the list · Mom's photo (your character) · Settings
        Row(
            Modifier
                .fillMaxWidth()
                .background(MomBg)
                .statusBarsPadding()
                .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.tap { a.openLog() }.padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(width = 10.dp, height = 18.dp)) {
                    val k = size.width / 10f
                    val p = Path().apply { moveTo(8 * k, 2 * k); lineTo(2 * k, 9 * k); lineTo(8 * k, 16 * k) }
                    drawPath(p, MomTeal, style = Stroke(2.2f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                Text("List", fontSize = 16.sp, color = MomTeal, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(start = 4.dp))
            }
            Box(
                Modifier.padding(start = 6.dp).size(42.dp).clip(CircleShape).background(Color(0xFFE9E4D8)),
                contentAlignment = Alignment.Center,
            ) {
                CharacterArt(
                    size = 38.dp, style = ArtStyle.FLAT, level = voice.level, listening = voice.listening,
                    glow = false, onTap = { voice.toggle() },
                )
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("Mom", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MomInk, fontFamily = FontFamily.SansSerif)
                Text(
                    if (voice.listening) "listening… tap her photo to stop" else "tap her photo to talk",
                    fontSize = 12.sp, color = if (voice.listening) MomTeal else MomSub,
                    fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MomTeal, fontFamily = FontFamily.SansSerif,
                modifier = Modifier.tap { a.openOptions() }.padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MomLine))

        // thread
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            var lastDay: java.time.LocalDate? = null
            spills.forEach { q ->
                val d = Time.date(q.created)
                if (d != lastDay) MomStampText(Time.dayName(d).let { if (it.length > 9) it.take(3) else it } + " " + Time.time(q.created), top = 4.dp)
                lastDay = d
                MomBubble(q.text, mine = true, maxW = mine, faded = q.done) { a.openQuest(q) }
                val ackSeed = momSeed(q.id, if (q.done) "done" else "ack")
                MomBubble(momAck(q, ackSeed), mine = false, maxW = hers) { a.openQuest(q) }
            }

            // what Mom is saying right now
            MomStampText(Time.dayName(Time.date(s.now)) + " " + Time.time(s.now), top = 10.dp)
            MomBubble(momSay(momGreetings(s.now), momSeed("hello", day, hourBucket)), mine = false, maxW = hers)
            if (justDone != null && justDone.id != spills.lastOrNull()?.id) {
                MomBubble(momSay(MomFinished, momSeed(justDone.id, "proud", day), justDone), mine = false, maxW = hers) { a.openQuest(justDone) }
            }
            val baseSeed = momSeed(focus?.id ?: "empty", day)
            if (focus != null) {
                MomBubble(momSay(momPoolFor(focus, s.now), baseSeed, focus, s.now), mine = false, maxW = hers) { a.openQuest(focus) }
            } else {
                MomBubble(momSay(MomEmpty, baseSeed), mine = false, maxW = hers)
            }
            val r = java.util.Random(baseSeed xor 0x5EED)
            if (r.nextInt(10) < 6) MomBubble(momSay(MomQuirks, baseSeed + 17), mine = false, maxW = hers)
            MomBubble(MomSignoffs[java.util.Random(baseSeed + 31).nextInt(MomSignoffs.size)], mine = false, maxW = hers)

            if (focus != null) {
                // reply chips: the done / snooze of this world
                Row(
                    Modifier.align(Alignment.End).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MomChip("done", MomTeal, MomTeal) { a.done(focus) }
                    MomChip("in a bit mom", Color(0xFFC9C5BB), MomIcon) { a.snooze(focus, 30) }
                    MomChip("tonight", Color(0xFFC9C5BB), MomIcon) { a.snooze(focus, 240) }
                }
            }

            if (voice.listening) {
                // what you're saying, appearing as a draft on your side
                MomBubble(if (heard.isNotBlank()) "$heard…" else "…", mine = true, maxW = mine, draft = true)
            } else {
                val note = voice.note
                if (note.isNotBlank()) MomStampText(note, top = 2.dp)
                MomTyping(Modifier.padding(top = 4.dp))
            }
        }

        // composer = spill. The list button sits inside the row, so nothing floats over it.
        Box(Modifier.fillMaxWidth().height(1.dp).background(MomLine))
        Row(
            Modifier.fillMaxWidth().background(MomBg).navigationBarsPadding().padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).background(MomGrayBubble, CircleShape).tap { a.openLog() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val k = size.width / 16f
                    val w = 1.8f * k
                    drawLine(MomIcon, Offset(3 * k, 4 * k), Offset(13 * k, 4 * k), w, cap = StrokeCap.Round)
                    drawLine(MomIcon, Offset(3 * k, 8 * k), Offset(13 * k, 8 * k), w, cap = StrokeCap.Round)
                    drawLine(MomIcon, Offset(3 * k, 12 * k), Offset(9 * k, 12 * k), w, cap = StrokeCap.Round)
                }
            }
            SpillInput(
                onSpill = a::spill,
                textStyle = TextStyle(fontSize = 16.sp, color = MomInk, fontFamily = FontFamily.SansSerif),
                placeholder = "Text Mom what you need to do",
                placeholderColor = MomStampColor,
                cursor = MomTeal,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFD9D5CC), RoundedCornerShape(10.dp))
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                trailing = { submit ->
                    Text(
                        "Send", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MomTeal, fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.tap { submit() }.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun MomStampText(text: String, top: Dp) {
    Box(Modifier.fillMaxWidth().padding(top = top, bottom = 2.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 11.sp, color = MomStampColor, fontFamily = FontFamily.SansSerif)
    }
}

@Composable
private fun MomBubble(
    text: String,
    mine: Boolean,
    maxW: Dp,
    faded: Boolean = false,
    draft: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = if (mine) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            text,
            fontSize = 16.sp, lineHeight = 21.sp, fontFamily = FontFamily.SansSerif,
            fontStyle = if (draft) FontStyle.Italic else FontStyle.Normal,
            color = when { draft -> MomTeal; mine -> Color.White; else -> MomInk },
            modifier = Modifier
                .widthIn(max = maxW)
                .graphicsLayer { alpha = if (faded) 0.6f else 1f }
                .then(
                    if (draft) Modifier.border(1.5.dp, MomTeal, shape)
                    else Modifier.background(if (mine) MomTeal else MomGrayBubble, shape)
                )
                .then(if (onClick != null) Modifier.tap(onClick) else Modifier)
                .padding(horizontal = 13.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun MomChip(label: String, border: Color, color: Color, onClick: () -> Unit) {
    Text(
        label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = color, fontFamily = FontFamily.SansSerif,
        modifier = Modifier
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .tap(onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun MomTyping(modifier: Modifier) {
    val clock = rememberClock()
    Row(
        modifier.background(MomGrayBubble, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in 0..2) {
            Box(
                Modifier
                    .graphicsLayer {
                        val p = momDot(clock.value - i * 0.15f)
                        translationY = -3.dp.toPx() * p
                        alpha = 0.4f + 0.6f * p
                    }
                    .size(7.dp)
                    .background(Color(0xFF8A867C), CircleShape)
            )
        }
    }
}

/** dot keyframes: rest, up at 30%, back down by 60% of a 1.2s loop. */
private fun momDot(t: Float): Float {
    val p = (((t % 1.2f) + 1.2f) % 1.2f) / 1.2f
    return when {
        p < 0.3f -> p / 0.3f
        p < 0.6f -> 1f - (p - 0.3f) / 0.3f
        else -> 0f
    }
}

// ---------------------------------------------------------------------------------------------
// What Mom says. Every pick is seeded by quest id + day, so a message stays put while you look at
// it but is different tomorrow, and different for each thing on your list.
// ---------------------------------------------------------------------------------------------

private fun momSeed(vararg parts: Any?): Long {
    var h = 1125899906842597L
    for (c in parts.joinToString("|")) h = 31 * h + c.code
    return h
}

/** Task text as Mom would type it mid-sentence: first letter lowercased unless it looks like an acronym. */
private fun momTask(q: Quest): String {
    val t = q.text.trim().trimEnd('.', '!')
    if (t.length < 2 || t[1].isUpperCase()) return t
    return t[0].lowercaseChar() + t.substring(1)
}

private fun momWhen(q: Quest, now: Long): String {
    val d = q.due ?: return "someday"
    val day = Time.dayName(Time.date(d), Time.date(now)).lowercase()
    return if (q.allDay) day else day + " " + (if (q.kind == "by") "by " else "at ") + Time.time(d)
}

private fun momSay(pool: List<String>, seed: Long, q: Quest? = null, now: Long = System.currentTimeMillis()): String {
    val r = java.util.Random(seed)
    var msg = pool[r.nextInt(pool.size)]
    if (q != null) {
        val due = q.due
        val time = when {
            due == null -> "someday"
            q.allDay -> Time.dayName(Time.date(due), Time.date(now)).lowercase()
            else -> Time.time(due)
        }
        msg = msg
            .replace("{t}", momTask(q))
            .replace("{T}", momTask(q).uppercase())
            .replace("{time}", time)
            .replace("{when}", momWhen(q, now))
            .replace("{day}", if (due == null) "someday" else Time.dayName(Time.date(due), Time.date(now)).lowercase())
            .replace("{left}", if (due == null) "a while" else Time.rel(due - now))
    }
    // sometimes she hits caps lock and doesn't notice
    if (r.nextInt(9) == 0) msg = msg.uppercase()
    return msg
}

private fun momPoolFor(q: Quest, now: Long): List<String> = when (q.shelf(now)) {
    Shelf.OVERDUE -> MomOverdue
    Shelf.TODAY -> if (q.allDay) MomAllDay else MomDueSoon
    Shelf.SOMEDAY -> MomSomeday
    Shelf.DONE -> MomFinished
    else -> MomLater
}

private fun momAck(q: Quest, seed: Long): String = when {
    q.done -> momSay(MomFinished, seed, q)
    q.due == null -> momSay(MomAckSomeday, seed, q)
    else -> momSay(MomAck, seed, q)
}

private fun momGreetings(now: Long): List<String> {
    val h = Time.ldt(now).hour
    return when {
        h < 5 -> MomLateNight
        h < 11 -> MomMorning
        h < 17 -> MomAfternoon
        else -> MomEvening
    }
}

private val MomMorning = listOf(
    "Good morning sweetie ☀️",
    "Morning! Did you sleep ok",
    "Good morning honey. Eat breakfast",
    "Morning sunshine 🌞🍳",
    "Rise and shine! It's Mom",
    "Good morning. I've been up since 5 don't ask",
    "Morning honey. Biscuit woke me up at 6 to go out 🐶",
    "Hi sweetie good morning comma hope you slept",
    "Good morning!! Coffee first then read this",
    "Morning. It's cold out wear a jacket 🧥",
    "Good mourning sweetie. *morning. Phone",
)

private val MomAfternoon = listOf(
    "Hi honey just checking in",
    "Hi sweetie. Quick thing",
    "Hi it's Mom",
    "Hope your day is going ok",
    "Hi! On my lunch break 🥪",
    "Hello dear. It's your mother",
    "Hi honey. At the grocery store, need anything?",
    "Hi sweetie hope work is ok. Don't let them push you around",
    "Hey hon. Aunt Linda just called. Long story. Anyway",
    "Hi!! Just thinking about you 🐔",
)

private val MomEvening = listOf(
    "Evening sweetie",
    "Hi honey hope your day was ok",
    "Are you home yet? Text me when you're home",
    "Hi. Watching my show. Quick thing",
    "Hi sweetie. Did you have dinner?",
    "Good evening dear. Your father fell asleep in his chair again 😂",
    "Hi honey. Walking Biscuit, he says hi 🐕",
    "Hey sweetie it's getting late",
    "Hi. Don't stay up too late on that phone",
    "Evening hon. Long day? Mine too",
)

private val MomLateNight = listOf(
    "Why are you up?? Go to bed",
    "Honey it's the middle of the night",
    "I can see you're awake. Moms know",
    "Can't sleep either? Warm milk",
    "It is very late. Bed. Now. Love you",
)

private val MomDueSoon = listOf(
    "Don't forget {t} at {time}",
    "Honey {t} is at {time}. That's in {left}!!",
    "Just a reminder about {t}. {time}. I set an alarm on my phone for you too",
    "{t} at {time}. Don't be late like last time",
    "DON'T FORGET {T} AT {time}",
    "Hi sweetie did you remember {t}? It's at {time}",
    "{t} is in {left} 👍👍👍",
    "I wrote {t} on my calendar for {time}. Is that right",
    "Hi it's Mom. {t} at {time}. Ok that's all",
    "Did you {t} yet question mark it's at {time} period",
    "Aunt Linda says {t} is very important. She's right for once. {time}",
    "{left} until {t}!! You can do it 🎂",
    "Sorry to bother you. {t} at {time}. Ok bye",
    "Leave early for {t}. Traffic. {time}",
    "Remember {t}!!! {time}!!! Love you!!!",
)

private val MomOverdue = listOf(
    "Did you do {t}? It was {time}",
    "Honey. {t}. It was supposed to be {time}",
    "I'm not mad I just want to know if {t} happened",
    "Hello?? {t}??",
    "Still waiting to hear about {t} 😂",
    "{t} was due {time}. Call me",
    "Your father says I should stop asking about {t}. I'm asking anyway",
    "I KNOW YOU SEE THESE. {T}",
    "It's ok if {t} is late. Just do it today ok? 🙏",
    "Did you duck {t} yet. *do. Stupid phone",
    "Just making sure you're alive. And about {t}",
    "Remember when you used to hide your homework under the bed. {t} sweetie",
    "{t}. {time}. That was a while ago honey",
    "I'm going to keep texting about {t} until you answer me",
)

private val MomAllDay = listOf(
    "{t} is today! Big day",
    "Today's the day for {t} 🎉",
    "Hi honey. {t} today. Don't wait until the last minute",
    "Is {t} today or tomorrow? I have it as today",
    "TODAY: {t}",
    "Good luck with {t} today 👻",
    "Just thinking about you and {t}. That's today right?",
    "I lit a candle for {t} today. Kidding. Maybe",
    "{t} today! Proud of you already",
    "Don't forget {t} today honey. Before dinner",
)

private val MomLater = listOf(
    "{t} is {day}. Just so you know",
    "Putting {t} on my calendar for {day}",
    "You have until {day} for {t}. But don't wait",
    "No rush on {t}, it's not until {day}",
    "Ok so {t} is {day} right? Aunt Linda wants to know if you're free",
    "Mark your calendar: {t} {day} 📅🐟",
    "Thinking ahead!! {t} {day}. Look at you",
    "Don't forget {t} {day}. I won't let you",
    "Hi just confirming {t} is {when}",
)

private val MomSomeday = listOf(
    "Whenever you get a chance, {t}",
    "Did you ever {t}? No pressure",
    "{t}. Someday. But maybe soon? 🙂",
    "Just saying {t} isn't going to do itself honey",
    "Not nagging. Just {t}",
    "Remember {t}? I remember",
    "Your cousin is so organized. Anyway, {t}",
    "When are you going to {t}? Pick a day sweetie",
    "{t} has been on your list a while honey 🐌",
    "I'm not saying anything about {t}. Just noticing",
)

private val MomEmpty = listOf(
    "Nothing on your list? Come over for dinner then",
    "You have nothing to do?? Call your grandmother",
    "Free day! Did you eat",
    "Proud of you for getting everything done. Now rest",
    "I have nothing to remind you about. That's a first 😂",
    "No chores? Must be nice. Kidding. Love you",
    "If you're bored you could call your mother",
    "ALL CAUGHT UP? WHO ARE YOU",
    "Enjoy your free time sweetie. Wear sunscreen",
    "Nothing to do. Want me to give you something? Your closet",
    "Your list is empty. My list is never empty. Anyway 🌻",
)

private val MomFinished = listOf(
    "So proud of you!! {t} done",
    "You did {t}! I told your father",
    "{T} DONE 🎉🎉🎉",
    "Knew you could do {t}. Never doubted. Ok maybe once",
    "Good job on {t} honey 😭",
    "Yay {t}! Treat yourself. Not too much",
    "I'm putting {t} on the fridge",
    "Told Aunt Linda about {t}. She says congrats",
    "That's my kid. {t} 👏",
    "{t}, done. Wonderful. Now drink some water",
    "Look at you go. {t}!! 🏆🥔",
)

private val MomAck = listOf(
    "Ok got it. {when}",
    "Ok! {when}. Writing it down",
    "Got it honey. {t} {when}",
    "K. {when} 👍",
    "Ok sweetie {when}. I'll remind you",
    "Noted. {when}. Did you eat",
    "Oh good. {when}. I'll text you",
    "Okay okay. {when} 🐢",
    "{t}?? Ok. {when}",
    "Ok I put it on the fridge calendar. {when}",
)

private val MomAckSomeday = listOf(
    "Ok got it. Whenever you can",
    "Ok no date? Ok",
    "Writing it down. Pick a day honey",
    "Ok. Don't forget",
    "Got it. I'll bug you about it later 😊",
    "Ok sweetie. Someday is not a day but ok",
    "Noted 📝",
)

private val MomQuirks = listOf(
    "Did you eat today?",
    "Have you eaten. Real food not chips",
    "Biscuit says hi 🐶",
    "The dog got into the trash again. Anyway",
    "Aunt Linda says hi. She got a new phone too 🙄",
    "CALL ME when you get a chance. Nothing's wrong",
    "Call me. Nothing is wrong I just want to hear your voice",
    "Did you get the article I sent you",
    "Are you wearing a coat? It's supposed to get cold",
    "Your father says hi. He's in the garage",
    "Drink some water 💧",
    "Uncle Ray's knee is better 😂 he's fine",
    "How do I make the letters bigger on this",
    "Sorry that was meant for Aunt Linda",
    "Who is this? Kidding it's Mom",
    "Dinner Sunday? 🎃",
    "Are you getting enough sleep",
    "Saw your friend's mom at the store. She says hi",
)

private val MomSignoffs = listOf(
    "Love, Mom",
    "Love Mom",
    "Love you. Mom",
    "-Mom",
    "Love, Mom ❤️🌽",
    "MOM",
    "Love, Mom (this is Mom)",
    "Love you!! Call me. Mom",
    "Text me \"done\" when it's done and I'll stop asking. Love, Mom",
    "Ok bye. Love, Mom",
    "xoxo Mom",
)

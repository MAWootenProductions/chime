package com.mwooten.chime.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.shelf
import com.mwooten.chime.ui.Fonts
import com.mwooten.chime.ui.tap
import kotlinx.coroutines.launch

// circa 2010: a solid blue bar, white boxes on pale gray, tiny tahoma-ish type, blue names.
private val BookBlue = Color(0xFF3C5A98)
private val BookBlueDark = Color(0xFF2A4580)
private val BookBg = Color(0xFFE9EBEE)
private val BookCardLine = Color(0xFFDDDFE2)
private val BookInk = Color(0xFF1D2129)
private val BookGray = Color(0xFF90949C)
private val BookLink = Color(0xFF3C5A98)
private val BookLikeBg = Color(0xFFEDEFF4)
private val BookHeadBg = Color(0xFFF6F7F9)
private val BookBadge = Color(0xFFDD3C10)
private val BookFieldLine = Color(0xFFBDC7D8)
private val BookFont = FontFamily.SansSerif

@Composable
fun FeedWorld(s: WorldState, a: WorldActions) {
    val feedScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val toTop: () -> Unit = { scope.launch { feedScroll.animateScrollTo(0) } }
    var liked by remember { mutableStateOf(setOf<String>()) }
    val voice = rememberVoice { heard -> a.spill(heard) }
    val focus = s.focus
    val others = Companion.entries.filter { it != s.companion }
    val requests = s.open.filter { s.now - it.created < 24 * 3600_000L }.take(3)
    val events = s.today.size + s.week.size

    Column(Modifier.fillMaxSize().background(BookBg).imePadding()) {
        // the blue bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(BookBlue)
                .statusBarsPadding()
                .height(44.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "chimebook", fontFamily = Fonts.archivo, fontWeight = FontWeight.W700, fontSize = 22.sp,
                letterSpacing = (-0.5).sp, color = Color.White, modifier = Modifier.tap { toTop() },
            )
            Row(Modifier.weight(1f).padding(start = 10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                BookJewel("Requests", requests.size) { toTop() }
                BookJewel("Due", s.today.size) { a.openLog() }
            }
            Text(
                "Settings ▾", fontFamily = BookFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White,
                modifier = Modifier.tap { a.openOptions() }.padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BookBlueDark))

        Row(Modifier.weight(1f).fillMaxWidth()) {
            // left column
            Column(
                Modifier.width(98.dp).verticalScroll(rememberScrollState()).padding(start = 8.dp, top = 10.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookPhoto(34.dp) {
                        CharacterArt(size = 32.dp, style = ArtStyle.FLAT, level = voice.level, listening = voice.listening, glow = false, onTap = { voice.toggle() })
                    }
                    Text(
                        "You", fontFamily = BookFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BookLink,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Box(Modifier.height(8.dp))
                BookNav("News Feed", BookBlue, bold = true) { toTop() }
                BookNav("Events" + if (events > 0) " ($events)" else "", Color(0xFFE0A030)) { a.openLog() }
                BookNav("Requests" + if (requests.isNotEmpty()) " (${requests.size})" else "", Color(0xFF4A9A4A)) { toTop() }
                BookNav("Memories", Color(0xFF8A6AB8)) { a.openLog() }
                BookNav("Settings", Color(0xFF8A8D91)) { a.openOptions() }
            }

            // the feed
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(feedScroll)
                    .padding(start = 4.dp, end = 8.dp, top = 10.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // status box
                BookBox {
                    Row(Modifier.fillMaxWidth().background(BookHeadBg).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Update Status", fontFamily = BookFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BookInk)
                        Text(
                            if (voice.listening) "  ·  listening…" else "  ·  tap your photo to talk",
                            fontFamily = BookFont, fontSize = 11.sp, color = BookGray, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
                        BookPhoto(40.dp) {
                            CharacterArt(size = 38.dp, style = ArtStyle.FLAT, level = voice.level, listening = voice.listening, glow = false, onTap = { voice.toggle() })
                        }
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            SpillInput(
                                onSpill = a::spill,
                                textStyle = TextStyle(fontFamily = BookFont, fontSize = 13.sp, color = BookInk),
                                placeholder = when {
                                    voice.listening && voice.heard.isNotBlank() -> voice.heard + "…"
                                    voice.listening -> "Listening…"
                                    else -> "What's on your mind?"
                                },
                                placeholderColor = if (voice.listening) BookLink else BookGray,
                                cursor = BookInk,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BookFieldLine)
                                    .background(Color.White)
                                    .padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                trailing = { submit -> BookButton("Share", primary = true, modifier = Modifier.padding(start = 6.dp)) { submit() } },
                            )
                            if (voice.note.isNotBlank()) Text(
                                voice.note, fontFamily = BookFont, fontSize = 11.sp, color = BookGray,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                // friend requests = things you just added
                if (requests.isNotEmpty()) {
                    BookBox {
                        BookBoxHead("Requests", "See All") { a.openLog() }
                        requests.forEach { q ->
                            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BookLink)) { append(q.text) }
                                        append(" wants to be on your list.")
                                    },
                                    fontFamily = BookFont, fontSize = 12.sp, lineHeight = 16.sp, color = BookInk,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.tap { a.openQuest(q) },
                                )
                                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BookButton("Confirm", primary = true) { a.openQuest(q) }
                                    BookButton("Not Now", primary = false) { a.snooze(q, 60) }
                                }
                            }
                        }
                    }
                }

                // birthday-style reminders
                if (s.today.isNotEmpty()) {
                    BookBox {
                        BookBoxHead("Reminders", "${s.today.size}") { a.openLog() }
                        s.today.take(3).forEach { q ->
                            Row(
                                Modifier.fillMaxWidth().tap { a.openQuest(q) }.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BookGift()
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BookLink)) { append(q.text) }
                                        append(bookBirthday(q, s.now))
                                    },
                                    fontFamily = BookFont, fontSize = 12.sp, lineHeight = 16.sp, color = BookInk,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }

                // the companion's status about your most urgent thing
                BookBox {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            BookPhoto(40.dp) {
                                CharacterArt(size = 38.dp, style = ArtStyle.FLAT, who = s.companion, glow = false, onTap = { voice.toggle() })
                            }
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BookLink)) { append(s.companion.title) }
                                        append(
                                            if (focus != null) " is reminding you to " + bookTask(focus) + "."
                                            else " is feeling " + s.companion.trait.lowercase() + "."
                                        )
                                    },
                                    fontFamily = BookFont, fontSize = 13.sp, lineHeight = 17.sp, color = BookInk,
                                    modifier = if (focus != null) Modifier.tap { a.openQuest(focus) } else Modifier,
                                )
                                Text(
                                    s.line, fontFamily = BookFont, fontSize = 13.sp, lineHeight = 17.sp, color = BookInk,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                val key = "main:" + (focus?.id ?: "none")
                                BookMeta(
                                    ago = focus?.let { bookAgo(s.now - it.created) } ?: "Just now",
                                    liked = key in liked,
                                    onLike = { liked = if (key in liked) liked - key else liked + key },
                                    onComment = { if (focus != null) a.openQuest(focus) else toTop() },
                                    onShare = toTop,
                                )
                                BookLikes(
                                    (if (key in liked) "You, " else "") + "${others[0].title} and ${others[1].title} like this.",
                                )
                                if (focus != null) {
                                    // two friends chime in underneath
                                    others.take(2).forEach { c -> BookComment(c, c.line(focus, s.now)) }
                                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        BookButton("Mark done", primary = true) { a.done(focus) }
                                        BookButton("Snooze 30 min", primary = false) { a.snooze(focus, 30) }
                                    }
                                }
                            }
                        }
                    }
                }

                // more posts: the others write on your wall about what's next
                val rest = s.open.filter { it.id != focus?.id }.take(5)
                rest.forEachIndexed { i, q ->
                    val who = others[i % others.size]
                    val key = "wall:" + q.id
                    BookBox {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            BookPhoto(40.dp) {
                                CharacterArt(size = 38.dp, style = ArtStyle.FLAT, who = who, glow = false, onTap = { voice.toggle() })
                            }
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BookLink)) { append(who.title) }
                                        withStyle(SpanStyle(color = BookGray)) { append(" ▸ ") }
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BookLink)) { append(q.text) }
                                    },
                                    fontFamily = BookFont, fontSize = 13.sp, lineHeight = 17.sp, color = BookInk,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.tap { a.openQuest(q) },
                                )
                                Text(
                                    who.line(q, s.now), fontFamily = BookFont, fontSize = 13.sp, lineHeight = 17.sp, color = BookInk,
                                    maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp),
                                )
                                BookMeta(
                                    ago = q.shortDue(),
                                    liked = key in liked,
                                    onLike = { liked = if (key in liked) liked - key else liked + key },
                                    onComment = { a.openQuest(q) },
                                    onShare = null,
                                    extra = "Done" to { a.done(q) },
                                )
                            }
                        }
                    }
                }

                // a memory from something you finished
                val memory = s.done.firstOrNull()
                if (memory != null) {
                    val doneAt = memory.doneAt ?: memory.created
                    BookBox {
                        Row(Modifier.padding(10.dp).tap { a.openLog() }, verticalAlignment = Alignment.Top) {
                            BookPhoto(40.dp) {
                                CharacterArt(size = 38.dp, style = ArtStyle.FLAT, glow = false, onTap = { voice.toggle() })
                            }
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BookLink)) { append("You") }
                                        append(" completed ")
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(memory.text) }
                                        append(".")
                                    },
                                    fontFamily = BookFont, fontSize = 13.sp, lineHeight = 17.sp, color = BookInk,
                                )
                                Text(
                                    bookMemory(memory, doneAt), fontFamily = BookFont, fontSize = 12.sp, lineHeight = 16.sp,
                                    fontStyle = FontStyle.Italic, color = BookGray, modifier = Modifier.padding(top = 3.dp),
                                )
                                Text(
                                    bookAgo(s.now - doneAt) + " · ${s.done.size} completed in all",
                                    fontFamily = BookFont, fontSize = 11.sp, color = BookGray, modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }

                // footer, the way the old site ended every page
                Text(
                    buildAnnotatedString {
                        append("chimebook © 2010 · English (US) · ")
                        withStyle(SpanStyle(color = BookLink)) { append("Settings") }
                    },
                    fontFamily = BookFont, fontSize = 11.sp, color = BookGray,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp).tap { a.openOptions() },
                )
            }
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding())
    }
}

@Composable
private fun BookBox(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).border(1.dp, BookCardLine)) { content() }
}

@Composable
private fun BookBoxHead(title: String, right: String, onRight: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(BookHeadBg).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontFamily = BookFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BookInk, modifier = Modifier.weight(1f))
        Text(right, fontFamily = BookFont, fontSize = 11.sp, color = BookLink, modifier = Modifier.tap(onRight))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BookCardLine))
}

/** A square profile photo with the thin gray frame those always had. */
@Composable
private fun BookPhoto(size: Dp, art: @Composable () -> Unit) {
    Box(
        Modifier.size(size).clipToBounds().background(Color(0xFFF0F2F5)).border(1.dp, BookCardLine),
        contentAlignment = Alignment.Center,
    ) { art() }
}

@Composable
private fun BookNav(label: String, swatch: Color, bold: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tap(onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(swatch, RoundedCornerShape(2.dp)))
        Text(
            label, fontFamily = BookFont, fontSize = 12.sp, color = BookInk,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** A top-bar notification jewel with a red count badge. */
@Composable
private fun BookJewel(label: String, count: Int, onClick: () -> Unit) {
    Row(
        Modifier.tap(onClick).padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = BookFont, fontSize = 11.sp, color = Color(0xFFD8DFEA), maxLines = 1)
        if (count > 0) Text(
            count.toString(), fontFamily = BookFont, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White,
            modifier = Modifier.padding(start = 3.dp).background(BookBadge, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun BookButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        label, fontFamily = BookFont, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        color = if (primary) Color.White else Color(0xFF333333), maxLines = 1,
        modifier = modifier
            .background(if (primary) Color(0xFF5B74A8) else Color(0xFFF2F2F2))
            .border(1.dp, if (primary) BookBlueDark else Color(0xFFCCCCCC))
            .tap(onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

/** "3 hours ago · Like · Comment · Share" under every post. */
@Composable
private fun BookMeta(
    ago: String,
    liked: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: (() -> Unit)?,
    extra: Pair<String, () -> Unit>? = null,
) {
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(ago, fontFamily = BookFont, fontSize = 11.sp, color = BookGray, maxLines = 1)
        BookDot()
        BookLinkText(if (liked) "Unlike" else "Like", onLike)
        BookDot()
        BookLinkText("Comment", onComment)
        if (onShare != null) {
            BookDot()
            BookLinkText("Share", onShare)
        }
        if (extra != null) {
            BookDot()
            BookLinkText(extra.first, extra.second)
        }
    }
}

@Composable
private fun BookDot() {
    Text(" · ", fontFamily = BookFont, fontSize = 11.sp, color = BookGray)
}

@Composable
private fun BookLinkText(label: String, onClick: () -> Unit) {
    Text(
        label, fontFamily = BookFont, fontSize = 11.sp, color = BookLink, maxLines = 1,
        modifier = Modifier.tap(onClick).padding(vertical = 6.dp),
    )
}

@Composable
private fun BookLikes(text: String) {
    Row(
        Modifier.fillMaxWidth().background(BookLikeBg).padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookThumb()
        Text(text, fontFamily = BookFont, fontSize = 11.sp, color = BookLink, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun BookComment(who: Companion, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 1.dp).background(BookLikeBg).padding(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BookPhoto(28.dp) { CharacterArt(size = 26.dp, style = ArtStyle.FLAT, who = who, glow = false) }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BookLink)) { append(who.title) }
                append(" ")
                append(text)
            },
            fontFamily = BookFont, fontSize = 12.sp, lineHeight = 16.sp, color = BookInk,
            maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
    }
}

/** A small hand-drawn thumbs-up, kept generic. */
@Composable
private fun BookThumb() {
    Canvas(Modifier.size(12.dp)) {
        val k = size.width / 12f
        drawRect(BookLink, Offset(1 * k, 5 * k), Size(3 * k, 6 * k))
        val p = Path().apply {
            moveTo(5 * k, 5 * k); lineTo(7 * k, 1 * k); lineTo(8.5f * k, 1.5f * k); lineTo(8 * k, 4.5f * k)
            lineTo(11 * k, 4.5f * k); lineTo(10.5f * k, 11 * k); lineTo(5 * k, 11 * k); close()
        }
        drawPath(p, BookLink)
    }
}

@Composable
private fun BookGift() {
    Canvas(Modifier.size(width = 18.dp, height = 17.dp)) {
        val k = size.width / 24f
        drawRect(Color(0xFF6D84B4), Offset(2 * k, 9 * k), Size(20 * k, 12 * k))
        drawRect(Color(0xFF8B9DC3), Offset(1 * k, 6 * k), Size(22 * k, 4 * k))
        drawLine(Color.White, Offset(12 * k, 6 * k), Offset(12 * k, 21 * k), 2 * k)
        val bow = Path().apply {
            moveTo(12 * k, 6 * k)
            cubicTo(8 * k, 0f, 4 * k, 4 * k, 12 * k, 6 * k)
            cubicTo(20 * k, 4 * k, 16 * k, 0f, 12 * k, 6 * k)
        }
        drawPath(bow, Color(0xFF6D84B4), style = Stroke(1.8f * k))
    }
}

/** Task text mid-sentence: first letter lowercased unless it looks like an acronym. */
private fun bookTask(q: Quest): String {
    val t = q.text.trim().trimEnd('.', '!')
    if (t.length < 2 || t[1].isUpperCase()) return t
    return t[0].lowercaseChar() + t.substring(1)
}

private fun bookBirthday(q: Quest, now: Long): String {
    val d = q.due ?: return " is waiting for a day."
    if (q.shelf(now) == Shelf.OVERDUE) {
        return if (q.allDay) " was due ${Time.dayName(Time.date(d)).lowercase()}." else " was due at ${Time.time(d)}."
    }
    if (q.allDay) return " is due today."
    return " is due today at ${Time.time(d)}."
}

private fun bookMemory(q: Quest, doneAt: Long): String {
    val d = q.due
    if (d != null && !q.allDay && doneAt < d) {
        val early = d - doneAt
        val hours = early / 3_600_000L
        val by = if (hours >= 24) "${hours / 24} day" + (if (hours / 24 == 1L) "" else "s") else if (hours >= 1) "$hours hour" + (if (hours == 1L) "" else "s") else "a few minutes"
        return "Finished $by early. Who was that person?"
    }
    return "Actually finished it. Who was that person?"
}

private fun bookAgo(ms: Long): String {
    val m = (ms / 60_000L).coerceAtLeast(0L)
    return when {
        m < 1 -> "Just now"
        m < 60 -> "$m min ago"
        m < 60 * 24 -> "${m / 60} hour" + (if (m / 60 == 1L) "" else "s") + " ago"
        else -> "${m / (60 * 24)} day" + (if (m / (60 * 24) == 1L) "" else "s") + " ago"
    }
}

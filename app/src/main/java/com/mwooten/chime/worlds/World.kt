package com.mwooten.chime.worlds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.focusQuest
import com.mwooten.chime.data.questOrder
import com.mwooten.chime.data.shelf

/**
 * Everything a world needs to draw itself. Worlds never touch storage directly:
 * they read [WorldState] and call [WorldActions].
 */
class WorldState(
    /** Every quest, done or not. */
    val quests: List<Quest>,
    /** Who is talking. Worlds draw their own version of the companion, tinted with its colors. */
    val companion: Companion,
    /** Wall-clock time this frame was computed for. */
    val now: Long,
) {
    val open: List<Quest> = quests.filter { !it.done }.sortedWith(questOrder)
    /** The most urgent open quest, or null when the list is empty. */
    val focus: Quest? = focusQuest(quests, now)
    /** What the companion says about [focus] (or its empty-list line). */
    val line: String = focus?.let { companion.line(it, now) } ?: companion.emptyLine
    /** Open quests due today or overdue, soonest first. */
    val today: List<Quest> = open.filter { val s = it.shelf(now); s == Shelf.TODAY || s == Shelf.OVERDUE }
    val week: List<Quest> = open.filter { val s = it.shelf(now); s == Shelf.TOMORROW || s == Shelf.WEEK }
    val later: List<Quest> = open.filter { it.shelf(now) == Shelf.LATER }
    val someday: List<Quest> = open.filter { it.due == null }
    val done: List<Quest> = quests.filter { it.done }.sortedByDescending { it.doneAt ?: 0 }
    val overdueCount: Int = open.count { it.shelf(now) == Shelf.OVERDUE }
}

interface WorldActions {
    fun done(q: Quest)
    fun snooze(q: Quest, minutes: Int = 30)
    /** Parse and add a new quest from plain words ("call dentist by 3pm"). */
    fun spill(text: String)
    /** Open the full quest log (list / calendar / shelves). */
    fun openLog()
    /** Open Options (world, companion, reminders, permissions). */
    fun openOptions()
    /** Open the detail sheet for one quest (rename, move, delete). */
    fun openQuest(q: Quest)
    /** Jump straight to the personality picker. */
    fun openPersonality() = openOptions()
    /** Jump straight to the mode picker. */
    fun openMode() = openOptions()
    /** Jump straight to the look and color picker. */
    fun openLook() = openOptions()
}

enum class Category(val title: String) {
    START("Start here"), TALK("Talk to me"), GADGETS("Phones and gadgets"), GAMES("Games"),
    COMPUTERS("Computers and the early web"), PAPER("Sea and sky"),
}

/** Every mode, in picker order. */
enum class WorldId(val title: String, val blurb: String, val category: Category, val dark: Boolean) {
    SIMPLE("Simple", "One sphere. Tap and talk.", Category.START, true),
    PLAIN("Plain", "A paper day planner", Category.TALK, false),
    MOM("Text from Mom", "She signs every text", Category.TALK, false),
    BUDDY("Buddy List", "Away messages and IMs", Category.TALK, false),
    GROUP("Group Chat", "All five, arguing", Category.TALK, true),
    FEED("Social Feed", "Stories and memories", Category.TALK, false),
    EMAIL("Email Inbox", "Swipe to done", Category.TALK, false),
    VOICEMAIL("Voicemail", "Press 7 to mark done", Category.TALK, true),

    FLIP("Old Flip Phone", "160 characters, T9", Category.GADGETS, true),
    ASSISTANT("Assistant on a Call", "A morning briefing", Category.GADGETS, true),
    WHEEL("Click-Wheel Player", "Now due, not now playing", Category.GADGETS, false),
    TOY("Talking Toy", "Learns your words", Category.GADGETS, false),
    HANDHELD("Handheld Console", "Four shades of green", Category.GADGETS, false),
    DEX("Creature Field Guide", "Seen, not caught", Category.GADGETS, true),

    ADVENTURE("Top-Down Adventure", "Deliver it by noon", Category.GAMES, true),
    MINES("Minesweeper", "Defuse your day", Category.GAMES, false),
    OVERWORLD("Overworld 64", "Read the sign", Category.GAMES, false),

    DESKTOP98("Retro Desktop", "Not responding since Thursday", Category.COMPUTERS, false),
    PLATINUM("Platinum Desktop", "Alerts and note pads", Category.COMPUTERS, false),
    PROFILE("Profile Page", "Your Top 8 quests", Category.COMPUTERS, true),

    BOTTLE("Message in a Bottle", "Before the tide comes in", Category.PAPER, false),
    NIGHT("Night Sky", "Your day as a star chart", Category.PAPER, true);

    companion object {
        fun byName(name: String?) = entries.firstOrNull { it.name == name } ?: SIMPLE
    }
}

/** Draws whichever world is selected. Each world fills the whole screen and handles its own system-bar padding. */
@Composable
fun WorldHost(id: WorldId, s: WorldState, a: WorldActions) {
    // every mode draws your character itself, in that mode's own medium (see CharacterArt)
    Box(Modifier.fillMaxSize()) {
    when (id) {
        WorldId.SIMPLE -> SimpleWorld(s, a)
        WorldId.PLAIN -> PlainWorld(s, a)
        WorldId.MOM -> MomWorld(s, a)
        WorldId.BUDDY -> BuddyWorld(s, a)
        WorldId.GROUP -> GroupWorld(s, a)
        WorldId.FEED -> FeedWorld(s, a)
        WorldId.EMAIL -> EmailWorld(s, a)
        WorldId.VOICEMAIL -> VoicemailWorld(s, a)
        WorldId.FLIP -> FlipWorld(s, a)
        WorldId.ASSISTANT -> AssistantWorld(s, a)
        WorldId.WHEEL -> WheelWorld(s, a)
        WorldId.TOY -> ToyWorld(s, a)
        WorldId.HANDHELD -> HandheldWorld(s, a)
        WorldId.DEX -> DexWorld(s, a)
        WorldId.ADVENTURE -> AdventureWorld(s, a)
        WorldId.MINES -> MinesWorld(s, a)
        WorldId.OVERWORLD -> OverworldWorld(s, a)
        WorldId.DESKTOP98 -> Desktop98World(s, a)
        WorldId.PLATINUM -> PlatinumWorld(s, a)
        WorldId.PROFILE -> ProfileWorld(s, a)
        WorldId.BOTTLE -> BottleWorld(s, a)
        WorldId.NIGHT -> NightWorld(s, a)
    }
    }
}

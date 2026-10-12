package com.mwooten.chime.data

/**
 * The five companions. Same reminders, five very different ways of saying them.
 * Colors are ARGB longs so non-UI code (notifications) can use them too.
 */
enum class Companion(
    val title: String,
    val trait: String,
    /** Main body color. */
    val color: Long,
    /** Lighter highlight. */
    val light: Long,
    /** Darker shade / outline. */
    val dark: Long,
    /** What the notification and call screen say first. */
    val callout: String,
) {
    WREN("Wren", "Gentle", 0xFF7CC35A, 0xFFD9F2C8, 0xFF3F6A2A, "Hey, when you have a sec…"),
    FIZZ("Fizz", "Sarcastic", 0xFFB07AF0, 0xFFF0E2FF, 0xFF5A2AA0, "Oh good, you're here."),
    PIP("Pip", "Funny", 0xFFFF9A3A, 0xFFFFE2BC, 0xFFB8561A, "Knock knock!"),
    DIM("Dim", "Self-deprecating", 0xFF6F95D6, 0xFFE2ECFF, 0xFF33508A, "Um, sorry… psst."),
    BLAZE("Blaze", "Aggressive", 0xFFF05A3C, 0xFFFFE0D6, 0xFF8A1E10, "HEY. YOU. NOW.");

    /** A one-liner about a single quest, in this companion's voice. */
    fun line(q: Quest, now: Long = System.currentTimeMillis()): String {
        val t = q.text
        if (q.done) return when (this) {
            WREN -> "“$t” is done. That's one less thing to carry."
            FIZZ -> "You finished “$t”. I'm as surprised as you are."
            PIP -> "“$t”: DONE. Somebody cue the confetti cannon."
            DIM -> "You finished “$t”. I knew you would. I mean I hoped. Same thing."
            BLAZE -> "“$t” DONE. GOOD. NEXT."
        }
        val due = q.due
        if (due == null) {
            val since = Time.dayName(Time.date(q.created)).lowercase()
            return when (this) {
                WREN -> "“$t” has been waiting since $since. Maybe give it a day?"
                FIZZ -> "“$t” has no date. Bold. It'll just do itself, probably."
                PIP -> "“$t” is floating around with no date, like a balloon at a birthday party."
                DIM -> "Sorry to bring up “$t” again. It doesn't have a day yet. Only if you want."
                BLAZE -> "“$t” HAS NO DEADLINE. PICK ONE."
            }
        }
        val s = q.shelf(now)
        val time = if (q.allDay) Time.dayName(Time.date(due)).lowercase() else Time.time(due)
        val rel = Time.rel(due - now)
        if (s == Shelf.OVERDUE) return when (this) {
            WREN -> "“$t” slipped past $time. That's okay. Want to do it now?"
            FIZZ -> "“$t” was due $time. Time is a construct, but that one wasn't."
            PIP -> "“$t” is overdue! It's not late, it's fashionably late. Still go."
            DIM -> "I should have reminded you better. “$t” was due $time. Sorry."
            BLAZE -> "“$t” IS LATE. FIX IT. RIGHT NOW."
        }
        if (q.allDay) return when (this) {
            WREN -> "Today's the day for “$t”. No rush, just today."
            FIZZ -> "“$t” is due today. Today. As in the one we're in."
            PIP -> "“$t” today! It's like a birthday, but for paperwork."
            DIM -> "“$t” is today. You'll handle it better than I would."
            BLAZE -> "“$t”. TODAY. NO EXCUSES."
        }
        val by = if (q.kind == "by") "by" else "at"
        return when (this) {
            WREN -> "“$t” is due $by $time, about $rel from now. You've got this."
            FIZZ -> "“$t” is due $by $time. That's $rel. Which, fun fact, keeps getting shorter."
            PIP -> "“$t” $by $time! That's $rel. Plenty of time to panic gracefully."
            DIM -> "Sorry to bother you. “$t” is due $by $time, in $rel."
            BLAZE -> "“$t”. $by ${time.uppercase()}. $rel LEFT. MOVE."
        }
    }

    /** What to say when nothing is on the list. */
    val emptyLine: String
        get() = when (this) {
            WREN -> "Nothing on your plate. Tell me anything that's on your mind."
            FIZZ -> "Empty list. Either you're done or you're hiding things from me."
            PIP -> "Zero quests! I'm bored. Give me something to nag you about."
            DIM -> "Nothing to remind you of. I'll just… be here. If you need me."
            BLAZE -> "NO QUESTS?! FIND ONE. SPILL IT."
        }

    /** A gentle check-in about the whole list. */
    fun checkIn(all: List<Quest>, now: Long = System.currentTimeMillis()): Pair<String, Quest?> {
        val f = focusQuest(all, now) ?: return emptyLine to null
        return line(f, now) to f
    }

    companion object {
        fun byName(name: String?) = entries.firstOrNull { it.name == name } ?: WREN
    }
}

/** The single most urgent open quest, or null. Overdue first, then soonest, then oldest someday. */
fun focusQuest(all: List<Quest>, now: Long = System.currentTimeMillis()): Quest? {
    val open = all.filter { !it.done }
    return open.filter { it.due != null }.sortedWith(questOrder).firstOrNull()
        ?: open.minByOrNull { it.created }
}

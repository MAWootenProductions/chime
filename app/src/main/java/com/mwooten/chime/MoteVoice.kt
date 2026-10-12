package com.mwooten.chime

import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Shelf
import com.mwooten.chime.data.Time
import com.mwooten.chime.data.questOrder
import com.mwooten.chime.data.shelf

/** Everything Mote says. */
object MoteVoice {
    const val CALLOUT = "Psst! Over here!"

    fun forQuest(q: Quest, now: Long = System.currentTimeMillis()): String {
        if (q.done) return "“${q.text}” is already finished. Nice work."
        val due = q.due ?: return "Remember “${q.text}”? It's been waiting since ${Time.dayName(Time.date(q.created)).lowercase()}. Want to give it a day?"
        return when (q.shelf(now)) {
            Shelf.OVERDUE ->
                if (q.allDay) "“${q.text}” was meant for ${Time.dayName(Time.date(due)).lowercase()}. Shall we knock it out now?"
                else "“${q.text}” slipped past ${Time.time(due)}. Shall we knock it out now?"
            else ->
                if (q.allDay) "Today's the day for “${q.text}”."
                else "“${q.text}” is due ${if (q.kind == "by") "by" else "at"} ${Time.time(due)}. That's ${Time.rel(due - now)} from now."
        }
    }

    /** A gentle check-in: picks the most useful thing to mention. */
    fun gentle(all: List<Quest>, now: Long = System.currentTimeMillis()): Pair<String, Quest?> {
        val open = all.filter { !it.done }.sortedWith(questOrder)
        if (open.isEmpty()) return "Your satchel is empty. Spill anything that's on your mind and I'll keep watch over it." to null
        val over = open.filter { it.shelf(now) == Shelf.OVERDUE }
        if (over.size > 1) return "${over.size} quests are past their time. The oldest is “${over[0].text}”." to over[0]
        if (over.size == 1) return "“${over[0].text}” is still waiting. A small step now counts." to over[0]
        val today = open.filter { it.shelf(now) == Shelf.TODAY }
        if (today.isNotEmpty()) {
            val t = today[0]
            return (if (t.allDay) "Still on today's list: “${t.text}”."
            else "Next up today: “${t.text}”, ${if (t.kind == "by") "by" else "at"} ${Time.time(t.due!!)}. That's ${Time.rel(t.due - now)} away.") to t
        }
        val someday = open.filter { it.due == null }.sortedBy { it.created }
        val next = open.firstOrNull { it.due != null }
        if (someday.isNotEmpty() && (next == null || Math.random() < 0.5)) return forQuest(someday[0], now) to someday[0]
        return "Nothing pressing today. Coming up: “${next!!.text}”, ${Time.dueLabel(next.due, next.allDay, next.kind).lowercase()}." to next
    }
}

package com.mwooten.chime.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class Quest(
    val id: String,
    val text: String,
    val due: Long? = null,
    val allDay: Boolean = false,
    val kind: String = "at", // "at" = happens at a time, "by" = deadline
    val done: Boolean = false,
    val doneAt: Long? = null,
    val created: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("text", text)
        if (due != null) put("due", due)
        put("allDay", allDay); put("kind", kind); put("done", done)
        if (doneAt != null) put("doneAt", doneAt)
        put("created", created)
    }

    companion object {
        fun fromJson(o: JSONObject) = Quest(
            id = o.getString("id"),
            text = o.optString("text"),
            due = if (o.has("due") && !o.isNull("due")) o.getLong("due") else null,
            allDay = o.optBoolean("allDay"),
            kind = o.optString("kind", "at"),
            done = o.optBoolean("done"),
            doneAt = if (o.has("doneAt") && !o.isNull("doneAt")) o.getLong("doneAt") else null,
            created = o.optLong("created", System.currentTimeMillis()),
        )

        fun newId(): String = "q" + java.lang.Long.toString(System.currentTimeMillis(), 36) +
            (1..5).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
    }
}

enum class Shelf(val label: String, val blurb: String) {
    OVERDUE("Overdue", "Slipped past their time"),
    TODAY("Today", "Before the sun sets"),
    TOMORROW("Tomorrow", "Next sunrise"),
    WEEK("This Week", "The next seven days"),
    LATER("Later", "Further down the path"),
    SOMEDAY("Someday", "No date yet"),
    DONE("Finished", "Quests you closed"),
}

object Time {
    val zone: ZoneId get() = ZoneId.systemDefault()
    private val tf = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val wd = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
    private val md = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    fun ldt(ms: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), zone)
    fun date(ms: Long): LocalDate = ldt(ms).toLocalDate()
    fun ms(t: LocalDateTime): Long = t.atZone(zone).toInstant().toEpochMilli()
    fun ms(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()
    fun time(ms: Long): String = tf.format(ldt(ms))

    fun dayName(d: LocalDate, today: LocalDate = LocalDate.now(zone)): String {
        val diff = ChronoUnit.DAYS.between(today, d)
        return when {
            diff == 0L -> "Today"
            diff == 1L -> "Tomorrow"
            diff == -1L -> "Yesterday"
            diff in 2..6 -> wd.format(d)
            else -> md.format(d)
        }
    }

    fun dueLabel(due: Long?, allDay: Boolean, kind: String): String {
        if (due == null) return "Someday"
        val dn = dayName(date(due))
        if (allDay) return dn
        return dn + " · " + (if (kind == "by") "by " else "at ") + time(due)
    }

    fun rel(deltaMs: Long): String {
        val m = Math.round(Math.abs(deltaMs) / 60000.0)
        if (m < 1) return "moments"
        if (m < 60) return "$m minute" + if (m == 1L) "" else "s"
        val h = Math.round(m / 60.0)
        if (h < 24) return "$h hour" + if (h == 1L) "" else "s"
        val d = Math.round(h / 24.0)
        return "$d day" + if (d == 1L) "" else "s"
    }
}

fun Quest.label() = Time.dueLabel(due, allDay, kind)

fun Quest.shelf(now: Long = System.currentTimeMillis()): Shelf {
    if (done) return Shelf.DONE
    val d = due ?: return Shelf.SOMEDAY
    val end = if (allDay) Time.ms(Time.date(d).plusDays(1)) else d
    if (end <= now) return Shelf.OVERDUE
    val diff = ChronoUnit.DAYS.between(Time.date(now), Time.date(d))
    return when {
        diff <= 0 -> Shelf.TODAY
        diff == 1L -> Shelf.TOMORROW
        diff < 7 -> Shelf.WEEK
        else -> Shelf.LATER
    }
}

val questOrder = compareBy<Quest>({ it.due ?: Long.MAX_VALUE }, { it.allDay }, { it.created })

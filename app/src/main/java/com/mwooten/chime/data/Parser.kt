package com.mwooten.chime.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/** Turns "respond to jury duty before noon" into a quest with a due time. */
object Parser {
    data class Parsed(val title: String, val due: Long?, val allDay: Boolean, val kind: String)

    private val I = RegexOption.IGNORE_CASE
    private val MONTHS = listOf("january", "february", "march", "april", "may", "june", "july",
        "august", "september", "october", "november", "december")
    private val DAYS = mapOf(
        "sun" to DayOfWeek.SUNDAY, "mon" to DayOfWeek.MONDAY, "tue" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY, "thu" to DayOfWeek.THURSDAY, "fri" to DayOfWeek.FRIDAY,
        "sat" to DayOfWeek.SATURDAY,
    )

    /** Date/time words we'll forgive typos in. Months are left out: "match" is too close to "march". */
    private val VOCAB = listOf("today", "tonight", "tomorrow", "morning", "afternoon", "evening", "noon",
        "midnight", "weekend", "before", "until", "monday", "tuesday", "wednesday", "thursday", "friday",
        "saturday", "sunday", "minutes", "hours")
    private val SLANG = mapOf("2day" to "today", "2nite" to "tonight", "tonite" to "tonight", "2night" to "tonight",
        "2morrow" to "tomorrow", "2moro" to "tomorrow", "tmw" to "tomorrow", "tmrw" to "tomorrow", "tmr" to "tomorrow",
        "tomoz" to "tomorrow", "arvo" to "afternoon", "wknd" to "weekend", "b4" to "before", "til" to "until")
    /** Real words that sit one letter away from a date word and must stay as typed. */
    private val KEEP = setOf("neon", "noun", "nook", "none", "noone", "noor", "noob", "nono", "noona", "norn", "toady",
        "todd", "toddy", "todas", "hour", "hors", "horus", "minute", "minuses", "minuets", "minutos", "mourning",
        "moaning", "mooning", "mcknight", "sundae", "sundry", "sunway", "sunray", "util", "weekends", "afternoons",
        "tuesdays", "wednesdays", "thursdays", "fridays", "mondays", "sundays", "saturdays", "evenings", "mornings")

    /** Optimal string alignment distance (counts a swapped pair like "todya" as one slip). */
    private fun dist(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val c = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + c)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
        }
        return d[a.length][b.length]
    }

    /** "respond to jury letter tdoay before nooon" → "respond to jury letter today before noon". */
    fun forgive(raw: String): String = Regex("[A-Za-z0-9']+").replace(raw) { m ->
        val w = m.value; val lw = w.lowercase()
        SLANG[lw]?.let { return@replace it }
        if (lw.length < 4 || lw in VOCAB || lw in KEEP || !lw.all { it.isLetter() }) return@replace w
        val best = VOCAB.filter { it[0] == lw[0] }.map { it to dist(lw, it) }.minByOrNull { it.second } ?: return@replace w
        val allowed = if (best.first.length >= 8 && lw.length >= 7) 2 else 1
        if (best.second in 1..allowed) best.first else w
    }

    fun parse(raw0: String, nowMs: Long = System.currentTimeMillis()): Parsed {
        val raw = forgive(raw0)
        val now: LocalDateTime = Time.ldt(nowMs)
        val today: LocalDate = now.toLocalDate()
        var s = " " + raw.trim().replace(Regex("\\s+"), " ") + " "
        var date: LocalDate? = null
        var hour: Int? = null
        var min = 0
        var kind = "at"
        var exact: LocalDateTime? = null
        var tonight = false

        fun take(re: Regex, fn: (MatchResult) -> Boolean): Boolean {
            val m = re.find(s) ?: return false
            if (!fn(m)) return false
            s = s.replaceRange(m.range, " ")
            return true
        }
        fun deadline(word: String?) = word != null && Regex("before|by|until|till", I).containsMatchIn(word)

        // "in 2 hours", "in 30 min", "in 3 days"
        take(Regex("\\bin\\s+(\\d+|an?|half an?)\\s*(minutes?|mins?|m|hours?|hrs?|h|days?|weeks?)\\b", I)) { m ->
            val n = when {
                m.groupValues[1].lowercase().startsWith("half") -> 0.5
                m.groupValues[1].lowercase() in listOf("a", "an") -> 1.0
                else -> m.groupValues[1].toDouble()
            }
            val u = m.groupValues[2].lowercase()
            when {
                u.startsWith("m") -> exact = now.plusSeconds((n * 60).toLong())
                u.startsWith("h") -> exact = now.plusSeconds((n * 3600).toLong())
                u.startsWith("d") -> date = today.plusDays(n.toLong())
                else -> date = today.plusDays((7 * n).toLong())
            }
            true
        }

        val pre = "(?:\\b(before|by|at|around|until|till)\\s+|@\\s*)?"
        take(Regex(pre + "\\b(noon|midday)\\b", I)) { m -> hour = 12; kind = if (deadline(m.groups[1]?.value)) "by" else "at"; true }
        if (hour == null) take(Regex(pre + "\\bmidnight\\b", I)) { hour = 23; min = 59; kind = "by"; true }
        if (hour == null) take(Regex(pre + "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)(?=\\s|$|[.,!])", I)) { m ->
            var h = m.groupValues[2].toInt()
            if (h > 12) return@take false
            val pm = m.groupValues[4].contains("p", ignoreCase = true)
            h = if (h == 12) (if (pm) 12 else 0) else if (pm) h + 12 else h
            hour = h; min = m.groupValues[3].ifEmpty { "0" }.toInt()
            kind = if (deadline(m.groups[1]?.value)) "by" else "at"; true
        }
        if (hour == null) take(Regex("\\b(before|by|at|around|until|till)\\s+(\\d{1,2})(?::(\\d{2}))?(?=\\s|$|[.,!])", I)) { m ->
            var h = m.groupValues[2].toInt()
            if (h > 23) return@take false
            if (h <= 7) h += 12
            hour = h; min = m.groupValues[3].ifEmpty { "0" }.toInt()
            kind = if (deadline(m.groupValues[1])) "by" else "at"; true
        }
        if (hour == null) take(Regex("\\b(by\\s+)?(?:the\\s+)?(end of (?:the )?day|eod|end of business|cob|eob)\\b", I)) { hour = 17; kind = "by"; true }
        if (hour == null) take(Regex("\\btonight\\b", I)) { hour = 20; tonight = true; kind = "by"; true }
        for ((word, h) in listOf("morning" to 9, "afternoon" to 15, "evening" to 18)) {
            if (hour != null) break
            take(Regex("\\b(this |in the |tomorrow )?($word)\\b", I)) { m ->
                hour = h
                if (m.groupValues[1].trim().equals("tomorrow", true)) date = today.plusDays(1)
                true
            }
        }

        if (date == null) {
            val p = "(?:by\\s+|on\\s+|for\\s+)?"
            take(Regex("\\b(?:on\\s+)?(?:the\\s+)?day after tomorrow\\b", I)) { date = today.plusDays(2); true } ||
            take(Regex("\\b$p(today|tonight)\\b", I)) { date = today; true } ||
            take(Regex("\\b$p(tomorrow|tmrw|tmr|tomorow)\\b", I)) { date = today.plusDays(1); true } ||
            take(Regex("\\b$p(?:this\\s+)?weekend\\b", I)) {
                date = if (today.dayOfWeek == DayOfWeek.SATURDAY) today else today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)); true
            } ||
            take(Regex("\\b(?:by\\s+|for\\s+)?next week\\b", I)) { date = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)); true } ||
            take(Regex("\\b(?:by\\s+|on\\s+|for\\s+|this\\s+|next\\s+)?(sunday|monday|tuesday|wednesday|thursday|friday|saturday|mon|tues?|wed|thu|thurs?|fri)\\b", I)) { m ->
                val dow = DAYS[m.groupValues[1].lowercase().take(3)] ?: return@take false
                date = today.with(TemporalAdjusters.next(dow)); true
            } ||
            take(Regex("\\b$p(" + MONTHS.joinToString("|") { it.take(3) + "(?:" + it.drop(3) + ")?" } + ")\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b", I)) { m ->
                val mi = MONTHS.indexOfFirst { it.startsWith(m.groupValues[1].lowercase().take(3)) }
                try {
                    var d = LocalDate.of(today.year, mi + 1, m.groupValues[2].toInt())
                    if (d.isBefore(today)) d = d.plusYears(1)
                    date = d; true
                } catch (e: Exception) { false }
            } ||
            take(Regex("\\b$p(\\d{1,2})/(\\d{1,2})\\b", I)) { m ->
                try {
                    var d = LocalDate.of(today.year, m.groupValues[1].toInt(), m.groupValues[2].toInt())
                    if (d.isBefore(today)) d = d.plusYears(1)
                    date = d; true
                } catch (e: Exception) { false }
            }
        }

        var due: Long? = null
        var allDay = false
        val ex = exact
        val hr = hour
        val dt = date
        when {
            ex != null -> { due = Time.ms(ex); kind = "at" }
            hr != null -> {
                var t = (dt ?: today).atTime(hr, min)
                if (dt == null && t.isBefore(now) && !tonight) t = t.plusDays(1)
                due = Time.ms(t)
            }
            dt != null -> { due = Time.ms(dt); allDay = true; kind = "by" }
        }

        val tail = Regex("(?:\\s|^)(?:by|before|at|on|due|for|until|till|in|this|next|@)\\s*$", I)
        var title = s.replace(Regex("\\s+"), " ").trim()
        repeat(2) { title = title.replace(tail, "") }
        title = title.trim().trim(',', ';', '-', ' ')
        if (title.isNotEmpty()) title = title.substring(0, 1).uppercase() + title.substring(1)
        return Parsed(title, due, allDay, kind)
    }
}

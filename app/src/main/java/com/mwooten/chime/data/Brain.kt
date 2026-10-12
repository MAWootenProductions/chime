package com.mwooten.chime.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Turns whatever you type back to your companion into changes plus a reply in the companion's voice.
 *
 * With an Anthropic API key it asks Claude, which understands anything ("thanks, don't worry about
 * that one today", "push the dentist to thursday and add milk"). Without a key, or if the call fails,
 * a small set of rules handles the common cases.
 */
object Brain {
    const val MODEL = "claude-haiku-5-5"

    data class Result(val reply: String, val changed: Boolean, val added: List<String> = emptyList())

    /** Ids of quests added during the current [handle] call (calls run one at a time). */
    private val added = mutableListOf<String>()

    /** [aboutId] is the quest the conversation is about (the one the companion just mentioned), if any. */
    suspend fun handle(ctx: Context, text: String, aboutId: String? = null): Result = withContext(Dispatchers.IO) {
        QuestStore.load(ctx)
        val who = Settings.companion(ctx)
        val about = (aboutId?.let { QuestStore.get(ctx, it) }) ?: focusQuest(QuestStore.all(ctx))
        remember(ctx, "user", text)
        added.clear()
        val key = Settings.apiKey(ctx)
        val result = if (key.isNotBlank()) {
            try { askClaude(ctx, key, text, about) } catch (e: Exception) { offline(ctx, text, about, who) }
        } else offline(ctx, text, about, who)
        remember(ctx, "assistant", result.reply)
        result.copy(added = added.toList())
    }

    // ---------------------------------------------------------------- Claude

    private fun persona(c: Companion): String = when (c) {
        Companion.WREN -> "Wren: gentle, warm, unhurried, reassuring. Short and kind."
        Companion.FIZZ -> "Fizz: dry and sarcastic, teasing but secretly on the user's side. Never cruel."
        Companion.PIP -> "Pip: goofy and funny, loves a bad joke or silly image, high energy."
        Companion.DIM -> "Dim: self-deprecating and apologetic, a little anxious, sweet, always says the user is better at this than Dim is."
        Companion.BLAZE -> "Blaze: loud drill-sergeant energy, ALL CAPS bursts, pushy but never insulting or mean."
    }

    private val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private fun askClaude(ctx: Context, key: String, text: String, about: Quest?): Result {
        val who = Settings.companion(ctx)
        val nowZ = ZonedDateTime.now(Time.zone)
        val open = QuestStore.all(ctx).filter { !it.done }.sortedWith(questOrder).take(40)
        val list = JSONArray()
        open.forEach { q ->
            list.put(JSONObject().put("id", q.id).put("text", q.text)
                .put("due", q.due?.let { iso.format(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), Time.zone)) } ?: JSONObject.NULL)
                .put("all_day", q.allDay))
        }
        val system = """
            You are ${who.title}, the companion inside Chime, a personal to-do app. Personality: ${persona(who)}
            The user just typed a message to you. Decide what they want and call the `respond` tool exactly once.
            - If they're reacting to a reminder ("thanks", "not today", "did it", "remind me later", "push it to friday"), act on the quest being discussed.
              "Don't worry about it today" / "not today" means move it to tomorrow (keep its time if it had one, else all-day).
            - If they mention new things to do, add them (one add per task, keep the user's words, short).
            - They can do several things in one message.
            - If it's just chat, take no actions and reply in character.
            - Never invent quests they didn't mention. Never delete unless they clearly ask to drop/cancel it.
            Your reply: 1-2 short sentences, in character, plain text, no emoji, and say what you changed.
            Current time: ${iso.format(nowZ)} (${nowZ.dayOfWeek}).
            Quest being discussed: ${about?.let { "${it.id} \"${it.text}\"" } ?: "none"}.
            Open quests (JSON): $list
        """.trimIndent()

        val messages = JSONArray()
        history(ctx).dropLast(1).takeLast(8).forEach { messages.put(it) }
        messages.put(JSONObject().put("role", "user").put("content", text))
        // the API needs strictly alternating roles starting with user
        val clean = JSONArray()
        var expect = "user"
        for (i in 0 until messages.length()) {
            val m = messages.getJSONObject(i)
            if (m.getString("role") == expect) { clean.put(m); expect = if (expect == "user") "assistant" else "user" }
        }
        if (clean.length() == 0 || clean.getJSONObject(clean.length() - 1).getString("role") != "user") {
            clean.put(JSONObject().put("role", "user").put("content", text))
        }

        val tool = JSONObject().put("name", "respond")
            .put("description", "Apply changes to the user's quests and reply to them.")
            .put("input_schema", JSONObject(
                """
                {"type":"object","properties":{
                  "actions":{"type":"array","items":{"type":"object","properties":{
                    "type":{"type":"string","enum":["add","done","undone","snooze","move","rename","delete"]},
                    "id":{"type":"string","description":"quest id for anything but add"},
                    "text":{"type":"string","description":"task text for add, new name for rename"},
                    "due":{"type":"string","description":"ISO-8601 local date-time with offset, or a date YYYY-MM-DD for all-day; for add/move/snooze"},
                    "all_day":{"type":"boolean"}
                  },"required":["type"]}},
                  "reply":{"type":"string"}
                },"required":["actions","reply"]}
                """.trimIndent()))

        val body = JSONObject()
            .put("model", MODEL)
            .put("max_tokens", 600)
            .put("system", system)
            .put("messages", clean)
            .put("tools", JSONArray().put(tool))
            .put("tool_choice", JSONObject().put("type", "tool").put("name", "respond"))

        val conn = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 10_000; readTimeout = 25_000
            setRequestProperty("content-type", "application/json")
            setRequestProperty("x-api-key", key)
            setRequestProperty("anthropic-version", "2023-06-01")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = conn.responseCode
        val resp = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
        if (code !in 200..299) throw IllegalStateException("API $code: $resp")
        val content = JSONObject(resp).getJSONArray("content")
        var input: JSONObject? = null
        for (i in 0 until content.length()) {
            val b = content.getJSONObject(i)
            if (b.optString("type") == "tool_use") { input = b.getJSONObject("input"); break }
        }
        val inp = input ?: throw IllegalStateException("no tool call")
        val changed = apply(ctx, inp.optJSONArray("actions") ?: JSONArray())
        return Result(inp.optString("reply").ifBlank { who.line(about ?: return Result(who.emptyLine, changed)) }, changed)
    }

    private fun parseDue(s: String?): Pair<Long, Boolean>? {
        if (s.isNullOrBlank()) return null
        return try {
            if (s.length == 10) Time.ms(LocalDate.parse(s)) to true
            else try { OffsetDateTime.parse(s).toInstant().toEpochMilli() to false }
            catch (e: Exception) { Time.ms(LocalDateTime.parse(s)) to false }
        } catch (e: Exception) { null }
    }

    private fun apply(ctx: Context, actions: JSONArray): Boolean {
        var changed = false
        for (i in 0 until actions.length()) {
            val a = actions.getJSONObject(i)
            val q = a.optString("id").takeIf { it.isNotBlank() }?.let { QuestStore.get(ctx, it) }
            val due = parseDue(a.optString("due").takeIf { it.isNotBlank() })
            when (a.optString("type")) {
                "add" -> {
                    val t = a.optString("text").trim()
                    if (t.isNotEmpty()) {
                        val p = Parser.parse(t)
                        val d = due ?: p.due?.let { it to p.allDay }
                        val title = if (due != null) t.replaceFirstChar { it.uppercase() } else p.title.ifBlank { t }
                        val nq = Quest(Quest.newId(), title, d?.first, d?.second ?: false, if (d?.second == true) "by" else p.kind)
                        QuestStore.upsert(ctx, nq); added += nq.id
                        changed = true
                    }
                }
                "done" -> if (q != null) { QuestStore.setDone(ctx, q.id, true); changed = true }
                "undone" -> if (q != null) { QuestStore.setDone(ctx, q.id, false); changed = true }
                "snooze", "move" -> if (q != null && due != null) {
                    QuestStore.upsert(ctx, q.copy(due = due.first, allDay = a.optBoolean("all_day", due.second), done = false)); changed = true
                }
                "rename" -> if (q != null && a.optString("text").isNotBlank()) { QuestStore.upsert(ctx, q.copy(text = a.optString("text").trim())); changed = true }
                "delete" -> if (q != null) { QuestStore.delete(ctx, q.id); changed = true }
            }
        }
        return changed
    }

    // ---------------------------------------------------------------- offline rules

    private val I = RegexOption.IGNORE_CASE

    private fun offline(ctx: Context, raw: String, about: Quest?, who: Companion): Result {
        val t = raw.trim().lowercase()
        fun say(w: String, f: String, p: String, d: String, b: String) = when (who) {
            Companion.WREN -> w; Companion.FIZZ -> f; Companion.PIP -> p; Companion.DIM -> d; Companion.BLAZE -> b
        }
        val name = about?.text?.let { "“$it”" } ?: "that"

        if (about != null && Regex("^(done|did it|finished|sent it|all done|completed|it's done|its done|handled|got it done)\\b", I).containsMatchIn(t)) {
            QuestStore.setDone(ctx, about.id, true)
            return Result(say("Lovely. $name is done.", "$name, done. I'll alert the press.", "$name: DEMOLISHED. Victory lap!", "You did it! I knew you would. Sorry I doubted. I didn't doubt.", "$name DONE. THAT'S WHAT I'M TALKING ABOUT."), true)
        }
        // a fix for the thing we're talking about: "today at 3", "no, by noon", "make it friday", "call it jury letter"
        if (about != null && !Regex("(not today|don'?t worry|skip|another day|leave it|^(later|snooze|cancel|never ?mind|forget it|drop it|delete|done|did it|finished))", I).containsMatchIn(t)) {
            val lead = Regex("^(no,?|nope,?|actually,?|wait,?|i meant|i mean|make it|change it to|change to|should be|it's|its|it is|move it to|set it (for|to))\\s+", I)
            val body = raw.trim().replace(lead, "")
            val rn = Regex("^(?:rename(?: it)? to|call it|name it|title)\\s+(.+)$", I).find(body)
            if (rn != null) {
                val newName = rn.groupValues[1].trim().trimEnd('.', '!')
                val p = Parser.parse(newName)
                val nm = p.title.ifBlank { newName }.replaceFirstChar { it.uppercase() }
                QuestStore.upsert(ctx, if (p.due != null) about.copy(text = nm, due = p.due, allDay = p.allDay, kind = p.kind) else about.copy(text = nm))
                return Result(say("Renamed it “$nm”.", "“$nm” it is. Much better.", "New name: “$nm”! Fancy.", "Okay, “$nm”. Sorry I got it wrong.", "“$nm”. FIXED."), true)
            }
            val p = Parser.parse(body)
            val leftover = p.title.split(" ").count { it.isNotBlank() }
            if (p.due != null && (leftover <= 1 || body.length < raw.trim().length)) {
                QuestStore.upsert(ctx, about.copy(due = p.due, allDay = p.allDay, kind = p.kind, done = false))
                val lbl = Time.dueLabel(p.due, p.allDay, p.kind).lowercase()
                return Result(say("Fixed. $name is now $lbl.", "Fixed: $name, $lbl. Typos happen.", "$name now lives at $lbl!", "Sorry about that. $name is $lbl now.", "FIXED. $name. $lbl."), true)
            }
        }
        if (about != null && Regex("(not today|don'?t worry about (it|this|that)|tomorrow|skip (it|today)|another day|leave it)", I).containsMatchIn(t)) {
            val tomorrow = LocalDate.now(Time.zone).plusDays(1)
            val d = about.due
            val nd = if (d != null && !about.allDay) Time.ms(tomorrow.atTime(Time.ldt(d).toLocalTime())) else Time.ms(tomorrow)
            QuestStore.upsert(ctx, about.copy(due = nd, allDay = d == null || about.allDay, done = false))
            return Result(say("Of course. I moved $name to tomorrow. Rest easy.", "Fine. $name is tomorrow's problem now.", "$name has been teleported to tomorrow. Bye for now!", "Okay, tomorrow then. Sorry for bringing it up.", "FINE. TOMORROW. BUT TOMORROW I'M LOUDER."), true)
        }
        if (about != null && Regex("^(later|snooze|in a bit|not now|remind me|brb|give me)", I).containsMatchIn(t)) {
            val p = Parser.parse(raw)
            val at = p.due ?: (System.currentTimeMillis() + 30 * 60_000L)
            QuestStore.upsert(ctx, about.copy(due = at, allDay = p.allDay && p.due != null, kind = "at", done = false))
            val when_ = Time.dueLabel(at, p.allDay && p.due != null, "at").lowercase()
            return Result(say("Okay, I'll bring $name up again $when_.", "Snoozed $name to $when_. Again.", "$name is napping until $when_. Shh.", "Okay, $when_. Sorry. I'll be quieter.", "$when_. NOT ONE MINUTE LATER."), true)
        }
        if (about != null && Regex("^(cancel|never ?mind|forget it|drop it|delete)", I).containsMatchIn(t)) {
            QuestStore.delete(ctx, about.id)
            return Result(say("Done, I've let $name go.", "$name, deleted. Out of sight, out of mind.", "$name has left the building.", "Okay, it's gone. Hope that was right.", "$name: DELETED. MOVING ON."), true)
        }
        if (Regex("^(thanks|thank you|thx|ty|ok|okay|cool|got it|nice|love you|appreciate)", I).containsMatchIn(t) && t.length < 40) {
            return Result(say("Anytime. I'm right here.", "Sure. Don't make it weird.", "You're welcome! Tips accepted in snacks.", "Oh! Thank YOU. Really.", "DON'T THANK ME. DO THINGS."), false)
        }
        // anything else is a new quest
        val p = Parser.parse(raw)
        if (p.title.isBlank()) return Result(who.emptyLine, false)
        val nq = Quest(Quest.newId(), p.title, p.due, p.allDay, p.kind)
        QuestStore.upsert(ctx, nq); added += nq.id
        val lbl = Time.dueLabel(p.due, p.allDay, p.kind).lowercase()
        return Result(say("Got it: “${p.title}”, $lbl.", "“${p.title}”, $lbl. Added. You're welcome.", "“${p.title}”, $lbl! Into the pile it goes.", "Added “${p.title}”, $lbl. I'll try to remind you well.", "“${p.title}”. $lbl. LOCKED IN."), true)
    }

    // ---------------------------------------------------------------- short memory

    private fun history(ctx: Context): List<JSONObject> = try {
        val a = JSONArray(Settings.raw(ctx).getString("chat", "[]"))
        (0 until a.length()).map { a.getJSONObject(it) }
    } catch (e: Exception) { emptyList() }

    private fun remember(ctx: Context, role: String, text: String) {
        val h = history(ctx).takeLast(11) + JSONObject().put("role", role).put("content", text)
        val a = JSONArray(); h.forEach { a.put(it) }
        Settings.raw(ctx).edit().putString("chat", a.toString()).apply()
    }
}

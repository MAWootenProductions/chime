package com.mwooten.chime.data

import android.content.Context
import com.mwooten.chime.Scheduler
import com.mwooten.chime.worlds.Finish
import com.mwooten.chime.worlds.Look
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import java.io.File

/** Quests live in one small JSON file on the phone. */
object QuestStore {
    private val _quests = MutableStateFlow<List<Quest>>(emptyList())
    val quests: StateFlow<List<Quest>> = _quests
    @Volatile private var loaded = false

    private fun file(ctx: Context) = File(ctx.filesDir, "quests.json")

    @Synchronized
    fun load(ctx: Context) {
        if (loaded) return
        val f = file(ctx)
        _quests.value = try {
            if (f.exists()) {
                val a = JSONArray(f.readText())
                (0 until a.length()).map { Quest.fromJson(a.getJSONObject(it)) }
            } else emptyList()
        } catch (e: Exception) { emptyList() }
        loaded = true
    }

    fun all(ctx: Context): List<Quest> { load(ctx); return _quests.value }
    fun get(ctx: Context, id: String): Quest? = all(ctx).firstOrNull { it.id == id }

    @Synchronized
    private fun write(ctx: Context, list: List<Quest>) {
        _quests.value = list
        val a = JSONArray(); list.forEach { a.put(it.toJson()) }
        val f = file(ctx); val tmp = File(f.path + ".tmp")
        tmp.writeText(a.toString()); tmp.renameTo(f)
    }

    fun upsert(ctx: Context, q: Quest) {
        load(ctx)
        val list = _quests.value.filterNot { it.id == q.id } + q
        write(ctx, list)
        Settings.clearCalled(ctx, q.id)
        Scheduler.rescheduleAll(ctx)
    }

    fun delete(ctx: Context, id: String) {
        load(ctx)
        write(ctx, _quests.value.filterNot { it.id == id })
        Scheduler.rescheduleAll(ctx)
    }

    fun clearDone(ctx: Context): Int {
        load(ctx)
        val n = _quests.value.count { it.done }
        write(ctx, _quests.value.filterNot { it.done })
        Scheduler.rescheduleAll(ctx)
        return n
    }

    fun setDone(ctx: Context, id: String, done: Boolean) {
        val q = get(ctx, id) ?: return
        upsert(ctx, q.copy(done = done, doneAt = if (done) System.currentTimeMillis() else null))
    }

    fun snooze(ctx: Context, id: String, minutes: Int) {
        val q = get(ctx, id) ?: return
        upsert(ctx, q.copy(due = System.currentTimeMillis() + minutes * 60_000L, allDay = false, kind = "at", done = false))
    }
}

/** Small preferences: Mote's chattiness, lead time, and which reminders already rang. */
object Settings {
    private fun p(ctx: Context) = ctx.getSharedPreferences("chime", Context.MODE_PRIVATE)

    val chatterOptions = listOf("Often" to 20, "Sometimes" to 60, "Rarely" to 150, "Never" to 0)
    val leadOptions = listOf(0, 5, 10, 15, 30)

    fun chatter(ctx: Context): String = p(ctx).getString("chatter", "Sometimes")!!
    fun setChatter(ctx: Context, v: String) = p(ctx).edit().putString("chatter", v).apply()
    fun chatterMinutes(ctx: Context): Int = chatterOptions.firstOrNull { it.first == chatter(ctx) }?.second ?: 60

    fun lead(ctx: Context): Int = p(ctx).getInt("lead", 10)
    fun setLead(ctx: Context, v: Int) = p(ctx).edit().putInt("lead", v).apply()

    fun world(ctx: Context): String = p(ctx).getString("world", "SIMPLE")!!
    fun setWorld(ctx: Context, v: String) = p(ctx).edit().putString("world", v).apply()

    fun companion(ctx: Context): Companion = Companion.byName(p(ctx).getString("companion", "WREN"))
    fun setCompanion(ctx: Context, v: Companion) = p(ctx).edit().putString("companion", v.name).apply()

    /** What the Simple-mode companion looks like. Picking a look also puts on that look's default finish. */
    fun look(ctx: Context): Look = Look.byName(p(ctx).getString("look", Look.STAR.name))
    fun setLook(ctx: Context, v: Look) = p(ctx).edit().putString("look", v.name).putString("finish", v.defaultFinish.name).apply()

    /** The material the look is cast in. */
    fun finish(ctx: Context): Finish = Finish.byName(p(ctx).getString("finish", Finish.AMBER.name))
    fun setFinish(ctx: Context, v: Finish) = p(ctx).edit().putString("finish", v.name).apply()

    /** First-run setup (look, color, personality) has been finished. */
    fun setupDone(ctx: Context): Boolean = p(ctx).getBoolean("setup_done", false)
    fun setSetupDone(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("setup_done", v).apply()

    /** Anthropic API key for smart replies (kept in this app's private storage on the phone). */
    fun apiKey(ctx: Context): String = p(ctx).getString("api_key", "")!!
    fun setApiKey(ctx: Context, v: String) = p(ctx).edit().putString("api_key", v.trim()).apply()

    /** GitHub token for the project tracker. */
    fun githubToken(ctx: Context): String = p(ctx).getString("gh_token", "")!!
    fun setGithubToken(ctx: Context, v: String) = p(ctx).edit().putString("gh_token", v.trim()).apply()

    /** Repos the tracker ignores ("owner/name"). Everything else pushed in the last 30 days is tracked. */
    fun mutedRepos(ctx: Context): Set<String> = p(ctx).getStringSet("muted_repos", emptySet())!!.toSet()
    fun setMuted(ctx: Context, repo: String, muted: Boolean) {
        val s = mutedRepos(ctx).toMutableSet(); if (muted) s += repo else s -= repo
        p(ctx).edit().putStringSet("muted_repos", s).apply()
    }

    /** Raw prefs for small JSON blobs (chat history, tracker cursors). */
    fun raw(ctx: Context) = p(ctx)

    fun rings(ctx: Context): Boolean = p(ctx).getBoolean("rings", true)
    fun setRings(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("rings", v).apply()

    fun wasCalled(ctx: Context, key: String) = p(ctx).getBoolean("called_$key", false)
    fun markCalled(ctx: Context, key: String) = p(ctx).edit().putBoolean("called_$key", true).apply()
    fun clearCalled(ctx: Context, id: String) {
        val e = p(ctx).edit()
        p(ctx).all.keys.filter { it.startsWith("called_$id") }.forEach { e.remove(it) }
        e.apply()
    }

    fun scheduledKeys(ctx: Context): Set<String> = p(ctx).getStringSet("scheduled", emptySet())!!.toSet()
    fun setScheduledKeys(ctx: Context, s: Set<String>) = p(ctx).edit().putStringSet("scheduled", s).apply()
}

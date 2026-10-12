package com.mwooten.chime.data

import android.content.Context
import com.mwooten.chime.Scheduler
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

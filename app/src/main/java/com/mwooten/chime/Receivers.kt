package com.mwooten.chime

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import com.mwooten.chime.data.QuestStore
import com.mwooten.chime.data.Settings

class ChimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        QuestStore.load(this)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        QuestStore.load(ctx)
        val id = intent.getStringExtra(Scheduler.EXTRA_ID)
        val key = intent.getStringExtra(Scheduler.EXTRA_KEY)
        val q = id?.let { QuestStore.get(ctx, it) }
        when (intent.action) {
            Scheduler.ACTION_CALL -> {
                if (key != null) Settings.markCalled(ctx, key)
                if (q != null && !q.done) {
                    if (Settings.rings(ctx)) Notifier.ring(ctx, q)
                    else Notifier.nudge(ctx, MoteVoice.forQuest(ctx, q), q)
                }
                Scheduler.rescheduleAll(ctx)
            }
            Scheduler.ACTION_FOLLOWUP -> {
                if (key != null) Settings.markCalled(ctx, key)
                if (q != null && !q.done) Notifier.nudge(ctx, MoteVoice.forQuest(ctx, q), q)
            }
            Scheduler.ACTION_GENTLE -> {
                val (msg, quest) = MoteVoice.gentle(ctx, QuestStore.all(ctx))
                if (QuestStore.all(ctx).any { !it.done }) Notifier.nudge(ctx, msg, quest)
                Scheduler.scheduleGentle(ctx)
            }
            Scheduler.ACTION_TEST -> Notifier.ring(ctx, null)
            Scheduler.ACTION_POLL -> {
                val pending = goAsync()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try { com.mwooten.chime.projects.Tracker.poll(ctx) } catch (e: Exception) { }
                    Scheduler.schedulePoll(ctx)
                    pending.finish()
                }
            }
            Notifier.ACTION_DECLINE -> {
                Notifier.stopRinging(ctx)
                if (q != null && !q.done) QuestStore.snooze(ctx, q.id, 10)
            }
            Notifier.ACTION_DONE -> {
                if (id != null) QuestStore.setDone(ctx, id, true)
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(Notifier.NUDGE_NOTIF_ID)
            }
            Notifier.ACTION_SNOOZE -> {
                if (id != null) QuestStore.snooze(ctx, id, 30)
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(Notifier.NUDGE_NOTIF_ID)
            }
        }
    }
}

/** Handles "Reply" typed straight into a companion notification. */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val text = androidx.core.app.RemoteInput.getResultsFromIntent(intent)?.getCharSequence(Notifier.KEY_REPLY)?.toString()?.trim()
        if (text.isNullOrEmpty()) return
        val id = intent.getStringExtra(Scheduler.EXTRA_ID)
        val convo = mutableListOf<Pair<Boolean, String>>()
        try {
            val a = org.json.JSONArray(intent.getStringExtra(Notifier.EXTRA_CONVO) ?: "[]")
            for (i in 0 until a.length()) a.getJSONObject(i).let { convo += it.getBoolean("me") to it.getString("t") }
        } catch (e: Exception) { }
        convo += true to text
        QuestStore.load(ctx)
        // show the reply right away while the companion thinks
        Notifier.nudge(ctx, text, id?.let { QuestStore.get(ctx, it) }, convo + (false to "…"), silent = true)
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val r = try { com.mwooten.chime.data.Brain.handle(ctx, text, id) } catch (e: Exception) { com.mwooten.chime.data.Brain.Result("Sorry, I lost my train of thought. Try me again?", false) }
            val q = id?.let { QuestStore.get(ctx, it) }
            Notifier.nudge(ctx, r.reply, if (q?.done == false) q else null, convo + (false to r.reply), silent = true)
            pending.finish()
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        QuestStore.load(ctx)
        Scheduler.rescheduleAll(ctx)
        Scheduler.scheduleGentle(ctx)
        Scheduler.schedulePoll(ctx)
    }
}

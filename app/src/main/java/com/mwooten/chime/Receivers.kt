package com.mwooten.chime

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                    else Notifier.nudge(ctx, MoteVoice.forQuest(q), q)
                }
                Scheduler.rescheduleAll(ctx)
            }
            Scheduler.ACTION_FOLLOWUP -> {
                if (key != null) Settings.markCalled(ctx, key)
                if (q != null && !q.done) Notifier.nudge(ctx, MoteVoice.forQuest(q), q)
            }
            Scheduler.ACTION_GENTLE -> {
                val (msg, quest) = MoteVoice.gentle(QuestStore.all(ctx))
                if (QuestStore.all(ctx).any { !it.done }) Notifier.nudge(ctx, msg, quest)
                Scheduler.scheduleGentle(ctx)
            }
            Scheduler.ACTION_TEST -> Notifier.ring(ctx, null)
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

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        QuestStore.load(ctx)
        Scheduler.rescheduleAll(ctx)
        Scheduler.scheduleGentle(ctx)
    }
}

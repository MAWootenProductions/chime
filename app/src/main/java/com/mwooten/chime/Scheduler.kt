package com.mwooten.chime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mwooten.chime.data.QuestStore
import com.mwooten.chime.data.Settings
import com.mwooten.chime.data.Time
import java.time.LocalTime
import kotlin.random.Random

/**
 * Turns quests into alarms.
 *  - Timed quests ring as a call [lead] minutes before they're due.
 *  - All-day quests ring at 9:00 that morning.
 *  - Anything still open an hour after its time gets a follow-up nudge.
 *  - Between 9am and 9pm, Mote checks in on her own every so often.
 */
object Scheduler {
    const val ACTION_CALL = "com.mwooten.chime.CALL"
    const val ACTION_FOLLOWUP = "com.mwooten.chime.FOLLOWUP"
    const val ACTION_GENTLE = "com.mwooten.chime.GENTLE"
    const val ACTION_TEST = "com.mwooten.chime.TEST"
    const val ACTION_POLL = "com.mwooten.chime.POLL"
    const val EXTRA_ID = "id"
    const val EXTRA_KEY = "key"

    private fun am(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    private fun pi(ctx: Context, action: String, key: String, id: String?): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).setAction(action)
            .putExtra(EXTRA_KEY, key)
        if (id != null) i.putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(ctx, key.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am(ctx).canScheduleExactAlarms()

    private fun setExact(ctx: Context, at: Long, p: PendingIntent) {
        try {
            if (canExact(ctx)) am(ctx).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
            else am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        } catch (e: SecurityException) {
            am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        }
    }

    @Synchronized
    fun rescheduleAll(ctx: Context) {
        val now = System.currentTimeMillis()
        val lead = Settings.lead(ctx) * 60_000L
        val wanted = mutableSetOf<String>()
        for (q in QuestStore.all(ctx)) {
            if (q.done) continue
            val due = q.due ?: continue
            val ringAt = if (q.allDay) Time.ms(Time.date(due).atTime(9, 0)) else due - lead
            val callKey = "${q.id}:call:$due"
            if (!Settings.wasCalled(ctx, callKey)) {
                val at = when {
                    ringAt > now -> ringAt
                    !q.allDay && due > now + 30_000 -> due // added inside the lead window: ring at the time itself
                    else -> null
                }
                if (at != null) {
                    setExact(ctx, at, pi(ctx, ACTION_CALL, callKey, q.id)); wanted += "$ACTION_CALL|$callKey|${q.id}"
                }
            }
            val followAt = (if (q.allDay) Time.ms(Time.date(due).atTime(17, 0)) else due + 60 * 60_000L)
            val followKey = "${q.id}:follow:$due"
            if (followAt > now && !Settings.wasCalled(ctx, followKey)) {
                setExact(ctx, followAt, pi(ctx, ACTION_FOLLOWUP, followKey, q.id)); wanted += "$ACTION_FOLLOWUP|$followKey|${q.id}"
            }
        }
        // cancel alarms for quests that changed, finished or were deleted
        for (old in Settings.scheduledKeys(ctx) - wanted) {
            val parts = old.split("|")
            if (parts.size == 3) am(ctx).cancel(pi(ctx, parts[0], parts[1], parts[2]))
        }
        Settings.setScheduledKeys(ctx, wanted)
    }

    fun scheduleGentle(ctx: Context) {
        val p = pi(ctx, ACTION_GENTLE, "gentle", null)
        val minutes = Settings.chatterMinutes(ctx)
        if (minutes <= 0) { am(ctx).cancel(p); return }
        val jitter = 0.75 + Random.nextDouble() * 0.5
        var at = Time.ldt(System.currentTimeMillis() + (minutes * jitter * 60_000).toLong())
        val t = at.toLocalTime()
        if (t.isBefore(LocalTime.of(9, 0))) at = at.toLocalDate().atTime(9, Random.nextInt(0, 40))
        else if (t.isAfter(LocalTime.of(21, 0))) at = at.toLocalDate().plusDays(1).atTime(9, Random.nextInt(0, 40))
        am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Time.ms(at), p)
    }

    /** Check GitHub projects roughly every 10 minutes (Android may stretch this when the phone is idle). */
    fun schedulePoll(ctx: Context) {
        val p = pi(ctx, ACTION_POLL, "poll", null)
        if (Settings.githubToken(ctx).isBlank()) { am(ctx).cancel(p); return }
        am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 60_000L, p)
    }

    fun testCall(ctx: Context, seconds: Int = 5) {
        setExact(ctx, System.currentTimeMillis() + seconds * 1000L, pi(ctx, ACTION_TEST, "test", null))
    }
}

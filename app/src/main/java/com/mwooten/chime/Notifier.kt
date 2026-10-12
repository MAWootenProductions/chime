package com.mwooten.chime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Settings

object Notifier {
    const val CH_CALLS = "mote_calls_v1"
    const val CH_NUDGES = "mote_nudges_v1"
    const val CALL_NOTIF_ID = 4201
    const val NUDGE_NOTIF_ID = 4202

    const val ACTION_DECLINE = "com.mwooten.chime.DECLINE"
    const val ACTION_DONE = "com.mwooten.chime.DONE"
    const val ACTION_SNOOZE = "com.mwooten.chime.SNOOZE"

    const val EXTRA_MESSAGE = "mote_message"
    const val EXTRA_QUEST = "mote_quest"
    const val EXTRA_ANSWERED = "answered"

    private fun raw(ctx: Context, name: String): Uri =
        Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + ctx.packageName + "/raw/" + name)

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val calls = NotificationChannel(CH_CALLS, "Companion calls", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings like an incoming call when a quest is due"
            setSound(raw(ctx, "mote_call"), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 180, 300, 900)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val nudges = NotificationChannel(CH_NUDGES, "Companion nudges", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Gentle check-ins and follow-ups"
            setSound(raw(ctx, "mote_chime"), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 90, 120)
        }
        nm.createNotificationChannels(listOf(calls, nudges))
    }

    /** Mote's portrait for the call screen: a soft green-gold glow. */
    private fun moteBitmap(ctx: Context): Bitmap {
        val who = Settings.companion(ctx)
        val s = 192
        val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1F18.toInt() }
        c.drawCircle(s / 2f, s / 2f, s / 2f, bg)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s / 2f, s / 2f, s * 0.42f,
                intArrayOf(0xFFFFFFFF.toInt(), who.light.toInt(), who.color.toInt(), (who.color and 0x00FFFFFFL).toInt()),
                floatArrayOf(0f, 0.2f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawCircle(s / 2f, s / 2f, s * 0.42f, p)
        return b
    }

    private fun mote(ctx: Context): Person = Person.Builder()
        .setName(Settings.companion(ctx).title)
        .setIcon(IconCompat.createWithBitmap(moteBitmap(ctx)))
        .setImportant(true)
        .build()

    private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun callIntent(ctx: Context, questId: String?, answered: Boolean) =
        Intent(ctx, CallActivity::class.java)
            .putExtra(EXTRA_QUEST, questId)
            .putExtra(EXTRA_ANSWERED, answered)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

    private fun broadcast(ctx: Context, action: String, questId: String?, code: Int): PendingIntent =
        PendingIntent.getBroadcast(ctx, code,
            Intent(ctx, AlarmReceiver::class.java).setAction(action).putExtra(Scheduler.EXTRA_ID, questId), flags())

    /** Ring like an incoming call. */
    fun ring(ctx: Context, q: Quest?) {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        val id = q?.id
        val full = PendingIntent.getActivity(ctx, 10, callIntent(ctx, id, false), flags())
        val answer = PendingIntent.getActivity(ctx, 11, callIntent(ctx, id, true), flags())
        val decline = broadcast(ctx, ACTION_DECLINE, id, 12)
        val b = NotificationCompat.Builder(ctx, CH_CALLS)
            .setSmallIcon(R.drawable.ic_stat_mote)
            .setContentTitle(Settings.companion(ctx).title)
            .setContentText(MoteVoice.callout(ctx))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(Settings.companion(ctx).color.toInt())
            .setOngoing(true)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(full)
            .setFullScreenIntent(full, true)
        val n = try {
            b.setStyle(NotificationCompat.CallStyle.forIncomingCall(mote(ctx), decline, answer)).build()
        } catch (e: Exception) {
            b.setStyle(null)
                .addAction(0, "Later", decline)
                .addAction(0, "Answer", answer)
                .build()
        }
        n.flags = n.flags or Notification.FLAG_INSISTENT
        try { NotificationManagerCompat.from(ctx).notify(CALL_NOTIF_ID, n) } catch (e: SecurityException) { }
    }

    fun stopRinging(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(CALL_NOTIF_ID)

    /** A gentle "Psst! Over here!" — tap to hear what Mote has to say. */
    fun nudge(ctx: Context, message: String, q: Quest?) {
        val open = Intent(ctx, MainActivity::class.java)
            .putExtra(EXTRA_MESSAGE, message)
            .putExtra(EXTRA_QUEST, q?.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tap = PendingIntent.getActivity(ctx, 20, open, flags())
        val b = NotificationCompat.Builder(ctx, CH_NUDGES)
            .setSmallIcon(R.drawable.ic_stat_mote)
            .setContentTitle(Settings.companion(ctx).title)
            .setContentText(MoteVoice.callout(ctx))
            .setSubText("Tap to listen")
            .setColor(Settings.companion(ctx).color.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)
        if (q != null && !q.done) {
            b.addAction(0, "Done it", broadcast(ctx, ACTION_DONE, q.id, 21))
            b.addAction(0, "Snooze 30 min", broadcast(ctx, ACTION_SNOOZE, q.id, 22))
        }
        try { NotificationManagerCompat.from(ctx).notify(NUDGE_NOTIF_ID, b.build()) } catch (e: SecurityException) { }
    }

    fun canRingFullScreen(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
}

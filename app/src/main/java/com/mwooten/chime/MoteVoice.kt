package com.mwooten.chime

import android.content.Context
import com.mwooten.chime.data.Quest
import com.mwooten.chime.data.Settings

/** Whatever the chosen companion says, for code outside the worlds (notifications, calls). */
object MoteVoice {
    fun who(ctx: Context) = Settings.companion(ctx)
    fun callout(ctx: Context): String = who(ctx).callout
    fun forQuest(ctx: Context, q: Quest): String = who(ctx).line(q)
    fun gentle(ctx: Context, all: List<Quest>): Pair<String, Quest?> = who(ctx).checkIn(all)
}

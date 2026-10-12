package com.mwooten.chime.projects

import android.content.Context
import com.mwooten.chime.Notifier
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** One repo the tracker watches. */
data class Project(
    val repo: String,            // owner/name
    val name: String,
    val url: String,
    val pushedAt: Long,
    val lastText: String,        // most recent thing that happened, plain words
    val lastAt: Long,
    val lastUrl: String?,
    val branch: String?,
    val ci: String?,             // "passed" | "failed" | "running" | null
    val ciName: String?,
    val muted: Boolean,
)

/**
 * Watches your GitHub repos for what Claude Code sessions do there: new commits on a branch,
 * pull requests opened or merged, and builds (GitHub Actions) that pass or fail.
 * Each new thing becomes a notification in your companion's voice.
 */
object Tracker {
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status
    private val lock = Mutex()

    private const val API = "https://api.github.com"

    private class Resp(val code: Int, val body: String, val etag: String?)

    private fun get(token: String, path: String, etag: String? = null): Resp {
        val c = (URL(API + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Chime-Android")
            if (etag != null) setRequestProperty("If-None-Match", etag)
        }
        val code = c.responseCode
        val body = if (code == 304) "" else try {
            (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) { "" }
        return Resp(code, body, c.getHeaderField("ETag"))
    }

    private fun ts(s: String?): Long = try { if (s.isNullOrBlank()) 0L else Instant.parse(s).toEpochMilli() } catch (e: Exception) { 0L }

    private fun state(ctx: Context): JSONObject = try { JSONObject(Settings.raw(ctx).getString("tracker", "{}")!!) } catch (e: Exception) { JSONObject() }
    private fun save(ctx: Context, s: JSONObject) = Settings.raw(ctx).edit().putString("tracker", s.toString()).apply()

    /** Load whatever we knew last time so the screen isn't empty while the network call runs. */
    fun loadCached(ctx: Context) {
        if (_projects.value.isNotEmpty()) return
        val st = state(ctx)
        val muted = Settings.mutedRepos(ctx)
        val list = mutableListOf<Project>()
        val repos = st.optJSONObject("repos") ?: return
        repos.keys().forEach { k -> repos.optJSONObject(k)?.let { list += fromJson(k, it, k in muted) } }
        _projects.value = list.sortedByDescending { maxOf(it.lastAt, it.pushedAt) }
    }

    private fun fromJson(repo: String, o: JSONObject, muted: Boolean) = Project(
        repo, repo.substringAfter('/'), o.optString("url"), o.optLong("pushedAt"), o.optString("lastText"), o.optLong("lastAt"),
        o.optString("lastUrl").ifBlank { null }, o.optString("branch").ifBlank { null },
        o.optString("ci").ifBlank { null }, o.optString("ciName").ifBlank { null }, muted)

    /**
     * Check every recently active repo once. Returns how many new things were found.
     * [notify] false is used for the first look, so you aren't flooded with old news.
     */
    suspend fun poll(ctx: Context, notify: Boolean = true): Int = lock.withLock {
        withContext(Dispatchers.IO) {
            val token = Settings.githubToken(ctx)
            if (token.isBlank()) { _status.value = "Add a GitHub token to start tracking."; return@withContext 0 }
            val who = Settings.companion(ctx)
            val st = state(ctx)
            val repos = st.optJSONObject("repos") ?: JSONObject()
            val firstEver = !st.optBoolean("seeded")
            val muted = Settings.mutedRepos(ctx)
            var found = 0

            val r = try { get(token, "/user/repos?sort=pushed&per_page=40&affiliation=owner,collaborator,organization_member") } catch (e: Exception) {
                _status.value = "Couldn't reach GitHub. Will try again soon."; return@withContext 0
            }
            if (r.code == 401) { _status.value = "GitHub didn't accept that token. Check it in Options."; return@withContext 0 }
            if (r.code !in 200..299) { _status.value = "GitHub said ${r.code}. Will try again soon."; return@withContext 0 }

            val cutoff = System.currentTimeMillis() - 30L * 24 * 3600_000
            val arr = JSONArray(r.body)
            for (i in 0 until arr.length()) {
                val ro = arr.getJSONObject(i)
                val full = ro.getString("full_name")
                val pushedAt = ts(ro.optString("pushed_at"))
                if (pushedAt < cutoff) continue
                val o = repos.optJSONObject(full) ?: JSONObject()
                o.put("url", ro.optString("html_url")).put("pushedAt", pushedAt)
                val quiet = full in muted
                val isNewRepo = !repos.has(full)

                // ---- events: pushes, pull requests, new branches
                try {
                    val ev = get(token, "/repos/$full/events?per_page=20", o.optString("etag").ifBlank { null })
                    if (ev.code in 200..299) {
                        ev.etag?.let { o.put("etag", it) }
                        val events = JSONArray(ev.body)
                        val lastSeen = o.optString("lastEvent", "0").toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
                        var maxSeen = lastSeen
                        val fresh = mutableListOf<JSONObject>()
                        for (j in 0 until events.length()) {
                            val e = events.getJSONObject(j)
                            val id = e.optString("id").toBigIntegerOrNull() ?: continue
                            if (id > maxSeen) maxSeen = id
                            if (id > lastSeen) fresh += e
                        }
                        o.put("lastEvent", maxSeen.toString())
                        // oldest first so notifications read in order; summarize the newest
                        fresh.reversed().forEach { e ->
                            val d = describe(full, e) ?: return@forEach
                            o.put("lastText", d.text).put("lastAt", ts(e.optString("created_at"))).put("lastUrl", d.url ?: "")
                            d.branch?.let { o.put("branch", it) }
                            if (notify && !firstEver && !isNewRepo && !quiet) {
                                Notifier.project(ctx, (full + e.optString("id")).hashCode(), "${full.substringAfter('/')}: ${d.title}", voice(who, d.kind, full.substringAfter('/'), d.text), d.url)
                                found++
                            }
                        }
                    }
                } catch (e: Exception) { }

                // ---- builds
                try {
                    val runs = get(token, "/repos/$full/actions/runs?per_page=3")
                    if (runs.code in 200..299) {
                        val list = JSONObject(runs.body).optJSONArray("workflow_runs") ?: JSONArray()
                        if (list.length() > 0) {
                            val run = list.getJSONObject(0)
                            val status = run.optString("status")
                            val concl = run.optString("conclusion")
                            val ci = when {
                                status != "completed" -> "running"
                                concl == "success" -> "passed"
                                concl == "failure" || concl == "timed_out" -> "failed"
                                else -> concl.ifBlank { null }
                            }
                            val runKey = run.optString("id") + ":" + (ci ?: "")
                            o.put("ci", ci ?: "").put("ciName", run.optString("name") + " · " + run.optString("head_branch"))
                            if (o.optString("lastRun") != runKey) {
                                o.put("lastRun", runKey)
                                if ((ci == "passed" || ci == "failed") && notify && !firstEver && !isNewRepo && !quiet) {
                                    val kind = if (ci == "passed") Kind.CI_PASS else Kind.CI_FAIL
                                    val text = "${run.optString("name")} on ${run.optString("head_branch")} ${if (ci == "passed") "passed" else "failed"}."
                                    Notifier.project(ctx, runKey.hashCode(), "${full.substringAfter('/')}: build $ci", voice(who, kind, full.substringAfter('/'), text), run.optString("html_url"))
                                    found++
                                }
                            }
                        }
                    }
                } catch (e: Exception) { }

                repos.put(full, o)
            }
            st.put("repos", repos).put("seeded", true)
            save(ctx, st)
            val list = mutableListOf<Project>()
            repos.keys().forEach { k -> repos.optJSONObject(k)?.let { if (it.optLong("pushedAt") >= cutoff) list += fromJson(k, it, k in muted) } }
            _projects.value = list.sortedByDescending { maxOf(it.lastAt, it.pushedAt) }
            _status.value = "Checked " + java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(java.time.LocalTime.now())
            found
        }
    }

    enum class Kind { PUSH, PR_OPEN, PR_MERGED, PR_CLOSED, BRANCH, CI_PASS, CI_FAIL }
    private class Desc(val kind: Kind, val title: String, val text: String, val url: String?, val branch: String?)

    private fun describe(full: String, e: JSONObject): Desc? {
        val p = e.optJSONObject("payload") ?: JSONObject()
        val base = "https://github.com/$full"
        return when (e.optString("type")) {
            "PushEvent" -> {
                val branch = p.optString("ref").removePrefix("refs/heads/")
                val commits = p.optJSONArray("commits")
                val n = commits?.length() ?: p.optInt("size", 0)
                val msg = commits?.takeIf { it.length() > 0 }?.let { it.getJSONObject(it.length() - 1) }?.optString("message")?.lineSequence()?.firstOrNull()
                val text = (if (n > 0) "$n new commit${if (n == 1) "" else "s"} on $branch" else "New work pushed to $branch") + (msg?.let { ": “$it”" } ?: "")
                Desc(Kind.PUSH, "new commits", text, "$base/commits/$branch", branch)
            }
            "PullRequestEvent" -> {
                val pr = p.optJSONObject("pull_request") ?: JSONObject()
                val title = pr.optString("title").ifBlank { "#" + p.optInt("number") }
                val url = pr.optString("html_url").ifBlank { "$base/pulls" }
                when (p.optString("action")) {
                    "opened", "reopened" -> Desc(Kind.PR_OPEN, "pull request ready", "Pull request opened: “$title”", url, pr.optJSONObject("head")?.optString("ref"))
                    "closed" -> if (pr.optBoolean("merged")) Desc(Kind.PR_MERGED, "merged", "Merged: “$title”", url, null)
                                else Desc(Kind.PR_CLOSED, "pull request closed", "Closed without merging: “$title”", url, null)
                    else -> null
                }
            }
            "CreateEvent" -> if (p.optString("ref_type") == "branch")
                Desc(Kind.BRANCH, "new branch", "Started a new branch: ${p.optString("ref")}", "$base/tree/${p.optString("ref")}", p.optString("ref")) else null
            else -> null
        }
    }

    /** The update, said by your companion. */
    fun voice(who: Companion, kind: Kind, repo: String, text: String): String = when (kind) {
        Kind.CI_PASS -> when (who) {
            Companion.WREN -> "Good news from $repo. $text"
            Companion.FIZZ -> "$repo's build passed. I'm as surprised as you are. $text"
            Companion.PIP -> "Green lights on $repo! Somebody do a little dance. $text"
            Companion.DIM -> "$repo passed its checks. Better than I'd have done. $text"
            Companion.BLAZE -> "$repo BUILD PASSED. GO REVIEW IT. $text"
        }
        Kind.CI_FAIL -> when (who) {
            Companion.WREN -> "$repo's build didn't pass. Worth a look when you can. $text"
            Companion.FIZZ -> "$repo broke. Shocking, I know. $text"
            Companion.PIP -> "Uh oh, $repo tripped over its shoelaces. $text"
            Companion.DIM -> "Sorry to tell you, $repo's build failed. $text"
            Companion.BLAZE -> "$repo FAILED. GET IN THERE. $text"
        }
        Kind.PR_OPEN -> when (who) {
            Companion.WREN -> "$repo has something ready for you to review. $text"
            Companion.FIZZ -> "$repo would like your attention, apparently. $text"
            Companion.PIP -> "Ding ding! $repo has a fresh pull request out of the oven. $text"
            Companion.DIM -> "$repo has a pull request, if you have a minute. $text"
            Companion.BLAZE -> "$repo PR IS UP. REVIEW IT. $text"
        }
        else -> when (who) {
            Companion.WREN -> "$repo is moving along. $text"
            Companion.FIZZ -> "$repo did a thing. $text"
            Companion.PIP -> "$repo update, hot off the press! $text"
            Companion.DIM -> "Small update on $repo. $text"
            Companion.BLAZE -> "$repo UPDATE. $text"
        }
    }
}

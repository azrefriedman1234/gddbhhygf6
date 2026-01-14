package com.pasiflonet.mobile.ui

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.x.XFeedAdapter
import com.pasiflonet.mobile.x.XFeedRepository
import java.util.concurrent.Executors
import kotlin.math.abs

object DashboardWidgets {

    private const val PREF = "dashboard_prefs"
    private const val KEY_URLS = "x_rss_urls"
    private const val KEY_BASELINE_MS = "baseline_ms"

    // ברירת מחדל: RSS חינמי (אפשר להחליף לכל RSS שאתה רוצה)
    private val DEFAULT_URLS = listOf(
        "https://news.google.com/rss/search?q=US%20destroyer%20Red%20Sea%20missile%20Middle%20East&hl=en-US&gl=US&ceid=US:en",
        "https://news.google.com/rss/search?q=Iran%20missile%20launch%20Middle%20East&hl=en-US&gl=US&ceid=US:en"
    )

    private val ui = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()

    private var started = false

    fun attach(activity: Activity) {
        if (started) return
        started = true

        // baseline = מהרגע שפתחת את האפליקציה (רק LIVE חדש)
        activity.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_BASELINE_MS, System.currentTimeMillis()).apply()

        val rv = activity.findViewById<RecyclerView?>(R.id.rvX)
        rv?.layoutManager = LinearLayoutManager(activity)
        val adapter = XFeedAdapter()
        rv?.adapter = adapter

        val radar = activity.findViewById<RadarView?>(R.id.radarView)

        fun urls(ctx: Context): List<String> {
            val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val raw = sp.getString(KEY_URLS, null)?.trim().orEmpty()
            if (raw.isEmpty()) return DEFAULT_URLS
            return raw.split("\n", ",", " ").map { it.trim() }.filter { it.startsWith("http") }.ifEmpty { DEFAULT_URLS }
        }

        fun baseline(ctx: Context): Long =
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_BASELINE_MS, 0L)

        fun tick() {
            io.execute {
                val items = XFeedRepository.fetchAll(urls(activity))
                val base = baseline(activity)
                val live = items.filter { it.publishedAtMs == 0L || it.publishedAtMs >= base }
                ui.post {
                    adapter.submit(live)

                    // הופך כותרות לבליפים ברדאר (ויזואלי)
                    val blips = live.take(18).mapIndexed { idx, it ->
                        val h = abs(it.title.hashCode())
                        val ang = (h % 360).toFloat()
                        val rad = 0.18f + ((h % 70) / 100f)
                        RadarBlip(ang, rad, it.title)
                    }
                    radar?.setBlips(blips)
                }
            }
            ui.postDelayed({ tick() }, 20000L)
        }

        tick()
    }
}

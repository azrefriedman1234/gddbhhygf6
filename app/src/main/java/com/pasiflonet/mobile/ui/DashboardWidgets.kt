package com.pasiflonet.mobile.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.rss.RssFetch
import com.pasiflonet.mobile.rss.RssSourceStore
import com.pasiflonet.mobile.x.XFeedAdapter
import com.pasiflonet.mobile.x.XItem
import java.util.concurrent.Executors

object DashboardWidgets {

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun attach(activity: Activity) {
        // לפתוח RSS Sources בלחיצה ארוכה על הגדרות (אם קיים)
        try {
            val btnSettings = activity.findViewById<View?>(R.id.btnSettings)
            btnSettings?.setOnLongClickListener {
                activity.startActivity(Intent(activity, RssSourcesActivity::class.java))
                true
            }
        } catch (_: Throwable) {}

        val rvX = activity.findViewById<RecyclerView?>(R.id.rvX) ?: return
        rvX.layoutManager = LinearLayoutManager(activity)
        val adapter = XFeedAdapter { item ->
            val url = item.link
            if (!url.isNullOrBlank()) {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Throwable) {
                    Toast.makeText(activity, url, Toast.LENGTH_SHORT).show()
                }
            }
        }
        rvX.adapter = adapter

        fun refresh() {
            io.execute {
                val sources = RssSourceStore.load(activity)
                val combined = ArrayList<XItem>()

                for (line in sources) {
                    val url = RssFetch.toUrl(line) ?: continue
                    val items = RssFetch.fetch(url, maxItems = 25)
                    for (it in items) {
                        combined.add(
                            XItem(
                                title = it.title,
                                description = it.summary,
                                link = it.link,
                                published = it.published
                            )
                        )
                    }
                }

                // dedupe by link/title
                val seen = HashSet<String>()
                val cleaned = combined.filter {
                    val k = (it.link ?: it.title).trim()
                    if (k.isEmpty()) false else seen.add(k)
                }.take(60)

                main.post { adapter.submitList(cleaned) }
            }
        }

        refresh()

        // רענון כל 60 שניות (אפשר לשנות)
        main.postDelayed(object : Runnable {
            override fun run() {
                refresh()
                main.postDelayed(this, 60_000)
            }
        }, 60_000)
    }
}

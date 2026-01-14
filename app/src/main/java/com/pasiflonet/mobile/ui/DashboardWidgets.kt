package com.pasiflonet.mobile.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.intel.TranslateHelper
import com.pasiflonet.mobile.x.XFeedAdapter
import com.pasiflonet.mobile.x.XFeedFetcher
import com.pasiflonet.mobile.x.XItem
import java.util.concurrent.Executors

object DashboardWidgets {
    private val exec = Executors.newSingleThreadExecutor()

    fun attach(activity: Activity) {
        val rvX = activity.findViewById<RecyclerView?>(R.id.rvX) ?: return
        activity.findViewById<RadarView?>(R.id.radarView) ?: return

        rvX.layoutManager = LinearLayoutManager(activity)
        val adapter = XFeedAdapter { item ->
            val link = item.link ?: return@XFeedAdapter
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            } catch (_: Throwable) {}
        }
        rvX.adapter = adapter

        val prefs = activity.getSharedPreferences("dashboard", Context.MODE_PRIVATE)
        val query = prefs.getString(
            "x_query",
            "destroyer OR missile OR strike Middle East"
        ) ?: "destroyer OR missile OR strike Middle East"

        var lastSeen = prefs.getLong("x_last_seen", 0L)

        val main = Handler(Looper.getMainLooper())

        fun tick() {
            exec.execute {
                val items = try {
                    XFeedFetcher.fetch(query, maxItems = 25)
                } catch (_: Throwable) {
                    emptyList()
                }

                // רק חדשים מאז הפעם הקודמת
                val filtered = if (lastSeen > 0) items.filter { it.publishedAtMillis > lastSeen } else items
                val maxTs = (filtered.maxOfOrNull { it.publishedAtMillis } ?: lastSeen)

                main.post {
                    if (filtered.isNotEmpty()) {
                        adapter.submit(filtered)
                        if (maxTs > lastSeen) {
                            lastSeen = maxTs
                            prefs.edit().putLong("x_last_seen", lastSeen).apply()
                        }

                        // תרגום רק למה שמוצג (best-effort, לא חוסם UI)
                        filtered.forEachIndexed { idx, x ->
                            if (x.heText.isNullOrBlank() && x.text.isNotBlank()) {
                                TranslateHelper.enToHe(
                                    x.text,
                                    onOk = { he ->
                                        val updated = x.copy(heText = he)
                                        adapter.updateItem(idx, updated)
                                    },
                                    onErr = { /* ignore */ }
                                )
                            }
                        }
                    }
                }
            }

            main.postDelayed({ tick() }, 30000) // כל 30 שניות
        }

        tick()
    }
}

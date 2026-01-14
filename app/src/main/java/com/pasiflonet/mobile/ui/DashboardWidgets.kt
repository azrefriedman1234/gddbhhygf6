package com.pasiflonet.mobile.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.intel.OpenIntelRepository

object DashboardWidgets {

    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    fun attach(activity: Activity) {
        if (started) return
        started = true

        val rvX = activity.findViewById<RecyclerView?>(R.id.rvX) ?: return
        val adapter = IntelFeedAdapter()

        rvX.layoutManager = LinearLayoutManager(activity)
        rvX.adapter = adapter

        val tick = object : Runnable {
            override fun run() {
                try {
                    val items = OpenIntelRepository.loadNow()
                    adapter.submit(items)
                } catch (_: Exception) { }
                handler.postDelayed(this, 30_000) // refresh every 30s
            }
        }
        handler.post(tick)
    }
}

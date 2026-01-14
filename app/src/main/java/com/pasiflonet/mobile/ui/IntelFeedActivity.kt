package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.intel.OpenIntelRepository

class IntelFeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rv = RecyclerView(this)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = IntelFeedAdapter()
        rv.adapter = adapter
        setContentView(rv)

        adapter.submit(OpenIntelRepository.loadNow())
    }
}

package com.pasiflonet.mobile.ui

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.intel.IntelItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntelFeedAdapter(
    private val items: List<IntelItem>,
    private val onClick: (IntelItem) -> Unit
) : RecyclerView.Adapter<IntelFeedAdapter.VH>() {

    private val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    class VH(val root: View, val title: TextView, val titleHe: TextView, val meta: TextView) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
        }
        val t1 = TextView(ctx).apply { textSize = 16f }
        val t2 = TextView(ctx).apply { textSize = 14f }
        val meta = TextView(ctx).apply { textSize = 12f }
        root.addView(t1)
        root.addView(t2)
        root.addView(meta)
        return VH(root, t1, t2, meta)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val it = items[pos]
        h.title.text = it.title
        h.titleHe.text = it.titleHe ?: ""
        h.meta.text = "${it.source} • ${if (it.pubMillis > 0) fmt.format(Date(it.pubMillis)) else ""}"
        h.root.setOnClickListener { onClick(it) }
    }
}

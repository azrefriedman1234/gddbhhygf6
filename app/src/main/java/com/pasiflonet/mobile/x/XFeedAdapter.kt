package com.pasiflonet.mobile.x

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.ui.TranslationBridge

class XFeedAdapter : RecyclerView.Adapter<XFeedAdapter.VH>() {

    private var items: List<XFeedItem> = emptyList()

    fun submit(list: List<XFeedItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_intel_row, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val it = items[pos]
        h.title.text = it.title
        h.translation.text = "תרגום: ..."
        TranslationBridge.translateToHebrew(it.title) { he ->
            h.translation.post { h.translation.text = if (he.isBlank() || he == it.title) "" else "תרגום: $he" }
        }

        h.itemView.setOnClickListener {
            try {
                val ctx = h.itemView.context
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.link)))
            } catch (_: Exception) {}
        }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvTitle)
        val translation: TextView = v.findViewById(R.id.tvTranslation)
    }
}

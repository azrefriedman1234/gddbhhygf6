package com.pasiflonet.mobile.x

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R

class XFeedAdapter(
    private val onClick: ((XItem) -> Unit)? = null
) : RecyclerView.Adapter<XFeedAdapter.VH>() {

    private val items = ArrayList<XItem>()

    fun submit(list: List<XItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_x_feed, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.title.text = it.title
        holder.meta.text = it.published ?: ""
        holder.desc.text = Html.fromHtml(it.description ?: "", Html.FROM_HTML_MODE_LEGACY).toString().trim()
        holder.itemView.setOnClickListener { onClick?.invoke(it) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvXTitle)
        val meta: TextView = v.findViewById(R.id.tvXMeta)
        val desc: TextView = v.findViewById(R.id.tvXDesc)
    }
}

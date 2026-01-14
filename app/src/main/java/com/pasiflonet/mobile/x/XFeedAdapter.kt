package com.pasiflonet.mobile.x

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R

class XFeedAdapter(
    private val onClick: (XItem) -> Unit
) : RecyclerView.Adapter<XFeedAdapter.VH>() {

    private val items = ArrayList<XItem>()

    fun submit(newItems: List<XItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateItem(index: Int, item: XItem) {
        if (index < 0 || index >= items.size) return
        items[index] = item
        notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_x, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.tvTitle.text = it.title
        holder.tvText.text = it.text
        holder.tvHe.text = it.heText ?: ""
        holder.tvTime.text =
            if (it.publishedAtMillis > 0) DateUtils.getRelativeTimeSpanString(it.publishedAtMillis) else ""

        holder.itemView.setOnClickListener { onClick(it) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvXTitle)
        val tvText: TextView = v.findViewById(R.id.tvXText)
        val tvHe: TextView = v.findViewById(R.id.tvXHe)
        val tvTime: TextView = v.findViewById(R.id.tvXTime)
    }
}

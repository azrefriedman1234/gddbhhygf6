package com.pasiflonet.mobile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.intel.IntelStoryItem

class IntelFeedAdapter(
    private val onClick: ((IntelStoryItem) -> Unit)? = null
) : RecyclerView.Adapter<IntelFeedAdapter.VH>() {

    private val items = ArrayList<IntelStoryItem>()

    fun submit(list: List<IntelStoryItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.t1.text = it.title
        holder.t2.text = it.source + (if (!it.summary.isNullOrBlank()) " • ${it.summary}" else "")
        holder.itemView.setOnClickListener { onClick?.invoke(it) }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val t1: TextView = v.findViewById(android.R.id.text1)
        val t2: TextView = v.findViewById(android.R.id.text2)
    }
}

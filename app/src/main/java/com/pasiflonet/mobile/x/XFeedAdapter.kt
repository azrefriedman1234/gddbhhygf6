package com.pasiflonet.mobile.x

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class XFeedAdapter(private val onClick: ((XItem) -> Unit)? = null) :
    RecyclerView.Adapter<XFeedAdapter.VH>() {

    private val items = ArrayList<XItem>()

    fun submitList(newItems: List<XItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title ?: ""
        holder.sub.text = item.published ?: (item.description ?: "")
        holder.itemView.setOnClickListener { onClick?.invoke(item) } // ✅ לא מעביר View
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(android.R.id.text1)
        val sub: TextView = itemView.findViewById(android.R.id.text2)
    }
}

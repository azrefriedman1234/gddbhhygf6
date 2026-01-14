package com.pasiflonet.mobile.ui

import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IntelFeedAdapter(
    private val onClick: ((IntelItem) -> Unit)? = null
) : RecyclerView.Adapter<IntelFeedAdapter.VH>() {

    private val items = ArrayList<IntelItem>()

    fun setItems(newItems: List<IntelItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val root = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val tvTitle = TextView(parent.context).apply {
            textSize = 15f
        }
        val tvHe = TextView(parent.context).apply {
            textSize = 13f
            alpha = 0.85f
        }
        val tvSrc = TextView(parent.context).apply {
            textSize = 11f
            alpha = 0.7f
            gravity = Gravity.END
        }
        root.addView(tvTitle)
        root.addView(tvHe)
        root.addView(tvSrc)
        return VH(root, tvTitle, tvHe, tvSrc)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.tvTitle.text = it.title
        holder.tvHe.text = it.titleHe ?: ""
        holder.tvSrc.text = it.source
        holder.itemView.setOnClickListener { onClick?.invoke(it) }
    }

    class VH(
        root: LinearLayout,
        val tvTitle: TextView,
        val tvHe: TextView,
        val tvSrc: TextView
    ) : RecyclerView.ViewHolder(root)
}

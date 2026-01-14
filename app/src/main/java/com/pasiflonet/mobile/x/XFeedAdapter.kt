package com.pasiflonet.mobile.x

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class XFeedAdapter(
    private val onOpen: ((String) -> Unit)? = null
) : RecyclerView.Adapter<XFeedAdapter.Holder>() {

    private val items = mutableListOf<Any>()

    fun submitList(newItems: List<*>?) = setItems(newItems)
    fun updateList(newItems: List<*>?) = setItems(newItems)
    fun setItems(newItems: List<*>?) {
        items.clear()
        newItems?.forEach { if (it != null) items.add(it) }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val text = pickString(item, listOf("text","fullText","content","message")) ?: item.toString()
        val link = pickString(item, listOf("link","url","href","permalink")) ?: ""

        holder.t1.text = text
        holder.t2.text = link

        holder.itemView.setOnClickListener {
            val u = link.trim()
            if (u.isNotEmpty()) {
                if (onOpen != null) onOpen.invoke(u)
                else {
                    try {
                        holder.itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val t1: TextView = v.findViewById(android.R.id.text1)
        val t2: TextView = v.findViewById(android.R.id.text2)
    }

    private fun pickString(obj: Any, names: List<String>): String? {
        val c = obj.javaClass
        for (n in names) {
            try {
                val getter = "get" + n.replaceFirstChar { it.uppercase() }
                val m = c.methods.firstOrNull { it.parameterTypes.isEmpty() && (it.name == getter || it.name == n) }
                val v = m?.invoke(obj)
                val s = v?.toString()?.trim()
                if (!s.isNullOrEmpty() && s != "null") return s
            } catch (_: Exception) {}
            try {
                val f = c.declaredFields.firstOrNull { it.name == n }
                if (f != null) {
                    f.isAccessible = true
                    val s = f.get(obj)?.toString()?.trim()
                    if (!s.isNullOrEmpty() && s != "null") return s
                }
            } catch (_: Exception) {}
        }
        return null
    }
}

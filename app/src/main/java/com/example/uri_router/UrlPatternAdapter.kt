package com.example.uri_router

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class UrlPatternAdapter(
    private val onDelete: (UrlPattern) -> Unit
) : ListAdapter<UrlPattern, UrlPatternAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<UrlPattern>() {
        override fun areItemsTheSame(oldItem: UrlPattern, newItem: UrlPattern): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UrlPattern, newItem: UrlPattern): Boolean =
            oldItem == newItem
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val pattern: TextView = view.findViewById(R.id.patternTextView)
        val delete: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_url_pattern, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.pattern.text = item.pattern
        holder.delete.setOnClickListener { onDelete(item) }
    }
}


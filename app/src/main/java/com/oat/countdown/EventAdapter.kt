package com.oat.countdown

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(
    private val events: List<CountdownEvent>,
    private val selectedIds: MutableSet<String>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<EventAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val emoji: TextView = view.findViewById(R.id.cfg_emoji)
        val title: TextView = view.findViewById(R.id.cfg_title)
        val date: TextView = view.findViewById(R.id.cfg_date)
        val days: TextView = view.findViewById(R.id.cfg_days)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.config_item, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ev = events[position]
        holder.emoji.text = ev.emoji
        holder.title.text = ev.title
        holder.date.text = ev.dateString
        holder.days.text = ev.daysLeft.toString()
        holder.checkbox.isChecked = ev.id in selectedIds

        val toggle = View.OnClickListener {
            if (ev.id in selectedIds) {
                selectedIds.remove(ev.id)
            } else {
                selectedIds.add(ev.id)
            }
            holder.checkbox.isChecked = ev.id in selectedIds
            onSelectionChanged()
        }

        holder.itemView.setOnClickListener(toggle)
        holder.checkbox.setOnClickListener(toggle)
    }

    override fun getItemCount() = events.size
}

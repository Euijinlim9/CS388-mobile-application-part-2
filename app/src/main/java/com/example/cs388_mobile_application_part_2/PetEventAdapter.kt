package com.example.cs388_mobile_application_part_2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PetEventAdapter(
    private val onEventClicked: (EventEntity) -> Unit,
    private var items: List<EventEntity> = emptyList()
) : RecyclerView.Adapter<PetEventAdapter.EventViewHolder>() {

    fun submitList(newItems: List<EventEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.upcoming_item, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position], onEventClicked)
    }

    override fun getItemCount(): Int = items.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.upcoming_title)
        private val time: TextView = itemView.findViewById(R.id.upcoming_time)
        private val icon: ImageView = itemView.findViewById(R.id.upcoming_to_calendar)
        private val formatter = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())

        fun bind(event: EventEntity, onEventClicked: (EventEntity) -> Unit) {
            title.text = event.title
            time.text = formatter.format(Date(event.time))
            icon.setImageResource(R.drawable.baseline_calendar_month_24)
            itemView.setOnClickListener { onEventClicked(event) }
        }
    }
}


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

class UpcomingAdapter(
    private var items: List<EventEntity> = emptyList()
) : RecyclerView.Adapter<UpcomingAdapter.UpcomingViewHolder>() {

    fun submitList(newItems: List<EventEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.upcoming_item, parent, false)
        return UpcomingViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpcomingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class UpcomingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.upcoming_title)
        private val time: TextView = itemView.findViewById(R.id.upcoming_time)
        private val calendarIcon: ImageView = itemView.findViewById(R.id.upcoming_to_calendar)
        private val formatter = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())

        fun bind(event: EventEntity) {
            title.text = event.title
            time.text = formatter.format(Date(event.time))
            calendarIcon.setImageResource(R.drawable.baseline_calendar_month_24)
        }
    }
}


package com.example.cs388_mobile_application_part_2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GameFinderAdapter(private val games: List<BoardGame>) : RecyclerView.Adapter<GameFinderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvGameName)
        val tvYear: TextView = view.findViewById(R.id.tvGameYear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        holder.tvName.text = game.name
        holder.tvYear.text = if (game.yearPublished.isNotEmpty()) game.yearPublished else "Year unknown"
    }

    override fun getItemCount() = games.size
}

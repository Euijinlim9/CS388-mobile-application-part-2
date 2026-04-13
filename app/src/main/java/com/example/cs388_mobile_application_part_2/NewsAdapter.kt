package com.example.cs388_mobile_application_part_2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NewsAdapter(private val games: List<BoardGame>) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        holder.tvRank.text = "Rank #${game.rank}"
        holder.tvName.text = game.name
        holder.tvYear.text = if (game.yearPublished.isNotEmpty()) "Published: ${game.yearPublished}" else ""
        if (game.thumbnail.isNotEmpty()) {
            Glide.with(holder.imgThumbnail.context)
                .load(game.thumbnail)
                .centerCrop()
                .into(holder.imgThumbnail)
        }
    }

    override fun getItemCount() = games.size
}

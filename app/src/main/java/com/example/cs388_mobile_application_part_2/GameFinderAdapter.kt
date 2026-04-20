package com.example.cs388_mobile_application_part_2

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.core.graphics.toColorInt

class GameFinderAdapter(private val games: List<BoardGame>) : RecyclerView.Adapter<GameFinderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumb: ImageView = view.findViewById(R.id.imgGameThumb)
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
        holder.tvYear.text = game.yearPublished.ifEmpty { "Year unknown" }
        if (game.thumbnail.isNotEmpty()) {
            Glide.with(holder.imgThumb.context).load(game.thumbnail).centerCrop().into(holder.imgThumb)
        } else {
            holder.imgThumb.setImageDrawable(null)
            holder.imgThumb.setBackgroundColor("#E0E0E0".toColorInt())
        }
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, GameDetailActivity::class.java).apply {
                putExtra("id", game.id)
                putExtra("name", game.name)
                putExtra("thumbnail", game.thumbnail)
                putExtra("year", game.yearPublished)
                putExtra("rank", game.rank)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = games.size
}

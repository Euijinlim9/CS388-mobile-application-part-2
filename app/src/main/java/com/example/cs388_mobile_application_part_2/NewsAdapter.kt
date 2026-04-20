package com.example.cs388_mobile_application_part_2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.core.content.edit

@SuppressLint("SetTextI18n")
class NewsAdapter(private val games: List<BoardGame>) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        val context = holder.itemView.context

        holder.tvRank.text = "Rank #${game.rank}"
        holder.tvName.text = game.name
        holder.tvYear.text = if (game.yearPublished.isNotEmpty()) "Published: ${game.yearPublished}" else ""

        if (game.thumbnail.isNotEmpty()) {
            Glide.with(context).load(game.thumbnail).centerCrop().into(holder.imgThumbnail)
        }

        holder.btnFavorite.setImageResource(
            if (isFavorite(context, game.id)) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )

        holder.btnFavorite.setOnClickListener {
            val nowFav = !isFavorite(context, game.id)
            setFavorite(context, game, nowFav)
            holder.btnFavorite.setImageResource(
                if (nowFav) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
        }

        holder.itemView.setOnClickListener {
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

    companion object {
        private const val PREFS = "favorites_prefs"

        fun isFavorite(context: Context, id: String) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(id, false)

        fun setFavorite(context: Context, game: BoardGame, fav: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                val ids = getFavoriteIds(context).toMutableSet()
                if (fav) {
                    putBoolean(game.id, true)
                    putString("name_${game.id}", game.name)
                    putString("thumb_${game.id}", game.thumbnail)
                    putString("year_${game.id}", game.yearPublished)
                    putInt("rank_${game.id}", game.rank)
                    ids.add(game.id)
                } else {
                    remove(game.id)
                    remove("name_${game.id}")
                    remove("thumb_${game.id}")
                    remove("year_${game.id}")
                    remove("rank_${game.id}")
                    ids.remove(game.id)
                }
                putStringSet("fav_ids", ids)
            }
        }

        fun getFavoriteIds(context: Context): Set<String> =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet("fav_ids", emptySet()) ?: emptySet()

        fun getFavorites(context: Context): List<BoardGame> {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return getFavoriteIds(context).map { id ->
                BoardGame(
                    id = id,
                    rank = prefs.getInt("rank_$id", 0),
                    name = prefs.getString("name_$id", "") ?: "",
                    thumbnail = prefs.getString("thumb_$id", "") ?: "",
                    yearPublished = prefs.getString("year_$id", "") ?: ""
                )
            }
        }
    }
}

package com.example.cs388_mobile_application_part_2

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
@SuppressLint("SetTextI18n")
class GameDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val id = intent.getStringExtra("id") ?: return
        val name = intent.getStringExtra("name") ?: ""
        val thumbnail = intent.getStringExtra("thumbnail") ?: ""
        val year = intent.getStringExtra("year") ?: ""
        val rank = intent.getIntExtra("rank", 0)

        val imgDetail = findViewById<ImageView>(R.id.imgDetail)
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvRank = findViewById<TextView>(R.id.tvDetailRank)
        val tvYear = findViewById<TextView>(R.id.tvDetailYear)
        val tvRating = findViewById<TextView>(R.id.tvDetailRating)
        val tvPlayers = findViewById<TextView>(R.id.tvDetailPlayers)
        val tvTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvDesc = findViewById<TextView>(R.id.tvDetailDescription)
        val progress = findViewById<ProgressBar>(R.id.progressDetail)

        supportActionBar?.title = name
        tvName.text = name
        tvRank.text = "BGG Rank: #$rank"
        tvYear.text = if (year.isNotEmpty()) "Year Published: $year" else ""

        if (thumbnail.isNotEmpty()) {
            Glide.with(this).load(thumbnail).centerCrop().into(imgDetail)
        }

        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.getGameDetails(id, 1)
                }
                parseDetails(response, tvRating, tvPlayers, tvTime, tvDesc)
            } catch (_: Exception) {
                tvDesc.text = "Could not load details"
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun parseDetails(xml: String, tvRating: TextView, tvPlayers: TextView, tvTime: TextView, tvDesc: TextView) {
        try {
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(InputSource(StringReader(xml)))

            val items = doc.getElementsByTagName("item")
            if (items.length == 0) return
            val item = items.item(0)
            val children = item.childNodes

            var description = ""
            var rating = ""
            var minPlayers = ""
            var maxPlayers = ""
            var playTime = ""

            for (i in 0 until children.length) {
                val child = children.item(i)
                when (child.nodeName) {
                    "description" -> description = child.textContent?.trim() ?: ""
                    "minplayers" -> minPlayers = child.attributes?.getNamedItem("value")?.nodeValue ?: ""
                    "maxplayers" -> maxPlayers = child.attributes?.getNamedItem("value")?.nodeValue ?: ""
                    "playingtime" -> playTime = child.attributes?.getNamedItem("value")?.nodeValue ?: ""
                    "statistics" -> {
                        val statsChildren = child.childNodes
                        for (j in 0 until statsChildren.length) {
                            val stat = statsChildren.item(j)
                            if (stat.nodeName == "ratings") {
                                val ratingChildren = stat.childNodes
                                for (k in 0 until ratingChildren.length) {
                                    val r = ratingChildren.item(k)
                                    if (r.nodeName == "average") {
                                        val raw = r.attributes?.getNamedItem("value")?.nodeValue ?: ""
                                        rating = if (raw.isNotEmpty()) "%.2f / 10".format(raw.toDoubleOrNull() ?: 0.0) else ""
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (rating.isNotEmpty()) tvRating.text = "⭐ Rating: $rating"
            if (minPlayers.isNotEmpty() && maxPlayers.isNotEmpty()) tvPlayers.text = "👥 Players: $minPlayers – $maxPlayers"
            if (playTime.isNotEmpty()) tvTime.text = "⏱ Play Time: $playTime min"
            tvDesc.text = description
                .replace("&#10;", "\n")
                .replace("&amp;", "&")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .ifEmpty { "No description available" }

        } catch (_: Exception) {
            tvDesc.text = "Error loading details"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}

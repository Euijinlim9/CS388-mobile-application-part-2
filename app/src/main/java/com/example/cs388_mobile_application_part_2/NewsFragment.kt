package com.example.cs388_mobile_application_part_2

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.xml.sax.InputSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
@SuppressLint("NotifyDataSetChanged")
class NewsFragment : Fragment() {

    private val games = mutableListOf<BoardGame>()
    private lateinit var adapter: NewsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerNews)
        val progress = view.findViewById<ProgressBar>(R.id.progressBar)
        val btnFavorites = view.findViewById<Button>(R.id.btnViewFavorites)

        adapter = NewsAdapter(games)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnFavorites.setOnClickListener { showFavoritesDialog() }

        progress.visibility = View.VISIBLE
        RetrofitClient.service.getHotGames().enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                progress.visibility = View.GONE
                response.body()?.let { parseHotGames(it) }
            }
            override fun onFailure(call: Call<String>, t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load games", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFavoritesDialog() {
        val favs = NewsAdapter.getFavorites(requireContext())
        if (favs.isEmpty()) {
            Toast.makeText(requireContext(), "No favorites yet — tap ⭐ on a card to save it", Toast.LENGTH_SHORT).show()
            return
        }
        val names = favs.map { "⭐ ${it.name} (${it.yearPublished})" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Your Favorites")
            .setItems(names) { _, index ->
                val game = favs[index]
                val intent = android.content.Intent(requireContext(), GameDetailActivity::class.java).apply {
                    putExtra("id", game.id)
                    putExtra("name", game.name)
                    putExtra("thumbnail", game.thumbnail)
                    putExtra("year", game.yearPublished)
                    putExtra("rank", game.rank)
                }
                startActivity(intent)
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun parseHotGames(xml: String) {
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(InputSource(StringReader(xml)))
            val items = doc.getElementsByTagName("item")
            for (i in 0 until items.length) {
                val item = items.item(i)
                val attrs = item.attributes
                val id = attrs.getNamedItem("id")?.nodeValue ?: continue
                val rank = attrs.getNamedItem("rank")?.nodeValue?.toIntOrNull() ?: (i + 1)
                var name = ""
                var thumbnail = ""
                var year = ""
                val children = item.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    when (child.nodeName) {
                        "name" -> name = child.attributes?.getNamedItem("value")?.nodeValue ?: ""
                        "thumbnail" -> thumbnail = child.attributes?.getNamedItem("value")?.nodeValue ?: ""
                        "yearpublished" -> year = child.attributes?.getNamedItem("value")?.nodeValue ?: ""
                    }
                }
                if (name.isNotEmpty()) {
                    games.add(BoardGame(id, rank, name, thumbnail, year))
                }
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error parsing data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

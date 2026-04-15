package com.example.cs388_mobile_application_part_2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.xml.sax.InputSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class GameFinderFragment : Fragment() {

    private val results = mutableListOf<BoardGame>()
    private lateinit var adapter: GameFinderAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_game_finder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val btnSearch = view.findViewById<Button>(R.id.btnSearch)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerGames)
        val progress = view.findViewById<ProgressBar>(R.id.progressBar)

        adapter = GameFinderAdapter(results)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val doSearch = {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) search(query, progress)
            else Toast.makeText(requireContext(), "Enter a game name", Toast.LENGTH_SHORT).show()
        }

        btnSearch.setOnClickListener { doSearch() }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
    }

    private fun search(query: String, progress: ProgressBar) {
        progress.visibility = View.VISIBLE
        results.clear()
        adapter.notifyDataSetChanged()

        RetrofitClient.service.searchGames(query).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                progress.visibility = View.GONE
                response.body()?.let { parseSearchResults(it) }
            }
            override fun onFailure(call: Call<String>, t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun parseSearchResults(xml: String) {
        try {
            val doc = DocumentBuilderFactory.newInstance().also {
                it.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                it.setFeature("http://xml.org/sax/features/external-general-entities", false)
                it.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }.newDocumentBuilder()
                .parse(InputSource(StringReader(xml)))
            val items = doc.getElementsByTagName("item")
            for (i in 0 until items.length) {
                val item = items.item(i)
                val id = item.attributes?.getNamedItem("id")?.nodeValue ?: continue
                var name = ""
                var year = ""
                val children = item.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    when (child.nodeName) {
                        "name" -> name = child.attributes?.getNamedItem("value")?.nodeValue ?: ""
                        "yearpublished" -> year = child.attributes?.getNamedItem("value")?.nodeValue ?: ""
                    }
                }
                if (name.isNotEmpty()) results.add(BoardGame(id, i + 1, name, "", year))
            }
            adapter.notifyDataSetChanged()
            if (results.isEmpty()) Toast.makeText(requireContext(), "No games found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error parsing results", Toast.LENGTH_SHORT).show()
        }
    }
}

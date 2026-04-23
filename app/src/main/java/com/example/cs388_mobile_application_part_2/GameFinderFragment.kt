package com.example.cs388_mobile_application_part_2

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xml.sax.InputSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
@SuppressLint("NotifyDataSetChanged")
class GameFinderFragment : Fragment() {

    private val results = mutableListOf<BoardGame>()
    private lateinit var adapter: GameFinderAdapter
    private lateinit var etSearch: EditText
    private lateinit var progress: ProgressBar
    //private val geminiApiKey = BuildConfig.GEMINI_API_KEY

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        handlePhotoSearch(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_game_finder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etSearch = view.findViewById(R.id.etSearch)
        val btnSearch = view.findViewById<Button>(R.id.btnSearch)
        val btnPhotoSearch = view.findViewById<Button>(R.id.btnPhotoSearch)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerGames)
        progress = view.findViewById(R.id.progressBar)

        adapter = GameFinderAdapter(results)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val doSearch = {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) search(query, progress)
            else Toast.makeText(requireContext(), "Enter a game name", Toast.LENGTH_SHORT).show()
        }

        btnSearch.setOnClickListener { doSearch() }
        btnPhotoSearch.setOnClickListener {
                pickImageLauncher.launch("image/*")
        }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
    }

    private fun handlePhotoSearch(uri: Uri) {

        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val recognizedName = withContext(Dispatchers.IO) {
                val imageBytes = readImageBytes(uri) ?: return@withContext null
                GeminiVisionClient.detectBoardGameName(
                    //apiKey = geminiApiKey,
                    imageBytes = imageBytes
                )
            }

            if (!isAdded) return@launch
            progress.visibility = View.GONE

            if (recognizedName.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Could not detect a board game", Toast.LENGTH_SHORT).show()
                return@launch
            }

            etSearch.setText(recognizedName)
            search(recognizedName, progress)
        }
    }

    private fun readImageBytes(uri: Uri): ByteArray? {
        return try {
            val resolver = requireContext().contentResolver
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    private fun search(query: String, progress: ProgressBar) {
        progress.visibility = View.VISIBLE
        results.clear()
        adapter.notifyDataSetChanged()

        RetrofitClient.service.searchGames(query).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                progress.visibility = View.GONE
                val body = response.body() ?: return
                parseSearchResults(body)
            }
            override fun onFailure(call: Call<String>, t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun parseSearchResults(xml: String) {
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
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
                if (name.isNotEmpty()) {
                    val game = BoardGame(id, i + 1, name, "", year)
                    results.add(game)
                    fetchThumbnail(game)
                }
            }
            adapter.notifyDataSetChanged()
            if (results.isEmpty()) Toast.makeText(requireContext(), "No games found", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Error parsing results", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchThumbnail(game: BoardGame) {
        RetrofitClient.service.getGameDetails(game.id, 0).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                val body = response.body() ?: return
                try {
                    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(InputSource(StringReader(body)))
                    val items = doc.getElementsByTagName("item")
                    if (items.length == 0) return
                    val children = items.item(0).childNodes
                    for (i in 0 until children.length) {
                        val child = children.item(i)
                        if (child.nodeName == "thumbnail") {
                            val thumb = child.textContent?.trim() ?: ""
                            if (thumb.isNotEmpty()) {
                                val index = results.indexOfFirst { it.id == game.id }
                                if (index >= 0) {
                                    results[index] = results[index].copy(thumbnail = thumb)
                                    adapter.notifyItemChanged(index)
                                }
                            }
                            break
                        }
                    }
                } catch (_: Exception) { }
            }
            override fun onFailure(call: Call<String>, t: Throwable) { }
        })
    }
}

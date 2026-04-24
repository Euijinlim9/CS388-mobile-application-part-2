package com.example.cs388_mobile_application_part_2

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
@SuppressLint("NotifyDataSetChanged")
class GameFinderFragment : Fragment() {

    private val results = mutableListOf<BoardGame>()
    private lateinit var adapter: GameFinderAdapter
    private lateinit var etSearch: EditText
    private lateinit var progress: ProgressBar
    private var searchJob: Job? = null
    private var tempImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) handlePhotoSearch(uri)
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { handlePhotoSearch(it) }
        }
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
            if (query.isNotEmpty()) search(query)
            else Toast.makeText(requireContext(), "Enter a game name", Toast.LENGTH_SHORT).show()
        }

        btnSearch.setOnClickListener { doSearch() }
        btnPhotoSearch.setOnClickListener {
            showImageSourceDialog()
        }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = try {
            createImageFile()
        } catch (_: Exception) {
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )
            tempImageUri = photoURI
            takePictureLauncher.launch(photoURI)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun handlePhotoSearch(uri: Uri) {
        searchJob?.cancel()
        progress.visibility = View.VISIBLE
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val recognizedName = withContext(Dispatchers.IO) {
                val imageBytes = readImageBytes(uri) ?: return@withContext null
                GeminiVisionClient.detectBoardGameName(imageBytes = imageBytes)
            }

            if (!isAdded) return@launch
            
            if (recognizedName.isNullOrBlank()) {
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Could not detect a board game", Toast.LENGTH_SHORT).show()
                return@launch
            }

            etSearch.setText(recognizedName)
            search(recognizedName)
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

    private fun search(query: String) {
        searchJob?.cancel()
        progress.visibility = View.VISIBLE
        results.clear()
        adapter.notifyDataSetChanged()

        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.searchGames(query)
                }
                parseSearchResults(response)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
                }
            } finally {
                progress.visibility = View.GONE
            }
        }
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val body = withContext(Dispatchers.IO) {
                    RetrofitClient.service.getGameDetails(game.id, 0)
                }
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(InputSource(StringReader(body)))
                val items = doc.getElementsByTagName("item")
                if (items.length == 0) return@launch
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
    }
}

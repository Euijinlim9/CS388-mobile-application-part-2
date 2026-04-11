package com.example.cs388_mobile_application_part_2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private lateinit var statusText: TextView

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            exportBackup(uri)
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not grant persistable permissions.
            }
            importBackup(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val exportButton = view.findViewById<MaterialButton>(R.id.button_export_backup)
        val importButton = view.findViewById<MaterialButton>(R.id.button_import_backup)
        statusText = view.findViewById(R.id.settings_status)

        exportButton.setOnClickListener { startExportFlow() }
        importButton.setOnClickListener { startImportFlow() }
        statusText.text = getString(R.string.settings_backup_ready)

        return view
    }

    private fun startExportFlow() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        exportBackupLauncher.launch("pet_health_backup_$timestamp.json")
    }

    private fun startImportFlow() {
        importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
    }

    private fun exportBackup(uri: Uri) {
        val db = (requireActivity().application as PetApplication).db
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pets = db.petDao().getAllPetsSnapshot()
                val events = db.eventDao().getAllEventsSnapshot()

                val payload = BackupPayload(
                    pets = pets.map { BackupPet(legacyId = it.id, name = it.name, age = it.age) },
                    events = events.map {
                        BackupEvent(
                            title = it.title,
                            content = it.content,
                            time = it.time,
                            petLegacyId = it.petId
                        )
                    }
                )

                val backupJson = json.encodeToString(payload)
                val outputStream = requireContext().contentResolver.openOutputStream(uri)
                    ?: throw IOException("Cannot open selected file")

                outputStream.bufferedWriter().use { writer ->
                    writer.write(backupJson)
                }

                withContext(Dispatchers.Main) {
                    statusText.text = getString(
                        R.string.settings_export_success,
                        payload.pets.size,
                        payload.events.size
                    )
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = getString(R.string.settings_export_failed)
                }
            }
        }
    }

    private fun importBackup(uri: Uri) {
        val db = (requireActivity().application as PetApplication).db
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val backupJson = requireContext().contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { reader -> reader.readText() }
                    ?: throw IOException("Cannot read selected file")

                val payload = json.decodeFromString<BackupPayload>(backupJson)
                if (payload.schemaVersion != 1) {
                    throw IllegalArgumentException("Unsupported backup format")
                }

                val insertedPets = mutableMapOf<Long, Long>()
                var importedEventCount = 0
                db.withTransaction {
                    db.eventDao().deleteAllEvents()
                    db.petDao().deleteAllPets()

                    payload.pets.forEach { pet ->
                        val newId = db.petDao().insertPet(
                            PetEntity(name = pet.name, age = pet.age, photoUri = null)
                        )
                        insertedPets[pet.legacyId] = newId
                    }

                    val mappedEvents = payload.events.mapNotNull { event ->
                        val newPetId = insertedPets[event.petLegacyId] ?: return@mapNotNull null
                        EventEntity(
                            title = event.title,
                            content = event.content,
                            time = event.time,
                            petId = newPetId
                        )
                    }
                    importedEventCount = mappedEvents.size
                    db.eventDao().insertEvents(mappedEvents)
                }

                withContext(Dispatchers.Main) {
                    statusText.text = getString(
                        R.string.settings_import_success,
                        insertedPets.size,
                        importedEventCount
                    )
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = getString(R.string.settings_import_failed)
                }
            }
        }
    }
}
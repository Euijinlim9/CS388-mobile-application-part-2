package com.example.cs388_mobile_application_part_2

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PetDetailActivity : AppCompatActivity() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private lateinit var petNameInput: EditText
    private lateinit var petAgeInput: EditText
    private lateinit var savePetButton: MaterialButton
    private lateinit var deletePetButton: MaterialButton
    private lateinit var addEventButton: MaterialButton
    private lateinit var changePetPhotoButton: MaterialButton
    private lateinit var petPhotoImage: ImageView
    private lateinit var eventsList: RecyclerView

    private lateinit var eventAdapter: PetEventAdapter

    private lateinit var petDao: PetDao
    private lateinit var eventDao: EventDao

    private var currentPet: PetEntity? = null
    private var selectedPhotoUri: String? = null
    private var petId: Long = -1L

    private val petPhotoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers may not grant persistable access.
            }
            selectedPhotoUri = uri.toString()
            petPhotoImage.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)

        val db = (application as PetApplication).db
        petDao = db.petDao()
        eventDao = db.eventDao()

        petId = intent.getLongExtra(EXTRA_PET_ID, -1L)
        if (petId <= 0L) {
            finish()
            return
        }

        petNameInput = findViewById(R.id.detail_pet_name)
        petAgeInput = findViewById(R.id.detail_pet_age)
        savePetButton = findViewById(R.id.save_pet_button)
        deletePetButton = findViewById(R.id.delete_pet_button)
        addEventButton = findViewById(R.id.add_event_button)
        changePetPhotoButton = findViewById(R.id.change_pet_photo_button)
        petPhotoImage = findViewById(R.id.detail_pet_photo)
        eventsList = findViewById(R.id.detail_events_list)

        eventAdapter = PetEventAdapter(onEventClicked = { event ->
            showEventDialog(event)
        })

        eventsList.layoutManager = LinearLayoutManager(this)
        eventsList.adapter = eventAdapter

        savePetButton.setOnClickListener { savePetChanges() }
        deletePetButton.setOnClickListener { deletePet() }
        addEventButton.setOnClickListener { showEventDialog(null) }
        changePetPhotoButton.setOnClickListener { petPhotoPicker.launch(arrayOf("image/*")) }

        loadPet()
        observeEvents()
    }

    private fun loadPet() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pet = petDao.getPetById(petId)
            withContext(Dispatchers.Main) {
                currentPet = pet
                if (pet == null) {
                    finish()
                    return@withContext
                }
                selectedPhotoUri = pet.photoUri
                petNameInput.setText(pet.name)
                petAgeInput.setText(pet.age.toString())
                updatePetPhotoPreview(pet.photoUri)
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            eventDao.getEventsForPet(petId).collect { events ->
                eventAdapter.submitList(events)
            }
        }
    }

    private fun savePetChanges() {
        val existingPet = currentPet ?: return
        val newName = petNameInput.text.toString().trim()
        val newAge = petAgeInput.text.toString().trim().toIntOrNull()

        if (newName.isBlank() || newAge == null) {
            Toast.makeText(this, "Please enter a valid name and age", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val updated = existingPet.copy(name = newName, age = newAge, photoUri = selectedPhotoUri ?: existingPet.photoUri)
            petDao.updatePet(updated)
            currentPet = updated
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PetDetailActivity, "Pet updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deletePet() {
        AlertDialog.Builder(this)
            .setTitle("Delete pet")
            .setMessage("Delete this pet and all related events?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    eventDao.deleteEventsForPet(petId)
                    petDao.deletePet(petId)
                    withContext(Dispatchers.Main) {
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePetPhotoPreview(photoUri: String?) {
        if (photoUri.isNullOrBlank()) {
            petPhotoImage.setImageResource(R.mipmap.ic_launcher_round)
        } else {
            runCatching { petPhotoImage.setImageURI(Uri.parse(photoUri)) }
                .onFailure { petPhotoImage.setImageResource(R.mipmap.ic_launcher_round) }
        }
    }

    private fun showEventDialog(existingEvent: EventEntity?) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 24, 50, 0)
        }

        val titleInput = EditText(this).apply {
            hint = "Event title"
            setText(existingEvent?.title.orEmpty())
        }

        val contentInput = EditText(this).apply {
            hint = "Event notes"
            setText(existingEvent?.content.orEmpty())
        }

        val dateInput = EditText(this).apply {
            hint = "yyyy-MM-dd HH:mm"
            inputType = InputType.TYPE_CLASS_DATETIME
            if (existingEvent != null) {
                setText(dateFormat.format(Date(existingEvent.time)))
            }
        }

        container.addView(titleInput)
        container.addView(contentInput)
        container.addView(dateInput)

        val title = if (existingEvent == null) "Add Event" else "Edit Event"

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                saveEvent(existingEvent, titleInput.text.toString(), contentInput.text.toString(), dateInput.text.toString())
            }
            .setNegativeButton("Cancel", null)

        if (existingEvent != null) {
            dialogBuilder.setNeutralButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    eventDao.deleteEvent(existingEvent.id)
                }
            }
        }

        dialogBuilder.show()
    }

    private fun saveEvent(existingEvent: EventEntity?, rawTitle: String, rawContent: String, rawTime: String) {
        val title = rawTitle.trim()
        val content = rawContent.trim()
        val parsedTime = dateFormat.parse(rawTime.trim())?.time

        if (title.isBlank() || content.isBlank() || parsedTime == null) {
            Toast.makeText(this, "Enter title, notes, and valid date-time", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            if (existingEvent == null) {
                eventDao.insertEvent(
                    EventEntity(
                        title = title,
                        content = content,
                        time = parsedTime,
                        petId = petId
                    )
                )
            } else {
                eventDao.updateEvent(
                    existingEvent.copy(
                        title = title,
                        content = content,
                        time = parsedTime
                    )
                )
            }
        }
    }

    companion object {
        const val EXTRA_PET_ID = "extra_pet_id"
    }
}



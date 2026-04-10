package com.example.cs388_mobile_application_part_2

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListFragment : Fragment() {
    private lateinit var petAdapter: PetAdapter
    private lateinit var addPetPhotoPicker: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private var addPetPhotoUri: String? = null
    private var addPetPhotoPreview: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPetPhotoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Persistable permission may not be available for every provider.
                }
                addPetPhotoUri = uri.toString()
                addPetPhotoPreview?.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.pet_list)
        val addPetButton = view.findViewById<FloatingActionButton>(R.id.add_pet_button)
        val db = (requireActivity().application as PetApplication).db

        petAdapter = PetAdapter(onPetClicked = { pet ->
            val intent = Intent(requireContext(), PetDetailActivity::class.java)
            intent.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.id)
            startActivity(intent)
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = petAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            db.petDao().getAllPets().collect { pets ->
                petAdapter.submitList(pets)
            }
        }

        addPetButton.setOnClickListener {
            showAddPetDialog(db.petDao())
        }

        return view
    }

    private fun showAddPetDialog(petDao: PetDao) {
        val context = requireContext()
        addPetPhotoUri = null
        addPetPhotoPreview = null
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 24, 50, 0)
        }

        val photoPreview = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(250, 250)
            setImageResource(R.mipmap.ic_launcher_round)
            contentDescription = getString(R.string.pet_photo_content_description)
        }
        addPetPhotoPreview = photoPreview

        val choosePhotoButton = com.google.android.material.button.MaterialButton(context).apply {
            text = getString(R.string.change_pet_photo)
            setOnClickListener { addPetPhotoPicker.launch(arrayOf("image/*")) }
        }

        val nameInput = EditText(context).apply {
            hint = getString(R.string.pet_name_hint)
        }
        val ageInput = EditText(context).apply {
            hint = getString(R.string.pet_age_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        container.addView(photoPreview)
        container.addView(choosePhotoButton)
        container.addView(nameInput)
        container.addView(ageInput)

        AlertDialog.Builder(context)
            .setTitle(R.string.add_pet)
            .setView(container)
            .setPositiveButton(R.string.save_pet) { _, _ ->
                val name = nameInput.text.toString().trim()
                val age = ageInput.text.toString().trim().toIntOrNull()
                if (name.isNotBlank() && age != null) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        petDao.insertPet(PetEntity(name = name, age = age, photoUri = addPetPhotoUri))
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
package com.example.cs388_mobile_application_part_2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PetAdapter(
    private val onPetClicked: (PetEntity) -> Unit,
    private var items: List<PetEntity> = emptyList()
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    fun submitList(newItems: List<PetEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(items[position], onPetClicked)
    }

    override fun getItemCount(): Int = items.size

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val petName: TextView = itemView.findViewById(R.id.pet_name)
        private val petAge: TextView = itemView.findViewById(R.id.pet_age)
        private val petImage: ImageView = itemView.findViewById(R.id.pet_image)

        fun bind(pet: PetEntity, onPetClicked: (PetEntity) -> Unit) {
            petName.text = pet.name
            val ageText = pet.age.toString()
            petAge.text = itemView.context.getString(R.string.pet_age_label, ageText)
            if (pet.photoUri.isNullOrBlank()) {
                petImage.setImageResource(R.mipmap.ic_launcher_round)
            } else {
                runCatching { petImage.setImageURI(Uri.parse(pet.photoUri)) }
                    .onFailure { petImage.setImageResource(R.mipmap.ic_launcher_round) }
            }
            itemView.setOnClickListener { onPetClicked(pet) }
        }
    }
}


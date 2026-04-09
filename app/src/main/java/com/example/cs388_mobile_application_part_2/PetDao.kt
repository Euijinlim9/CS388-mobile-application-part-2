package com.example.cs388_mobile_application_part_2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao{
    @Query("SELECT * FROM pets")
    fun getAllPets(): Flow<List<PetEntity>>
    @Query("SELECT * FROM pets WHERE id = :petId")
    fun getPetById(petId: Long): PetEntity
    @Insert
    fun insertPet(pet: PetEntity): Long
}
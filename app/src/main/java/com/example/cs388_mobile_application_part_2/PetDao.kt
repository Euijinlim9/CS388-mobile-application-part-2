package com.example.cs388_mobile_application_part_2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao{
    @Query("SELECT * FROM pets ORDER BY name COLLATE NOCASE ASC")
    fun getAllPets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets ORDER BY id ASC")
    suspend fun getAllPetsSnapshot(): List<PetEntity>

    @Query("SELECT * FROM pets WHERE id = :petId")
    suspend fun getPetById(petId: Long): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity): Long

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Query("DELETE FROM pets WHERE id = :petId")
    suspend fun deletePet(petId: Long)

    @Query("DELETE FROM pets")
    suspend fun deleteAllPets()
}
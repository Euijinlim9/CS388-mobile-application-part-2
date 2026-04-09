package com.example.cs388_mobile_application_part_2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE petId = :petId")
    fun getEventsForPet(petId: Long): Flow<List<EventEntity>>
    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getEventById(eventId: Long): EventEntity
    @Insert
    fun insertEvent(event: EventEntity): Long
    @Query("DELETE FROM events WHERE id = :eventId")
    fun deleteEvent(eventId: Long)
}
package com.example.cs388_mobile_application_part_2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE petId = :petId ORDER BY time ASC")
    fun getEventsForPet(petId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE time > (strftime('%s','now') * 1000) ORDER BY time ASC")
    fun getUpcomingEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Long)

    @Query("DELETE FROM events WHERE petId = :petId")
    suspend fun deleteEventsForPet(petId: Long)
}
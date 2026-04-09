package com.example.cs388_mobile_application_part_2

import android.app.Application
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PetEntity::class, EventEntity::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun eventDao(): EventDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }

            }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, "pet-db"
            ).build()
    }
}
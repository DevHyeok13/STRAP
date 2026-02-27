package com.example.strapxml.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.strapxml.data.dao.StretchRecordDao
import com.example.strapxml.data.entity.StretchRecord

@Database(entities = [StretchRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stretchRecordDao(): StretchRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stretch_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
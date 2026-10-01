package com.day.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TaskEntity::class,
        DocumentEntity::class,
        NoteEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DayDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun documentDao(): DocumentDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: DayDatabase? = null

        fun getInstance(context: Context): DayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DayDatabase::class.java,
                    "day_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

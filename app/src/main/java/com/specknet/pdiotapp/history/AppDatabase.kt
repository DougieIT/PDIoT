package com.specknet.pdiotapp.database
import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [ActivityLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityHistoryManager(): ActivityHistoryManager

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            Log.d("Databse Context:", context.toString())
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "activity_log_database"
                ).fallbackToDestructiveMigration() // Simplifies version upgrades
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
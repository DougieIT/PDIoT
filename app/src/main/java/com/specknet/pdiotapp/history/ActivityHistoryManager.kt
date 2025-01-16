package com.specknet.pdiotapp.database

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActivityHistoryManager {

    @Insert
    suspend fun insert(element: ActivityLog)

    @Query("SELECT * FROM activity_log WHERE userEmail = :email AND timeStamp LIKE :selectedDate || '%' AND activity = :activity")
    suspend fun getUserActivityOnDate(email: String, selectedDate: String, activity: String): List<ActivityLog>

    @Query("SELECT * FROM activity_log WHERE userEmail = :email AND timeStamp LIKE :selectedDate || '%'")
    suspend fun getAllDataOnDate(email: String, selectedDate: String) : List<ActivityLog>

    suspend fun insertSampleData() {

        val sampleData = listOf(
            ActivityLog(userEmail = "user@pdiot.com", timeStamp = "2024-11-17 08:00:01", activity = "running"),
            ActivityLog(userEmail = "user@pdiot.com", timeStamp = "2024-11-17 09:00:01", activity = "walking"),
            ActivityLog(userEmail = "user@pdiot.com", timeStamp = "2024-11-17 10:00:01", activity = "misc")
        )

        for (log in sampleData) {
            Log.d("Inserting data", "into database")
            insert(log)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activityLogs: List<ActivityLog>)



}




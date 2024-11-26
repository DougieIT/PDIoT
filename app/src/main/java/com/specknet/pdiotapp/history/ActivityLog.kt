package com.specknet.pdiotapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_log")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val timeStamp: String, // Store as ISO 8601 formatted string
    val activity: String
)
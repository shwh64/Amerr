package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counter_logs")
data class CounterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val changeType: String, // "INCREMENT", "DECREMENT", "RESET"
    val countAfterChange: Int,
    val timestamp: Long = System.currentTimeMillis()
)

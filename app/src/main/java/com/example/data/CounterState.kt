package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counter_state")
data class CounterState(
    @PrimaryKey val id: Int = 1,
    val count: Int = 0
)

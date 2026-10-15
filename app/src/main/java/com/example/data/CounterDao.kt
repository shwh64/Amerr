package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {
    @Query("SELECT * FROM counter_state WHERE id = 1")
    fun getCounterState(): Flow<CounterState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCounterState(state: CounterState)

    @Query("SELECT * FROM counter_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<CounterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CounterLog)

    @Query("DELETE FROM counter_logs")
    suspend fun clearLogs()
}

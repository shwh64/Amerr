package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CounterRepository(private val counterDao: CounterDao) {

    val counterState: Flow<CounterState> = counterDao.getCounterState()
        .map { it ?: CounterState() }

    val recentLogs: Flow<List<CounterLog>> = counterDao.getRecentLogs()

    suspend fun updateCount(newCount: Int, changeType: String) {
        counterDao.saveCounterState(CounterState(count = newCount))
        counterDao.insertLog(CounterLog(changeType = changeType, countAfterChange = newCount))
    }

    suspend fun resetCounter() {
        counterDao.saveCounterState(CounterState(count = 0))
        counterDao.insertLog(CounterLog(changeType = "RESET", countAfterChange = 0))
        counterDao.clearLogs()
    }
}

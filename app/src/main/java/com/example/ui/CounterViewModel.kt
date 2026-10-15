package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CounterLog
import com.example.data.CounterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CounterViewModel(private val repository: CounterRepository) : ViewModel() {

    val counterValue: StateFlow<Int> = repository.counterState
        .map { it.count }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val logs: StateFlow<List<CounterLog>> = repository.recentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun increment() {
        viewModelScope.launch {
            val current = counterValue.value
            repository.updateCount(current + 1, "INCREMENT")
        }
    }

    fun decrement() {
        viewModelScope.launch {
            val current = counterValue.value
            repository.updateCount(current - 1, "DECREMENT")
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.resetCounter()
        }
    }
}

class CounterViewModelFactory(private val repository: CounterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CounterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CounterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

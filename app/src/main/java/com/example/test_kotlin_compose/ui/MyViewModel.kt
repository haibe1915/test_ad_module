package com.example.test_kotlin_compose.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyViewModel : ViewModel() {

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    fun increment() {
        viewModelScope.launch {
            _count.value = _count.value + 1
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _count.value = 10  // giả sử API trả về số 10
        }
    }

    override fun onCleared() {
        super.onCleared()
        println("ViewModel destroyed") // cleanup
    }
}

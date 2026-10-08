package com.tomdunkley.dailypuzzles

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation: StateFlow<String?> = _pendingNavigation.asStateFlow()

    fun navigate(dest: String) { _pendingNavigation.value = dest }
    fun clearNavigation() { _pendingNavigation.value = null }
}

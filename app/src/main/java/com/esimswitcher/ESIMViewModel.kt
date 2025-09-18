package com.esimswitcher

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ESIMViewModel : ViewModel() {
    
    private val esimManager = ESIMManager()
    
    private val _profiles = MutableLiveData<List<ESIMProfile>>()
    val profiles: LiveData<List<ESIMProfile>> = _profiles
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadESIMProfiles(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val profiles = withContext(Dispatchers.IO) {
                    esimManager.getESIMProfiles(context)
                }
                _profiles.value = profiles
            } catch (e: Exception) {
                _error.value = "Failed to load eSIM profiles: ${e.message}"
                _profiles.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun switchProfile(context: Context, profile: ESIMProfile, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                esimManager.switchToProfile(context, profile, callback)
            }
        }
    }
}
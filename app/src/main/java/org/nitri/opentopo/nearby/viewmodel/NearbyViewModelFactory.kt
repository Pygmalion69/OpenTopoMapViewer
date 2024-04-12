package org.nitri.opentopo.nearby.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.nitri.opentopo.nearby.repo.NearbyRepository

class NearbyViewModelFactory(private val repository: NearbyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NearbyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NearbyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
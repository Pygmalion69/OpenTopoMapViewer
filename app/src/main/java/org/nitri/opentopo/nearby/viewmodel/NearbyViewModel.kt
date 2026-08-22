package org.nitri.opentopo.nearby.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.nearby.repo.NearbyRepository

class NearbyViewModel(private val mRepository: NearbyRepository) : ViewModel() {
     private val _items = MutableLiveData<List<NearbyItem>>()
    val items: LiveData<List<NearbyItem>> get() = _items

    init {
        viewModelScope.launch {
            mRepository.items.collect {
                _items.value = it
            }
        }
        mRepository.refresh(viewModelScope)
    }
}

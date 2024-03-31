package org.nitri.opentopo.nearby.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.nearby.repo.NearbyRepository

class NearbyViewModel(private val mRepository: NearbyRepository) : ViewModel() {
     private val _items = MutableLiveData<List<NearbyItem>>()
    val items: LiveData<List<NearbyItem>> get() = _items

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val nearbyItems: List<NearbyItem> = mRepository.loadNearbyItems(viewModelScope)
                _items.postValue(nearbyItems)
            }
        }
    }

}

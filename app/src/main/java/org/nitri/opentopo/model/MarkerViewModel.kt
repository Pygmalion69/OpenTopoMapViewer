package org.nitri.opentopo.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MarkerViewModel : ViewModel() {

    val markers: MutableLiveData<List<MarkerModel>> by lazy {
        MutableLiveData<List<MarkerModel>>()
    }

    fun addMarker(marker: MarkerModel) {
        val currentList = markers.value ?: emptyList()
        markers.value = currentList + marker
    }

    fun removeMarker(markerId: Int) {
        markers.value = markers.value?.filter { it.id != markerId }
    }

    fun updateMarker(updatedMarker: MarkerModel) {
        markers.value = markers.value?.map { marker ->
            if (marker.id == updatedMarker.id) updatedMarker else marker
        }
    }
}
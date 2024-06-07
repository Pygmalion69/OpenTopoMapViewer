package org.nitri.opentopo.overlay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nitri.opentopo.overlay.OverlayDatabase
import org.nitri.opentopo.overlay.model.MarkerModel

class MarkerViewModel(application: Application) : AndroidViewModel(application) {
    private val db: OverlayDatabase = OverlayDatabase.getDatabase(application)

    val markers: LiveData<List<MarkerModel>> = db.markerDao().getAllMarkers()

    fun addMarker(marker: MarkerModel) = viewModelScope.launch {
        db.markerDao().insertMarker(marker)
    }

    fun removeMarker(markerId: Int) = viewModelScope.launch {
        db.markerDao().deleteMarkerById(markerId)
    }

    fun updateMarker(marker: MarkerModel) = viewModelScope.launch {
        db.markerDao().updateMarker(marker)
    }
}
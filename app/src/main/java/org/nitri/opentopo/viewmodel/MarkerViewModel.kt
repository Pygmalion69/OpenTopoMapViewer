package org.nitri.opentopo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.overlay.OverlayDatabase

class MarkerViewModel(application: Application) : AndroidViewModel(application) {
    private val db: OverlayDatabase = OverlayDatabase.getDatabase(application)

    val markers: LiveData<List<MarkerModel>> = db.markerDao().getAllMarkers()

    fun addMarker(marker: MarkerModel) = viewModelScope.launch {
        db.markerDao().insertMarker(marker)
    }

    fun addMarkers(markers: List<MarkerModel>) = viewModelScope.launch {
        db.markerDao().insertMarkers(markers)
    }

    suspend fun getAllMarkersNow(): List<MarkerModel> {
        return withContext(Dispatchers.IO) {
            db.markerDao().getAllMarkersNow()
        }
    }

    fun removeMarker(markerId: Int) = viewModelScope.launch {
        db.markerDao().deleteMarkerById(markerId)
    }

    fun removeMarkers(markerIds: List<Int>) = viewModelScope.launch {
        db.markerDao().deleteMarkersByIds(markerIds)
    }

    fun updateMarker(marker: MarkerModel) = viewModelScope.launch {
        db.markerDao().updateMarker(marker)
    }

    fun hasRoutePoints(): Boolean {
        return markers.value?.any { it.routeWaypoint } ?: false
    }
}

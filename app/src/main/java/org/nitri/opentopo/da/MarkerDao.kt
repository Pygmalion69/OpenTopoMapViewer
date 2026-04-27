package org.nitri.opentopo.da

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.nitri.opentopo.model.MarkerModel

@Dao
interface MarkerDao {
    @Query("SELECT * FROM MarkerModel")
    fun getAllMarkers(): LiveData<List<MarkerModel>>

    @Query("SELECT * FROM MarkerModel")
    suspend fun getAllMarkersNow(): List<MarkerModel>

    @Insert
    suspend fun insertMarker(marker: MarkerModel)

    @Insert
    suspend fun insertMarkers(markers: List<MarkerModel>)

    @Update
    suspend fun updateMarker(marker: MarkerModel)

    @Query("DELETE FROM MarkerModel WHERE id = :markerId")
    suspend fun deleteMarkerById(markerId: Int)

    @Query("DELETE FROM MarkerModel WHERE id IN (:markerIds)")
    suspend fun deleteMarkersByIds(markerIds: List<Int>)
}

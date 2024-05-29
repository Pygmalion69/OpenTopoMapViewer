package org.nitri.opentopo.overlay.da

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.nitri.opentopo.overlay.model.MarkerModel

@Dao
interface MarkerDao {
    @Query("SELECT * FROM MarkerModel")
    fun getAllMarkers(): LiveData<List<MarkerModel>>

    @Insert
    suspend fun insertMarker(marker: MarkerModel)

    @Update
    suspend fun updateMarker(marker: MarkerModel)

    @Query("DELETE FROM MarkerModel WHERE id = :markerId")
    suspend fun deleteMarkerById(markerId: Int)
}
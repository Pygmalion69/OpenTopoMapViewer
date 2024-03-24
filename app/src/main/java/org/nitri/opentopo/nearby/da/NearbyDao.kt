package org.nitri.opentopo.nearby.da

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.nitri.opentopo.nearby.entity.NearbyItem

@Dao
interface NearbyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItems(vararg nearbyItems: NearbyItem)

    @Query("SELECT * FROM Nearby")
    fun loadAll(): LiveData<List<NearbyItem>>

    @Query("DELETE FROM Nearby")
    fun delete()
}

package org.nitri.opentopo.nearby.da

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.nitri.opentopo.nearby.entity.NearbyItem

@Dao
interface NearbyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItems(vararg nearbyItems: NearbyItem)

    @Query("SELECT * FROM Nearby")
    fun loadAll(): List<NearbyItem>

    @Query("SELECT * FROM Nearby")
    fun observeAll(): Flow<List<NearbyItem>>

    @Query("DELETE FROM Nearby")
    fun delete()

    @Transaction
    fun replaceAll(items: List<NearbyItem>) {
        delete()
        insertItems(*items.toTypedArray())
    }
}

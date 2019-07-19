package org.nitri.opentopo.nearby.da;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.nitri.opentopo.nearby.entity.NearbyItem;

import java.util.List;

@Dao
public interface NearbyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItems(NearbyItem... nearbyItems);

    @Query("SELECT * FROM Nearby")
    LiveData<List<NearbyItem>> loadAll();

    @Query("DELETE FROM Nearby")
    void delete();
}

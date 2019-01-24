package org.nitri.opentopo.nearby.da;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

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

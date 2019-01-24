package org.nitri.opentopo.nearby.api;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import org.nitri.opentopo.nearby.da.NearbyDao;
import org.nitri.opentopo.nearby.entity.NearbyItem;

@Database(entities = {NearbyItem.class}, version = 1, exportSchema = false)
public abstract class NearbyDatabase extends RoomDatabase {

    private static NearbyDatabase instance;

    public abstract NearbyDao nearbyDao();

    public static synchronized NearbyDatabase getDatabase(Context context) {
        if (instance == null) {
            instance =
                    Room.databaseBuilder(context.getApplicationContext(), NearbyDatabase.class, "reading-database")
                            .build();
        }
        return instance;
    }

}

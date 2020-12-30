package org.nitri.opentopo.nearby.api;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
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
                    Room.databaseBuilder(context.getApplicationContext(), NearbyDatabase.class, "nearby-database")
                            .build();
        }
        return instance;
    }

}

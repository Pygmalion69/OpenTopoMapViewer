package org.nitri.opentopo.da

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.overlay.OverlayDatabase

@RunWith(AndroidJUnit4::class)
class MarkerDaoInstrumentedTest {
    private lateinit var db: OverlayDatabase
    private lateinit var dao: MarkerDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OverlayDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.markerDao()
    }

    @After fun teardown() { db.close() }

    @Test fun insertUpdateDeleteMarker_flowWorks() = runBlocking {
        dao.insertMarker(MarkerModel(seq = 1, latitude = 1.0, longitude = 2.0, name = "A", description = "x"))
        val inserted = dao.getAllMarkersNow()
        assertEquals(1, inserted.size)
        val updated = inserted.first().copy(name = "B")
        dao.updateMarker(updated)
        assertEquals("B", dao.getAllMarkersNow().first().name)
        dao.deleteMarkerById(updated.id)
        assertTrue(dao.getAllMarkersNow().isEmpty())
    }

    @Test fun deleteMarkersByIds_removesMultiple() = runBlocking {
        dao.insertMarkers(listOf(
            MarkerModel(seq = 1, latitude = 1.0, longitude = 1.0, name = "a", description = ""),
            MarkerModel(seq = 2, latitude = 2.0, longitude = 2.0, name = "b", description = ""),
            MarkerModel(seq = 3, latitude = 3.0, longitude = 3.0, name = "c", description = "")
        ))
        val all = dao.getAllMarkersNow()
        dao.deleteMarkersByIds(listOf(all[0].id, all[1].id))
        val remaining = dao.getAllMarkersNow()
        assertEquals(1, remaining.size)
        assertEquals("c", remaining.first().name)
    }
}

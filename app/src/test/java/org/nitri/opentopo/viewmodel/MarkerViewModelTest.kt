package org.nitri.opentopo.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.nitri.opentopo.da.MarkerDao
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.overlay.OverlayDatabase

@OptIn(ExperimentalCoroutinesApi::class)
class MarkerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var database: OverlayDatabase

    @Mock
    private lateinit var markerDao: MarkerDao

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        `when`(database.markerDao()).thenReturn(markerDao)
        `when`(markerDao.getAllMarkers()).thenReturn(MutableLiveData(emptyList()))
    }

    @Test
    fun hasRoutePoints_returnsTrueWhenRoutePointsExist() {
        val markers = listOf(
            MarkerModel(seq = 1, latitude = 0.0, longitude = 0.0, name = "A", description = "", routeWaypoint = true)
        )
        `when`(markerDao.getAllMarkers()).thenReturn(MutableLiveData(markers))
        
        val mockDbStatic: MockedStatic<OverlayDatabase> = mockStatic(OverlayDatabase::class.java)
        mockDbStatic.`when`<OverlayDatabase> { OverlayDatabase.getDatabase(any()) }.thenReturn(database)
        
        val vm = MarkerViewModel(application)
        assertTrue(vm.hasRoutePoints())
        
        mockDbStatic.close()
    }

    @Test
    fun hasRoutePoints_returnsFalseWhenNoRoutePoints() {
        val markers = listOf(
            MarkerModel(seq = 1, latitude = 0.0, longitude = 0.0, name = "A", description = "", routeWaypoint = false)
        )
        `when`(markerDao.getAllMarkers()).thenReturn(MutableLiveData(markers))
        
        val mockDbStatic: MockedStatic<OverlayDatabase> = mockStatic(OverlayDatabase::class.java)
        mockDbStatic.`when`<OverlayDatabase> { OverlayDatabase.getDatabase(any()) }.thenReturn(database)
        
        val vm = MarkerViewModel(application)
        assertFalse(vm.hasRoutePoints())
        
        mockDbStatic.close()
    }

    @Test
    fun addMarker_callsDao() = runTest {
        val marker = MarkerModel(seq = 1, latitude = 0.0, longitude = 0.0, name = "A", description = "")
        
        val mockDbStatic: MockedStatic<OverlayDatabase> = mockStatic(OverlayDatabase::class.java)
        mockDbStatic.`when`<OverlayDatabase> { OverlayDatabase.getDatabase(any()) }.thenReturn(database)
        
        val viewModel = MarkerViewModel(application)
        viewModel.addMarker(marker)
        advanceUntilIdle()
        verify(markerDao).insertMarker(marker)
        
        mockDbStatic.close()
    }

    @Test
    fun removeMarker_callsDao() = runTest {
        val mockDbStatic: MockedStatic<OverlayDatabase> = mockStatic(OverlayDatabase::class.java)
        mockDbStatic.`when`<OverlayDatabase> { OverlayDatabase.getDatabase(any()) }.thenReturn(database)
        
        val viewModel = MarkerViewModel(application)
        viewModel.removeMarker(123)
        advanceUntilIdle()
        verify(markerDao).deleteMarkerById(123)
        
        mockDbStatic.close()
    }
}

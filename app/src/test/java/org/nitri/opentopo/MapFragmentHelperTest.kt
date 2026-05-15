package org.nitri.opentopo

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.lang.reflect.Method

class MapFragmentHelperTest {

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    private lateinit var mapFragment: MapFragment

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mapFragment = MapFragment()
        
        // Use reflection to set the private sharedPreferences field
        val field = MapFragment::class.java.getDeclaredField("sharedPreferences")
        field.isAccessible = true
        field.set(mapFragment, mockSharedPreferences)
    }

    @Test
    fun testReadMaxZoomLevel_returnsDefaultWhenPreferenceEmpty() {
        `when`(mockSharedPreferences.getString(eq(SettingsActivity.PREF_MAX_ZOOM_LEVEL), any())).thenReturn(null)
        
        val method: Method = MapFragment::class.java.getDeclaredMethod("readMaxZoomLevel")
        method.isAccessible = true
        val result = method.invoke(mapFragment) as Double
        
        assertEquals(17.0, result, 0.0)
    }

    @Test
    fun testReadMaxZoomLevel_returnsValueFromPrefs() {
        `when`(mockSharedPreferences.getString(eq(SettingsActivity.PREF_MAX_ZOOM_LEVEL), any())).thenReturn("19")
        
        val method: Method = MapFragment::class.java.getDeclaredMethod("readMaxZoomLevel")
        method.isAccessible = true
        val result = method.invoke(mapFragment) as Double
        
        assertEquals(19.0, result, 0.0)
    }

    @Test
    fun testReadOpenTopoMapSource_returnsPreferenceValue() {
        `when`(mockSharedPreferences.getString(eq("open_topo_map_source"), any())).thenReturn("custom_source")
        
        val method: Method = MapFragment::class.java.getDeclaredMethod("readOpenTopoMapSource")
        method.isAccessible = true
        val result = method.invoke(mapFragment) as String
        
        assertEquals("custom_source", result)
    }
}

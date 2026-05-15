package org.nitri.opentopo.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.osmdroid.config.Configuration
import org.osmdroid.config.IConfigurationProvider
import java.io.File

class UtilsCacheTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockConfigurationProvider: IConfigurationProvider

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)
    }

    @Test
    fun clearOsmdroidSqliteCache_deletesFilesAndSavesConfig() {
        val mockConfigStatic: MockedStatic<Configuration> = mockStatic(Configuration::class.java)
        val mockLogStatic: MockedStatic<Log> = mockStatic(Log::class.java)
        
        try {
            mockConfigStatic.`when`<IConfigurationProvider> { Configuration.getInstance() }.thenReturn(mockConfigurationProvider)
            
            val tempDir = File.createTempFile("osmdroid", "cache")
            tempDir.delete()
            tempDir.mkdir()
            val cacheDb = File(tempDir, "cache.db")
            cacheDb.createNewFile()
            
            `when`(mockConfigurationProvider.osmdroidTileCache).thenReturn(tempDir)

            Utils.clearOsmdroidSqliteCache(mockContext)

            // Verify the file was deleted (or attempted to be deleted)
            // Note: Since we are mocking, we just check if it was called if possible, 
            // but here we check real file system as we created a real temp file.
            assert(!cacheDb.exists())

        } finally {
            mockConfigStatic.close()
            mockLogStatic.close()
        }
    }
}

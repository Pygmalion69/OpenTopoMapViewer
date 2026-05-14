package org.nitri.opentopo.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqlTileWriter
import java.io.File

/**
 * Imports OpenTopoMap tiles from the download directory to the internal SQLite cache.
 *
 * @return An array containing the number of tiles imported and the number of tiles deleted (if applicable).
 */
suspend fun importOpenTopoMapTilesToSqliteCache(): IntArray =
    withContext(Dispatchers.IO) {
        val base = File(
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            ),
            "osmdroid"
        )

        Configuration.getInstance().osmdroidBasePath = base
        Configuration.getInstance().osmdroidTileCache = File(base, "tiles")

        val writer = SqlTileWriter()

        // false = keep original PNG file tree
        // true  = delete PNG files after successful import
        writer.importFromFileCache(false)
    }

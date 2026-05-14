package org.nitri.opentopo.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqlTileWriter
import java.io.File

/**
 * Imports OpenTopoMap tiles from the current osmdroid file cache to the SQLite cache.
 *
 * @return An array containing the number of tiles imported and the number of tiles deleted (if applicable).
 */
suspend fun importOpenTopoMapTilesToSqliteCache(): IntArray =
    withContext(Dispatchers.IO) {
        val writer = SqlTileWriter()
        try {
            writer.refreshDb()
            // false = keep original PNG file tree
            // true  = delete PNG files after successful import
            writer.importFromFileCache(false)
        } finally {
            writer.onDetach()
        }
    }

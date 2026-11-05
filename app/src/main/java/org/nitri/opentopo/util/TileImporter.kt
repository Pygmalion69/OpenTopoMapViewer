package org.nitri.opentopo.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.modules.DatabaseFileArchive
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import java.io.IOException
import java.util.zip.ZipInputStream

private const val TAG = "TileImporter"

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val skippedSamples: List<String>,
    val error: String? = null
)

suspend fun importOpenTopoMapZipToSqliteCache(
    context: Context,
    uri: Uri
): ImportResult {
    val requestedGeneration = MapCache.generation
    val requestedDuringClear = MapCache.clearing.value
    return withContext(Dispatchers.IO) {
        // Wait for a running import before purging. Reject queued imports crossing a clear.
        synchronized(MapCache.lock) {
            if (requestedDuringClear || MapCache.clearing.value ||
                requestedGeneration != MapCache.generation
            ) {
                ImportResult(0, 0, emptyList(), context.getString(org.nitri.opentopo.R.string.clear_map_cache_progress))
            } else {
                importZipLocked(context, uri)
            }
        }
    }
}

private fun importZipLocked(context: Context, uri: Uri): ImportResult {
    var imported = 0
    var skipped = 0
    val skippedSamples = mutableListOf<String>()

    val tileSource = XYTileSource(
        "OpenTopoMap",
        0,
        17,
        256,
        ".png",
        emptyArray()
    )

    return try {
        val writer = SqlTileWriter()
        // Do not close the SQLite connection shared by active map providers.
        
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open ZIP file")
        stream.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase().endsWith(".png")) {
                        val path = entry.name
                        // Layout A: OpenTopoMap/{z}/{x}/{y}.png
                        // Layout B: {z}/{x}/{y}.png
                        
                        var cleanPath = path
                        if (path.startsWith("OpenTopoMap/", ignoreCase = true)) {
                            cleanPath = path.substring("OpenTopoMap/".length)
                        }
                        
                        val parts = cleanPath.split("/")
                        if (parts.size == 3) {
                            try {
                                val z = parts[0].toInt()
                                val x = parts[1].toInt()
                                val y = parts[2].substringBefore(".png").toInt()
                                
                                val index = MapTileIndex.getTileIndex(z, x, y)
                                
                                val bytes = zis.readBytes()
                                // osmdroid 6.1.20 returns false even after a successful insert.
                                writer.saveFile(tileSource, index, bytes.inputStream(), Long.MAX_VALUE)
                                val key = SqlTileWriter.getPrimaryKeyParameters(
                                    SqlTileWriter.getIndex(index), tileSource
                                )
                                val stored = writer.getTileCursor(
                                    key, arrayOf(DatabaseFileArchive.COLUMN_TILE, SqlTileWriter.COLUMN_EXPIRES)
                                )?.use { cursor ->
                                    cursor.moveToFirst() && cursor.getLong(1) == Long.MAX_VALUE &&
                                        bytes.contentEquals(cursor.getBlob(0))
                                } == true
                                if (!stored) throw IOException("Tile could not be verified in the cache")
                                imported++
                                if (imported % 100 == 0) {
                                    Log.d(TAG, "Imported $imported tiles...")
                                }
                            } catch (e: Exception) {
                                skipped++
                                if (skippedSamples.size < 5) skippedSamples.add(path)
                                Log.w(TAG, "Failed to parse or save tile: $path", e)
                            }
                        } else {
                            skipped++
                            if (skippedSamples.size < 5) skippedSamples.add(path)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        }
        ImportResult(imported, skipped, skippedSamples)
    } catch (e: Exception) {
        Log.e(TAG, "ZIP import failed", e)
        ImportResult(imported, skipped, skippedSamples, e.message ?: "Unknown error")
    }
}
package org.nitri.opentopo.util

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import org.nitri.opentopo.CacheSettingsFragment
import org.osmdroid.config.Configuration
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.modules.DatabaseFileArchive
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import java.io.IOException
import java.io.File
import java.io.FilterInputStream
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

private const val MIB = 1024L * 1024
private const val CACHE_INCREMENT_MIB = 100L

/**
 * ZIP stream entry sizes may be unknown until decompressed. Count PNG payloads plus
 * 1 KiB per entry for SQLite rows/indexes, then allow 25% on existing and incoming
 * data. Counting overlapping tiles again is deliberately conservative.
 */
private fun prepareCacheForImport(context: Context, uri: Uri, writer: MapCache.GuardedWriter) {
    var compressedBytes = 0L
    var tileBytes = 0L
    var tileCount = 0L
    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Unable to open ZIP file")
    val counted = object : FilterInputStream(input) {
        override fun read(): Int = super.read().also { if (it >= 0) compressedBytes++ }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            super.read(buffer, offset, length).also { if (it > 0) compressedBytes = Math.addExact(compressedBytes, it.toLong()) }
    }
    ZipInputStream(counted).use { zip ->
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val entry = zip.nextEntry ?: break
            val tile = !entry.isDirectory && entry.name.lowercase().endsWith(".png")
            if (tile) tileCount = Math.addExact(tileCount, 1)
            while (true) {
                val n = zip.read(buffer)
                if (n < 0) break
                if (tile) tileBytes = Math.addExact(tileBytes, n.toLong())
            }
            zip.closeEntry()
        }
    }

    val configuration = Configuration.getInstance()
    val database = writer.activeDatabase() ?: throw IOException("Tile cache is unavailable")
    val dbFile = File(database.path)
    val expected = File(configuration.osmdroidTileCache, SqlTileWriter.DATABASE_FILENAME)
    if (dbFile.canonicalFile != expected.canonicalFile) throw IOException("Active tile cache does not match the configured cache")
    val existing = listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-journal"))
        .fold(0L) { total, file -> Math.addExact(total, file.length()) }
    val incoming = Math.addExact(tileBytes, Math.multiplyExact(tileCount, 1024L))
    val projected = Math.addExact(existing, incoming)
    val required = Math.addExact(projected, Math.addExact(projected, 3) / 4)
    val increment = CACHE_INCREMENT_MIB * MIB
    val rounded = Math.multiplyExact(Math.addExact(required, increment - 1) / increment, increment)
    val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    val currentMb = prefs.getInt(CacheSettingsFragment.PREF_CACHE_SIZE, CacheSettingsFragment.DEFAULT_CACHE_SIZE)
    val current = maxOf(configuration.tileFileSystemCacheMaxBytes, currentMb.toLong() * MIB)

    // Free space is separate from the cache limit. Include headroom for SQLite growth.
    val diskNeeded = Math.addExact(incoming, Math.addExact(incoming, 3) / 4)
    if (diskNeeded > dbFile.parentFile!!.usableSpace) {
        throw IOException("Not enough free storage to import this map")
    }

    if (required > current) {
        val newMb = rounded / MIB
        if (newMb > Int.MAX_VALUE) throw IOException("Map package is too large for the cache setting")
        if (!prefs.edit().putInt(CacheSettingsFragment.PREF_CACHE_SIZE, newMb.toInt()).commit()) {
            throw IOException("Unable to save the expanded cache size")
        }
        configuration.tileFileSystemCacheMaxBytes = rounded
    } else {
        configuration.tileFileSystemCacheMaxBytes = current
    }
    val max = configuration.tileFileSystemCacheMaxBytes
    configuration.tileFileSystemCacheTrimBytes = maxOf(configuration.tileFileSystemCacheTrimBytes, max - max / 10)
    configuration.save(context.applicationContext, prefs)
    Log.d(TAG, "ZIP $compressedBytes bytes, tiles $tileBytes bytes, cache $existing bytes; limit $max bytes")
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
        val writer = MapCache.GuardedWriter()
        // Do not close the SQLite connection shared by active map providers.
        prepareCacheForImport(context, uri, writer)
        
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
                                if (Configuration.getInstance().osmdroidTileCache.usableSpace < MIB) {
                                    throw IOException("Not enough free storage to complete the import", e)
                                }
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
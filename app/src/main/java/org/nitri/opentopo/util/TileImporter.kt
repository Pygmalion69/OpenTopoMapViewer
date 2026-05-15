package org.nitri.opentopo.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import java.io.InputStream
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
): ImportResult = withContext(Dispatchers.IO) {
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

    try {
        val writer = SqlTileWriter()
        writer.refreshDb()
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
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
                                
                                // SqlTileWriter.saveFile reads the inputStream. 
                                // We provide a wrapper that doesn't close the ZipInputStream.
                                val entryWrapper = object : InputStream() {
                                    override fun read(): Int = zis.read()
                                    override fun read(b: ByteArray): Int = zis.read(b)
                                    override fun read(b: ByteArray, off: Int, len: Int): Int = zis.read(b, off, len)
                                    // Do not close zis
                                    override fun close() {}
                                }
                                
                                writer.saveFile(tileSource, index, entryWrapper, Long.MAX_VALUE)
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
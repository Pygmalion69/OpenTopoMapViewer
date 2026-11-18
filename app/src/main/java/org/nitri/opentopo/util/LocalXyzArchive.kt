package org.nitri.opentopo.util

import java.io.Closeable
import java.io.File
import java.util.zip.ZipFile

/** Indexed reads share exactly the same path rules as managed-package validation. */
internal class LocalXyzArchive(file: File, val sourceId: String = "local-xyz") : Closeable {
    private val zip = ZipFile(file)
    private val index = try { XyzZipIndex.read(zip) } catch (e: Exception) { zip.close(); throw e }
    private var closed = false
    val zoomLevels = index.tiles.keys.map { it.z }.toSortedSet()
    val minZoom = zoomLevels.first()
    val maxZoom = zoomLevels.last()
    val tileCount = index.tiles.size
    // Immutable coordinate snapshot; no tile IO is needed to plan a sparse-package viewport.
    val tiles: Set<XyzTile> = index.tiles.keys.toSet()

    @Synchronized fun read(z: Int, x: Int, y: Int): ByteArray? {
        if (closed) return null
        val entry = index.tiles[XyzTile(z, x, y)] ?: return null
        return zip.getInputStream(entry).use { input ->
            val bytes = input.readNBytesCompat(LocalPackageLimits.TILE_BYTES)
            bytes
        }
    }

    @Synchronized override fun close() {
        if (!closed) { closed = true; zip.close() }
    }
}

package org.nitri.opentopo.util

import java.io.Closeable
import java.io.File
import java.util.zip.ZipFile

/** Read-only indexed ZIP. The legacy directory prefix is a layout, not a source identity. */
internal class LocalXyzArchive(file: File) : Closeable {
    private val zip = ZipFile(file)
    private var closed = false
    val minZoom: Int
    val maxZoom: Int
    val tileCount: Int

    init {
        try {
            val zooms = zip.entries().asSequence().filter { !it.isDirectory }
                .mapNotNull { entry ->
                    val match = TILE.matchEntire(entry.name) ?: return@mapNotNull null
                    val (z, x, y) = match.destructured
                    val zoom = z.toIntOrNull() ?: return@mapNotNull null
                    if (zoom !in 0..29) return@mapNotNull null
                    val limit = 1L shl zoom
                    if ((x.toLongOrNull() ?: -1) !in 0 until limit ||
                        (y.toLongOrNull() ?: -1) !in 0 until limit) return@mapNotNull null
                    zoom
                }.toList()
            require(zooms.isNotEmpty()) { "No XYZ PNG tiles in local package" }
            minZoom = zooms.min()
            maxZoom = zooms.max()
            tileCount = zooms.size
        } catch (e: Exception) {
            zip.close()
            throw e
        }
    }

    @Synchronized fun read(z: Int, x: Int, y: Int): ByteArray? {
        if (closed || z !in minZoom..maxZoom) return null
        val entry = zip.getEntry("OpenTopoMap/$z/$x/$y.png") ?: return null
        return zip.getInputStream(entry).use { it.readBytes() }
    }

    @Synchronized override fun close() {
        if (!closed) {
            closed = true
            zip.close()
        }
    }

    private companion object {
        val TILE = Regex("OpenTopoMap/([0-9]+)/([0-9]+)/([0-9]+)\\.png")
    }
}

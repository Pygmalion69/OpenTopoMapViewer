package org.nitri.opentopo.util

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal data class XyzTile(val z: Int, val x: Int, val y: Int)
internal data class XyzZipIndex(val tiles: Map<XyzTile, ZipEntry>, val attributionEntry: ZipEntry?) {
    companion object {
        private val tilePath = Regex("(?:([^/]+)/)?([0-9]+)/([0-9]+)/([0-9]+)\\.png")
        fun read(zip: ZipFile, maxEntries: Int = LocalPackageLimits.ENTRIES): XyzZipIndex {
            val tiles = LinkedHashMap<XyzTile, ZipEntry>()
            var root: String? = null
            var attribution: ZipEntry? = null
            val names = HashSet<String>()
            var count = 0
            zip.entries().asSequence().forEach { entry ->
                require(++count <= maxEntries) { "Entry limit exceeded" }
                val name = entry.name
                require(name.length <= 1024 && !name.startsWith("/") && !name.contains('\\') &&
                    !name.contains(':') && name.none { it.code < 32 } &&
                    name.trimEnd('/').split('/').none { it == "." || it == ".." || it.isEmpty() }) {
                    "Unsafe ZIP path"
                }
                require(names.add(name)) { "Duplicate entry" }
                if (entry.isDirectory) return@forEach
                val match = tilePath.matchEntire(name)
                if (match == null) {
                    // PNG-looking or numeric tile paths must never be silently skipped.
                    require(!name.endsWith(".png", true) &&
                        name.split('/').none { it.matches(Regex("[0-9]+")) }) { "Invalid tile path" }
                    if (name == "attribution.txt") attribution = entry
                    return@forEach
                }
                val prefix = match.groupValues[1]
                require(root == null || root == prefix) { "Mixed tile roots" }
                root = prefix
                val z = match.groupValues[2].toIntOrNull() ?: error("Invalid zoom")
                require(z in 0..29) { "Unsupported zoom" }
                val x = match.groupValues[3].toIntOrNull() ?: error("Invalid x")
                val y = match.groupValues[4].toIntOrNull() ?: error("Invalid y")
                require(x.toLong() in 0 until (1L shl z) && y.toLong() in 0 until (1L shl z)) {
                    "Out of range XYZ"
                }
                require(tiles.put(XyzTile(z, x, y), entry) == null) { "Duplicate XYZ" }
            }
            require(tiles.isNotEmpty()) { "No XYZ tiles" }
            return XyzZipIndex(tiles, attribution)
        }
    }
}

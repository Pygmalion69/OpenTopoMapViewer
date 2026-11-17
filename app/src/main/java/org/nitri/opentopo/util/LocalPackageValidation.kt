package org.nitri.opentopo.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.Inflater

internal object LocalPackageLimits {
    const val ARCHIVE_BYTES = 512L * 1024 * 1024
    const val ENTRIES = 50_000
    const val TILE_BYTES = 2 * 1024 * 1024
    const val TOTAL_BYTES = 2L * 1024 * 1024 * 1024
    const val METADATA_BYTES = 64 * 1024
}

internal fun InputStream.readNBytesCompat(limit: Int, checkActive: () -> Unit = {}): ByteArray {
    val result = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        checkActive()
        val count = read(buffer, 0, minOf(buffer.size, limit - result.size() + 1))
        if (count < 0) break
        require(result.size().toLong() + count <= limit) { "Entry byte limit exceeded" }
        result.write(buffer, 0, count)
    }
    return result.toByteArray()
}

/** Checks all chunk CRCs and the complete zlib stream before the platform decoder runs. */
internal object TilePngValidator {
    fun validate(bytes: ByteArray) {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        val signature = ByteArray(8).also { input.readFully(it) }
        require(signature.contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))) { "Not PNG" }
        var header: ByteArray? = null
        var ended = false
        var sawData = false
        var dataEnded = false
        val compressed = ByteArrayOutputStream()
        while (input.available() > 0) {
            val length = input.readInt()
            require(length >= 0 && length <= input.available() - 8) { "Truncated PNG" }
            val type = ByteArray(4).also { input.readFully(it) }
            val name = String(type, Charsets.US_ASCII)
            require(type.all { it.toInt().toChar() in 'A'..'Z' || it.toInt().toChar() in 'a'..'z' })
            val payload = ByteArray(length).also { input.readFully(it) }
            val crc = CRC32().apply { update(type); update(payload) }
            require(input.readInt().toLong().and(0xffffffffL) == crc.value) { "PNG CRC mismatch" }
            require(!ended && (header != null || name == "IHDR")) { "Invalid PNG order" }
            when (name) {
                "IHDR" -> { require(header == null && length == 13); header = payload }
                "IDAT" -> { require(!dataEnded); sawData = true; compressed.write(payload) }
                "IEND" -> { require(length == 0 && sawData); ended = true }
                else -> {
                    if (sawData) dataEnded = true
                    require(name == "PLTE" || type[0].toInt().and(32) != 0) { "Unknown critical PNG chunk" }
                }
            }
        }
        require(ended)
        val h = DataInputStream(ByteArrayInputStream(requireNotNull(header)))
        require(h.readInt() == 256 && h.readInt() == 256) { "Expected 256 x 256" }
        val depth = h.readUnsignedByte()
        val color = h.readUnsignedByte()
        val channels = when (color) {
            0 -> { require(depth in listOf(1, 2, 4, 8, 16)); 1 }
            2 -> { require(depth == 8 || depth == 16); 3 }
            3 -> { require(depth in listOf(1, 2, 4, 8)); 1 }
            4 -> { require(depth == 8 || depth == 16); 2 }
            6 -> { require(depth == 8 || depth == 16); 4 }
            else -> error("Invalid PNG color")
        }
        require(h.readUnsignedByte() == 0 && h.readUnsignedByte() == 0)
        val interlace = h.readUnsignedByte()
        require(interlace in 0..1)
        val passes = if (interlace == 0) listOf(intArrayOf(0, 0, 1, 1)) else listOf(
            intArrayOf(0, 0, 8, 8), intArrayOf(4, 0, 8, 8), intArrayOf(0, 4, 4, 8),
            intArrayOf(2, 0, 4, 4), intArrayOf(0, 2, 2, 4), intArrayOf(1, 0, 2, 2),
            intArrayOf(0, 1, 1, 2))
        val inflater = Inflater()
        try {
            val encoded = compressed.toByteArray()
            inflater.setInput(encoded)
            val raw = ByteArray(600_000) // Max 16-bit RGBA, including Adam7 row filters.
            var count = 0
            while (!inflater.finished()) {
                require(count < raw.size)
                val n = inflater.inflate(raw, count, raw.size - count)
                require(n > 0 || inflater.finished()) { "Truncated PNG zlib" }
                count += n
            }
            require(inflater.remaining == 0) { "Trailing zlib bytes" }
            var position = 0
            passes.forEach { p ->
                val width = (256 - p[0] + p[2] - 1) / p[2]
                val rows = (256 - p[1] + p[3] - 1) / p[3]
                val rowBytes = (width * channels * depth + 7) / 8
                repeat(rows) {
                    require(position < count && raw[position].toInt() in 0..4) { "Invalid PNG row" }
                    position += rowBytes + 1
                }
            }
            require(position == count) { "Wrong PNG data length" }
        } finally { inflater.end() }
    }
}

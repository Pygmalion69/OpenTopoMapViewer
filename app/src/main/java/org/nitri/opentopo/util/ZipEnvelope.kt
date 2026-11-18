package org.nitri.opentopo.util

import java.io.File
import java.io.RandomAccessFile

/** Bound central-directory work before ZipFile allocates its native index. ZIP64 is unnecessary
 * within these product limits and is deliberately unsupported, as are split/encrypted archives. */
internal object ZipEnvelope {
    fun validate(file: File) = RandomAccessFile(file, "r").use { input ->
        fun u16(): Int = input.readUnsignedByte() or (input.readUnsignedByte() shl 8)
        fun u32(): Long = u16().toLong() or (u16().toLong() shl 16)
        val length = input.length()
        require(length >= 22)
        var eocd = length - 22
        val earliest = maxOf(0, length - 22 - 65535)
        var found = false
        while (eocd >= earliest) {
            input.seek(eocd)
            if (u32() == 0x06054b50L) {
                input.seek(eocd + 20)
                if (eocd + 22 + u16() == length) { found = true; break }
            }
            eocd--
        }
        require(found) { "Missing ZIP directory" }
        input.seek(eocd + 4)
        require(u16() == 0 && u16() == 0) { "Split ZIP unsupported" }
        val diskCount = u16()
        val count = u16()
        val size = u32()
        val offset = u32()
        require(count == diskCount && count in 1..LocalPackageLimits.ENTRIES) { "Entry limit exceeded" }
        require(size <= 32L * 1024 * 1024 && offset + size == eocd) { "Invalid ZIP directory" }
        input.seek(offset)
        repeat(count) {
            val start = input.filePointer
            require(start + 46 <= eocd && u32() == 0x02014b50L) { "Invalid ZIP entry" }
            input.seek(start + 8)
            require(u16().and(1) == 0) { "Encrypted ZIP unsupported" }
            require(u16() in listOf(0, 8)) { "Unsupported compression" }
            input.seek(start + 28)
            val nameSize = u16()
            val extraSize = u16()
            val commentSize = u16()
            require(u16() == 0) { "Split ZIP unsupported" }
            val next = start + 46 + nameSize + extraSize + commentSize
            require(next <= eocd)
            input.seek(next)
        }
        require(input.filePointer == eocd) { "ZIP entry count mismatch" }
    }
}

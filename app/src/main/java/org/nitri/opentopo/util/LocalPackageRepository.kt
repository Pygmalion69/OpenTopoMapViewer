package org.nitri.opentopo.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Properties
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipFile
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh

internal class InvalidLocalPackage(cause: Exception) : Exception(cause)

internal data class LocalPackage(
    val id: String,
    val displayName: String,
    val zoomLevels: List<Int>,
    val tileCount: Int,
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
    val attribution: String?
)

/** URI-free storage/validation. The caller supplies a stream, never a resolved document path. */
internal class LocalPackageRepository(
    private val root: File,
    private val decodePng: (ByteArray) -> Unit,
    private val archiveLimit: Long = LocalPackageLimits.ARCHIVE_BYTES,
    private val totalLimit: Long = LocalPackageLimits.TOTAL_BYTES
) {
    private val installed get() = File(root, "packages")
    private val staging get() = File(root, "staging")

    private suspend fun <T> locked(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(root.isDirectory || root.mkdirs()) { "Cannot create store" }
            RandomAccessFile(File(root, "store.lock"), "rw").use { file ->
                val lock = file.channel.tryLock() ?: error("Package store busy")
                lock.use { block() }
            }
        }
    }

    private fun recover() {
        check(installed.isDirectory || installed.mkdirs())
        check(staging.isDirectory || staging.mkdirs())
        // Only invoked while holding the process mutex AND the cross-process store lock.
        staging.listFiles()?.forEach { check(it.deleteRecursively()) { "Staging cleanup failed" } }
    }

    suspend fun list(): List<LocalPackage> = locked {
        recover()
        installed.listFiles().orEmpty().filter { validId(it.name) && it.isDirectory }
            .map { readMetadata(it) }.sortedWith(compareBy({ it.displayName }, { it.id }))
    }

    suspend fun lookup(id: String): LocalPackage? = locked {
        require(validId(id))
        val directory = File(installed, id)
        if (directory.isDirectory) readMetadata(directory) else null
    }

    suspend fun open(id: String): LocalXyzArchive? = locked {
        require(validId(id))
        val directory = File(installed, id)
        if (!directory.isDirectory) return@locked null
        readMetadata(directory)
        LocalXyzArchive(File(directory, "tiles.zip"), id).also {
            readers.getOrPut(directory.canonicalPath) { mutableListOf() }.apply {
                removeAll { reference -> reference.get() == null }
                add(java.lang.ref.WeakReference(it))
            }
        }
    }

    suspend fun remove(id: String) = locked {
        require(validId(id))
        recover()
        val directory = File(installed, id)
        // close() and read() synchronize on the archive: wait for an in-flight read.
        readers.remove(directory.canonicalPath)?.forEach { it.get()?.close() }
        if (directory.exists()) {
            val removed = File(staging, "removed-$id")
            check(directory.renameTo(removed)) { "Cannot retire package" }
            check(removed.deleteRecursively()) { "Cannot delete retired package" }
        }
    }

    /** Progress: copied bytes during copy; completed/total entries during validation. */
    suspend fun import(
        displayName: String,
        openDocument: () -> InputStream,
        progress: (validating: Boolean, completed: Long, total: Long) -> Unit = { _, _, _ -> }
    ): LocalPackage = locked {
        recover()
        val id = UUID.randomUUID().toString()
        val directory = File(staging, id)
        check(directory.mkdir()) { "Cannot create staging" }
        try {
            val archive = File(directory, "tiles.zip")
            openDocument().use { input ->
                FileOutputStream(archive).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytes = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer, 0, minOf(buffer.size.toLong(), archiveLimit - bytes + 1).toInt())
                        if (count < 0) break
                        bytes += count
                        require(bytes <= archiveLimit) { "Archive byte limit exceeded" }
                        output.write(buffer, 0, count)
                        progress(false, bytes, 0)
                    }
                    output.fd.sync()
                }
            }
            currentCoroutineContext().ensureActive()
            val metadata = try {
                ZipEnvelope.validate(archive)
                validate(archive, id, displayName, progress)
            }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { throw InvalidLocalPackage(e) }
            FileOutputStream(File(directory, "metadata.properties")).use { output ->
                properties(metadata).store(output, "OpenTopo local XYZ package v1")
                output.fd.sync()
            }
            currentCoroutineContext().ensureActive()
            val destination = File(installed, id)
            check(!destination.exists()) { "Package ID already installed" }
            check(directory.renameTo(destination)) { "Cannot publish package" }
            metadata // Publication is the commit point; cancellation cannot publish twice.
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun validate(
        file: File, id: String, name: String,
        progress: (Boolean, Long, Long) -> Unit
    ): LocalPackage = ZipFile(file).use { zip ->
        val index = XyzZipIndex.read(zip, LocalPackageLimits.ENTRIES)
        val tileNames = index.tiles.values.map { it.name }.toHashSet()
        var inflated = 0L
        var completed = 0L
        var attribution: String? = null
        zip.entries().asSequence().forEach { entry ->
            currentCoroutineContext().ensureActive()
            val tile = entry.name in tileNames
            val limit = if (tile) LocalPackageLimits.TILE_BYTES else LocalPackageLimits.METADATA_BYTES
            require(entry.size in 0..limit.toLong()) { "Declared entry size exceeds limit" }
            val context = currentCoroutineContext()
            val remaining = minOf(limit.toLong(), totalLimit - inflated).toInt()
            val bytes = zip.getInputStream(entry).use {
                it.readNBytesCompat(remaining) { context.ensureActive() }
            }
            inflated += bytes.size
            require(inflated <= totalLimit) { "Total inflated byte limit exceeded" }
            require(bytes.size.toLong() == entry.size) { "Truncated ZIP entry" }
            val crc = CRC32().apply { update(bytes) }
            require(crc.value == entry.crc) { "ZIP CRC mismatch" }
            if (entry.isDirectory) require(bytes.isEmpty())
            if (tile) {
                TilePngValidator.validate(bytes)
                decodePng(bytes)
            } else if (entry.name == index.attributionEntry?.name) {
                attribution = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString().trim().ifEmpty { null }
            }
            progress(true, ++completed, zip.size().toLong())
        }
        val tiles = index.tiles.keys
        fun longitude(x: Long, z: Int) = x.toDouble() / (1L shl z) * 360.0 - 180.0
        fun latitude(y: Long, z: Int) = atan(sinh(PI * (1.0 - 2.0 * y / (1L shl z)))) * 180.0 / PI
        LocalPackage(
            id, name.take(200).ifBlank { id }, tiles.map { it.z }.distinct().sorted(), tiles.size,
            tiles.minOf { longitude(it.x.toLong(), it.z) },
            tiles.minOf { latitude(it.y.toLong() + 1, it.z) },
            tiles.maxOf { longitude(it.x.toLong() + 1, it.z) },
            tiles.maxOf { latitude(it.y.toLong(), it.z) }, attribution
        )
    }

    private fun properties(p: LocalPackage) = Properties().apply {
        setProperty("version", "1")
        setProperty("id", p.id)
        setProperty("name", p.displayName)
        setProperty("zooms", p.zoomLevels.joinToString(","))
        setProperty("tiles", p.tileCount.toString())
        setProperty("west", p.west.toString()); setProperty("south", p.south.toString())
        setProperty("east", p.east.toString()); setProperty("north", p.north.toString())
        p.attribution?.let { setProperty("attribution", it) }
    }

    private fun readMetadata(directory: File): LocalPackage {
        val p = Properties().apply { File(directory, "metadata.properties").inputStream().use { load(it) } }
        require(p.getProperty("version") == "1" && p.getProperty("id") == directory.name)
        check(File(directory, "tiles.zip").isFile)
        return LocalPackage(directory.name, p.getProperty("name"), p.getProperty("zooms").split(',').map { it.toInt() },
            p.getProperty("tiles").toInt(), p.getProperty("west").toDouble(), p.getProperty("south").toDouble(),
            p.getProperty("east").toDouble(), p.getProperty("north").toDouble(), p.getProperty("attribution"))
    }

    companion object {
        private val mutex = Mutex()
        private val readers = mutableMapOf<String, MutableList<java.lang.ref.WeakReference<LocalXyzArchive>>>()
        private fun validId(id: String) = runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
    }
}

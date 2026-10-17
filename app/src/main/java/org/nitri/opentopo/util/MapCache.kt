package org.nitri.opentopo.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nitri.opentopo.R
import android.widget.Toast
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.ITileSource
import java.io.File
import java.io.InputStream

/** Serializes imports, downloader writes and purges. Old providers cannot write after a purge. */
object MapCache {
    internal val lock = Any()
    @Volatile var generation = 0L
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableClearing = MutableStateFlow(false)
    val clearing = mutableClearing.asStateFlow()
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()

    fun provider(context: Context, source: ITileSource) =
        MapTileProviderBasic(context, source, GuardedWriter())

    /** Replaces tile memory and request queues without rebuilding the view or its overlays. */
    fun resetProvider(map: MapView) {
        val source = map.tileProvider.tileSource
        val online = map.useDataConnection()
        map.setTileProvider(provider(map.context, source))
        map.setUseDataConnection(online)
        map.invalidate()
    }

    // Called on main. Application scope lets a confirmed operation finish across rotation/back.
    fun clear(context: Context) {
        if (mutableClearing.value) return
        mutableClearing.value = true
        val app = context.applicationContext
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                synchronized(lock) {
                    try {
                        purgeLocked()
                    } catch (e: Exception) {
                        Log.e("MapCache", "Could not clear map cache", e)
                        false
                    }
                }
            }
            // Recreate even on failure: the attempted purge invalidated the old writers.
            mutableRevision.value++
            mutableClearing.value = false
            Toast.makeText(app, if (success) R.string.clear_map_cache_success else
                R.string.clear_map_cache_failure, Toast.LENGTH_LONG).show()
        }
    }

    /** Caller holds [lock], including for the entire duration of a ZIP import. */
    internal fun purgeLocked(): Boolean {
        generation++
        return GuardedWriter().purgeActiveCache()
    }

    class GuardedWriter : SqlTileWriter() {
        private val createdGeneration = generation
        override fun saveFile(source: ITileSource, index: Long, stream: InputStream, expiration: Long?): Boolean =
            synchronized(lock) {
                if (createdGeneration != generation || mutableClearing.value) false
                else super.saveFile(source, index, stream, expiration)
            }

        fun purgeActiveCache(): Boolean {
            // SqlTileWriter shares a static connection. Never delete a stale/inactive database
            // if settings changed before providers were recreated.
            val db = getDb() ?: return false
            val expected = File(Configuration.getInstance().osmdroidTileCache, DATABASE_FILENAME)
            if (File(db.path).canonicalFile != expected.canonicalFile) return false
            return purgeCache()
        }
    }
}

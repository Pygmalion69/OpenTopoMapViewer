package org.nitri.opentopo

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.analytics.AnalyticsNames
import org.nitri.opentopo.analytics.AnalyticsProvider
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.util.Utils
import org.osmdroid.config.Configuration
import java.io.File


class CacheSettingsFragment : DialogFragment() {

    private var externalStorageState by mutableStateOf(false)
    private var tileCacheState by mutableStateOf("")
    private var cacheSizeState by mutableStateOf("")

    private var currentExternalStorage = false
    private var currentTileCache = ""
    private var currentCacheSize = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = activity ?: return super.onCreateDialog(savedInstanceState)
        AnalyticsProvider.get(fragmentActivity).trackScreen(
            AnalyticsNames.Screen.CACHE_SETTINGS,
            CacheSettingsFragment::class.java.simpleName
        )

        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(fragmentActivity.applicationContext)
        val cachePrefs: SharedPreferences = fragmentActivity.getSharedPreferences("cache_prefs", Context.MODE_PRIVATE)

        currentExternalStorage = cachePrefs.getBoolean(PREF_EXTERNAL_STORAGE,
            defaultPrefs.getBoolean(PREF_EXTERNAL_STORAGE, false)
        )
        currentTileCache = defaultPrefs.getString(PREF_TILE_CACHE, DEFAULT_TILE_CACHE) ?: DEFAULT_TILE_CACHE
        currentCacheSize = defaultPrefs.getInt(PREF_CACHE_SIZE, DEFAULT_CACHE_SIZE)

        // Initialize state
        externalStorageState = currentExternalStorage
        tileCacheState = currentTileCache
        cacheSizeState = currentCacheSize.toString()

        val composeView = ComposeView(fragmentActivity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    val currentStorageRoot = Utils.getOsmdroidBasePath(fragmentActivity.applicationContext, externalStorageState).absolutePath
                    val storageRootText = stringResource(id = R.string.storage_root, currentStorageRoot)
                    CacheSettingsContent(
                        storageRootText = storageRootText,
                        externalStorage = externalStorageState,
                        onExternalStorageChange = { externalStorageState = it },
                        tileCache = tileCacheState,
                        onTileCacheChange = { tileCacheState = it },
                        cacheSize = cacheSizeState,
                        onCacheSizeChange = { cacheSizeState = it }
                    )
                }
            }
        }

        // Set the ViewTree owners on the ComposeView
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        val builder = AlertDialog.Builder(fragmentActivity)
            .setView(composeView)
            .setPositiveButton(android.R.string.ok, null) // Listener set in onResume to prevent auto-dismiss
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> dismiss() }
            .setNeutralButton(R.string.reset, null) // Listener set in onResume

        val dialog = builder.create()
        setCancelable(false)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setOnShowListener {
            val decorView = dialog.window?.decorView
            if (decorView != null) {
                // Compose can crash with 'ViewTreeLifecycleOwner not found from AlertDialogLayout'
                // if we don't set these on the dialog's decor view.
                decorView.setViewTreeLifecycleOwner(this)
                decorView.setViewTreeViewModelStoreOwner(this)
                decorView.setViewTreeSavedStateRegistryOwner(this)
            }

            // Required for Compose TextFields inside AppCompat AlertDialog.
            // Without this, Compose receives focus but the soft keyboard may not appear.
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        return dialog
    }

    @Composable
    private fun CacheSettingsContent(
        storageRootText: String,
        externalStorage: Boolean,
        onExternalStorageChange: (Boolean) -> Unit,
        tileCache: String,
        onTileCacheChange: (String) -> Unit,
        cacheSize: String,
        onCacheSizeChange: (String) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = storageRootText,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.external_storage),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = externalStorage,
                    onCheckedChange = onExternalStorageChange
                )
            }
            OutlinedTextField(
                value = tileCache,
                onValueChange = onTileCacheChange,
                label = { Text(stringResource(id = R.string.tile_cache)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = cacheSize,
                onValueChange = onCacheSizeChange,
                label = { Text(stringResource(id = R.string.max_cache_size)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val dialog = dialog as? AlertDialog
        dialog?.let { ad ->
            ad.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                tileCacheState = DEFAULT_TILE_CACHE
                cacheSizeState = DEFAULT_CACHE_SIZE.toString()
            }
            ad.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                applySettings()
            }
        }
    }

    private fun applySettings() {
        val fragmentActivity = activity ?: return
        val appContext = fragmentActivity.applicationContext
        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        val cachePrefs: SharedPreferences = fragmentActivity.getSharedPreferences("cache_prefs", Context.MODE_PRIVATE)

        val newExternalStorage = externalStorageState
        val newTileCache = tileCacheState
        val newCacheSizeText = cacheSizeState
        val newCacheSize = newCacheSizeText.toIntOrNull() ?: -1

        if (newCacheSize == -1) {
            Log.e(TAG, "Invalid cache size: $newCacheSizeText")
        }

        if (newCacheSize > 0) {
            val newBasePath = Utils.getOsmdroidBasePath(appContext, newExternalStorage)
            val newCacheDir = File(newBasePath, newTileCache)

            // Validate the cache directory before applying.
            if (!Utils.isCacheDirValid(newCacheDir)) {
                Toast.makeText(appContext, R.string.storage_not_accessible, Toast.LENGTH_LONG).show()
                return
            }

            defaultPrefs.edit().apply {
                putString(PREF_TILE_CACHE, newTileCache)
                putInt(PREF_CACHE_SIZE, newCacheSize)
                apply()
            }
            cachePrefs.edit { putBoolean(PREF_EXTERNAL_STORAGE, newExternalStorage) }

            val configuration = Configuration.getInstance()
            configuration.osmdroidBasePath = newBasePath
            configuration.osmdroidTileCache = newCacheDir
            configuration.tileFileSystemCacheMaxBytes =
                newCacheSize.toLong() * 1024 * 1024
            // Remove any leftover sqlite files to avoid SqlTileWriter using them
            Utils.clearOsmdroidSqliteCache(appContext)
            configuration.save(appContext, defaultPrefs)
            val intent = Intent(ACTION_CACHE_CHANGED)
            val localBroadcastManager = LocalBroadcastManager.getInstance(
                fragmentActivity
            )
            if (currentExternalStorage != newExternalStorage || currentTileCache != newTileCache || currentCacheSize != newCacheSize) {
                localBroadcastManager.sendBroadcast(intent)
                fragmentActivity.finish()
            }
            dismiss()
        } else {
            Toast.makeText(context ?: fragmentActivity, R.string.invalid_cache_size, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val TAG = CacheSettingsFragment::class.java.simpleName

        const val DEFAULT_TILE_CACHE =
            "tiles"  // Hardcoded in org.osmdroid.config.DefaultConfigurationProvider
        const val DEFAULT_CACHE_SIZE = 600
        const val PREF_TILE_CACHE = "tile_cache"
        const val PREF_CACHE_SIZE = "cache_size"
        const val PREF_EXTERNAL_STORAGE = "external_storage"
        const val ACTION_CACHE_CHANGED = "cache_changed"
    }
}

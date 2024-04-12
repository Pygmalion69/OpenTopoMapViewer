package org.nitri.opentopo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.osmdroid.config.Configuration
import java.io.File

class CacheSettingsFragment : DialogFragment() {
    private lateinit var etTileCache: EditText
    private lateinit var etCacheSize: EditText
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        @SuppressLint("InflateParams") val view =
            inflater.inflate(R.layout.fragment_cache_settings, null)
        val prefs =
            requireActivity().getSharedPreferences(MapFragment.MAP_PREFS, Context.MODE_PRIVATE)
        val tvExternalStorageRoot = view.findViewById<TextView>(R.id.tvExternalStorageRoot)
        etTileCache = view.findViewById(R.id.etTileCache)
        etCacheSize = view.findViewById(R.id.etCacheSize)
        val storageRoot = Configuration.getInstance().osmdroidBasePath.absolutePath
        tvExternalStorageRoot.text = getString(R.string.storage_root, storageRoot)
        val currentTileCache = prefs.getString(PREF_TILE_CACHE, DEFAULT_TILE_CACHE)
        val currentCacheSize = prefs.getInt(PREF_CACHE_SIZE, DEFAULT_CACHE_SIZE)
        etTileCache.setText(currentTileCache)
        etCacheSize.setText(currentCacheSize.toString())
        builder.setView(view)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                val newTileCache = etTileCache.text.toString()
                val newCacheSize = etCacheSize.text.toString().toInt()
                val editor = prefs.edit()
                editor.putString(PREF_TILE_CACHE, newTileCache)
                editor.putInt(PREF_CACHE_SIZE, newCacheSize)
                editor.apply()
                val cacheDir = File("$storageRoot/$newTileCache")
                if (cacheDir.mkdirs()) {
                    Log.i(TAG, "Tile cache created: $newTileCache")
                }
                Configuration.getInstance().osmdroidTileCache = cacheDir
                Configuration.getInstance().tileFileSystemCacheMaxBytes =
                    newCacheSize.toLong() * 1024 * 1024
                Configuration.getInstance().save(requireActivity().applicationContext, prefs)
                if (newTileCache != currentTileCache || newCacheSize != currentCacheSize) restart()
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> dismiss() }
            .setNeutralButton(R.string.reset) { _: DialogInterface?, _: Int -> }
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    /**
     * Restart the rude way
     */
    private fun restart() {
        requireActivity().finish()
        startActivity(requireActivity().intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requireActivity().overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                0,
                0,
                Color.TRANSPARENT
            )
        } else {
            requireActivity().overridePendingTransition(0, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        val dialog = dialog as AlertDialog?
        if (dialog != null) {
            val positiveButton = dialog.getButton(Dialog.BUTTON_NEUTRAL)
            positiveButton.setOnClickListener {
                etTileCache.setText(DEFAULT_TILE_CACHE)
                etCacheSize.setText(DEFAULT_CACHE_SIZE.toString())
            }
        }
    }

    companion object {
        private val TAG = CacheSettingsFragment::class.java.simpleName

        // Hardcoded in org.osmdroid.config.DefaultConfigurationProvider
        private const val DEFAULT_TILE_CACHE = "osmdroid/tiles"
        private const val DEFAULT_CACHE_SIZE = 600
        private const val PREF_TILE_CACHE = "tile_cache"
        private const val PREF_CACHE_SIZE = "cache_size"
    }
}
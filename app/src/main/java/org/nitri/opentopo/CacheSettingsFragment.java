package org.nitri.opentopo;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.osmdroid.config.Configuration;

import java.io.File;

public class CacheSettingsFragment extends DialogFragment {

    private static final String TAG = CacheSettingsFragment.class.getSimpleName();

    // Hardcoded in org.osmdroid.config.DefaultConfigurationProvider
    private static final String DEFAULT_TILE_CACHE = "osmdroid/tiles";
    private static final int DEFAULT_CACHE_SIZE = 600;

    private final static String PREF_TILE_CACHE = "tile_cache";
    private final static String PREF_CACHE_SIZE = "cache_size";

    private EditText etTileCache;
    private EditText etCacheSize;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_cache_settings, null);

        SharedPreferences prefs = requireActivity().getSharedPreferences(MapFragment.MAP_PREFS, Context.MODE_PRIVATE);

        TextView tvExternalStorageRoot = view.findViewById(R.id.tvExternalStorageRoot);

        etTileCache = view.findViewById(R.id.etTileCache);
        etCacheSize = view.findViewById(R.id.etCacheSize);

        String storageRoot = Configuration.getInstance().getOsmdroidBasePath().getAbsolutePath();
        tvExternalStorageRoot.setText(getString(R.string.storage_root, storageRoot));

        String currentTileCache = prefs.getString(PREF_TILE_CACHE, DEFAULT_TILE_CACHE);
        int currentCacheSize = prefs.getInt(PREF_CACHE_SIZE, DEFAULT_CACHE_SIZE);

        etTileCache.setText(currentTileCache);
        etCacheSize.setText(String.valueOf(currentCacheSize));

        builder.setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    String newTileCache = etTileCache.getText().toString();
                    int newCacheSize = Integer.parseInt(etCacheSize.getText().toString());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(PREF_TILE_CACHE, newTileCache);
                    editor.putInt(PREF_CACHE_SIZE, newCacheSize);
                    editor.apply();
                    File cacheDir = new File(storageRoot + "/" + newTileCache);
                    if (cacheDir.mkdirs()) {
                        Log.i(TAG, "Tile cache created: " + newTileCache);
                    }
                    Configuration.getInstance().setOsmdroidTileCache(cacheDir);
                    Configuration.getInstance().setTileFileSystemCacheMaxBytes((long) newCacheSize * 1024 * 1024);
                    Configuration.getInstance().save(requireActivity().getApplicationContext(), prefs);
                    if (!newTileCache.equals(currentTileCache) || newCacheSize != currentCacheSize)
                        restart();
                    dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dismiss())
                .setNeutralButton(R.string.reset, (dialog, id) -> {
                    // NOP: override in onResume() (do not dismiss dialog)
                });


        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    /**
     * Restart the rude way
     */
    private void restart() {
        requireActivity().finish();
        startActivity(requireActivity().getIntent());
        requireActivity().overridePendingTransition(0, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button positiveButton = dialog.getButton(Dialog.BUTTON_NEUTRAL);
            positiveButton.setOnClickListener(v -> {
                etTileCache.setText(DEFAULT_TILE_CACHE);
                etCacheSize.setText(String.valueOf(DEFAULT_CACHE_SIZE));
            });
        }
    }

}

package org.nitri.opentopo

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.nitri.opentopo.adapter.MarkerListAdapter
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.util.GpxMarkerExporter
import org.nitri.opentopo.util.GpxMarkerImporter
import org.nitri.opentopo.view.MarkerEditorDialog
import org.nitri.opentopo.viewmodel.MarkerViewModel

class MarkerListActivity : AppCompatActivity() {
    private val markerViewModel: MarkerViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: MarkerListAdapter

    private var markers: List<MarkerModel> = emptyList()
    private val selectedMarkerIds = mutableSetOf<Int>()
    private var actionMode: ActionMode? = null

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri ->
            uri?.let { exportSelectedMarkers(it) }
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importMarkers(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_list)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.markers)

        recyclerView = findViewById(R.id.markerRecyclerView)
        emptyView = findViewById(R.id.emptyView)

        adapter = MarkerListAdapter(
            onClick = { marker ->
                if (actionMode != null) {
                    toggleSelection(marker.id)
                } else {
                    openMarkerEditor(marker)
                }
            },
            onLongClick = { marker ->
                if (actionMode == null) {
                    actionMode = startSupportActionMode(selectionActionModeCallback)
                }
                toggleSelection(marker.id)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        markerViewModel.markers.observe(this) { markerItems ->
            markers = markerItems.sortedBy { it.seq }
            adapter.submitList(markers)
            emptyView.visibility = if (markers.isEmpty()) View.VISIBLE else View.GONE

            selectedMarkerIds.retainAll(markers.map { it.id }.toSet())
            if (actionMode != null && selectedMarkerIds.isEmpty()) {
                actionMode?.finish()
            } else {
                updateSelectionUi()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_marker_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_import_markers -> {
                importLauncher.launch(arrayOf("*/*"))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val selectionActionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_marker_selection, menu)
            updateSelectionUi()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_select_all_markers -> {
                    selectedMarkerIds.clear()
                    selectedMarkerIds.addAll(markers.map { it.id })
                    updateSelectionUi()
                    return true
                }
                R.id.action_export_selected_markers -> {
                    if (selectedMarkerIds.isEmpty()) {
                        Toast.makeText(this@MarkerListActivity, R.string.no_markers_selected, Toast.LENGTH_SHORT).show()
                    } else {
                        exportLauncher.launch(DEFAULT_EXPORT_FILENAME)
                    }
                    return true
                }
                R.id.action_delete_selected_markers -> {
                    if (selectedMarkerIds.isEmpty()) {
                        Toast.makeText(this@MarkerListActivity, R.string.no_markers_selected, Toast.LENGTH_SHORT).show()
                    } else {
                        showDeleteSelectedConfirmation()
                    }
                    return true
                }
                else -> return false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            selectedMarkerIds.clear()
            actionMode = null
            updateSelectionUi()
        }
    }

    private fun toggleSelection(markerId: Int) {
        if (selectedMarkerIds.contains(markerId)) {
            selectedMarkerIds.remove(markerId)
        } else {
            selectedMarkerIds.add(markerId)
        }

        if (selectedMarkerIds.isEmpty()) {
            actionMode?.finish()
        } else {
            updateSelectionUi()
        }
    }

    private fun updateSelectionUi() {
        val selectedCount = selectedMarkerIds.size
        actionMode?.title = getString(R.string.markers_selected_count, selectedCount)
        adapter.setSelection(actionMode != null, selectedMarkerIds)
    }

    private fun openMarkerEditor(marker: MarkerModel) {
        MarkerEditorDialog.show(
            context = this,
            markerModel = marker.copy(),
            onUpdate = { updated -> markerViewModel.updateMarker(updated) },
            onDelete = { toDelete -> markerViewModel.removeMarker(toDelete.id) }
        )
    }

    private fun showDeleteSelectedConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete))
            .setMessage(getString(R.string.delete_selected_markers_message, selectedMarkerIds.size))
            .setPositiveButton(R.string.delete) { _, _ ->
                markerViewModel.removeMarkers(selectedMarkerIds.toList())
                actionMode?.finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportSelectedMarkers(uri: Uri) {
        val markersToExport = markers.filter { selectedMarkerIds.contains(it.id) }
        if (markersToExport.isEmpty()) {
            Toast.makeText(this, R.string.no_markers_selected, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    GpxMarkerExporter().export(markersToExport, outputStream)
                } ?: error("Output stream unavailable")
            }.onSuccess {
                Toast.makeText(this@MarkerListActivity, R.string.markers_export_success, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this@MarkerListActivity, R.string.markers_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importMarkers(uri: Uri) {
        lifecycleScope.launch {
            runCatching {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val currentMaxSeq = markers.maxOfOrNull { it.seq } ?: 0
                    GpxMarkerImporter().import(inputStream, currentMaxSeq)
                } ?: error("Input stream unavailable")
            }.onSuccess { result ->
                if (result.markers.isEmpty()) {
                    Toast.makeText(
                        this@MarkerListActivity,
                        R.string.import_no_valid_waypoints,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@onSuccess
                }

                markerViewModel.addMarkers(result.markers)
                if (result.skippedCount > 0) {
                    Toast.makeText(
                        this@MarkerListActivity,
                        getString(R.string.import_success_with_skipped, result.markers.size, result.skippedCount),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MarkerListActivity,
                        getString(R.string.import_success_count, result.markers.size),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Toast.makeText(this@MarkerListActivity, R.string.markers_import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val DEFAULT_EXPORT_FILENAME = "opentopomap-markers.gpx"
    }
}

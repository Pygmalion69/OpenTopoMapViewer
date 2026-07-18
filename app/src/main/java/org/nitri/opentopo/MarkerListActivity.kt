package org.nitri.opentopo

import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.nitri.opentopo.analytics.AnalyticsNames
import org.nitri.opentopo.analytics.AnalyticsProvider
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.util.GpxMarkerExporter
import org.nitri.opentopo.util.GpxMarkerImporter
import org.nitri.opentopo.view.MarkerEditorDialog
import org.nitri.opentopo.viewmodel.MarkerViewModel
import java.util.Locale

class MarkerListActivity : ComponentActivity() {
    private val markerViewModel: MarkerViewModel by viewModels()

    private var idsToExport: Set<Int> = emptySet()

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri ->
            uri?.let { exportSelectedMarkers(it, idsToExport) }
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importMarkers(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        AnalyticsProvider.get(this).trackScreen(
            AnalyticsNames.Screen.MARKERS,
            MarkerListActivity::class.java.simpleName
        )

        setContent {
            OpenTopoTheme(dynamicColor = false) {
                MarkerListScreen(
                    markerViewModel = markerViewModel,
                    onBack = { finish() },
                    onImport = { importLauncher.launch(arrayOf("*/*")) },
                    onExport = { selectedIds ->
                        idsToExport = selectedIds
                        exportLauncher.launch(DEFAULT_EXPORT_FILENAME)
                    },
                    onDeleteSelected = { selectedIds ->
                        showDeleteSelectedConfirmation(selectedIds)
                    },
                    onOpenEditor = { marker -> openMarkerEditor(marker) }
                )
            }
        }
    }

    private fun openMarkerEditor(marker: MarkerModel) {
        MarkerEditorDialog.show(
            context = this,
            markerModel = marker.copy(),
            onUpdate = { updated -> markerViewModel.updateMarker(updated) },
            onDelete = { toDelete ->
                markerViewModel.removeMarker(toDelete.id)
                AnalyticsProvider.get(this).trackMarkersDeleted(1)
            }
        )
    }

    private fun showDeleteSelectedConfirmation(selectedIds: Set<Int>) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete))
            .setMessage(getString(R.string.delete_selected_markers_message, selectedIds.size))
            .setPositiveButton(R.string.delete) { _, _ ->
                val deletedCount = selectedIds.size
                markerViewModel.removeMarkers(selectedIds.toList())
                AnalyticsProvider.get(this@MarkerListActivity).trackMarkersDeleted(deletedCount)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also {
                it.requestWindowFeature(Window.FEATURE_NO_TITLE)
                it.show()
            }
    }

    private fun exportSelectedMarkers(uri: Uri, selectedIds: Set<Int>) {
        lifecycleScope.launch {
            val allMarkers = markerViewModel.markers.value ?: emptyList()
            val markersToExport = allMarkers.filter { selectedIds.contains(it.id) }
            if (markersToExport.isEmpty()) {
                Toast.makeText(this@MarkerListActivity, R.string.no_markers_selected, Toast.LENGTH_SHORT).show()
                return@launch
            }

            runCatching {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    GpxMarkerExporter().export(markersToExport, outputStream)
                } ?: error("Output stream unavailable")
            }.onSuccess {
                AnalyticsProvider.get(this@MarkerListActivity).trackMarkersExported(markersToExport.size)
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
                    val allMarkers = markerViewModel.markers.value ?: emptyList()
                    val currentMaxSeq = allMarkers.maxOfOrNull { it.seq } ?: 0
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
                AnalyticsProvider.get(this@MarkerListActivity).trackMarkersImported(
                    importedCount = result.markers.size,
                    skippedCount = result.skippedCount
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerListScreen(
    markerViewModel: MarkerViewModel,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onExport: (Set<Int>) -> Unit,
    onDeleteSelected: (Set<Int>) -> Unit,
    onOpenEditor: (MarkerModel) -> Unit
) {
    val markerItems by markerViewModel.markers.observeAsState(emptyList())
    val markers = remember(markerItems) { markerItems.sortedBy { it.seq } }

    // Using remember instead of rememberSaveable for Set<Int> to avoid potential serialization issues.
    // Selection state is not critical enough to require persistence across process death in this case.
    var selectedIds by remember { mutableStateOf(emptySet<Int>()) }

    // Selection mode is derived from selectedIds.
    // Clearing the final selected marker automatically exits selection mode.
    val selectionMode = selectedIds.isNotEmpty()

    LaunchedEffect(markers) {
        val validIds = markers.mapTo(mutableSetOf()) { it.id }
        selectedIds = selectedIds.intersect(validIds)
    }

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedIds.size,
                    totalCount = markers.size,
                    onCloseSelection = { selectedIds = emptySet() },
                    onExport = { onExport(selectedIds) },
                    onDelete = { onDeleteSelected(selectedIds) },
                    onSelectAll = { selectedIds = markers.map { it.id }.toSet() }
                )
            } else {
                NormalTopAppBar(
                    onBack = onBack,
                    onImport = onImport
                )
            }
        }
    ) { innerPadding ->
        if (markers.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding
            ) {
                items(
                    items = markers,
                    key = { it.id }
                ) { marker ->
                    MarkerRow(
                        marker = marker,
                        selectionMode = selectionMode,
                        selected = selectedIds.contains(marker.id),
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (selectedIds.contains(marker.id)) {
                                    selectedIds - marker.id
                                } else {
                                    selectedIds + marker.id
                                }
                            } else {
                                onOpenEditor(marker)
                            }
                        },
                        onLongClick = {
                            if (selectionMode) {
                                // Toggle selection on long press when already in selection mode
                                selectedIds = if (selectedIds.contains(marker.id)) {
                                    selectedIds - marker.id
                                } else {
                                    selectedIds + marker.id
                                }
                            } else {
                                selectedIds = setOf(marker.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalTopAppBar(onBack: () -> Unit, onImport: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.markers)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            IconButton(onClick = onImport) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_gpx),
                    contentDescription = stringResource(R.string.import_gpx)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    totalCount: Int,
    onCloseSelection: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.markers_selected_count, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onCloseSelection) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            IconButton(onClick = onExport) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export_selected))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_selected))
            }
            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (selectedCount < totalCount) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.select_all)) },
                        onClick = {
                            onSelectAll()
                            showMenu = false
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarkerRow(
    marker: MarkerModel,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val displayName = marker.name.ifBlank {
        stringResource(
            R.string.marker_fallback_coordinates,
            marker.latitude,
            marker.longitude
        )
    }
    val coordinates = String.format(
        Locale.US,
        "%.5f, %.5f",
        marker.latitude,
        marker.longitude
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = coordinates,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_markers_available),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

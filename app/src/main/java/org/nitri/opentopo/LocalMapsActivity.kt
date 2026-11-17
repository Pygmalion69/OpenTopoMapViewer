package org.nitri.opentopo

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.util.LocalPackage
import org.nitri.opentopo.util.LocalPackageRepository
import java.io.File
import java.io.IOException

class LocalMapsActivity : AppCompatActivity() {
    private val model: LocalMapsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenTopoTheme(dynamicColor = false) {
            LocalMapsScreen(model, onBack = { finish() })
        } }
    }
}

internal data class LocalMapsState(
    val packages: List<LocalPackage> = emptyList(),
    val busy: Boolean = true,
    val importing: Boolean = false,
    val validating: Boolean = false,
    val completed: Long = 0,
    val total: Long = 0,
    val message: Int? = null
)

internal class DocumentUnavailable(cause: Exception) : IOException(cause)

internal class LocalMapsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalPackageRepository(
        File(application.noBackupFilesDir, "local-xyz"),
        decodePng = { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalArgumentException("PNG decoder failed")
            try { require(bitmap.width == 256 && bitmap.height == 256) } finally { bitmap.recycle() }
        }
    )
    private val mutableState = MutableStateFlow(LocalMapsState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    init { refresh() }

    private fun refresh() {
        job = viewModelScope.launch {
            try { mutableState.update { it.copy(packages = repository.list()) } }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("LocalMaps", "Cannot read catalog", e)
                mutableState.update { it.copy(message = R.string.local_storage_error) }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun import(uri: Uri) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, importing = true, completed = 0, total = 0, validating = false, message = null) }
        job = viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            try {
                withContext(Dispatchers.IO) {
                    // The picker grant survives configuration changes. Do not acquire or
                    // release persistent grants owned by the background importer.
                    val name = try {
                        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            if (it.moveToFirst()) it.getString(0) else null
                        }
                    } catch (_: Exception) { null }
                    repository.import(name ?: getApplication<Application>().getString(R.string.local_unnamed), {
                        try {
                            val input = resolver.openInputStream(uri) ?: throw IOException("Provider returned no stream")
                            object : java.io.FilterInputStream(input) {
                                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                                    try { super.read(buffer, offset, length) }
                                    catch (e: IOException) { throw DocumentUnavailable(e) }
                            }
                        } catch (e: Exception) { throw DocumentUnavailable(e) }
                    }) { validating, completed, total ->
                        mutableState.update { it.copy(validating = validating, completed = completed, total = total) }
                    }
                }
                mutableState.update { it.copy(packages = repository.list(), message = R.string.local_imported) }
            } catch (e: CancellationException) {
                mutableState.update { it.copy(message = R.string.local_cancelled) }
                throw e
            } catch (e: Exception) {
                Log.e("LocalMaps", "Import failed", e)
                val message = when (e) {
                    is DocumentUnavailable -> R.string.local_open_error
                    is org.nitri.opentopo.util.InvalidLocalPackage -> R.string.local_invalid_error
                    else -> R.string.local_storage_error
                }
                mutableState.update { it.copy(message = message) }
            } finally {
                // Cancellation may race the atomic publication point. Always reload the
                // committed catalog before enabling another submission.
                withContext(NonCancellable) {
                    runCatching { repository.list() }.onSuccess { packages ->
                        mutableState.update { it.copy(packages = packages) }
                    }.onFailure { Log.e("LocalMaps", "Cannot refresh catalog after import", it) }
                }
                mutableState.update { it.copy(busy = false, importing = false) }
            }
        }
    }

    fun cancel() { if (mutableState.value.importing) job?.cancel() }

    fun remove(id: String) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, message = null) }
        job = viewModelScope.launch {
            try {
                repository.remove(id)
                mutableState.update { it.copy(packages = repository.list()) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("LocalMaps", "Removal failed", e)
                mutableState.update { it.copy(message = R.string.local_storage_error) }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalMapsScreen(model: LocalMapsViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsState()
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picking = false
        uri?.let(model::import)
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.local_maps)) }, navigationIcon = {
            TextButton(onClick = onBack) { Text(stringResource(R.string.local_back)) }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Button(enabled = !state.busy && !picking, onClick = {
                picking = true
                picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            }) { Text(stringResource(R.string.local_import)) }
            Text(stringResource(R.string.local_explanation))
            state.message?.let { Text(stringResource(it), modifier = Modifier.padding(vertical = 8.dp)) }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.importing) {
                    Text(if (state.validating) stringResource(R.string.local_validating, state.completed, state.total)
                        else stringResource(R.string.local_copying, state.completed / 1024))
                    TextButton(onClick = model::cancel) { Text(stringResource(android.R.string.cancel)) }
                }
            } else if (state.packages.isEmpty()) {
                Text(stringResource(R.string.local_empty), Modifier.padding(vertical = 16.dp))
            }
            LazyColumn {
                items(state.packages, key = { it.id }) { p ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(p.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.local_details, p.tileCount, p.zoomLevels.joinToString(", ")))
                        // Full ID disambiguates even packages whose name and coverage are identical.
                        Text(p.id, style = MaterialTheme.typography.bodySmall)
                        p.attribution?.let { Text(it) }
                        TextButton(enabled = !state.busy, onClick = { pendingRemoval = p.id }) {
                            Text(stringResource(R.string.local_remove))
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    state.packages.firstOrNull { it.id == pendingRemoval }?.let { p ->
        AlertDialog(onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.local_remove)) },
            text = { Text(stringResource(R.string.local_remove_confirm, p.displayName, p.id)) },
            confirmButton = { TextButton(onClick = { pendingRemoval = null; model.remove(p.id) }) {
                Text(stringResource(R.string.local_remove))
            } },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) {
                Text(stringResource(android.R.string.cancel))
            } })
    }
}

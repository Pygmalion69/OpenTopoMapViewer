package org.nitri.opentopo.view

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.MapFragment
import org.nitri.opentopo.R
import org.nitri.opentopo.model.PlaceSearchResult
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.viewmodel.PlaceSearchUiState
import org.nitri.opentopo.viewmodel.PlaceSearchViewModel

class PlaceSearchDialogFragment : DialogFragment() {

    private val viewModel: PlaceSearchViewModel by viewModels {
        val focusLon = arguments?.getDouble(ARG_FOCUS_LON)
        val focusLat = arguments?.getDouble(ARG_FOCUS_LAT)
        PlaceSearchViewModel.Factory(
            clientProvider = {
                if (isAdded) {
                    (activity as? MapFragment.OnFragmentInteractionListener)?.getOpenRouteServiceClient()
                } else {
                    null
                }
            },
            focusLon = focusLon,
            focusLat = focusLat
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    PlaceSearchDialogContent(
                        viewModel = viewModel,
                        isInitialLaunch = savedInstanceState == null,
                        onResultSelected = { result ->
                            setFragmentResult(
                                REQUEST_KEY,
                                Bundle().apply {
                                    putDouble(KEY_LATITUDE, result.latitude)
                                    putDouble(KEY_LONGITUDE, result.longitude)
                                    putString(KEY_NAME, result.name)
                                    putString(KEY_LABEL, result.label)
                                }
                            )
                            dismiss()
                        },
                        onDismiss = { dismiss() }
                    )
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(composeView)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setOnShowListener {
                window?.decorView?.let { decorView ->
                    decorView.setViewTreeLifecycleOwner(this@PlaceSearchDialogFragment)
                    decorView.setViewTreeViewModelStoreOwner(this@PlaceSearchDialogFragment)
                    decorView.setViewTreeSavedStateRegistryOwner(this@PlaceSearchDialogFragment)
                }
            }
        }
    }

    companion object {
        const val TAG = "PlaceSearchDialogFragment"
        const val REQUEST_KEY = "place_search_request_key"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_NAME = "name"
        const val KEY_LABEL = "label"

        private const val ARG_FOCUS_LON = "focus_lon"
        private const val ARG_FOCUS_LAT = "focus_lat"

        fun newInstance(focusLon: Double, focusLat: Double): PlaceSearchDialogFragment {
            return PlaceSearchDialogFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_FOCUS_LON, focusLon)
                    putDouble(ARG_FOCUS_LAT, focusLat)
                }
            }
        }
    }
}

@Composable
private fun PlaceSearchDialogContent(
    viewModel: PlaceSearchViewModel,
    isInitialLaunch: Boolean,
    onResultSelected: (PlaceSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (isInitialLaunch) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.search_place),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_place)) },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                )
            )

            Spacer(Modifier.height(8.dp))

            when (val state = uiState) {
                is PlaceSearchUiState.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                is PlaceSearchUiState.QueryTooShort -> {
                    Text(
                        text = stringResource(R.string.search_min_chars),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                is PlaceSearchUiState.Empty -> {
                    Text(
                        text = stringResource(R.string.no_matching_places),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                is PlaceSearchUiState.Error -> {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(R.string.unable_to_search_places),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = { viewModel.retry() },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                is PlaceSearchUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        items(
                            items = state.results,
                            key = { it.stableId }
                        ) { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onResultSelected(result) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = result.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

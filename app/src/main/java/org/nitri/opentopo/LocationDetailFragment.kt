package org.nitri.opentopo

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.util.Utils
import org.nitri.opentopo.util.Utils.elevationFromNmea
import org.nitri.opentopo.viewmodel.LocationViewModel

class LocationDetailFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = requireActivity()
        val locationViewModel = ViewModelProvider(fragmentActivity)[LocationViewModel::class.java]

        val composeView = ComposeView(fragmentActivity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    LocationDetailContent(locationViewModel)
                }
            }
        }

        // Set the ViewTree owners on the ComposeView
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        val builder = AlertDialog.Builder(fragmentActivity)
        builder.setView(composeView)
            .setPositiveButton(R.string.close) { _: DialogInterface?, _: Int -> dismiss() }
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setOnShowListener {
            val decorView = dialog.window?.decorView
            if (decorView != null) {
                // This prevents: ViewTreeLifecycleOwner not found from AlertDialogLayout
                // and therefore must not be removed.
                decorView.setViewTreeLifecycleOwner(this)
                decorView.setViewTreeViewModelStoreOwner(this)
                decorView.setViewTreeSavedStateRegistryOwner(this)
            }
        }

        return dialog
    }

    @Composable
    private fun LocationDetailContent(locationViewModel: LocationViewModel) {
        val location by locationViewModel.currentLocation.observeAsState()
        val nmea by locationViewModel.currentNmea.observeAsState()

        val latitude = location?.let { String.format("%.5f", it.latitude) } ?: stringResource(R.string.unknown_symbol)
        val longitude = location?.let { String.format("%.5f", it.longitude) } ?: stringResource(R.string.unknown_symbol)

        val elevationValue = nmea?.let { elevationFromNmea(it) }
        val elevation = if (elevationValue != null && elevationValue != Utils.NO_ELEVATION_VALUE.toDouble()) {
            String.format("%.1f m", elevationValue)
        } else {
            stringResource(R.string.unknown_symbol)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LocationDetailRow(stringResource(R.string.label_latitude), latitude)
            LocationDetailRow(stringResource(R.string.label_longitude), longitude)
            LocationDetailRow(stringResource(R.string.label_elevation), elevation)
        }
    }

    @Composable
    private fun LocationDetailRow(label: String, value: String) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, Modifier.weight(1f), textAlign = TextAlign.End)
            Spacer(Modifier.width(16.dp))
            Text(value, Modifier.weight(1f), textAlign = TextAlign.Start)
        }
    }
}

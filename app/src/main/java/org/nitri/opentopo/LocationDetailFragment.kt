package org.nitri.opentopo

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.CancellationException
import org.nitri.opentopo.ors.Elevation
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.util.Utils
import org.nitri.opentopo.util.Utils.elevationFromNmea
import org.nitri.opentopo.viewmodel.LocationViewModel
import org.nitri.ors.OrsClient
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

class LocationDetailFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = requireActivity()
        val locationViewModel = ViewModelProvider(fragmentActivity)[LocationViewModel::class.java]

        val orsClient =
            (fragmentActivity as? MapFragment.OnFragmentInteractionListener)
                ?.getOpenRouteServiceClient()

        val composeView = ComposeView(fragmentActivity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    LocationDetailContent(locationViewModel, orsClient)
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
    private fun LocationDetailContent(
        locationViewModel: LocationViewModel,
        orsClient: OrsClient?
    ) {
        val location by locationViewModel.currentLocation.observeAsState()
        val nmea by locationViewModel.currentNmea.observeAsState()

        val latitude = location?.let { String.format(Locale.getDefault(), "%.5f", it.latitude) } ?: stringResource(R.string.unknown_symbol)
        val longitude = location?.let { String.format(Locale.getDefault(), "%.5f", it.longitude) } ?: stringResource(R.string.unknown_symbol)

        val nmeaElevationValue = nmea?.let { elevationFromNmea(it) }
        val nmeaElevation = if (nmeaElevationValue != null && nmeaElevationValue != Utils.NO_ELEVATION_VALUE.toDouble()) {
            String.format(LocalLocale.current.platformLocale, "%.1f m", nmeaElevationValue)
        } else {
            stringResource(R.string.unknown_symbol)
        }

        var orsRequestCoordinates by remember {
            mutableStateOf<Pair<Double, Double>?>(null)
        }

        LaunchedEffect(location) {
            val current = location ?: return@LaunchedEffect
            if (orsRequestCoordinates == null) {
                orsRequestCoordinates = current.longitude to current.latitude
            }
        }

        val orsElevationState by produceState<OrsElevationState>(
            initialValue = OrsElevationState.Unavailable,
            orsClient,
            orsRequestCoordinates
        ) {
            val client = orsClient
            val coordinates = orsRequestCoordinates

            if (client == null || coordinates == null) {
                value = OrsElevationState.Unavailable
                return@produceState
            }

            value = OrsElevationState.Loading

            value = try {
                val elevation = Elevation(client).getPointElevation(
                    longitude = coordinates.first,
                    latitude = coordinates.second
                )
                OrsElevationState.Success(elevation)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(
                    LocationDetailFragment::class.java.simpleName,
                    "Unable to retrieve ORS elevation",
                    exception
                )
                OrsElevationState.Error
            }
        }

        val orsElevation = when (val state = orsElevationState) {
            OrsElevationState.Unavailable -> null
            OrsElevationState.Loading -> stringResource(R.string.loading)
            is OrsElevationState.Success ->
                String.format(LocalLocale.current.platformLocale, "%.1f m", state.meters)
            OrsElevationState.Error -> stringResource(R.string.unknown_symbol)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LocationDetailRow(stringResource(R.string.label_latitude), latitude)
            LocationDetailRow(stringResource(R.string.label_longitude), longitude)
            LocationDetailRow(stringResource(R.string.label_elevation_gps_nmea), nmeaElevation)
            if (orsClient != null && orsElevation != null) {
                LocationDetailRow(stringResource(R.string.label_elevation_ors), orsElevation)
            }
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

    private sealed interface OrsElevationState {
        data object Unavailable : OrsElevationState
        data object Loading : OrsElevationState
        data class Success(val meters: Double) : OrsElevationState
        data object Error : OrsElevationState
    }
}

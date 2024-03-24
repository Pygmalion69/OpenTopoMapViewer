package org.nitri.opentopo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.nitri.opentopo.Util.elevationFromNmea
import org.nitri.opentopo.model.LocationViewModel

class LocationDetailFragment : DialogFragment() {
    private var view: View? = null
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        view = inflater.inflate(R.layout.fragment_location_detail, null)
        bindView()
        builder.setView(view)
            .setPositiveButton(R.string.close) { _: DialogInterface?, _: Int -> dismiss() }
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    @SuppressLint("DefaultLocale")
    private fun bindView() {
        val locationViewModel = ViewModelProvider(requireActivity())[LocationViewModel::class.java]
        val tvLatitude = requireView().findViewById<TextView>(R.id.textViewLatitude)
        val tvLongitude = requireView().findViewById<TextView>(R.id.textViewLongitude)
        val tvElevation = requireView().findViewById<TextView>(R.id.textViewElevation)
        val locationObserver = Observer<Location> { location: Location? ->
            if (location != null) {
                tvLatitude.text = String.format("%.5f", location.latitude)
                tvLongitude.text = String.format("%.5f", location.longitude)
            }
        }
        val nmeaObserver = Observer<String> { nmea: String? ->
            val elevation = elevationFromNmea(
                nmea!!
            )
            if (elevation != Util.NO_ELEVATION_VALUE.toDouble()) {
                tvElevation.text = String.format("%.1f m", elevation)
            }
        }
        locationViewModel.currentLocation?.observe(requireActivity(), locationObserver)
        locationViewModel.currentNmea.observe(requireActivity(), nmeaObserver)
    }
}

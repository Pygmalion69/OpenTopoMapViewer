package org.nitri.opentopo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.nitri.opentopo.model.LocationViewModel;

public class LocationDetailFragment extends DialogFragment {

    private View view;

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        view = inflater.inflate(R.layout.fragment_location_detail, null);

        bindView();

        builder.setView(view)
                .setPositiveButton(R.string.close, (dialog, id) -> dismiss());

        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @SuppressLint("DefaultLocale")
    private void bindView() {
        LocationViewModel locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);

        TextView tvLatitude = view.findViewById(R.id.textViewLatitude);
        TextView tvLongitude = view.findViewById(R.id.textViewLongitude);
        TextView tvElevation = view.findViewById(R.id.textViewElevation);

        Observer<Location> locationObserver = location -> {
            if (location != null) {
                tvLatitude.setText(String.format("%.5f", location.getLatitude()));
                tvLongitude.setText(String.format("%.5f", location.getLongitude()));
            }
        };

        Observer<String> nmeaObserver = nmea -> {
            double elevation = Util.elevationFromNmea(nmea);
            if (elevation != Util.NO_ELEVATION_VALUE) {
                tvElevation.setText(String.format("%.1f m", elevation));
            }
        };

        locationViewModel.getCurrentLocation().observe(requireActivity(), locationObserver);
        locationViewModel.getCurrentNmea().observe(requireActivity(), nmeaObserver);
    }

}

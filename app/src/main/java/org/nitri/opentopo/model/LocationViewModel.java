package org.nitri.opentopo.model;

import android.location.Location;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LocationViewModel extends ViewModel {

    private MutableLiveData<Location> currentLocation;
    private MutableLiveData<String> nmea;

    public MutableLiveData<Location> getCurrentLocation() {
        if (currentLocation == null) {
            currentLocation = new MutableLiveData<>();
        }
        return currentLocation;
    }

    public MutableLiveData<String> getCurrentNmea() {
        if (nmea == null) {
            nmea = new MutableLiveData<>();
        }
        return nmea;
    }

}

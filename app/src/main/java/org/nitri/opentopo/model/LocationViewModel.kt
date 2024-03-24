package org.nitri.opentopo.model

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel : ViewModel() {
    var currentLocation: MutableLiveData<Location>? = null
        get() {
            if (field == null) {
                field = MutableLiveData()
            }
            return field
        }
        private set
    private var nmea: MutableLiveData<String>? = null
    val currentNmea: MutableLiveData<String>
        get() {
            if (nmea == null) {
                nmea = MutableLiveData()
            }
            return nmea as MutableLiveData<String>
        }
}

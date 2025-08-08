package org.nitri.opentopo.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel : ViewModel() {

    val currentLocation: MutableLiveData<Location> by lazy {
        MutableLiveData<Location>()
    }

    val currentNmea: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}

package org.nitri.opentopo.viewmodel

import androidx.lifecycle.ViewModel
import org.osmdroid.bonuspack.kml.KmlDocument

class KmlViewModel : ViewModel() {
    var kmlUriString: String? = null
    var kmlDocument: KmlDocument? = null
}

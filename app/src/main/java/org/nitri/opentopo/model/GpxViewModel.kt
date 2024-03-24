package org.nitri.opentopo.model

import androidx.lifecycle.ViewModel
import io.ticofab.androidgpxparser.parser.domain.Gpx

class GpxViewModel : ViewModel() {
    var gpx: Gpx? = null
}

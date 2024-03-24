package org.nitri.opentopo.nearby.api.mediawiki

class Page {
    var pageid = 0
    var ns = 0
    var title: String? = null
    var index = 0
    var coordinates: List<PointCoordinates>? = null
    var thumbnail: Thumbnail? = null
    var terms: Terms? = null
    var fullurl: String? = null
    var canonicalurl: String? = null
}

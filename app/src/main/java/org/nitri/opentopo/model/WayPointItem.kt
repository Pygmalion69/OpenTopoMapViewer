package org.nitri.opentopo.model

import io.ticofab.androidgpxparser.parser.domain.WayPoint

class WayPointItem(@JvmField var wayPoint: WayPoint) : WayPointListItem {

    override fun getListItemType(): Int {
        return WayPointListItem.WAY_POINT
    }
}

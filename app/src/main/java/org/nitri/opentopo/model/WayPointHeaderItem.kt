package org.nitri.opentopo.model

class WayPointHeaderItem(@JvmField var header: String) : WayPointListItem {

    override fun getListItemType(): Int {
        return WayPointListItem.HEADER
    }
}

package org.nitri.opentopo.model

interface WayPointListItem {
    fun getListItemType(): Int

    companion object {
        const val HEADER = 0
        const val WAY_POINT = 1
    }
}

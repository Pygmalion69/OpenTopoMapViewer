package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.core.content.ContextCompat
import io.ticofab.androidgpxparser.parser.domain.Gpx
import org.nitri.opentopo.R
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow

class OverlayHelper(private val mContext: Context, private val mMapView: MapView?) {
    private var mWayPointOverlay: ItemizedIconInfoOverlay? = null
    private var mNearbyItemOverlay: ItemizedIconInfoOverlay? = null
    private var mOverlay = OVERLAY_NONE
    private var mOverlayTileProvider: MapTileProviderBasic? = null
    private var mTilesOverlay: TilesOverlay? = null
    private val tileOverlayAlphaMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 0.8f, 0f
        )
    )
    private val tileOverlayAlphaFilter = ColorMatrixColorFilter(tileOverlayAlphaMatrix)
    private val mWayPointItemGestureListener: OnItemGestureListener<OverlayItem?> =
        object : OnItemGestureListener<OverlayItem?> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                if (mWayPointOverlay != null && mMapView != null) {
                    mWayPointOverlay!!.showWayPointInfo(mMapView, item!!)
                }
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                return false
            }
        }
    private val mNearbyItemGestureListener: OnItemGestureListener<OverlayItem?> =
        object : OnItemGestureListener<OverlayItem?> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                if (mNearbyItemOverlay != null && mMapView != null) {
                    mNearbyItemOverlay!!.showNearbyItemInfo(mMapView, item!!)
                }
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                clearNearby()
                mMapView!!.invalidate()
                return true
            }
        }
    val infoWindow: BasicInfoWindow?
        get() = if (mWayPointOverlay != null) {
            mWayPointOverlay!!.infoWindow
        } else null
    private var mTrackOverlay: TrackOverlay? = null

    /**
     * Add GPX as an overlay
     *
     * @param gpx
     * @see Gpx
     */
    fun setGpx(gpx: Gpx) {
        clearGpx()
        if (gpx.tracks != null) {
            for (track in gpx.tracks) {
                mTrackOverlay = TrackOverlay(mContext, track!!)
                mMapView!!.overlays.add(0, mTrackOverlay)
            }
        }
        val wayPointItems: MutableList<OverlayItem?> = ArrayList()
        if (gpx.wayPoints != null) {
            for (wayPoint in gpx.wayPoints) {
                val gp = GeoPoint(wayPoint.latitude, wayPoint.longitude)
                val item = OverlayItem(wayPoint.name, wayPoint.desc, gp)
                wayPointItems.add(item)
            }
            mWayPointOverlay = ItemizedIconInfoOverlay(
                wayPointItems, ContextCompat.getDrawable(mContext, R.drawable.ic_default_marker),
                mWayPointItemGestureListener, mContext
            )
            mMapView!!.overlays.add(mWayPointOverlay)
        }
        mMapView!!.invalidate()
    }

    /**
     * Remove GPX layer
     */
    fun clearGpx() {
        if (mMapView != null) {
            if (mTrackOverlay != null) {
                mMapView.overlays.remove(mTrackOverlay)
                mTrackOverlay = null
            }
            if (mWayPointOverlay != null) {
                mMapView.overlays.remove(mWayPointOverlay)
                mWayPointOverlay = null
            }
        }
    }

    fun setNearby(item: NearbyItem) {
        clearNearby()
        val geoPoint = GeoPoint(item.lat, item.lon)
        val mapItem = OverlayItem(item.title, item.description, geoPoint)
        mNearbyItemOverlay = ItemizedIconInfoOverlay(
            ArrayList(listOf(mapItem)),
            ContextCompat.getDrawable(mContext, R.drawable.ic_default_marker),
            mNearbyItemGestureListener,
            mContext
        )
        mMapView!!.overlays.add(mNearbyItemOverlay)
        mMapView.invalidate()
    }

    /**
     * Remove nearby item layer
     */
    fun clearNearby() {
        if (mMapView != null && mNearbyItemOverlay != null) {
            mMapView.overlays.remove(mNearbyItemOverlay)
            mNearbyItemOverlay = null
        }
    }

    /**
     * Returns whether a GPX has been added
     *
     * @return GPX layer present
     */
    fun hasGpx(): Boolean {
        return mTrackOverlay != null || mWayPointOverlay != null
    }

    fun setTilesOverlay(overlay: Int) {
        mOverlay = overlay
        var overlayTiles: ITileSource? = null
        if (mTilesOverlay != null) {
            mMapView!!.overlays.remove(mTilesOverlay)
        }
        when (mOverlay) {
            OVERLAY_NONE -> {}
            OVERLAY_HIKING -> overlayTiles = XYTileSource(
                "hiking", 1, 17, 256, ".png", arrayOf(
                    "https://tile.waymarkedtrails.org/hiking/"
                ), mContext.getString(R.string.lonvia_copy)
            )

            OVERLAY_CYCLING -> overlayTiles = XYTileSource(
                "cycling", 1, 17, 256, ".png", arrayOf(
                    "https://tile.waymarkedtrails.org/cycling/"
                ), mContext.getString(R.string.lonvia_copy)
            )
        }
        if (overlayTiles != null) {
            mOverlayTileProvider = MapTileProviderBasic(mContext)
            mOverlayTileProvider!!.setTileSource(overlayTiles)
            mTilesOverlay = TilesOverlay(mOverlayTileProvider, mContext)
            mTilesOverlay!!.loadingBackgroundColor = Color.TRANSPARENT
            mTilesOverlay!!.setColorFilter(tileOverlayAlphaFilter)
            mOverlayTileProvider!!.tileRequestCompleteHandlers.clear()
            mOverlayTileProvider!!.tileRequestCompleteHandlers.add(mMapView!!.tileRequestCompleteHandler)
            mMapView.overlays.add(0, mTilesOverlay)
        }
        mMapView!!.invalidate()
    }

    val copyrightNotice: String?
        /**
         * Copyright notice for the tile overlay
         *
         * @return copyright notice or null
         */
        get() {
            return if (mOverlayTileProvider != null && mOverlay != OVERLAY_NONE) {
                mOverlayTileProvider!!.tileSource.copyrightNotice
            } else null
        }

    fun destroy() {
        mTilesOverlay = null
        mOverlayTileProvider = null
        mWayPointOverlay = null
    }

    companion object {
        /**
         * Tiles Overlays
         */
        const val OVERLAY_NONE = 1
        const val OVERLAY_HIKING = 2
        const val OVERLAY_CYCLING = 3
    }
}

package org.nitri.opentopo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.GpsStatus.NmeaListener
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import io.ticofab.androidgpxparser.parser.domain.Gpx
import org.nitri.opentopo.model.LocationViewModel
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.overlay.GestureOverlay
import org.nitri.opentopo.overlay.GestureOverlay.GestureCallback
import org.nitri.opentopo.overlay.OverlayHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

class MapFragment() : Fragment(), LocationListener, PopupMenu.OnMenuItemClickListener,
    GestureCallback {
    private lateinit var mMapView: MapView
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var mCompassOverlay: CompassOverlay? = null
    private var mScaleBarOverlay: ScaleBarOverlay? = null
    private var mRotationGestureOverlay: RotationGestureOverlay? = null
    private var mLocationManager: LocationManager? = null
    private var mOverlayHelper: OverlayHelper? = null
    private lateinit var mMapHandler: Handler
    private val mCenterRunnable: Runnable = object : Runnable {
        override fun run() {
            if (mLocationViewModel!!.currentLocation?.value != null) {
                mMapView.controller.animateTo(GeoPoint(mLocationViewModel!!.currentLocation?.value))
            }
            mMapHandler.postDelayed(this, 5000)
        }
    }
    private val mEnableFollowRunnable: Runnable = object : Runnable {
        override fun run() {
            if (mLocationOverlay != null) {
                mLocationOverlay!!.enableFollowLocation()
                mLocationOverlay!!.enableAutoStop = true
            }
            mMapHandler.postDelayed(this, 5000)
        }
    }
    private val mDragListener: MapListener = object : MapListener {
        override fun onScroll(event: ScrollEvent): Boolean {
            return false
        }

        override fun onZoom(event: ZoomEvent): Boolean {
            onUserMapInteraction()
            return false
        }
    }
    private var mFollow = false
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var mPrefs: SharedPreferences
    private var mBaseMap = BASE_MAP_OTM
    private var mCopyRightView: TextView? = null
    private var mOverlay = OverlayHelper.OVERLAY_NONE
    private var mMapCenterState: GeoPoint? = null
    private var mZoomState = DEFAULT_ZOOM
    private var mLastNearbyAnimateToId = 0
    private var mLocationViewModel: LocationViewModel? = null
    private var mGestureOverlay: GestureOverlay? = null
    @SuppressLint("ApplySharedPref")
    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val context = requireActivity().applicationContext
        mPrefs = requireActivity().getSharedPreferences(MAP_PREFS, Context.MODE_PRIVATE)
        val configuration = Configuration.getInstance()
        configuration.userAgentValue = BuildConfig.APPLICATION_ID
        val basePath = File(context.cacheDir.absolutePath, "osmdroid")
        configuration.osmdroidBasePath = basePath
        val tileCache = File(configuration.osmdroidBasePath.absolutePath, "tile")
        configuration.osmdroidTileCache = tileCache
        val edit = mPrefs.edit()
        edit.putString("osmdroid.basePath", basePath.absolutePath)
        edit.putString("osmdroid.cachePath", tileCache.absolutePath)
        edit.commit()
        configuration.load(context, mPrefs)
        mBaseMap = mPrefs.getInt(PREF_BASE_MAP, BASE_MAP_OTM)
        mOverlay = mPrefs.getInt(PREF_OVERLAY, OverlayHelper.OVERLAY_NONE)
        mLocationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mLocationViewModel = ViewModelProvider(requireActivity()).get(
            LocationViewModel::class.java
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val nmeaListener = OnNmeaMessageListener { s: String?, l: Long ->
                if (mLocationViewModel != null) {
                    mLocationViewModel!!.currentNmea.setValue(s)
                }
            }
            if (requireActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationManager!!.addNmeaListener(nmeaListener)
            }
        } else {
            val nmeaListener = NmeaListener { l: Long, s: String? ->
                if (mLocationViewModel != null) {
                    mLocationViewModel!!.currentNmea.setValue(s)
                }
            }
            if (ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationManager!!.addNmeaListener(nmeaListener)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mMapView = view.findViewById(R.id.mapview)
        val dm = this.resources.displayMetrics
        val activity = activity
        if (activity != null) {
            mCompassOverlay = CompassOverlay(
                getActivity(), InternalCompassOrientationProvider(getActivity()),
                mMapView
            )
            mLocationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(getActivity()),
                mMapView
            )
            val bmMapLocation =
                Util.getBitmapFromDrawable(requireActivity(), R.drawable.ic_position, 204)
            mLocationOverlay!!.setPersonIcon(bmMapLocation)
            mLocationOverlay!!.setPersonHotspot(
                bmMapLocation!!.width / 2f,
                bmMapLocation.height / 2f
            )
            val bmMapBearing =
                Util.getBitmapFromDrawable(requireActivity(), R.drawable.ic_direction, 204)
            mLocationOverlay!!.setDirectionArrow(bmMapLocation, bmMapBearing)
            mScaleBarOverlay = ScaleBarOverlay(mMapView)
            mScaleBarOverlay!!.setCentred(true)
            mScaleBarOverlay!!.setScaleBarOffset(dm.widthPixels / 2, 10)
            mRotationGestureOverlay = RotationGestureOverlay(mMapView)
            mRotationGestureOverlay!!.isEnabled = true
            mGestureOverlay = GestureOverlay(this)
            mMapView.overlays.add(mGestureOverlay)
            mMapView.maxZoomLevel = 17.0
            mMapView.isTilesScaledToDpi = true
            showZoomControls(mListener == null || !mListener!!.isFullscreen)
            mMapView.setMultiTouchControls(true)
            mMapView.isFlingEnabled = true
            mMapView.overlays.add(mLocationOverlay)
            mMapView.overlays.add(mCompassOverlay)
            mMapView.overlays.add(mScaleBarOverlay)
            mMapView.addMapListener(DelayedMapListener(mDragListener))
            mCopyRightView = view.findViewById(R.id.copyrightView)
            setBaseMap()
            mLocationOverlay!!.enableMyLocation()
            mLocationOverlay!!.disableFollowLocation()
            mLocationOverlay!!.isOptionsMenuEnabled = true
            mCompassOverlay!!.enableCompass()
            mMapView.visibility = View.VISIBLE
            mOverlayHelper = OverlayHelper(requireContext(), mMapView)
            setTilesOverlay()
            mLocationOverlay!!.runOnFirstFix {
                val location = mLocationOverlay!!.myLocation
                if (location != null) {
                    try {
                        requireActivity().runOnUiThread {
                            mMapView.controller.animateTo(location)
                        }
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mMapHandler = Handler(requireActivity().mainLooper)
        mListener!!.setGpx()
        val arguments = arguments
        // Move to received geo intent coordinates
        if (arguments != null) {
            if (arguments.containsKey(PARAM_LATITUDE) && arguments.containsKey(PARAM_LONGITUDE)) {
                val lat = arguments.getDouble(PARAM_LATITUDE)
                val lon = arguments.getDouble(PARAM_LONGITUDE)
                animateToLatLon(lat, lon)
            }
        }
        if (mListener!!.selectedNearbyPlace != null) {
            showNearbyPlace(mListener!!.selectedNearbyPlace)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                requireActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationViewModel!!.currentLocation?.value =
                    mLocationManager!!.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
        } else {
            mLocationViewModel!!.currentLocation?.setValue(
                mLocationManager!!.getLastKnownLocation(
                    LocationManager.PASSIVE_PROVIDER
                )
            )
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_LATITUDE) && savedInstanceState.containsKey(
                    STATE_LONGITUDE
                )
            ) {
                mMapCenterState = GeoPoint(
                    savedInstanceState.getDouble(STATE_LATITUDE, 0.0),
                    savedInstanceState.getDouble(STATE_LONGITUDE, 0.0)
                )
                Log.d(
                    TAG,
                    String.format(
                        "Restoring center state: %f, %f",
                        mMapCenterState!!.latitude,
                        mMapCenterState!!.longitude
                    )
                )
                mZoomState = savedInstanceState.getDouble(STATE_ZOOM, DEFAULT_ZOOM)
            } else {
                Log.d(TAG, "No center state delivered")
            }
        }
        if (mMapCenterState == null) {
            Log.d(TAG, "No saved center state")
            val prefLat = mPrefs.getFloat(PREF_LATITUDE, 0f)
            val prefLon = mPrefs.getFloat(PREF_LONGITUDE, 0f)
            mMapCenterState = GeoPoint(prefLat.toDouble(), prefLon.toDouble())
            Log.d(TAG, String.format("Restoring center state from prefs: %f, %f", prefLat, prefLon))
        }
        if (mMapCenterState != null) {
            mMapView.controller.setCenter(mMapCenterState)
        } else if (mLocationViewModel!!.currentLocation != null && mLocationViewModel!!.currentLocation?.value != null) {
            mMapView.controller.setCenter(
                GeoPoint(
                    mLocationViewModel!!.currentLocation?.value!!.latitude,
                    mLocationViewModel!!.currentLocation?.value!!.longitude
                )
            )
        }
        mMapView.controller.setZoom(mZoomState)
        if (mPrefs.getBoolean(PREF_FOLLOW, false)) enableFollow()
        mMapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (mListener != null) {
                    mListener!!.onMapTap()
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                if (mListener != null) {
                    mListener!!.onMapLongPress()
                }
                return false
            }
        }))
        mMapView.keepScreenOn = mListener!!.isKeepScreenOn
        mListener!!.isFullscreen = mPrefs.getBoolean(MainActivity.PREF_FULLSCREEN, false)
    }

    private fun animateToLatLon(lat: Double, lon: Double) {
        mMapHandler.postDelayed({
            disableFollow()
            mMapView.controller.animateTo(GeoPoint(lat, lon))
        }, 500)
    }

    private fun setBaseMap() {
        when (mBaseMap) {
            BASE_MAP_OTM -> mMapView.setTileSource(TileSourceFactory.OpenTopo)
            BASE_MAP_OSM -> mMapView.setTileSource(TileSourceFactory.MAPNIK)
        }
        mMapView.invalidate()

        setCopyrightNotice()
    }

    private fun setTilesOverlay() {
        mOverlayHelper!!.setTilesOverlay(mOverlay)
        setCopyrightNotice()
    }

    private fun setCopyrightNotice() {
        val copyrightStringBuilder = StringBuilder()
        val mapCopyRightNotice = mMapView.tileProvider.tileSource.copyrightNotice
        copyrightStringBuilder.append(mapCopyRightNotice)
        if (mOverlayHelper != null) {
            val overlayCopyRightNotice = mOverlayHelper!!.copyrightNotice
            if (!TextUtils.isEmpty(mapCopyRightNotice) && !TextUtils.isEmpty(overlayCopyRightNotice)) {
                copyrightStringBuilder.append(", ")
            }
            copyrightStringBuilder.append(overlayCopyRightNotice)
        }
        val copyRightNotice = copyrightStringBuilder.toString()
        if (!TextUtils.isEmpty(copyRightNotice)) {
            mCopyRightView!!.text = copyRightNotice
            mCopyRightView!!.visibility = View.VISIBLE
        } else {
            mCopyRightView!!.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun initMap() {
        if (mFollow) {
            mLocationOverlay!!.enableFollowLocation()
            mMapHandler.removeCallbacks(mCenterRunnable)
            mMapHandler.post(mCenterRunnable)
        }
        mLocationOverlay!!.enableMyLocation()
        mCompassOverlay!!.enableCompass()
        mScaleBarOverlay!!.enableScaleBar()
        mMapView.invalidate()
    }

    private fun enableFollow() {
        mFollow = true
        if (activity != null) (activity as AppCompatActivity?)!!.supportInvalidateOptionsMenu()
        mLocationOverlay!!.enableFollowLocation()
        mLocationOverlay!!.enableAutoStop = true
        mMapHandler.removeCallbacks(mCenterRunnable)
        //mMapHandler.post(mCenterRunnable);
        mPrefs.edit().putBoolean(PREF_FOLLOW, true).apply()
    }

    private fun disableFollow() {
        mFollow = false
        if (activity != null) (activity as AppCompatActivity?)!!.supportInvalidateOptionsMenu()
        mLocationOverlay!!.disableFollowLocation()
        mMapHandler.removeCallbacksAndMessages(null)
        mPrefs.edit().putBoolean(PREF_FOLLOW, false).apply()
    }

    private fun saveMapCenterPrefs() {
        if (mMapCenterState != null) {
            mPrefs.edit().putFloat(PREF_LATITUDE, mMapCenterState!!.latitude.toFloat()).apply()
            mPrefs.edit().putFloat(PREF_LONGITUDE, mMapCenterState!!.longitude.toFloat()).apply()
            Log.d(
                TAG,
                String.format(
                    "Saving center prefs: %f, %f",
                    mMapCenterState!!.latitude,
                    mMapCenterState!!.longitude
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        val basePath = Configuration.getInstance().osmdroidBasePath
        val cache = Configuration.getInstance().osmdroidTileCache
        Log.d(TAG, "Cache: " + cache.absolutePath)
        initMap()
        if (mMapCenterState != null) {
            mMapView.controller.setCenter(mMapCenterState)
            mMapCenterState = null // We're done with the old state
        } else if (mLocationViewModel!!.currentLocation != null && mLocationViewModel!!.currentLocation?.value != null) {
            mMapView.controller.setCenter(
                GeoPoint(
                    mLocationViewModel!!.currentLocation?.value!!.latitude,
                    mLocationViewModel!!.currentLocation?.value!!.longitude
                )
            )
        }
        if (mLocationManager != null) {
            try {
                mLocationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    this
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            try {
                mLocationManager!!.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    this
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onPause() {
        mMapCenterState = mMapView.mapCenter as GeoPoint
        try {
            mLocationManager!!.removeUpdates(this)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        mMapHandler.removeCallbacks(mCenterRunnable)
        mCompassOverlay!!.disableCompass()
        mLocationOverlay!!.disableFollowLocation()
        mLocationOverlay!!.disableMyLocation()
        mScaleBarOverlay!!.disableScaleBar()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState()")
        mMapCenterState = mMapView.mapCenter as GeoPoint
        outState.putDouble(STATE_LATITUDE, mMapCenterState!!.latitude)
        outState.putDouble(STATE_LONGITUDE, mMapCenterState!!.longitude)
        Log.d(
            TAG,
            String.format(
                "Saving center state: %f, %f",
                mMapCenterState!!.latitude,
                mMapCenterState!!.longitude
            )
        )
        outState.putDouble(STATE_ZOOM, mMapView.zoomLevelDouble)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        saveMapCenterPrefs()
        super.onStop()
    }

    fun setGpx(gpx: Gpx?, zoom: Boolean) {
        if (gpx == null) {
            return;
        }
        mOverlayHelper?.setGpx(gpx)
        if (activity != null) (activity as AppCompatActivity?)!!.supportInvalidateOptionsMenu()
        if (zoom) {
            disableFollow()
            zoomToBounds(Util.area(gpx))
        }
    }

    private fun showGpxDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity(), R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.gpx))
            .setMessage(getString(R.string.discard_current_gpx))
            .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                if (mOverlayHelper != null) {
                    mOverlayHelper!!.clearGpx()
                    if (activity != null) (activity as AppCompatActivity?)!!.supportInvalidateOptionsMenu()
                }
                if (mListener != null) {
                    mListener!!.selectGpx()
                }
                dialog.dismiss()
            }
            .setNegativeButton(
                android.R.string.cancel
            ) { dialog: DialogInterface, _: Int -> dialog.cancel() }
            .setIcon(R.drawable.ic_alert)
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.show()
    }

    private fun zoomToBounds(box: BoundingBox) {
        if (mMapView.height > 0) {
            mMapView.zoomToBoundingBox(box, true, 64)
        } else {
            val vto = mMapView.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mMapView.zoomToBoundingBox(box, true, 64)
                    vto.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

    fun setNearbyPlace() {
        val nearbyPlace = mListener!!.selectedNearbyPlace
        showNearbyPlace(nearbyPlace)
    }

    fun showZoomControls(show: Boolean) {
        if (show) {
            mMapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        } else {
            mMapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        }
    }

    private fun showNearbyPlace(nearbyPlace: NearbyItem?) {
        if (nearbyPlace == null) {
            return
        }
        mOverlayHelper!!.setNearby(nearbyPlace)
        if (nearbyPlace.id != mLastNearbyAnimateToId) {
            // Animate only once
            animateToLatLon(nearbyPlace.lat, nearbyPlace.lon)
            mLastNearbyAnimateToId = nearbyPlace.id
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        mListener!!.setUpNavigation(false)
        inflater.inflate(R.menu.menu_main, menu)
        val fullscreenItem = menu.findItem(R.id.action_fullscreen)
        val keepScreenOnItem = menu.findItem(R.id.action_keep_screen_on)
        if (mListener != null) {
            fullscreenItem.isChecked = mListener!!.isFullscreenOnMapTap
            keepScreenOnItem.isChecked = mListener!!.isKeepScreenOn
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (mFollow) {
            menu.findItem(R.id.action_follow).isVisible = false
            menu.findItem(R.id.action_no_follow).isVisible = true
        } else {
            menu.findItem(R.id.action_follow).isVisible = true
            menu.findItem(R.id.action_no_follow).isVisible = false
        }
        if (mOverlayHelper != null && mOverlayHelper!!.hasGpx()) {
            menu.findItem(R.id.action_gpx_details).isVisible = true
            menu.findItem(R.id.action_gpx_zoom).isVisible = true
        } else {
            menu.findItem(R.id.action_gpx_details).isVisible = false
            menu.findItem(R.id.action_gpx_zoom).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val fm: FragmentManager
        val itemId = item.itemId
        if (itemId == R.id.action_gpx) {
            if (mOverlayHelper != null && mOverlayHelper!!.hasGpx()) {
                showGpxDialog()
            } else {
                mListener!!.selectGpx()
            }
            return true
        } else if (itemId == R.id.action_location) {
            if (mLocationViewModel!!.currentLocation != null && mLocationViewModel!!.currentLocation?.value != null) {
                mMapView.controller.animateTo(GeoPoint(mLocationViewModel!!.currentLocation?.value))
            }
            return true
        } else if (itemId == R.id.action_follow) {
            enableFollow()
            Toast.makeText(activity, R.string.follow_enabled, Toast.LENGTH_SHORT).show()
            return true
        } else if (itemId == R.id.action_no_follow) {
            disableFollow()
            Toast.makeText(activity, R.string.follow_disabled, Toast.LENGTH_SHORT).show()
            return true
        } else if (itemId == R.id.action_gpx_details) {
            if (mListener != null) mListener!!.addGpxDetailFragment()
            return true
        } else if (itemId == R.id.action_location_details) {
            fm = requireActivity().supportFragmentManager
            val locationDetailFragment = LocationDetailFragment()
            locationDetailFragment.show(fm, "location_detail")
            return true
        } else if (itemId == R.id.action_nearby) {
            if (mListener != null) {
                mListener!!.clearSelectedNearbyPlace()
                val nearbyCenter = mMapView.mapCenter as GeoPoint
                mListener!!.addNearbyFragment(nearbyCenter)
                Toast.makeText(activity, R.string.location_unknown, Toast.LENGTH_SHORT).show()
            }
            return true
        } else if (itemId == R.id.action_cache_settings) {
            mMapCenterState = mMapView.mapCenter as GeoPoint
            saveMapCenterPrefs()
            fm = requireActivity().supportFragmentManager
            val cacheSettingsFragment = CacheSettingsFragment()
            cacheSettingsFragment.show(fm, "cache_settings")
            return true
        } else if (itemId == R.id.action_gpx_zoom) {
            disableFollow()
            zoomToBounds(Util.area(mListener!!.getGpx()))
            return true
        } else if (itemId == R.id.action_layers) {
            if (activity != null) {
                val anchorView = requireActivity().findViewById<View>(R.id.popupAnchorView)
                val popup = PopupMenu(activity, anchorView)
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.menu_tile_sources, popup.menu)
                val openTopoMapItem = popup.menu.findItem(R.id.otm)
                val openStreetMapItem = popup.menu.findItem(R.id.osm)
                val overlayNoneItem = popup.menu.findItem(R.id.none)
                val overlayHikingItem = popup.menu.findItem(R.id.lonvia_hiking)
                val overlayCyclingItem = popup.menu.findItem(R.id.lonvia_cycling)
                when (mBaseMap) {
                    BASE_MAP_OTM -> openTopoMapItem.isChecked = true
                    BASE_MAP_OSM -> openStreetMapItem.isChecked = true
                }
                when (mOverlay) {
                    OverlayHelper.OVERLAY_NONE -> overlayNoneItem.isChecked = true
                    OverlayHelper.OVERLAY_HIKING -> overlayHikingItem.isChecked = true
                    OverlayHelper.OVERLAY_CYCLING -> overlayCyclingItem.isChecked = true
                }
                popup.setOnMenuItemClickListener(this@MapFragment)
                popup.show()
                return true
            } else {
                return false
            }
        } else if (itemId == R.id.action_fullscreen) {
            if (mListener != null) {
                item.isChecked = !item.isChecked
                mListener!!.isFullscreenOnMapTap = item.isChecked
            }
        } else if (itemId == R.id.action_keep_screen_on) {
            if (mListener != null) {
                item.isChecked = !item.isChecked
                mListener!!.isKeepScreenOn = item.isChecked
                mMapView.keepScreenOn = item.isChecked
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Popup menu click
     *
     * @param menuItem
     * @return
     */
    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
            val itemId = menuItem.itemId
            if (itemId == R.id.otm) {
                mBaseMap = BASE_MAP_OTM
            } else if (itemId == R.id.osm) {
                mBaseMap = BASE_MAP_OSM
            } else if (itemId == R.id.none) {
                mOverlay = OverlayHelper.OVERLAY_NONE
            } else if (itemId == R.id.lonvia_hiking) {
                mOverlay = OverlayHelper.OVERLAY_HIKING
            } else if (itemId == R.id.lonvia_cycling) {
                mOverlay = OverlayHelper.OVERLAY_CYCLING
            }
            mPrefs.edit().putInt(PREF_BASE_MAP, mBaseMap).apply()
            mPrefs.edit().putInt(PREF_OVERLAY, mOverlay).apply()
            setBaseMap()
            setTilesOverlay()
        }
        return true
    }

    override fun onLocationChanged(location: Location) {
        if (BuildConfig.DEBUG) Log.d(
            TAG,
            String.format("Location: %f, %f", location.latitude, location.longitude)
        )
        mLocationViewModel!!.currentLocation?.value = location
    }

    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(
                (context.toString()
                        + " must implement OnFragmentInteractionListener")
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationManager = null
        mLocationOverlay = null
        mCompassOverlay = null
        mScaleBarOverlay = null
        mRotationGestureOverlay = null
        mGestureOverlay = null
        if (mOverlayHelper != null) mOverlayHelper!!.destroy()
    }

    override fun onUserMapInteraction() {
        if (mFollow) {
            // follow disabled by gesture -> re-enable with delay
            mMapHandler.removeCallbacksAndMessages(null)
            mMapHandler.postDelayed(mEnableFollowRunnable, 5000)
        }
    }

    interface OnFragmentInteractionListener {
        /**
         * Start GPX file selection flow
         */
        fun selectGpx()

        /**
         * Request to set a GPX layer, e.g. after a configuration change
         */
        fun setGpx()

        /**
         * Retrieve the current GPX
         *
         * @return Gpx
         */
        fun getGpx(): Gpx?

        /**
         * Present GPX details
         */
        fun addGpxDetailFragment()

        /**
         * Present nearby items
         */
        fun addNearbyFragment(nearbyCenterPoint: GeoPoint)

        /**
         * Set up navigation arrow
         */
        fun setUpNavigation(upNavigation: Boolean)

        /**
         * Selected nearby item to show on map
         */
        var selectedNearbyPlace: NearbyItem?

        /**
         * Clear selected nearby place
         */
        fun clearSelectedNearbyPlace()

        /**
         * Single tap map
         */
        fun onMapTap()

        /**
         * Long press on map
         */
        fun onMapLongPress()

        /**
         * Fullscreen on map tap
         */
        var isFullscreenOnMapTap: Boolean

        var isFullscreen: Boolean
        var isKeepScreenOn: Boolean
    }

    companion object {
        private const val PARAM_LATITUDE = "latitude"
        private const val PARAM_LONGITUDE = "longitude"
        private const val STATE_LATITUDE = "latitude"
        private const val STATE_LONGITUDE = "longitude"
        private const val STATE_ZOOM = "zoom"
        const val MAP_PREFS = "map_prefs"
        private const val PREF_BASE_MAP = "base_map"
        private const val PREF_OVERLAY = "overlay"
        private const val PREF_LATITUDE = "latitude"
        private const val PREF_LONGITUDE = "longitude"
        private const val PREF_FOLLOW = "follow"
        private const val BASE_MAP_OTM = 1
        private const val BASE_MAP_OSM = 2
        private const val DEFAULT_ZOOM = 15.0
        private val TAG = MapFragment::class.java.simpleName
        fun newInstance(): MapFragment {
            return MapFragment()
        }

        fun newInstance(lat: Double, lon: Double): MapFragment {
            val mapFragment = MapFragment()
            val arguments = Bundle()
            arguments.putDouble(PARAM_LATITUDE, lat)
            arguments.putDouble(PARAM_LONGITUDE, lon)
            mapFragment.arguments = arguments
            return mapFragment
        }
    }
}
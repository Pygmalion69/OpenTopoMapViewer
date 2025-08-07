package org.nitri.opentopo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import io.ticofab.androidgpxparser.parser.domain.Gpx
import org.nitri.opentopo.SettingsActivity.Companion.PREF_KEEP_SCREEN_ON
import org.nitri.opentopo.SettingsActivity.Companion.PREF_ORS_PROFILE
import org.nitri.opentopo.viewmodel.LocationViewModel
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.ors.Directions
import org.nitri.opentopo.overlay.ClickableCompassOverlay
import org.nitri.opentopo.overlay.GestureOverlay
import org.nitri.opentopo.overlay.GestureOverlay.GestureCallback
import org.nitri.opentopo.overlay.OverlayHelper
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.viewmodel.MarkerViewModel
import org.nitri.opentopo.util.MapOrientation
import org.nitri.opentopo.util.OrientationSensor
import org.nitri.opentopo.util.Utils
import org.nitri.ors.api.OpenRouteServiceApi
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

class MapFragment : Fragment(), LocationListener, PopupMenu.OnMenuItemClickListener,
    GestureCallback, ClickableCompassOverlay.OnCompassClickListener {

    /**
     * Enum representing the possible states of the map display regarding GPX tracks
     */
    enum class GpxDisplayState {
        IDLE,           // Nothing on display
        LOADED_FROM_FILE, // GPX loaded from file
        CALCULATED      // GPX calculated from routing service
    }

    private var gpxDisplayState: GpxDisplayState = GpxDisplayState.IDLE
    private var orientationSensor: OrientationSensor? = null
    @Volatile
    private var mapRotation: Boolean = false
    private lateinit var mapView: MapView
    private var locationOverlay: MyLocationNewOverlay? = null
    private var compassOverlay: CompassOverlay? = null
    private var scaleBarOverlay: ScaleBarOverlay? = null
    private var rotationGestureOverlay: RotationGestureOverlay? = null
    private var locationManager: LocationManager? = null
    private var overlayHelper: OverlayHelper? = null
    private lateinit var mapHandler: Handler
    private val centerMapRunnable: Runnable = object : Runnable {
        override fun run() {
            locationViewModel?.currentLocation?.value?.let { location ->
                mapView.controller.animateTo(GeoPoint(location))
            }
            mapHandler.postDelayed(this, 5000)
        }
    }
    private val enableFollowRunnable: Runnable = object : Runnable {
        override fun run() {
            if (followEnabled) {
                locationOverlay?.let {
                    it.enableFollowLocation()
                    it.enableAutoStop = true
                }
                mapHandler.postDelayed(this, 5000)
            }
        }
    }
    private val dragListener: MapListener = object : MapListener {
        override fun onScroll(event: ScrollEvent): Boolean {
            return false
        }

        override fun onZoom(event: ZoomEvent): Boolean {
            onUserMapInteraction()
            return false
        }
    }

    fun setKeepScreenOn(value: Boolean) {
        Log.d(TAG, "keepScreenOn: $value")
        mapView.keepScreenOn = value
        if (value) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private var followEnabled = false
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var baseMap = BASE_MAP_OTM
    private var copyrightView: TextView? = null
    private var overlay = OverlayHelper.OVERLAY_NONE
    private var mapCenterState: GeoPoint? = null
    private var zoomState = DEFAULT_ZOOM
    private var lastNearbyAnimateToId = 0
    private var locationViewModel: LocationViewModel? = null
    private var gestureOverlay: GestureOverlay? = null

    private val markerViewModel: MarkerViewModel by viewModels()

    @SuppressLint("ApplySharedPref")
    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val context = requireActivity().applicationContext
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val configuration = Configuration.getInstance()
        configuration.userAgentValue = BuildConfig.APPLICATION_ID
        val basePath = Utils.getOsmdroidBasePath(context, sharedPreferences.getBoolean(CacheSettingsFragment.PREF_EXTERNAL_STORAGE, false))
        configuration.osmdroidBasePath = basePath
        val tileCache = File(configuration.osmdroidBasePath.absolutePath,
            sharedPreferences.getString(CacheSettingsFragment.PREF_TILE_CACHE, CacheSettingsFragment.DEFAULT_TILE_CACHE)
                ?: CacheSettingsFragment.DEFAULT_TILE_CACHE)
        configuration.osmdroidTileCache = tileCache
        val maxCacheSize = sharedPreferences.getInt(CacheSettingsFragment.PREF_CACHE_SIZE, CacheSettingsFragment.DEFAULT_CACHE_SIZE)
        configuration.tileFileSystemCacheMaxBytes =
            maxCacheSize.toLong() * 1024 * 1024
        sharedPreferences.edit(commit = true) {
            putString("osmdroid.basePath", basePath.absolutePath)
            putString("osmdroid.cachePath", tileCache.absolutePath)
        }
        configuration.load(context, sharedPreferences)
        baseMap = sharedPreferences.getInt(PREF_BASE_MAP, BASE_MAP_OTM)
        overlay = sharedPreferences.getInt(PREF_OVERLAY, OverlayHelper.OVERLAY_NONE)
        mapRotation = sharedPreferences.getBoolean(SettingsActivity.PREF_ROTATE, false)
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationViewModel = ViewModelProvider(requireActivity())[LocationViewModel::class.java]
        val nmeaListener = OnNmeaMessageListener { s: String?, _: Long ->
            locationViewModel?.currentNmea?.value = s

        }
        if (requireActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager?.addNmeaListener(nmeaListener)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.mapview)
        val dm = this.resources.displayMetrics
        val activity = activity ?: return null

        mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                listener?.onMapTap()
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                listener?.onMapLongPress()
                val highestSeq = markerViewModel.markers.value?.maxByOrNull { it.seq }?.seq ?: 0
                val seq = highestSeq + 1
                val marker = MarkerModel(
                    seq = seq,
                    latitude = p.latitude,
                    longitude = p.longitude,
                    name = getString(R.string.default_marker_name, seq),
                    description = "")
                markerViewModel.addMarker(marker)
                return true
            }
        }))

        compassOverlay = ClickableCompassOverlay(
            activity, InternalCompassOrientationProvider(activity),
            mapView, this
        )
        locationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(activity),
            mapView
        )
        val bmMapLocation =
            Utils.getBitmapFromDrawable(requireActivity(), R.drawable.ic_position, 204)
        locationOverlay?.setPersonIcon(bmMapLocation)
        locationOverlay?.setPersonHotspot(
            bmMapLocation.width / 2f,
            bmMapLocation.height / 2f
        )
        val bmMapBearing =
            Utils.getBitmapFromDrawable(requireActivity(), R.drawable.ic_direction, 204)
        locationOverlay?.setDirectionArrow(bmMapLocation, bmMapBearing)
        scaleBarOverlay = ScaleBarOverlay(mapView)
        scaleBarOverlay?.setCentred(true)
        scaleBarOverlay?.setScaleBarOffset(dm.widthPixels / 2, 10)
        rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay?.isEnabled = true
        gestureOverlay = GestureOverlay(this)
        mapView.overlays.add(gestureOverlay)
        mapView.maxZoomLevel = 17.0
        mapView.isTilesScaledToDpi = true
        showZoomControls(listener?.isFullscreen?.not() ?: true)
        mapView.setMultiTouchControls(true)
        mapView.isFlingEnabled = true
        mapView.overlays.add(locationOverlay)
        mapView.overlays.add(compassOverlay)
        mapView.overlays.add(scaleBarOverlay)
        mapView.addMapListener(DelayedMapListener(dragListener))
        copyrightView = view.findViewById(R.id.copyrightView)
        setBaseMap()
        locationOverlay?.enableMyLocation()
        locationOverlay?.disableFollowLocation()
        locationOverlay?.isOptionsMenuEnabled = true
        compassOverlay?.enableCompass()
        mapView.visibility = View.VISIBLE
        overlayHelper = OverlayHelper(requireContext(), mapView)
        setTilesOverlay()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapHandler = Handler(requireActivity().mainLooper)
        listener?.setGpx()

        // Check if there's already a GPX track loaded and update the state accordingly
        if (overlayHelper?.hasGpx() == true) {
            gpxDisplayState = GpxDisplayState.LOADED_FROM_FILE
        }

        val arguments = arguments
        var mapCenterSet = false
        // Move to received geo intent coordinates
        arguments?.let {
            val lat = it.getDouble(PARAM_LATITUDE, Double.MIN_VALUE)
            val lon = it.getDouble(PARAM_LONGITUDE, Double.MIN_VALUE)
            if (lat != Double.MIN_VALUE && lon != Double.MIN_VALUE) {
                mapCenterSet = true
                animateToLatLon(lat, lon)
            }
        }

        if (requireActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            requireActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            locationViewModel?.let {
                it.currentLocation.value = locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
        }
        savedInstanceState?.let {
            if (it.containsKey(STATE_LATITUDE) && savedInstanceState.containsKey(STATE_LONGITUDE)
            ) {
                mapCenterState = GeoPoint(
                    it.getDouble(STATE_LATITUDE, 0.0),
                    it.getDouble(STATE_LONGITUDE, 0.0)
                )
                Log.d(
                    TAG,
                    String.format(
                        "Restoring center state: %f, %f",
                        mapCenterState?.latitude,
                        mapCenterState?.longitude
                    )
                )
                zoomState = it.getDouble(STATE_ZOOM, DEFAULT_ZOOM)
            } else {
                Log.d(TAG, "No center state delivered")
            }
        }
        if (mapCenterState == null) {
            Log.d(TAG, "No saved center state")
            val prefLat = sharedPreferences.getFloat(PREF_LATITUDE, 0f)
            val prefLon = sharedPreferences.getFloat(PREF_LONGITUDE, 0f)
            if (prefLat > 0 && prefLon > 0) {
                mapCenterState = GeoPoint(prefLat.toDouble(), prefLon.toDouble())
                Log.d(
                    TAG,
                    String.format("Restoring center state from prefs: %f, %f", prefLat, prefLon)
                )
            }
        }
        mapCenterState?.let {
            mapView.controller.setCenter(it)
            mapCenterSet = true
        } ?: run {
            locationViewModel?.currentLocation?.value?.let { location ->
                mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                mapCenterSet = true
            }
        }
        if (!mapCenterSet) {
            centerOnFirstFix()
        }
        mapView.controller.setZoom(zoomState)
        if (sharedPreferences.getBoolean(PREF_FOLLOW, false)) enableFollow()

        setKeepScreenOn(sharedPreferences.getBoolean(PREF_KEEP_SCREEN_ON, false))

        markerViewModel.markers.observe(viewLifecycleOwner) { markers ->
            overlayHelper?.setMarkers(markers, object : OverlayHelper.MarkerInteractionListener {
                override fun onMarkerMoved(markerModel: MarkerModel) {
                    //NOP
                }

                override fun onMarkerClicked(markerModel: MarkerModel) {
                    //NOP
                }

                override fun onMarkerDelete(markerModel: MarkerModel) {
                    markerViewModel.removeMarker(markerModel.id)
                }

                override fun onMarkerUpdate(markerModel: MarkerModel) {
                    markerViewModel.updateMarker(markerModel)
                }

                override fun onMarkerWaypointsChanged() {
                    calculateRoute()
                }
            })
            if (markerViewModel.hasRoutePoints()) {
                calculateRoute()
            }
        }
    }

    private fun calculateRoute() {
        val coordinates = mutableListOf<List<Double>>()
        val currentLocation = locationViewModel?.currentLocation?.value
        currentLocation?.let {
            coordinates.add(listOf(it.longitude, it.latitude))
        }
        if (coordinates.isEmpty()) {
            if (gpxDisplayState == GpxDisplayState.CALCULATED) {
                listener?.clearGpx()
            }
            return
        }
        markerViewModel.markers.value?.forEach { marker ->
            if (marker.routeWaypoint) {
                coordinates.add(listOf(marker.longitude, marker.latitude))
            }
        }
        if (coordinates.size < 2) {
            // Insufficient coordinates; nothing to do
            if (gpxDisplayState != GpxDisplayState.LOADED_FROM_FILE) {
                removeGpx()
                listener?.clearGpx()
            }
            return
        }
        val locale =  Resources.getSystem().configuration.locales.get(0)
        val language = locale.toLanguageTag().lowercase()
        listener?.getOpenRouteServiceApi()?.let { api ->
            val profile = sharedPreferences.getString(PREF_ORS_PROFILE, "driving-car")
            profile?.let {
                val directions = Directions(api, it)
                directions.getRouteGpx(coordinates, language, object : Directions.RouteGpResult {
                    override fun onSuccess(gpx: String) {
                        Log.d(TAG, "GPX: $gpx")
                        if (gpxDisplayState == GpxDisplayState.LOADED_FROM_FILE) {
                            showGpxDialog {
                                listener?.clearGpx()
                                listener?.parseCalculatedGpx(gpx)
                            }
                        } else {
                            removeGpx()
                            listener?.clearGpx()
                            listener?.parseCalculatedGpx(gpx)
                        }
                    }

                    override fun onError(message: String) {
                        Log.e(TAG, "Error fetching GPX: $message")
                        Toast.makeText(
                            requireContext(),
                            "Error fetching GPX: $message",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                )
            }
        }
    }

    private fun centerOnFirstFix() {
        locationOverlay?.runOnFirstFix {
            val location = locationOverlay?.myLocation
            location?.let {
                try {
                    requireActivity().runOnUiThread {
                        mapView.controller.animateTo(it)
                    }
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun animateToLatLon(lat: Double, lon: Double) {
        mapHandler.postDelayed({
            disableFollow()
            mapView.controller.animateTo(GeoPoint(lat, lon))
        }, 500)
    }

    private fun setBaseMap() {
        when (baseMap) {
            BASE_MAP_OTM -> mapView.setTileSource(TileSourceFactory.OpenTopo)
            BASE_MAP_OSM -> mapView.setTileSource(TileSourceFactory.MAPNIK)
        }
        mapView.invalidate()

        setCopyrightNotice()
    }

    private fun setTilesOverlay() {
        overlayHelper?.setTilesOverlay(overlay)
        setCopyrightNotice()
    }

    private fun setCopyrightNotice() {
        val copyrightStringBuilder = StringBuilder()
        val mapCopyrightNotice = mapView.tileProvider.tileSource.copyrightNotice
        copyrightStringBuilder.append(mapCopyrightNotice)
        overlayHelper?.let {
            val overlayCopyRightNotice = it.copyrightNotice
            if (!TextUtils.isEmpty(mapCopyrightNotice) && !TextUtils.isEmpty(overlayCopyRightNotice)) {
                copyrightStringBuilder.append(", ")
            }
            copyrightStringBuilder.append(overlayCopyRightNotice)
        }
        val copyRightNotice = copyrightStringBuilder.toString()
        if (!TextUtils.isEmpty(copyRightNotice)) {
            copyrightView?.text = copyRightNotice
            copyrightView?.visibility = View.VISIBLE
        } else {
            copyrightView?.visibility = View.GONE
        }
    }

    private fun enableFollow() {
        followEnabled = true
        activity?.let { (it as AppCompatActivity).supportInvalidateOptionsMenu() }
        locationOverlay?.enableFollowLocation()
        locationOverlay?.enableAutoStop = true
        mapHandler.removeCallbacks(centerMapRunnable)
        mapHandler.post(centerMapRunnable);
        sharedPreferences.edit { putBoolean(PREF_FOLLOW, true) }
    }

    private fun disableFollow() {
        followEnabled = false
        activity?.let { (it as AppCompatActivity).supportInvalidateOptionsMenu() }
        locationOverlay?.disableFollowLocation()
        mapHandler.removeCallbacksAndMessages(null)
        sharedPreferences.edit { putBoolean(PREF_FOLLOW, false) }
    }

    private fun saveMapCenterPrefs() {
        mapCenterState?.let {
            sharedPreferences.edit { putFloat(PREF_LATITUDE, it.latitude.toFloat()) }
            sharedPreferences.edit { putFloat(PREF_LONGITUDE, it.longitude.toFloat()) }
            Log.d(
                TAG,
                String.format(
                    "Saving center prefs: %f, %f",
                    it.latitude,
                    it.longitude
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        // val basePath = Configuration.getInstance().osmdroidBasePath
        val cache = Configuration.getInstance().osmdroidTileCache
        Log.d(TAG, "Cache: " + cache.absolutePath)
        if (followEnabled) {
            locationOverlay?.enableFollowLocation()
            mapHandler.removeCallbacks(centerMapRunnable)
            mapHandler.post(centerMapRunnable)
        }
        locationOverlay?.enableMyLocation()
        compassOverlay?.enableCompass()
        scaleBarOverlay?.enableScaleBar()
        mapView.invalidate()
        mapCenterState?.let {
            mapView.controller.setCenter(mapCenterState)
            mapCenterState = null // We're done with the old state
        } ?: run {
            locationViewModel?.currentLocation?.value?.let { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.setCenter(geoPoint)
            }
        }
        locationManager?.let {
            try {
                it.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    this
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            try {
                it.requestLocationUpdates(
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
        mapCenterState = mapView.mapCenter as GeoPoint
        try {
            locationManager?.removeUpdates(this)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        mapHandler.removeCallbacks(centerMapRunnable)
        compassOverlay?.disableCompass()
        locationOverlay?.disableFollowLocation()
        locationOverlay?.disableMyLocation()
        scaleBarOverlay?.disableScaleBar()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState()")
        mapCenterState = mapView.mapCenter as GeoPoint
        mapCenterState?.let {
            outState.putDouble(STATE_LATITUDE, it.latitude)
            outState.putDouble(STATE_LONGITUDE, it.longitude)
            Log.d(
                TAG,
                String.format(
                    "Saving center state: %f, %f",
                    it.latitude,
                    it.longitude
                )
            )
        }
        outState.putDouble(STATE_ZOOM, mapView.zoomLevelDouble)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        saveMapCenterPrefs()
        super.onStop()
    }

    fun setGpx(gpx: Gpx?, displayState: GpxDisplayState, zoom: Boolean) {
        gpx?.let {
            overlayHelper?.setGpx(it)
            gpxDisplayState = displayState
            if (activity != null) (activity as AppCompatActivity).supportInvalidateOptionsMenu()
            if (zoom) {
                disableFollow()
                zoomToBounds(Utils.area(it))
            }
        }
    }

    private fun removeGpx() {
        overlayHelper?.let {
            it.clearGpx()
            gpxDisplayState = GpxDisplayState.IDLE
            if (activity != null) (activity as AppCompatActivity).supportInvalidateOptionsMenu()
        }
    }

    private fun showGpxDialog(onConfirmed: (() -> Unit)? = null) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity(), R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.gpx))
            .setMessage(getString(R.string.discard_current_gpx))
            .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                removeGpx()
                onConfirmed?.invoke()
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
        mapView.post {
            if (mapView.height > 0) {
                mapView.zoomToBoundingBox(box, true, 64)
            } else {
                mapView.viewTreeObserver.addOnGlobalLayoutListener(object :
                    OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        mapView.zoomToBoundingBox(box, true, 64)
                        mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }
    }

    fun setNearbyPlace() {
        val nearbyPlace = listener?.selectedNearbyPlace
        nearbyPlace?.let { showNearbyPlace(it) }
    }

    private fun showNearbyPlace(nearbyPlace: NearbyItem?) {
        if (nearbyPlace == null) {
            return
        }

        val existingMarker = markerViewModel.markers.value?.any {
            it.nearbyId == nearbyPlace.id
        } ?: false

        if (!existingMarker) {
            val highestSeq = markerViewModel.markers.value?.maxByOrNull { it.seq }?.seq ?: 0
            val seq = highestSeq + 1
            val marker = MarkerModel(
                seq = seq,
                latitude = nearbyPlace.lat,
                longitude = nearbyPlace.lon,
                name = nearbyPlace.title ?: "Marker $seq",
                description = nearbyPlace.description ?: "",
                nearbyId = nearbyPlace.id
            )
            markerViewModel.addMarker(marker)
        }

        if (nearbyPlace.id != lastNearbyAnimateToId) {
            disableFollow()
            animateToLatLon(nearbyPlace.lat, nearbyPlace.lon)
            lastNearbyAnimateToId = nearbyPlace.id
        }
    }

    fun showZoomControls(show: Boolean) {
        if (!::mapView.isInitialized) return
        if (show) {
            mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        } else {
            mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        listener?.let {
            it.setUpNavigation(false)
            inflater.inflate(R.menu.menu_main, menu)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (followEnabled) {
            menu.findItem(R.id.action_follow).isVisible = false
            menu.findItem(R.id.action_no_follow).isVisible = true
        } else {
            menu.findItem(R.id.action_follow).isVisible = true
            menu.findItem(R.id.action_no_follow).isVisible = false
        }

        val gpxVisible = gpxDisplayState != GpxDisplayState.IDLE
        menu.findItem(R.id.action_gpx_details).isVisible = gpxVisible
        menu.findItem(R.id.action_gpx_zoom).isVisible = gpxVisible

        listener?.let {
            menu.findItem(R.id.action_privacy_settings).isVisible = it.isPrivacyOptionsRequired()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val fm: FragmentManager
        val itemId = item.itemId
        when (itemId) {
            R.id.action_gpx -> {
                if (gpxDisplayState != GpxDisplayState.IDLE) {
                    showGpxDialog {
                        markerViewModel.markers.value?.forEach {
                            it.routeWaypoint = false
                        }
                        listener?.clearGpx()
                        listener?.selectGpx()
                    }
                } else {
                    listener?.selectGpx()
                }
                return true
            }
            R.id.action_location -> {
                locationViewModel?.currentLocation?.value?.let { location ->
                    mapView.controller.animateTo(GeoPoint(location))
                }
                return true
            }
            R.id.action_follow -> {
                enableFollow()
                Toast.makeText(activity, R.string.follow_enabled, Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_no_follow -> {
                disableFollow()
                Toast.makeText(activity, R.string.follow_disabled, Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_gpx_details -> {
                listener?.addGpxDetailFragment()
                return true
            }
            R.id.action_location_details -> {
                fm = requireActivity().supportFragmentManager
                val locationDetailFragment = LocationDetailFragment()
                locationDetailFragment.show(fm, "location_detail")
                return true
            }
            R.id.action_nearby -> {
                listener?.let { listener ->
                    listener.clearSelectedNearbyPlace()
                    val nearbyCenter = mapView.mapCenter as GeoPoint
                    nearbyCenter.let { center ->
                        listener.addNearbyFragment(center)
                    }
                }
                return true
            }
            R.id.action_gpx_zoom -> {
                disableFollow()
                listener?.let { zoomToBounds(Utils.area(it.getGpx())) }
                return true
            }
            R.id.action_layers -> {
                activity?.let {
                    val anchorView = it.findViewById<View>(R.id.popupAnchorView)
                    val popup = PopupMenu(activity, anchorView)
                    val inflater = popup.menuInflater
                    inflater.inflate(R.menu.menu_tile_sources, popup.menu)
                    val openTopoMapItem = popup.menu.findItem(R.id.otm)
                    val openStreetMapItem = popup.menu.findItem(R.id.osm)
                    val overlayNoneItem = popup.menu.findItem(R.id.none)
                    val overlayHikingItem = popup.menu.findItem(R.id.lonvia_hiking)
                    val overlayCyclingItem = popup.menu.findItem(R.id.lonvia_cycling)
                    when (baseMap) {
                        BASE_MAP_OTM -> openTopoMapItem.isChecked = true
                        BASE_MAP_OSM -> openStreetMapItem.isChecked = true
                    }
                    when (overlay) {
                        OverlayHelper.OVERLAY_NONE -> overlayNoneItem.isChecked = true
                        OverlayHelper.OVERLAY_HIKING -> overlayHikingItem.isChecked = true
                        OverlayHelper.OVERLAY_CYCLING -> overlayCyclingItem.isChecked = true
                    }
                    popup.setOnMenuItemClickListener(this@MapFragment)
                    popup.show()
                    return true
                }
                return false
            }
            R.id.action_settings -> {
                mapCenterState = mapView.mapCenter as GeoPoint
                saveMapCenterPrefs()
                val settingsIntent = Intent(activity, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
            R.id.action_about -> {
                showAboutDialog()
                return true
            }
            R.id.action_privacy_settings -> {
                listener?.showPrivacyOptionsForm()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        activity?.let {
            val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)

            val versionTextView = dialogView.findViewById<TextView>(R.id.appVersion)
            versionTextView.text = getString(R.string.app_version, Utils.getAppVersion(it))

            val authorTextView = dialogView.findViewById<TextView>(R.id.authorName)
            authorTextView.movementMethod = LinkMovementMethod.getInstance()
            authorTextView.text = Utils.fromHtml(
                getString(R.string.app_author)
            )

            val dialog = AlertDialog.Builder(it)
                .setTitle(Utils.getAppName(it))
                .setView(dialogView)
                .setPositiveButton(R.string.close) { dialog, _ -> dialog.dismiss() }
                .create()
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.show()
        }
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
            when (itemId) {
                R.id.otm -> {
                    baseMap = BASE_MAP_OTM
                }
                R.id.osm -> {
                    baseMap = BASE_MAP_OSM
                }
                R.id.none -> {
                    overlay = OverlayHelper.OVERLAY_NONE
                }
                R.id.lonvia_hiking -> {
                    overlay = OverlayHelper.OVERLAY_HIKING
                }
                R.id.lonvia_cycling -> {
                    overlay = OverlayHelper.OVERLAY_CYCLING
                }
            }
            sharedPreferences.edit { putInt(PREF_BASE_MAP, baseMap) }
            sharedPreferences.edit { putInt(PREF_OVERLAY, overlay) }
            setBaseMap()
            setTilesOverlay()
        }
        return true
    }

    override fun onLocationChanged(location: Location) {

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}, mapRotation: $mapRotation")

        requireActivity().runOnUiThread {
            locationViewModel?.currentLocation?.postValue(location)
            if (mapRotation) {
                if (location.hasBearing()) {
                    stopOrientationSensor()
                    MapOrientation.setTargetMapOrientation(mapView, location.bearing)
                } else {
                    // Use device orientation
                    orientationSensor =
                        orientationSensor ?: OrientationSensor(requireContext(), mapView)
                }
            } else {
                stopOrientationSensor()
                if (mapView.mapOrientation != 0f) MapOrientation.reset(mapView)
            }
        }
    }

    private fun stopOrientationSensor() {
        // Log.d(TAG, "stopOrientationSensor()")
        orientationSensor?.stop()
        orientationSensor = null
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(s: String) {}

    override fun onProviderDisabled(s: String) {}

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(
                (context.toString()
                        + " must implement OnFragmentInteractionListener")
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        gpxDisplayState = GpxDisplayState.IDLE
        locationManager = null
        locationOverlay = null
        compassOverlay = null
        scaleBarOverlay = null
        rotationGestureOverlay = null
        gestureOverlay = null
        overlayHelper?.apply { destroy() }
    }

    override fun onUserMapInteraction() {
        if (followEnabled) {
            // follow disabled by gesture -> re-enable with delay
            mapHandler.removeCallbacksAndMessages(null)
            mapHandler.postDelayed(enableFollowRunnable, 5000)
        }
    }

    override fun onCompassClicked() {
        if (!sharedPreferences.getBoolean(SettingsActivity.PREF_TAP_COMPASS_TO_ROTATE, false)) {
            return
        }
        mapRotation = !mapRotation
        //Log.d(TAG, "map rotation set to $mapRotation")
        sharedPreferences.edit { putBoolean(SettingsActivity.PREF_ROTATE, mapRotation) }
        if (mapRotation) {
            val lastLocation = locationViewModel?.currentLocation?.value
            if (lastLocation?.hasBearing() == true) {
                stopOrientationSensor()
                MapOrientation.setTargetMapOrientation(mapView, lastLocation.bearing)
            } else {
                orientationSensor = orientationSensor ?: OrientationSensor(requireContext(), mapView)
            }
            Toast.makeText(requireContext(), R.string.rotation_on, Toast.LENGTH_SHORT).show()
        } else {
            stopOrientationSensor()
            MapOrientation.reset(mapView)
            Toast.makeText(requireContext(), R.string.rotation_off, Toast.LENGTH_SHORT).show()
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
         * Clear GOX so it won't ne restored on config change
         */
        fun clearGpx()

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

        var isFullscreen: Boolean

        /**
         * Need to enable privacy settings or not
         */
        fun isPrivacyOptionsRequired(): Boolean

        /**
         * Consent form
         */
        fun showPrivacyOptionsForm()

        /**
         * Get the ORS API if available
         */
        fun getOpenRouteServiceApi(): OpenRouteServiceApi?

        /**
         * Parse GPX string
         */
        fun parseCalculatedGpx(gpxString: String)

    }

    companion object {
        private const val PARAM_LATITUDE = "latitude"
        private const val PARAM_LONGITUDE = "longitude"
        private const val STATE_LATITUDE = "latitude"
        private const val STATE_LONGITUDE = "longitude"
        private const val STATE_ZOOM = "zoom"
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

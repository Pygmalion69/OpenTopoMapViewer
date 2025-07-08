package org.nitri.opentopo

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import de.k3b.geo.api.GeoPointDto
import de.k3b.geo.io.GeoUri
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import kotlinx.coroutines.launch
import org.nitri.opentopo.SettingsActivity.Companion.PREF_FULLSCREEN
import org.nitri.opentopo.SettingsActivity.Companion.PREF_FULLSCREEN_ON_MAP_TAP
import org.nitri.opentopo.SettingsActivity.Companion.PREF_KEEP_SCREEN_ON
import org.nitri.opentopo.SettingsActivity.Companion.PREF_ORS_API_KEY
import org.nitri.opentopo.model.GpxViewModel
import org.nitri.opentopo.nearby.NearbyFragment
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.ors.OpenRouteService
import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.client.OpenRouteServiceClient
import org.osmdroid.util.GeoPoint
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

open class BaseMainActivity : AppCompatActivity(), MapFragment.OnFragmentInteractionListener,
    GpxDetailFragment.OnFragmentInteractionListener, NearbyFragment.OnFragmentInteractionListener {
    private var openRouteServiceApi: OpenRouteServiceApi? = null
    private var geoPointFromIntent: GeoPointDto? = null
    private var gpxUriString: String? = null
    private var gpxUri: Uri? = null
    private var shouldZoomToGpx = false
    override var selectedNearbyPlace: NearbyItem? = null
    private var mapFragment: MapFragment? = null
    private val gpxViewModel: GpxViewModel by viewModels()
    override var isFullscreen = false

    private var windowInsetsController: WindowInsetsControllerCompat? = null
    private var actionBar: ActionBar? = null
    private lateinit var mapContainer: ViewGroup

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            PREF_KEEP_SCREEN_ON -> {
                mapFragment?.setKeepScreenOn(sharedPreferences.getBoolean(key, false))
            }
        }
    }

    private val cacheChangedReceiver = object: BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            restart()
        }
    }

    private val orsApiKeyChangesReceiver = object: BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
           createOrsApi()
        }
    }

    override fun isPrivacyOptionsRequired(): Boolean {
        return false
    }

    override fun showPrivacyOptionsForm() {
        // NOP
    }

    override fun getOpenRouteServiceApi(): OpenRouteServiceApi? {
        return openRouteServiceApi
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white)

        setSupportActionBar(toolbar)

        if (savedInstanceState != null) {
            gpxUriString = savedInstanceState.getString(GPX_URI_STATE)
        }

        mapContainer = findViewById(R.id.map_container)
        val mainContainer = findViewById<ViewGroup>(R.id.main_container)

        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBarsInsets.top,
                bottom = systemBarsInsets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        handler = Handler(mainLooper)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val intent = intent
        if (intent != null && intent.data != null) {
            handleIntent(intent)
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            addMapFragment()
        }
        if (savedInstanceState != null) {
            mapFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                MAP_FRAGMENT_TAG
            ) as MapFragment?
        }
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let {
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        actionBar = supportActionBar

        isFullscreen = sharedPreferences.getBoolean(PREF_FULLSCREEN, false)
        applyFullscreen()


        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        LocalBroadcastManager.getInstance(this).registerReceiver(cacheChangedReceiver, IntentFilter(CacheSettingsFragment.ACTION_CACHE_CHANGED))
        LocalBroadcastManager.getInstance(this).registerReceiver(orsApiKeyChangesReceiver, IntentFilter(SettingsActivity.ACTION_API_KEY_CHANGED))

        // Test ORS
        val ors = OpenRouteService(getString(R.string.ors_api_key), this)
        lifecycleScope.launch {
            val result = ors.routeRepository.getRoute(
                start = Pair(8.681495, 49.41461),
                end = Pair(8.687872, 49.420318),
                profile = "driving-car"
            )
            Log.d("ORS", "Distance: ${result.routes.firstOrNull()?.summary?.distance} m")
        }

        createOrsApi()
    }

    private fun createOrsApi() {
        val apiKey = sharedPreferences.getString(PREF_ORS_API_KEY, "")
        if (apiKey?.isNotEmpty() == true) {
            openRouteServiceApi = OpenRouteServiceClient.create(apiKey, this@BaseMainActivity)
        }
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        //tapDetector.onTouchEvent(motionEvent);
        return super.dispatchTouchEvent(motionEvent)
    }

    private fun handleIntent(intent: Intent) {
        intent.data?.let { uri ->
            when (uri.scheme) {
                "geo" -> geoPointFromIntent = getGeoPointDtoFromIntent(intent)
                "file", "content" -> {
                    gpxUri = uri
                    gpxUriString = uri.toString()
                    Log.i(TAG, "Uri: $gpxUriString")
                    shouldZoomToGpx = true
                }
                else -> Log.i(TAG, "Unsupported scheme: ${uri.scheme}")
            }
        }
    }

    private fun applyFullscreen() {
        handler.removeCallbacksAndMessages(null)

        // Return early if windowInsetsController is null
        val insetsController = windowInsetsController ?: return

        if (isFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            actionBar?.hide()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                mapContainer.apply {
                    layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                        topMargin = 0
                    }
                }
            }

            handler.postDelayed({ mapFragment?.showZoomControls(false) }, 3000)
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            actionBar?.show()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                actionBar?.height?.let { actionBarHeight ->
                    if (actionBarHeight > 0) {
                        mapContainer.apply {
                            layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                                topMargin = actionBarHeight
                            }
                        }
                    }
                }
            }

            if (mapFragment?.isAdded == true) {
                mapFragment?.showZoomControls(true)
            }
        }

        // Persist the fullscreen state preference
        PreferenceManager.getDefaultSharedPreferences(this).edit().apply {
            putBoolean(PREF_FULLSCREEN, isFullscreen)
            apply()
        }
    }

    open fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        applyFullscreen()
    }

    private fun addMapFragment() {
        if (mapFragmentAdded()) {
            return
        }
        if (geoPointFromIntent == null) {
            mapFragment =
                supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
            if (mapFragment == null) {
                mapFragment = MapFragment.newInstance()
            }
        } else {
            geoPointFromIntent?.let {
                mapFragment = MapFragment.newInstance(
                    it.latitude,
                    it.longitude
                )
            }

        }
        mapFragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(R.id.map_container, it, MAP_FRAGMENT_TAG)
                .commit()
        }
        setGpx()
    }

    private fun mapFragmentAdded(): Boolean {
        val mapFragment = supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG)
        return (mapFragment != null && mapFragment.isAdded)
    }

    override fun addGpxDetailFragment() {
        val gpxDetailFragment = GpxDetailFragment.newInstance()
        supportFragmentManager.beginTransaction().addToBackStack("gpx")
            .replace(R.id.map_container, gpxDetailFragment, GPX_DETAIL_FRAGMENT_TAG)
            .commit()
    }

    override fun addNearbyFragment(nearbyCenterPoint: GeoPoint) {
        val nearbyFragment =
            NearbyFragment.newInstance(nearbyCenterPoint.latitude, nearbyCenterPoint.longitude)
        supportFragmentManager.beginTransaction().addToBackStack("nearby")
            .replace(R.id.map_container, nearbyFragment, NEARBY_FRAGMENT_TAG)
            .commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addMapFragment()
            } else {
                finish()
            }
        }
    }

    override fun selectGpx() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val activityIntent = Intent.createChooser(intent, "GPX")
        activityResultLauncher.launch(activityIntent)
    }

    private var activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let {data ->
                gpxUri = data.data
                shouldZoomToGpx = true
                gpxUri?.let {
                    Log.i(TAG, "Uri: $it")
                    parseGpx(it)
                }
            }
        }
    }

    override fun setGpx() {
        if (!TextUtils.isEmpty(gpxUriString)) {
            parseGpx(Uri.parse(gpxUriString))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                onBackPressedDispatcher.onBackPressed()
            } else {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (!TextUtils.isEmpty(gpxUriString)) {
            outState.putString(GPX_URI_STATE, gpxUriString)
        }
        mapFragment?.let {
            supportFragmentManager.putFragment(outState, MAP_FRAGMENT_TAG, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun setUpNavigation(upNavigation: Boolean) {
        if (upNavigation) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)
        }
    }

    private fun parseGpx(uri: Uri) {
        val parser = GPXParser()
        val contentResolver = contentResolver
        if (contentResolver != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    gpxViewModel.gpx = parser.parse(inputStream)
                    val mapFragment =
                        supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
                    if (mapFragment != null && gpxViewModel.gpx != null) {
                        mapFragment.setGpx(gpxViewModel.gpx, shouldZoomToGpx)
                        gpxUriString = uri.toString()
                        shouldZoomToGpx = false
                    }
                }
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
                Toast.makeText(
                    this, getString(R.string.invalid_gpx) + ": " + e.message,
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(
                    this, getString(R.string.invalid_gpx) + ": " + e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getGeoPointDtoFromIntent(intent: Intent?): GeoPointDto? {
        val uri = intent?.data
        val uriAsString = uri?.toString()
        var geoPointFromIntent: GeoPointDto? = null
        if (uriAsString != null) {
            val parser = GeoUri(GeoUri.OPT_PARSE_INFER_MISSING)
            geoPointFromIntent = parser.fromUri(uriAsString, GeoPointDto())
        }
        return geoPointFromIntent
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(cacheChangedReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(orsApiKeyChangesReceiver)
    }

    override fun getGpx(): Gpx? {
        return gpxViewModel.gpx
    }

    override fun clearGpx() {
        gpxViewModel.gpx = null
        gpxUriString = null
    }

    override fun showNearbyPlace(nearbyItem: NearbyItem?) {
        selectedNearbyPlace = nearbyItem
        supportFragmentManager.popBackStack()
        addMapFragment()
        val mapFragment = supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
        mapFragment?.setNearbyPlace()
    }

    override fun clearSelectedNearbyPlace() {
        selectedNearbyPlace = null
    }

    override fun onMapTap() {
        if (mapFragmentAdded()) {
            if (sharedPreferences.getBoolean(PREF_FULLSCREEN_ON_MAP_TAP, false)) {
                toggleFullscreen()
            }
        }
    }

    override fun onMapLongPress() {}

    /**
     * Restart the rude way
     */
    private fun restart() {
        finish()
        startActivity(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                0,
                0,
                Color.TRANSPARENT
            )
        } else {
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private val TAG = BaseMainActivity::class.java.simpleName
        private const val MAP_FRAGMENT_TAG = "map_fragment"
        const val GPX_DETAIL_FRAGMENT_TAG = "gpx_detail_fragment"
        const val WAY_POINT_DETAIL_FRAGMENT_TAG = "way_point_detail_fragment"
        private const val NEARBY_FRAGMENT_TAG = "nearby_fragment"
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val GPX_URI_STATE = "gpx_uri"
    }
}
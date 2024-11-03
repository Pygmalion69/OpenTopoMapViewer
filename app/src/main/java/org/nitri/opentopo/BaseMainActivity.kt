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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import de.k3b.geo.api.GeoPointDto
import de.k3b.geo.io.GeoUri
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import org.nitri.opentopo.CacheSettingsFragment.Companion.PREF_CACHE_SIZE
import org.nitri.opentopo.CacheSettingsFragment.Companion.PREF_EXTERNAL_STORAGE
import org.nitri.opentopo.CacheSettingsFragment.Companion.PREF_TILE_CACHE
import org.nitri.opentopo.SettingsActivity.Companion.PREF_FULLSCREEN
import org.nitri.opentopo.SettingsActivity.Companion.PREF_FULLSCREEN_ON_MAP_TAP
import org.nitri.opentopo.SettingsActivity.Companion.PREF_KEEP_SCREEN_ON
import org.nitri.opentopo.model.GpxViewModel
import org.nitri.opentopo.nearby.NearbyFragment
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.osmdroid.util.GeoPoint
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

open class BaseMainActivity : AppCompatActivity(), MapFragment.OnFragmentInteractionListener,
    GpxDetailFragment.OnFragmentInteractionListener, NearbyFragment.OnFragmentInteractionListener {
    private var mGeoPointFromIntent: GeoPointDto? = null
    private var mGpxUriString: String? = null
    private var mGpxUri: Uri? = null
    private var mZoomToGpx = false
    override var selectedNearbyPlace: NearbyItem? = null
    private var mMapFragment: MapFragment? = null
    private val gpxViewModel: GpxViewModel by viewModels()
    override var isFullscreen = false
        set(value) {
            applyFullscreen(value)
            field = value
        }
    private var windowInsetsController: WindowInsetsControllerCompat? = null
    private var actionBar: ActionBar? = null

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            PREF_KEEP_SCREEN_ON -> {
                mMapFragment?.setKeepScreenOn(sharedPreferences.getBoolean(key, false))
            }
        }
    }

    private val cacheChangedReceiver = object: BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            restart()
        }

    }

    override fun isPrivacyOptionsRequired(): Boolean {
        return false
    }

    override fun showPrivacyOptionsForm() {
        // NOP
    }

    private lateinit var mPrefs: SharedPreferences
    private lateinit var handler: Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState != null) {
            mGpxUriString = savedInstanceState.getString(GPX_URI_STATE)
        }
        handler = Handler(mainLooper)
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
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
            mMapFragment = supportFragmentManager.getFragment(
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

        mPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        LocalBroadcastManager.getInstance(this).registerReceiver(cacheChangedReceiver, IntentFilter(CacheSettingsFragment.ACTION_CACHE_CHANGED))
        Log.d(TAG, "SEMH receiver registered")

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        //tapDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev)
    }

    private fun handleIntent(intent: Intent) {
        intent.data?.let { uri ->
            when (uri.scheme) {
                "geo" -> mGeoPointFromIntent = getGeoPointDtoFromIntent(intent)
                "file", "content" -> {
                    mGpxUri = uri
                    mGpxUriString = uri.toString()
                    Log.i(TAG, "Uri: $mGpxUriString")
                    mZoomToGpx = true
                }
                else -> Log.i(TAG, "Unsupported scheme: ${uri.scheme}")
            }
        }
    }

    private fun applyFullscreen(fullscreen: Boolean) {
        handler.removeCallbacksAndMessages(null)

        // Return early if windowInsetsController is null
        val insetsController = windowInsetsController ?: return

        if (fullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            actionBar?.hide()
            handler.postDelayed({ mMapFragment?.showZoomControls(false) }, 3000)
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            actionBar?.show()
            mMapFragment?.showZoomControls(true)
        }

        // Persist the fullscreen state preference
        PreferenceManager.getDefaultSharedPreferences(this).edit().apply {
            putBoolean(PREF_FULLSCREEN, this@BaseMainActivity.isFullscreen)
            apply()
        }
    }

    open fun toggleFullscreen() {
        isFullscreen = !isFullscreen
    }

    private fun addMapFragment() {
        if (mapFragmentAdded()) {
            return
        }
        if (mGeoPointFromIntent == null) {
            mMapFragment =
                supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
            if (mMapFragment == null) {
                mMapFragment = MapFragment.newInstance()
            }
        } else {
            mGeoPointFromIntent?.let {
                mMapFragment = MapFragment.newInstance(
                    it.latitude,
                    it.longitude
                )
            }

        }
        mMapFragment?.let {
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
                mGpxUri = data.data
                mZoomToGpx = true
                mGpxUri?.let {
                    Log.i(TAG, "Uri: $it")
                    parseGpx(it)
                }
            }
        }
    }

    override fun setGpx() {
        if (!TextUtils.isEmpty(mGpxUriString)) {
            parseGpx(Uri.parse(mGpxUriString))
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
        if (!TextUtils.isEmpty(mGpxUriString)) {
            outState.putString(GPX_URI_STATE, mGpxUriString)
        }
        mMapFragment?.let {
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
                        mapFragment.setGpx(gpxViewModel.gpx, mZoomToGpx)
                        mGpxUriString = uri.toString()
                        mZoomToGpx = false
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
        var pointFromIntent: GeoPointDto? = null
        if (uriAsString != null) {
            val parser = GeoUri(GeoUri.OPT_PARSE_INFER_MISSING)
            pointFromIntent = parser.fromUri(uriAsString, GeoPointDto())
        }
        return pointFromIntent
    }

    override fun onDestroy() {
        super.onDestroy()
        mPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(cacheChangedReceiver)
    }

    override fun getGpx(): Gpx? {
        return gpxViewModel.gpx
    }

    override fun clearGpx() {
        gpxViewModel.gpx = null
        mGpxUriString = null
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
            if (mPrefs.getBoolean(PREF_FULLSCREEN_ON_MAP_TAP, false)) {
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
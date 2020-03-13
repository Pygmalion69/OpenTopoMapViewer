package org.nitri.opentopo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import org.nitri.opentopo.model.LocationViewModel;
import org.nitri.opentopo.nearby.entity.NearbyItem;
import org.nitri.opentopo.overlay.OverlayHelper;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.Objects;

import io.ticofab.androidgpxparser.parser.domain.Gpx;


public class MapFragment extends Fragment implements LocationListener, PopupMenu.OnMenuItemClickListener {

    private MapView mMapView;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private LocationManager mLocationManager;
    private OverlayHelper mOverlayHelper;
    private Handler mMapHandler = new Handler();
    private Runnable mCenterRunnable = new Runnable() {

        @Override
        public void run() {
            if (mMapView != null && mLocationViewModel.getCurrentLocation().getValue() != null) {
                mMapView.getController().animateTo(new GeoPoint(mLocationViewModel.getCurrentLocation().getValue()));
            }
            mMapHandler.postDelayed(this, 5000);
        }
    };

    private MapListener mDragListener = new MapListener() {
        @Override
        public boolean onScroll(ScrollEvent event) {
            if (mFollow && mMapHandler != null && mCenterRunnable != null) {
                mMapHandler.removeCallbacks(mCenterRunnable);
                mMapHandler.postDelayed(mCenterRunnable, 6000);
            }
            return true;
        }

        @Override
        public boolean onZoom(ZoomEvent event) {
            return false;
        }
    };

    private boolean mFollow;

    private OnFragmentInteractionListener mListener;

    private final static String PARAM_LATITUDE = "latitude";
    private final static String PARAM_LONGITUDE = "longitude";

    private final static String STATE_LATITUDE = "latitude";
    private final static String STATE_LONGITUDE = "longitude";
    private final static String STATE_ZOOM = "zoom";


    private SharedPreferences mPrefs;
    static final String MAP_PREFS = "map_prefs";

    private final static String PREF_BASE_MAP = "base_map";
    private final static String PREF_OVERLAY = "overlay";
    private final static String PREF_LATITUDE = "latitude";
    private final static String PREF_LONGITUDE = "longitude";

    private final static int BASE_MAP_OTM = 1;
    private final static int BASE_MAP_OSM = 2;

    private final static double DEFAULT_ZOOM = 15d;

    private int mBaseMap = BASE_MAP_OTM;

    private static final String TAG = MapFragment.class.getSimpleName();
    private TextView mCopyRightView;
    private int mOverlay = OverlayHelper.OVERLAY_NONE;
    private GeoPoint mMapCenterState;
    private double mZoomState = DEFAULT_ZOOM;
    private int mLastNearbyAnimateToId;

    private LocationViewModel mLocationViewModel;

    public MapFragment() {
    }

    static MapFragment newInstance() {
        return new MapFragment();
    }

    static MapFragment newInstance(double lat, double lon) {
        MapFragment mapFragment = new MapFragment();
        Bundle arguments = new Bundle();
        arguments.putDouble(PARAM_LATITUDE, lat);
        arguments.putDouble(PARAM_LONGITUDE, lon);
        mapFragment.setArguments(arguments);
        return mapFragment;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Context context = requireActivity().getApplicationContext();
        mPrefs = requireActivity().getSharedPreferences(MAP_PREFS, Context.MODE_PRIVATE);
        IConfigurationProvider configuration = Configuration.getInstance();
        configuration.setUserAgentValue(BuildConfig.APPLICATION_ID);
        configuration.load(context, mPrefs);
        mBaseMap = mPrefs.getInt(PREF_BASE_MAP, BASE_MAP_OTM);
        mOverlay = mPrefs.getInt(PREF_OVERLAY, OverlayHelper.OVERLAY_NONE);
        mLocationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        mLocationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            OnNmeaMessageListener nmeaListener = (s, l) -> {
                if (mLocationViewModel != null && mLocationViewModel.getCurrentNmea() != null) {
                    mLocationViewModel.getCurrentNmea().setValue(s);
                }
            };
            if (requireActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationManager.addNmeaListener(nmeaListener);
            }
        } else {
            android.location.GpsStatus.NmeaListener nmeaListener = (l, s) -> {
                if (mLocationViewModel != null && mLocationViewModel.getCurrentNmea() != null) {
                    mLocationViewModel.getCurrentNmea().setValue(s);
                }
            };
            mLocationManager.addNmeaListener(nmeaListener);
        }
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_LATITUDE) && savedInstanceState.containsKey(STATE_LONGITUDE)) {
                mMapCenterState = new GeoPoint(savedInstanceState.getDouble(STATE_LATITUDE, 0),
                        savedInstanceState.getDouble(STATE_LONGITUDE, 0));
                Log.d(TAG, String.format("Restoring center state: %f, %f", mMapCenterState.getLatitude(), mMapCenterState.getLongitude()));
                mZoomState = savedInstanceState.getDouble(STATE_ZOOM, DEFAULT_ZOOM);
            } else {
                Log.d(TAG, "No center state delivered");
            }
        }
        if (mMapCenterState == null) {
            Log.d(TAG, "No saved center state");
            float prefLat = mPrefs.getFloat(PREF_LATITUDE, 0f);
            float prefLon = mPrefs.getFloat(PREF_LONGITUDE, 0f);
            mMapCenterState = new GeoPoint(prefLat, prefLon);
            Log.d(TAG, String.format("Restoring center state from prefs: %f, %f", prefLat, prefLon));
        }

        if (mMapCenterState != null) {
            mMapView.getController().setCenter(mMapCenterState);
        } else if (mLocationViewModel.getCurrentLocation() != null && mLocationViewModel.getCurrentLocation().getValue() != null) {
            mMapView.getController().setCenter(new GeoPoint(mLocationViewModel.getCurrentLocation().getValue().getLatitude(),
                    mLocationViewModel.getCurrentLocation().getValue().getLongitude()));
        }

        mMapView.getController().setZoom(mZoomState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mMapView = view.findViewById(R.id.mapview);

        final DisplayMetrics dm = this.getResources().getDisplayMetrics();

        FragmentActivity activity = getActivity();

        if (activity != null) {
            mCompassOverlay = new CompassOverlay(getActivity(), new InternalCompassOrientationProvider(getActivity()),
                    mMapView);
            mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getActivity()),
                    mMapView);

            Bitmap bmCrosshairs = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_crosshairs);

            mLocationOverlay.setPersonIcon(bmCrosshairs);
            mLocationOverlay.setPersonHotspot(bmCrosshairs.getWidth() / 2f, bmCrosshairs.getHeight() / 2f);

            mScaleBarOverlay = new ScaleBarOverlay(mMapView);
            mScaleBarOverlay.setCentred(true);
            mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);

            mRotationGestureOverlay = new RotationGestureOverlay(mMapView);
            mRotationGestureOverlay.setEnabled(true);

            mMapView.setMaxZoomLevel(17d);
            mMapView.setTilesScaledToDpi(true);
            mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
            mMapView.setMultiTouchControls(true);
            mMapView.setFlingEnabled(true);
            mMapView.getOverlays().add(this.mLocationOverlay);
            mMapView.getOverlays().add(this.mCompassOverlay);
            mMapView.getOverlays().add(this.mScaleBarOverlay);

            mMapView.addMapListener(new DelayedMapListener(mDragListener));

            mCopyRightView = view.findViewById(R.id.copyrightView);

            setBaseMap();

            mLocationOverlay.enableMyLocation();
            mLocationOverlay.disableFollowLocation();
            mLocationOverlay.setOptionsMenuEnabled(true);
            mCompassOverlay.enableCompass();
            mMapView.setVisibility(View.VISIBLE);
            mOverlayHelper = new OverlayHelper(getActivity(), mMapView);

            setTilesOverlay();
        }

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListener.setGpx();
        Bundle arguments = getArguments();
        // Move to received geo intent coordinates
        if (arguments != null) {
            if (arguments.containsKey(PARAM_LATITUDE) && arguments.containsKey(PARAM_LONGITUDE)) {
                final double lat = arguments.getDouble(PARAM_LATITUDE);
                final double lon = arguments.getDouble(PARAM_LONGITUDE);
                animateToLatLon(lat, lon);
            }
        }
        if (mListener.getSelectedNearbyPlace() != null) {
            showNearbyPlace(mListener.getSelectedNearbyPlace());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    requireActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationViewModel.getCurrentLocation().setValue(mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
            }
        } else {
            mLocationViewModel.getCurrentLocation().setValue(mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
        }


    }

    private void animateToLatLon(double lat, double lon) {
        mMapHandler.postDelayed(() -> {
            if (mMapView != null) {
                disableFollow();
                mMapView.getController().animateTo(new GeoPoint(lat, lon));
            }
        }, 500);
    }

    private void setBaseMap() {
        switch (mBaseMap) {
            case BASE_MAP_OTM:
                mMapView.setTileSource(TileSourceFactory.OpenTopo);
                break;
            case BASE_MAP_OSM:
                mMapView.setTileSource(TileSourceFactory.MAPNIK);
                break;
        }
        mMapView.invalidate();

        //final OnlineTileSourceBase localTopo = new XYTileSource("OpenTopoMap", 0, 19, 256, ".png",
        //        new String[]{"http://192.168.2.108/hot/"}, "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Kartendarstellung: © OpenTopoMap (CC-BY-SA)");
        //mMapView.setTileSource(localTopo);

        setCopyrightNotice();
    }

    private void setTilesOverlay() {
        mOverlayHelper.setTilesOverlay(mOverlay);
        setCopyrightNotice();
    }

    private void setCopyrightNotice() {

        StringBuilder copyrightStringBuilder = new StringBuilder();
        String mapCopyRightNotice = mMapView.getTileProvider().getTileSource().getCopyrightNotice();
        copyrightStringBuilder.append(mapCopyRightNotice);
        if (mOverlayHelper != null) {
            String overlayCopyRightNotice = mOverlayHelper.getCopyrightNotice();
            if (!TextUtils.isEmpty(mapCopyRightNotice) && !TextUtils.isEmpty(overlayCopyRightNotice)) {
                copyrightStringBuilder.append(", ");
            }
            copyrightStringBuilder.append(overlayCopyRightNotice);
        }
        String copyRightNotice = copyrightStringBuilder.toString();

        if (!TextUtils.isEmpty(copyRightNotice)) {
            mCopyRightView.setText(copyRightNotice);
            mCopyRightView.setVisibility(View.VISIBLE);
        } else {
            mCopyRightView.setVisibility(View.GONE);
        }
    }

    @SuppressLint("MissingPermission")
    private void initMap() {

        if (mFollow) {
            mLocationOverlay.enableFollowLocation();
            mMapHandler.removeCallbacks(mCenterRunnable);
            mMapHandler.post(mCenterRunnable);
        }
        mLocationOverlay.enableMyLocation();
        mCompassOverlay.enableCompass();
        mScaleBarOverlay.enableScaleBar();

        mMapView.invalidate();
    }

    private void enableFollow() {
        mFollow = true;
        if (getActivity() != null)
            ((AppCompatActivity) getActivity()).supportInvalidateOptionsMenu();
        mLocationOverlay.enableFollowLocation();
        mMapHandler.removeCallbacks(mCenterRunnable);
        mMapHandler.post(mCenterRunnable);
    }

    private void disableFollow() {
        mFollow = false;
        if (getActivity() != null)
            ((AppCompatActivity) getActivity()).supportInvalidateOptionsMenu();
        mLocationOverlay.disableFollowLocation();
        mMapHandler.removeCallbacks(mCenterRunnable);
    }

    private void saveMapCenterPrefs() {
        if (mMapCenterState != null) {
            mPrefs.edit().putFloat(PREF_LATITUDE, (float) mMapCenterState.getLatitude()).apply();
            mPrefs.edit().putFloat(PREF_LONGITUDE, (float) mMapCenterState.getLongitude()).apply();
            Log.d(TAG, String.format("Saving center prefs: %f, %f", mMapCenterState.getLatitude(), mMapCenterState.getLongitude()));
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        //File basePath = Configuration.getInstance().getOsmdroidBasePath();
        File cache = Configuration.getInstance().getOsmdroidTileCache();
        Log.d(TAG, "Cache: " + cache.getAbsolutePath());
        initMap();

        if (mMapCenterState != null) {
            mMapView.getController().setCenter(mMapCenterState);
            mMapCenterState = null; // We're done with the old state
        } else if (mLocationViewModel.getCurrentLocation() != null && mLocationViewModel.getCurrentLocation().getValue() != null) {
            mMapView.getController().setCenter(new GeoPoint(mLocationViewModel.getCurrentLocation().getValue().getLatitude(),
                    mLocationViewModel.getCurrentLocation().getValue().getLongitude()));
        }

        if (mLocationManager != null) {
            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this);
            } catch (Exception ex) {
                ex.printStackTrace();

            }
            try {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onPause() {

        if (mMapView != null) {
            mMapCenterState = (GeoPoint) mMapView.getMapCenter();
        }

        try {
            mLocationManager.removeUpdates(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mMapHandler.removeCallbacks(mCenterRunnable);

        mCompassOverlay.disableCompass();
        mLocationOverlay.disableFollowLocation();
        mLocationOverlay.disableMyLocation();
        mScaleBarOverlay.disableScaleBar();

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "onSaveInstanceState()");
        if (mMapView != null) {
            mMapCenterState = (GeoPoint) mMapView.getMapCenter();
            outState.putDouble(STATE_LATITUDE, mMapCenterState.getLatitude());
            outState.putDouble(STATE_LONGITUDE, mMapCenterState.getLongitude());
            Log.d(TAG, String.format("Saving center state: %f, %f", mMapCenterState.getLatitude(), mMapCenterState.getLongitude()));
            outState.putDouble(STATE_ZOOM, mMapView.getZoomLevelDouble());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        saveMapCenterPrefs();
        super.onStop();
    }

    void setGpx(Gpx gpx, boolean zoom) {
        mOverlayHelper.setGpx(gpx);
        if (getActivity() != null)
            ((AppCompatActivity) getActivity()).supportInvalidateOptionsMenu();
        if (zoom) {
            disableFollow();
            zoomToBounds(Util.area(gpx));
        }
    }

    private void showGpxdialog() {
        final AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()), R.style.AlertDialogTheme);
        builder.setTitle(getString(R.string.gpx))
                .setMessage(getString(R.string.discard_current_gpx))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (mOverlayHelper != null) {
                        mOverlayHelper.clearGpx();
                        if (getActivity() != null)
                            ((AppCompatActivity) getActivity()).supportInvalidateOptionsMenu();
                    }
                    if (mListener != null) {
                        mListener.selectGpx();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .setIcon(R.drawable.ic_alert);
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();

    }

    private void zoomToBounds(final BoundingBox box) {
        if (mMapView.getHeight() > 0) {
            mMapView.zoomToBoundingBox(box, true, 64);
        } else {
            ViewTreeObserver vto = mMapView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mMapView.zoomToBoundingBox(box, true, 64);
                    ViewTreeObserver vto = mMapView.getViewTreeObserver();
                    vto.removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    void setNearbyPlace() {
        NearbyItem nearbyPlace = mListener.getSelectedNearbyPlace();
        showNearbyPlace(nearbyPlace);
    }

    private void showNearbyPlace(NearbyItem nearbyPlace) {
        mOverlayHelper.setNearby(nearbyPlace);
        if (nearbyPlace.getId() != mLastNearbyAnimateToId) {
            // Animate only once
            animateToLatLon(nearbyPlace.getLat(), nearbyPlace.getLon());
            mLastNearbyAnimateToId = nearbyPlace.getId();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mListener.setUpNavigation(false);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (mFollow) {
            menu.findItem(R.id.action_follow).setVisible(false);
            menu.findItem(R.id.action_no_follow).setVisible(true);
        } else {
            menu.findItem(R.id.action_follow).setVisible(true);
            menu.findItem(R.id.action_no_follow).setVisible(false);
        }
        if (mOverlayHelper != null && mOverlayHelper.hasGpx()) {
            menu.findItem(R.id.action_gpx_details).setVisible(true);
            menu.findItem(R.id.action_gpx_zoom).setVisible(true);
        } else {
            menu.findItem(R.id.action_gpx_details).setVisible(false);
            menu.findItem(R.id.action_gpx_zoom).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        FragmentManager fm;

        switch (item.getItemId()) {
            case R.id.action_gpx:
                if (mOverlayHelper != null && mOverlayHelper.hasGpx()) {
                    showGpxdialog();
                } else {
                    mListener.selectGpx();
                }
                return true;
            case R.id.action_location:
                if (mLocationViewModel.getCurrentLocation() != null && mLocationViewModel.getCurrentLocation().getValue() != null) {
                    mMapView.getController().animateTo(new GeoPoint(mLocationViewModel.getCurrentLocation().getValue()));
                }
                return true;
            case R.id.action_follow:
                enableFollow();
                Toast.makeText(getActivity(), R.string.follow_enabled, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_no_follow:
                disableFollow();
                Toast.makeText(getActivity(), R.string.follow_disabled, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_gpx_details:
                if (mListener != null)
                    mListener.addGpxDetailFragment();
                return true;
            case R.id.action_location_details:
                fm = requireActivity().getSupportFragmentManager();
                LocationDetailFragment locationDetailFragment = new LocationDetailFragment();
                locationDetailFragment.show(fm, "location_detail");
                return true;
            case R.id.action_nearby:
                if (mListener != null) {
                    mListener.clearSelectedNearbyPlace();
                    GeoPoint nearbyCenter = null;
                    if (mMapView != null) {
                        nearbyCenter = (GeoPoint) mMapView.getMapCenter();
                        mListener.addNearbyFragment(nearbyCenter);
                    }
                    if (nearbyCenter == null && mLocationViewModel.getCurrentLocation() != null &&
                            mLocationViewModel.getCurrentLocation().getValue() != null) {
                        nearbyCenter = new GeoPoint(mLocationViewModel.getCurrentLocation().getValue().getLatitude(),
                                mLocationViewModel.getCurrentLocation().getValue().getLongitude());
                        mListener.addNearbyFragment(nearbyCenter);
                    }
                    if (nearbyCenter == null) {
                        Toast.makeText(getActivity(), R.string.location_unknown, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            case R.id.action_cache_settings:
                if (mMapView != null) {
                    mMapCenterState = (GeoPoint) mMapView.getMapCenter();
                }
                saveMapCenterPrefs();
                fm = requireActivity().getSupportFragmentManager();
                CacheSettingsFragment cacheSettingsFragment = new CacheSettingsFragment();
                cacheSettingsFragment.show(fm, "cache_settings");
                return true;
            case R.id.action_gpx_zoom:
                disableFollow();
                zoomToBounds(Util.area(mListener.getGpx()));
                return true;
            case R.id.action_layers:
                if (getActivity() != null) {
                    View anchorView = getActivity().findViewById(R.id.popupAnchorView);
                    PopupMenu popup = new PopupMenu(getActivity(), anchorView);
                    android.view.MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.menu_tile_sources, popup.getMenu());

                    MenuItem openTopoMapItem = popup.getMenu().findItem(R.id.otm);
                    MenuItem openStreetMapItem = popup.getMenu().findItem(R.id.osm);
                    MenuItem overlayNoneItem = popup.getMenu().findItem(R.id.none);
                    MenuItem overlayHikingItem = popup.getMenu().findItem(R.id.lonvia_hiking);
                    MenuItem overlayCyclingItem = popup.getMenu().findItem(R.id.lonvia_cycling);

                    switch (mBaseMap) {
                        case BASE_MAP_OTM:
                            openTopoMapItem.setChecked(true);
                            break;
                        case BASE_MAP_OSM:
                            openStreetMapItem.setChecked(true);
                            break;
                    }

                    switch (mOverlay) {
                        case OverlayHelper.OVERLAY_NONE:
                            overlayNoneItem.setChecked(true);
                            break;
                        case OverlayHelper.OVERLAY_HIKING:
                            overlayHikingItem.setChecked(true);
                            break;
                        case OverlayHelper.OVERLAY_CYCLING:
                            overlayCyclingItem.setChecked(true);
                            break;
                    }

                    popup.setOnMenuItemClickListener(MapFragment.this);
                    popup.show();
                    return true;
                } else {
                    return false;
                }

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Popup menu click
     *
     * @param menuItem
     * @return
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (!menuItem.isChecked()) {
            menuItem.setChecked(true);
            switch (menuItem.getItemId()) {
                case R.id.otm:
                    mBaseMap = BASE_MAP_OTM;
                    break;
                case R.id.osm:
                    mBaseMap = BASE_MAP_OSM;
                    break;
                case R.id.none:
                    mOverlay = OverlayHelper.OVERLAY_NONE;
                    break;
                case R.id.lonvia_hiking:
                    mOverlay = OverlayHelper.OVERLAY_HIKING;
                    break;
                case R.id.lonvia_cycling:
                    mOverlay = OverlayHelper.OVERLAY_CYCLING;
                    break;
            }
            mPrefs.edit().putInt(PREF_BASE_MAP, mBaseMap).apply();
            mPrefs.edit().putInt(PREF_OVERLAY, mOverlay).apply();
            setBaseMap();
            setTilesOverlay();
        }
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocationViewModel.getCurrentLocation().setValue(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationManager = null;
        mLocationOverlay = null;
        mCompassOverlay = null;
        mScaleBarOverlay = null;
        mRotationGestureOverlay = null;
        if (mOverlayHelper != null)
            mOverlayHelper.destroy();
        mMapView = null;
    }

    public interface OnFragmentInteractionListener {

        /**
         * Start GPX file selection flow
         */
        void selectGpx();

        /**
         * Request to set a GPX layer, e.g. after a configuration change
         */
        void setGpx();

        /**
         * Retrieve the current GPX
         *
         * @return Gpx
         */
        Gpx getGpx();

        /**
         * Present GPX details
         */
        void addGpxDetailFragment();

        /**
         * Present nearby items
         */
        void addNearbyFragment(GeoPoint nearbyCenterPoint);

        /**
         * Set up navigation arrow
         */
        void setUpNavigation(boolean upNavigation);

        /**
         * Get selected nearby item to show on map
         *
         * @return NearbyItem
         */
        NearbyItem getSelectedNearbyPlace();

        /**
         * Clear selected nearby place
         */
        void clearSelectedNearbyPlace();
    }

}


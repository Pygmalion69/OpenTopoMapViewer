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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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

import java.util.Objects;

import io.ticofab.androidgpxparser.parser.domain.Gpx;


public class MapFragment extends Fragment implements LocationListener, PopupMenu.OnMenuItemClickListener {

    private MapView mMapView;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private LocationManager mLocationManager;
    private Location mCurrentLocation = null;
    private OverlayHelper mOverlayHelper;
    private Handler mMapHandler = new Handler();
    private Runnable mCenterRunnable = new Runnable() {

        @Override
        public void run() {
            if (mMapView != null && mCurrentLocation != null) {
                mMapView.getController().animateTo(new GeoPoint(mCurrentLocation));
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

    private SharedPreferences mPrefs;
    private static final String MAP_PREFS = "map_prefs";

    private final static String PREF_BASE_MAP = "base_map";
    private final static String PREF_OVERLAY = "overlay";

    private final static int BASE_MAP_OTM = 1;
    private final static int BASE_MAP_OSM = 2;


    private int mBaseMap = BASE_MAP_OTM;

    private static final String TAG = MapFragment.class.getSimpleName();
    private TextView mCopyRightView;
    private int mOverlay = OverlayHelper.OVERLAY_NONE;
    private GeoPoint mMapCenterState;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    public static MapFragment newInstance(double lat, double lon) {
        MapFragment mapFragment = new MapFragment();
        Bundle arguments = new Bundle();
        arguments.putDouble(PARAM_LATITUDE, lat);
        arguments.putDouble(PARAM_LONGITUDE, lon);
        mapFragment.setArguments(arguments);
        return mapFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        Context context = requireActivity().getApplicationContext();
        IConfigurationProvider configuration = Configuration.getInstance();
        configuration.setUserAgentValue(BuildConfig.APPLICATION_ID);
        configuration.load(context, PreferenceManager.getDefaultSharedPreferences(context));
        mPrefs = requireActivity().getSharedPreferences(MAP_PREFS, Context.MODE_PRIVATE);
        mBaseMap = mPrefs.getInt(PREF_BASE_MAP, BASE_MAP_OTM);
        mOverlay = mPrefs.getInt(PREF_OVERLAY, OverlayHelper.OVERLAY_NONE);
        mLocationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mMapCenterState = new GeoPoint(savedInstanceState.getDouble(STATE_LATITUDE, 0),
                    savedInstanceState.getDouble(STATE_LONGITUDE, 0));
        }
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

            mMapView.getController().setZoom(15d);
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
                mCurrentLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
        } else {
            mCurrentLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }

        if (mMapCenterState != null) {
            mMapView.getController().setCenter(mMapCenterState);
            mMapCenterState = null; // We're done with the old state
        } else if (mCurrentLocation != null) {
            mMapView.getController().setCenter(new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
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

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        //File basePath = Configuration.getInstance().getOsmdroidBasePath();
        //File cache = Configuration.getInstance().getOsmdroidTileCache();
        initMap();
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
        super.onPause();
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
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapCenterState = (GeoPoint) mMapView.getMapCenter();
            outState.putDouble(STATE_LATITUDE, mMapCenterState.getLatitude());
            outState.putDouble(STATE_LONGITUDE, mMapCenterState.getLongitude());
        }
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

    public void zoomToBounds(final BoundingBox box) {
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

    public void setNearbyPlace() {
        NearbyItem nearbyPlace = mListener.getSelectedNearbyPlace();
        showNearbyPlace(nearbyPlace);
    }

    private void showNearbyPlace(NearbyItem nearbyPlace) {
        mOverlayHelper.setNearby(nearbyPlace);
        animateToLatLon(nearbyPlace.getLat(), nearbyPlace.getLon());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mListener.setUpNavigation(false);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_gpx:
                if (mOverlayHelper != null && mOverlayHelper.hasGpx()) {
                    showGpxdialog();
                } else {
                    mListener.selectGpx();
                }
                return true;
            case R.id.action_location:
                if (mCurrentLocation != null) {
                    mMapView.getController().animateTo(new GeoPoint(mCurrentLocation));
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
            case R.id.action_nearby:
                if (mListener != null)
                    if (mCurrentLocation != null) {
                        mListener.addNearbyFragment(mCurrentLocation);
                    } else {
                        Toast.makeText(getActivity(), R.string.location_unknown, Toast.LENGTH_SHORT).show();
                    }
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
        mCurrentLocation = location;
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
    public void onAttach(Context context) {
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
        mCurrentLocation = null;
        mLocationOverlay = null;
        mCompassOverlay = null;
        mScaleBarOverlay = null;
        mRotationGestureOverlay = null;
        mOverlayHelper.destroy();
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
        void addNearbyFragment(Location location);

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
    }

}


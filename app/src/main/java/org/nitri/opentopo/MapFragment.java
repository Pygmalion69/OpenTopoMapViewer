package org.nitri.opentopo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

import org.nitri.opentopo.overlay.OverlayHelper;
import org.osmdroid.config.Configuration;
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

    final static String PARAM_LATITUDE = "latitude";
    final static String PARAM_LONGITUDE = "longitude";

    private static final String TAG = MapFragment.class.getSimpleName();

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
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
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
            mLocationOverlay.setPersonHotspot(bmCrosshairs.getWidth() / 2, bmCrosshairs.getHeight() / 2);

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

            mMapView.setTileSource(TileSourceFactory.OpenTopo);

            //final OnlineTileSourceBase localTopo = new XYTileSource("OpenTopoMap", 0, 19, 256, ".png",
            //        new String[]{"http://192.168.2.108/hot/"}, "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Kartendarstellung: © OpenTopoMap (CC-BY-SA)");
            //mMapView.setTileSource(localTopo);

            String copyRightNotice = mMapView.getTileProvider().getTileSource().getCopyrightNotice();
            TextView copyRightView = view.findViewById(R.id.copyrightView);

            if (!TextUtils.isEmpty(copyRightNotice)) {
                copyRightView.setText(copyRightNotice);
                copyRightView.setVisibility(View.VISIBLE);
            } else {
                copyRightView.setVisibility(View.GONE);
            }

            mMapView.addMapListener(new DelayedMapListener(mDragListener));

            mLocationOverlay.enableMyLocation();
            mLocationOverlay.enableFollowLocation();
            mLocationOverlay.setOptionsMenuEnabled(true);
            mCompassOverlay.enableCompass();
            mMapView.setVisibility(View.VISIBLE);
            mOverlayHelper = new OverlayHelper(getActivity(), mMapView);
        }

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListener.setGpx();
        Bundle arguments = getArguments();
        // Move to received geo intent coordinates
        if (arguments != null && arguments.containsKey(PARAM_LATITUDE) && arguments.containsKey(PARAM_LONGITUDE)) {
            final double lat = arguments.getDouble(PARAM_LATITUDE);
            final double lon = arguments.getDouble(PARAM_LONGITUDE);
            mMapHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mMapView != null) {
                        disableFollow();
                        mMapView.getController().animateTo(new GeoPoint(lat, lon));
                    }
                }
            }, 500);

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

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        //File basePath = Configuration.getInstance().getOsmdroidBasePath();
        //File cache = Configuration.getInstance().getOsmdroidTileCache();
        initMap();
        if (getActivity() != null) {
            mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
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
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mOverlayHelper != null) {
                            mOverlayHelper.clearGpx();
                            if (getActivity() != null)
                                ((AppCompatActivity) getActivity()).supportInvalidateOptionsMenu();
                        }
                        if (mListener != null) {
                            mListener.selectGpx();
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
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
            case R.id.action_gpx_zoom:
                disableFollow();
                zoomToBounds(Util.area(mListener.getGpx()));
                return true;
            case R.id.action_layers:
                View anchorView = getActivity().findViewById(R.id.popupAnchorView);
                PopupMenu popup = new PopupMenu(getActivity(), anchorView);
                android.view.MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_tile_sources, popup.getMenu());

                MenuItem openTopoItem = popup.getMenu().findItem(R.id.otm);
                openTopoItem.setChecked(true);

                popup.setOnMenuItemClickListener(MapFragment.this);
                popup.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
    }

    /**
     * Popup menu click
     * @param menuItem
     * @return
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
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
         * Set up navigation arrow
         */
        void setUpNavigation(boolean upNavigation);
    }
}

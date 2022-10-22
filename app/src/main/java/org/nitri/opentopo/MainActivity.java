package org.nitri.opentopo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.nitri.opentopo.model.GpxViewModel;
import org.nitri.opentopo.nearby.NearbyFragment;
import org.nitri.opentopo.nearby.entity.NearbyItem;
import org.osmdroid.util.GeoPoint;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.io.GeoUri;
import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;

public class MainActivity extends AppCompatActivity implements MapFragment.OnFragmentInteractionListener,
        GpxDetailFragment.OnFragmentInteractionListener, NearbyFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    protected static final String MAP_FRAGMENT_TAG = "map_fragment";
    protected static final String GPX_DETAIL_FRAGMENT_TAG = "gpx_detail_fragment";
    protected static final String WAY_POINT_DETAIL_FRAGMENT_TAG = "way_point_detail_fragment";
    protected static final String NEARBY_FRAGMENT_TAG = "nearby_fragment";

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int READ_REQUEST_CODE = 69;

    private static final String GPX_URI_STATE = "gpx_uri";
    private GeoPointDto mGeoPointFromIntent;
    private String mGpxUriString;
    private Uri mGpxUri;
    private boolean mZoomToGpx;
    private NearbyItem mSelectedNearbyPlace;
    private Fragment mMapFragment;
    private GpxViewModel mGpxViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            mGpxUriString = savedInstanceState.getString(GPX_URI_STATE);
        }

        mGpxViewModel = new ViewModelProvider(this).get(GpxViewModel.class);

        Intent intent = getIntent();

        if (intent != null && intent.getData() != null) {
            handleIntent(intent);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            addMapFragment();
        }

        if (savedInstanceState != null) {
            mMapFragment = getSupportFragmentManager().getFragment(savedInstanceState, MAP_FRAGMENT_TAG);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent.getData() != null) {
            String scheme = intent.getData().getScheme();
            if (scheme != null) {
                switch (scheme) {
                    case "geo":
                        mGeoPointFromIntent = getGeoPointDtoFromIntent(intent);
                        break;
                    case "file":
                    case "content":
                        mGpxUri = intent.getData();
                        mGpxUriString = mGpxUri.toString();
                        Log.i(TAG, "Uri: " + mGpxUriString);
                        mZoomToGpx = true;
                        break;
                }
            }
        }
    }


    private void addMapFragment() {
        if (mapFragmentAdded()) {
            return;
        }
        if (mGeoPointFromIntent == null) {
            mMapFragment = getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
            if (mMapFragment == null) {
                mMapFragment = MapFragment.newInstance();
            }
        } else {
            mMapFragment = MapFragment.newInstance(mGeoPointFromIntent.getLatitude(), mGeoPointFromIntent.getLongitude());
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map_container, mMapFragment, MAP_FRAGMENT_TAG)
                .commit();
        setGpx();
    }

    private boolean mapFragmentAdded() {
        return getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG) != null;
    }

    @Override
    public void addGpxDetailFragment() {
        GpxDetailFragment gpxDetailFragment = GpxDetailFragment.newInstance();
        getSupportFragmentManager().beginTransaction().addToBackStack("gpx")
                .replace(R.id.map_container, gpxDetailFragment, GPX_DETAIL_FRAGMENT_TAG)
                .commit();
    }

    @Override
    public void addNearbyFragment(GeoPoint nearbyCenterPoint) {
        NearbyFragment nearbyFragment = NearbyFragment.newInstance(nearbyCenterPoint.getLatitude(), nearbyCenterPoint.getLongitude());
        getSupportFragmentManager().beginTransaction().addToBackStack("nearby")
                .replace(R.id.map_container, nearbyFragment, NEARBY_FRAGMENT_TAG)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addMapFragment();
            } else {
                finish();
            }
        }
    }

    @Override
    public void selectGpx() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "GPX"), READ_REQUEST_CODE);
    }

    @Override
    public void setGpx() {
        if (!TextUtils.isEmpty(mGpxUriString)) {
            parseGpx(Uri.parse(mGpxUriString));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            if (resultData != null) {
                mGpxUri = resultData.getData();
                mZoomToGpx = true;
                if (mGpxUri != null) {
                    Log.i(TAG, "Uri: " + mGpxUri.toString());
                    parseGpx(mGpxUri);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (!TextUtils.isEmpty(mGpxUriString)) {
            outState.putString(GPX_URI_STATE, mGpxUriString);
        }
        getSupportFragmentManager().putFragment(outState, MAP_FRAGMENT_TAG, mMapFragment);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setUpNavigation(boolean upNavigation) {
        if (getSupportActionBar() != null) {
            if (upNavigation) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setDisplayShowHomeEnabled(false);
            }
        }
    }

    private void parseGpx(Uri uri) {
        GPXParser parser = new GPXParser();
        ContentResolver contentResolver = getContentResolver();
        if (contentResolver != null) {
            try {
                InputStream inputStream = contentResolver.openInputStream(uri);
                if (inputStream != null) {
                    mGpxViewModel.gpx = parser.parse(inputStream);
                    MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
                    if (mapFragment != null && mGpxViewModel.gpx != null) {
                        mapFragment.setGpx(mGpxViewModel.gpx, mZoomToGpx);
                        mGpxUriString = uri.toString();
                        mZoomToGpx = false;
                    }
                }

            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
                Toast.makeText(this, getString(R.string.invalid_gpx) + ": " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private GeoPointDto getGeoPointDtoFromIntent(Intent intent) {
        final Uri uri = (intent != null) ? intent.getData() : null;
        String uriAsString = (uri != null) ? uri.toString() : null;
        GeoPointDto pointFromIntent = null;
        if (uriAsString != null) {
            GeoUri parser = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);
            pointFromIntent = parser.fromUri(uriAsString, new GeoPointDto());
        }
        return pointFromIntent;
    }

    @Override
    public Gpx getGpx() {
        return mGpxViewModel.gpx;
    }

    @Override
    public void showNearbyPlace(NearbyItem nearbyItem) {
        mSelectedNearbyPlace = nearbyItem;
        getSupportFragmentManager().popBackStack();
        addMapFragment();
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapFragment != null) {
            mapFragment.setNearbyPlace();
        }
    }

    @Override
    public NearbyItem getSelectedNearbyPlace() {
        return mSelectedNearbyPlace;
    }

    @Override
    public void clearSelectedNearbyPlace() {
        mSelectedNearbyPlace = null;
    }
}

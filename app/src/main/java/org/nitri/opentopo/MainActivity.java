package org.nitri.opentopo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import net.danlew.android.joda.JodaTimeAndroid;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.io.GeoUri;
import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;

public class MainActivity extends AppCompatActivity implements MapFragment.OnFragmentInteractionListener, GpxDetailFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String MAP_FRAGMENT_TAG = "map_fragment";

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int READ_REQUEST_CODE = 69;

    private static final String GPX_URI_STATE = "gpx_uri";
    private GeoPointDto mGeoPointFromIntent;
    private String mGpxUriString;
    private Uri mGpxUri;
    private Gpx mGpx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            mGpxUriString = savedInstanceState.getString(GPX_URI_STATE);
        }

        Intent intent = getIntent();

        if (intent != null && intent.getData() != null) {
            handleIntent(intent);
        }

        JodaTimeAndroid.init(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            addMapFragment();
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
                        mGpxUri = intent.getData();
                        mGpxUriString = mGpxUri.toString();
                        Log.i(TAG, "Uri: " + mGpxUriString);
                        break;
                }
            }
        }
    }

    private void addMapFragment() {
        if (mapFragmentAdded()) {
            return;
        }
        MapFragment mapFragment;
        if (mGeoPointFromIntent == null)
            mapFragment = MapFragment.newInstance();
        else
            mapFragment = MapFragment.newInstance(mGeoPointFromIntent.getLatitude(), mGeoPointFromIntent.getLongitude());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.map_container, mapFragment, MAP_FRAGMENT_TAG)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
        if (mGpxUri != null) {
            parseGpx(mGpxUri);
        }
    }

    private boolean mapFragmentAdded() {
        return getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG) != null;
    }

    @Override
    public void addGpxDetailFragment() {
        GpxDetailFragment gpxDetailFragment = GpxDetailFragment.newInstance();
        getSupportFragmentManager().beginTransaction().addToBackStack(null)
                .replace(R.id.map_container, gpxDetailFragment, MAP_FRAGMENT_TAG)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                    } else {
                        addMapFragment();
                    }
                }
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addMapFragment();
                } else {
                    finish();
                }
                break;
        }
    }

    @Override
    public void selectGpx() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
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

        if (requestCode == READ_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            if (resultData != null) {
                mGpxUri = resultData.getData();
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
    protected void onSaveInstanceState(Bundle outState) {
        if (!TextUtils.isEmpty(mGpxUriString)) {
            outState.putString(GPX_URI_STATE, mGpxUriString);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (getSupportFragmentManager() != null && getSupportFragmentManager().getBackStackEntryCount() == 0 && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
    }

    private void parseGpx(Uri uri) {
        GPXParser parser = new GPXParser();
        ContentResolver contentResolver = getContentResolver();
        if (contentResolver != null) {
            try {
                InputStream inputStream = contentResolver.openInputStream(uri);
                if (inputStream != null) {
                    mGpx = parser.parse(inputStream);
                    MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
                    if (mapFragment != null && mGpx != null) {
                        mapFragment.setGpx(mGpx);
                        mGpxUriString = uri.toString();
                    }
                }

            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
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
        return mGpx;
    }
}

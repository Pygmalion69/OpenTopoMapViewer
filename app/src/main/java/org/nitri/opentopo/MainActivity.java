package org.nitri.opentopo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;

public class MainActivity extends AppCompatActivity implements MapFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String MAP_FRAGMENT_TAG = "map_fragment";

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int READ_REQUEST_CODE = 69;
    private String mGpxUri;

    private static final String GPX_URI_STATE = "gpx_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            mGpxUri = savedInstanceState.getString(GPX_URI_STATE);
        }
    }

    private void addMapFragment() {
        if (mapFragmentAdded()) {
            return;
        }
        MapFragment mapFragment = MapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.map_container, mapFragment, MAP_FRAGMENT_TAG)
                .commit();
    }

    private boolean mapFragmentAdded() {
        return getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG) != null;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            addMapFragment();
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
        if (!TextUtils.isEmpty(mGpxUri)) {
            parseGpx(Uri.parse(mGpxUri));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                if (uri != null) {
                    Log.i(TAG, "Uri: " + uri.toString());
                    parseGpx(uri);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (!TextUtils.isEmpty(mGpxUri)) {
            outState.putString(GPX_URI_STATE, mGpxUri);
        }
        super.onSaveInstanceState(outState);
    }

    private void parseGpx(Uri uri) {
        GPXParser parser = new GPXParser();
        try {
            Gpx gpx = parser.parse(getContentResolver().openInputStream(uri));
            MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
            if (mapFragment != null && gpx != null) {
                mapFragment.setGpx(gpx);
                mGpxUri = uri.toString();
            }

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
}

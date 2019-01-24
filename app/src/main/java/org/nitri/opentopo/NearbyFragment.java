package org.nitri.opentopo;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.nitri.opentopo.nearby.api.NearbyDatabase;
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiApi;
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiResponse;
import org.nitri.opentopo.nearby.da.NearbyDao;
import org.nitri.opentopo.nearby.entity.NearbyItem;
import org.nitri.opentopo.nearby.repo.NearbyRepository;
import org.nitri.opentopo.nearby.viewmodel.NearbyViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class NearbyFragment extends Fragment {
    final static String PARAM_LATITUDE = "latitude";
    final static String PARAM_LONGITUDE = "longitude";

    private String mParam1;
    private String mParam2;

    private static final String TAG = NearbyFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private double mLatitude;
    private double mLongitude;

    private static final String URL = "https://en.wikipedia.org/";

    Gson gson = new GsonBuilder().setLenient().create();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    List<NearbyItem> mNearbyItems = new ArrayList<>();


    public NearbyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param lat latitute
     * @param lon longitude
     * @return A new instance of fragment NearbyFragment.
     */
    public static NearbyFragment newInstance(double lat, double lon) {
        NearbyFragment fragment = new NearbyFragment();
        Bundle arguments = new Bundle();
        arguments.putDouble(PARAM_LATITUDE, lat);
        arguments.putDouble(PARAM_LONGITUDE, lon);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        if (getArguments() != null) {
            mLatitude = getArguments().getDouble(PARAM_LATITUDE);
            mLongitude = getArguments().getDouble(PARAM_LONGITUDE);
        }

        NearbyViewModel nearbyViewModel = ViewModelProviders.of(this).get(NearbyViewModel.class);

        MediaWikiApi api = retrofit.create(MediaWikiApi.class);
        NearbyDao dao = NearbyDatabase.getDatabase(getActivity()).nearbyDao();
        NearbyRepository nearbyRepository = new NearbyRepository(dao, api, mLatitude, mLongitude);

        nearbyViewModel.setRepository(nearbyRepository);

        Observer<List<NearbyItem>> nearbyObserver = items -> {
            mNearbyItems.clear();
            if (items != null) {
                mNearbyItems.addAll(items);
            }
        };

        nearbyViewModel.getItems().observe(this, nearbyObserver);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mListener.setUpNavigation(true);
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

    public interface OnFragmentInteractionListener {

        void onPoiSelected(int id);

        /**
         * Set up navigation arrow
         */
        void setUpNavigation(boolean upNavigation);
    }
}

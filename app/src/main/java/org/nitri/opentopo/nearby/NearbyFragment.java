package org.nitri.opentopo.nearby;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.nitri.opentopo.BuildConfig;
import org.nitri.opentopo.R;
import org.nitri.opentopo.Util;
import org.nitri.opentopo.nearby.adapter.NearbyAdapter;
import org.nitri.opentopo.nearby.api.NearbyDatabase;
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiApi;
import org.nitri.opentopo.nearby.da.NearbyDao;
import org.nitri.opentopo.nearby.entity.NearbyItem;
import org.nitri.opentopo.nearby.repo.NearbyRepository;
import org.nitri.opentopo.nearby.viewmodel.NearbyViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class NearbyFragment extends Fragment implements NearbyAdapter.OnItemClickListener {
    private final static String PARAM_LATITUDE = "latitude";
    private final static String PARAM_LONGITUDE = "longitude";

    private static final String TAG = NearbyFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private double mLatitude;
    private double mLongitude;

    private final Gson gson = new GsonBuilder().setLenient().create();

    private final List<NearbyItem> mNearbyItems = new ArrayList<>();
    private NearbyAdapter mNearbyAdapter;


    public NearbyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param lat latitude
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
        if (getArguments() != null) {
            mLatitude = getArguments().getDouble(PARAM_LATITUDE);
            mLongitude = getArguments().getDouble(PARAM_LONGITUDE);
        }

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("User-Agent", getString(R.string.app_name) + " "
                            + BuildConfig.VERSION_NAME)
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });

        String wikiBaseUrl = requireContext().getString(R.string.wiki_base_url);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(wikiBaseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();

        mNearbyAdapter = new NearbyAdapter(mNearbyItems, this);
        mNearbyAdapter.setHasStableIds(true);

        NearbyViewModel nearbyViewModel = new ViewModelProvider(requireActivity()).get(NearbyViewModel.class);

        MediaWikiApi api = retrofit.create(MediaWikiApi.class);
        NearbyDao dao = NearbyDatabase.getDatabase(getActivity()).nearbyDao();
        NearbyRepository nearbyRepository = new NearbyRepository(dao, api, mLatitude, mLongitude);

        nearbyViewModel.setRepository(nearbyRepository);

        Observer<List<NearbyItem>> nearbyObserver = items -> {
            mNearbyItems.clear();
            if (items != null) {
                mNearbyItems.addAll(items);
                setDistance();
                Collections.sort(mNearbyItems);
                mNearbyAdapter.notifyDataSetChanged();
            }
        };

        nearbyViewModel.getItems().observe(this, nearbyObserver);

    }

    private void setDistance() {
        for (NearbyItem item : mNearbyItems) {
            int distance = (int) Math.round(Util.distance(mLatitude, mLongitude, item.getLat(), item.getLon()));
            item.setDistance(distance);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
        RecyclerView nearbyRecyclerView = rootView.findViewById(R.id.nearby_recycler_view);
        nearbyRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        if (nearbyRecyclerView.getItemAnimator() != null)
            nearbyRecyclerView.getItemAnimator().setChangeDuration(0);
        nearbyRecyclerView.setAdapter(mNearbyAdapter);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mListener.setUpNavigation(true);
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
    public void onItemClick(int index) {
        Uri uri = Uri.parse(mNearbyItems.get(index).getUrl());
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(browserIntent);
    }

    @Override
    public void onMapItemClick(int index) {
        mListener.showNearbyPlace(mNearbyItems.get(index));
    }

    public interface OnFragmentInteractionListener {

        /**
         * Show nearby item on map
         *
         * @param nearbyItem
         */
        void showNearbyPlace(NearbyItem nearbyItem);

        /**
         * Set up navigation arrow
         */
        void setUpNavigation(boolean upNavigation);
    }
}

package org.nitri.opentopo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;


public class NearbyFragment extends Fragment {
    final static String PARAM_LATITUDE = "latitude";
    final static String PARAM_LONGITUDE = "longitude";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private double mLatitude;
    private double mLongtitude;

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
            mLongtitude = getArguments().getDouble(PARAM_LONGITUDE);
            }
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

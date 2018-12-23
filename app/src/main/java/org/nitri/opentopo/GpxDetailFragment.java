package org.nitri.opentopo;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;

public class GpxDetailFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private Gpx mGpx;

    public GpxDetailFragment() {
        // Required empty public constructor
    }


    public static GpxDetailFragment newInstance() {
        GpxDetailFragment fragment = new GpxDetailFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGpx = mListener.getGpx();
        if (mGpx != null && mGpx.getTracks() != null) {
            for (Track track : mGpx.getTracks()) {
                buildTrackDistanceLine(track);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gpx_detail, container, false);
    }

    private void buildTrackDistanceLine(Track track) {
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
        Gpx getGpx();
    }
}

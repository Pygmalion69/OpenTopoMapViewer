package org.nitri.opentopo;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;

import org.nitri.opentopo.domain.DistancePoint;

import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

public class GpxDetailFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private Gpx mGpx;
    private List<DistancePoint> mTrackDistanceLine;
    private double mDistance;
    private boolean mElevation;
    private LineChart mElevationChart;
    private TextView tvName;
    private TextView tvDescription;
    private TextView tvLength;

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
        View rootView = inflater.inflate(R.layout.fragment_gpx_detail, container, false);
        tvName = rootView.findViewById(R.id.tvName);
        tvDescription = rootView.findViewById(R.id.tvDescription);
        tvLength = rootView.findViewById(R.id.tvLength);
        mElevationChart = rootView.findViewById(R.id.elevationChart);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void buildTrackDistanceLine(Track track) {
        mTrackDistanceLine = new ArrayList<>();
        mDistance = 0;
        mElevation = false;
        TrackPoint prevTrackPoint = null;
        if (track.getTrackSegments() != null) {
            for (TrackSegment segment: track.getTrackSegments()) {
                if (segment.getTrackPoints() != null) {
                    for (TrackPoint trackPoint: segment.getTrackPoints()) {
                        if (prevTrackPoint != null) {
                            DistancePoint.Builder builder = new DistancePoint.Builder();
                            mDistance += Util.distance(prevTrackPoint, trackPoint);
                            builder.setDistance(mDistance);
                            if (trackPoint.getElevation() != null) {
                                builder.setElevation(trackPoint.getElevation());
                                mElevation = true;
                            }
                            mTrackDistanceLine.add(builder.build());
                        }
                        prevTrackPoint = trackPoint;
                    }
                }
            }
        }
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

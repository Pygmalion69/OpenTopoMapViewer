package org.nitri.opentopo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.nitri.opentopo.adapter.WayPointListAdapter;
import org.nitri.opentopo.domain.DistancePoint;
import org.nitri.opentopo.model.GpxViewModel;
import org.nitri.opentopo.model.WayPointHeaderItem;
import org.nitri.opentopo.model.WayPointItem;
import org.nitri.opentopo.model.WayPointListItem;
import org.nitri.opentopo.view.ChartValueMarkerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;
import io.ticofab.androidgpxparser.parser.domain.WayPoint;

public class GpxDetailFragment extends Fragment implements WayPointListAdapter.OnItemClickListener, WayPointDetailDialogFragment.Callback {

    private OnFragmentInteractionListener mListener;
    private List<DistancePoint> mTrackDistanceLine;
    private double mDistance;
    private boolean mElevation;
    private LineChart mElevationChart;
    private TextView tvName;
    private TextView tvDescription;
    private TextView tvLength;
    private Typeface mTfRegular;
    private Typeface mTfLight;

    private double mMinElevation = 0;
    private double mMaxElevation = 0;
    List<WayPointListItem> mWayPointListItems = new ArrayList<>();
    private WayPointListAdapter mWayPointListAdapter;
    private WebView wvDescription;
    private int mSelectedIndex;
    private GpxViewModel mGpxViewModel;
    private ConstraintLayout chartContainer;


    public GpxDetailFragment() {
        // Required empty public constructor
    }


    public static GpxDetailFragment newInstance() {
        return new GpxDetailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mWayPointListAdapter = new WayPointListAdapter(mWayPointListItems, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gpx_detail, container, false);
        tvName = rootView.findViewById(R.id.tvTitle);
        tvDescription = rootView.findViewById(R.id.tvDescription);
        wvDescription = rootView.findViewById(R.id.wvDescription);
        wvDescription.setBackgroundColor(Color.TRANSPARENT);
        tvLength = rootView.findViewById(R.id.tvLength);
        chartContainer = rootView.findViewById(R.id.chartContainer);
        mElevationChart = rootView.findViewById(R.id.elevationChart);
        RecyclerView wayPointRecyclerView = rootView.findViewById(R.id.way_point_recycler_view);
        wayPointRecyclerView.setNestedScrollingEnabled(false);
        wayPointRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        wayPointRecyclerView.setAdapter(mWayPointListAdapter);


        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mGpxViewModel = new ViewModelProvider(requireActivity()).get(GpxViewModel.class);

        if (mGpxViewModel.gpx != null && mGpxViewModel.gpx.getTracks() != null) {
            for (Track track : mGpxViewModel.gpx.getTracks()) {
                buildTrackDistanceLine(track);
            }
           /* if (getActivity() != null) {
                mTfRegular = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Regular.ttf");
                mTfLight = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Light.ttf");
            }*/
        }

        // For now, use title and description of first track
        if (mGpxViewModel.gpx != null && mGpxViewModel.gpx.getTracks() != null && mGpxViewModel.gpx.getTracks().get(0) != null) {
            if (TextUtils.isEmpty(mGpxViewModel.gpx.getTracks().get(0).getTrackName())) {
                tvName.setVisibility(View.GONE);
            } else {
                tvName.setText(mGpxViewModel.gpx.getTracks().get(0).getTrackName());
            }
            String description = mGpxViewModel.gpx.getTracks().get(0).getTrackDesc();
            if (TextUtils.isEmpty(description)) {
                tvDescription.setVisibility(View.GONE);
                wvDescription.setVisibility(View.GONE);
            } else {
                if (description.matches(".*<\\s*img\\s.*>.*")) {
                    tvDescription.setVisibility(View.GONE);
                    wvDescription.setVisibility(View.VISIBLE);
                    wvDescription.loadData(description, "text/html; charset=utf-8", "UTF-8");
                } else {
                    tvDescription.setVisibility(View.VISIBLE);
                    wvDescription.setVisibility(View.GONE);
                    tvDescription.setText(Util.fromHtml(description));
                    tvDescription.setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        }

        if (mElevation) {
            setUpElevationChart();
            setChartData();
        } else {
            chartContainer.setVisibility(View.GONE);
        }

        if (mDistance > 0) {
            tvLength.setText(String.format(Locale.getDefault(), "%.2f km", mDistance / 1000f));
        } else {
            tvLength.setVisibility(View.GONE);
        }

        if (mGpxViewModel.gpx != null && mGpxViewModel.gpx.getWayPoints() != null) {
            buildWayPointList();
            mWayPointListAdapter.notifyDataSetChanged();
        }
    }

    private void buildTrackDistanceLine(Track track) {
        mTrackDistanceLine = new ArrayList<>();
        mDistance = 0;
        mElevation = false;
        TrackPoint prevTrackPoint = null;
        if (track.getTrackSegments() != null) {
            for (TrackSegment segment : track.getTrackSegments()) {
                if (segment.getTrackPoints() != null) {
                    mMinElevation = mMaxElevation = segment.getTrackPoints().get(0).getElevation();
                    for (TrackPoint trackPoint : segment.getTrackPoints()) {
                        if (prevTrackPoint != null) {
                            mDistance += Util.distance(prevTrackPoint, trackPoint);
                        }

                        DistancePoint.Builder builder = new DistancePoint.Builder();
                        builder.setDistance(mDistance);
                        if (trackPoint.getElevation() != null) {
                            double elevation = trackPoint.getElevation();
                            if (elevation < mMinElevation)
                                mMinElevation = elevation;
                            if (elevation > mMaxElevation)
                                mMaxElevation = elevation;
                            builder.setElevation(elevation);
                            mElevation = true;

                            mTrackDistanceLine.add(builder.build());
                        }
                        prevTrackPoint = trackPoint;
                    }
                }
            }
        }
    }

    private void buildWayPointList() {
        String defaultType = getString(R.string.poi);
        List<WayPoint> wayPoints;
        mWayPointListItems.clear();
        for (String type : Util.getWayPointTypes(mGpxViewModel.gpx, defaultType)) {
            wayPoints = Util.getWayPointsByType(mGpxViewModel.gpx, type);
            if (type.equals(defaultType))
                wayPoints.addAll(Util.getWayPointsByType(mGpxViewModel.gpx, null));
            if (wayPoints.size() > 0) {
                mWayPointListItems.add(new WayPointHeaderItem(type));
                for (WayPoint wayPoint : wayPoints) {
                    mWayPointListItems.add(new WayPointItem(wayPoint));
                }
            }
        }
    }

    private void setUpElevationChart() {
        Legend l = mElevationChart.getLegend();
        l.setEnabled(false);
        mElevationChart.getDescription().setEnabled(false);

        ChartValueMarkerView mv = new ChartValueMarkerView(getActivity(), R.layout.chart_value_marker_view);
        mv.setChartView(mElevationChart);
        mElevationChart.setMarker(mv);

        int primaryTextColorInt = Util.resolveColorAttr(getActivity(), android.R.attr.textColorPrimary);

        XAxis xAxis = mElevationChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(primaryTextColorInt);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter((value, axis) -> String.format(Locale.getDefault(), "%.1f", value / 1000));

        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum((float) mDistance);

        YAxis yAxis = mElevationChart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setTypeface(mTfLight);
        yAxis.setDrawGridLines(false);
        yAxis.setGranularityEnabled(true);
        yAxis.setTextColor(primaryTextColorInt);

        float margin = (float) mMaxElevation * .2f;
        float yMin = (float) mMinElevation - margin;
        float yMax = (float) mMaxElevation + margin;
        if (yMin < 0 && mMinElevation >= 0)
            yMin = 0;

        yAxis.setAxisMinimum(yMin);
        yAxis.setAxisMaximum(yMax);

        mElevationChart.getAxisRight().setDrawLabels(false);

        mElevationChart.getViewPortHandler().setMaximumScaleX(2f);
        mElevationChart.getViewPortHandler().setMaximumScaleY(2f);
    }

    private void setChartData() {
        ArrayList<Entry> elevationValues = new ArrayList<>();
        for (DistancePoint point : mTrackDistanceLine) {
            if (point.getElevation() != null)
                elevationValues.add(new Entry(point.getDistance().floatValue(), point.getElevation().floatValue()));
        }
        LineDataSet elevationDataSet = new LineDataSet(elevationValues, getString(R.string.elevation));
        elevationDataSet.setDrawValues(false);
        elevationDataSet.setLineWidth(2f);
        elevationDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        elevationDataSet.setColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
        elevationDataSet.setDrawCircles(false);
        elevationDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        LineData elevationData = new LineData(elevationDataSet);
        mElevationChart.setData(elevationData);
        mElevationChart.invalidate();
    }

    @Override
    public void onItemClick(int index) {
        mSelectedIndex = index;
        if (getActivity() != null && !getActivity().isFinishing()) {
            WayPointDetailDialogFragment wayPointDetailDialogFragment = new WayPointDetailDialogFragment();
            wayPointDetailDialogFragment.show(getActivity().getSupportFragmentManager(), MainActivity.WAY_POINT_DETAIL_FRAGMENT_TAG);
        }
    }

    @Override
    public WayPointItem getSelectedWayPointItem() {
        return (WayPointItem) mWayPointListItems.get(mSelectedIndex);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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

    public interface OnFragmentInteractionListener {

        /**
         * Retrieve the current GPX
         *
         * @return Gpx
         */
        Gpx getGpx();

        /**
         * Set up navigation arrow
         */
        void setUpNavigation(boolean upNavigation);
    }
}

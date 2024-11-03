package org.nitri.opentopo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.Track
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import io.ticofab.androidgpxparser.parser.domain.WayPoint
import org.nitri.opentopo.adapter.WayPointListAdapter
import org.nitri.opentopo.domain.DistancePoint
import org.nitri.opentopo.model.GpxViewModel
import org.nitri.opentopo.model.WayPointHeaderItem
import org.nitri.opentopo.model.WayPointItem
import org.nitri.opentopo.model.WayPointListItem
import org.nitri.opentopo.util.DistanceCalculator
import org.nitri.opentopo.util.Util
import org.nitri.opentopo.view.ChartValueMarkerView
import java.util.Locale


class GpxDetailFragment : Fragment(), WayPointListAdapter.OnItemClickListener,
    WayPointDetailDialogFragment.Callback {
    private lateinit var mListener: OnFragmentInteractionListener
    private lateinit var mTrackDistanceLine: MutableList<DistancePoint>
    private var mDistance = 0.0
    private var mElevation = false
    private lateinit var mElevationChart: LineChart
    private lateinit var tvName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLength: TextView
    private val mTfRegular: Typeface? = null
    private val mTfLight: Typeface? = null
    private var mMinElevation = 0.0
    private var mMaxElevation = 0.0
    private var mWayPointListItems: MutableList<WayPointListItem> = ArrayList()
    private lateinit var mWayPointListAdapter: WayPointListAdapter
    private lateinit var wvDescription: WebView
    private var mSelectedIndex = 0
    private lateinit var mGpxViewModel: GpxViewModel
    private lateinit var chartContainer: ConstraintLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mWayPointListAdapter = WayPointListAdapter(mWayPointListItems, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_gpx_detail, container, false)
        tvName = rootView.findViewById(R.id.tvTitle)
        tvDescription = rootView.findViewById(R.id.tvDescription)
        wvDescription = rootView.findViewById(R.id.wvDescription)
        wvDescription.setBackgroundColor(Color.TRANSPARENT)
        tvLength = rootView.findViewById(R.id.tvLength)
        chartContainer = rootView.findViewById(R.id.chartContainer)
        mElevationChart = rootView.findViewById(R.id.elevationChart)
        val wayPointRecyclerView = rootView.findViewById<RecyclerView>(R.id.way_point_recycler_view)
        wayPointRecyclerView.isNestedScrollingEnabled = false
        wayPointRecyclerView.layoutManager = LinearLayoutManager(activity)
        wayPointRecyclerView.adapter = mWayPointListAdapter
        return rootView
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mGpxViewModel = ViewModelProvider(requireActivity())[GpxViewModel::class.java]
        mGpxViewModel.gpx?.tracks?.forEach {
            buildTrackDistanceLine(it)
        }
        /* if (getActivity() != null) {
            mTfRegular = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Regular.ttf");
            mTfLight = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Light.ttf");
        }*/

        // For now, use title and description of first track
        mGpxViewModel.gpx?.tracks?.firstOrNull()?.let { firstTrack ->
            if (TextUtils.isEmpty(firstTrack.trackName)) {
                tvName.visibility = View.GONE
            } else {
                tvName.text = firstTrack.trackName
                tvName.visibility = View.VISIBLE
            }

            val description = firstTrack.trackDesc
            if (TextUtils.isEmpty(description)) {
                tvDescription.visibility = View.GONE
                wvDescription.visibility = View.GONE
            } else {
                when {
                    description.matches(".*<\\s*img\\s.*>.*".toRegex()) -> {
                        tvDescription.visibility = View.GONE
                        wvDescription.visibility = View.VISIBLE
                        wvDescription.loadData(description, "text/html; charset=utf-8", "UTF-8")
                    }
                    else -> {
                        tvDescription.visibility = View.VISIBLE
                        wvDescription.visibility = View.GONE
                        tvDescription.text = Util.fromHtml(description)
                        tvDescription.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        }
        if (mElevation) {
            setUpElevationChart()
            setChartData()
        } else {
            chartContainer.visibility = View.GONE
        }
        if (mDistance > 0) {
            tvLength.text = String.format(Locale.getDefault(), "%.2f km", mDistance / 1000f)
        } else {
            tvLength.visibility = View.GONE
        }
        mGpxViewModel.gpx?.wayPoints?.let {
            buildWayPointList()
            mWayPointListAdapter.notifyDataSetChanged()
        }
    }

    private fun buildTrackDistanceLine(track: Track) {
        mTrackDistanceLine = ArrayList()
        mDistance = 0.0
        mElevation = false
        var prevTrackPoint: TrackPoint? = null
        track.trackSegments?.forEach { segment ->
            segment.trackPoints?.let { points ->
                mMaxElevation = points.first().elevation ?: 0.0
                mMinElevation = mMaxElevation
                points.forEach { trackPoint ->
                    prevTrackPoint?.let { prevPoint ->
                        mDistance += DistanceCalculator.distance(prevPoint, trackPoint)
                    }
                    val builder = DistancePoint.Builder()
                    builder.setDistance(mDistance)
                    trackPoint.elevation?.also { elevation ->
                        if (elevation < mMinElevation) mMinElevation = elevation
                        if (elevation > mMaxElevation) mMaxElevation = elevation
                        builder.setElevation(elevation)
                        mElevation = true
                        mTrackDistanceLine.add(builder.build())
                    }
                    prevTrackPoint = trackPoint
                }
            }
        }

    }

    private fun buildWayPointList() {
        val defaultType = getString(R.string.poi)
        var wayPoints: MutableList<WayPoint?>
        mWayPointListItems.clear()

        var waypointTypes: List<String>?
        mGpxViewModel.gpx?.let { gpx ->
            waypointTypes = Util.getWayPointTypes(gpx, defaultType)
            waypointTypes?.forEach { type ->
                wayPoints = Util.getWayPointsByType(gpx, type).toMutableList()
                if (type == defaultType) wayPoints.addAll(
                    Util.getWayPointsByType(
                        gpx, null
                    )
                )
                if (wayPoints.isNotEmpty()) {
                    mWayPointListItems.add(WayPointHeaderItem(type))
                    for (wayPoint in wayPoints) {
                        wayPoint?.let { WayPointItem(it)}
                            ?.let { mWayPointListItems.add(it) }
                    }
                }
            }
        }
    }

    private fun setUpElevationChart() {
        val l = mElevationChart.legend
        l.isEnabled = false
        mElevationChart.description.isEnabled = false
        val mv = ChartValueMarkerView(activity, R.layout.chart_value_marker_view)
        mv.chartView = mElevationChart
        mElevationChart.marker = mv
        val primaryTextColorInt = context?.let { Util.resolveColorAttr(it, android.R.attr.textColorPrimary) }
        val xAxis = mElevationChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.typeface = mTfLight
        xAxis.textSize = 10f
        xAxis.textColor = Color.WHITE
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        if (primaryTextColorInt != null) {
            xAxis.textColor = primaryTextColorInt
        }
        xAxis.granularity = 1f
        xAxis.valueFormatter = IAxisValueFormatter { value: Float, axis: AxisBase? ->
            String.format(
                Locale.getDefault(), "%.1f", value / 1000
            )
        }
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = mDistance.toFloat()
        val yAxis = mElevationChart.axisLeft
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        yAxis.typeface = mTfLight
        yAxis.setDrawGridLines(false)
        yAxis.isGranularityEnabled = true
        if (primaryTextColorInt != null) {
            yAxis.textColor = primaryTextColorInt
        }
        val margin = mMaxElevation.toFloat() * .2f
        var yMin = mMinElevation.toFloat() - margin
        val yMax = mMaxElevation.toFloat() + margin
        if (yMin < 0 && mMinElevation >= 0) yMin = 0f
        yAxis.axisMinimum = yMin
        yAxis.axisMaximum = yMax
        mElevationChart.axisRight.setDrawLabels(false)
        mElevationChart.viewPortHandler.setMaximumScaleX(2f)
        mElevationChart.viewPortHandler.setMaximumScaleY(2f)
    }

    private fun setChartData() {
        val elevationValues = ArrayList<Entry>()
        for (point in mTrackDistanceLine) {
            if (point.distance != null && point.elevation != null) elevationValues.add(
                Entry(
                    point.distance.toFloat(),
                    point.elevation.toFloat()
                )
            )
        }
        val elevationDataSet = LineDataSet(elevationValues, getString(R.string.elevation))
        elevationDataSet.setDrawValues(false)
        elevationDataSet.lineWidth = 2f
        elevationDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        elevationDataSet.color = ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
        elevationDataSet.setDrawCircles(false)
        elevationDataSet.axisDependency = YAxis.AxisDependency.LEFT
        val elevationData = LineData(elevationDataSet)
        mElevationChart.data = elevationData
        mElevationChart.invalidate()
    }

    override fun onItemClick(index: Int) {
        mSelectedIndex = index
        if (activity != null && !requireActivity().isFinishing) {
            val wayPointDetailDialogFragment = WayPointDetailDialogFragment()
            wayPointDetailDialogFragment.show(
                requireActivity().supportFragmentManager,
                BaseMainActivity.WAY_POINT_DETAIL_FRAGMENT_TAG
            )
        }
    }

    override fun getSelectedWayPointItem(): WayPointItem {
        return mWayPointListItems[mSelectedIndex] as WayPointItem
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        mListener.setUpNavigation(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(
                context.toString()
                        + " must implement OnFragmentInteractionListener"
            )
        }
    }

    interface OnFragmentInteractionListener {
        /**
         * Retrieve the current GPX
         *
         * @return Gpx
         */
        fun getGpx(): Gpx?

        /**
         * Set up navigation arrow
         */
        fun setUpNavigation(upNavigation: Boolean)
    }

    companion object {
        fun newInstance(): GpxDetailFragment {
            return GpxDetailFragment()
        }
    }
}
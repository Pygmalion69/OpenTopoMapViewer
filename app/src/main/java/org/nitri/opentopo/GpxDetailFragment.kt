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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.Track
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import io.ticofab.androidgpxparser.parser.domain.WayPoint
import org.nitri.opentopo.adapter.WayPointListAdapter
import org.nitri.opentopo.domain.DistancePoint
import org.nitri.opentopo.viewmodel.GpxViewModel
import org.nitri.opentopo.model.WayPointHeaderItem
import org.nitri.opentopo.model.WayPointItem
import org.nitri.opentopo.model.WayPointListItem
import org.nitri.opentopo.util.DistanceCalculator
import org.nitri.opentopo.util.Utils
import org.nitri.opentopo.view.ChartValueMarkerView
import java.util.Locale


class GpxDetailFragment : Fragment(), WayPointListAdapter.OnItemClickListener,
    WayPointDetailDialogFragment.Callback {
    private lateinit var fragmentInteractionListener: OnFragmentInteractionListener
    private lateinit var trackDistancePoints: MutableList<DistancePoint>
    private var totalDistance = 0.0
    private var hasElevationData = false
    private lateinit var elevationChart: LineChart
    private lateinit var tvName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLength: TextView
    private val regularTypeface: Typeface? = null
    private val lightTypeface: Typeface? = null
    private var minElevation = 0.0
    private var maxElevation = 0.0
    private var wayPointListItems: MutableList<WayPointListItem> = ArrayList()
    private lateinit var mWayPointListAdapter: WayPointListAdapter
    private lateinit var wvDescription: WebView
    private var mSelectedIndex = 0
    private lateinit var mGpxViewModel: GpxViewModel
    private lateinit var chartContainer: ConstraintLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mWayPointListAdapter = WayPointListAdapter(wayPointListItems, this)
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
        elevationChart = rootView.findViewById(R.id.elevationChart)
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
            regularTypeface = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Regular.ttf");
            lightTypeface = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Light.ttf");
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
                        tvDescription.text = Utils.fromHtml(description)
                        tvDescription.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        }
        if (hasElevationData) {
            setUpElevationChart()
            setChartData()
        } else {
            chartContainer.visibility = View.GONE
        }
        if (totalDistance > 0) {
            tvLength.text = String.format(Locale.getDefault(), "%.2f km", totalDistance / 1000f)
        } else {
            tvLength.visibility = View.GONE
        }
        mGpxViewModel.gpx?.wayPoints?.let {
            buildWayPointList()
            mWayPointListAdapter.notifyDataSetChanged()
        }
    }

    private fun buildTrackDistanceLine(track: Track) {
        trackDistancePoints = ArrayList()
        totalDistance = 0.0
        hasElevationData = false
        var prevTrackPoint: TrackPoint? = null

        minElevation = Double.MAX_VALUE
        maxElevation = Double.MIN_VALUE

        track.trackSegments?.forEach { segment ->
            if (trackDistancePoints.isNotEmpty()) {
                trackDistancePoints.add(DistancePoint.Builder().build())
            }
            prevTrackPoint = null
            segment.trackPoints?.forEach { trackPoint ->
                prevTrackPoint?.let { prevPoint ->
                    totalDistance += DistanceCalculator.distance(prevPoint, trackPoint)
                }

                val builder = DistancePoint.Builder()
                    .setDistance(totalDistance)

                trackPoint.elevation?.also { elevation ->
                    if (elevation < minElevation) minElevation = elevation
                    if (elevation > maxElevation) maxElevation = elevation
                    builder.setElevation(elevation)
                    hasElevationData = true
                }

                trackDistancePoints.add(builder.build())
                prevTrackPoint = trackPoint
            }
        }

        if (!hasElevationData) {
            minElevation = 0.0
            maxElevation = 0.0
        }
    }


    private fun buildWayPointList() {
        val defaultType = getString(R.string.poi)
        var wayPoints: MutableList<WayPoint?>
        wayPointListItems.clear()

        var waypointTypes: List<String>?
        mGpxViewModel.gpx?.let { gpx ->
            waypointTypes = Utils.getWayPointTypes(gpx, defaultType)
            waypointTypes?.forEach { type ->
                wayPoints = Utils.getWayPointsByType(gpx, type).toMutableList()
                if (type == defaultType) wayPoints.addAll(
                    Utils.getWayPointsByType(
                        gpx, null
                    )
                )
                if (wayPoints.isNotEmpty()) {
                    wayPointListItems.add(WayPointHeaderItem(type))
                    for (wayPoint in wayPoints) {
                        wayPoint?.let { WayPointItem(it)}
                            ?.let { wayPointListItems.add(it) }
                    }
                }
            }
        }
    }

    private fun setUpElevationChart() {
        val l = elevationChart.legend
        l.isEnabled = false
        elevationChart.description.isEnabled = false
        val mv = ChartValueMarkerView(activity, R.layout.chart_value_marker_view)
        mv.chartView = elevationChart
        elevationChart.marker = mv
        val primaryTextColorInt = context?.let { Utils.resolveColorAttr(it, android.R.attr.textColorPrimary) }
        val xAxis = elevationChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.typeface = lightTypeface
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
        xAxis.axisMaximum = totalDistance.toFloat()
        val yAxis = elevationChart.axisLeft
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        yAxis.typeface = lightTypeface
        yAxis.setDrawGridLines(false)
        yAxis.isGranularityEnabled = true
        if (primaryTextColorInt != null) {
            yAxis.textColor = primaryTextColorInt
        }
        val margin = maxElevation.toFloat() * .2f
        var yMin = minElevation.toFloat() - margin
        val yMax = maxElevation.toFloat() + margin
        if (yMin < 0 && minElevation >= 0) yMin = 0f
        yAxis.axisMinimum = yMin
        yAxis.axisMaximum = yMax
        elevationChart.axisRight.setDrawLabels(false)
        elevationChart.viewPortHandler.setMaximumScaleX(2f)
        elevationChart.viewPortHandler.setMaximumScaleY(2f)
    }

    private fun setChartData() {
        val elevationDataSets = ArrayList<ILineDataSet>()
        var elevationValues = ArrayList<Entry>()
        for (point in trackDistancePoints) {
            val distance = point.distance
            val elevation = point.elevation
            if (distance != null && elevation != null) {
                elevationValues.add(Entry(distance.toFloat(), elevation.toFloat()))
            } else if (elevationValues.isNotEmpty()) {
                elevationDataSets.add(createElevationDataSet(elevationValues))
                elevationValues = ArrayList()
            }
        }
        if (elevationValues.isNotEmpty()) {
            elevationDataSets.add(createElevationDataSet(elevationValues))
        }
        val elevationData = LineData(elevationDataSets)
        elevationChart.data = elevationData
        elevationChart.invalidate()
    }

    private fun createElevationDataSet(entries: List<Entry>): LineDataSet {
        val elevationDataSet = LineDataSet(entries, getString(R.string.elevation))
        elevationDataSet.setDrawValues(false)
        elevationDataSet.lineWidth = 2f
        elevationDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        elevationDataSet.color = ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
        elevationDataSet.setDrawCircles(false)
        elevationDataSet.axisDependency = YAxis.AxisDependency.LEFT
        return elevationDataSet
    }

    override fun onItemClick(index: Int) {
        mSelectedIndex = index
        activity?.takeIf { !it.isFinishing }?.let { fragmentActivity ->
            val wayPointDetailDialogFragment = WayPointDetailDialogFragment()
            wayPointDetailDialogFragment.show(
                fragmentActivity.supportFragmentManager,
                BaseMainActivity.WAY_POINT_DETAIL_FRAGMENT_TAG
            )
        }
    }

    override fun getSelectedWayPointItem(): WayPointItem {
        return wayPointListItems[mSelectedIndex] as WayPointItem
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        fragmentInteractionListener.setUpNavigation(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentInteractionListener = if (context is OnFragmentInteractionListener) {
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
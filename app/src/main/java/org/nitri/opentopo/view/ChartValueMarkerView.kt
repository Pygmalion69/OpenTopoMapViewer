package org.nitri.opentopo.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import org.nitri.opentopo.R

/**
 * Custom implementation of the MarkerView.
 *
 */
class ChartValueMarkerView(context: Context?, layoutResource: Int) :
    MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)

    @SuppressLint("DefaultLocale")
    override fun refreshContent(e: Entry, highlight: Highlight) {
        tvContent.text = String.format("%.1f", e.y)
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

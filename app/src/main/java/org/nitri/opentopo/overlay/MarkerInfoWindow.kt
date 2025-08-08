package org.nitri.opentopo.overlay

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import org.nitri.opentopo.R
import org.nitri.opentopo.util.Utils.fromHtml
import org.nitri.opentopo.model.MarkerModel
import org.osmdroid.api.IMapView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow

class MarkerInfoWindow(
    layoutResId: Int,
    private val titleResourceId: Int,
    private val descriptionResourceId: Int,
    private val subDescriptionResourceId: Int,
    private val subDescription: String?,
    mapView: MapView?,
    private val orsEnabled: Boolean = false
) : BasicInfoWindow(layoutResId, mapView) {

    var onMarkerInfoEditClickListener: OnMarkerInfoEditClickListener? = null
    var onMarkerWaypointClickListener: OnMarkerWaypointClickListener? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onOpen(item: Any) {
        val markerModel = item as MarkerModel
        val title = markerModel.name
        if (mView == null) {
            Log.w(IMapView.LOGTAG, "Error trapped, MarkerInfoWindow.open, mView is null!")
            return
        }

        val tvTitle = mView.findViewById<TextView>(titleResourceId)
        if (tvTitle != null) tvTitle.text = title

        val snippet = markerModel.description
        val snippetHtml = fromHtml(snippet.replace("href=\"//", "href=\"http://"))
        val snippetText = mView.findViewById<TextView>(descriptionResourceId)
        snippetText.text = snippetHtml
        snippetText.setOnTouchListener { v, event ->
            var ret = false
            val text = (v as TextView).text
            val sText = Spannable.Factory.getInstance().newSpannable(text)
            val action = event.action
            if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN
            ) {
                var x = event.x.toInt()
                var y = event.y.toInt()
                x -= v.totalPaddingLeft
                y -= v.totalPaddingTop
                x += v.scrollX
                y += v.scrollY
                val layout = v.layout
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                val link = sText.getSpans(off, off, ClickableSpan::class.java)
                if (link.isNotEmpty()) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(v)
                    }
                    ret = true
                }
            }
            ret
        }
        val subDescText = mView.findViewById<TextView>(subDescriptionResourceId)
        if (subDescription != null && "" != subDescription) {
            subDescText.text = fromHtml(subDescription)
            subDescText.visibility = View.VISIBLE
            subDescText.movementMethod = LinkMovementMethod.getInstance()
        } else {
            subDescText.visibility = View.GONE
        }

        val btnInfoEdit : ImageButton = mView.findViewById(R.id.bubble_edit)
        btnInfoEdit.setOnClickListener {
            onMarkerInfoEditClickListener?.onMarkerInfoEditClick(
                markerModel
            )
            close()
        }

        val btnWaypoint : ImageButton = mView.findViewById(R.id.bubble_waypoint)

        if (orsEnabled) {
            btnWaypoint.visibility = View.VISIBLE
            // Set the appropriate icon based on whether it's a waypoint or not
            btnWaypoint.setImageResource(
                if (markerModel.routeWaypoint) R.drawable.ic_action_remove_waypoint
                else R.drawable.ic_action_add_waypoint
            )

            btnWaypoint.setOnClickListener {
                if (markerModel.routeWaypoint) {
                    onMarkerWaypointClickListener?.onMarkerWaypointRemoveClick(markerModel)
                } else {
                    onMarkerWaypointClickListener?.onMarkerWaypointAddClick(markerModel)
                }
                close()
            }
        } else {
            btnWaypoint.visibility = View.GONE
        }
    }

    interface OnMarkerInfoEditClickListener {
        fun onMarkerInfoEditClick(markerModel: MarkerModel)
    }

    interface OnMarkerWaypointClickListener {
        fun onMarkerWaypointAddClick(markerModel: MarkerModel)
        fun onMarkerWaypointRemoveClick(markerModel: MarkerModel)
    }
}

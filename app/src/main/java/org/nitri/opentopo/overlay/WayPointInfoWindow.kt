package org.nitri.opentopo.overlay

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import org.nitri.opentopo.Util.fromHtml
import org.osmdroid.api.IMapView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow

class WayPointInfoWindow(
    layoutResId: Int,
    private val mTitleId: Int,
    private val mDescriptionId: Int,
    private val mSubDescriptionId: Int,
    private val mSubDescription: String?,
    mapView: MapView?
) : BasicInfoWindow(layoutResId, mapView) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onOpen(item: Any) {
        val overlayItem = item as OverlayItem
        var title = overlayItem.title
        if (title == null) title = ""
        if (mView == null) {
            Log.w(IMapView.LOGTAG, "Error trapped, BasicInfoWindow.open, mView is null!")
            return
        }
        val temp = mView.findViewById<TextView>(mTitleId)
        if (temp != null) temp.text = title
        var snippet = overlayItem.snippet
        if (snippet == null) snippet = ""
        val snippetHtml = fromHtml(snippet.replace("href=\"//", "href=\"http://"))
        val snippetText = mView.findViewById<TextView>(mDescriptionId)
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
        val subDescText = mView.findViewById<TextView>(mSubDescriptionId)
        if (mSubDescription != null && "" != mSubDescription) {
            subDescText.text = fromHtml(mSubDescription)
            subDescText.visibility = View.VISIBLE
            subDescText.movementMethod = LinkMovementMethod.getInstance()
        } else {
            subDescText.visibility = View.GONE
        }
    }
}

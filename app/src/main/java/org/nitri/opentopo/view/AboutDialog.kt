package org.nitri.opentopo.view

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.nitri.opentopo.R
import org.nitri.opentopo.util.Utils

object AboutDialog {

    fun show(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_about, null)

        val versionTextView = dialogView.findViewById<TextView>(R.id.appVersion)
        versionTextView.text = context.getString(R.string.app_version, Utils.getAppVersion(context))

        val authorTextView = dialogView.findViewById<TextView>(R.id.authorName)
        authorTextView.setHtmlText(context.getString(R.string.app_author))

        val productPageTextView = dialogView.findViewById<TextView>(R.id.productPage)
        productPageTextView.setHtmlText(context.getString(R.string.app_product_page))

        val openTopoMapInfoTextView = dialogView.findViewById<TextView>(R.id.openTopoMapInfo)
        openTopoMapInfoTextView.setHtmlText(context.getString(R.string.about_open_topo_map))

        val openStreetMapInfoTextView = dialogView.findViewById<TextView>(R.id.openStreetMapInfo)
        openStreetMapInfoTextView.setHtmlText(context.getString(R.string.about_open_street_map))

        val openTopoMapRInfoTextView = dialogView.findViewById<TextView>(R.id.openTopoMapRInfo)
        openTopoMapRInfoTextView.setHtmlText(context.getString(R.string.about_open_topo_map_r))

        val topOMapInfoTextView = dialogView.findViewById<TextView>(R.id.topOMapInfo)
        topOMapInfoTextView.setHtmlText(context.getString(R.string.about_top_o_map))

        val freemapSkInfoTextView = dialogView.findViewById<TextView>(R.id.freemapSkInfo)
        freemapSkInfoTextView.setHtmlText(context.getString(R.string.about_freemap_sk))

        val waymarkedTrailsInfoTextView = dialogView.findViewById<TextView>(R.id.waymarkedTrailsInfo)
        waymarkedTrailsInfoTextView.setHtmlText(context.getString(R.string.about_waymarked_trails))

        val dialog = AlertDialog.Builder(context)
            .setTitle(Utils.getAppName(context))
            .setView(dialogView)
            .setPositiveButton(R.string.close) { dialog, _ -> dialog.dismiss() }
            .create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.show()
    }

    private fun TextView.setHtmlText(htmlText: String) {
        movementMethod = LinkMovementMethod.getInstance()
        text = Utils.fromHtml(htmlText)
    }
}

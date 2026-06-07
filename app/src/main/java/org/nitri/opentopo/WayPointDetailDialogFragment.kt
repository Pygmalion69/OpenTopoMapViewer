package org.nitri.opentopo

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.nitri.opentopo.util.Utils.fromHtml
import org.nitri.opentopo.model.WayPointItem

class WayPointDetailDialogFragment : DialogFragment() {
    private var mCallback: Callback? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mCallback =
            requireActivity().supportFragmentManager.findFragmentByTag(BaseMainActivity.GPX_DETAIL_FRAGMENT_TAG) as Callback?
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater

        // Pass null as the parent view because it's going in the dialog layout
        @SuppressLint("InflateParams") val rootView =
            inflater.inflate(R.layout.fragment_way_point_detail, null)
        val tvName = rootView.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = rootView.findViewById<TextView>(R.id.tvDescription)
        tvDescription.movementMethod = LinkMovementMethod.getInstance()
        builder.setView(rootView)
        val dialog: Dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (mCallback != null) {
            val item = mCallback?.getSelectedWayPointItem()
            item?.wayPoint?.let { wp ->
                tvName.text = wp.name ?: ""

                val rawDesc = wp.desc
                if (!rawDesc.isNullOrBlank()) {
                    tvDescription.text = fromHtml(
                        rawDesc.replace("href=\"//", "href=\"http://")
                    )
                    tvDescription.visibility = View.VISIBLE
                } else {
                    tvDescription.text = ""
                    tvDescription.visibility = View.GONE
                }
            }
        }
        return dialog
    }

    internal interface Callback {
        fun getSelectedWayPointItem(): WayPointItem?
    }
}

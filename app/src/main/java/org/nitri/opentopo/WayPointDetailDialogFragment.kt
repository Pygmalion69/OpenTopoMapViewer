package org.nitri.opentopo

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.nitri.opentopo.Util.fromHtml
import org.nitri.opentopo.model.WayPointItem

class WayPointDetailDialogFragment : DialogFragment() {
    private var mCallback: Callback? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mCallback =
            requireActivity().supportFragmentManager.findFragmentByTag(MainActivity.GPX_DETAIL_FRAGMENT_TAG) as Callback?
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
            item?.wayPoint?.let{
                tvName.text = it.name
                tvDescription.text =
                    fromHtml(it.desc.replace("href=\"//", "href=\"http://"))
            }
        }
        return dialog
    }

    internal interface Callback {
        fun getSelectedWayPointItem(): WayPointItem?
    }
}

package org.nitri.opentopo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nitri.opentopo.R
import org.nitri.opentopo.model.WayPointHeaderItem
import org.nitri.opentopo.model.WayPointItem
import org.nitri.opentopo.model.WayPointListItem

class WayPointListAdapter(
    private val mItems: List<WayPointListItem>,
    private val mListener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int {
        return mItems[position].getListItemType()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): RecyclerView.ViewHolder {
        val view: View
        when (type) {
            WayPointListItem.HEADER -> {
                view = LayoutInflater
                    .from(viewGroup.context)
                    .inflate(R.layout.way_point_header_item, viewGroup, false)
                return ViewHolderHeader(view)
            }

            WayPointListItem.WAY_POINT -> {
                view = LayoutInflater
                    .from(viewGroup.context)
                    .inflate(R.layout.way_point_item, viewGroup, false)
                return ViewHolderWayPoint(view)
            }

            else -> return DefaultViewHolder(View(viewGroup.context))  // Should never occur!
        }

    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, pos: Int) {
        val item = mItems[pos]
        (viewHolder as ViewHolder).bindType(item)
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    abstract class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(
        itemView) {
        abstract fun bindType(item: WayPointListItem)
    }

    class ViewHolderHeader internal constructor(itemView: View) : ViewHolder(itemView) {
        private val textView: TextView

        init {
            textView = itemView.findViewById(R.id.textView)
        }

        override fun bindType(item: WayPointListItem) {
            textView.text = (item as WayPointHeaderItem).header
        }
    }

    /**
     * Class for non-null returns
     */
    class DefaultViewHolder(view: View) : ViewHolder(view) {
        override fun bindType(item: WayPointListItem) {
            // NOP
        }
    }

    inner class ViewHolderWayPoint internal constructor(itemView: View) : ViewHolder(itemView),
        View.OnClickListener {
        private val textView: TextView

        init {
            textView = itemView.findViewById(R.id.textView)
            itemView.setOnClickListener(this)
        }

        override fun bindType(item: WayPointListItem) {
            textView.text = (item as WayPointItem).wayPoint.name
            val itemIndex = mItems.indexOf(item)
            itemView.tag = itemIndex
        }

        override fun onClick(view: View) {
            mListener.onItemClick(view.tag as Int)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(index: Int)
    }
}
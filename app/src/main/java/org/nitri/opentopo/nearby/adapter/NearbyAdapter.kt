package org.nitri.opentopo.nearby.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.nitri.opentopo.R
import org.nitri.opentopo.nearby.adapter.NearbyAdapter.ItemViewHolder
import org.nitri.opentopo.nearby.entity.NearbyItem

class NearbyAdapter(
    private val mItems: MutableList<NearbyItem?>,
    private val mListener: OnItemClickListener
) : RecyclerView.Adapter<ItemViewHolder>() {
    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ItemViewHolder {
        val view = LayoutInflater
            .from(viewGroup.context)
            .inflate(R.layout.nearby_item, viewGroup, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        val item = mItems[position] ?: return
        Picasso.get().load(item.thumbnail).placeholder(R.drawable.ic_place).resize(60, 60)
            .centerCrop().into(viewHolder.ivThumb)
        viewHolder.tvTitle.text = item.title
        viewHolder.tvDescription.text = item.description
        if (position == mItems.size - 1) viewHolder.divider.visibility = View.GONE
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun getItemId(position: Int): Long {
        val pageId = mItems[position]?.pageid ?: return 0
        return pageId.toLong()
    }

    inner class ItemViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var ivThumb: ImageView
        var tvTitle: TextView
        var tvDescription: TextView
        var ivMap: ImageView
        var divider: View

        init {
            ivThumb = itemView.findViewById(R.id.ivThumb)
            tvTitle = itemView.findViewById(R.id.tvTitle)
            tvDescription = itemView.findViewById(R.id.tvDescription)
            ivMap = itemView.findViewById(R.id.ivMap)
            ivMap.setOnClickListener(this)
            divider = itemView.findViewById(R.id.divider)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.ivMap) {
                mListener.onMapItemClick(getBindingAdapterPosition())
            } else {
                mListener.onItemClick(getBindingAdapterPosition())
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(index: Int)
        fun onMapItemClick(index: Int)
    }
}

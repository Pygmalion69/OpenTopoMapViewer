package org.nitri.opentopo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nitri.opentopo.R
import org.nitri.opentopo.model.MarkerModel
import java.util.Locale

class MarkerListAdapter(
    private val onClick: (MarkerModel) -> Unit,
    private val onLongClick: (MarkerModel) -> Unit
) : RecyclerView.Adapter<MarkerListAdapter.MarkerViewHolder>() {

    private var markers: List<MarkerModel> = emptyList()
    private var selectionMode = false
    private var selectedIds: Set<Int> = emptySet()

    fun submitList(items: List<MarkerModel>) {
        markers = items
        notifyDataSetChanged()
    }

    fun setSelection(selectionMode: Boolean, selectedIds: Set<Int>) {
        this.selectionMode = selectionMode
        this.selectedIds = selectedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_marker, parent, false)
        return MarkerViewHolder(view)
    }

    override fun getItemCount(): Int = markers.size

    override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
        val marker = markers[position]
        val name = marker.name.ifBlank {
            holder.itemView.context.getString(R.string.marker_fallback_coordinates, marker.latitude, marker.longitude)
        }
        holder.labelView.text = name
        holder.coordinatesView.text = String.format(Locale.US, "%.5f, %.5f", marker.latitude, marker.longitude)

        if (selectionMode) {
            holder.checkBox.visibility = View.VISIBLE
            holder.checkBox.isChecked = selectedIds.contains(marker.id)
        } else {
            holder.checkBox.visibility = View.GONE
            holder.checkBox.isChecked = false
        }

        holder.itemView.setOnClickListener { onClick(marker) }
        holder.itemView.setOnLongClickListener {
            onLongClick(marker)
            true
        }
    }

    class MarkerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelView: TextView = view.findViewById(R.id.markerLabelText)
        val coordinatesView: TextView = view.findViewById(R.id.markerCoordinatesText)
        val checkBox: CheckBox = view.findViewById(R.id.markerSelectedCheckBox)
    }
}

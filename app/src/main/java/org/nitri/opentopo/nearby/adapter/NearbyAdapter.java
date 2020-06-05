package org.nitri.opentopo.nearby.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.nitri.opentopo.R;
import org.nitri.opentopo.nearby.entity.NearbyItem;

import java.util.List;

public class NearbyAdapter extends RecyclerView.Adapter<NearbyAdapter.ItemViewHolder>{


    private final List<NearbyItem> mItems;
    private final NearbyAdapter.OnItemClickListener mListener;

    public NearbyAdapter(List<NearbyItem> items, NearbyAdapter.OnItemClickListener listener) {
        mItems = items;
        mListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
        View view = LayoutInflater
                .from(viewGroup.getContext())
                .inflate(R.layout.nearby_item, viewGroup, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder viewHolder, int position) {
        NearbyItem item = mItems.get(position);
        Picasso.get().load(item.getThumbnail()).placeholder(R.drawable.ic_place).resize(60,60).centerCrop().into(viewHolder.ivThumb);
        viewHolder.tvTitle.setText(item.getTitle());
        viewHolder.tvDescription.setText(item.getDescription());
        if (position == mItems.size() - 1)
            viewHolder.divider.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(mItems.get(position).getPageid());
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView ivThumb;
        TextView tvTitle;
        TextView tvDescription;
        ImageView ivMap;
        View divider;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription= itemView.findViewById(R.id.tvDescription);
            ivMap = itemView.findViewById(R.id.ivMap);
            ivMap.setOnClickListener(this);
            divider = itemView.findViewById(R.id.divider);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.ivMap) {
                mListener.onMapItemClick(getBindingAdapterPosition());
            } else {
                mListener.onItemClick(getBindingAdapterPosition());
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int index);
        void onMapItemClick(int index);
    }

}

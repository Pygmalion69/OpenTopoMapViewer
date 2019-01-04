package org.nitri.opentopo.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.nitri.opentopo.R;
import org.nitri.opentopo.model.WayPointHeaderItem;
import org.nitri.opentopo.model.WayPointItem;
import org.nitri.opentopo.model.WayPointListItem;

import java.util.List;

public class WayPointListAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<WayPointListItem> mItems;

    public WayPointListAdapter(List<WayPointListItem> items) {
        mItems = items;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getListItemType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
        View view;
        switch (type) {
            case WayPointListItem.HEADER:
                view = LayoutInflater
                        .from(viewGroup.getContext())
                        .inflate(R.layout.way_point_header_item, viewGroup, false);
                return new ViewHolderHeader(view);
            case WayPointListItem.WAY_POINT:
                view = LayoutInflater
                        .from(viewGroup.getContext())
                        .inflate(R.layout.way_point_item, viewGroup, false);
                return new ViewHolderWayPoint(view);
        }
        //noinspection ConstantConditions
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int pos) {
        WayPointListItem item = mItems.get(pos);
        ((ViewHolder) viewHolder).bindType(item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public abstract class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }

        public abstract void bindType(WayPointListItem item);
    }

    public class ViewHolderHeader extends ViewHolder {
        private final TextView textView;

        ViewHolderHeader(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.textView);
        }

        public void bindType(WayPointListItem item) {
            textView.setText(((WayPointHeaderItem) item).getHeader());
        }
    }

    public class ViewHolderWayPoint extends ViewHolder {
        private final TextView textView;

        ViewHolderWayPoint(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.textView);
        }

        public void bindType(WayPointListItem item) {
            textView.setText(((WayPointItem) item).getWayPoint().getName());
        }
    }
}
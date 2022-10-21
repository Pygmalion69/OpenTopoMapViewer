package org.nitri.opentopo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.nitri.opentopo.model.WayPointItem;


public class WayPointDetailDialogFragment extends DialogFragment {

    private Callback mCallback;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (requireActivity().getSupportFragmentManager() != null)
            mCallback = (Callback) requireActivity().getSupportFragmentManager().findFragmentByTag(MainActivity.GPX_DETAIL_FRAGMENT_TAG);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Pass null as the parent view because it's going in the dialog layout
        @SuppressLint("InflateParams")
        View rootView = inflater.inflate(R.layout.fragment_way_point_detail, null);

        TextView tvName = rootView.findViewById(R.id.tvTitle);
        TextView tvDescription = rootView.findViewById(R.id.tvDescription);

        tvDescription.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setView(rootView);

        Dialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (mCallback != null) {
            WayPointItem item = mCallback.getSelectedWayPointItem();
            if (item != null && item.getWayPoint() != null) {
                tvName.setText(item.getWayPoint().getName());
                tvDescription.setText(Util.fromHtml(item.getWayPoint().getDesc().replace("href=\"//", "href=\"http://")));
            }
        }

        return dialog;
    }


    interface Callback {
        WayPointItem getSelectedWayPointItem();
    }
}

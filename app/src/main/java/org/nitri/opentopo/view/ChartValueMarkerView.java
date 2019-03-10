
package org.nitri.opentopo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import org.nitri.opentopo.R;

/**
 * Custom implementation of the MarkerView.
 *
 */
public class ChartValueMarkerView extends MarkerView {

    private TextView tvContent;

    public ChartValueMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);

        tvContent = findViewById(R.id.tvContent);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        tvContent.setText(String.format("%.1f", e.getY()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
